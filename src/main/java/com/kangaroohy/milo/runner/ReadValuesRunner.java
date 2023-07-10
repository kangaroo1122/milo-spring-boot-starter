package com.kangaroohy.milo.runner;

import com.kangaroohy.milo.model.ReadWriteEntity;
import com.kangaroohy.milo.utils.CustomUtil;
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
            List<NodeId> nodeIds = new ArrayList<>();
            identifiers.forEach(identifier -> nodeIds.add(CustomUtil.parseNodeId(identifier)));
            // 读取指定点位的值，10s超时
            List<DataValue> dataValues = opcUaClient.readValues(10000, TimestampsToReturn.Both, nodeIds).get();
            if (dataValues.size() == identifiers.size()) {
                for (int i = 0; i < identifiers.size(); i++) {
                    String id = identifiers.get(i);
                    Object value = dataValues.get(i).getValue().getValue();
                    StatusCode status = dataValues.get(i).getStatusCode();
                    assert status != null;
                    if (status.isGood()) {
                        log.info("读取点位 '{}' 的值为 {}", id, value);
                    }
                    entityList.add(ReadWriteEntity.builder()
                            .identifier(id)
                            .value(value)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("读值时出现了异常：{}", e.getMessage(), e);
        }
        return entityList;
    }
}
