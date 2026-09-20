package com.kangaroohy.milo.utils;

import com.kangaroohy.milo.configuration.MiloProperties;
import com.kangaroohy.milo.exception.EndPointNotFoundException;
import com.kangaroohy.milo.exception.IdentityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.springframework.util.StringUtils;

import java.net.*;
import java.util.*;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @desc
 * @since 2020/4/13
 */
@Slf4j
public class CustomUtil {

    private static final String OPC_UA_NOT_CONFIG = "请配置OPC UA地址信息";

    private CustomUtil() {
    }

    public static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException var1) {
            return "localhost";
        }
    }

    public static Set<String> getHostnames(String address) {
        return getHostnames(address, true);
    }

    public static Set<String> getHostnames(String address, boolean includeLoopback) {
        HashSet<String> hostnames = new HashSet<>();

        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            if (inetAddress.isAnyLocalAddress()) {
                try {
                    Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

                    for (NetworkInterface ni : Collections.list(nis)) {
                        Collections.list(ni.getInetAddresses()).forEach((ia) -> {
                            if (ia instanceof Inet4Address) {
                                boolean loopback = ia.isLoopbackAddress();
                                if (!loopback || includeLoopback) {
                                    hostnames.add(ia.getHostName());
                                    hostnames.add(ia.getHostAddress());
                                    hostnames.add(ia.getCanonicalHostName());
                                }
                            }

                        });
                    }
                } catch (SocketException var7) {
                    log.warn("Failed to NetworkInterfaces for bind address: {}", address, var7);
                }
            } else {
                boolean loopback = inetAddress.isLoopbackAddress();
                if (!loopback || includeLoopback) {
                    hostnames.add(inetAddress.getHostName());
                    hostnames.add(inetAddress.getHostAddress());
                    hostnames.add(inetAddress.getCanonicalHostName());
                }
            }
        } catch (UnknownHostException var8) {
            log.warn("Failed to get InetAddress for bind address: {}", address, var8);
        }

        return hostnames;
    }

    public static NodeId parseNodeId(String identifier) {
        NodeId nodeId = new NodeId(2, identifier);
        if (identifier.startsWith("ns=") && identifier.contains(";")) {
            nodeId = NodeId.parseOrNull(identifier);
        }
        if (nodeId == null) {
            throw new IdentityNotFoundException("NodeId 解析失败，请检查");
        }
        return nodeId;
    }

    public static void verifyProperties(MiloProperties properties, String primary) {
        if (properties.getConfig().isEmpty()) {
            throw new EndPointNotFoundException(OPC_UA_NOT_CONFIG);
        }
        if (StringUtils.hasText(primary)) {
            Set<String> keySet = properties.getConfig().keySet();
            if (!keySet.contains(primary)) {
                log.warn("The primary property '{}' does not exist in the config, ignore it", primary);
            } else {
                properties.setPrimary(primary);
            }
        }
        if (!StringUtils.hasText(properties.getPrimary())) {
            Set<String> keySet = properties.getConfig().keySet();
            properties.setPrimary(keySet.stream().findFirst().orElseThrow(() -> new EndPointNotFoundException(OPC_UA_NOT_CONFIG)));
            log.warn("The primary property is '{}'.", properties.getPrimary());
        }
        properties.getConfig().forEach((key, config) -> {
            if (config == null) {
                throw new EndPointNotFoundException(OPC_UA_NOT_CONFIG + ": " + key);
            }
            if (!StringUtils.hasText(config.getEndpoint())) {
                throw new EndPointNotFoundException(OPC_UA_NOT_CONFIG + ": " + key);
            }
            if (config.getSecurityPolicy() == null) {
                config.setSecurityPolicy(SecurityPolicy.None);
            }
        });
    }

}
