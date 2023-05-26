package com.kangaroohy.milo.runner;

import com.kangaroohy.milo.model.WriteEntity;
import com.kangaroohy.milo.utils.CustomUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

import java.util.LinkedList;
import java.util.List;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @since 2020/4/14
 */
@Slf4j
public class WriteValuesRunner {
    private final List<WriteEntity> entities;

    public WriteValuesRunner(List<WriteEntity> entities) {
        this.entities = entities;
    }

    public void run(OpcUaClient opcUaClient) {
        try {
            if (!entities.isEmpty()) {
                List<NodeId> nodeIds = new LinkedList<>();
                List<DataValue> dataValues = new LinkedList<>();
                for (WriteEntity entity : entities) {
                    nodeIds.add(CustomUtil.parseNodeId(entity.getIdentifier()));
                    dataValues.add(new DataValue(entity.getVariant(), null, null));
                }

                List<StatusCode> statusCodeList = opcUaClient.writeValues(nodeIds, dataValues).join();
                for (int i = 0; i < statusCodeList.size(); i++) {
                    if (statusCodeList.get(i).isGood()) {
                        log.info("将值 '{}' 写入到点位：{} 成功", dataValues.get(i).getValue(), nodeIds.get(i));
                    } else {
                        log.error("点位：{} 写入时出现了异常：{}", nodeIds.get(i), statusCodeList.get(i));
                    }
                }
            }
        } catch (Exception e) {
            log.error("批量写值出现异常出现了异常：{}", e.getMessage(), e);
        }
    }
}
