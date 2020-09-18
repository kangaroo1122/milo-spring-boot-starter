package com.coctrl.milo.runner;

import com.coctrl.milo.configuration.MiloProperties;
import com.coctrl.milo.model.HistoryRead;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.*;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;

/**
 * 类 MiloHistoryReadRunner 功能描述：
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2020/9/18
 */
@Slf4j
public class MiloHistoryReadRunner extends MiloRunner{
    private final String identifier;

    public MiloHistoryReadRunner(String identifier, MiloProperties properties) {
        super(properties);
        this.identifier = identifier;
    }

    @Override
    public Object run(OpcUaClient opcUaClient) {
        try {
            opcUaClient.connect().get();
            HistoryRead historyRead = new HistoryRead();
            HistoryReadDetails historyReadDetails = new ReadRawModifiedDetails(
                    false,
                    DateTime.MIN_VALUE,
                    DateTime.now(),
                    uint(0),
                    true
            );
            HistoryReadValueId historyReadValueId = new HistoryReadValueId(
                    new NodeId(2, identifier),
                    null,
                    QualifiedName.NULL_VALUE,
                    ByteString.NULL_VALUE
            );
            List<HistoryReadValueId> nodesToRead = new ArrayList<>();
            nodesToRead.add(historyReadValueId);

            HistoryReadResponse historyReadResponse = opcUaClient.historyRead(
                    historyReadDetails,
                    TimestampsToReturn.Both,
                    false,
                    nodesToRead
            ).get();
            HistoryReadResult[] historyReadResults = historyReadResponse.getResults();

            if (historyReadResults != null) {
                HistoryReadResult historyReadResult = historyReadResults[0];
                HistoryData historyData = (HistoryData) historyReadResult.getHistoryData().decode(
                        opcUaClient.getSerializationContext()
                );

                List<DataValue> dataValues = l(historyData.getDataValues());
                List<Object> values = new ArrayList<>();
                dataValues.forEach(v -> values.add(v.getValue().getValue()));
                historyRead.setIdentifier(identifier);
                historyRead.setValue(values);
            }
            opcUaClient.disconnect().get();
            return historyRead;
        } catch (Exception e) {
            log.error("读值时出现了异常：{}", e.getMessage(), e);
            return null;
        }
    }
}
