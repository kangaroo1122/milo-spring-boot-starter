package com.coctrl.milo.runner;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @desc
 * @since 2020/4/14
 */
public interface Runner {

    /**
     * 客服端执行方法
     * @param opcUaClient
     * @return
     * @throws Exception
     */
    Object run(OpcUaClient opcUaClient) throws Exception;
}
