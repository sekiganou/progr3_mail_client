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
import progr3.mail.client.models.NotificationManager;
import progr3.mail.client.models.StatusManager;

public class Client {

    private static final int POLLING_INTERVAL_SECONDS = 30;
    private HealthStore healthStore;
    private MessageStore messageStore;
    private ScheduledExecutorService scheduler;

    public Client() {
        var apiHandler = new ApiHandler();
        var healthApi = new HealthApi(apiHandler);
        var messageApi = new MessageApi(apiHandler);
        this.healthStore = new HealthStore(healthApi);
        this.messageStore = new MessageStore(messageApi, healthStore);
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        healthStore.checkHealth();

        scheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                healthStore.checkHealth();

                messageStore.loadNewMessages(new MessageStore.LoadCallback() {
                    @Override
                    public void onSuccess(int messageCount) {
                        if (messageCount == 0) {
                            StatusManager.setStatus("No new messages", StatusManager.Type.INFO);
                            return;
                        }

                        StatusManager.setStatus("Loaded " + messageCount + " new message(s)",
                                StatusManager.Type.SUCCESS);
                        NotificationManager.show("Loaded " + messageCount + " new message(s)",
                                NotificationManager.Type.SUCCESS);

                    }

                    @Override
                    public void onFailure() {
                        StatusManager.setStatus("Failed to load new messages", StatusManager.Type.ERROR);
                        NotificationManager.show("Failed to load new messages", NotificationManager.Type.ERROR);
                    }
                });
            });
        }, POLLING_INTERVAL_SECONDS, POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }
}
