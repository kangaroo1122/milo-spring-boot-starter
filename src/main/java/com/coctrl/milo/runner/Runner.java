package com.coctrl.milo.runner;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;

import java.util.function.Predicate;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @desc
 * @since 2020/4/14
 */
public interface Runner {
    /**
     * OPC UA 连接地址
     * @return
     */
    String endpointUrl();

    default Predicate<EndpointDescription> endpointFilter() {
        return e -> true;
    }

    /**
     * algorithms
     * @return
     */
    SecurityPolicy securityPolicy();

    /**
     * 认证方式：
     * anonymousProvider：匿名访问
     * usernameProvider：用户名和密码访问
     * @return
     */
    IdentityProvider identityProvider();

    /**
     * 客服端执行方法
     * @param opcUaClient
     * @return
     * @throws Exception
     */
    Object run(OpcUaClient opcUaClient) throws Exception;
}
