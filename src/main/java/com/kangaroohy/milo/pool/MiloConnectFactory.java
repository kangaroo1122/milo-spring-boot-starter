package com.kangaroohy.milo.pool;

import com.kangaroohy.milo.configuration.MiloProperties;
import com.kangaroohy.milo.exception.EndPointNotFoundException;
import com.kangaroohy.milo.exception.IdentityNotFoundException;
import com.kangaroohy.milo.utils.KeyStoreLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;

/**
 * 类 MiloConnectFactory 功能描述：<br/>
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2023/5/4 18:56
 */
@Slf4j
public class MiloConnectFactory implements PooledObjectFactory<OpcUaClient> {

    private final MiloProperties properties;

    public MiloConnectFactory(MiloProperties properties) {
        if (properties.getEndpoint() == null || "".equals(properties.getEndpoint())) {
            throw new EndPointNotFoundException("请配置OPC UA地址信息");
        }
        this.properties = properties;
    }

    /**
     * 创建对象
     *
     * @return
     * @throws Exception
     */
    @Override
    public PooledObject<OpcUaClient> makeObject() throws Exception {
        OpcUaClient client = createClient();
        client.connect().get();
        return new DefaultPooledObject<>(client);
    }

    /**
     * 对象要被销毁时(validateObject方法返回false或者超时)后被调用
     *
     * @param pooledObject
     * @throws Exception
     */
    @Override
    public void destroyObject(PooledObject<OpcUaClient> pooledObject) throws Exception {
        pooledObject.getObject().disconnect().get();
    }

    /**
     * 每次获取对象和还回对象时会被调用，如果返回false会销毁对象
     */
    @Override
    public boolean validateObject(PooledObject<OpcUaClient> pooledObject) {
        return true;
    }

    /**
     * 调用获取对象方法前被调用
     * 此方法一般进行一些前置操作
     */
    @Override
    public void activateObject(PooledObject<OpcUaClient> pooledObject) throws Exception {

    }

    /**
     * 当还回对象并且validateObject方法返回true后被调用
     * 一般在此方法中对刚刚使用完成的对象进行重置
     */
    @Override
    public void passivateObject(PooledObject<OpcUaClient> pooledObject) throws Exception {

    }

    private OpcUaClient createClient() throws Exception {
        KeyStoreLoader loader = new KeyStoreLoader().load();

        return OpcUaClient.create(
                this.endpointUrl(),
                endpoints ->
                        endpoints.stream()
//                                .filter(e -> securityPolicy().getUri().equals(e.getSecurityPolicyUri()))
                                .findFirst(),
                configBuilder ->
                        configBuilder
                                .setApplicationName(LocalizedText.english("milo opc-ua client"))
                                .setApplicationUri("urn:coctrl:milo:client")
                                .setCertificate(loader.getClientCertificate())
                                .setKeyPair(loader.getClientKeyPair())
                                .setIdentityProvider(this.identityProvider())
                                .setRequestTimeout(Unsigned.uint(5000))
                                .build()
        );
    }

    private String endpointUrl() {
        return properties.getEndpoint();
    }

    private SecurityPolicy securityPolicy() {
        return properties.getSecurityPolicy();
    }

    private IdentityProvider identityProvider() {
        if (properties.getSecurityPolicy().equals(SecurityPolicy.None)) {
            return new AnonymousProvider();
        }
        if (properties.getUsername() == null || properties.getPassword() == null) {
            throw new IdentityNotFoundException("连接信息未完善");
        } else {
            return new UsernameProvider(properties.getUsername(), properties.getPassword());
        }
    }
}
