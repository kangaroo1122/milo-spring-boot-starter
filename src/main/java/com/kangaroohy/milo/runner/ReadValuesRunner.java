package com.kangaroohy.milo.runner;

import com.kangaroohy.milo.model.ReadWriteEntity;
import com.kangaroohy.milo.utils.CompletableFutureTimeoutUtil;
import com.kangaroohy.milo.utils.CustomUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @desc
 * @since 2020/4/14
 */
@Slf4j
public class ReadValuesRunner {
    /**
     * 要读的点位list
     */
    private final List<String> identifiers;

    private final double maxAge;

    public ReadValuesRunner(List<String> identifiers) {
        this.identifiers = identifiers;
        this.maxAge = 10000.0D;
    }

    public ReadValuesRunner(List<String> identifiers, double maxAge) {
        this.identifiers = identifiers;
        this.maxAge = maxAge;
    }

    public List<ReadWriteEntity> run(OpcUaClient opcUaClient) {
        List<ReadWriteEntity> entityList = new ArrayList<>();
        try {
            Map<String, CompletableFuture<DataValue>> futureMap = new HashMap<>();
            identifiers.forEach(identifier -> {
                NodeId nodeId = CustomUtil.parseNodeId(identifier);
                // 读取指定点位的值，10s超时
                CompletableFuture<DataValue> future = opcUaClient.readValue(maxAge, TimestampsToReturn.Both, nodeId);
                CompletableFutureTimeoutUtil.completeOnTimeout(DataValue.newValue().build(), future, maxAge / 1000, TimeUnit.SECONDS, null);
                futureMap.put(identifier, future);
            });
            CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0])).get();
            for (Map.Entry<String, CompletableFuture<DataValue>> entry : futureMap.entrySet()) {
                String nodeId = entry.getKey();
                DataValue dataValue = entry.getValue().get();
                StatusCode status = dataValue.getStatusCode();
                Object value;
                if (Objects.isNull(status)) {
                    dataValue = DataValue.newValue().build();
                    value = null;
                    log.info("读取点位 '{}' 的值超时而失败", nodeId);
                } else {
                    value = dataValue.getValue().getValue();
                    if (status.isGood()) {
                        log.info("读取点位 '{}' 的值为 {}", nodeId, value);
                    }
                }
                entityList.add(ReadWriteEntity.builder()
                        .identifier(nodeId)
                        .value(value)
                        .dataValue(dataValue)
                        .build());
            }
        } catch (Exception e) {
            log.error("读值时出现了异常：{}", e.getMessage(), e);
        }
        return entityList;
    }
}
