package splitwise.util;

import splitwise.exception.SplitwiseException;

import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^\\+?[1-9]\\d{1,14}$");

    public static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new SplitwiseException("Invalid email format: " + email, "VALIDATION_ERROR");
        }
    }

    public static void validatePhone(String phone) {
        if (phone != null && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new SplitwiseException("Invalid phone format: " + phone, "VALIDATION_ERROR");
        }
    }

    public static void validateAmount(double amount) {
        if (amount <= 0) {
            throw new SplitwiseException("Amount must be positive", "VALIDATION_ERROR");
        }
        if (amount > 1_000_000) {
            throw new SplitwiseException("Amount exceeds maximum limit of 1,000,000", "VALIDATION_ERROR");
        }
    }

    public static void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new SplitwiseException(fieldName + " cannot be null", "VALIDATION_ERROR");
        }
    }

    public static void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new SplitwiseException(fieldName + " cannot be empty", "VALIDATION_ERROR");
        }
    }
}