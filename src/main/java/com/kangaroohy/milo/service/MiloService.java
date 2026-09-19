package com.kangaroohy.milo.service;

import com.kangaroohy.milo.configuration.MiloProperties;
import com.kangaroohy.milo.model.ReadWriteEntity;
import com.kangaroohy.milo.model.WriteEntity;
import com.kangaroohy.milo.runner.BrowseNodeRunner;
import com.kangaroohy.milo.runner.BrowseRunner;
import com.kangaroohy.milo.runner.ReadValuesRunner;
import com.kangaroohy.milo.runner.WriteValuesRunner;
import com.kangaroohy.milo.runner.subscription.SubscriptionCallback;
import com.kangaroohy.milo.runner.subscription.SubscriptionHandle;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * OPC UA 业务服务入口，提供浏览、读值、写值和订阅能力。
 *
 * <p>客户端连接由 {@link MiloClientManager} 统一管理，订阅由
 * {@link MiloSubscriptionManager} 统一复用和维护。</p>
 *
 * @author kangaroo hy
 * @date 2020/4/25
 * @desc milo-spring-boot-starter
 * @since 0.0.1
 */
public class MiloService {

    private final MiloClientManager clients;
    private final MiloSubscriptionManager subscriptions;
    private final MiloProperties properties;

    public MiloService(MiloClientManager clients,
                       MiloSubscriptionManager subscriptions,
                       MiloProperties properties) {
        this.clients = clients;
        this.subscriptions = subscriptions;
        this.properties = properties;
    }

    /**
     * 遍历 OPC UA 服务器根节点。
     *
     * @return 根节点列表
     */
    public List<String> browseRoot() throws Exception {
        return browseRoot(null);
    }

    /**
     * 使用指定 endpoint 遍历 OPC UA 服务器根节点。
     *
     * @param clientName 配置 key，为 null 时使用 primary
     * @return 根节点列表
     */
    public List<String> browseRoot(String clientName) throws Exception {
        return withClient(clientName, client -> new BrowseRunner().run(client));
    }

    /**
     * 遍历指定节点下的叶子节点。
     *
     * @param browseRoot 节点标识
     * @return 节点列表
     */
    public List<String> browseNode(String browseRoot) throws Exception {
        return browseNode(browseRoot, null);
    }

    /**
     * 使用指定 endpoint 遍历指定节点下的叶子节点。
     *
     * @param browseRoot 节点标识
     * @param clientName 配置 key，为 null 时使用 primary
     * @return 节点列表
     */
    public List<String> browseNode(String browseRoot, String clientName) throws Exception {
        return withClient(clientName, client -> new BrowseNodeRunner(browseRoot).run(client));
    }

    /**
     * 按调用方指定的数据类型写入 Kepware 点位。
     *
     * @param entity 待写入数据
     */
    public void writeSpecifyType(WriteEntity entity) throws Exception {
        writeSpecifyType(Collections.singletonList(entity));
    }

    /**
     * 使用指定 endpoint 按调用方指定的数据类型写入点位。
     *
     * @param entity 待写入数据
     * @param clientName 配置 key，为 null 时使用 primary
     */
    public void writeSpecifyType(WriteEntity entity, String clientName) throws Exception {
        writeSpecifyType(Collections.singletonList(entity), clientName);
    }

    /**
     * 批量写入不同类型的点位。
     *
     * @param entities 待写入数据
     */
    public void writeSpecifyType(List<WriteEntity> entities) throws Exception {
        writeSpecifyType(entities, null);
    }

    /**
     * 使用指定 endpoint 批量写入不同类型的点位。
     *
     * @param entities 待写入数据
     * @param clientName 配置 key，为 null 时使用 primary
     */
    public void writeSpecifyType(List<WriteEntity> entities, String clientName) throws Exception {
        List<WriteEntity> safeEntities = entities == null ? Collections.emptyList() : entities;
        withClient(clientName, client -> {
            new WriteValuesRunner(safeEntities, properties.getWriteBatchSize(), properties.getRequestTimeout()).run(client);
            return null;
        });
    }

    /**
     * 使用通用 Variant 类型写入点位。
     *
     * @param entity 待写入数据
     */
    public void writeToOpcUa(ReadWriteEntity entity) throws Exception {
        writeToOpcUa(Collections.singletonList(entity));
    }

    /**
     * 使用指定 endpoint 和通用 Variant 类型写入点位。
     *
     * @param entity 待写入数据
     * @param clientName 配置 key，为 null 时使用 primary
     */
    public void writeToOpcUa(ReadWriteEntity entity, String clientName) throws Exception {
        writeToOpcUa(Collections.singletonList(entity), clientName);
    }

    /**
     * 批量使用通用 Variant 类型写入点位。
     *
     * @param entities 待写入数据
     */
    public void writeToOpcUa(List<ReadWriteEntity> entities) throws Exception {
        writeToOpcUa(entities, null);
    }

    /**
     * 使用指定 endpoint 批量写入通用 Variant 类型点位。
     *
     * @param entities 待写入数据
     * @param clientName 配置 key，为 null 时使用 primary
     */
    public void writeToOpcUa(List<ReadWriteEntity> entities, String clientName) throws Exception {
        List<WriteEntity> writes = new ArrayList<>();
        if (entities != null) {
            for (ReadWriteEntity entity : entities) {
                if (entity == null) {
                    throw new IllegalArgumentException("写入实体不能为空");
                }
                writes.add(WriteEntity.builder()
                        .identifier(entity.getIdentifier())
                        .variant(new Variant(entity.getValue()))
                        .build());
            }
        }
        writeSpecifyType(writes, clientName);
    }

    /**
     * Kepware Char 类型：8 位带符号整数。
     *
     * @param entity 待写入数据
     */
    public void writeToOpcChar(ReadWriteEntity entity) throws Exception {
        writeToOpcChar(Collections.singletonList(entity));
    }

    /**
     * 使用指定 endpoint 写入 Kepware Char 类型（8 位带符号整数）。
     *
     * @param entity 待写入数据
     * @param clientName 配置 key，为 null 时使用 primary
     */
    public void writeToOpcChar(ReadWriteEntity entity, String clientName) throws Exception {
        writeToOpcChar(Collections.singletonList(entity), clientName);
    }

    /**
     * 批量写入 Kepware Char 类型（8 位带符号整数）。
     *
     * @param entities 待写入数据
     */
    public void writeToOpcChar(List<ReadWriteEntity> entities) throws Exception {
        writeToOpcChar(entities, null);
    }

    /**
     * 使用指定 endpoint 批量写入 Kepware Char 类型。
     *
     * @param entities 待写入数据
     * @param clientName 配置 key，为 null 时使用 primary
     */
    public void writeToOpcChar(List<ReadWriteEntity> entities, String clientName) throws Exception {
        writeConverted(entities, clientName, value -> ((Number) value).byteValue());
    }

    /**
     * Kepware Byte 类型：8 位无符号整数。
     *
     * @param entity 待写入数据
     */
    public void writeToOpcByte(ReadWriteEntity entity) throws Exception {
        writeToOpcByte(Collections.singletonList(entity));
    }

    /**
     * 使用指定 endpoint 写入 Kepware Byte 类型（8 位无符号整数）。
     *
     * @param entity 待写入数据
     * @param clientName 配置 key，为 null 时使用 primary
     */
    public void writeToOpcByte(ReadWriteEntity entity, String clientName) throws Exception {
        writeToOpcByte(Collections.singletonList(entity), clientName);
    }

    /**
     * 批量写入 Kepware Byte 类型（8 位无符号整数）。
     *
     * @param entities 待写入数据
     */
    public void writeToOpcByte(List<ReadWriteEntity> entities) throws Exception {
        writeToOpcByte(entities, null);
    }

    /**
     * 使用指定 endpoint 批量写入 Kepware Byte 类型。
     *
     * @param entities 待写入数据
     * @param clientName 配置 key，为 null 时使用 primary
     */
    public void writeToOpcByte(List<ReadWriteEntity> entities, String clientName) throws Exception {
        writeConverted(entities, clientName, value -> Unsigned.ubyte(((Number) value).intValue()));
    }

    /**
     * Kepware Short 类型：16 位带符号整数。
     *
     * @param entity 待写入数据
     */
    public void writeToOpcShort(ReadWriteEntity entity) throws Exception {
        writeToOpcShort(Collections.singletonList(entity));
    }

    /**
     * 使用指定 endpoint 写入 Kepware Short 类型（16 位带符号整数）。
     *
     * @param entity 待写入数据
     * @param clientName 配置 key，为 null 时使用 primary
     */
    public void writeToOpcShort(ReadWriteEntity entity, String clientName) throws Exception {
        writeToOpcShort(Collections.singletonList(entity), clientName);
    }

    /**
     * 批量写入 Kepware Short 类型（16 位带符号整数）。
     *
     * @param entities 待写入数据
     */
    public void writeToOpcShort(List<ReadWriteEntity> entities) throws Exception {
        writeToOpcShort(entities, null);
    }

    /**
     * 使用指定 endpoint 批量写入 Kepware Short 类型。
     *
     * @param entities 待写入数据
     * @param clientName 配置 key，为 null 时使用 primary
     */
    public void writeToOpcShort(List<ReadWriteEntity> entities, String clientName) throws Exception {
        writeConverted(entities, clientName, value -> ((Number) value).shortValue());
    }

    /**
     * Kepware Word 类型：16 位无符号整数。
     *
     * @param entity 待写入数据
     */
    public void writeToOpcWord(ReadWriteEntity entity) throws Exception {
        writeToOpcWord(Collections.singletonList(entity));
    }

    /**
     * 使用指定 endpoint 写入 Kepware Word 类型（16 位无符号整数）。
     *
     * @param entity 待写入数据
     * @param clientName 配置 key，为 null 时使用 primary
     */
    public void writeToOpcWord(ReadWriteEntity entity, String clientName) throws Exception {
        writeToOpcWord(Collections.singletonList(entity), clientName);
    }

    /**
     * 批量写入 Kepware Word 类型（16 位无符号整数）。
     *
     * @param entities 待写入数据
     */
    public void writeToOpcWord(List<ReadWriteEntity> entities) throws Exception {
        writeToOpcWord(entities, null);
    }

    /**
     * 使用指定 endpoint 批量写入 Kepware Word 类型。
     *
     * @param entities 待写入数据
     * @param clientName 配置 key，为 null 时使用 primary
     */
    public void writeToOpcWord(List<ReadWriteEntity> entities, String clientName) throws Exception {
        writeConverted(entities, clientName, value -> Unsigned.ushort(((Number) value).intValue()));
    }

    /**
     * 读取单个 Kepware 点位值。
     *
     * @param id 点位标识
     * @return 点位值
     */
    public ReadWriteEntity readFromOpcUa(String id) throws Exception {
        return readFromOpcUa(id, 10000.0);
    }

    /**
     * 读取单个点位值。
     *
     * @param id 点位标识
     * @param maxAge 服务端允许使用的缓存年龄，单位毫秒
     * @return 点位值
     */
    public ReadWriteEntity readFromOpcUa(String id, double maxAge) throws Exception {
        return readFromOpcUa(id, maxAge, null);
    }

    /**
     * 使用指定 endpoint 读取单个点位值。
     *
     * @param id 点位标识
     * @param clientName 配置 key，为 null 时使用 primary
     * @return 点位值
     */
    public ReadWriteEntity readFromOpcUa(String id, String clientName) throws Exception {
        return readFromOpcUa(id, 10000.0, clientName);
    }

    /**
     * 使用指定 endpoint 读取单个点位值。
     *
     * @param id 点位标识
     * @param maxAge 服务端允许使用的缓存年龄，单位毫秒
     * @param clientName 配置 key，为 null 时使用 primary
     * @return 点位值
     */
    public ReadWriteEntity readFromOpcUa(String id, double maxAge, String clientName) throws Exception {
        List<ReadWriteEntity> values = readFromOpcUa(Collections.singletonList(id), maxAge, clientName);
        return values.isEmpty() ? null : values.get(0);
    }

    /**
     * 批量读取 Kepware 点位值。
     *
     * @param ids 点位标识列表
     * @return 点位值列表
     */
    public List<ReadWriteEntity> readFromOpcUa(List<String> ids) throws Exception {
        return readFromOpcUa(ids, 10000.0);
    }

    /**
     * 批量读取点位值，并指定服务端缓存最大年龄。
     *
     * @param ids 点位标识列表
     * @param maxAge 服务端允许使用的缓存年龄，单位毫秒
     * @return 点位值列表
     */
    public List<ReadWriteEntity> readFromOpcUa(List<String> ids, double maxAge) throws Exception {
        return readFromOpcUa(ids, maxAge, null);
    }

    /**
     * 使用指定 endpoint 批量读取点位值。
     *
     * @param ids 点位标识列表
     * @param clientName 配置 key，为 null 时使用 primary
     * @return 点位值列表
     */
    public List<ReadWriteEntity> readFromOpcUa(List<String> ids, String clientName) throws Exception {
        return readFromOpcUa(ids, 10000.0, clientName);
    }

    /**
     * 使用指定 endpoint 批量读取点位值。
     *
     * @param ids 点位标识列表
     * @param maxAge 服务端允许使用的缓存年龄，单位毫秒
     * @param clientName 配置 key，为 null 时使用 primary
     * @return 点位值列表
     */
    public List<ReadWriteEntity> readFromOpcUa(List<String> ids, double maxAge, String clientName) throws Exception {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return withClient(clientName, client -> new ReadValuesRunner(
                ids, maxAge, properties.getReadBatchSize(), properties.getRequestTimeout()).run(client));
    }

    /**
     * 订阅点位变化，默认发布/采样周期为 1000ms。
     *
     * @param ids 点位标识列表
     * @param callback 订阅回调
     * @return 可关闭的订阅句柄
     */
    public SubscriptionHandle subscriptionFromOpcUa(List<String> ids,
                                                     SubscriptionCallback callback) throws Exception {
        return subscriptionFromOpcUa(ids, 1000.0, null, callback);
    }

    /**
     * 使用指定 endpoint 订阅点位变化。
     *
     * @param ids 点位标识列表
     * @param clientName 配置 key，为 null 时使用 primary
     * @param callback 订阅回调
     * @return 可关闭的订阅句柄
     */
    public SubscriptionHandle subscriptionFromOpcUa(List<String> ids,
                                                     String clientName,
                                                     SubscriptionCallback callback) throws Exception {
        return subscriptionFromOpcUa(ids, 1000.0, clientName, callback);
    }

    /**
     * 订阅点位变化并指定发布周期。
     *
     * @param ids 点位标识列表
     * @param publishingInterval 发布周期，单位毫秒
     * @param callback 订阅回调
     * @return 可关闭的订阅句柄
     */
    public SubscriptionHandle subscriptionFromOpcUa(List<String> ids,
                                                     double publishingInterval,
                                                     SubscriptionCallback callback) throws Exception {
        return subscriptionFromOpcUa(ids, publishingInterval, null, callback);
    }

    /**
     * 使用指定 endpoint 订阅点位变化并指定发布周期。
     *
     * @param ids 点位标识列表
     * @param publishingInterval 发布周期，单位毫秒
     * @param clientName 配置 key，为 null 时使用 primary
     * @param callback 订阅回调
     * @return 可关闭的订阅句柄
     */
    public SubscriptionHandle subscriptionFromOpcUa(List<String> ids,
                                                     double publishingInterval,
                                                     String clientName,
                                                     SubscriptionCallback callback) throws Exception {
        return subscriptions.subscribe(ids, publishingInterval, clientName, callback);
    }

    /**
     * 使用独立的发布周期和采样周期注册监控项。
     *
     * @param ids 点位标识列表
     * @param publishingInterval 发布周期，单位毫秒
     * @param samplingInterval 采样周期，单位毫秒
     * @param clientName 配置 key，为 null 时使用 primary
     * @param callback 订阅回调
     * @return 可关闭的订阅句柄
     */
    public SubscriptionHandle subscriptionFromOpcUa(List<String> ids,
                                                     double publishingInterval,
                                                     double samplingInterval,
                                                     String clientName,
                                                     SubscriptionCallback callback) throws Exception {
        return subscriptions.subscribe(ids, publishingInterval, samplingInterval, clientName, callback);
    }

    private void writeConverted(List<ReadWriteEntity> entities,
                                String clientName,
                                Function<Object, Object> converter) throws Exception {
        List<WriteEntity> writes = new ArrayList<>();
        if (entities != null) {
            for (ReadWriteEntity entity : entities) {
                if (entity == null) {
                    throw new IllegalArgumentException("写入实体不能为空");
                }
                writes.add(WriteEntity.builder()
                        .identifier(entity.getIdentifier())
                        .variant(new Variant(converter.apply(entity.getValue())))
                        .build());
            }
        }
        writeSpecifyType(writes, clientName);
    }

    private <T> T withClient(String clientName, ClientOperation<T> operation) throws Exception {
        OpcUaClient client = clients.getClient(clientName);
        return operation.apply(client);
    }

    @FunctionalInterface
    private interface ClientOperation<T> {
        T apply(OpcUaClient client) throws Exception;
    }
}
