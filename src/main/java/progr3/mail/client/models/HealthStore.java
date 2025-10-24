package progr3.mail.client.models;

import javafx.beans.property.SimpleBooleanProperty;
import progr3.mail.client.api.HealthApi;

public class HealthStore {
    private HealthApi healthApi;
    private static SimpleBooleanProperty isServerHealthy = new SimpleBooleanProperty();

    public HealthStore(HealthApi healthApi) {
        this.healthApi = healthApi;
    }

    public interface HealthCallback {
        void onSuccess();

        void onFailure();
    }

    public SimpleBooleanProperty isServerHealthyProperty() {
        return isServerHealthy;
    }

    public void checkHealth() {
        new Thread(() -> {
            isServerHealthy.set(healthApi.isServerHealthy());
        }).start();
    }

    public void checkHealth(HealthCallback callback) {
        new Thread(() -> {
            isServerHealthy.set(healthApi.isServerHealthy());
            if (isServerHealthy.get()) {
                callback.onSuccess();
            } else {
                callback.onFailure();
            }
        }).start();
    }
}
