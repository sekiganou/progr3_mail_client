package progr3.mail.client.hooks;

import progr3.mail.client.model.User;

public class Auth {
    private static User user;

    public static void setUser(User user) {
        Auth.user = user;
    }

    public static String getUserId() {
        if (Auth.isAuthenticated())
            return Auth.user.getGuid();
        return null;
    }

    public static User getUser() {
        if (Auth.isAuthenticated())
            return Auth.user;
        return null;
    }

    public static boolean isAuthenticated() {
        return Auth.user != null;
    }

    public static void clearAuth() {
        Auth.user = null;
    }
}
