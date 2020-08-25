package com.coctrl.milo.service;

import com.coctrl.milo.configuration.MiloProperties;
import com.coctrl.milo.model.ReadOrWrite;
import com.coctrl.milo.runner.MiloReadRunner;
import com.coctrl.milo.runner.MiloSubscriptionRunner;
import com.coctrl.milo.runner.MiloWriteRunner;
import com.coctrl.milo.utils.KeyStoreLoader;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.springframework.stereotype.Service;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * @author kangaroo hy
 * @date 2020/4/25
 * @desc milo-spring-boot-starter
 * @since 0.0.1
 */
@Service
@Slf4j
public class MiloService {
    private MiloProperties properties;

    public MiloService() {
    }

    public MiloService(MiloProperties properties) {
        this.properties = properties;
    }

    public void writeToOpcUa(ReadOrWrite entity) {
        new MiloWriteRunner(entity).run(getConnect());
    }

    @SuppressWarnings("unchecked")
    public List<ReadOrWrite> readFromOpcUa(List<String> ids) {
        return (List<ReadOrWrite>) new MiloReadRunner(ids).run(getConnect());
    }

    public Object subscriptionFromOpcUa(List<String> ids) {
        return new MiloSubscriptionRunner(ids).run(getConnect());
    }

    private OpcUaClient getConnect() {
        try {
            return createClient();
        } catch (Exception e) {
            log.error("OpcUaClient create error: ", e);
        }
        return null;
    }

    private OpcUaClient createClient() throws Exception {
        Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("unable to create security dir: " + securityTempDir);
        }
        log.info("security temp dir: {}", securityTempDir.toAbsolutePath());

        KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

        return OpcUaClient.create(
                this.endpointUrl(),
                endpoints ->
                        endpoints.stream()
                                //.filter(e -> true)
                                .findFirst(),
                configBuilder ->
                        configBuilder
                                .setApplicationName(LocalizedText.english("milo opc-ua client"))
                                .setApplicationUri("urn:coctrl:milo:client")
                                .setCertificate(loader.getClientCertificate())
                                .setKeyPair(loader.getClientKeyPair())
                                .setIdentityProvider(this.identityProvider())
                                .setRequestTimeout(uint(5000))
                                .build()
        );
    }

    private String endpointUrl() {
        return properties.getEndpoint();
    }

    private SecurityPolicy securityPolicy() {
        if (!properties.getAnonymous()) {
            return SecurityPolicy.Basic256Sha256;
        }
        return SecurityPolicy.None;
    }

    private IdentityProvider identityProvider() {
        if (!properties.getAnonymous() && properties.getUsername() != null) {
            return new UsernameProvider(properties.getUsername(), properties.getPassword());
        }
        return new AnonymousProvider();
    }
}
