package com.hotelos.identity.security;

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
public class TrustedGatewayIpResolver {

    private static final Logger log = LoggerFactory.getLogger(TrustedGatewayIpResolver.class);
    private static final Pattern IP_PATTERN = Pattern.compile("^[a-fA-F0-9.:]+$");

    private final List<String> configuredProxies;
    private final Set<String> staticIpMatches = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, CachedResolution> dnsCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5000L;

    private static class CachedResolution {
        final Set<String> ips;
        final long expiresAt;

        CachedResolution(Set<String> ips, long expiresAt) {
            this.ips = ips;
            this.expiresAt = expiresAt;
        }
    }

    public TrustedGatewayIpResolver(
            @Value("${hotelos.identity.trusted-proxies:gateway-service,gateway}") String trustedProxiesStr
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

    public boolean isTrustedGateway(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        String cleanRemote = remoteAddr.trim();
        if (staticIpMatches.contains(cleanRemote)) {
            return true;
        }
        long now = System.currentTimeMillis();
        for (String proxy : configuredProxies) {
            if (!staticIpMatches.contains(proxy)) {
                CachedResolution cached = dnsCache.get(proxy);
                if (cached != null && cached.expiresAt > now) {
                    if (cached.ips.contains(cleanRemote)) {
                        return true;
                    }
                } else {
                    try {
                        InetAddress[] addresses = InetAddress.getAllByName(proxy);
                        Set<String> resolvedIps = ConcurrentHashMap.newKeySet();
                        for (InetAddress addr : addresses) {
                            resolvedIps.add(addr.getHostAddress());
                        }
                        dnsCache.put(proxy, new CachedResolution(resolvedIps, now + CACHE_TTL_MS));
                        if (resolvedIps.contains(cleanRemote)) {
                            return true;
                        }
                    } catch (Exception e) {
                        if (cached != null && cached.ips.contains(cleanRemote)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedGateway(remoteAddr)) {
            String hotelOsClientIp = request.getHeader("X-HotelOS-Client-IP");
            if (hotelOsClientIp != null && !hotelOsClientIp.isBlank()) {
                String trimmed = hotelOsClientIp.trim();
                if (IP_PATTERN.matcher(trimmed).matches()) {
                    return trimmed;
                }
            }
        }
        return remoteAddr != null ? remoteAddr.trim() : "127.0.0.1";
    }
}
