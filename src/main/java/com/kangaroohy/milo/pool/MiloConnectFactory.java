package com.kangaroohy.milo.pool;

import com.kangaroohy.milo.configuration.MiloProperties;
import com.kangaroohy.milo.exception.EndPointNotFoundException;
import com.kangaroohy.milo.exception.IdentityNotFoundException;
import com.kangaroohy.milo.utils.CustomUtil;
import com.kangaroohy.milo.utils.KeyStoreLoader;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 类 MiloConnectFactory 功能描述：<br/>
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2023/5/4 18:56
 */
public class MiloConnectFactory {

    private final MiloProperties properties;

    public MiloConnectFactory(MiloProperties properties, String primary) {
        this.properties = properties;
        CustomUtil.verifyProperties(properties, primary);
    }

    /**
     * 创建并连接一个长期使用的 OPC UA 客户端。
     *
     * @param key endpoint 配置
     * @return 已连接的客户端
     */
    public OpcUaClient createConnectedClient(MiloProperties.Config key) throws Exception {
        OpcUaClient client = createClient(key);
        try {
            client.connect().get(properties.getRequestTimeout(), TimeUnit.MILLISECONDS);
            return client;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            try {
                client.disconnect().get(properties.getRequestTimeout(), TimeUnit.MILLISECONDS);
            } catch (Exception disconnectException) {
                e.addSuppressed(disconnectException);
            }
            throw e;
        }
    }

    private OpcUaClient createClient(MiloProperties.Config key) throws Exception {
        boolean secureEndpoint = !SecurityPolicy.None.equals(securityPolicy(key));
        if (secureEndpoint) {
            KeyStoreLoader.load();
        }

        return OpcUaClient.create(
                this.endpointUrl(key),
                endpoints -> {
                    List<EndpointDescription> compatible = endpoints.stream()
                            .filter(e -> securityPolicy(key).getUri().equals(e.getSecurityPolicyUri()))
                            .filter(e -> key.getSecurityMode() == null || key.getSecurityMode() == e.getSecurityMode())
                            .collect(java.util.stream.Collectors.toList());
                    // Kepware 经内网穿透或代理时，发现结果可能返回内网 IP，
                    // 而实际连接使用的是公网域名。优先使用完全匹配的 URL，
                    // 但不能因为 host 不同就拒绝安全策略兼容的 endpoint。
                    EndpointDescription description = compatible.stream()
                            .filter(e -> endpointUrl(key).equals(e.getEndpointUrl()))
                            .findFirst()
                            .orElseGet(() -> compatible.stream().findFirst()
                                    .orElseThrow(() -> new EndPointNotFoundException("no desired endpoints returned")));
                    if (!description.getEndpointUrl().equals(endpointUrl(key))) {
                        URI configuredUri = getUri(key);
                        int configuredPort = configuredUri.getPort() > 0
                                ? configuredUri.getPort()
                                : EndpointUtil.getPort(endpointUrl(key));
                        description = EndpointUtil.updateUrl(description, configuredUri.getHost(), configuredPort);
                    }
                    return Optional.of(description);
                },
                configBuilder -> {
                    configBuilder
                            .setApplicationName(LocalizedText.english("milo opc-ua client"))
                            .setApplicationUri("urn:kangaroohy:milo:client")
                            .setIdentityProvider(this.identityProvider(key))
                            .setRequestTimeout(Unsigned.uint(properties.getRequestTimeout()));
                    if (secureEndpoint) {
                        configBuilder
                                .setKeyPair(KeyStoreLoader.getClientKeyPair())
                                .setCertificate(KeyStoreLoader.getClientCertificate())
                                .setCertificateChain(KeyStoreLoader.getClientCertificateChain())
                                .setCertificateValidator(KeyStoreLoader.getCertificateValidator());
                    }
                    return configBuilder.build();
                }
        );
    }

    private URI getUri(MiloProperties.Config key) {
        try {
            return new URI(endpointUrl(key));
        } catch (URISyntaxException e) {
            throw new EndPointNotFoundException("endpoint 配置异常");
        }
    }

    private String endpointUrl(MiloProperties.Config key) {
        return key.getEndpoint();
    }

    private SecurityPolicy securityPolicy(MiloProperties.Config key) {
        return key.getSecurityPolicy();
    }

    private IdentityProvider identityProvider(MiloProperties.Config key) {
        boolean hasUsername = key.getUsername() != null && !key.getUsername().trim().isEmpty();
        boolean hasPassword = key.getPassword() != null;
        if (!hasUsername && !hasPassword) {
            return new AnonymousProvider();
        }
        if (!hasUsername || !hasPassword) {
            throw new IdentityNotFoundException("连接信息未完善");
        }
        return new UsernameProvider(key.getUsername(), key.getPassword());
    }
}
