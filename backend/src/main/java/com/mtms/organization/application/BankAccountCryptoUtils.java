package com.mtms.organization.application;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Utility for masking and encrypting sensitive bank account data at rest.
 */
public final class BankAccountCryptoUtils {

    private static final String DEFAULT_SECRET = "mtms_bank_account_encryption_secret_key_2026";
    private static final SecretKeySpec SECRET_KEY_SPEC;

    static {
        try {
            byte[] key = DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // 128-bit AES key
            SECRET_KEY_SPEC = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private BankAccountCryptoUtils() {
    }

    /**
     * Produces masked bank account number (e.g. "**** **** 5678").
     */
    public static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return "****";
        }
        String clean = accountNumber.replaceAll("\\s+", "");
        if (clean.length() <= 4) {
            return "**** " + clean;
        }
        String last4 = clean.substring(clean.length() - 4);
        return "**** **** " + last4;
    }

    /**
     * Encrypts bank account number to Base64 ciphertext.
     */
    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY_SPEC);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt bank account number", e);
        }
    }

    /**
     * Decrypts Base64 ciphertext back to plaintext bank account number.
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY_SPEC);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt bank account number", e);
        }
    }
}
