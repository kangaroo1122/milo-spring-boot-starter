package com.kangaroohy.milo.runner.subscription;

/**
 * 类 SubscriptionCallback 功能描述：<br/>
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2023/5/8 22:14
 */
public interface SubscriptionCallback {

    void onSubscribe(String identifier, Object value);
}
