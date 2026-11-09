package com.pet.vetalya.medshop.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
public class StoreJwtConfig {

    private static final Logger log = LoggerFactory.getLogger(StoreJwtConfig.class);

    public static final String STORE_ISSUER = "medshop";
    public static final String STORE_AUDIENCE = "medshop-api";

    @Value("${app.store.jwt.private-key-path:classpath:keys/medshop_private.pem}")
    private String privateKeyPath;

    @Value("${app.store.jwt.public-key-path:classpath:keys/medshop_public.pem}")
    private String publicKeyPath;

    private final ResourceLoader resourceLoader;

    public StoreJwtConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean(name = "storeJwtKeyPair")
    public KeyPair storeJwtKeyPair() {
        try {
            RSAPrivateKey privateKey = loadPrivateKey(privateKeyPath);
            RSAPublicKey publicKey = loadPublicKey(publicKeyPath);
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            log.warn("MedShop PEM keys not found at configured paths ({}, {}). Generating in-memory RSA keypair.",
                    privateKeyPath, publicKeyPath);
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                return generator.generateKeyPair();
            } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException("Failed to generate RSA key pair", ex);
            }
        }
    }

    @Bean(name = "storeJwtEncoder")
    public JwtEncoder storeJwtEncoder(KeyPair storeJwtKeyPair) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) storeJwtKeyPair.getPublic())
                .privateKey((RSAPrivateKey) storeJwtKeyPair.getPrivate())
                .keyID("medshop-jwt-key")
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean(name = "storeJwtDecoder")
    public JwtDecoder storeJwtDecoder(KeyPair storeJwtKeyPair) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) storeJwtKeyPair.getPublic()).build();

        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(STORE_ISSUER);
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(STORE_AUDIENCE);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator));

        return decoder;
    }

    private RSAPrivateKey loadPrivateKey(String path) throws Exception {
        Resource resource = resourceLoader.getResource(path);
        try (InputStream is = resource.getInputStream()) {
            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        }
    }

    private RSAPublicKey loadPublicKey(String path) throws Exception {
        Resource resource = resourceLoader.getResource(path);
        try (InputStream is = resource.getInputStream()) {
            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String base64 = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
        }
    }

    public static class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String expectedAudience;

        public AudienceValidator(String expectedAudience) {
            this.expectedAudience = expectedAudience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            List<String> audiences = token.getAudience();
            if (audiences != null && audiences.contains(expectedAudience)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "The required audience '" + expectedAudience + "' is missing", null));
        }
    }
}

