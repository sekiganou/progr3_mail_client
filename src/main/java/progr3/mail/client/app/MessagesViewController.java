package progr3.mail.client.app;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import progr3.mail.client.api.HealthApi;
import progr3.mail.client.api.MessageApi;
import progr3.mail.client.api.UserApi;
import progr3.mail.client.hooks.Auth;
import progr3.mail.client.model.Message;
import progr3.mail.client.model.User;
import progr3.mail.client.models.MessageStore;
import progr3.mail.client.models.UserCache;
import progr3.mail.client.util.ToastNotification;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class MessagesViewController {

    @FXML
    private Label userLabel;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Message> messagesTableView;

    @FXML
    private TableColumn<Message, String> statusColumn;

    @FXML
    private TableColumn<Message, String> senderColumn;

    @FXML
    private TableColumn<Message, String> subjectColumn;

    @FXML
    private TableColumn<Message, String> timestampColumn;

    @FXML
    private Label detailSenderLabel;

    @FXML
    private Label detailRecipientsLabel;

    @FXML
    private Label detailSubjectLabel;

    @FXML
    private Label detailDateLabel;

    @FXML
    private TextArea detailBodyArea;

    @FXML
    private Label statusLabel;

    @FXML
    private Label messageCountLabel;

    @FXML
    private Circle connectionIndicator;

    private ApiHandler apiHandler;
    private MessageApi messageApi;
    private UserApi userApi;
    private HealthApi healthApi;
    private UserCache userCache;
    private MessageStore messageStore;

    public ObservableList<Message> filteredMessageList = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private static final int POLLING_INTERVAL_SECONDS = 30;

    public MessagesViewController() {
        this.apiHandler = new ApiHandler();
        this.messageApi = new MessageApi(apiHandler);
        this.userApi = new UserApi(apiHandler);
        this.healthApi = new HealthApi(apiHandler);
        this.userCache = new UserCache(userApi);
        this.messageStore = new MessageStore(messageApi);
    }

    @FXML
    public void initialize() {
        setupUserInfo();
        setupTableColumns();
        setupSearchFilter();
        setupMessageSelection();

        setupMessageListListener();

        checkServerHealth();
        loadMessages();
        loadNewMessagesRecursively(POLLING_INTERVAL_SECONDS);
    }

    private void setupUserInfo() {
        if (Auth.isAuthenticated()) {
            userLabel.setText("Logged in as: " + Auth.getUser().getEmail());
        }
    }

    private void setupTableColumns() {
        // Status column (read/unread indicator)
        statusColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(messageStore.isNew(cellData.getValue()) ? "●" : ""));

        statusColumn.setCellFactory(column -> new TableCell<Message, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("●")) {
                        setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold; -fx-font-size: 16px;");
                    } else {
                        setStyle("-fx-text-fill: #CCCCCC; -fx-font-size: 14px;");
                    }
                }
            }
        });

        // Sender column
        senderColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        userCache.getUserById(cellData.getValue().getSenderUserGUID()).getName()));

        // Subject column
        subjectColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getSubject()));

        // Timestamp column
        timestampColumn.setCellValueFactory(cellData -> {
            String timestamp = cellData.getValue().getDate().toString();
            String formattedTime = formatTimestamp(DATE_FORMATTER, timestamp);
            return new SimpleStringProperty(formattedTime);
        });

        // Apply row styling based on read status
        messagesTableView.setRowFactory(tv -> new TableRow<Message>() {
            @Override
            protected void updateItem(Message item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (messageStore.isNew(item)) {
                    setStyle("-fx-font-weight: bold; -fx-background-color: #E3F2FD;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private String formatTimestamp(DateTimeFormatter formatter, String timestamp) {
        try {
            Instant instant = Instant.parse(timestamp);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return localDateTime.format(formatter);
        } catch (Exception e) {
            return timestamp;
        }
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterMessages(newValue);
        });
    }

    private void setupMessageSelection() {
        messagesTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        displayMessageDetails(newValue);
                        newValue = messageStore.setIsNotNew(newValue);
                    }
                    // messagesTableView.refresh();
                    // updateMessageCount();
                });
    }

    private void filterMessages(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            filteredMessageList.setAll(messageStore.messageList);
        } else {
            String lowerCaseFilter = searchTerm.toLowerCase();
            filteredMessageList.clear();
            for (Message message : messageStore.messageList) {
                if (message.getSenderUserGUID().toLowerCase().contains(lowerCaseFilter) ||
                        message.getSubject().toLowerCase().contains(lowerCaseFilter) ||
                        message.getBody().toLowerCase().contains(lowerCaseFilter)) {
                    filteredMessageList.add(message);
                }
            }
        }
        messagesTableView.setItems(filteredMessageList);
        updateMessageCount();
    }

    private void displayMessageDetails(Message message) {
        var recipients = new ArrayList<String>();
        message.getRecipientsUserGUIDs().forEach(guid -> {
            recipients.add(userCache.getUserById(guid).getEmail());
        });
        detailRecipientsLabel.setText(String.join(", ", recipients));
        detailSenderLabel.setText(userCache.getUserById(message.getSenderUserGUID()).getName() + " <" +
                userCache.getUserById(message.getSenderUserGUID()).getEmail() + ">");
        detailSubjectLabel.setText(message.getSubject());
        detailDateLabel.setText(formatTimestamp(DATE_TIME_FORMATTER, message.getDate().toString()));
        detailBodyArea.setText(message.getBody());
    }

    private void loadNewMessagesRecursively(double delayInSeconds) {
        new Thread(() -> {
            try {
                Thread.sleep((long) (delayInSeconds * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                checkServerHealth();
                loadNewMessages();
                loadNewMessagesRecursively(POLLING_INTERVAL_SECONDS);
            });
        }).start();

    }

    private void loadMessages() {
        messageStore.loadMessages(new MessageStore.LoadCallback() {
            @Override
            public void onSuccess(int messageCount) {
                statusLabel.setText("Loaded " + messageCount + " message(s)");
                statusLabel.setStyle("-fx-text-fill: #4CAF50;");
            }

            @Override
            public void onFailure() {
                statusLabel.setText("Failed to load messages");
                statusLabel.setStyle("-fx-text-fill: #F44336;");
                ToastNotification.show("Failed to load messages", ToastNotification.Type.ERROR);
            }
        });
    }

    private void loadNewMessages() {
        messageStore.loadNewMessages(new MessageStore.LoadCallback() {
            @Override
            public void onSuccess(int messageCount) {
                statusLabel.setText("Loaded " + messageCount + " new message(s)");
                statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                ToastNotification.show("Loaded " + messageCount + " new message(s)", ToastNotification.Type.SUCCESS);
            }

            @Override
            public void onFailure() {
                statusLabel.setText("No new messages");
                statusLabel.setStyle("-fx-text-fill: #2196F3;");
            }
        });
    }

    private void checkServerHealth() {
        new Thread(() -> {
            boolean isHealthy = healthApi.isServerHealthy();
            updateConnectionIndicator(isHealthy);
        }).start();
    }

    private void updateConnectionIndicator(boolean isConnected) {
        Platform.runLater(() -> {
            if (isConnected) {
                connectionIndicator.setFill(javafx.scene.paint.Color.LIMEGREEN);
                connectionIndicator.setStroke(javafx.scene.paint.Color.DARKGREEN);
            } else {
                connectionIndicator.setFill(javafx.scene.paint.Color.RED);
                connectionIndicator.setStroke(javafx.scene.paint.Color.DARKRED);
            }
        });
    }

    private void updateMessageCount() {
        int unreadCount = 0;
        for (Message msg : messageStore.messageList) {
            if (messageStore.isNew(msg)) {
                unreadCount++;
            }
        }

        messageCountLabel.setText("Total messages: " + filteredMessageList.size() +
                (filteredMessageList.size() != messageStore.messageList.size()
                        ? " (filtered from " + messageStore.messageList.size() + ")"
                        : "")
                +
                " | Unread: " + unreadCount);
    }

    private void setupMessageListListener() {
        messageStore.messageList.addListener((ListChangeListener<Message>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    Message[] addedMessages = change.getAddedSubList().toArray(new Message[0]);
                    userCache.updateUserCache(addedMessages);
                    messagesTableView.refresh();
                    updateMessageCount();
                    filterMessages(searchField.getText());
                }

                if (change.wasRemoved()) {
                    messagesTableView.refresh();
                    updateMessageCount();
                    filterMessages(searchField.getText());
                }

                // if (change.wasUpdated()) { ... }
            }
        });
    }

    @FXML
    private void onRefreshClick() {
        ToastNotification.show("Refreshing messages...", ToastNotification.Type.INFO);

        loadNewMessages();
    }

    @FXML
    private void onLogoutClick() {
        userApi.Logout();
        ToastNotification.show("Logged out successfully", ToastNotification.Type.INFO);

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 400, 300);

            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setTitle("Mail Client - Login");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            ToastNotification.show("Error loading login view",
                    ToastNotification.Type.ERROR);
        }
    }

    @FXML
    private void onComposeClick() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("send-message-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 700, 600);

            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setTitle("Mail Client - Compose Message");
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
            ToastNotification.show("Error loading compose view",
                    ToastNotification.Type.ERROR);
        }
    }

    private void openSendMessageViewWithPrefill(String to, String subject, String body, String title) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("send-message-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 700, 600);

            SendMessageViewController controller = fxmlLoader.getController();
            controller.fromLabel.setText(Auth.getUser().getEmail());
            controller.toField.setText(to);
            controller.subjectField.setText(subject);
            controller.bodyArea.setText(body);

            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setTitle("Mail Client - " + title);
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading " + title.toLowerCase() + " view");
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            ToastNotification.show("Error loading " + title.toLowerCase() + " view",
                    ToastNotification.Type.ERROR);
        }
    }

    @FXML
    private void onReplyClick() {
        Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();
        if (selectedMessage != null) {
            User sender = userCache.getUserById(selectedMessage.getSenderUserGUID());
            if (sender != null) {
                String to = sender.getEmail();
                String subject = selectedMessage.getSubject();
                if (!subject.toLowerCase().startsWith("re:")) {
                    subject = "Re: " + subject;
                }
                String body = "\n\n--- On "
                        + formatTimestamp(DATE_TIME_FORMATTER, selectedMessage.getDate().toString()) +
                        ", " + sender.getName() + " wrote: ---\n" + selectedMessage.getBody();
                openSendMessageViewWithPrefill(to, subject, body, "Reply Message");
            }
        }
    }

    @FXML
    private void onReplyAllClick() {
        Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();
        if (selectedMessage != null) {
            User sender = userCache.getUserById(selectedMessage.getSenderUserGUID());
            if (sender != null) {
                var recipients = new ArrayList<String>();
                recipients.add(sender.getEmail());
                selectedMessage.getRecipientsUserGUIDs().forEach(guid -> {
                    var user = userCache.getUserById(guid);
                    if (user != null && !user.getEmail().equals(Auth.getUser().getEmail())
                            && !user.getEmail().equals(sender.getEmail()))
                        recipients.add(user.getEmail());
                });
                String to = String.join(", ", recipients);
                String subject = selectedMessage.getSubject();
                if (!subject.toLowerCase().startsWith("re:"))
                    subject = "Re: " + subject;

                String body = "\n\n--- On "
                        + formatTimestamp(DATE_TIME_FORMATTER, selectedMessage.getDate().toString()) +
                        ", " + sender.getName() + " wrote: ---\n" + selectedMessage.getBody();
                openSendMessageViewWithPrefill(to, subject, body, "Reply All Message");
            }
        }
    }

    @FXML
    private void onForwardClick() {
        Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();
        if (selectedMessage != null) {
            String subject = "Fwd: " + selectedMessage.getSubject();
            String body = "\n\n--- Forwarded message ---\nFrom: " +
                    userCache.getUserById(selectedMessage.getSenderUserGUID()).getName() + " <" +
                    userCache.getUserById(selectedMessage.getSenderUserGUID()).getEmail() + ">\nDate: " +
                    formatTimestamp(DATE_TIME_FORMATTER, selectedMessage.getDate().toString()) + "\nSubject: " +
                    selectedMessage.getSubject() + "\n\n" + selectedMessage.getBody();
            openSendMessageViewWithPrefill("", subject, body, "Forward Message");
        }
    }

    @FXML
    private void onDeleteClick() {
        try {
            Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();
            if (selectedMessage != null) {
                String response = messageApi.deleteMessage(selectedMessage.getGuid());
                if (response == null || !response.equals("OK")) {
                    statusLabel.setText("Failed to delete message");
                    statusLabel.setStyle("-fx-text-fill: #F44336;");

                    ToastNotification.show("Failed to delete message", ToastNotification.Type.ERROR);
                    return;
                }

                messageStore.messageList.remove(selectedMessage);
                filterMessages(searchField.getText());
                updateMessageCount();
                detailBodyArea.clear();
                detailDateLabel.setText("");
                detailRecipientsLabel.setText("");
                detailSenderLabel.setText("");
                detailSubjectLabel.setText("");

                statusLabel.setText("Message deleted successfully");
                statusLabel.setStyle("-fx-text-fill: #4CAF50;");

                ToastNotification.show("Message deleted", ToastNotification.Type.SUCCESS);
            } else {
                statusLabel.setText("No message selected to delete");
                statusLabel.setStyle("-fx-text-fill: #F44336;");

                ToastNotification.show("No message selected to delete", ToastNotification.Type.WARNING);
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error deleting message");
            statusLabel.setStyle("-fx-text-fill: #F44336;");

            ToastNotification.show("Error deleting message", ToastNotification.Type.ERROR);
        }
    }

}