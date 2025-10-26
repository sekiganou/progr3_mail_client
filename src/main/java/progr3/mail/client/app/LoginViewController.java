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
import progr3.mail.client.model.User;
import progr3.mail.client.models.AuthStore;
import progr3.mail.client.models.EmailValidator;
import progr3.mail.client.models.HealthStore;
import progr3.mail.client.models.NavigationManager;
import progr3.mail.client.models.StatusManager;
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
        this.healthStore = new HealthStore(healthApi);
        this.authStore = new AuthStore(userApi, healthStore);
        this.navigationManager = new NavigationManager();
    }

    @FXML
    public void initialize() {
        statusLabel.setText("");
        setupHealthListener();
        setupStatusListener();
    }

    private void setupStatusListener() {
        StatusManager.getStatusLabelText().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> statusLabel.setText(newValue));
        });

        StatusManager.getStatusLabelStyle().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> statusLabel.setStyle(newValue));
        });

        StatusManager.getConnectionLabelText().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> connectionLabel.setText(newValue));
        });

        StatusManager.getConnectionLabelStyle().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> connectionIndicator.setStyle(newValue));
        });
    }

    private void setupHealthListener() {
        healthStore.getIsServerHealthyProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                if (newValue)
                    StatusManager.setConnectionStatus("Server is reachable", StatusManager.Type.SUCCESS);
                else
                    StatusManager.setConnectionStatus("Server is unreachable", StatusManager.Type.ERROR);

            });
        });
    }

    @FXML
    private void onLoginClick() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            StatusManager.setStatus("Please enter your email address", StatusManager.Type.ERROR);
            return;
        }

        if (!EmailValidator.isValidEmail(email)) {
            StatusManager.setStatus("Please enter a valid email address", StatusManager.Type.ERROR);
            return;
        }

        StatusManager.setStatus("Logging in...", StatusManager.Type.INFO);

        authStore.login(email, new AuthStore.AuthCallback() {
            @Override
            public void onSuccess(User user) {
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
                    StatusManager.setStatus("Login failed. Please try again.", StatusManager.Type.ERROR);
                });
            }
        });
    }

}