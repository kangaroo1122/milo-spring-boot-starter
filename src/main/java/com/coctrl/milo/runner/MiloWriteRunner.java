package com.coctrl.milo.runner;

import com.coctrl.milo.model.WriteEntity;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.util.concurrent.CompletableFuture;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @desc
 * @since 2020/4/14
 */
@Slf4j
public class MiloWriteRunner {
    private final WriteEntity entity;

    public MiloWriteRunner(WriteEntity entity) {
        this.entity = entity;
    }

    public boolean run(OpcUaClient opcUaClient) {
        try {
            NodeId nodeId = new NodeId(2, entity.getIdentifier());

            Variant variant = entity.getVariant();

            // 不需要写 status 和 timestamps
            DataValue dataValue = new DataValue(variant, null, null);

            CompletableFuture<StatusCode> future = opcUaClient.writeValue(nodeId, dataValue);
            StatusCode status = future.get();
            if (status.isGood()) {
                log.info("将值 '{}' 写入到点位：{} 成功", variant, nodeId);
            } else {
                log.error("点位：{} 写入时出现了异常：{}", entity.getIdentifier(), status);
            }
            return status.isGood();
        } catch (Exception e) {
            log.error("写入到 {} 时出现了异常：{}", entity.getIdentifier(), e.getMessage(), e);
            return false;
        }
    }
}
