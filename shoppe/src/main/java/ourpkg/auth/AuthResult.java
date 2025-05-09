package ourpkg.auth;

import ourpkg.user_role_permission.user.User;

public class AuthResult {
    private final boolean success;
    private final User user;
    private final String errorCode;

    private AuthResult(boolean success, User user, String errorCode) {
        this.success = success;
        this.user = user;
        this.errorCode = errorCode;
    }

    public static AuthResult success(User user) {
        return new AuthResult(true, user, null);
    }

    public static AuthResult error(String errorCode) {
        return new AuthResult(false, null, errorCode);
    }

    public boolean isSuccess() {
        return success;
    }

    public User getUser() {
        return user;
    }

    public String getErrorCode() {
        return errorCode;
    }
}