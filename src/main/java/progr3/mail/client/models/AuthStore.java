package progr3.mail.client.models;

import javafx.application.Platform;
import progr3.mail.client.api.UserApi;
import progr3.mail.client.model.User;

public class AuthStore {
    private static User user;
    private UserApi userApi;
    private static boolean isFirstLogin = true;

    public AuthStore(UserApi userApi) {
        this.userApi = userApi;
    }

    public interface AuthCallback {
        void onSuccess();

        void onFailure();
    }

    public static boolean isFirstLogin() {
        return isFirstLogin;
    }

    public static boolean setIsNotFirstLogin() {
        return isFirstLogin = false;
    }

    public void loginAsync(String email, AuthCallback callback) {
        new Thread(() -> {
            user = userApi.login(email);

            Platform.runLater(() -> {
                if (user != null) {
                    NotificationManager.show("Login successful! Welcome " +
                            (user.getName() != null ? user.getName() : user.getEmail()),
                            Status.SUCCESS);

                    callback.onSuccess();
                } else {
                    NotificationManager.show("Login failed. Please try again.", Status.ERROR);
                    StatusManager.setStatus("Login failed. Please try again.", Status.ERROR);

                    callback.onFailure();
                }
            });
        }).start();
    }

    public boolean logout() {
        isFirstLogin = true;

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
