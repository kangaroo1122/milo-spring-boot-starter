package com.coctrl.milo.runner.subscription;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 类 ManagedSubscriptionRunner 功能描述：
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2022/01/01 23:49
 */
@Slf4j
public class ManagedSubscriptionRunner {
    /**
     * 点位list
     */
    private final List<String> identifiers;

    public ManagedSubscriptionRunner(List<String> identifiers) {
        this.identifiers = identifiers;
    }

    public void run(OpcUaClient opcUaClient) {
        ManagedSubscriptionHandler handler = new ManagedSubscriptionHandler();

        final CountDownLatch downLatch = new CountDownLatch(1);

        //添加订阅监听器，用于处理断线重连后的订阅问题
        opcUaClient.getSubscriptionManager().addSubscriptionListener(new ManagedSubscriptionListener(opcUaClient, handler, identifiers));

        //处理订阅逻辑
        handler.handler(opcUaClient, identifiers);

        try {
            //持续监听
            downLatch.await();
        } catch (Exception e) {
            log.error("订阅时出现了异常：{}", e.getMessage(), e);
        }
    }
}
