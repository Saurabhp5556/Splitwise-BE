package splitwise.exception;

public class SplitwiseException extends RuntimeException {
    private final String errorCode;

    public SplitwiseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SplitwiseException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}