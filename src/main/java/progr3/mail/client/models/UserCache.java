package progr3.mail.client.models;

import java.util.HashMap;

import progr3.mail.client.api.UserApi;
import progr3.mail.client.model.Message;
import progr3.mail.client.model.User;

public class UserCache {
    private HashMap<String, User> userCache = new HashMap<>();
    private UserApi userApi;

    public UserCache(UserApi userApi) {
        this.userApi = userApi;
    }

    public User getUserById(String userId) {
        return userCache.get(userId);
    }

    public void updateUserCache(Message[] messageList) {
        HashMap<String, Boolean> missingUserIds = new HashMap<>();
        for (Message msg : messageList) {
            if (!userCache.containsKey(msg.getSenderUserGUID())) {
                missingUserIds.put(msg.getSenderUserGUID(), true);
            }

            for (String recipientId : msg.getRecipientsUserGUIDs()) {
                if (!userCache.containsKey(recipientId)) {
                    missingUserIds.put(recipientId, true);
                }
            }
        }

        if (!missingUserIds.isEmpty()) {
            User[] users = userApi.getUsers(missingUserIds.keySet().stream().toList());
            if (users != null) {
                for (User user : users) {
                    userCache.put(user.getGuid(), user);
                }
            }
        }
    }

}
