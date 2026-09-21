package com.kangaroohy.milo.service;

import com.kangaroohy.milo.configuration.MiloProperties;
import com.kangaroohy.milo.utils.CustomUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.client.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/** Certificate resources owned by one application context, loaded only for secure endpoints. */
@Slf4j
public class MiloCertificateManager implements AutoCloseable {
    private static final String CLIENT_ALIAS = "milo-client";
    private final Path directory;
    private final char[] password;
    private final String applicationUri;
    private boolean closed;
    private X509Certificate certificate;
    private X509Certificate[] chain;
    private KeyPair keyPair;
    private DefaultTrustListManager trustListManager;
    private DefaultClientCertificateValidator validator;

    public MiloCertificateManager(MiloProperties.Certificate properties) {
        directory = java.nio.file.Paths.get(properties.getDirectory());
        password = properties.getPassword().toCharArray();
        applicationUri = properties.getApplicationUri();
        if (applicationUri == null || applicationUri.trim().isEmpty()) {
            throw new IllegalArgumentException("certificate.application-uri 不能为空");
        }
    }

    public synchronized void load() throws Exception {
        if (closed) {
            throw new IllegalStateException("证书管理器已关闭");
        }
        if (validator != null) {
            return;
        }
        Files.createDirectories(directory);
        Path keyStorePath = directory.resolve("milo-client.pfx");
        KeyStore store = KeyStore.getInstance("PKCS12");
        if (Files.exists(keyStorePath)) {
            try (InputStream in = Files.newInputStream(keyStorePath)) {
                store.load(in, password);
            }
        } else {
            store.load(null, password);
            KeyPair generated = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);
            SelfSignedCertificateBuilder builder = new SelfSignedCertificateBuilder(generated)
                    .setCommonName("Milo Client").setOrganization("kangaroohy")
                    .setApplicationUri(applicationUri).addDnsName("localhost").addIpAddress("127.0.0.1");
            for (String hostname : CustomUtil.getHostnames("0.0.0.0")) {
                if (hostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                    builder.addIpAddress(hostname);
                } else {
                    builder.addDnsName(hostname);
                }
            }
            store.setKeyEntry(CLIENT_ALIAS, generated.getPrivate(), password,
                    new X509Certificate[]{builder.build()});
            try (OutputStream out = Files.newOutputStream(keyStorePath,
                    java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE)) {
                store.store(out, password);
            }
        }
        Key privateKey = store.getKey(CLIENT_ALIAS, password);
        java.security.cert.Certificate[] storedChain = store.getCertificateChain(CLIENT_ALIAS);
        if (!(privateKey instanceof PrivateKey) || storedChain == null || storedChain.length == 0) {
            throw new KeyStoreException("OPC UA client key/certificate missing: " + CLIENT_ALIAS);
        }
        X509Certificate[] loadedChain = Arrays.stream(storedChain)
                .map(X509Certificate.class::cast).toArray(X509Certificate[]::new);
        loadedChain[0].checkValidity();
        java.util.Collection<java.util.List<?>> names = loadedChain[0].getSubjectAlternativeNames();
        if (names == null || names.stream().noneMatch(name -> Integer.valueOf(6).equals(name.get(0))
                && applicationUri.equals(name.get(1)))) {
            throw new KeyStoreException("证书 Application URI 与 certificate.application-uri 不一致");
        }
        Path pki = directory.resolve("pki");
        DefaultTrustListManager loadedTrust = new DefaultTrustListManager(pki.toFile());
        try {
            validator = new DefaultClientCertificateValidator(loadedTrust);
            trustListManager = loadedTrust;
            chain = loadedChain;
            certificate = chain[0];
            keyPair = new KeyPair(certificate.getPublicKey(), (PrivateKey) privateKey);
        } catch (Exception e) {
            try {
                loadedTrust.close();
            } catch (Exception cleanup) {
                e.addSuppressed(cleanup);
            }
            throw e;
        }
    }

    public String getApplicationUri() { return applicationUri; }
    public synchronized X509Certificate getClientCertificate() { return certificate; }
    public synchronized X509Certificate[] getClientCertificateChain() { return chain == null ? null : chain.clone(); }
    public synchronized KeyPair getClientKeyPair() { return keyPair; }
    public synchronized DefaultClientCertificateValidator getCertificateValidator() { return validator; }

    @Override
    public synchronized void close() {
        if (closed) { return; }
        closed = true;
        if (trustListManager != null) {
            try {
                trustListManager.close();
            } catch (Exception e) {
                log.warn("Failed to close OPC UA trust list manager", e);
            }
        }
        trustListManager = null;
        validator = null;
        certificate = null;
        chain = null;
        keyPair = null;
        Arrays.fill(password, '\0');
    }
}
