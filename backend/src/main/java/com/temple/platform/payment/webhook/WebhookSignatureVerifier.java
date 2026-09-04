package com.temple.platform.payment.webhook;

import com.temple.platform.payment.config.MockPaymentProperties;
import com.temple.platform.payment.exception.InvalidWebhookSignatureException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class WebhookSignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    private final MockPaymentProperties properties;

    public WebhookSignatureVerifier(MockPaymentProperties properties) {
        this.properties = properties;
    }

    public void verify(byte[] rawBody, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new InvalidWebhookSignatureException();
        }
        String secret = properties.webhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new InvalidWebhookSignatureException();
        }
        String expected = PREFIX + hmacSha256Hex(secret, rawBody);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureHeader.trim().getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidWebhookSignatureException();
        }
    }

    public String sign(byte[] rawBody) {
        String secret = properties.webhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Mock payment webhook secret is not configured");
        }
        return PREFIX + hmacSha256Hex(secret, rawBody);
    }

    private static String hmacSha256Hex(String secret, byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(payload));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute webhook signature", ex);
        }
    }
}
