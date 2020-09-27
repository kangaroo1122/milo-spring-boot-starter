package com.coctrl.milo.service;

import com.coctrl.milo.configuration.MiloProperties;
import com.coctrl.milo.model.ReadOrWrite;
import com.coctrl.milo.model.SubscriptValues;
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
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private final MiloProperties properties;

    private Queue<OpcUaClient> queue = new ConcurrentLinkedQueue<>();

    public MiloService(MiloProperties properties) {
        this.properties = properties;
    }

    public boolean writeToOpcUa(ReadOrWrite entity) {
        MiloWriteRunner runner = new MiloWriteRunner(entity);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                return runner.run(client);
            } finally {
                disconnect(client);
            }
        }
        return false;
    }

    public List<ReadOrWrite> readFromOpcUa(List<String> ids) {
        MiloReadRunner runner = new MiloReadRunner(ids);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                return runner.run(client);
            } finally {
                disconnect(client);
            }
        }
        return null;
    }

    public void subscriptionFromOpcUa(List<String> ids) {
        MiloSubscriptionRunner runner = new MiloSubscriptionRunner(ids);
        OpcUaClient client = connect();
        if (client != null) {
            try {
                runner.run(client);
            } finally {
                disconnect(client);
            }
        }
    }

    public Map<String, Object> readSubscriptionValues(){
        return SubscriptValues.getSubscriptValues();
    }

    public Object readSubscriptionValues(String id){
        if (SubscriptValues.getSubscriptValues().containsKey(id)){
            return SubscriptValues.getSubscriptValues().get(id);
        }
        return null;
    }

    private OpcUaClient connect() {
        OpcUaClient client = queue.poll();
        if (client != null) {
            return client;
        }
        try {
            client = createClient();
            client.connect().get();
            queue.add(client);
            return client;
        } catch (Exception e) {
            log.error("OpcUaClient create error: ", e);
        }
        return null;
    }

    void disconnect(OpcUaClient client) {
        queue.add(client);
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
