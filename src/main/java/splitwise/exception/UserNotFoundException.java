package splitwise.exception;

public class UserNotFoundException extends SplitwiseException {
    public UserNotFoundException(String userId) {
        super("User not found: " + userId, "USER_NOT_FOUND");
    }
}