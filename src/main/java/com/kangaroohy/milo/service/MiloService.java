package com.kangaroohy.milo.service;

import com.kangaroohy.milo.configuration.MiloProperties;
import com.kangaroohy.milo.model.ReadWriteEntity;
import com.kangaroohy.milo.model.WriteEntity;
import com.kangaroohy.milo.pool.MiloConnectPool;
import com.kangaroohy.milo.runner.BrowseNodeRunner;
import com.kangaroohy.milo.runner.BrowseRunner;
import com.kangaroohy.milo.runner.ReadValuesRunner;
import com.kangaroohy.milo.runner.WriteValuesRunner;
import com.kangaroohy.milo.runner.subscription.SubscriptionCallback;
import com.kangaroohy.milo.runner.subscription.SubscriptionRunner;
import com.kangaroohy.milo.utils.CustomUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author kangaroo hy
 * @date 2020/4/25
 * @desc milo-spring-boot-starter
 * @since 0.0.1
 */
@Service
@Slf4j
public class MiloService {
    private final MiloConnectPool connectPool;
    private final MiloProperties properties;

    public MiloService(MiloConnectPool connectPool, MiloProperties properties) {
        this.connectPool = connectPool;
        this.properties = properties;
    }

    /**
     * 遍历OPC UA服务器根节点
     *
     * @return 根节点列表
     */
    public List<String> browseRoot() throws Exception {
        return browseRoot(null);
    }

    /**
     * 遍历OPC UA服务器根节点
     *
     * @param clientName 配置key
     * @return 根节点列表
     */
    public List<String> browseRoot(String clientName) throws Exception {
        MiloProperties.Config config = CustomUtil.getConfig(properties, clientName);
        BrowseRunner runner = new BrowseRunner();
        OpcUaClient client = connectPool.borrowObject(config);
        if (client != null) {
            try {
                return runner.run(client);
            } finally {
                connectPool.returnObject(config, client);
            }
        }
        return Collections.emptyList();
    }

    /**
     * 遍历OPC UA服务器指定节点
     *
     * @param browseRoot 节点名称
     * @return 指定节点 tag列表
     */
    public List<String> browseNode(String browseRoot) throws Exception {
        return browseNode(browseRoot, null);
    }

    /**
     * 遍历OPC UA服务器指定节点
     *
     * @param browseRoot 节点名称
     * @param clientName 配置key
     * @return 指定节点 tag列表
     */
    public List<String> browseNode(String browseRoot, String clientName) throws Exception {
        MiloProperties.Config config = CustomUtil.getConfig(properties, clientName);
        BrowseNodeRunner runner = new BrowseNodeRunner(browseRoot);
        OpcUaClient client = connectPool.borrowObject(config);
        if (client != null) {
            try {
                return runner.run(client);
            } finally {
                connectPool.returnObject(config, client);
            }
        }
        return Collections.emptyList();
    }

    /**
     * 指定类型 写入kep点位值
     *
     * @param entity 待写入数据
     */
    public void writeSpecifyType(WriteEntity entity) throws Exception {
        writeSpecifyType(Collections.singletonList(entity));
    }

    /**
     * 指定类型 写入kep点位值
     *
     * @param entity     待写入数据
     * @param clientName 配置key
     */
    public void writeSpecifyType(WriteEntity entity, String clientName) throws Exception {
        writeSpecifyType(Collections.singletonList(entity), clientName);
    }

    /**
     * 指定类型 写入kep点位值，可批量写入不同类型的值
     *
     * @param entities 待写入数据
     */
    public void writeSpecifyType(List<WriteEntity> entities) throws Exception {
        writeSpecifyType(entities, null);
    }

    /**
     * 指定类型 写入kep点位值，可批量写入不同类型的值
     *
     * @param entities   待写入数据
     * @param clientName 配置key
     */
    public void writeSpecifyType(List<WriteEntity> entities, String clientName) throws Exception {
        MiloProperties.Config config = CustomUtil.getConfig(properties, clientName);
        WriteValuesRunner runner = new WriteValuesRunner(entities);
        OpcUaClient client = connectPool.borrowObject(config);
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(config, client);
            }
        }
    }

    /**
     * 写入kep点位值
     *
     * @param entity 待写入数据
     */
    public void writeToOpcUa(ReadWriteEntity entity) throws Exception {
        writeToOpcUa(Collections.singletonList(entity));
    }

    /**
     * 写入kep点位值
     *
     * @param entity     待写入数据
     * @param clientName 配置key
     */
    public void writeToOpcUa(ReadWriteEntity entity, String clientName) throws Exception {
        writeToOpcUa(Collections.singletonList(entity), clientName);
    }

    /**
     * 写入kep点位值
     *
     * @param entities 待写入数据
     */
    public void writeToOpcUa(List<ReadWriteEntity> entities) throws Exception {
        writeToOpcUa(entities, null);
    }

    /**
     * 写入kep点位值
     *
     * @param entities   待写入数据
     * @param clientName 配置key
     */
    public void writeToOpcUa(List<ReadWriteEntity> entities, String clientName) throws Exception {
        MiloProperties.Config config = CustomUtil.getConfig(properties, clientName);
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
        OpcUaClient client = connectPool.borrowObject(config);
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(config, client);
            }
        }
    }

    /**
     * kepware 数据类型为：Char<br/>
     * 8位带符号整数
     *
     * @param entity 待写入数据
     */
    public void writeToOpcChar(ReadWriteEntity entity) throws Exception {
        writeToOpcChar(Collections.singletonList(entity));
    }

    /**
     * kepware 数据类型为：Char<br/>
     * 8位带符号整数
     *
     * @param entity     待写入数据
     * @param clientName 配置key
     */
    public void writeToOpcChar(ReadWriteEntity entity, String clientName) throws Exception {
        writeToOpcChar(Collections.singletonList(entity), clientName);
    }

    /**
     * kepware 数据类型为：Char<br/>
     * 8位带符号整数
     *
     * @param entities 待写入数据
     */
    public void writeToOpcChar(List<ReadWriteEntity> entities) throws Exception {
        writeToOpcChar(entities, null);
    }

    /**
     * kepware 数据类型为：Char<br/>
     * 8位带符号整数
     *
     * @param entities   待写入数据
     * @param clientName 配置key
     */
    public void writeToOpcChar(List<ReadWriteEntity> entities, String clientName) throws Exception {
        MiloProperties.Config config = CustomUtil.getConfig(properties, clientName);
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
        OpcUaClient client = connectPool.borrowObject(config);
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(config, client);
            }
        }
    }

    /**
     * kepware 数据类型为：Byte<br/>
     * 8位无符号整数
     *
     * @param entity 待写入数据
     */
    public void writeToOpcByte(ReadWriteEntity entity) throws Exception {
        writeToOpcByte(Collections.singletonList(entity));
    }

    /**
     * kepware 数据类型为：Byte<br/>
     * 8位无符号整数
     *
     * @param entity     待写入数据
     * @param clientName 配置key
     */
    public void writeToOpcByte(ReadWriteEntity entity, String clientName) throws Exception {
        writeToOpcByte(Collections.singletonList(entity), clientName);
    }

    /**
     * kepware 数据类型为：Byte<br/>
     * 8位无符号整数
     *
     * @param entities 待写入数据
     */
    public void writeToOpcByte(List<ReadWriteEntity> entities) throws Exception {
        writeToOpcByte(entities, null);
    }

    /**
     * kepware 数据类型为：Byte<br/>
     * 8位无符号整数
     *
     * @param entities   待写入数据
     * @param clientName 配置key
     */
    public void writeToOpcByte(List<ReadWriteEntity> entities, String clientName) throws Exception {
        MiloProperties.Config config = CustomUtil.getConfig(properties, clientName);
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
        OpcUaClient client = connectPool.borrowObject(config);
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(config, client);
            }
        }
    }

    /**
     * kepware 数据类型为：Short<br/>
     * 16位带符号整数
     *
     * @param entity 待写入数据
     */
    public void writeToOpcShort(ReadWriteEntity entity) throws Exception {
        writeToOpcShort(Collections.singletonList(entity));
    }

    /**
     * kepware 数据类型为：Short<br/>
     * 16位带符号整数
     *
     * @param entity     待写入数据
     * @param clientName 配置key
     */
    public void writeToOpcShort(ReadWriteEntity entity, String clientName) throws Exception {
        writeToOpcShort(Collections.singletonList(entity), clientName);
    }

    /**
     * kepware 数据类型为：Short<br/>
     * 16位带符号整数
     *
     * @param entities 待写入数据
     */
    public void writeToOpcShort(List<ReadWriteEntity> entities) throws Exception {
        writeToOpcShort(entities, null);
    }

    /**
     * kepware 数据类型为：Short<br/>
     * 16位带符号整数
     *
     * @param entities   待写入数据
     * @param clientName 配置key
     */
    public void writeToOpcShort(List<ReadWriteEntity> entities, String clientName) throws Exception {
        MiloProperties.Config config = CustomUtil.getConfig(properties, clientName);
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
        OpcUaClient client = connectPool.borrowObject(config);
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(config, client);
            }
        }
    }

    /**
     * kepware 数据类型为：Word<br/>
     * 16位无符号整数
     *
     * @param entity 待写入数据
     */
    public void writeToOpcWord(ReadWriteEntity entity) throws Exception {
        writeToOpcWord(Collections.singletonList(entity));
    }

    /**
     * kepware 数据类型为：Word<br/>
     * 16位无符号整数
     *
     * @param entity     待写入数据
     * @param clientName 配置key
     */
    public void writeToOpcWord(ReadWriteEntity entity, String clientName) throws Exception {
        writeToOpcWord(Collections.singletonList(entity), clientName);
    }

    /**
     * kepware 数据类型为：Word<br/>
     * 16位无符号整数
     *
     * @param entities 待写入数据
     */
    public void writeToOpcWord(List<ReadWriteEntity> entities) throws Exception {
        writeToOpcWord(entities, null);
    }

    /**
     * kepware 数据类型为：Word<br/>
     * 16位无符号整数
     *
     * @param entities   待写入数据
     * @param clientName 配置key
     */
    public void writeToOpcWord(List<ReadWriteEntity> entities, String clientName) throws Exception {
        MiloProperties.Config config = CustomUtil.getConfig(properties, clientName);
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
        OpcUaClient client = connectPool.borrowObject(config);
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(config, client);
            }
        }
    }

    /**
     * 读取kep点位值
     *
     * @param id 点位id
     * @return
     */
    public ReadWriteEntity readFromOpcUa(String id) throws Exception {
        return readFromOpcUa(id, null);
    }

    /**
     * 读取kep点位值
     *
     * @param id         点位id
     * @param clientName 配置key
     * @return
     */
    public ReadWriteEntity readFromOpcUa(String id, String clientName) throws Exception {
        List<ReadWriteEntity> entityList = readFromOpcUa(Collections.singletonList(id), clientName);
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
    public List<ReadWriteEntity> readFromOpcUa(List<String> ids) throws Exception {
        return readFromOpcUa(ids, null);
    }

    /**
     * 读取kep点位值
     *
     * @param ids        点位id数组
     * @param clientName 配置key
     * @return
     */
    public List<ReadWriteEntity> readFromOpcUa(List<String> ids, String clientName) throws Exception {
        MiloProperties.Config config = CustomUtil.getConfig(properties, clientName);
        ReadValuesRunner runner = new ReadValuesRunner(ids);
        OpcUaClient client = connectPool.borrowObject(config);
        if (client != null) {
            try {
                return runner.run(client);
            } finally {
                connectPool.returnObject(config, client);
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
    public void subscriptionFromOpcUa(List<String> ids, SubscriptionCallback callback) throws Exception {
        subscriptionFromOpcUa(ids, 1000.0, callback);
    }

    /**
     * 订阅kep点位值
     *
     * @param ids 点位id数组
     * @param clientName 配置key
     * @return
     */
    public void subscriptionFromOpcUa(List<String> ids, String clientName, SubscriptionCallback callback) throws Exception {
        subscriptionFromOpcUa(ids, 1000.0, clientName, callback);
    }

    /**
     * 订阅kep点位值
     *
     * @param ids              点位id数组
     * @param samplingInterval 订阅时间间隔 默认1000 ms
     * @return
     */
    public void subscriptionFromOpcUa(List<String> ids, double samplingInterval, SubscriptionCallback callback) throws Exception {
        subscriptionFromOpcUa(ids, samplingInterval, null, callback);
    }

    /**
     * 订阅kep点位值
     *
     * @param ids              点位id数组
     * @param samplingInterval 订阅时间间隔 默认1000 ms
     * @param clientName 配置key
     * @return
     */
    public void subscriptionFromOpcUa(List<String> ids, double samplingInterval, String clientName, SubscriptionCallback callback) throws Exception {
        MiloProperties.Config config = CustomUtil.getConfig(properties, clientName);
        SubscriptionRunner runner = new SubscriptionRunner(ids, samplingInterval);
        OpcUaClient client = connectPool.borrowObject(config);
        if (client != null) {
            try {
                runner.run(client, callback);
            } finally {
                connectPool.returnObject(config, client);
            }
        }
    }

}
