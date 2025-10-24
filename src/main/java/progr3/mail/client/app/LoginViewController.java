package progr3.mail.client.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import progr3.mail.client.api.ApiHandler;
import progr3.mail.client.api.HealthApi;
import progr3.mail.client.api.UserApi;
import progr3.mail.client.models.AuthStore;
import progr3.mail.client.models.EmailValidator;
import progr3.mail.client.models.HealthStore;
import progr3.mail.client.models.NavigationManager;
import progr3.mail.client.util.ToastNotification;

public class LoginViewController {

    @FXML
    private TextField emailField;

    @FXML
    private Label statusLabel;

    @FXML
    private Circle connectionIndicator;

    @FXML
    private Label connectionLabel;

    private AuthStore authStore;
    private HealthStore healthStore;
    private NavigationManager navigationManager;

    public LoginViewController() {
        var apiHandler = new ApiHandler();
        var userApi = new UserApi(apiHandler);
        var healthApi = new HealthApi(apiHandler);
        this.authStore = new AuthStore(userApi);
        this.healthStore = new HealthStore(healthApi);
        this.navigationManager = new NavigationManager();
    }

    @FXML
    public void initialize() {
        statusLabel.setText("");
        setupHealthListener();
    }

    private void setupHealthListener() {
        healthStore.isServerHealthyProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                if (newValue) {
                    connectionIndicator.setFill(javafx.scene.paint.Color.LIMEGREEN);
                    // connectionIndicator.setStroke(javafx.scene.paint.Color.DARKGREEN);
                    connectionLabel.setText("Server is reachable");
                } else {
                    System.out.println("server is unreachable");
                    connectionIndicator.setFill(javafx.scene.paint.Color.RED);
                    // connectionIndicator.setStroke(javafx.scene.paint.Color.DARKRED);
                    connectionLabel.setText("Server is unreachable");
                }
            });
        });
    }

    @FXML
    private void onLoginClick() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            statusLabel.setText("Please enter your email address");
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            return;
        }

        if (!EmailValidator.isValidEmail(email)) {
            statusLabel.setText("Please enter a valid email address");
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            return;
        }

        statusLabel.setText("Logging in...");
        statusLabel.setStyle("-fx-text-fill: #2196F3;");

        authStore.login(email, new AuthStore.AuthCallback() {
            @Override
            public void onSuccess(progr3.mail.client.model.User user) {
                Platform.runLater(() -> {
                    ToastNotification.show("Login successful! Welcome " +
                            (user.getName() != null ? user.getName() : user.getEmail()),
                            ToastNotification.Type.SUCCESS);
                    navigationManager.navigateTo((Stage) emailField.getScene().getWindow(),
                            navigationManager.getInboxView());
                });
            }

            @Override
            public void onFailure() {
                Platform.runLater(() -> {
                    statusLabel.setText("Login failed. Please try again.");
                    statusLabel.setStyle("-fx-text-fill: #F44336;");
                });
            }
        });
    }

}