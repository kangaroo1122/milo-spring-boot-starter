package com.kangaroohy.milo.utils;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.client.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.regex.Pattern;

@Slf4j
public class KeyStoreLoader {

    private static final Pattern IP_ADDR_PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private static final String CLIENT_ALIAS = "client-ai";
    private static final char[] PASSWORD = "password".toCharArray();

    private static X509Certificate clientCertificate;
    private static X509Certificate[] clientCertificateChain;
    private static KeyPair clientKeyPair;
    private static DefaultClientCertificateValidator certificateValidator;

    private static boolean isLoaded = false;

    private static final Path SECURITY_TEMP_DIR = Paths.get(System.getProperty("user.home"), ".milo-security");

    public static synchronized void load() throws Exception {
        if (isLoaded) {
            log.info("KeyStore is already loaded, skipping initialization.");
            return;
        }

        Files.createDirectories(SECURITY_TEMP_DIR);
        if (!Files.exists(SECURITY_TEMP_DIR)) {
            throw new Exception("unable to create security dir: " + SECURITY_TEMP_DIR);
        }

        File pkiDir = SECURITY_TEMP_DIR.resolve("pki").toFile();

        log.info("security temp dir: {}", SECURITY_TEMP_DIR.toAbsolutePath());

        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        Path serverKeyStore = SECURITY_TEMP_DIR.resolve("milo-client.pfx");

        DefaultTrustListManager trustListManager = new DefaultTrustListManager(pkiDir);

        certificateValidator = new DefaultClientCertificateValidator(trustListManager);

        log.info("Loading KeyStore at {}", serverKeyStore);

        if (!Files.exists(serverKeyStore)) {
            keyStore.load(null, PASSWORD);

            KeyPair keyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);

            SelfSignedCertificateBuilder builder = new SelfSignedCertificateBuilder(keyPair)
                    .setCommonName("Milo Client")
                    .setOrganization("kangaroohy")
                    .setOrganizationalUnit("dev")
                    .setLocalityName("Folsom")
                    .setStateName("CA")
                    .setCountryCode("US")
                    .setApplicationUri("urn:kangaroohy:milo:client")
                    .addDnsName("localhost")
                    .addIpAddress("127.0.0.1");

            // Get as many hostnames and IP addresses as we can listed in the certificate.
            for (String hostname : CustomUtil.getHostnames("0.0.0.0")) {
                if (IP_ADDR_PATTERN.matcher(hostname).matches()) {
                    builder.addIpAddress(hostname);
                } else {
                    builder.addDnsName(hostname);
                }
            }

            X509Certificate certificate = builder.build();

            keyStore.setKeyEntry(CLIENT_ALIAS, keyPair.getPrivate(), PASSWORD, new X509Certificate[]{certificate});
            try (OutputStream out = Files.newOutputStream(serverKeyStore)) {
                keyStore.store(out, PASSWORD);
            }
        } else {
            try (InputStream in = Files.newInputStream(serverKeyStore)) {
                keyStore.load(in, PASSWORD);
            }
        }

        Key clientPrivateKey = keyStore.getKey(CLIENT_ALIAS, PASSWORD);
        if (clientPrivateKey instanceof PrivateKey) {
            clientCertificate = (X509Certificate) keyStore.getCertificate(CLIENT_ALIAS);

            clientCertificateChain = Arrays.stream(keyStore.getCertificateChain(CLIENT_ALIAS))
                    .map(X509Certificate.class::cast)
                    .toArray(X509Certificate[]::new);

            PublicKey clientPublicKey = clientCertificate.getPublicKey();
            clientKeyPair = new KeyPair(clientPublicKey, (PrivateKey) clientPrivateKey);
        }

        isLoaded = true;
    }

    public static X509Certificate getClientCertificate() {
        return clientCertificate;
    }

    public static X509Certificate[] getClientCertificateChain() {
        return clientCertificateChain;
    }

    public static DefaultClientCertificateValidator getCertificateValidator() {
        return certificateValidator;
    }

    public static KeyPair getClientKeyPair() {
        return clientKeyPair;
    }

}
