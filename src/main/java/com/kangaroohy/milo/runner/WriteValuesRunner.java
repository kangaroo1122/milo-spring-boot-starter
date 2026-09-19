package com.kangaroohy.milo.runner;

import com.kangaroohy.milo.model.WriteEntity;
import com.kangaroohy.milo.utils.CustomUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.StatusCodes;

import java.util.LinkedList;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @since 2020/4/14
 */
@Slf4j
public class WriteValuesRunner {
    /** 待写入的点位和值。 */
    private final List<WriteEntity> entities;
    private final int batchSize;
    private final long requestTimeout;

    public WriteValuesRunner(List<WriteEntity> entities) {
        this(entities, 200, 5000L);
    }

    public WriteValuesRunner(List<WriteEntity> entities, int batchSize, long requestTimeout) {
        this.entities = entities == null ? Collections.emptyList() : new ArrayList<>(entities);
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize 必须大于 0");
        }
        if (requestTimeout <= 0) {
            throw new IllegalArgumentException("requestTimeout 必须大于 0");
        }
        this.batchSize = batchSize;
        this.requestTimeout = requestTimeout;
    }

    /**
     * 分批写入点位，并返回服务端写入状态。
     *
     * @param opcUaClient OPC UA 客户端
     * @return 按输入顺序排列的写入状态
     */
    public List<StatusCode> run(OpcUaClient opcUaClient) throws Exception {
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<StatusCode> allStatusCodes = new ArrayList<>(entities.size());
        List<String> failures = new LinkedList<>();
        for (int start = 0; start < entities.size(); start += batchSize) {
            int end = Math.min(start + batchSize, entities.size());
            List<WriteEntity> batch = entities.subList(start, end);
            List<NodeId> nodeIds = new ArrayList<>(batch.size());
            List<DataValue> dataValues = new ArrayList<>(batch.size());
            for (WriteEntity entity : batch) {
                if (entity == null || entity.getIdentifier() == null
                        || entity.getIdentifier().trim().isEmpty()) {
                    throw new IllegalArgumentException("写入实体或 NodeId 不能为空");
                }
                if (entity.getVariant() == null) {
                    throw new IllegalArgumentException("写入值不能为空: " + entity.getIdentifier());
                }
                nodeIds.add(CustomUtil.parseNodeId(entity.getIdentifier()));
                dataValues.add(new DataValue(entity.getVariant(), null, null));
            }

            List<StatusCode> statusCodes;
            try {
                statusCodes = opcUaClient.writeValues(nodeIds, dataValues)
                        .get(requestTimeout, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.warn("OPC UA 批量写入超时，点位数: {}", batch.size());
                statusCodes = failureStatuses(batch.size(), StatusCodes.Bad_Timeout);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                log.warn("OPC UA 批量写入失败，点位数: {}，原因: {}", batch.size(), e.getMessage());
                statusCodes = failureStatuses(batch.size(), StatusCodes.Bad_CommunicationError);
            }
            if (statusCodes == null || statusCodes.size() != batch.size()) {
                log.warn("OPC UA 返回的写入状态数量与请求数量不一致，期望: {}，实际: {}",
                        batch.size(), statusCodes == null ? 0 : statusCodes.size());
                statusCodes = failureStatuses(batch.size(), StatusCodes.Bad_CommunicationError);
            } else {
                statusCodes = new ArrayList<>(statusCodes);
            }
            for (int i = 0; i < statusCodes.size(); i++) {
                if (statusCodes.get(i) == null) {
                    statusCodes.set(i, new StatusCode(StatusCodes.Bad_CommunicationError));
                }
            }
            allStatusCodes.addAll(statusCodes);
            for (int i = 0; i < statusCodes.size(); i++) {
                StatusCode statusCode = statusCodes.get(i);
                if (statusCode.isGood()) {
                    log.debug("将值 '{}' 写入到点位：{} 成功", dataValues.get(i).getValue(), nodeIds.get(i));
                } else {
                    failures.add(nodeIds.get(i) + "=" + statusCode);
                }
            }
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("OPC UA 写入失败: " + failures);
        }
        return allStatusCodes;
    }

    private List<StatusCode> failureStatuses(int size, long statusCode) {
        List<StatusCode> statuses = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            statuses.add(new StatusCode(statusCode));
        }
        return statuses;
    }
}
