package progr3.mail.client.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import progr3.mail.client.api.MessageApi;
import progr3.mail.client.hooks.Auth;
import progr3.mail.client.util.ToastNotification;

import java.io.IOException;
import java.util.List;

public class SendMessageViewController {

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

    private ApiHandler apiHandler;
    private MessageApi messageApi;

    public SendMessageViewController() {
        this.apiHandler = new ApiHandler();
        this.messageApi = new MessageApi(apiHandler);
    }

    @FXML
    public void initialize() {
        setupUserInfo();
        setupCharacterCounter();
        statusLabel.setText("");
    }

    private void setupUserInfo() {
        if (Auth.isAuthenticated()) {
            fromLabel.setText(Auth.getUser().getEmail());
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
            if (!recipients[i].contains("@")) {
                statusLabel.setText("Invalid email format: " + recipients[i]);
                statusLabel.setStyle("-fx-text-fill: #F44336;");
                ToastNotification.show("Invalid email: " + recipients[i],
                        ToastNotification.Type.WARNING);
                return;
            }
        }

        // Send message
        statusLabel.setText("Sending message...");
        statusLabel.setStyle("-fx-text-fill: #2196F3;");

        new Thread(() -> {
            boolean success = messageApi.sendMessage(List.of(recipients), subject, body) != null;

            Platform.runLater(() -> {
                if (success) {
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
                            Platform.runLater(this::onBackClick);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    statusLabel.setText("Failed to send message");
                    statusLabel.setStyle("-fx-text-fill: #F44336;");
                    // Toast notification already shown by ApiHandler
                }
            });
        }).start();
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
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("messages-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 700);

            Stage stage = (Stage) fromLabel.getScene().getWindow();
            stage.setTitle("Mail Client - Messages");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            ToastNotification.show("Error loading messages view",
                    ToastNotification.Type.ERROR);
        }
    }
}