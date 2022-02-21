package com.coctrl.milo.service;

import com.coctrl.milo.configuration.MiloProperties;
import com.coctrl.milo.exception.EndPointNotFoundException;
import com.coctrl.milo.exception.IdentityNotFoundException;
import com.coctrl.milo.model.ReadWriteEntity;
import com.coctrl.milo.model.SubscriptValues;
import com.coctrl.milo.model.WriteEntity;
import com.coctrl.milo.runner.*;
import com.coctrl.milo.runner.subscription.ManagedSubscriptionRunner;
import com.coctrl.milo.runner.subscription.SubscriptionRunner;
import com.coctrl.milo.utils.KeyStoreLoader;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author kangaroo hy
 * @date 2020/4/25
 * @desc milo-spring-boot-starter
 * @since 0.0.1
 */
@Service
@Slf4j
public class MiloService {
    private final MiloProperties properties;

    private Queue<OpcUaClient> queue = new ConcurrentLinkedQueue<>();

    public MiloService(MiloProperties properties) {
        if (properties.getEndpoint() == null || "".equals(properties.getEndpoint())) {
            throw new EndPointNotFoundException("请配置OPC UA地址信息");
        }
        this.properties = properties;
    }

    /**
     * 遍历OPC UA服务器根节点
     *
     * @return 根节点列表
     */
    public List<String> browseRoot() {
        BrowseRunner runner = new BrowseRunner();
        OpcUaClient client = connect();
        if (client != null) {
            try {
                return runner.run(client);
            } finally {
                disconnect(client);
            }
        }
        return new ArrayList<>();
    }

    /**
     * 遍历OPC UA服务器指定节点
     *
     * @param browseRoot 节点名称
     * @return 指定节点 tag列表
     */
    public List<String> browseNode(String browseRoot) {
        BrowseNodeRunner runner = new BrowseNodeRunner(browseRoot);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                return runner.run(client);
            } finally {
                disconnect(client);
            }
        }
        return new ArrayList<>();
    }

    /**
     * 指定类型 写入kep点位值
     *
     * @param entity 待写入数据
     */
    public void writeSpecifyType(WriteEntity entity) {
        WriteValueRunner runner = new WriteValueRunner(entity);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    /**
     * 指定类型 写入kep点位值，可批量写入不同类型的值
     *
     * @param entities 待写入数据
     */
    public void writeSpecifyType(List<WriteEntity> entities) {
        WriteValuesRunner runner = new WriteValuesRunner(entities);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    /**
     * 写入kep点位值
     *
     * @param entity 待写入数据
     */
    public boolean writeToOpcUa(ReadWriteEntity entity) {
        WriteValueRunner runner = new WriteValueRunner(WriteEntity.builder()
                .identifier(entity.getIdentifier())
                .variant(new Variant(entity.getValue()))
                .build());
        OpcUaClient client = connect();
        if (client != null) {
            try {
                return runner.run(client);
            } finally {
                disconnect(client);
            }
        }
        return false;
    }

    /**
     * 写入kep点位值
     *
     * @param entities 待写入数据
     */
    public void writeToOpcUa(List<ReadWriteEntity> entities) {
        List<WriteEntity> writeEntityList = new ArrayList<>();
        if (!entities.isEmpty()) {
            for (ReadWriteEntity entity : entities) {
                writeEntityList.add(WriteEntity.builder()
                        .identifier(entity.getIdentifier())
                        .variant(new Variant(entity.getValue()))
                        .build());
            }
        }
        WriteValuesRunner runner = new WriteValuesRunner(writeEntityList);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    /**
     * kepware 数据类型为：Char<br/>
     * 8位带符号整数
     *
     * @param entity 待写入数据
     */
    public void writeToOpcChar(ReadWriteEntity entity) {
        WriteValueRunner runner = new WriteValueRunner(WriteEntity.builder()
                .identifier(entity.getIdentifier())
                .variant(new Variant(((Integer) entity.getValue()).byteValue()))
                .build());
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    /**
     * kepware 数据类型为：Char<br/>
     * 8位带符号整数
     *
     * @param entities 待写入数据
     */
    public void writeToOpcChar(List<ReadWriteEntity> entities) {
        List<WriteEntity> writeEntityList = new ArrayList<>();
        if (!entities.isEmpty()) {
            for (ReadWriteEntity entity : entities) {
                writeEntityList.add(WriteEntity.builder()
                        .identifier(entity.getIdentifier())
                        .variant(new Variant(((Integer) entity.getValue()).byteValue()))
                        .build());
            }
        }
        WriteValuesRunner runner = new WriteValuesRunner(writeEntityList);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    /**
     * kepware 数据类型为：Byte<br/>
     * 8位无符号整数
     *
     * @param entity 待写入数据
     */
    public void writeToOpcByte(ReadWriteEntity entity) {
        WriteValueRunner runner = new WriteValueRunner(WriteEntity.builder()
                .identifier(entity.getIdentifier())
                .variant(new Variant(Unsigned.ubyte((Integer) entity.getValue())))
                .build());
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    /**
     * kepware 数据类型为：Byte<br/>
     * 8位无符号整数
     *
     * @param entities 待写入数据
     */
    public void writeToOpcByte(List<ReadWriteEntity> entities) {
        List<WriteEntity> writeEntityList = new ArrayList<>();
        if (!entities.isEmpty()) {
            for (ReadWriteEntity entity : entities) {
                writeEntityList.add(WriteEntity.builder()
                        .identifier(entity.getIdentifier())
                        .variant(new Variant(Unsigned.ubyte((Integer) entity.getValue())))
                        .build());
            }
        }
        WriteValuesRunner runner = new WriteValuesRunner(writeEntityList);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    /**
     * kepware 数据类型为：Short<br/>
     * 16位带符号整数
     *
     * @param entity 待写入数据
     */
    public void writeToOpcShort(ReadWriteEntity entity) {
        WriteValueRunner runner = new WriteValueRunner(WriteEntity.builder()
                .identifier(entity.getIdentifier())
                .variant(new Variant(((Integer) entity.getValue()).shortValue()))
                .build());
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    /**
     * kepware 数据类型为：Short<br/>
     * 16位带符号整数
     *
     * @param entities 待写入数据
     */
    public void writeToOpcShort(List<ReadWriteEntity> entities) {
        List<WriteEntity> writeEntityList = new ArrayList<>();
        if (!entities.isEmpty()) {
            for (ReadWriteEntity entity : entities) {
                writeEntityList.add(WriteEntity.builder()
                        .identifier(entity.getIdentifier())
                        .variant(new Variant(((Integer) entity.getValue()).shortValue()))
                        .build());
            }
        }
        WriteValuesRunner runner = new WriteValuesRunner(writeEntityList);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    /**
     * kepware 数据类型为：Word<br/>
     * 16位无符号整数
     *
     * @param entity 待写入数据
     */
    public void writeToOpcWord(ReadWriteEntity entity) {
        WriteValueRunner runner = new WriteValueRunner(WriteEntity.builder()
                .identifier(entity.getIdentifier())
                .variant(new Variant(Unsigned.ushort((Integer) entity.getValue())))
                .build());
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    /**
     * kepware 数据类型为：Word<br/>
     * 16位无符号整数
     *
     * @param entities 待写入数据
     */
    public void writeToOpcWord(List<ReadWriteEntity> entities) {
        List<WriteEntity> writeEntityList = new ArrayList<>();
        if (!entities.isEmpty()) {
            for (ReadWriteEntity entity : entities) {
                writeEntityList.add(WriteEntity.builder()
                        .identifier(entity.getIdentifier())
                        .variant(new Variant(Unsigned.ushort((Integer) entity.getValue())))
                        .build());
            }
        }
        WriteValuesRunner runner = new WriteValuesRunner(writeEntityList);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    /**
     * 读取kep点位值
     *
     * @param id 点位id
     * @return
     */
    public ReadWriteEntity readFromOpcUa(String id) {
        List<String> ids = new ArrayList<>();
        ids.add(id);
        List<ReadWriteEntity> entityList = readFromOpcUa(ids);
        if (!entityList.isEmpty()) {
            return entityList.get(0);
        }
        return null;
    }

    /**
     * 读取kep点位值
     *
     * @param ids 点位id数组
     * @return
     */
    public List<ReadWriteEntity> readFromOpcUa(List<String> ids) {
        ReadValuesRunner runner = new ReadValuesRunner(ids);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                return runner.run(client);
            } finally {
                disconnect(client);
            }
        }
        return new ArrayList<>();
    }

    /**
     * 订阅kep点位值
     *
     * @param ids 点位id数组
     * @return
     */
    public void subscriptionFromOpcUa(List<String> ids) {
        SubscriptionRunner runner = new SubscriptionRunner(ids);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    /**
     * 订阅kep点位值
     *
     * @param ids 点位id数组
     * @return
     */
    public void managedSubscriptionFromOpcUa(List<String> ids) {
        ManagedSubscriptionRunner runner = new ManagedSubscriptionRunner(ids);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    public Map<String, Object> readSubscriptionValues() {
        return SubscriptValues.getSubscriptValues();
    }

    public Object readSubscriptionValues(String id) {
        if (SubscriptValues.getSubscriptValues().containsKey(id)) {
            return SubscriptValues.getSubscriptValues().get(id);
        }
        return null;
    }

    private OpcUaClient connect() {
        OpcUaClient client = queue.poll();
        if (client != null) {
            return client;
        }
        try {
            client = createClient();
            client.connect().get();
            queue.add(client);
            return client;
        } catch (Exception e) {
            log.error("OpcUaClient create error: ", e);
        }
        return null;
    }

    void disconnect(OpcUaClient client) {
        queue.add(client);
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
