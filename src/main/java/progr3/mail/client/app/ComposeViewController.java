package progr3.mail.client.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import progr3.mail.client.api.ApiHandler;
import progr3.mail.client.api.MessageApi;
import progr3.mail.client.models.AuthStore;
import progr3.mail.client.models.EmailValidator;
import progr3.mail.client.models.MessageStore;
import progr3.mail.client.models.NavigationManager;
import progr3.mail.client.util.ToastNotification;
import java.util.List;

public class ComposeViewController {

    @FXML
    public Label headerLabel;

    @FXML
    public Label fromLabel;

    @FXML
    public TextField toField;

    @FXML
    public TextField subjectField;

    @FXML
    public TextArea bodyArea;

    @FXML
    private Label statusLabel;

    @FXML
    private Label charCountLabel;

    private NavigationManager navigationManager;
    private MessageStore messageStore;

    public ComposeViewController() {
        this.navigationManager = new NavigationManager();

        var apiHandler = new ApiHandler();
        var messageApi = new MessageApi(apiHandler);
        this.messageStore = new MessageStore(messageApi);
    }

    @FXML
    public void initialize() {
        setupUserInfo();
        setupCharacterCounter();
        statusLabel.setText("");
    }

    public void prefillForm(String header, String to, String subject, String body) {
        if (header != null && !header.isEmpty()) {
            headerLabel.setText(header);
        }

        if (to != null && !to.isEmpty()) {
            toField.setText(to);
        }
        if (subject != null && !subject.isEmpty()) {
            subjectField.setText(subject);
        }
        if (body != null && !body.isEmpty()) {
            bodyArea.setText(body);
        }
        // Set the from field with current user
        if (AuthStore.getUser() != null) {
            fromLabel.setText(AuthStore.getUser().getEmail());
        }
    }

    private void setupUserInfo() {
        if (AuthStore.isAuthenticated()) {
            fromLabel.setText(AuthStore.getUser().getEmail());
        }
    }

    private void setupCharacterCounter() {
        bodyArea.textProperty().addListener((observable, oldValue, newValue) -> {
            int charCount = newValue != null ? newValue.length() : 0;
            charCountLabel.setText("Characters: " + charCount);
        });
    }

    @FXML
    private void onSendClick() {
        // Validate fields
        String to = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String body = bodyArea.getText().trim();

        if (to.isEmpty()) {
            statusLabel.setText("Please enter recipient email(s)");
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            ToastNotification.show("Recipient email is required", ToastNotification.Type.WARNING);
            return;
        }

        if (subject.isEmpty()) {
            statusLabel.setText("Please enter a subject");
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            ToastNotification.show("Subject is required", ToastNotification.Type.WARNING);
            return;
        }

        if (body.isEmpty()) {
            statusLabel.setText("Please enter a message");
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            ToastNotification.show("Message body is required", ToastNotification.Type.WARNING);
            return;
        }

        // Parse recipients
        String[] recipients = to.split(",");
        for (int i = 0; i < recipients.length; i++) {
            recipients[i] = recipients[i].trim();
            var recipient = recipients[i];
            if (!EmailValidator.isValidEmail(recipient)) {
                statusLabel.setText("Invalid email format: " + recipient);
                statusLabel.setStyle("-fx-text-fill: #F44336;");
                ToastNotification.show("Invalid email: " + recipient,
                        ToastNotification.Type.WARNING);
                return;
            }
        }

        // Send message
        statusLabel.setText("Sending message...");
        statusLabel.setStyle("-fx-text-fill: #2196F3;");

        messageStore.sendMessage(List.of(recipients), subject, body, new MessageStore.SendCallback() {
            @Override
            public void onSuccess() {
                Platform.runLater(() -> {
                    statusLabel.setText("Message sent successfully!");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                    ToastNotification.show("Message sent to " + recipients.length +
                            " recipient(s)", ToastNotification.Type.SUCCESS);

                    // Clear form after successful send
                    clearForm();

                    // Optional: Go back to messages view after a delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            Platform.runLater(ComposeViewController.this::onBackClick);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });
            }

            @Override
            public void onFailure() {
                Platform.runLater(() -> {
                    statusLabel.setText("Failed to send message");
                    statusLabel.setStyle("-fx-text-fill: #F44336;");
                });
            }
        });
    }

    @FXML
    private void onClearClick() {
        clearForm();
        statusLabel.setText("Form cleared");
        statusLabel.setStyle("-fx-text-fill: #666666;");
    }

    private void clearForm() {
        toField.clear();
        subjectField.clear();
        bodyArea.clear();
    }

    @FXML
    private void onBackClick() {
        Stage stage = (Stage) fromLabel.getScene().getWindow();
        navigationManager.navigateTo(stage, navigationManager.getInboxView());
    }
}