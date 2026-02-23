package com.example.myapplication.util;

import android.text.TextUtils;
import java.util.regex.Pattern;

/**
 * Validation utilities for forms and inputs
 */
public class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9]{10,13}$"
    );
    
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$"
    );
    
    /**
     * Validate email address
     */
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validate phone number
     */
    public static boolean isValidPhone(String phone) {
        if (TextUtils.isEmpty(phone)) return false;
        String cleanPhone = phone.replaceAll("[\\s()-]", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }
    
    /**
     * Validate URL
     */
    public static boolean isValidUrl(String url) {
        return !TextUtils.isEmpty(url) && URL_PATTERN.matcher(url).matches();
    }
    
    /**
     * Validate name (at least 2 characters)
     */
    public static boolean isValidName(String name) {
        return !TextUtils.isEmpty(name) && name.trim().length() >= 2;
    }
    
    /**
     * Validate password strength
     */
    public static boolean isStrongPassword(String password) {
        if (TextUtils.isEmpty(password) || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        
        return hasUpper && hasLower && hasDigit;
    }
    
    /**
     * Get password strength message
     */
    public static String getPasswordStrengthMessage(String password) {
        if (TextUtils.isEmpty(password)) {
            return "Password required";
        }
        
        if (password.length() < 8) {
            return "Password must be at least 8 characters";
        }
        
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()].*");
        
        int strength = 0;
        if (hasUpper) strength++;
        if (hasLower) strength++;
        if (hasDigit) strength++;
        if (hasSpecial) strength++;
        
        if (strength >= 4) return "Strong password";
        if (strength >= 3) return "Medium strength";
        return "Weak password - add uppercase, lowercase, and numbers";
    }
    
    /**
     * Validate that string is not empty
     */
    public static boolean isNotEmpty(String text) {
        return !TextUtils.isEmpty(text) && !text.trim().isEmpty();
    }
    
    /**
     * Validate minimum length
     */
    public static boolean hasMinLength(String text, int minLength) {
        return !TextUtils.isEmpty(text) && text.trim().length() >= minLength;
    }
}
