package com.coctrl.milo.runner;

import com.coctrl.milo.model.SubscriptValues;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @desc
 * @since 2020/4/14
 */
@Slf4j
public class SubscriptionRunner {
    /**
     * 点位list
     */
    private final List<String> identifiers;

    public SubscriptionRunner(List<String> identifiers) {
        this.identifiers = identifiers;
    }

    public void run(OpcUaClient opcUaClient) {
        try {
            // create a subscription @ 1000ms
            UaSubscription subscription = opcUaClient.getSubscriptionManager().createSubscription(1000.0).get();
            for (String identifier : identifiers) {
                // subscribe to the Value attribute of the server's CurrentTime node
                ReadValueId readValueId = new ReadValueId(
                        new NodeId(2, identifier),
                        AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE
                );

                // IMPORTANT: client handle must be unique per item within the context of a subscription.
                // You are not required to use the UaSubscription's client handle sequence; it is provided as a convenience.
                // Your application is free to assign client handles by whatever means necessary.
                UInteger clientHandle = subscription.nextClientHandle();

                MonitoringParameters parameters = new MonitoringParameters(
                        clientHandle,
                        // sampling interval
                        1000.0,
                        // filter, null means use default
                        null,
                        // queue size
                        uint(10),
                        // discard oldest
                        true
                );

                MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
                        readValueId,
                        MonitoringMode.Reporting,
                        parameters
                );

                // when creating items in MonitoringMode.Reporting this callback is where each item needs to have its
                // value/event consumer hooked up. The alternative is to create the item in sampling mode, hook up the
                // consumer after the creation call completes, and then change the mode for all items to reporting.
                UaSubscription.ItemCreationCallback onItemCreated =
                        (item, id) -> item.setValueConsumer(this::onSubscriptionValue);

                List<UaMonitoredItem> items = subscription.createMonitoredItems(
                        TimestampsToReturn.Both,
                        newArrayList(request),
                        onItemCreated
                ).get();

                for (UaMonitoredItem item : items) {
                    if (item.getStatusCode().isGood()) {
                        log.info("item created for nodeId={}", item.getReadValueId().getNodeId());
                    } else {
                        log.warn(
                                "failed to create item for nodeId={} (status={})",
                                item.getReadValueId().getNodeId(), item.getStatusCode());
                    }
                }
            }
        } catch (Exception e) {
            log.error("读值时出现了异常：{}", e.getMessage(), e);
        }
    }

    private void onSubscriptionValue(UaMonitoredItem item, DataValue value) {
        SubscriptValues.getSubscriptValues().put(item.getReadValueId().getNodeId().getIdentifier().toString(), value.getValue().getValue());
    }
}
