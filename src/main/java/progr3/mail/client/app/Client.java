package progr3.mail.client.app;

import javafx.application.Platform;
import progr3.mail.client.api.ApiHandler;
import progr3.mail.client.api.HealthApi;
import progr3.mail.client.models.HealthStore;

public class Client {

    private static final int POLLING_INTERVAL_SECONDS = 30;
    private HealthStore healthStore;

    public Client() {
        var apiHandler = new ApiHandler();
        var healthApi = new HealthApi(apiHandler);
        this.healthStore = new HealthStore(healthApi);
    }

    private void checkHealth() {
        new Thread(() -> {
            try {
                Thread.sleep((long) (POLLING_INTERVAL_SECONDS * 1000));

                Platform.runLater(() -> {
                    healthStore.checkHealth(new HealthStore.HealthCallback() {
                        @Override
                        public void onSuccess() {
                            checkHealth();
                        }

                        @Override
                        public void onFailure() {
                            checkHealth();
                        }
                    });
                });

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void start() {
        healthStore.checkHealth();

        checkHealth();
    }
}
