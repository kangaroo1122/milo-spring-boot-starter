package com.coctrl.milo.runner;

import com.coctrl.milo.model.ReadOrWrite;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @desc
 * @since 2020/4/14
 */
@Slf4j
public class MiloWriteRunner implements Runner {
    private final ReadOrWrite entity;

    public MiloWriteRunner(ReadOrWrite entity) {
        this.entity = entity;
    }

    @Override
    public Object run(OpcUaClient opcUaClient) {
        try {
            opcUaClient.connect().get();
            List<NodeId> nodeIds = ImmutableList.of(new NodeId(2, entity.getIdentifier()));

            Variant variant = new Variant(entity.getValue());

            // 不需要写 status 和 timestamps
            DataValue dataValue = new DataValue(variant, null, null);

            CompletableFuture<List<StatusCode>> future = opcUaClient.writeValues(nodeIds, ImmutableList.of(dataValue));
            List<StatusCode> statusCodes = future.get();
            StatusCode status = statusCodes.get(0);
            if (status.isGood()) {
                log.info("将值 '{}' 写入到点位：{} 成功", variant, nodeIds.get(0));
            } else {
                log.error("点位：{} 写入时出现了异常：{}", entity.getIdentifier(), status);
            }
            opcUaClient.disconnect().get();
        } catch (Exception e) {
            log.error("写入时出现了异常：{}", e.getMessage(), e);
        }
        return null;
    }
}
