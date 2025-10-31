package progr3.mail.client.models;

import progr3.mail.client.api.UserApi;
import progr3.mail.client.model.User;

public class AuthStore {
    private static User user;
    private UserApi userApi;

    public AuthStore(UserApi userApi) {
        this.userApi = userApi;
    }

    public interface AuthCallback {
        void onSuccess(User user);

        void onFailure();
    }

    public void login(String email, AuthCallback callback) {
        new Thread(() -> {
            user = userApi.login(email);
            if (user != null) {
                callback.onSuccess(user);
            } else {
                callback.onFailure();
            }
        }).start();
    }

    public boolean logout() {
        if (AuthStore.isAuthenticated())
            AuthStore.clearAuth();

        return !AuthStore.isAuthenticated();
    }

    public static String getUserId() {
        if (AuthStore.isAuthenticated())
            return AuthStore.user.getGuid();
        return null;
    }

    public static User getUser() {
        if (AuthStore.isAuthenticated())
            return AuthStore.user;
        return null;
    }

    public static boolean isAuthenticated() {
        return user != null;
    }

    public static void clearAuth() {
        AuthStore.user = null;
    }
}
