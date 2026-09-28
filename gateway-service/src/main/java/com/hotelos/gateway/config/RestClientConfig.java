package com.hotelos.gateway.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Configuration
public class RestClientConfig {

    private static final List<String> SPOOFED_HEADERS = List.of(
            "X-User", "X-Username", "X-Role", "X-Roles",
            "X-Authenticated-User", "X-Internal-User", "X-Internal-Role"
    );

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RestClientConfig.class);

    @Bean
    public ClientHttpRequestInterceptor authHeaderForwardingInterceptor() {
        return (request, body, execution) -> {
            for (String spoofed : SPOOFED_HEADERS) {
                request.getHeaders().remove(spoofed);
            }
            if (!request.getURI().getPath().startsWith("/api/auth")) {
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
                    if (attributes instanceof ServletRequestAttributes servletAttributes) {
                        HttpServletRequest servletRequest = servletAttributes.getRequest();
                        String authHeader = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            request.getHeaders().set(HttpHeaders.AUTHORIZATION, authHeader);
                        }
                    }
                }
            }
            return execution.execute(request, body);
        };
    }

    private RestClient createClient(RestClient.Builder builder, String baseUrl, ClientHttpRequestInterceptor interceptor) {
        return builder.requestFactory(new JdkClientHttpRequestFactory())
                .baseUrl(baseUrl)
                .requestInterceptor(interceptor)
                .build();
    }

    @Bean
    public RestClient identityClient(RestClient.Builder builder, ServiceUrls serviceUrls, ClientHttpRequestInterceptor interceptor) {
        return createClient(builder, serviceUrls.identityUrl(), interceptor);
    }

    @Bean
    public RestClient receptionClient(RestClient.Builder builder, ServiceUrls serviceUrls, ClientHttpRequestInterceptor interceptor) {
        return createClient(builder, serviceUrls.receptionUrl(), interceptor);
    }

    @Bean
    public RestClient housekeepingClient(RestClient.Builder builder, ServiceUrls serviceUrls, ClientHttpRequestInterceptor interceptor) {
        return createClient(builder, serviceUrls.housekeepingUrl(), interceptor);
    }

    @Bean
    public RestClient roomServiceClient(RestClient.Builder builder, ServiceUrls serviceUrls, ClientHttpRequestInterceptor interceptor) {
        return createClient(builder, serviceUrls.roomServiceUrl(), interceptor);
    }

    @Bean
    public RestClient maintenanceClient(RestClient.Builder builder, ServiceUrls serviceUrls, ClientHttpRequestInterceptor interceptor) {
        return createClient(builder, serviceUrls.maintenanceUrl(), interceptor);
    }

    @Bean
    public RestClient dashboardClient(RestClient.Builder builder, ServiceUrls serviceUrls, ClientHttpRequestInterceptor interceptor) {
        return createClient(builder, serviceUrls.dashboardUrl(), interceptor);
    }
}
