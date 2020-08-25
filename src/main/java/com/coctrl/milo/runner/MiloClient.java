package com.coctrl.milo.runner;

import com.coctrl.milo.utils.KeyStoreLoader;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @desc
 * @since 2020/4/14
 */
@Slf4j
public class MiloClient {
    private final Runner runner;

    public MiloClient(Runner runner) {
        this.runner = runner;
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
                runner.endpointUrl(),
                endpoints ->
                        endpoints.stream()
                                .filter(runner.endpointFilter())
                                .findFirst(),
                configBuilder ->
                        configBuilder
                                .setApplicationName(LocalizedText.english("milo opc-ua client"))
                                .setApplicationUri("urn:coctrl:milo:client")
                                .setCertificate(loader.getClientCertificate())
                                .setKeyPair(loader.getClientKeyPair())
                                .setIdentityProvider(runner.identityProvider())
                                .setRequestTimeout(uint(5000))
                                .build()
        );
    }

    public Object run() {
        OpcUaClient client;
        try {
            client = createClient();
            return runner.run(client);
        } catch (Exception e) {
            log.error("发生异常: {}", e.getMessage(), e);
            return null;
        }
    }
}
