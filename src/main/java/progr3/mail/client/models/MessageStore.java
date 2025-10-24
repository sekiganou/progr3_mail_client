package progr3.mail.client.models;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import progr3.mail.client.api.MessageApi;
import progr3.mail.client.model.Message;

public class MessageStore {
    private ObservableList<Message> messageList = FXCollections.observableArrayList();
    private ObservableList<Message> filteredMessageList = FXCollections.observableArrayList();
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

    public ObservableList<Message> getMessageList() {
        return messageList;
    }

    public ObservableList<Message> getFilteredMessageList() {
        return filteredMessageList;
    }

    public int getMessageCount() {
        return messageList.size();
    }

    public int getFilteredMessageCount() {
        return filteredMessageList.size();
    }

    public int getNewMessageCount() {
        return messageList.stream().mapToInt(msg -> isNew(msg) ? 1 : 0).sum();
    }

    public Message setIsNotNew(Message msg) {
        return setIsNew(msg, false);
    }

    public boolean isNew(Message msg) {
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

        new Thread(() -> {

            Message[] messages = messageApi.getMessages();

            Platform.runLater(() -> {
                if (messages != null) {
                    messageList.clear();
                    for (Message message : messages) {
                        message = setIsNew(message, false);
                    }
                    messageList.addAll(messages);
                    callback.onSuccess(messages.length);
                } else {
                    callback.onFailure();
                }
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

        new Thread(() -> {
            Message[] messages = messageApi.getMessagesWithFilter(latestTimestamp, new Date());
            Platform.runLater(() -> {
                if (messages != null && messages.length > 0) {
                    for (Message message : messages) {
                        message = setIsNew(message, true);
                        messageList.add(0, message);
                    }
                    // filterMessages("");
                    callback.onSuccess(messages.length);
                } else {
                    callback.onFailure();
                }
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

        new Thread(() -> {
            String messageId = messageApi.sendMessage(recipientUserEmails, subject, body);
            Platform.runLater(() -> {
                if (messageId != null) {
                    callback.onSuccess();
                } else {
                    callback.onFailure();
                }
            });
        }).start();
    }

    public void deleteMessage(Message message, DeleteCallback callback) {
        if (!AuthStore.isAuthenticated())
            return;

        new Thread(() -> {
            boolean success = (messageApi.deleteMessage(message.getGuid()) != null);
            Platform.runLater(() -> {

                if (!success) {
                    callback.onFailure();
                    return;
                }

                var removedFromMessageList = messageList.remove(message);
                var removedFromFilteredMessageList = filteredMessageList.remove(message);

                if (!removedFromMessageList && !removedFromFilteredMessageList) {
                    callback.onFailure();
                    return;
                }

                callback.onSuccess();
            });
        }).start();
    }
}
