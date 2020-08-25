package com.coctrl.milo.runner;

import com.coctrl.milo.configuration.MiloProperties;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;

/**
 * 类 MiloRunner 功能描述：
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2020/8/24
 */
public class MiloRunner implements Runner {
    private final MiloProperties properties;

    public MiloRunner(MiloProperties properties){
        this.properties = properties;
    }

    @Override
    public String endpointUrl() {
        return properties.getEndpoint();
    }

    @Override
    public SecurityPolicy securityPolicy() {
        if (!properties.getAnonymous()) {
            return SecurityPolicy.Basic256Sha256;
        }
        return SecurityPolicy.None;
    }

    @Override
    public IdentityProvider identityProvider() {
        if (!properties.getAnonymous() && properties.getUsername() != null) {
            return new UsernameProvider(properties.getUsername(), properties.getPassword());
        }
        return new AnonymousProvider();
    }

    @Override
    public Object run(OpcUaClient opcUaClient) throws Exception {
        return null;
    }
}
