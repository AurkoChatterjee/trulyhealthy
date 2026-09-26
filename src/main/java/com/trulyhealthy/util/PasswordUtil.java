package com.trulyhealthy.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Salted SHA-256 password hashing. Stored format: "<saltHex>$<hashHex>".
 * A dedicated password-hashing algorithm (bcrypt/argon2) would be preferable
 * for a production system — this is a deliberate, documented simplification
 * to avoid an extra native/heavy dependency in a portfolio project.
 */
public final class PasswordUtil {

    private static final SecureRandom RNG = new SecureRandom();

    private PasswordUtil() {}

    public static String hash(String rawPassword) {
        byte[] saltBytes = new byte[8];
        RNG.nextBytes(saltBytes);
        String salt = HexFormat.of().formatHex(saltBytes);
        return salt + "$" + sha256(salt + rawPassword);
    }

    public static boolean verify(String rawPassword, String stored) {
        if (stored == null || !stored.contains("$")) return false;
        String[] parts = stored.split("\\$", 2);
        String salt = parts[0];
        String expectedHash = parts[1];
        return sha256(salt + rawPassword).equals(expectedHash);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(input.getBytes("UTF-8"));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
