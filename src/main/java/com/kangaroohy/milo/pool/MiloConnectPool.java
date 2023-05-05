package com.kangaroohy.milo.pool;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

/**
 * 类 MiloConnectPool 功能描述：<br/>
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2023/5/4 19:17
 */
public class MiloConnectPool extends GenericObjectPool<OpcUaClient> {

    public MiloConnectPool(PooledObjectFactory<OpcUaClient> factory) {
        super(factory);
    }

    public MiloConnectPool(PooledObjectFactory<OpcUaClient> factory, GenericObjectPoolConfig<OpcUaClient> config) {
        super(factory, config);
    }

    public MiloConnectPool(PooledObjectFactory<OpcUaClient> factory, GenericObjectPoolConfig<OpcUaClient> config, AbandonedConfig abandonedConfig) {
        super(factory, config, abandonedConfig);
    }
}
