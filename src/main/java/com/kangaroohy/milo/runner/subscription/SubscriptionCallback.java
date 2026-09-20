package com.kangaroohy.milo.runner.subscription;

import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

/**
 * 类 SubscriptionCallback 功能描述：<br/>
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2023/5/8 22:14
 */
@FunctionalInterface
public interface SubscriptionCallback {

    /**
     * Invoked asynchronously for a monitored value change. Implementations
     * should still be short-lived; long-running work belongs in the caller's
     * own queue or executor.
     *
     * @param dataItem 发生变化的监控项
     * @param value 最新数据值
     */
    void onSubscribe(OpcUaMonitoredItem dataItem, DataValue value);
}
