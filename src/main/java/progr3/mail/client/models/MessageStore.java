package progr3.mail.client.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import progr3.mail.client.api.MessageApi;
import progr3.mail.client.model.Message;

public class MessageStore {
    private static ObservableList<Message> messageList = FXCollections.observableArrayList();
    private static Map<String, Message> messageMap = new ConcurrentHashMap<>();
    private MessageApi messageApi;
    private static final String IS_NEW = "isNew";

    public MessageStore(MessageApi messageApi) {
        this.messageApi = messageApi;
    }

    public interface LoadCallback {
        void onSuccess(int messageCount);

        void onFailure();
    }

    public interface DeleteCallback {
        void onSuccess();

        void onFailure();
    }

    public interface SendCallback {
        void onSuccess();

        void onFailure();
    }

    public static ObservableList<Message> getMessageList() {
        return messageList;
    }

    public static int getMessageCount() {
        return messageList.size();
    }

    public static int getNewMessageCount() {
        return messageList.stream().mapToInt(msg -> isNew(msg) ? 1 : 0).sum();
    }

    public Message setIsNotNew(Message msg) {
        return setIsNew(msg, false);
    }

    private Date getLatestMessageDate() {
        return messageList.stream()
                .map(Message::getDate)
                .max(new Comparator<Date>() {
                    @Override
                    public int compare(Date o1, Date o2) {
                        return o1.compareTo(o2);
                    }
                })
                .orElse(new Date(0));
    }

    public static boolean isNew(Message msg) {
        if (msg.getAdditionalProperties().containsKey(IS_NEW)) {
            Object isNewObj = msg.getAdditionalProperties().get(IS_NEW);
            if (isNewObj instanceof Boolean) {
                return (Boolean) isNewObj;
            }
        }
        return false;
    }

    private Message setIsNew(Message msg, boolean isNew) {
        msg.setAdditionalProperty(IS_NEW, isNew);
        return msg;
    }

    public void loadMessagesAsync(LoadCallback callback) {
        if (!AuthStore.isAuthenticated())
            return;

        Platform.runLater(() -> {
            StatusManager.setStatus("Loading messages...", Status.INFO);
        });

        new Thread(() -> {
            if (AuthStore.isFirstLogin()) {
                messageMap.clear();
            }

            Message[] messages = AuthStore.isFirstLogin() ? messageApi
                    .getMessages() : messageMap.values().toArray(new Message[0]);

            if (AuthStore.isFirstLogin()) {
                AuthStore.setIsNotFirstLogin();
            }

            Platform.runLater(() -> {
                if (messages == null) {
                    StatusManager.setStatus("Failed to load messages", Status.ERROR);
                    NotificationManager.show("Failed to load messages", Status.ERROR);

                    callback.onFailure();
                    return;
                }

                messageList.clear();
                for (Message message : messages) {
                    message = setIsNew(message, false);
                    if (!messageMap.containsKey(message.getGuid())) {
                        messageMap.put(message.getGuid(), message);
                    }
                }
                messageList.addAll(messages);

                int messageCount = messages.length;
                StatusManager.setStatus("Loaded " + messageCount + " message(s)", Status.SUCCESS);
                callback.onSuccess(messageCount);
            });
        }).start();
    }

    private void loadNewMessages(LoadCallback callback) {
        var latestTimestamp = getLatestMessageDate();

        Message[] messages = messageApi.getMessagesWithFilter(latestTimestamp, new Date());

        Platform.runLater(() -> {
            if (messages == null) {
                StatusManager.setStatus("Failed to load new messages", Status.ERROR);
                NotificationManager.show("Failed to load new messages", Status.ERROR);

                callback.onFailure();
                return;
            }

            if (messages.length == 0) {
                StatusManager.setStatus("No new messages", Status.INFO);
                callback.onSuccess(0);
                return;
            }

            for (Message message : messages) {
                message = setIsNew(message, true);
                messageList.add(0, message);
                messageMap.put(message.getGuid(), message);
            }

            int messageCount = messages.length;
            StatusManager.setStatus("Loaded " + messageCount + " new message(s)", Status.SUCCESS);
            NotificationManager.show("Loaded " + messageCount + " new message(s)",
                    Status.SUCCESS);
            callback.onSuccess(messageCount);
        });
    }

    public void loadNewMessagesAsync(LoadCallback callback) {
        if (!AuthStore.isAuthenticated())
            return;

        Platform.runLater(() -> StatusManager.setStatus("Checking for new messages...", Status.INFO));

        new Thread(() -> {
            loadNewMessages(callback);
        }).start();
    }

    public void loadNewMessagesSync(LoadCallback callback) {
        if (!AuthStore.isAuthenticated())
            return;

        Platform.runLater(() -> StatusManager.setStatus("Checking for new messages...", Status.INFO));

        loadNewMessages(callback);
    }

    public void sendMessageAsync(List<String> recipientUserEmails, String subject, String body, SendCallback callback) {
        if (!AuthStore.isAuthenticated())
            return;

        List<String> missingFields = new ArrayList<>();

        if (recipientUserEmails.isEmpty() || recipientUserEmails.stream().allMatch(String::isEmpty)) {
            missingFields.add("recipient email(s)");
        }

        if (subject.isEmpty()) {
            missingFields.add("subject");
        }

        if (body.isEmpty()) {
            missingFields.add("message body");
        }

        if (missingFields.size() > 0) {
            String missing = String.join(", ", missingFields);
            Platform.runLater(() -> {
                StatusManager.setStatus("Please fill in the following fields: " + missing, Status.WARNING);
                NotificationManager.show("Missing fields: " + missing, Status.WARNING);
            });
            return;
        }

        var trimmedRecipientUserEmails = recipientUserEmails.stream()
                .map(String::trim)
                .toList();

        var invalidRecipientEmail = new ArrayList<String>();

        for (String email : trimmedRecipientUserEmails) {
            if (!EmailValidator.isValidEmail(email))
                invalidRecipientEmail.add(email);
        }

        if (invalidRecipientEmail.size() > 0) {
            String invalidEmails = String.join(", ", invalidRecipientEmail);
            Platform.runLater(() -> {
                StatusManager.setStatus("Invalid recipient email(s): " + invalidEmails, Status.WARNING);
                NotificationManager.show("Invalid recipient email(s): " + invalidEmails, Status.WARNING);
            });
            return;
        }

        StatusManager.setStatus("Sending message...", Status.INFO);

        new Thread(() -> {
            String messageId = messageApi.sendMessage(trimmedRecipientUserEmails, subject, body);

            Platform.runLater(() -> {
                if (messageId == null || messageId.isEmpty()) {
                    NotificationManager.show("Failed to send message", Status.ERROR);
                    StatusManager.setStatus("Failed to send message", Status.ERROR);

                    callback.onFailure();
                    return;
                }

                StatusManager.setStatus("Message sent successfully", Status.SUCCESS);
                NotificationManager.show("Message sent to " + trimmedRecipientUserEmails.size() +
                        " recipient(s)", Status.SUCCESS);
                callback.onSuccess();
            });
        }).start();
    }

    public void deleteMessageAsync(Message message, DeleteCallback callback) {
        if (!AuthStore.isAuthenticated())
            return;

        if (message == null) {
            StatusManager.setStatus("No message selected to delete", Status.WARNING);
            NotificationManager.show("No message selected to delete", Status.WARNING);
            return;
        }

        StatusManager.setStatus("Deleting message...", Status.INFO);

        new Thread(() -> {
            boolean success = (messageApi.deleteMessage(message.getGuid()) != null);

            Platform.runLater(() -> {

                if (!success) {
                    StatusManager.setStatus("Failed to delete message", Status.ERROR);
                    NotificationManager.show("Failed to delete message", Status.ERROR);

                    callback.onFailure();
                    return;
                }

                var removedFromMap = messageMap.remove(message.getGuid()) != null;
                var removedFromMessageList = messageList.remove(message);

                if (!removedFromMessageList && !removedFromMap) {
                    StatusManager.setStatus("Message deleted but you may need to refresh the view",
                            Status.WARNING);
                    callback.onFailure();
                    return;
                }

                StatusManager.setStatus("Message deleted successfully", Status.SUCCESS);
                NotificationManager.show("Message deleted successfully", Status.SUCCESS);
                callback.onSuccess();
            });
        }).start();
    }

}
