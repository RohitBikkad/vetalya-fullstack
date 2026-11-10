package com.pet.vetalya.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    @Value("${app.whatsapp.verify-token:${WHATSAPP_VERIFY_TOKEN:vetalya_whatsapp_secret}}")
    private String expectedVerifyToken;

    /**
     * Meta Webhook Handshake Verification.
     * When configuring the webhook in Meta Developers Dashboard, Meta sends a GET request
     * with hub.mode, hub.verify_token, and hub.challenge.
     */
    @GetMapping(value = "/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        log.info("🔍 [WhatsApp Webhook Handshake] mode={}, token={}", mode, verifyToken);

        if ("subscribe".equals(mode) && expectedVerifyToken != null && expectedVerifyToken.equals(verifyToken)) {
            log.info("✅ [WhatsApp Webhook Handshake] Verified successfully! Returning challenge.");
            return ResponseEntity.ok(challenge);
        }

        log.warn("❌ [WhatsApp Webhook Handshake] Failed validation. Expected token: {}", expectedVerifyToken);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    /**
     * Meta Webhook Events Receiver.
     * Receives message delivery receipts (sent, delivered, read) and inbound messages.
     */
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleWebhookEvent(@RequestBody String payload) {
        log.info("📩 [WhatsApp Webhook Event Received]: {}", payload);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
