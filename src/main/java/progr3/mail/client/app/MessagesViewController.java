package progr3.mail.client.app;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
import progr3.mail.client.util.ToastNotification;

import java.io.IOException;
import java.util.Date;
import javafx.util.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

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
    private HashMap<String, User> userCache = new HashMap<>();
    private ObservableList<Message> messageList = FXCollections.observableArrayList();
    private ObservableList<Message> filteredMessageList = FXCollections.observableArrayList();

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private static final int POLLING_INTERVAL_SECONDS = 30;

    private static final String IS_NEW = "isNew";

    private boolean getIsNew(Message msg) {
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

    public MessagesViewController() {
        this.apiHandler = new ApiHandler();
        this.messageApi = new MessageApi(apiHandler);
        this.userApi = new UserApi(apiHandler);
        this.healthApi = new HealthApi(apiHandler);
    }

    @FXML
    public void initialize() {
        setupUserInfo();
        setupTableColumns();
        setupSearchFilter();
        setupMessageSelection();

        checkServerHealth();
        loadMessages();
        loadNewMessagesRecursively(POLLING_INTERVAL_SECONDS);

        // checkServerHealthRecursively(POLLING_INTERVAL_SECONDS);
    }

    private void setupUserInfo() {
        if (Auth.isAuthenticated()) {
            userLabel.setText("Logged in as: " + Auth.getUser().getEmail());
        }
    }

    private void setupTableColumns() {
        // Status column (read/unread indicator)
        statusColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(getIsNew(cellData.getValue()) ? "●" : ""));

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
                cellData -> new SimpleStringProperty(userCache.get(cellData.getValue().getSenderUserGUID()).getName()));

        // Subject column
        subjectColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getSubject()));

        // Timestamp column
        timestampColumn.setCellValueFactory(cellData -> {
            String timestamp = cellData.getValue().getDate().toString();
            String formattedTime = formatTimestamp(timestamp);
            return new SimpleStringProperty(formattedTime);
        });

        // Apply row styling based on read status
        messagesTableView.setRowFactory(tv -> new TableRow<Message>() {
            @Override
            protected void updateItem(Message item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (getIsNew(item)) {
                    setStyle("-fx-font-weight: bold; -fx-background-color: #E3F2FD;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private String formatTimestamp(String timestamp) {
        try {
            Instant instant = Instant.parse(timestamp);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return localDateTime.format(TIME_FORMATTER);
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
                        newValue = setIsNew(newValue, false);
                    }
                    messagesTableView.refresh();
                    updateMessageCount();
                });
    }

    private void filterMessages(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            filteredMessageList.setAll(messageList);
        } else {
            String lowerCaseFilter = searchTerm.toLowerCase();
            filteredMessageList.clear();
            for (Message message : messageList) {
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
            recipients.add(userCache.get(guid).getEmail());
        });
        detailRecipientsLabel.setText(String.join(", ", recipients));
        detailSenderLabel.setText(userCache.get(message.getSenderUserGUID()).getName() + " <" +
                userCache.get(message.getSenderUserGUID()).getEmail() + ">");
        detailSubjectLabel.setText(message.getSubject());
        detailDateLabel.setText(formatTimestamp(message.getDate().toString()));
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
        new Thread(() -> {
            Message[] messages = messageApi.getMessages();

            Platform.runLater(() -> {
                if (messages != null) {
                    messageList.clear();
                    for (Message message : messages) {
                        message = setIsNew(message, false);
                    }
                    messageList.addAll(messages);
                    filterMessages(searchField.getText());
                    updateMessageCount();
                    updateUserCache();
                    statusLabel.setText("Loaded " + messages.length + " message(s)");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                } else {
                    statusLabel.setText("Failed to load messages");
                    statusLabel.setStyle("-fx-text-fill: #F44336;");

                    ToastNotification.show(
                            "Failed to load messages",
                            ToastNotification.Type.ERROR);
                }
            });
        }).start();
    }

    private void loadNewMessages() {
        new Thread(() -> {
            var latestTimestamp = messageList.stream()
                    .map(Message::getDate)
                    .max(new Comparator<Date>() {
                        @Override
                        public int compare(Date o1, Date o2) {
                            return o1.compareTo(o2);
                        }
                    })
                    .orElse(new Date(0));
            Message[] newMessages = messageApi.getMessagesWithFilter(latestTimestamp, new Date());

            Platform.runLater(() -> {
                if (newMessages != null && newMessages.length > 0) {
                    for (Message message : newMessages) {
                        message = setIsNew(message, false);
                    }
                    messageList.addAll(newMessages);
                    filterMessages(searchField.getText());
                    updateMessageCount();
                    updateUserCache();
                    statusLabel.setText("Loaded " + newMessages.length + " new message(s)");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                    messagesTableView.refresh();

                    ToastNotification.show(
                            "Loaded " + newMessages.length + " new message(s)",
                            ToastNotification.Type.SUCCESS);
                } else {
                    statusLabel.setText("No new messages");
                    statusLabel.setStyle("-fx-text-fill: #2196F3;");
                }
            });
        }).start();
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
        for (Message msg : messageList) {
            if (getIsNew(msg)) {
                unreadCount++;
            }
        }

        messageCountLabel.setText("Total messages: " + filteredMessageList.size() +
                (filteredMessageList.size() != messageList.size() ? " (filtered from " + messageList.size() + ")" : "")
                +
                " | Unread: " + unreadCount);
    }

    private void updateUserCache() {
        HashMap<String, Boolean> missingUserIds = new HashMap<>();
        for (Message msg : messageList) {
            if (!userCache.containsKey(msg.getSenderUserGUID())) {
                missingUserIds.put(msg.getSenderUserGUID(), true);
            }

            for (String recipientId : msg.getRecipientsUserGUIDs()) {
                if (!userCache.containsKey(recipientId)) {
                    missingUserIds.put(recipientId, true);
                }
            }
        }

        if (!missingUserIds.isEmpty()) {
            User[] users = userApi.getUsers(missingUserIds.keySet().stream().toList());
            if (users != null) {
                for (User user : users) {
                    userCache.put(user.getGuid(), user);
                }
            }
        }
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

    @FXML
    private void onReplyClick() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("send-message-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 700, 600);

            SendMessageViewController controller = fxmlLoader.getController();
            Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();

            if (selectedMessage != null) {
                User sender = userCache.get(selectedMessage.getSenderUserGUID());
                if (sender != null) {
                    controller.fromLabel.setText(Auth.getUser().getEmail());
                    controller.toField.setText(sender.getEmail());
                    String subject = selectedMessage.getSubject();
                    if (!subject.toLowerCase().startsWith("re:")) {
                        subject = "Re: " + subject;
                    }
                    controller.subjectField.setText(subject);
                    String body = "\n\n--- On " + formatTimestamp(selectedMessage.getDate().toString()) +
                            ", " + sender.getName() + " wrote: ---\n" + selectedMessage.getBody();
                    controller.bodyArea.setText(body);
                }
            }

            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setTitle("Mail Client - Reply Message");
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading reply view");
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            ToastNotification.show("Error loading reply view",
                    ToastNotification.Type.ERROR);
        }
    }

    @FXML
    public void onReplyAllClick() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("send-message-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 700, 600);

            SendMessageViewController controller = fxmlLoader.getController();
            Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();
            if (selectedMessage != null) {
                User sender = userCache.get(selectedMessage.getSenderUserGUID());
                if (sender != null) {
                    controller.fromLabel.setText(Auth.getUser().getEmail());
                    var recipients = new ArrayList<String>();
                    recipients.add(sender.getEmail());
                    selectedMessage.getRecipientsUserGUIDs().forEach(guid -> {
                        var user = userCache.get(guid);
                        if (user != null && !user.getEmail().equals(Auth.getUser().getEmail())
                                && !user.getEmail().equals(sender.getEmail())) {
                            recipients.add(user.getEmail());
                        }
                    });
                    controller.toField.setText(String.join(", ", recipients));
                    String subject = selectedMessage.getSubject();
                    if (!subject.toLowerCase().startsWith("re:")) {
                        subject = "Re: " + subject;
                    }
                    controller.subjectField.setText(subject);
                    String body = "\n\n--- On " + formatTimestamp(selectedMessage.getDate().toString()) +
                            ", " + sender.getName() + " wrote: ---\n" + selectedMessage.getBody();
                    controller.bodyArea.setText(body);
                }
            }
            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setTitle("Mail Client - Reply All Message");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading reply all view");
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            ToastNotification.show("Error loading reply all view",
                    ToastNotification.Type.ERROR);
        }

    }

    @FXML
    private void onForwardClick() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("send-message-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 700, 600);

            SendMessageViewController controller = fxmlLoader.getController();
            Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();
            if (selectedMessage != null) {
                controller.fromLabel.setText(Auth.getUser().getEmail());
                controller.subjectField.setText("Fwd: " + selectedMessage.getSubject());
                String body = "\n\n--- Forwarded message ---\nFrom: " +
                        userCache.get(selectedMessage.getSenderUserGUID()).getName() + " <" +
                        userCache.get(selectedMessage.getSenderUserGUID()).getEmail() + ">\nDate: " +
                        formatTimestamp(selectedMessage.getDate().toString()) + "\nSubject: " +
                        selectedMessage.getSubject() + "\n\n" + selectedMessage.getBody();
                controller.bodyArea.setText(body);
            }
            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setTitle("Mail Client - Forward Message");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading forward view");
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            ToastNotification.show("Error loading forward view",
                    ToastNotification.Type.ERROR);
        }
    }

    @FXML
    private void onDeleteClick() {
        try {
            Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();
            if (selectedMessage != null) {
                messageApi.deleteMessage(selectedMessage.getGuid());

                messageList.remove(selectedMessage);
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