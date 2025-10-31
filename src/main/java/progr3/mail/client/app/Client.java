package progr3.mail.client.app;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import progr3.mail.client.api.ApiHandler;
import progr3.mail.client.api.HealthApi;
import progr3.mail.client.api.MessageApi;
import progr3.mail.client.models.HealthStore;
import progr3.mail.client.models.MessageStore;

public class Client {

    private static final int POLLING_MESSAGES_SECONDS = 30;
    private static final int POLLING_HEALTH_SECONDS = 5;
    private HealthStore healthStore;
    private MessageStore messageStore;
    private ScheduledExecutorService scheduler;

    public Client() {
        var apiHandler = new ApiHandler();
        var healthApi = new HealthApi(apiHandler);
        var messageApi = new MessageApi(apiHandler);
        this.healthStore = new HealthStore(healthApi);
        this.messageStore = new MessageStore(messageApi);
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                healthStore.checkHealth();
            });
        }, POLLING_HEALTH_SECONDS, POLLING_HEALTH_SECONDS, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                messageStore.loadNewMessages(new MessageStore.LoadCallback() {
                    @Override
                    public void onSuccess(int messageCount) {
                    }

                    @Override
                    public void onFailure() {
                    }
                });
            });
        }, POLLING_MESSAGES_SECONDS, POLLING_MESSAGES_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }
}
