package progr3.mail.client.api;

import java.util.List;

import progr3.mail.client.model.Request.Command;
import progr3.mail.client.model.User;

public class UserApi {

    private ApiHandler api;

    public UserApi(ApiHandler api) {
        this.api = api;
    }

    public User login(String email) {
        var user = api.sendRequest(Command.LOGIN, email, User.class);
        return user;
    }

    public User[] getUsers(List<String> userIds) {
        return api.sendRequest(Command.GET_USERS, userIds, User[].class);
    }
}
