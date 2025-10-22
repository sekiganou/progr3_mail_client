package progr3.mail.client.app;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import progr3.mail.client.api.ApiHandler;
import progr3.mail.client.api.HealthApi;
import progr3.mail.client.api.MessageApi;
import progr3.mail.client.api.UserApi;
import progr3.mail.client.hooks.Auth;
import progr3.mail.client.model.Message;
import progr3.mail.client.model.User;
import progr3.mail.client.models.MessageStore;
import progr3.mail.client.models.NavigationManager;
import progr3.mail.client.models.UserCache;
import progr3.mail.client.util.ToastNotification;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class InboxViewController {

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
    private NavigationManager navigationManager;

    public ObservableList<Message> filteredMessageList = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private static final int POLLING_INTERVAL_SECONDS = 30;

    public InboxViewController() {
        this.navigationManager = new NavigationManager();
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
            messageStore.filterMessages(newValue);
            messagesTableView.setItems(messageStore.getFilteredMessageList());
            updateMessageCountLabel();
        });
    }

    private void setupMessageSelection() {
        messagesTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        displayMessageDetails(newValue);
                        newValue = messageStore.setIsNotNew(newValue);
                    }
                    updateMessageCountLabel();
                    messagesTableView.refresh();
                });
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
                messagesTableView.setItems(messageStore.getFilteredMessageList());
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

    private void updateMessageCountLabel() {
        messageCountLabel.setText("Total messages: " + messageStore.getFilteredMessageCount() +
                (messageStore
                        .getFilteredMessageCount() != messageStore.getMessageCount()
                                ? " (filtered from " + messageStore.getMessageCount() + ")"
                                : "")
                +
                " | Unread: " + messageStore.getNewMessageCount());

    }

    private void setupMessageListListener() {
        messageStore.getMessageList().addListener((ListChangeListener<Message>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    Message[] addedMessages = change.getAddedSubList().toArray(new Message[0]);
                    userCache.updateUserCache(addedMessages);
                    messageStore.filterMessages(searchField.getText());
                    messagesTableView.refresh();
                    updateMessageCountLabel();
                }

                if (change.wasRemoved()) {
                    messageStore.filterMessages(searchField.getText());
                    messagesTableView.refresh();
                    updateMessageCountLabel();
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
        var success = userApi.Logout();

        if (!success) {
            ToastNotification.show("Logout failed. Please try again.", ToastNotification.Type.ERROR);
            return;
        }

        if (success) {
            ToastNotification.show("Logged out successfully", ToastNotification.Type.INFO);
            navigationManager.navigateTo((Stage) userLabel.getScene().getWindow(), navigationManager.getLoginView());
        }

    }

    @FXML
    private void onComposeClick() {
        navigationManager.navigateTo((Stage) userLabel.getScene().getWindow(),
                navigationManager.getComposeView());
    }

    private void openSendMessageViewWithPrefill(String header, String to, String subject, String body) {
        ComposeViewController controller = navigationManager.navigateTo(
                (Stage) userLabel.getScene().getWindow(),
                navigationManager.getComposeView());

        if (controller != null) {
            controller.prefillForm(header, to, subject, body);
        } else {
            ToastNotification.show("Error opening compose view", ToastNotification.Type.ERROR);
        }
    }

    @FXML
    private void onReplyClick() {
        Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();
        if (selectedMessage == null) {
            ToastNotification.show("No message selected to reply", ToastNotification.Type.WARNING);
            return;
        }

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
            openSendMessageViewWithPrefill("Reply Message", to, subject, body);
        }
    }

    @FXML
    private void onReplyAllClick() {
        Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();

        if (selectedMessage == null) {
            ToastNotification.show("No message selected to reply all", ToastNotification.Type.WARNING);
            return;
        }

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
            openSendMessageViewWithPrefill("Reply All Message", to, subject, body);
        }
    }

    @FXML
    private void onForwardClick() {
        Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();

        if (selectedMessage == null) {
            ToastNotification.show("No message selected to forward", ToastNotification.Type.WARNING);
            return;
        }

        String subject = "Fwd: " + selectedMessage.getSubject();
        String body = "\n\n--- Forwarded message ---\nFrom: " +
                userCache.getUserById(selectedMessage.getSenderUserGUID()).getName() + " <" +
                userCache.getUserById(selectedMessage.getSenderUserGUID()).getEmail() + ">\nDate: " +
                formatTimestamp(DATE_TIME_FORMATTER, selectedMessage.getDate().toString()) + "\nSubject: " +
                selectedMessage.getSubject() + "\n\n" + selectedMessage.getBody();
        openSendMessageViewWithPrefill("Forward Message", "", subject, body);
    }

    @FXML
    private void onDeleteClick() {
        Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();

        if (selectedMessage == null) {
            statusLabel.setText("No message selected to delete");
            statusLabel.setStyle("-fx-text-fill: #F44336;");

            ToastNotification.show("No message selected to delete", ToastNotification.Type.WARNING);
            return;
        }

        messageStore.deleteMessage(selectedMessage, new MessageStore.DeleteCallback() {
            @Override
            public void onSuccess() {
                updateMessageCountLabel();
                detailBodyArea.clear();
                detailDateLabel.setText("");
                detailRecipientsLabel.setText("");
                detailSenderLabel.setText("");
                detailSubjectLabel.setText("");

                statusLabel.setText("Message deleted successfully");
                statusLabel.setStyle("-fx-text-fill: #4CAF50;");

                ToastNotification.show("Message deleted", ToastNotification.Type.SUCCESS);
            }

            @Override
            public void onFailure() {
                statusLabel.setText("Failed to delete message");
                statusLabel.setStyle("-fx-text-fill: #F44336;");

                ToastNotification.show("Failed to delete message", ToastNotification.Type.ERROR);
            }
        });
    }

}