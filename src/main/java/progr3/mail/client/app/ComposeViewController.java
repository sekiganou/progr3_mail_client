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
import progr3.mail.client.models.MessageStore;
import progr3.mail.client.models.NavigationManager;
import progr3.mail.client.models.Status;
import progr3.mail.client.models.StatusManager;

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
        String to = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String body = bodyArea.getText().trim();
        String[] recipientEmails = to.split(",");

        messageStore.sendMessage(List.of(recipientEmails), subject, body, new MessageStore.SendCallback() {
            @Override
            public void onSuccess() {
                Platform.runLater(() -> {
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
            }
        });
    }

    @FXML
    private void onClearClick() {
        clearForm();
        StatusManager.setStatus("Form cleared", Status.INFO);
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