package com.kangaroohy.milo.utils;

import com.google.common.collect.Sets;
import com.kangaroohy.milo.exception.IdentityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

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
        HashSet<String> hostnames = Sets.newHashSet();

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
}
