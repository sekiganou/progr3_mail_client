package progr3.mail.client.models;

import java.util.HashMap;

import progr3.mail.client.api.UserApi;
import progr3.mail.client.model.Message;
import progr3.mail.client.model.User;

public class UserStore {
    private static HashMap<String, User> userMap = new HashMap<>();
    private UserApi userApi;

    public UserStore(UserApi userApi) {
        this.userApi = userApi;
    }

    public User getUserById(String userId) {
        return userMap.get(userId);
    }

    public void updateUserCacheSync(Message[] messageList) {
        HashMap<String, Boolean> missingUserIds = new HashMap<>();
        for (Message msg : messageList) {
            if (!userMap.containsKey(msg.getSenderUserGUID())) {
                missingUserIds.put(msg.getSenderUserGUID(), true);
            }

            for (String recipientId : msg.getRecipientsUserGUIDs()) {
                if (!userMap.containsKey(recipientId)) {
                    missingUserIds.put(recipientId, true);
                }
            }
        }

        if (!missingUserIds.isEmpty()) {
            User[] users = userApi.getUsers(missingUserIds.keySet().stream().toList());
            if (users != null) {
                for (User user : users) {
                    userMap.put(user.getGuid(), user);
                }
            }
        }
    }

}
