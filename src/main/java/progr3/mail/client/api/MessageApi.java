package progr3.mail.client.api;

import java.util.Date;
import java.util.List;

import progr3.mail.client.app.ApiHandler;
import progr3.mail.client.hooks.Auth;
import progr3.mail.client.model.Message;
import progr3.mail.client.model.MailRequest.ForwardMessageBody;
import progr3.mail.client.model.MailRequest.GetMessagesWithFiltersBody;
import progr3.mail.client.model.MailRequest.SendMessageBody;
import progr3.mail.client.model.Request.Command;

public class MessageApi {
    private ApiHandler api;

    public MessageApi(ApiHandler api) {
        this.api = api;
    }

    public Message[] getMessages() {
        return api.sendRequest(Command.GET_MESSAGES, Auth.getUserId(), Message[].class);
    }

    public Message[] getMessagesWithFilter(Date startDate, Date endDate) {
        GetMessagesWithFiltersBody body = new GetMessagesWithFiltersBody();
        body.setUserId(Auth.getUserId());
        body.setStartDate(startDate);
        body.setEndDate(endDate);

        return api.sendRequest(Command.GET_MESSAGES_WITH_FILTERS, body, Message[].class);
    }

    // public String replyToMessage(String messageId, String subject, String body) {
    // ReplySingleMessageBody requestBody = new ReplySingleMessageBody();
    // requestBody.setSenderUserId(Auth.getUserId());
    // requestBody.setMessageId(messageId);
    // requestBody.setSubject(subject);
    // requestBody.setBody(body);

    // return api.sendRequest(Command.REPLY_SINGLE_MESSAGE, requestBody,
    // String.class);
    // }

    public String sendMessage(List<String> recipientUserEmails, String subject, String body) {
        SendMessageBody requestBody = new SendMessageBody();
        requestBody.setSenderUserId(Auth.getUserId());
        requestBody.setRecipientsUserEmails(recipientUserEmails);
        requestBody.setSubject(subject);
        requestBody.setBody(body);

        return api.sendRequest(Command.SEND_MESSAGE, requestBody, String.class);
    }

    public String forwardMessage(String messageId, List<String> recipientUserEmails) {
        ForwardMessageBody requestBody = new ForwardMessageBody();
        requestBody.setForwarderUserId(Auth.getUserId());
        requestBody.setMessageId(messageId);
        requestBody.setRecipientsUserEmails(recipientUserEmails);

        return api.sendRequest(Command.FORWARD_MESSAGE, requestBody, String.class);
    }

    public String deleteMessage(String messageId) {
        return api.sendRequest(Command.DELETE_MESSAGE, messageId, String.class);
    }

}
