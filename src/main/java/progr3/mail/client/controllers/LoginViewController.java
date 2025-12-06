package progr3.mail.client.controllers;

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
import progr3.mail.client.models.StatusManager;
import progr3.mail.client.models.NotificationManager;
import progr3.mail.client.models.Status;

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
    private NavigationManager navigationManager;

    private HealthStore healthStore;

    public LoginViewController() {
        var apiHandler = new ApiHandler();

        var userApi = new UserApi(apiHandler);
        this.authStore = new AuthStore(userApi);
        this.navigationManager = new NavigationManager();

        var healthApi = new HealthApi(apiHandler);
        this.healthStore = new HealthStore(healthApi);
    }

    @FXML
    public void initialize() {
        statusLabel.setText("");
        setupHealthListener();
        setupStatusListener();

        healthStore.checkHealth();
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
        HealthStore.getIsServerHealthyProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                if (newValue.intValue() == HealthStore.HEALTHY)
                    StatusManager.setConnectionStatus("Server is reachable", Status.SUCCESS);
                else
                    StatusManager.setConnectionStatus("Server is unreachable", Status.ERROR);

            });
        });
    }

    @FXML
    private void onLoginClick() {
        String email = emailField.getText().trim();

        if (!EmailValidator.isValidEmail(email)) {
            StatusManager.setStatus("Please enter a valid email address", Status.WARNING);
            NotificationManager.show("Invalid email format", Status.WARNING);
            return;
        }

        StatusManager.setStatus("Logging in...", Status.INFO);

        authStore.login(email, new AuthStore.AuthCallback() {
            @Override
            public void onSuccess() {
                navigationManager.navigateTo((Stage) emailField.getScene().getWindow(),
                        navigationManager.getInboxView());
            }

            @Override
            public void onFailure() {

            }
        });
    }

}