package com.hotelos.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class OwnerPromotionRunner implements CommandLineRunner {

    private final OwnerPromotionService ownerPromotionService;
    private final String promoteUsername;

    public OwnerPromotionRunner(
            OwnerPromotionService ownerPromotionService,
            @Value("${HOTELOS_GOVERNANCE_PROMOTE_OWNER_USERNAME:${hotelos.governance.promote-owner-username:}}") String promoteUsername
    ) {
        this.ownerPromotionService = ownerPromotionService;
        this.promoteUsername = promoteUsername;
    }

    @Override
    public void run(String... args) {
        ownerPromotionService.promoteOwnerIfConfigured(promoteUsername);
    }
}
