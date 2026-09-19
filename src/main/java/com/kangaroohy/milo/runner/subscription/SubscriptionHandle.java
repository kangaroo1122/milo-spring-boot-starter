package com.kangaroohy.milo.runner.subscription;

/** 订阅调用返回的可取消句柄。 */
public interface SubscriptionHandle extends AutoCloseable {

    @Override
    void close();
}
