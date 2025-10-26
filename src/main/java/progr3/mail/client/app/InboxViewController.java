package progr3.mail.client.app;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import progr3.mail.client.api.ApiHandler;
import progr3.mail.client.api.HealthApi;
import progr3.mail.client.api.MessageApi;
import progr3.mail.client.api.UserApi;
import progr3.mail.client.model.Message;
import progr3.mail.client.model.User;
import progr3.mail.client.models.AuthStore;
import progr3.mail.client.models.DateFormatManager;
import progr3.mail.client.models.HealthStore;
import progr3.mail.client.models.MessageStore;
import progr3.mail.client.models.NavigationManager;
import progr3.mail.client.models.StatusManager;
import progr3.mail.client.models.NotificationManager;
import progr3.mail.client.models.UserCache;

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

    private UserCache userCache;
    private MessageStore messageStore;
    private AuthStore authStore;
    private HealthStore healthStore;
    private NavigationManager navigationManager;
    private static final int POLLING_INTERVAL_SECONDS = 30;

    public InboxViewController() {
        this.navigationManager = new NavigationManager();
        var apiHandler = new ApiHandler();

        var healthApi = new HealthApi(apiHandler);
        this.healthStore = new HealthStore(healthApi);

        var messageApi = new MessageApi(apiHandler);
        this.messageStore = new MessageStore(messageApi, healthStore);

        var userApi = new UserApi(apiHandler);
        this.userCache = new UserCache(userApi);
        this.authStore = new AuthStore(userApi, healthStore);
    }

    @FXML
    public void initialize() {
        setupUserInfo();
        setupTableColumns();
        setupSearchFilter();
        setupMessageSelection();

        setupMessageListListener();
        setupHealthListener();
        setupStatusListener();

        loadMessages();

        loadNewMessagesRecursively(POLLING_INTERVAL_SECONDS);
    }

    private void setupStatusListener() {
        StatusManager.getStatusLabelText().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> statusLabel.setText(newValue));
        });

        StatusManager.getStatusLabelStyle().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> statusLabel.setStyle(newValue));
        });

        StatusManager.getConnectionLabelStyle().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> connectionIndicator.setStyle(newValue));
        });
    }

    private void setupUserInfo() {
        if (AuthStore.isAuthenticated()) {
            userLabel.setText("Logged in as: " + AuthStore.getUser().getEmail());
        }
    }

    private void setupTableColumns() {
        // Status column (read/unread indicator)
        statusColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(MessageStore.isNew(cellData.getValue()) ? "●" : ""));

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
            String formattedTime = DateFormatManager.formatTimestamp(DateFormatManager.DATE_FORMATTER, timestamp);
            return new SimpleStringProperty(formattedTime);
        });

        // Apply row styling based on read status
        messagesTableView.setRowFactory(tv -> new TableRow<Message>() {
            @Override
            protected void updateItem(Message item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (MessageStore.isNew(item)) {
                    setStyle("-fx-font-weight: bold; -fx-background-color: #E3F2FD;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            messageStore.filterMessages(newValue);
            messagesTableView.setItems(MessageStore.getFilteredMessageList());
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
        detailDateLabel.setText(
                DateFormatManager.formatTimestamp(DateFormatManager.DATE_TIME_FORMATTER, message.getDate().toString()));
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
                loadNewMessages(false);
                loadNewMessagesRecursively(POLLING_INTERVAL_SECONDS);
            });
        }).start();

    }

    private void loadMessages() {
        messageStore.loadMessages(new MessageStore.LoadCallback() {
            @Override
            public void onSuccess(int messageCount) {
                StatusManager.setStatus("Loaded " + messageCount + " message(s)", StatusManager.Type.SUCCESS);
                messagesTableView.setItems(MessageStore.getFilteredMessageList());
            }

            @Override
            public void onFailure() {
                StatusManager.setStatus("Failed to load messages", StatusManager.Type.ERROR);
                NotificationManager.show("Failed to load messages", NotificationManager.Type.ERROR);
            }
        });
    }

    private void loadNewMessages(boolean showNotificationWhenNone) {
        messageStore.loadNewMessages(new MessageStore.LoadCallback() {
            @Override
            public void onSuccess(int messageCount) {
                if (messageCount == 0) {
                    StatusManager.setStatus("No new messages", StatusManager.Type.INFO);
                    if (showNotificationWhenNone) {
                        NotificationManager.show("No new messages", NotificationManager.Type.INFO);
                    }
                    return;
                }

                StatusManager.setStatus("Loaded " + messageCount + " new message(s)", StatusManager.Type.SUCCESS);
                NotificationManager.show("Loaded " + messageCount + " new message(s)",
                        NotificationManager.Type.SUCCESS);

            }

            @Override
            public void onFailure() {
                StatusManager.setStatus("Failed to load new messages", StatusManager.Type.ERROR);
                NotificationManager.show("Failed to load new messages", NotificationManager.Type.ERROR);
            }
        });
    }

    private void updateMessageCountLabel() {
        messageCountLabel.setText("Total messages: " + MessageStore.getFilteredMessageCount() +
                (MessageStore
                        .getFilteredMessageCount() != MessageStore.getMessageCount()
                                ? " (filtered from " + MessageStore.getMessageCount() + ")"
                                : "")
                +
                " | Unread: " + MessageStore.getNewMessageCount());

    }

    private void setupHealthListener() {
        healthStore.getIsServerHealthyProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                if (newValue) {
                    // text is set but there is not label in the UI for it
                    StatusManager.setConnectionStatus("Server is reachable", StatusManager.Type.SUCCESS);
                } else {
                    StatusManager.setConnectionStatus("Server is unreachable", StatusManager.Type.ERROR);
                }
            });
        });
    }

    private void setupMessageListListener() {
        MessageStore.getMessageList().addListener((ListChangeListener<Message>) change -> {
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
        loadNewMessages(true);
    }

    @FXML
    private void onLogoutClick() {
        var success = authStore.logout();

        if (!success) {
            NotificationManager.show("Logout failed. Please try again.",
                    NotificationManager.Type.ERROR);
            return;
        }

        if (success) {
            NotificationManager.show("Logged out successfully",
                    NotificationManager.Type.INFO);
            navigationManager.navigateTo((Stage) userLabel.getScene().getWindow(),
                    navigationManager.getLoginView());
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
            NotificationManager.show("Error opening compose view", NotificationManager.Type.ERROR);
        }
    }

    @FXML
    private void onReplyClick() {
        Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();

        if (selectedMessage == null) {
            StatusManager.setStatus("No message selected to reply", StatusManager.Type.WARNING);
            NotificationManager.show("No message selected to reply", NotificationManager.Type.WARNING);
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
                    + DateFormatManager.formatTimestamp(DateFormatManager.DATE_TIME_FORMATTER,
                            selectedMessage.getDate().toString())
                    +
                    ", " + sender.getName() + " wrote: ---\n" + selectedMessage.getBody();
            openSendMessageViewWithPrefill("Reply Message", to, subject, body);
        }
    }

    @FXML
    private void onReplyAllClick() {
        Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();

        if (selectedMessage == null) {
            StatusManager.setStatus("No message selected to reply all", StatusManager.Type.WARNING);
            NotificationManager.show("No message selected to reply all", NotificationManager.Type.WARNING);
            return;
        }

        User sender = userCache.getUserById(selectedMessage.getSenderUserGUID());
        if (sender != null) {
            var recipients = new ArrayList<String>();
            recipients.add(sender.getEmail());
            selectedMessage.getRecipientsUserGUIDs().forEach(guid -> {
                var user = userCache.getUserById(guid);
                if (user != null && !user.getEmail().equals(AuthStore.getUser().getEmail())
                        && !user.getEmail().equals(sender.getEmail()))
                    recipients.add(user.getEmail());
            });
            String to = String.join(", ", recipients);
            String subject = selectedMessage.getSubject();
            if (!subject.toLowerCase().startsWith("re:"))
                subject = "Re: " + subject;

            String body = "\n\n--- On "
                    + DateFormatManager.formatTimestamp(DateFormatManager.DATE_TIME_FORMATTER,
                            selectedMessage.getDate().toString())
                    +
                    ", " + sender.getName() + " wrote: ---\n" + selectedMessage.getBody();
            openSendMessageViewWithPrefill("Reply All Message", to, subject, body);
        }
    }

    @FXML
    private void onForwardClick() {
        Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();

        if (selectedMessage == null) {
            StatusManager.setStatus("No message selected to forward", StatusManager.Type.WARNING);
            NotificationManager.show("No message selected to forward", NotificationManager.Type.WARNING);
            return;
        }

        String subject = "Fwd: " + selectedMessage.getSubject();
        String body = "\n\n--- Forwarded message ---\nFrom: " +
                userCache.getUserById(selectedMessage.getSenderUserGUID()).getName() + " <" +
                userCache.getUserById(selectedMessage.getSenderUserGUID()).getEmail() + ">\nDate: " +
                DateFormatManager.formatTimestamp(DateFormatManager.DATE_TIME_FORMATTER,
                        selectedMessage.getDate().toString())
                + "\nSubject: " +
                selectedMessage.getSubject() + "\n\n" + selectedMessage.getBody();
        openSendMessageViewWithPrefill("Forward Message", "", subject, body);
    }

    @FXML
    private void onDeleteClick() {
        Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();

        if (selectedMessage == null) {
            StatusManager.setStatus("No message selected to delete", StatusManager.Type.WARNING);
            NotificationManager.show("No message selected to delete", NotificationManager.Type.WARNING);
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

                StatusManager.setStatus("Message deleted successfully", StatusManager.Type.SUCCESS);
                NotificationManager.show("Message deleted successfully", NotificationManager.Type.SUCCESS);
            }

            @Override
            public void onFailure() {
                StatusManager.setStatus("Failed to delete message", StatusManager.Type.ERROR);
                NotificationManager.show("Failed to delete message", NotificationManager.Type.ERROR);
            }
        });
    }

}