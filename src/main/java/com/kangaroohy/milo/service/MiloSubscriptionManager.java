package com.kangaroohy.milo.service;

import com.kangaroohy.milo.configuration.MiloProperties;
import com.kangaroohy.milo.runner.subscription.SubscriptionCallback;
import com.kangaroohy.milo.runner.subscription.SubscriptionHandle;
import com.kangaroohy.milo.utils.CustomUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

/**
 * Groups monitored items by endpoint and publishing interval. A single client
 * can therefore serve many business subscriptions, and a single server-side
 * Subscription can serve many monitored items.
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
                        new ThreadPoolExecutor.CallerRunsPolicy());
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
        if (publishingInterval <= 0) {
            throw new IllegalArgumentException("publishingInterval 必须大于 0");
        }
        if (samplingInterval <= 0) {
            throw new IllegalArgumentException("samplingInterval 必须大于 0");
        }
        if (callback == null) {
            throw new IllegalArgumentException("订阅回调不能为空");
        }

        String resolvedClientName = clients.resolveClientName(clientName);
        SubscriptionKey key = new SubscriptionKey(resolvedClientName, publishingInterval, samplingInterval);
        SubscriptionContext context = contexts.get(key);
        if (context == null) {
            SubscriptionContext created = new SubscriptionContext(key, clients.getClient(resolvedClientName));
            SubscriptionContext previous = contexts.putIfAbsent(key, created);
            context = previous == null ? created : previous;
            if (previous != null) {
                created.close();
            }
        }
        return context.add(identifiers, callback);
    }

    /**
     * 返回当前由管理器维护的服务端 Subscription 数量。
     *
     * @return 服务端 Subscription 数量
     */
    public int subscriptionCount() {
        return contexts.size();
    }

    @Override
    public void close() {
        for (SubscriptionContext context : new ArrayList<>(contexts.values())) {
            context.close();
        }
        contexts.clear();
        for (ExecutorService executor : ownedCallbackExecutors) {
            executor.shutdown();
        }
        managementExecutor.shutdown();
    }

    private final class SubscriptionContext implements AutoCloseable {
        private final SubscriptionKey key;
        private final OpcUaClient client;
        private final Map<String, ItemRegistration> items = new LinkedHashMap<>();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicBoolean rebuilding = new AtomicBoolean();
        private final OpcUaSubscription.SubscriptionListener subscriptionListener = new SubscriptionListener();
        private volatile OpcUaSubscription subscription;

        private SubscriptionContext(SubscriptionKey key, OpcUaClient client) {
            this.key = key;
            this.client = client;
        }

        private synchronized SubscriptionHandle add(List<String> identifiers,
                                                     SubscriptionCallback callback) throws Exception {
            ensureOpen();
            ensureSubscription();

            LinkedHashSet<String> uniqueIdentifiers = new LinkedHashSet<>(identifiers);
            List<ItemRegistration> registrations = new ArrayList<>(uniqueIdentifiers.size());
            List<ItemRegistration> newRegistrations = new ArrayList<>();
            try {
                for (String identifier : uniqueIdentifiers) {
                    if (identifier == null || identifier.trim().isEmpty()) {
                        throw new IllegalArgumentException("NodeId 不能为空");
                    }
                    ItemRegistration registration = items.get(identifier);
                    if (registration == null) {
                        OpcUaMonitoredItem item = OpcUaMonitoredItem.newDataItem(
                                CustomUtil.parseNodeId(identifier));
                        item.setSamplingInterval(key.samplingInterval);
                        item.setQueueSize(Unsigned.uint(subscriptionQueueSize));
                        item.setDiscardOldest(true);
                        registration = new ItemRegistration(identifier, item);
                        attachListener(registration);
                        items.put(identifier, registration);
                        newRegistrations.add(registration);
                    }
                    registration.callbacks.add(callback);
                    registrations.add(registration);
                }

                if (!newRegistrations.isEmpty()) {
                    createDataItems(subscription, newRegistrations);
                }
            } catch (Exception e) {
                rollbackAdd(registrations, newRegistrations, callback, e);
                throw e;
            }

            AtomicBoolean handleClosed = new AtomicBoolean();
            return () -> {
                if (handleClosed.compareAndSet(false, true)) {
                    remove(registrations, callback);
                }
            };
        }

        private void rollbackAdd(List<ItemRegistration> registrations,
                                 List<ItemRegistration> newRegistrations,
                                 SubscriptionCallback callback,
                                 Exception original) {
            for (ItemRegistration registration : registrations) {
                registration.callbacks.remove(callback);
            }
            for (ItemRegistration registration : newRegistrations) {
                items.remove(registration.identifier, registration);
                registration.generation++;
                registration.item.setDataValueListener(null);
            }
            if (!newRegistrations.isEmpty()) {
                try {
                    removeDataItems(subscription, newRegistrations);
                } catch (Exception cleanupException) {
                    original.addSuppressed(cleanupException);
                }
            }
            if (items.isEmpty()) {
                close();
            }
        }

        private void attachListener(ItemRegistration registration) {
            long itemGeneration = registration.generation;
            registration.item.setDataValueListener((item, value) ->
                    callbackExecutorFor(registration.identifier)
                            .execute(() -> dispatch(registration, item, itemGeneration, value)));
        }

        private void dispatch(ItemRegistration registration,
                              OpcUaMonitoredItem source,
                              long expectedGeneration,
                              DataValue value) {
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
                    log.error("OPC UA subscription callback failed for {}", registration.identifier, e);
                }
            }
        }

        private synchronized void remove(List<ItemRegistration> registrations,
                                         SubscriptionCallback callback) {
            if (closed.get()) {
                return;
            }
            List<ItemRegistration> removed = new ArrayList<>();
            for (ItemRegistration registration : registrations) {
                registration.callbacks.remove(callback);
                if (registration.callbacks.isEmpty()
                        && items.remove(registration.identifier, registration)) {
                    registration.generation++;
                    registration.item.setDataValueListener(null);
                    removed.add(registration);
                }
            }
            if (items.isEmpty()) {
                close();
            } else if (!removed.isEmpty()) {
                try {
                    removeDataItems(subscription, removed);
                } catch (Exception e) {
                    log.warn("Failed to delete OPC UA monitored items for {}", key, e);
                }
            }
        }

        private void ensureSubscription() throws Exception {
            if (subscription == null) {
                OpcUaSubscription created = new OpcUaSubscription(client, key.publishingInterval);
                created.setMaxMonitoredItemsPerCall(Unsigned.uint(subscriptionBatchSize));
                created.setSubscriptionListener(subscriptionListener);
                created.create();
                subscription = created;
            } else if (subscription.getSyncState() == OpcUaSubscription.SyncState.INITIAL) {
                subscription.create();
                subscription.synchronizeMonitoredItems();
            }
        }

        private void recreateAfterTransferFailure(OpcUaSubscription failedSubscription) {
            if (closed.get() || !rebuilding.compareAndSet(false, true)) {
                return;
            }
            try {
                synchronized (this) {
                    if (closed.get() || items.isEmpty() || subscription != failedSubscription) {
                        return;
                    }
                    if (failedSubscription.getSyncState() != OpcUaSubscription.SyncState.INITIAL) {
                        return;
                    }
                    failedSubscription.create();
                    failedSubscription.synchronizeMonitoredItems();
                }
            } catch (Exception e) {
                log.error("Failed to recreate OPC UA subscription for {}", key, e);
            } finally {
                rebuilding.set(false);
            }
        }

        private void createDataItems(OpcUaSubscription target,
                                     List<ItemRegistration> registrations) throws Exception {
            List<OpcUaMonitoredItem> dataItems = monitoredItems(registrations);
            target.addMonitoredItems(dataItems);
            try {
                target.synchronizeMonitoredItems();
                for (OpcUaMonitoredItem item : dataItems) {
                    StatusCode itemStatus = item.getCreateResult().orElse(null);
                    if (itemStatus == null || !itemStatus.isGood()) {
                        throw new IllegalStateException("OPC UA 监控项创建失败: " + itemStatus);
                    }
                }
            } catch (Exception e) {
                try {
                    target.removeMonitoredItems(dataItems);
                    target.synchronizeMonitoredItems();
                } catch (Exception cleanupException) {
                    e.addSuppressed(cleanupException);
                }
                throw e;
            }
        }

        private void removeDataItems(OpcUaSubscription target,
                                     List<ItemRegistration> registrations) throws Exception {
            target.removeMonitoredItems(monitoredItems(registrations));
            target.synchronizeMonitoredItems();
        }

        private List<OpcUaMonitoredItem> monitoredItems(List<ItemRegistration> registrations) {
            List<OpcUaMonitoredItem> dataItems = new ArrayList<>(registrations.size());
            for (ItemRegistration registration : registrations) {
                dataItems.add(registration.item);
            }
            return dataItems;
        }

        private void deleteSubscription() {
            OpcUaSubscription current = subscription;
            subscription = null;
            if (current != null) {
                current.setSubscriptionListener(null);
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
            for (ItemRegistration registration : items.values()) {
                registration.generation++;
                registration.callbacks.clear();
                registration.item.setDataValueListener(null);
            }
            items.clear();
            deleteSubscription();
            contexts.remove(key, this);
        }

        private final class SubscriptionListener implements OpcUaSubscription.SubscriptionListener {
            @Override
            public void onTransferFailed(OpcUaSubscription failedSubscription, StatusCode statusCode) {
                if (failedSubscription == subscription) {
                    try {
                        managementExecutor.execute(
                                () -> recreateAfterTransferFailure(failedSubscription));
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
        private final OpcUaMonitoredItem item;
        private volatile long generation;

        private ItemRegistration(String identifier, OpcUaMonitoredItem item) {
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
