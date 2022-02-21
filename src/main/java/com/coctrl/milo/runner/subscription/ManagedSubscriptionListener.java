package com.coctrl.milo.runner.subscription;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

import java.util.List;

/**
 * 类 CustomSubscriptionListener 功能描述：
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2022/01/02 00:19
 */
@Slf4j
public class ManagedSubscriptionListener implements UaSubscriptionManager.SubscriptionListener {
    private final OpcUaClient client;
    private final ManagedSubscriptionHandler handler;
    private final List<String> identifiers;

    public ManagedSubscriptionListener(OpcUaClient client, ManagedSubscriptionHandler handler, List<String> identifiers) {
        this.client = client;
        this.handler = handler;
        this.identifiers = identifiers;
    }

    @Override
    public void onKeepAlive(UaSubscription subscription, DateTime publishTime) {
        log.debug("onKeepAlive");
    }

    @Override
    public void onStatusChanged(UaSubscription subscription, StatusCode status) {
        log.debug("onStatusChanged");
    }

    @Override
    public void onPublishFailure(UaException exception) {
        log.debug("onPublishFailure");
    }

    @Override
    public void onNotificationDataLost(UaSubscription subscription) {
        log.debug("onNotificationDataLost");
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
        handler.handler(client, identifiers);
    }
}
