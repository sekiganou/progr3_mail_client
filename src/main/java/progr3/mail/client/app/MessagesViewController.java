package progr3.mail.client.app;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import progr3.mail.client.api.MessageApi;
import progr3.mail.client.api.UserApi;
import progr3.mail.client.hooks.Auth;
import progr3.mail.client.model.Message;
import progr3.mail.client.model.User;
import progr3.mail.client.util.ToastNotification;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private Label detailSubjectLabel;

    @FXML
    private Label detailDateLabel;

    @FXML
    private TextArea detailBodyArea;

    @FXML
    private Label statusLabel;

    @FXML
    private Label messageCountLabel;

    private ApiHandler apiHandler;
    private MessageApi messageApi;
    private UserApi userApi;
    private HashMap<String, User> userCache = new HashMap<>();
    private ObservableList<Message> messageList = FXCollections.observableArrayList();
    private ObservableList<Message> filteredMessageList = FXCollections.observableArrayList();

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MessagesViewController() {
        this.apiHandler = new ApiHandler();
        this.messageApi = new MessageApi(apiHandler);
        this.userApi = new UserApi(apiHandler);
    }

    @FXML
    public void initialize() {
        setupUserInfo();
        setupTableColumns();
        setupSearchFilter();
        setupMessageSelection();
        loadMessages();
    }

    private void setupUserInfo() {
        if (Auth.isAuthenticated()) {
            userLabel.setText("Logged in as: " + Auth.getUser().getEmail());
        }
    }

    private void setupTableColumns() {
        // Status column (read/unread indicator)
        // statusColumn.setCellValueFactory(
        // cellData -> new SimpleStringProperty(cellData.getValue().isRead() ? "✓" :
        // "●"));

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
                    // } else if (!item.isRead()) {
                    // setStyle("-fx-font-weight: bold; -fx-background-color: #E3F2FD;");
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
                    }
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
        detailSenderLabel.setText(userCache.get(message.getSenderUserGUID()).getName() + " <" +
                userCache.get(message.getSenderUserGUID()).getEmail() + ">");
        detailSubjectLabel.setText(message.getSubject());
        detailDateLabel.setText(formatTimestamp(message.getDate().toString()));
        detailBodyArea.setText(message.getBody());
    }

    private void loadMessages() {
        statusLabel.setText("Loading messages...");
        statusLabel.setStyle("-fx-text-fill: #2196F3;");

        new Thread(() -> {
            Message[] messages = messageApi.getMessages();

            Platform.runLater(() -> {
                if (messages != null) {
                    messageList.setAll(messages);
                    filteredMessageList.setAll(messages);
                    messagesTableView.setItems(filteredMessageList);
                    updateMessageCount();
                    updateUserCache();
                    statusLabel.setText("Messages loaded successfully");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");

                    ToastNotification.show(
                            "Loaded " + messages.length + " message(s)",
                            ToastNotification.Type.SUCCESS);
                } else {
                    statusLabel.setText("Failed to load messages");
                    statusLabel.setStyle("-fx-text-fill: #F44336;");
                    messageList.clear();
                    filteredMessageList.clear();
                    updateMessageCount();
                }
            });
        }).start();
    }

    private void updateMessageCount() {
        int unreadCount = 0;
        // for (Message msg : messageList) {
        // if (!msg.isRead()) {
        // unreadCount++;
        // }
        // }

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
        loadMessages();
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
        ToastNotification.show("Reply feature not implemented yet", ToastNotification.Type.INFO);
    }

    @FXML
    public void onReplyAllClick() {
        ToastNotification.show("Reply All feature not implemented yet", ToastNotification.Type.INFO);
    }

    @FXML
    private void onForwardClick() {
        ToastNotification.show("Forward feature not implemented yet", ToastNotification.Type.INFO);
    }

    @FXML
    private void onDeleteClick() {
        ToastNotification.show("Delete feature not implemented yet", ToastNotification.Type.INFO);
    }

}