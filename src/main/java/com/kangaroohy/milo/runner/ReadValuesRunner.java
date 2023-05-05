package com.kangaroohy.milo.runner;

import com.kangaroohy.milo.model.ReadWriteEntity;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;

import java.util.ArrayList;
import java.util.List;

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

    public ReadValuesRunner(List<String> identifiers) {
        this.identifiers = identifiers;
    }

    public List<ReadWriteEntity> run(OpcUaClient opcUaClient) {
        List<ReadWriteEntity> entityList = new ArrayList<>();
        try {
            for (String id : identifiers) {
                NodeId nodeId = new NodeId(2, id);

                // 读取指定点位的值，10s超时
                DataValue dataValue = opcUaClient.readValue(10000, TimestampsToReturn.Both, nodeId).get();

                Object value = dataValue.getValue().getValue();

                StatusCode status = dataValue.getStatusCode();
                assert status != null;
                if (status.isGood()) {
                    log.info("读取点位 '{}' 的值为 {}", nodeId, value);
                }
                entityList.add(ReadWriteEntity.builder()
                        .identifier(id)
                        .value(value)
                        .build());
            }
        } catch (Exception e) {
            log.error("读值时出现了异常：{}", e.getMessage(), e);
        }
        return entityList;
    }
}
