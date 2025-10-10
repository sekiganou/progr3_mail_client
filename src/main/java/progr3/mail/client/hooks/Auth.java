package progr3.mail.client.hooks;

import progr3.mail.client.model.User;

public class Auth {
    private static User user;

    public static void setUser(User user) {
        Auth.user = user;
    }

    public static String getUserId() {
        return Auth.user.getGuid();
    }

    public static User getUser() {
        return Auth.user;
    }

    public static boolean isAuthenticated() {
        return Auth.user != null;
    }

    public static void clearAuth() {
        Auth.user = null;
    }
}
