package progr3.mail.client.api;

import progr3.mail.client.model.Response;

public class HealthApi {
    private ApiHandler api;

    public HealthApi(ApiHandler api) {
        this.api = api;
    }

    public boolean isServerHealthy() {
        String status = api.sendRequest(progr3.mail.client.model.Request.Command.HEALTH, "",
                String.class);

        if (status == null)
            return false;

        if (Response.Status.OK == Response.Status.valueOf(status))
            return true;
        else
            return false;
    }
}
