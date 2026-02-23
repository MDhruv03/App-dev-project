package com.example.myapplication.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Security utilities for encryption and hashing
 */
public class SecurityUtils {
    
    private static final String ALGORITHM_SHA256 = "SHA-256";
    private static final String ALGORITHM_MD5 = "MD5";
    
    /**
     * Generate a secure random string
     */
    public static String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }
    
    /**
     * Hash a password using SHA-256
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM_SHA256);
            byte[] hash = digest.digest(password.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    
    /**
     * Verify password against hash
     */
    public static boolean verifyPassword(String password, String hash) {
        String passwordHash = hashPassword(password);
        return passwordHash.equals(hash);
    }
    
    /**
     * Generate MD5 hash (for non-security purposes like cache keys)
     */
    public static String generateMD5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM_MD5);
            byte[] hash = digest.digest(input.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
    
    /**
     * Convert byte array to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Generate a session token
     */
    public static String generateSessionToken() {
        return generateRandomString(32);
    }
    
    /**
     * Sanitize input to prevent injection
     */
    public static String sanitizeInput(String input) {
        if (input == null) return null;
        
        // Remove potentially harmful characters
        return input.replaceAll("[<>\"']", "")
                   .trim();
    }
}
