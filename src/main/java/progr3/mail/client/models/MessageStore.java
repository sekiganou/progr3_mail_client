package progr3.mail.client.models;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import progr3.mail.client.api.MessageApi;
import progr3.mail.client.model.Message;

public class MessageStore {
    private static ObservableList<Message> messageList = FXCollections.observableArrayList();
    private static ObservableList<Message> filteredMessageList = FXCollections.observableArrayList();
    private static HashMap<String, Message> messageMap = new HashMap<>();
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

    public static ObservableList<Message> getFilteredMessageList() {
        return filteredMessageList;
    }

    public static int getMessageCount() {
        return messageList.size();
    }

    public static int getFilteredMessageCount() {
        return filteredMessageList.size();
    }

    public static int getNewMessageCount() {
        return messageList.stream().mapToInt(msg -> isNew(msg) ? 1 : 0).sum();
    }

    public Message setIsNotNew(Message msg) {
        return setIsNew(msg, false);
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

    public void loadMessages(LoadCallback callback) {
        if (!AuthStore.isAuthenticated())
            return;

        StatusManager.setStatus("Loading messages...", StatusManager.Type.INFO);
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
                    StatusManager.setStatus("Failed to load messages", StatusManager.Type.ERROR);
                    NotificationManager.show("Failed to load messages", NotificationManager.Type.ERROR);

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
                StatusManager.setStatus("Loaded " + messageCount + " message(s)", StatusManager.Type.SUCCESS);
                callback.onSuccess(messageCount);
            });
        }).start();
    }

    public void loadNewMessages(LoadCallback callback) {
        if (!AuthStore.isAuthenticated())
            return;

        var latestTimestamp = messageList.stream()
                .map(Message::getDate)
                .max(new Comparator<Date>() {
                    @Override
                    public int compare(Date o1, Date o2) {
                        return o1.compareTo(o2);
                    }
                })
                .orElse(new Date(0));

        StatusManager.setStatus("Checking for new messages...", StatusManager.Type.INFO);

        new Thread(() -> {
            Message[] messages = messageApi.getMessagesWithFilter(latestTimestamp, new Date());

            Platform.runLater(() -> {
                if (messages == null) {
                    StatusManager.setStatus("Failed to load new messages", StatusManager.Type.ERROR);
                    NotificationManager.show("Failed to load new messages", NotificationManager.Type.ERROR);

                    callback.onFailure();
                    return;
                }

                if (messages.length == 0) {
                    StatusManager.setStatus("No new messages", StatusManager.Type.INFO);
                    callback.onSuccess(0);
                    return;
                }

                for (Message message : messages) {
                    message = setIsNew(message, true);
                    messageList.add(0, message);
                    messageMap.put(message.getGuid(), message);
                }

                int messageCount = messages.length;
                StatusManager.setStatus("Loaded " + messageCount + " new message(s)", StatusManager.Type.SUCCESS);
                NotificationManager.show("Loaded " + messageCount + " new message(s)",
                        NotificationManager.Type.SUCCESS);
                callback.onSuccess(messageCount);
            });
        }).start();
    }

    public void filterMessages(String filterText) {
        if (filterText == null || filterText.trim().isEmpty()) {
            filteredMessageList.setAll(messageList);
        } else {
            String lowerCaseFilter = filterText.toLowerCase();
            filteredMessageList.clear();
            for (Message message : messageList) {
                if (message.getSenderUserGUID().toLowerCase().contains(lowerCaseFilter) ||
                        message.getSubject().toLowerCase().contains(lowerCaseFilter) ||
                        message.getBody().toLowerCase().contains(lowerCaseFilter)) {
                    filteredMessageList.add(message);
                }
            }
        }
    }

    public void sendMessage(List<String> recipientUserEmails, String subject, String body, SendCallback callback) {
        if (!AuthStore.isAuthenticated())
            return;

        if (recipientUserEmails.isEmpty()) {
            StatusManager.setStatus("Please enter recipient email(s)", StatusManager.Type.WARNING);
            NotificationManager.show("Recipient email is required", NotificationManager.Type.WARNING);
            return;
        }

        if (subject.isEmpty()) {
            StatusManager.setStatus("Please enter a subject", StatusManager.Type.WARNING);
            NotificationManager.show("Subject is required", NotificationManager.Type.WARNING);
            return;
        }

        if (body.isEmpty()) {
            StatusManager.setStatus("Please enter a message", StatusManager.Type.WARNING);
            NotificationManager.show("Message body is required", NotificationManager.Type.WARNING);
            return;
        }

        var trimmedRecipientUserEmails = recipientUserEmails.stream()
                .map(String::trim)
                .toList();

        trimmedRecipientUserEmails.stream()
                .filter(email -> !EmailValidator.isValidEmail(email))
                .findFirst()
                .ifPresent(invalid -> {
                    StatusManager.setStatus("Invalid email: " + invalid, StatusManager.Type.WARNING);
                    NotificationManager.show("Invalid email: " + invalid, NotificationManager.Type.WARNING);
                });

        StatusManager.setStatus("Sending message...", StatusManager.Type.INFO);

        new Thread(() -> {
            String messageId = messageApi.sendMessage(trimmedRecipientUserEmails, subject, body);

            Platform.runLater(() -> {
                if (messageId == null || messageId.isEmpty()) {
                    NotificationManager.show("Failed to send message", NotificationManager.Type.ERROR);
                    StatusManager.setStatus("Failed to send message", StatusManager.Type.ERROR);

                    callback.onFailure();
                    return;
                }

                StatusManager.setStatus("Message sent successfully", StatusManager.Type.SUCCESS);
                NotificationManager.show("Message sent to " + trimmedRecipientUserEmails.size() +
                        " recipient(s)", NotificationManager.Type.SUCCESS);
                callback.onSuccess();
            });
        }).start();
    }

    public void deleteMessage(Message message, DeleteCallback callback) {
        if (!AuthStore.isAuthenticated())
            return;

        if (message == null) {
            StatusManager.setStatus("No message selected to delete", StatusManager.Type.WARNING);
            NotificationManager.show("No message selected to delete", NotificationManager.Type.WARNING);
            return;
        }

        StatusManager.setStatus("Deleting message...", StatusManager.Type.INFO);

        new Thread(() -> {
            boolean success = (messageApi.deleteMessage(message.getGuid()) != null);

            Platform.runLater(() -> {

                if (!success) {
                    StatusManager.setStatus("Failed to delete message", StatusManager.Type.ERROR);
                    NotificationManager.show("Failed to delete message", NotificationManager.Type.ERROR);

                    callback.onFailure();
                    return;
                }

                var removedFromMap = messageMap.remove(message.getGuid()) != null;
                var removedFromMessageList = messageList.remove(message);
                var removedFromFilteredMessageList = filteredMessageList.remove(message);

                if (!removedFromMessageList && !removedFromFilteredMessageList && !removedFromMap) {
                    StatusManager.setStatus("Message deleted but you may need to refresh the view",
                            StatusManager.Type.WARNING);
                    callback.onFailure();
                    return;
                }

                StatusManager.setStatus("Message deleted successfully", StatusManager.Type.SUCCESS);
                NotificationManager.show("Message deleted successfully", NotificationManager.Type.SUCCESS);
                callback.onSuccess();
            });
        }).start();
    }

}
