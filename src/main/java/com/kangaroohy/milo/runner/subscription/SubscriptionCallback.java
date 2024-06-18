package com.kangaroohy.milo.runner.subscription;

import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedDataItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

/**
 * 类 SubscriptionCallback 功能描述：<br/>
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2023/5/8 22:14
 */
public interface SubscriptionCallback {

    void onSubscribe(ManagedDataItem dataItem, DataValue value);
}
