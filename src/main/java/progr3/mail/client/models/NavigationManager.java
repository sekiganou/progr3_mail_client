package progr3.mail.client.models;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationManager {

    public class View {
        private String title;
        private String fxmlPath;
        private int width;
        private int height;

        public View(String fxmlPath, String title, int width, int height) {
            this.fxmlPath = fxmlPath;
            this.title = title;
            this.width = width;
            this.height = height;
        }

        public String getFxmlPath() {
            return fxmlPath;
        }

        public String getTitle() {
            return title;
        }

    }

    private View loginView;
    private View inboxView;
    private View composeView;

    public View getLoginView() {
        return loginView;
    }

    public View getInboxView() {
        return inboxView;
    }

    public View getComposeView() {
        return composeView;
    }

    public NavigationManager() {
        this.loginView = new View("login-view.fxml",
                "Login",
                400,
                300);
        this.inboxView = new View("inbox-view.fxml",
                "Inbox",
                1000,
                700);
        this.composeView = new View("compose-view.fxml",
                "Compose Email",
                700,
                600);
    }

    public <Controller> Controller navigateTo(Stage fromStage, View toView) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    progr3.mail.client.app.MailClientApplication.class.getResource(toView.getFxmlPath()));
            Scene scene = new Scene(fxmlLoader.load(), toView.width, toView.height);
            fromStage.setTitle(toView.getTitle());
            fromStage.setScene(scene);

            return fxmlLoader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            NotificationManager.show("Error loading '" + toView.getTitle() + "' view",
                    NotificationManager.Type.ERROR);
            return null;
        }
    }
}