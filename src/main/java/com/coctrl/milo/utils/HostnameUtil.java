package com.coctrl.milo.utils;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.util.*;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @desc
 * @since 2020/4/13
 */
@Slf4j
public class HostnameUtil {
    public HostnameUtil() {
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
}
