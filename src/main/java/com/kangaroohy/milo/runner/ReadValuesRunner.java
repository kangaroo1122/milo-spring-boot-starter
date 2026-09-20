package com.kangaroohy.milo.runner;

import com.kangaroohy.milo.model.ReadWriteEntity;
import com.kangaroohy.milo.utils.CustomUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Performs chunked OPC UA Read requests and merges the results in input order.
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @desc 分批读取 OPC UA 点位
 * @since 2020/4/14
 */
@Slf4j
public class ReadValuesRunner {
    /** A conservative default below common server MaxNodesPerRead limits. */
    public static final int DEFAULT_BATCH_SIZE = 200;

    /** 要读取的点位 ID 列表。 */
    private final List<String> identifiers;
    private final double maxAge;
    private final int batchSize;
    private final long requestTimeout;

    public ReadValuesRunner(List<String> identifiers) {
        this(identifiers, 10000.0D);
    }

    public ReadValuesRunner(List<String> identifiers, double maxAge) {
        this(identifiers, maxAge, DEFAULT_BATCH_SIZE);
    }

    public ReadValuesRunner(List<String> identifiers, double maxAge, int batchSize) {
        this(identifiers, maxAge, batchSize, Math.max(1L, (long) maxAge));
    }

    public ReadValuesRunner(List<String> identifiers, double maxAge, int batchSize, long requestTimeout) {
        this.identifiers = identifiers == null ? Collections.emptyList() : new ArrayList<>(identifiers);
        if (maxAge < 0) {
            throw new IllegalArgumentException("maxAge 不能小于 0");
        }
        this.maxAge = maxAge;
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize 必须大于 0");
        }
        this.batchSize = batchSize;
        if (requestTimeout <= 0) {
            throw new IllegalArgumentException("requestTimeout 必须大于 0");
        }
        this.requestTimeout = requestTimeout;
    }

    /**
     * 分批读取点位，并按输入顺序合并结果。
     *
     * @param client OPC UA 客户端
     * @return 点位值和对应的 DataValue
     */
    public List<ReadWriteEntity> run(OpcUaClient client) throws Exception {
        if (identifiers.isEmpty()) {
            return Collections.emptyList();
        }

        List<ReadWriteEntity> result = new ArrayList<>(identifiers.size());
        for (int start = 0; start < identifiers.size(); start += batchSize) {
            int end = Math.min(start + batchSize, identifiers.size());
            List<String> batchIdentifiers = identifiers.subList(start, end);
            for (String identifier : batchIdentifiers) {
                if (identifier == null || identifier.trim().isEmpty()) {
                    throw new IllegalArgumentException("NodeId 不能为空");
                }
            }
            DataValue[] values = readBatch(client, batchIdentifiers);
            for (int i = 0; i < batchIdentifiers.size(); i++) {
                DataValue dataValue = i < values.length && values[i] != null
                        ? values[i]
                        : new DataValue(new StatusCode(StatusCodes.Bad_CommunicationError));
                StatusCode status = dataValue.getStatusCode();
                Object value = dataValue.getValue() == null ? null : dataValue.getValue().getValue();
                if (status == null || !status.isGood()) {
                    value = null;
                }
                result.add(ReadWriteEntity.builder()
                        .identifier(batchIdentifiers.get(i))
                        .value(value)
                        .dataValue(dataValue)
                        .build());
            }
        }
        return result;
    }

    private DataValue[] readBatch(OpcUaClient client, List<String> batchIdentifiers) throws Exception {
        List<ReadValueId> readValueIds = new ArrayList<>(batchIdentifiers.size());
        for (String identifier : batchIdentifiers) {
            NodeId nodeId = CustomUtil.parseNodeId(identifier);
            readValueIds.add(new ReadValueId(nodeId, Unsigned.uint(13), null, QualifiedName.NULL_VALUE));
        }

        try {
            ReadResponse response = client.readAsync(maxAge, TimestampsToReturn.Both, readValueIds)
                    .get(requestTimeout, TimeUnit.MILLISECONDS);
            DataValue[] results = response == null ? null : response.getResults();
            return results == null ? failureValues(batchIdentifiers.size(), StatusCodes.Bad_CommunicationError) : results;
        } catch (TimeoutException e) {
            log.warn("OPC UA 批量读取超时，点位数: {}", batchIdentifiers.size());
            return failureValues(batchIdentifiers.size(), StatusCodes.Bad_Timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            log.warn("OPC UA 批量读取失败，点位数: {}，原因: {}", batchIdentifiers.size(), e.getMessage());
            return failureValues(batchIdentifiers.size(), StatusCodes.Bad_CommunicationError);
        }
    }

    private DataValue[] failureValues(int size, long statusCode) {
        DataValue[] values = new DataValue[size];
        for (int i = 0; i < size; i++) {
            values[i] = new DataValue(new StatusCode(statusCode));
        }
        return values;
    }
}
