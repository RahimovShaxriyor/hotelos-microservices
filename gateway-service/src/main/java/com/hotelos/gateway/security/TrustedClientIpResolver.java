package com.hotelos.gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class TrustedClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(TrustedClientIpResolver.class);
    private static final Pattern IP_PATTERN = Pattern.compile("^[a-fA-F0-9.:]+$");

    private final List<String> configuredProxies;
    private final Set<String> staticIpMatches = ConcurrentHashMap.newKeySet();

    public TrustedClientIpResolver(
            @Value("${hotelos.gateway.trusted-proxies:edge,nginx}") String trustedProxiesStr
    ) {
        this.configuredProxies = Arrays.stream(trustedProxiesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        for (String proxy : configuredProxies) {
            if (proxy.matches("^[0-9.]+$") || proxy.contains(":")) {
                staticIpMatches.add(proxy);
            }
        }
    }

    public boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        String cleanRemote = remoteAddr.trim();
        if (staticIpMatches.contains(cleanRemote)) {
            return true;
        }
        for (String proxy : configuredProxies) {
            if (!staticIpMatches.contains(proxy)) {
                try {
                    InetAddress[] addresses = InetAddress.getAllByName(proxy);
                    for (InetAddress addr : addresses) {
                        if (cleanRemote.equals(addr.getHostAddress())) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    // Ignored: host not resolvable in current environment
                }
            }
        }
        return false;
    }

    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank() && IP_PATTERN.matcher(realIp.trim()).matches()) {
                return realIp.trim();
            }
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String firstIp = xff.split(",")[0].trim();
                if (IP_PATTERN.matcher(firstIp).matches()) {
                    return firstIp;
                }
            }
        }
        return remoteAddr != null ? remoteAddr.trim() : "127.0.0.1";
    }
}
