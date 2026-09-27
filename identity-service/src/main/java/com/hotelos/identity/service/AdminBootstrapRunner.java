package com.hotelos.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private final AdminBootstrapService bootstrapService;
    private final String bootstrapUsername;
    private final String bootstrapPassword;

    public AdminBootstrapRunner(
            AdminBootstrapService bootstrapService,
            @Value("${hotelos.bootstrap.admin.username:}") String bootstrapUsername,
            @Value("${hotelos.bootstrap.admin.password:}") String bootstrapPassword
    ) {
        this.bootstrapService = bootstrapService;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(String... args) {
        bootstrapService.executeBootstrap(bootstrapUsername, bootstrapPassword);
    }
}
