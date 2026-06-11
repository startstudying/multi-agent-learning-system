package com.learningos.agent.application;

import com.learningos.config.AppProperties;
import com.learningos.config.AuthProperties;
import com.learningos.config.ModelProviderProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ModelProviderSecretCodec {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final ModelProviderProperties properties;
    private final AuthProperties authProperties;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public ModelProviderSecretCodec(
            ModelProviderProperties properties,
            AuthProperties authProperties,
            AppProperties appProperties
    ) {
        this.properties = properties;
        this.authProperties = authProperties;
        this.appProperties = appProperties;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("API key is required");
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("MODEL_PROVIDER_SECRET_ENCODE_FAILED");
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new IllegalArgumentException("Encrypted API key is required");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(ciphertext);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("MODEL_PROVIDER_SECRET_DECODE_FAILED");
        }
    }

    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 8) {
            return "***";
        }
        return trimmed.substring(0, 3) + "***" + trimmed.substring(trimmed.length() - 2);
    }

    private SecretKey secretKey() {
        return new SecretKeySpec(deriveKeyMaterial(), "AES");
    }

    private byte[] deriveKeyMaterial() {
        String configured = properties.encryptionKey();
        if (configured != null && !configured.isBlank()) {
            return sha256(configured.trim());
        }
        if (isProductionLike()) {
            throw new IllegalStateException("MODEL_PROVIDER_ENCRYPTION_KEY is required in production-like environments");
        }
        String fallback = authProperties.jwtSecret();
        if (fallback == null || fallback.isBlank()) {
            fallback = "local-development-only-secret-32b";
        }
        return sha256(fallback);
    }

    private boolean isProductionLike() {
        String environment = appProperties.environment();
        return "prod".equalsIgnoreCase(environment)
                || "production".equalsIgnoreCase(environment)
                || "staging".equalsIgnoreCase(environment);
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("MODEL_PROVIDER_SECRET_KEY_DERIVATION_FAILED");
        }
    }
}
