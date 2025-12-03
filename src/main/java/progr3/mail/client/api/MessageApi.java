package progr3.mail.client.api;

import java.util.Date;
import java.util.List;

import progr3.mail.client.model.Message;
import progr3.mail.client.model.MailRequest.DeleteMessageBody;
import progr3.mail.client.model.MailRequest.GetMessagesWithFiltersBody;
import progr3.mail.client.model.MailRequest.SendMessageBody;
import progr3.mail.client.model.Request.Command;
import progr3.mail.client.models.AuthStore;

public class MessageApi {
    private ApiHandler api;

    public MessageApi(ApiHandler api) {
        this.api = api;
    }

    public Message[] getMessages() {
        return api.sendRequest(Command.GET_MESSAGES, AuthStore.getUserId(), Message[].class);
    }

    public Message[] getMessagesWithFilter(Date startDate, Date endDate) {
        GetMessagesWithFiltersBody body = new GetMessagesWithFiltersBody();
        body.setUserId(AuthStore.getUserId());
        body.setStartDate(startDate);
        body.setEndDate(endDate);

        return api.sendRequest(Command.GET_MESSAGES_WITH_FILTERS, body, Message[].class);
    }

    public String sendMessage(List<String> recipientUserEmails, String subject, String body) {
        SendMessageBody requestBody = new SendMessageBody();
        requestBody.setSenderUserId(AuthStore.getUserId());
        requestBody.setRecipientsUserEmails(recipientUserEmails);
        requestBody.setSubject(subject);
        requestBody.setBody(body);

        return api.sendRequest(Command.SEND_MESSAGE, requestBody, String.class);
    }

    public String deleteMessage(String messageId) {
        DeleteMessageBody requestBody = new DeleteMessageBody();
        requestBody.setUserId(AuthStore.getUserId());
        requestBody.setMessageId(messageId);
        return api.sendRequest(Command.DELETE_MESSAGE, requestBody, String.class);
    }

}
