package com.kangaroohy.milo.runner.subscription;

import com.kangaroohy.milo.utils.CustomUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedDataItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedSubscription;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 类 SubscriptionRunner 功能描述：
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2022/01/01 23:49
 */
@Slf4j
public class SubscriptionRunner {
    /**
     * 点位list
     */
    private final List<String> identifiers;

    private final double samplingInterval;

    public SubscriptionRunner(List<String> identifiers) {
        this.identifiers = identifiers;
        this.samplingInterval = 1000.0D;
    }

    public SubscriptionRunner(List<String> identifiers, double samplingInterval) {
        this.identifiers = identifiers;
        this.samplingInterval = samplingInterval;
    }

    public void run(OpcUaClient opcUaClient, SubscriptionCallback callback) {

        final CountDownLatch downLatch = new CountDownLatch(1);

        //添加订阅监听器，用于处理断线重连后的订阅问题
        opcUaClient.getSubscriptionManager().addSubscriptionListener(new CustomSubscriptionListener(opcUaClient, callback));

        //处理订阅逻辑
        handler(opcUaClient, callback);

        try {
            //持续监听
            downLatch.await();
        } catch (Exception e) {
            log.error("订阅时出现了异常：{}", e.getMessage(), e);
        }
    }

    private void handler(OpcUaClient opcUaClient, SubscriptionCallback callback) {
        try {
            //创建订阅
            ManagedSubscription subscription = ManagedSubscription.create(opcUaClient, samplingInterval);
            subscription.setDefaultSamplingInterval(samplingInterval);
            subscription.setDefaultQueueSize(UInteger.valueOf(10));

            List<NodeId> nodeIdList = new ArrayList<>();
            for (String identifier : identifiers) {
                nodeIdList.add(CustomUtil.parseNodeId(identifier));
            }
            List<ManagedDataItem> dataItemList = subscription.createDataItems(nodeIdList);
            for (ManagedDataItem dataItem : dataItemList) {
                dataItem.addDataValueListener((item) -> callback.onSubscribe(dataItem.getNodeId().getIdentifier().toString(), item.getValue().getValue()));
            }
        } catch (Exception e) {
            log.error("订阅时出现了异常：{}", e.getMessage(), e);
        }
    }

    private class CustomSubscriptionListener implements UaSubscriptionManager.SubscriptionListener {
        private final OpcUaClient client;
        private final SubscriptionCallback callback;

        public CustomSubscriptionListener(OpcUaClient client, SubscriptionCallback callback) {
            this.client = client;
            this.callback = callback;
        }

        /**
         * 重连时 尝试恢复之前的订阅失败时 会调用此方法
         * @param uaSubscription 订阅
         * @param statusCode 状态
         */
        @Override
        public void onSubscriptionTransferFailed(UaSubscription uaSubscription, StatusCode statusCode) {
            log.debug("恢复订阅失败 需要重新订阅");
            //在回调方法中重新订阅
            handler(client, callback);
        }
    }
}
