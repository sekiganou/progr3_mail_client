package progr3.mail.client.app;

import javafx.application.Application;
import javafx.stage.Stage;
import progr3.mail.client.models.NavigationManager;

import java.io.IOException;

public class MailClientApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        NavigationManager navigationManager = new NavigationManager();
        navigationManager.navigateTo(stage, navigationManager.getLoginView());
        stage.show();
    }

}
