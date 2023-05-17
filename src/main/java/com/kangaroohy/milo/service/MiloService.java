package com.kangaroohy.milo.service;

import com.kangaroohy.milo.model.ReadWriteEntity;
import com.kangaroohy.milo.model.WriteEntity;
import com.kangaroohy.milo.pool.MiloConnectPool;
import com.kangaroohy.milo.runner.*;
import com.kangaroohy.milo.runner.subscription.SubscriptionCallback;
import com.kangaroohy.milo.runner.subscription.SubscriptionRunner;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    public MiloService(MiloConnectPool connectPool) {
        this.connectPool = connectPool;
    }

    /**
     * 遍历OPC UA服务器根节点
     *
     * @return 根节点列表
     */
    public List<String> browseRoot() throws Exception {
        BrowseRunner runner = new BrowseRunner();
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                return runner.run(client);
            } finally {
                connectPool.returnObject(client);
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
    public List<String> browseNode(String browseRoot) throws Exception {
        BrowseNodeRunner runner = new BrowseNodeRunner(browseRoot);
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                return runner.run(client);
            } finally {
                connectPool.returnObject(client);
            }
        }
        return new ArrayList<>();
    }

    /**
     * 指定类型 写入kep点位值
     *
     * @param entity 待写入数据
     */
    public void writeSpecifyType(WriteEntity entity) throws Exception {
        WriteValueRunner runner = new WriteValueRunner(entity);
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(client);
            }
        }
    }

    /**
     * 指定类型 写入kep点位值，可批量写入不同类型的值
     *
     * @param entities 待写入数据
     */
    public void writeSpecifyType(List<WriteEntity> entities) throws Exception {
        WriteValuesRunner runner = new WriteValuesRunner(entities);
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(client);
            }
        }
    }

    /**
     * 写入kep点位值
     *
     * @param entity 待写入数据
     */
    public boolean writeToOpcUa(ReadWriteEntity entity) throws Exception {
        WriteValueRunner runner = new WriteValueRunner(WriteEntity.builder()
                .identifier(entity.getIdentifier())
                .variant(new Variant(entity.getValue()))
                .build());
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                return runner.run(client);
            } finally {
                connectPool.returnObject(client);
            }
        }
        return false;
    }

    /**
     * 写入kep点位值
     *
     * @param entities 待写入数据
     */
    public void writeToOpcUa(List<ReadWriteEntity> entities) throws Exception {
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
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(client);
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
        WriteValueRunner runner = new WriteValueRunner(WriteEntity.builder()
                .identifier(entity.getIdentifier())
                .variant(new Variant(((Integer) entity.getValue()).byteValue()))
                .build());
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(client);
            }
        }
    }

    /**
     * kepware 数据类型为：Char<br/>
     * 8位带符号整数
     *
     * @param entities 待写入数据
     */
    public void writeToOpcChar(List<ReadWriteEntity> entities) throws Exception {
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
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(client);
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
        WriteValueRunner runner = new WriteValueRunner(WriteEntity.builder()
                .identifier(entity.getIdentifier())
                .variant(new Variant(Unsigned.ubyte((Integer) entity.getValue())))
                .build());
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(client);
            }
        }
    }

    /**
     * kepware 数据类型为：Byte<br/>
     * 8位无符号整数
     *
     * @param entities 待写入数据
     */
    public void writeToOpcByte(List<ReadWriteEntity> entities) throws Exception {
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
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(client);
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
        WriteValueRunner runner = new WriteValueRunner(WriteEntity.builder()
                .identifier(entity.getIdentifier())
                .variant(new Variant(((Integer) entity.getValue()).shortValue()))
                .build());
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(client);
            }
        }
    }

    /**
     * kepware 数据类型为：Short<br/>
     * 16位带符号整数
     *
     * @param entities 待写入数据
     */
    public void writeToOpcShort(List<ReadWriteEntity> entities) throws Exception {
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
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(client);
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
        WriteValueRunner runner = new WriteValueRunner(WriteEntity.builder()
                .identifier(entity.getIdentifier())
                .variant(new Variant(Unsigned.ushort((Integer) entity.getValue())))
                .build());
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(client);
            }
        }
    }

    /**
     * kepware 数据类型为：Word<br/>
     * 16位无符号整数
     *
     * @param entities 待写入数据
     */
    public void writeToOpcWord(List<ReadWriteEntity> entities) throws Exception {
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
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                connectPool.returnObject(client);
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
    public List<ReadWriteEntity> readFromOpcUa(List<String> ids) throws Exception {
        ReadValuesRunner runner = new ReadValuesRunner(ids);
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                return runner.run(client);
            } finally {
                connectPool.returnObject(client);
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
     * @param samplingInterval 订阅时间间隔 默认1000 ms
     * @return
     */
    public void subscriptionFromOpcUa(List<String> ids, double samplingInterval, SubscriptionCallback callback) throws Exception {
        SubscriptionRunner runner = new SubscriptionRunner(ids, samplingInterval);
        OpcUaClient client = connectPool.borrowObject();
        if (client != null) {
            try {
                runner.run(client, callback);
            } finally {
                connectPool.returnObject(client);
            }
        }
    }

}
