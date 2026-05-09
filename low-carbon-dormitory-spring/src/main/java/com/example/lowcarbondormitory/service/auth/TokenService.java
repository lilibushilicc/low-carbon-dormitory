package com.example.lowcarbondormitory.service.auth;

import com.example.lowcarbondormitory.common.AuthException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long ttlSeconds;

    public TokenService(
            ObjectMapper objectMapper,
            @Value("${app.auth.token-secret:low-carbon-dormitory-token-secret}") String secret,
            @Value("${app.auth.token-ttl-seconds:86400}") long ttlSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public String createStudentToken(String stuNum) {
        return createToken("STUDENT", stuNum, Map.of("stuNum", stuNum));
    }

    public String createAdminToken(Long adminId, String username) {
        return createToken("ADMIN", String.valueOf(adminId), Map.of("adminId", adminId, "username", username));
    }

    public TokenPayload verify(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthException("缺少登录 token");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new AuthException("登录 token 格式无效");
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new AuthException("登录 token 签名无效");
        }

        Map<String, Object> payload = parseJson(parts[1]);
        String role = stringValue(payload.get("role"));
        String subject = stringValue(payload.get("sub"));
        long expiresAt = longValue(payload.get("exp"));
        if (expiresAt <= Instant.now().getEpochSecond()) {
            throw new AuthException("登录 token 已过期");
        }
        return new TokenPayload(role, subject, payload);
    }

    private String createToken(String role, String subject, Map<String, Object> claims) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("role", role);
        payload.put("sub", subject);
        payload.put("iat", now);
        payload.put("exp", now + ttlSeconds);
        payload.putAll(claims);

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signingInput = encodedHeader + "." + encodedPayload;
        return signingInput + "." + sign(signingInput);
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("生成登录 token 失败", exception);
        }
    }

    private Map<String, Object> parseJson(String encodedJson) {
        try {
            byte[] bytes = URL_DECODER.decode(encodedJson);
            return objectMapper.readValue(bytes, MAP_TYPE);
        } catch (Exception exception) {
            throw new AuthException("登录 token 内容无效");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("登录 token 签名失败", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }
        int result = 0;
        for (int index = 0; index < expectedBytes.length; index++) {
            result |= expectedBytes[index] ^ actualBytes[index];
        }
        return result == 0;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception exception) {
            throw new AuthException("登录 token 过期时间无效");
        }
    }

    public record TokenPayload(String role, String subject, Map<String, Object> claims) {
    }
}
