package progr3.mail.client.models;

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

    public static SimpleIntegerProperty getIsServerHealthyProperty() {
        return isServerHealthy;
    }

    public void checkHealth() {
        isServerHealthy.set(UNKNOWN);
        StatusManager.setConnectionStatus("Checking health...", StatusManager.Type.INFO);

        new Thread(() -> {
            isServerHealthy.set(healthApi.isServerHealthy() ? HEALTHY : UNHEALTHY);
        }).start();
    }

    public void checkHealth(HealthCallback callback) {
        isServerHealthy.set(UNKNOWN);
        StatusManager.setConnectionStatus("Checking health...", StatusManager.Type.INFO);

        new Thread(() -> {
            isServerHealthy.set(healthApi.isServerHealthy() ? HEALTHY : UNHEALTHY);
            if (isServerHealthy.get() == 1) {
                callback.onSuccess();
            } else {
                callback.onFailure();
            }
        }).start();
    }
}
