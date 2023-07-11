package com.kangaroohy.milo.pool;

import com.kangaroohy.milo.configuration.MiloProperties;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.*;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

/**
 * 类 MiloConnectPool 功能描述：<br/>
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2023/5/4 19:17
 */
public class MiloConnectPool extends GenericKeyedObjectPool<MiloProperties.Config, OpcUaClient> {

    public MiloConnectPool(KeyedPooledObjectFactory<MiloProperties.Config, OpcUaClient> factory) {
        super(factory);
    }

    public MiloConnectPool(KeyedPooledObjectFactory<MiloProperties.Config, OpcUaClient> factory, GenericKeyedObjectPoolConfig<OpcUaClient> config) {
        super(factory, config);
    }

    public MiloConnectPool(KeyedPooledObjectFactory<MiloProperties.Config, OpcUaClient> factory, GenericKeyedObjectPoolConfig<OpcUaClient> config, AbandonedConfig abandonedConfig) {
        super(factory, config, abandonedConfig);
    }
}
