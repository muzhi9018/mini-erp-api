package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.vault.VaultProtocol;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.MapperFeature;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/**
 * <p>
 * {@code VaultCodec}：严格协议编码与公开回执序列化，不执行条目解密
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public final class VaultCodec {
    private static final JsonMapper JSON = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

    private VaultCodec() { }

    public static UUID uuid(String value) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(value == null || !value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        return UUID.fromString(value);
    }

    public static byte[] decode(String value, int minimum, int maximum) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(value == null || value.length() > ((maximum + 2) / 3) * 4
                || !value.matches("[A-Za-z0-9_-]+"));
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            boolean invalid = bytes.length < minimum || bytes.length > maximum || !VaultCodec.encode(bytes).equals(value);
            VaultEnum.Error.INVALID_REQUEST.rejectIf(invalid);
            return bytes;
        } catch (IllegalArgumentException failure) {
            throw VaultEnum.Error.INVALID_REQUEST.exception();
        }
    }

    public static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static byte[] random(int length) {
        byte[] result = new byte[length];
        new SecureRandom().nextBytes(result);
        return result;
    }

    public static byte[] digest(Object value) {
        byte[] bytes = VaultCodec.json(value).getBytes(StandardCharsets.UTF_8);
        return VaultCodec.sha256(bytes);
    }

    public static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }

    public static String json(Object value) {
        return JSON.writeValueAsString(value);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> receipt(String value) {
        return JSON.readValue(value, Map.class);
    }

    public static long version(Long value) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(value == null || value < 1 || value > VaultProtocol.MAX_VERSION);
        return value;
    }
}
