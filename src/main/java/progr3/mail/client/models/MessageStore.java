package progr3.mail.client.models;

import java.util.Comparator;
import java.util.Date;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import progr3.mail.client.api.MessageApi;
import progr3.mail.client.model.Message;

public class MessageStore {
    public ObservableList<Message> messageList = FXCollections.observableArrayList();
    private MessageApi messageApi;
    private static final String IS_NEW = "isNew";

    public MessageStore(MessageApi messageApi) {
        this.messageApi = messageApi;
    }

    public interface LoadCallback {
        void onSuccess(int messageCount);

        void onFailure();
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
                    callback.onSuccess(messages.length);
                } else {
                    callback.onFailure();
                }
            });
        }).start();
    }
}
