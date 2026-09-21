package com.kangaroohy.milo.service;

import com.kangaroohy.milo.configuration.MiloProperties;
import com.kangaroohy.milo.runner.subscription.SubscriptionCallback;
import com.kangaroohy.milo.runner.subscription.SubscriptionHandle;
import com.kangaroohy.milo.utils.CustomUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedDataItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedSubscription;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Groups monitored items by endpoint and publishing interval.  A single
 * client can therefore serve many business subscriptions, and a single
 * server-side Subscription can serve many monitored items.
 */
@Slf4j
public class MiloSubscriptionManager implements AutoCloseable {

    private final MiloClientManager clients;
    private final List<Executor> callbackExecutors;
    private final List<ExecutorService> ownedCallbackExecutors;
    private final ExecutorService managementExecutor;
    private final int subscriptionBatchSize;
    private final int subscriptionQueueSize;
    private final Map<SubscriptionKey, SubscriptionContext> contexts = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ReentrantReadWriteLock lifecycle = new ReentrantReadWriteLock();
    private final AtomicLong droppedCallbacks = new AtomicLong();
    private final AtomicLong callbackFailures = new AtomicLong();
    private final AtomicLong recoveryFailures = new AtomicLong();

    public MiloSubscriptionManager(MiloClientManager clients) {
        this(clients, null, 200, 10, 2, 10000);
    }

    public MiloSubscriptionManager(MiloClientManager clients, Executor callbackExecutor) {
        this(clients, callbackExecutor, 200, 10, 0, 0);
    }

    public MiloSubscriptionManager(MiloClientManager clients, MiloProperties properties) {
        this(clients, null,
                properties == null ? 200 : properties.getSubscriptionBatchSize(),
                properties == null ? 10 : properties.getSubscriptionQueueSize(),
                properties == null ? 2 : properties.getCallbackThreads(),
                properties == null ? 10000 : properties.getCallbackQueueCapacity());
    }

    private MiloSubscriptionManager(MiloClientManager clients,
                                     Executor callbackExecutor,
                                     int subscriptionBatchSize,
                                     int subscriptionQueueSize,
                                     int callbackThreads,
                                     int callbackQueueCapacity) {
        this.clients = clients;
        if (subscriptionBatchSize <= 0 || subscriptionQueueSize <= 0) {
            throw new IllegalArgumentException("订阅批次大小和队列大小必须大于 0");
        }
        this.subscriptionBatchSize = subscriptionBatchSize;
        this.subscriptionQueueSize = subscriptionQueueSize;
        if (callbackExecutor == null) {
            if (callbackThreads <= 0 || callbackQueueCapacity <= 0) {
                throw new IllegalArgumentException("回调线程数和队列容量必须大于 0");
            }
            List<Executor> executors = new ArrayList<>(callbackThreads);
            List<ExecutorService> ownedExecutors = new ArrayList<>(callbackThreads);
            int queuePerThread = Math.max(1, callbackQueueCapacity / callbackThreads);
            for (int i = 0; i < callbackThreads; i++) {
                final int threadIndex = i;
                ThreadPoolExecutor executor = new ThreadPoolExecutor(
                        1, 1, 0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>(queuePerThread),
                        runnable -> {
                            Thread thread = new Thread(runnable,
                                    "milo-subscription-callback-" + threadIndex);
                            thread.setDaemon(true);
                            return thread;
                        },
                        (task, pool) -> {
                            if (!pool.isShutdown()) {
                                long count = droppedCallbacks.incrementAndGet();
                                // Drop the newest notification, never run it ahead of queued values.
                                if ((count & (count - 1)) == 0) {
                                    log.warn("OPC UA callback queue full; dropped {} notifications", count);
                                }
                            }
                        });
                executors.add(executor);
                ownedExecutors.add(executor);
            }
            this.callbackExecutors = Collections.unmodifiableList(executors);
            this.ownedCallbackExecutors = ownedExecutors;
        } else {
            this.callbackExecutors = Collections.singletonList(callbackExecutor);
            this.ownedCallbackExecutors = Collections.emptyList();
        }
        this.managementExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "milo-subscription-management");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 注册订阅；发布周期和采样周期相同时使用该快捷重载。
     *
     * @param identifiers 点位 ID 列表
     * @param publishingInterval 发布周期，单位毫秒
     * @param clientName endpoint 配置 key，为 null 时使用 primary
     * @param callback 业务回调
     * @return 可关闭的订阅句柄
     */
    public SubscriptionHandle subscribe(List<String> identifiers,
                                        double publishingInterval,
                                        String clientName,
                                        SubscriptionCallback callback) throws Exception {
        return subscribe(identifiers, publishingInterval, publishingInterval, clientName, callback);
    }

    /**
     * 注册订阅，并按 endpoint、发布周期和采样周期复用服务端 Subscription。
     *
     * @param identifiers 点位 ID 列表
     * @param publishingInterval 发布周期，单位毫秒
     * @param samplingInterval 采样周期，单位毫秒
     * @param clientName endpoint 配置 key，为 null 时使用 primary
     * @param callback 业务回调
     * @return 可关闭的订阅句柄
     */
    public SubscriptionHandle subscribe(List<String> identifiers,
                                        double publishingInterval,
                                        double samplingInterval,
                                        String clientName,
                                        SubscriptionCallback callback) throws Exception {
        if (identifiers == null || identifiers.isEmpty()) {
            throw new IllegalArgumentException("订阅点位不能为空");
        }
        if (!Double.isFinite(publishingInterval) || publishingInterval <= 0) {
            throw new IllegalArgumentException("publishingInterval 必须大于 0");
        }
        if (!Double.isFinite(samplingInterval) || samplingInterval <= 0) {
            throw new IllegalArgumentException("samplingInterval 必须大于 0");
        }
        if (callback == null) {
            throw new IllegalArgumentException("订阅回调不能为空");
        }

        List<String> validatedIdentifiers = new ArrayList<>(identifiers);
        for (String identifier : validatedIdentifiers) {
            CustomUtil.parseNodeId(identifier);
        }
        lifecycle.readLock().lock();
        try {
            if (closed.get()) {
                throw new IllegalStateException("订阅管理器已关闭");
            }
            String resolvedClientName = clients.resolveClientName(clientName);
            SubscriptionKey key = new SubscriptionKey(resolvedClientName, publishingInterval, samplingInterval);
            while (true) {
                SubscriptionContext context = contexts.get(key);
                if (context == null) {
                    SubscriptionContext created = new SubscriptionContext(key, clients.getClient(resolvedClientName));
                    SubscriptionContext previous = contexts.putIfAbsent(key, created);
                    context = previous == null ? created : previous;
                    if (previous != null) {
                        created.close();
                    }
                }
                synchronized (context) {
                    // The last handle may have closed this context after the map lookup.
                    if (!context.closed.get()) {
                        return context.add(validatedIdentifiers, callback);
                    }
                }
            }
        } finally {
            lifecycle.readLock().unlock();
        }
    }

    /**
     * 返回当前由管理器维护的服务端 Subscription 数量。
     *
     * @return 服务端 Subscription 数量
     */
    public int subscriptionCount() {
        return contexts.size();
    }

    public long droppedCallbackCount() {
        return droppedCallbacks.get();
    }

    public long callbackFailureCount() {
        return callbackFailures.get();
    }

    public long recoveryFailureCount() {
        return recoveryFailures.get();
    }

    public int pendingCallbackCount() {
        return ownedCallbackExecutors.stream()
                .mapToInt(executor -> ((ThreadPoolExecutor) executor).getQueue().size()).sum();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        lifecycle.writeLock().lock();
        try {
            for (SubscriptionContext context : new ArrayList<>(contexts.values())) {
                context.close();
            }
            contexts.clear();
            for (ExecutorService executor : ownedCallbackExecutors) {
                executor.shutdown();
            }
            managementExecutor.shutdown();
        } finally {
            lifecycle.writeLock().unlock();
        }
    }

    private final class SubscriptionContext implements AutoCloseable {
        private final SubscriptionKey key;
        private final OpcUaClient client;
        private final Map<String, ItemRegistration> items = new LinkedHashMap<>();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicBoolean rebuilding = new AtomicBoolean();
        private final UaSubscriptionManager.SubscriptionListener subscriptionListener = new SubscriptionListener();
        private volatile ManagedSubscription subscription;

        private SubscriptionContext(SubscriptionKey key, OpcUaClient client) {
            this.key = key;
            this.client = client;
            client.getSubscriptionManager().addSubscriptionListener(subscriptionListener);
        }

        private synchronized SubscriptionHandle add(List<String> identifiers,
                                                     SubscriptionCallback callback) throws Exception {
            ensureOpen();
            List<ItemRegistration> registrations = new ArrayList<>();
            List<ManagedDataItem> createdDataItems = Collections.emptyList();
            try {
                ensureSubscription();
                LinkedHashSet<String> uniqueIdentifiers = new LinkedHashSet<>(identifiers);
                List<String> newIdentifiers = new ArrayList<>();
                for (String identifier : uniqueIdentifiers) {
                    if (identifier == null || identifier.trim().isEmpty()) {
                        throw new IllegalArgumentException("NodeId 不能为空");
                    }
                    if (!items.containsKey(identifier)) {
                        newIdentifiers.add(identifier);
                    }
                }

                if (!newIdentifiers.isEmpty()) {
                    List<ManagedDataItem> dataItems = createDataItems(subscription, newIdentifiers);
                    createdDataItems = dataItems;
                    for (int i = 0; i < newIdentifiers.size(); i++) {
                        String identifier = newIdentifiers.get(i);
                        ManagedDataItem item = dataItems.get(i);
                        ItemRegistration registration = new ItemRegistration(identifier, item);
                        items.put(identifier, registration);
                        registrations.add(registration);
                        registration.listener = attachListener(registration, item);
                    }
                }

                for (String identifier : uniqueIdentifiers) {
                    ItemRegistration registration = items.get(identifier);
                    registration.callbacks.add(callback);
                    if (!registrations.contains(registration)) {
                        registrations.add(registration);
                    }
                }
            } catch (Exception e) {
                remove(registrations, callback);
                deleteUntrackedItems(createdDataItems);
                throw e;
            }
            AtomicBoolean handleClosed = new AtomicBoolean();
            return () -> {
                if (handleClosed.compareAndSet(false, true)) {
                    remove(registrations, callback);
                }
            };
        }

        private void deleteUntrackedItems(List<ManagedDataItem> candidates) {
            if (candidates == null || candidates.isEmpty()) {
                return;
            }
            for (ManagedDataItem candidate : candidates) {
                boolean tracked = false;
                for (ItemRegistration registration : items.values()) {
                    if (registration.item == candidate) {
                        tracked = true;
                        break;
                    }
                }
                if (!tracked) {
                    try {
                        candidate.delete();
                    } catch (Exception cleanupException) {
                        log.debug("Failed to clean up untracked monitored item", cleanupException);
                    }
                }
            }
        }

        private ManagedDataItem.DataValueListener attachListener(ItemRegistration registration,
                                                                  ManagedDataItem item) {
            long itemGeneration = registration.generation;
            Consumer<DataValue> consumer = value -> callbackExecutorFor(registration.identifier)
                    .execute(() -> dispatch(registration, item, itemGeneration, value));
            return item.addDataValueListener(consumer);
        }

        private void dispatch(ItemRegistration registration, ManagedDataItem source,
                              long expectedGeneration, DataValue value) {
            if (closed.get() || registration.item != source
                    || expectedGeneration != registration.generation) {
                return;
            }
            for (SubscriptionCallback callback : registration.callbacks) {
                if (closed.get() || registration.item != source
                        || expectedGeneration != registration.generation) {
                    return;
                }
                try {
                    callback.onSubscribe(source, value);
                } catch (Exception e) {
                    callbackFailures.incrementAndGet();
                    log.error("OPC UA subscription callback failed for {}", registration.identifier, e);
                }
            }
        }

        private synchronized void remove(List<ItemRegistration> registrations,
                                         SubscriptionCallback callback) {
            if (closed.get()) {
                return;
            }
            for (ItemRegistration registration : registrations) {
                registration.callbacks.remove(callback);
                if (registration.callbacks.isEmpty() && items.remove(registration.identifier, registration)) {
                    registration.generation++;
                    try {
                        registration.item.delete();
                    } catch (Exception e) {
                        log.debug("Failed to delete monitored item {}", registration.identifier, e);
                    }
                }
            }
            if (items.isEmpty()) {
                close();
            }
        }

        private void ensureSubscription() throws Exception {
            if (subscription != null) {
                return;
            }
            subscription = ManagedSubscription.create(client, key.publishingInterval);
            subscription.setDefaultSamplingInterval(key.samplingInterval);
            subscription.setDefaultQueueSize(UInteger.valueOf(subscriptionQueueSize));
        }

        private void recreateAfterTransferFailure() {
            if (closed.get() || !rebuilding.compareAndSet(false, true)) {
                return;
            }
            ManagedSubscription replacement = null;
            try {
                synchronized (this) {
                    if (closed.get() || items.isEmpty()) {
                        return;
                    }
                    ManagedSubscription old = subscription;
                    replacement = ManagedSubscription.create(client, key.publishingInterval);
                    replacement.setDefaultSamplingInterval(key.samplingInterval);
                    replacement.setDefaultQueueSize(UInteger.valueOf(subscriptionQueueSize));

                    List<String> identifiers = new ArrayList<>(items.keySet());
                    List<ManagedDataItem> dataItems = createDataItems(replacement, identifiers);
                    if (dataItems.size() != identifiers.size()) {
                        throw new IllegalStateException("OPC UA 返回的监控项数量与请求不一致");
                    }
                    List<ManagedDataItem.DataValueListener> replacementListeners = new ArrayList<>(dataItems.size());
                    try {
                        for (int i = 0; i < dataItems.size(); i++) {
                            ItemRegistration registration = items.get(identifiers.get(i));
                            // Advance the item generation before registering the
                            // replacement listener so new notifications capture
                            // the new generation rather than the stale one.
                            registration.generation++;
                            replacementListeners.add(attachListener(registration, dataItems.get(i)));
                        }
                    } catch (Exception listenerException) {
                        for (int i = 0; i < replacementListeners.size(); i++) {
                            dataItems.get(i).removeDataValueListener(replacementListeners.get(i));
                        }
                        throw listenerException;
                    }

                    for (int i = 0; i < dataItems.size(); i++) {
                        ItemRegistration registration = items.get(identifiers.get(i));
                        if (registration.listener != null) {
                            registration.item.removeDataValueListener(registration.listener);
                        }
                        registration.item = dataItems.get(i);
                        registration.listener = replacementListeners.get(i);
                    }
                    subscription = replacement;
                    if (old != null) {
                        try {
                            old.delete();
                        } catch (Exception e) {
                            log.debug("Failed to delete transferred subscription", e);
                        }
                    }
                }
            } catch (Exception e) {
                try {
                    if (replacement != null) {
                        replacement.delete();
                    }
                } catch (Exception ignored) {
                    // Preserve the original subscription error.
                }
                recoveryFailures.incrementAndGet();
                log.error("Failed to recreate OPC UA subscription for {}", key, e);
            } finally {
                rebuilding.set(false);
            }
        }

        private List<ManagedDataItem> createDataItems(ManagedSubscription target,
                                                      List<String> identifiers) throws Exception {
            List<ManagedDataItem> created = new ArrayList<>();
            try {
                for (int start = 0; start < identifiers.size(); start += subscriptionBatchSize) {
                    int end = Math.min(start + subscriptionBatchSize, identifiers.size());
                    List<NodeId> nodeIds = new ArrayList<>(end - start);
                    for (String identifier : identifiers.subList(start, end)) {
                        nodeIds.add(CustomUtil.parseNodeId(identifier));
                    }
                    List<ManagedDataItem> batch = target.createDataItems(nodeIds);
                    if (batch.size() != nodeIds.size()) {
                        throw new IllegalStateException("OPC UA 返回的监控项数量与请求数量不一致");
                    }
                    created.addAll(batch);
                    for (ManagedDataItem item : batch) {
                        StatusCode itemStatus = item.getStatusCode();
                        if (itemStatus == null || !itemStatus.isGood()) {
                            throw new IllegalStateException("OPC UA 监控项创建失败: " + itemStatus);
                        }
                    }
                }
                return created;
            } catch (Exception e) {
                try {
                    if (!created.isEmpty()) {
                        target.deleteDataItems(created);
                    }
                } catch (Exception cleanupException) {
                    e.addSuppressed(cleanupException);
                }
                throw e;
            }
        }

        private void deleteSubscription() {
            ManagedSubscription current = subscription;
            subscription = null;
            if (current != null) {
                try {
                    current.delete();
                } catch (Exception e) {
                    log.debug("Failed to delete OPC UA subscription", e);
                }
            }
        }

        private void ensureOpen() {
            if (closed.get()) {
                throw new IllegalStateException("订阅已关闭");
            }
        }

        @Override
        public synchronized void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            client.getSubscriptionManager().removeSubscriptionListener(subscriptionListener);
            for (ItemRegistration registration : items.values()) {
                registration.generation++;
                registration.callbacks.clear();
            }
            items.clear();
            deleteSubscription();
            contexts.remove(key, this);
        }

        private final class SubscriptionListener implements UaSubscriptionManager.SubscriptionListener {
            @Override
            public void onSubscriptionTransferFailed(UaSubscription failedSubscription, StatusCode statusCode) {
                ManagedSubscription current = SubscriptionContext.this.subscription;
                if (failedSubscription != null && current != null && current.getSubscription() != null
                        && current.getSubscription().getSubscriptionId() != null
                        && current.getSubscription().getSubscriptionId().equals(failedSubscription.getSubscriptionId())) {
                    try {
                        managementExecutor.execute(SubscriptionContext.this::recreateAfterTransferFailure);
                    } catch (java.util.concurrent.RejectedExecutionException ignored) {
                        // Application is shutting down.
                    }
                }
            }
        }
    }

    private static final class ItemRegistration {
        private final String identifier;
        private final CopyOnWriteArrayList<SubscriptionCallback> callbacks = new CopyOnWriteArrayList<>();
        private volatile ManagedDataItem item;
        private ManagedDataItem.DataValueListener listener;
        private volatile long generation;

        private ItemRegistration(String identifier, ManagedDataItem item) {
            this.identifier = identifier;
            this.item = item;
        }
    }

    private Executor callbackExecutorFor(String identifier) {
        int index = (identifier.hashCode() & Integer.MAX_VALUE) % callbackExecutors.size();
        return callbackExecutors.get(index);
    }

    private static final class SubscriptionKey {
        private final String clientName;
        private final double publishingInterval;
        private final double samplingInterval;

        private SubscriptionKey(String clientName, double publishingInterval, double samplingInterval) {
            this.clientName = clientName;
            this.publishingInterval = publishingInterval;
            this.samplingInterval = samplingInterval;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SubscriptionKey)) return false;
            SubscriptionKey that = (SubscriptionKey) o;
            return Double.compare(that.publishingInterval, publishingInterval) == 0
                    && Double.compare(that.samplingInterval, samplingInterval) == 0
                    && clientName.equals(that.clientName);
        }

        @Override
        public int hashCode() {
            int result = clientName.hashCode();
            long temp = Double.doubleToLongBits(publishingInterval);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(samplingInterval);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return clientName + "@pub=" + publishingInterval + ",sample=" + samplingInterval;
        }
    }
}
