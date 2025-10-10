package progr3.mail.client.app;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import progr3.mail.client.api.UserApi;
import progr3.mail.client.hooks.Auth;
import progr3.mail.client.util.ToastNotification;

import java.io.IOException;

public class LoginViewController {

    @FXML
    private TextField emailField;

    @FXML
    private Label statusLabel;

    private ApiHandler apiHandler;
    private UserApi userApi;

    public LoginViewController() {
        this.apiHandler = new ApiHandler();
        this.userApi = new UserApi(apiHandler);
    }

    @FXML
    public void initialize() {
        statusLabel.setText("");
    }

    @FXML
    private void onLoginClick() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            statusLabel.setText("Please enter your email address");
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            return;
        }

        if (!email.contains("@")) {
            statusLabel.setText("Please enter a valid email address");
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            return;
        }

        statusLabel.setText("Logging in...");
        statusLabel.setStyle("-fx-text-fill: #2196F3;");

        // Perform login in background thread
        new Thread(() -> {
            var user = userApi.login(email);

            javafx.application.Platform.runLater(() -> {
                if (user != null && Auth.isAuthenticated()) {
                    ToastNotification.show("Login successful! Welcome " +
                            (user.getName() != null ? user.getName() : user.getEmail()),
                            ToastNotification.Type.SUCCESS);
                    openMessagesView();
                } else {
                    statusLabel.setText("Login failed. Please try again.");
                    statusLabel.setStyle("-fx-text-fill: #F44336;");
                }
            });
        }).start();
    }

    private void openMessagesView() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("messages-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 700);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle("Mail Client - Messages");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            ToastNotification.show("Error loading messages view",
                    ToastNotification.Type.ERROR);
        }
    }
}