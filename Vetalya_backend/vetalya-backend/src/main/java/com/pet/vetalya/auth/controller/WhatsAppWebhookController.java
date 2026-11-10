package com.pet.vetalya.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Handles Meta WhatsApp Cloud API webhook verification (GET) and event delivery (POST).
 * Mapped at both /api/public/whatsapp and /api/auth/whatsapp so it works
 * regardless of which security filter chain matches first.
 *
 * Both paths are explicitly permitAll() in ApiSecurityConfig.
 */
@RestController
@RequestMapping({"/api/public/whatsapp", "/api/auth/whatsapp"})
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    @Value("${app.whatsapp.verify-token:vetalya_whatsapp_secret}")
    private String expectedVerifyToken;

    /**
     * Meta Webhook Handshake Verification (GET).
     * Meta calls this with hub.mode=subscribe, hub.verify_token, and hub.challenge.
     * We echo back hub.challenge as plain text with HTTP 200 to confirm ownership.
     * Also handles trailing-slash variant (/webhook/) via Spring's suffix matching.
     */
    @GetMapping(value = {"/webhook", "/webhook/"}, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        log.info("🔍 [WhatsApp Webhook] Handshake received — mode={}, token_provided={}", mode, verifyToken != null);

        if ("subscribe".equals(mode) && expectedVerifyToken.equals(verifyToken)) {
            log.info("✅ [WhatsApp Webhook] Verified! Returning challenge: {}", challenge);
            return ResponseEntity.ok(challenge);
        }

        log.warn("❌ [WhatsApp Webhook] Validation FAILED — mode={}, token_match={}", mode,
                expectedVerifyToken.equals(verifyToken));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    /**
     * Meta Webhook Events Receiver (POST).
     * Receives inbound messages, delivery receipts (sent/delivered/read), and status updates.
     * Always return 200 immediately — heavy processing should be async.
     */
    @PostMapping(value = {"/webhook", "/webhook/"})
    public ResponseEntity<String> handleWebhookEvent(@RequestBody String payload) {
        log.info("📩 [WhatsApp Webhook] Event received: {}", payload);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}

