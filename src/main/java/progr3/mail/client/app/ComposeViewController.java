package progr3.mail.client.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import progr3.mail.client.api.ApiHandler;
import progr3.mail.client.api.HealthApi;
import progr3.mail.client.api.MessageApi;
import progr3.mail.client.models.AuthStore;
import progr3.mail.client.models.EmailValidator;
import progr3.mail.client.models.HealthStore;
import progr3.mail.client.models.MessageStore;
import progr3.mail.client.models.NavigationManager;
import progr3.mail.client.models.StatusManager;
import progr3.mail.client.models.NotificationManager;

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
        var healthApi = new HealthApi(apiHandler);
        var healthStore = new HealthStore(healthApi);
        this.messageStore = new MessageStore(messageApi, healthStore);
    }

    @FXML
    public void initialize() {
        setupUserInfo();
        setupCharacterCounter();
        setupStatusListener();
    }

    private void setupStatusListener() {
        StatusManager.getStatusLabelText().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> statusLabel.setText(newValue));
        });

        StatusManager.getStatusLabelStyle().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> statusLabel.setStyle(newValue));
        });
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

        // Parse recipients
        String[] recipients = to.split(",");
        for (int i = 0; i < recipients.length; i++) {
            recipients[i] = recipients[i].trim();
            var recipient = recipients[i];
            if (!EmailValidator.isValidEmail(recipient)) {
                StatusManager.setStatus("Invalid email: " + recipient, StatusManager.Type.WARNING);
                NotificationManager.show("Invalid email: " + recipient,
                        NotificationManager.Type.WARNING);
                return;
            }
        }

        StatusManager.setStatus("Sending message...", StatusManager.Type.INFO);

        messageStore.sendMessage(List.of(recipients), subject, body, new MessageStore.SendCallback() {
            @Override
            public void onSuccess() {
                Platform.runLater(() -> {
                    StatusManager.setStatus("Message sent successfully", StatusManager.Type.SUCCESS);
                    NotificationManager.show("Message sent to " + recipients.length +
                            " recipient(s)", NotificationManager.Type.SUCCESS);

                    clearForm();

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
                    StatusManager.setStatus("Failed to send message", StatusManager.Type.ERROR);
                });
            }
        });
    }

    @FXML
    private void onClearClick() {
        clearForm();
        StatusManager.setStatus("Form cleared", StatusManager.Type.INFO);
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