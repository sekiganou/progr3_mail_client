package progr3.mail.client.app;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
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
import progr3.mail.client.models.Status;
import progr3.mail.client.models.UserStore;

import java.util.ArrayList;
import java.util.Date;

public class InboxViewController {

    @FXML
    private Label userLabel;

    @FXML
    private TableView<Message> messagesTableView;

    @FXML
    private TableColumn<Message, String> statusColumn;

    @FXML
    private TableColumn<Message, String> senderColumn;

    @FXML
    private TableColumn<Message, String> subjectColumn;

    @FXML
    private TableColumn<Message, java.util.Date> timestampColumn;

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

    private UserStore userStore;
    private MessageStore messageStore;
    private AuthStore authStore;
    private HealthStore healthStore;
    private NavigationManager navigationManager;

    public InboxViewController() {
        this.navigationManager = new NavigationManager();
        var apiHandler = new ApiHandler();

        var messageApi = new MessageApi(apiHandler);
        this.messageStore = new MessageStore(messageApi);

        var userApi = new UserApi(apiHandler);
        this.userStore = new UserStore(userApi);
        this.authStore = new AuthStore(userApi);

        var healthApi = new HealthApi(apiHandler);
        this.healthStore = new HealthStore(healthApi);
    }

    @FXML
    public void initialize() {
        setupUserInfo();
        setupTableColumns();
        setupMessageSelection();

        setupMessageListListener();
        setupHealthListener();
        setupStatusListener();

        loadMessages();

        healthStore.checkHealth();
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

        senderColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        userStore.getUserById(cellData.getValue().getSenderUserGUID()).getName()));

        subjectColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getSubject()));

        timestampColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(cellData.getValue().getDate()));

        timestampColumn.setCellFactory(column -> new TableCell<Message, Date>() {
            @Override
            protected void updateItem(Date date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(DateFormatManager.formatTimestamp(DateFormatManager.DATE_FORMATTER, date.toString()));
                }
            }
        });

        timestampColumn.setSortType(TableColumn.SortType.DESCENDING);

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
            recipients.add(userStore.getUserById(guid).getEmail());
        });
        detailRecipientsLabel.setText(String.join(", ", recipients));
        detailSenderLabel.setText(userStore.getUserById(message.getSenderUserGUID()).getName() + " <" +
                userStore.getUserById(message.getSenderUserGUID()).getEmail() + ">");
        detailSubjectLabel.setText(message.getSubject());
        detailDateLabel.setText(
                DateFormatManager.formatTimestamp(DateFormatManager.DATE_TIME_FORMATTER, message.getDate().toString()));
        detailBodyArea.setText(message.getBody());
    }

    private void loadMessages() {
        messageStore.loadMessages(new MessageStore.LoadCallback() {
            @Override
            public void onSuccess(int messageCount) {
                messagesTableView.setItems(MessageStore.getMessageList());
                messagesTableView.getSortOrder().clear();
                messagesTableView.getSortOrder().add(timestampColumn);
                messagesTableView.sort();
            }

            @Override
            public void onFailure() {
            }
        });
    }

    private void refreshMessages() {
        messageStore.loadNewMessages(new MessageStore.LoadCallback() {
            @Override
            public void onSuccess(int messageCount) {
                if (messageCount == 0) {
                    NotificationManager.show("No new messages", Status.INFO);
                    return;
                }
            }

            @Override
            public void onFailure() {
            }
        });
    }

    private void updateMessageCountLabel() {
        messageCountLabel.setText("Total messages: " + MessageStore.getMessageCount()
                +
                " | Unread: " + MessageStore.getNewMessageCount());

    }

    private void setupHealthListener() {
        HealthStore.getIsServerHealthyProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                if (newValue.intValue() == HealthStore.HEALTHY) {
                    // text is set but there is not label in the UI for it
                    StatusManager.setConnectionStatus("Server is reachable", Status.SUCCESS);
                } else {
                    StatusManager.setConnectionStatus("Server is unreachable", Status.ERROR);
                }
            });
        });
    }

    private void setupMessageListListener() {
        MessageStore.getMessageList().addListener((ListChangeListener<Message>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    Message[] addedMessages = change.getAddedSubList().toArray(new Message[0]);
                    userStore.updateUserCache(addedMessages);
                    messagesTableView.refresh();
                    updateMessageCountLabel();
                }

                if (change.wasRemoved()) {
                    messagesTableView.refresh();
                    updateMessageCountLabel();
                }

                // if (change.wasUpdated()) { ... }
            }
        });
    }

    @FXML
    private void onRefreshClick() {
        refreshMessages();
    }

    @FXML
    private void onLogoutClick() {
        var success = authStore.logout();

        if (!success) {
            NotificationManager.show("Logout failed. Please try again.",
                    Status.ERROR);
            return;
        }

        if (success) {
            NotificationManager.show("Logged out successfully",
                    Status.INFO);
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
            NotificationManager.show("Error opening compose view", Status.ERROR);
        }
    }

    @FXML
    private void onReplyClick() {
        Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();

        if (selectedMessage == null) {
            StatusManager.setStatus("No message selected to reply", Status.WARNING);
            NotificationManager.show("No message selected to reply", Status.WARNING);
            return;
        }

        User sender = userStore.getUserById(selectedMessage.getSenderUserGUID());
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
            StatusManager.setStatus("No message selected to reply all", Status.WARNING);
            NotificationManager.show("No message selected to reply all", Status.WARNING);
            return;
        }

        User sender = userStore.getUserById(selectedMessage.getSenderUserGUID());
        if (sender != null) {
            var recipients = new ArrayList<String>();
            recipients.add(sender.getEmail());
            selectedMessage.getRecipientsUserGUIDs().forEach(guid -> {
                var user = userStore.getUserById(guid);
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
            StatusManager.setStatus("No message selected to forward", Status.WARNING);
            NotificationManager.show("No message selected to forward", Status.WARNING);
            return;
        }

        String subject = "Fwd: " + selectedMessage.getSubject();
        String body = "\n\n--- Forwarded message ---\nFrom: " +
                userStore.getUserById(selectedMessage.getSenderUserGUID()).getName() + " <" +
                userStore.getUserById(selectedMessage.getSenderUserGUID()).getEmail() + ">\nDate: " +
                DateFormatManager.formatTimestamp(DateFormatManager.DATE_TIME_FORMATTER,
                        selectedMessage.getDate().toString())
                + "\nSubject: " +
                selectedMessage.getSubject() + "\n\n" + selectedMessage.getBody();
        openSendMessageViewWithPrefill("Forward Message", "", subject, body);
    }

    @FXML
    private void onDeleteClick() {
        Message selectedMessage = messagesTableView.getSelectionModel().getSelectedItem();

        messageStore.deleteMessage(selectedMessage, new MessageStore.DeleteCallback() {
            @Override
            public void onSuccess() {
                detailBodyArea.clear();
                detailDateLabel.setText("");
                detailRecipientsLabel.setText("");
                detailSenderLabel.setText("");
                detailSubjectLabel.setText("");

            }

            @Override
            public void onFailure() {
            }
        });
    }

}