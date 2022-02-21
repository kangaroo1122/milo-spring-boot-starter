package com.coctrl.milo.runner.subscription;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedDataItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedSubscription;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import java.util.ArrayList;
import java.util.List;

/**
 * 类 ManagedSubscriptionHandler 功能描述：
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2022/01/02 00:26
 */
@Slf4j
public class ManagedSubscriptionHandler {

    public void handler(OpcUaClient opcUaClient, List<String> identifiers) {
        try {
            //创建订阅
            ManagedSubscription subscription = ManagedSubscription.create(opcUaClient);

            List<NodeId> nodeIdList = new ArrayList<>();
            for (String identifier : identifiers) {
                nodeIdList.add(new NodeId(2, identifier));
            }
            List<ManagedDataItem> dataItemList = subscription.createDataItems(nodeIdList);
            for (ManagedDataItem dataItem : dataItemList) {
                dataItem.addDataValueListener((item) -> {
                    System.out.println(dataItem.getNodeId().getIdentifier().toString() + "：" + item.getValue().getValue().toString());
                });
            }
        } catch (Exception e) {
            log.error("订阅时出现了异常：{}", e.getMessage(), e);
        }
    }
}
