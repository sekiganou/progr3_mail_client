package progr3.mail.client.models;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import progr3.mail.client.api.HealthApi;

public class HealthStore {
    private HealthApi healthApi;
    private static SimpleIntegerProperty isServerHealthy = new SimpleIntegerProperty();

    public static final int HEALTHY = 1;
    public static final int UNHEALTHY = 0;
    public static final int UNKNOWN = -1;

    public HealthStore(HealthApi healthApi) {
        this.healthApi = healthApi;
    }

    public interface HealthCallback {
        void onSuccess();

        void onFailure();
    }

    public static int getIsServerHealthy() {
        return isServerHealthy.get();
    }

    public static SimpleIntegerProperty getIsServerHealthyProperty() {
        return isServerHealthy;
    }

    public static void setConnectionStatus(int status) {
        Platform.runLater(() -> {
            if (status == HEALTHY) {
                StatusManager.setConnectionStatus("Server is reachable", Status.SUCCESS);
            } else if (status == UNHEALTHY) {
                StatusManager.setConnectionStatus("Server is not reachable", Status.ERROR);
            }
        });
    }

    public void checkHealth() {
        isServerHealthy.set(UNKNOWN);
        StatusManager.setConnectionStatus("Checking health...", Status.INFO);

        new Thread(() -> {
            isServerHealthy.set(healthApi.isServerHealthy() ? HEALTHY : UNHEALTHY);

            setConnectionStatus(isServerHealthy.get());
        }).start();
    }

    public void checkHealth(HealthCallback callback) {
        isServerHealthy.set(UNKNOWN);
        StatusManager.setConnectionStatus("Checking health...", Status.INFO);

        new Thread(() -> {
            isServerHealthy.set(healthApi.isServerHealthy() ? HEALTHY : UNHEALTHY);

            setConnectionStatus(isServerHealthy.get());

            if (isServerHealthy.get() == HEALTHY) {
                callback.onSuccess();
            } else {
                callback.onFailure();
            }
        }).start();
    }
}
