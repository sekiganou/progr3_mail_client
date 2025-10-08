package progr3.mail.client.api;

import progr3.mail.client.app.ApiHandler;
import progr3.mail.client.hooks.Auth;
import progr3.mail.client.model.Message;
import progr3.mail.client.model.Request.Command;

public class MessageApi {
    private ApiHandler api;

    public MessageApi(ApiHandler api) {
        this.api = api;
    }

    public Message[] getMessagesForUser() {
        return api.sendRequest(Command.GET_MESSAGES, Auth.getUserId(), Message[].class);
    }

}
