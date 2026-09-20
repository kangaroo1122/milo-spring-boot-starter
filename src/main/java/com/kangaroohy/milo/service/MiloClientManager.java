package com.kangaroohy.milo.service;

import com.kangaroohy.milo.configuration.MiloProperties;
import com.kangaroohy.milo.exception.EndPointNotFoundException;
import com.kangaroohy.milo.pool.MiloConnectFactory;
import com.kangaroohy.milo.utils.KeyStoreLoader;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns one long-lived OPC UA client per configured endpoint.
 *
 * <p>OpcUaClient is an asynchronous, multiplexed client.  It is not a
 * request-scoped resource and should not be borrowed and returned for every
 * read or write.  Keeping one client per endpoint also makes subscription
 * ownership explicit and prevents a subscription from pinning a pool slot
 * forever.</p>
 */
public class MiloClientManager implements AutoCloseable {

    private final MiloProperties properties;
    private final MiloConnectFactory factory;
    private final Map<String, OpcUaClient> clients = new ConcurrentHashMap<>();
    private final Object creationLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean();

    public MiloClientManager(MiloProperties properties) {
        this.properties = properties;
        this.factory = new MiloConnectFactory(properties, properties.getPrimary());
    }

    /**
     * 获取指定 endpoint 的长期客户端；客户端不存在时才建立连接。
     *
     * @param clientName endpoint 配置 key，为 null 时使用 primary
     * @return 长期 OPC UA 客户端
     */
    public OpcUaClient getClient(String clientName) throws Exception {
        if (closed.get()) {
            throw new IllegalStateException("OPC UA client manager 已关闭");
        }
        String key = resolveClientName(clientName);
        OpcUaClient existing = clients.get(key);
        if (existing != null) {
            return existing;
        }

        synchronized (creationLock) {
            existing = clients.get(key);
            if (existing == null) {
                existing = factory.createConnectedClient(config(key));
                clients.put(key, existing);
            }
            return existing;
        }
    }

    /**
     * 解析 endpoint 配置 key。
     *
     * @param clientName endpoint 配置 key，为 null 或空白时使用 primary
     * @return 实际使用的 endpoint 配置 key
     */
    public String resolveClientName(String clientName) {
        String key = StringUtils.hasText(clientName) ? clientName : properties.getPrimary();
        if (!StringUtils.hasText(key) || !properties.getConfig().containsKey(key)) {
            throw new EndPointNotFoundException("OPC UA client 配置不存在: " + key);
        }
        return key;
    }

    /**
     * 获取并校验 endpoint 配置。
     *
     * @param clientName endpoint 配置 key，为 null 或空白时使用 primary
     * @return 已校验的 endpoint 配置
     */
    public MiloProperties.Config config(String clientName) {
        String key = resolveClientName(clientName);
        MiloProperties.Config config = properties.getConfig().get(key);
        if (config == null || !StringUtils.hasText(config.getEndpoint())) {
            throw new EndPointNotFoundException("OPC UA client 配置不存在或 endpoint 为空: " + key);
        }
        return config;
    }

    /**
     * 返回当前已建立的客户端数量。
     *
     * @return 已建立的客户端数量
     */
    public int size() {
        return clients.size();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        clients.forEach((key, client) -> {
            try {
                client.disconnectAsync().get(properties.getRequestTimeout(), TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
                // Best effort during application shutdown.
            }
        });
        clients.clear();
        KeyStoreLoader.close();
    }
}
