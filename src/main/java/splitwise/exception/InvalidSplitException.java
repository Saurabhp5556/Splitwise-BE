package splitwise.exception;

public class InvalidSplitException extends SplitwiseException {
    public InvalidSplitException(String message) {
        super(message, "INVALID_SPLIT");
    }
}