package progr3.mail.client.app;

import java.net.Socket;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import progr3.mail.client.model.Request;
import progr3.mail.client.model.Response;
import progr3.mail.client.util.ToastNotification;
import progr3.mail.client.model.Request.Command;

public class ApiHandler {
    private ObjectMapper mapper = new ObjectMapper();

    public <T> T sendRequest(Command command, Object requestBody, Class<T> responseBodyType) {
        try {
            Socket socket = new Socket("localhost", 8080);

            // Prepare request
            Request request = new Request();
            request.setCommand(command);

            if (requestBody.getClass() == String.class)
                request.setBody((String) requestBody);
            else
                request.setBody(mapper.writeValueAsString(requestBody));

            // Serialize request to JSON
            String requestJson = mapper.writeValueAsString(request);
            byte[] requestBytes = requestJson.getBytes("UTF-8");

            // Send request
            socket.getOutputStream().write(requestBytes);
            socket.getOutputStream().flush();
            socket.shutdownOutput();

            // Read response
            byte[] responseBytes = socket.getInputStream().readAllBytes();
            String responseJson = new String(responseBytes, "UTF-8");

            socket.close();

            // Deserialize response from JSON
            var response = mapper.readValue(responseJson, Response.class);

            // Check for error status
            if (response.getResult() == Response.Result.FAILURE) {
                String errorMsg = response.getMessage() != null ? response.getMessage()
                        : "Request failed with status " + response.getStatus();

                Platform.runLater(() -> ToastNotification.show(errorMsg, ToastNotification.Type.ERROR));
                return null;
            }

            return mapper.readValue(response.getBody(), responseBodyType);
        } catch (Exception e) {
            System.out
                    .println("Error during API request: " + e.getMessage() + " (" + e.getClass().getSimpleName() + ")");

            Platform.runLater(() -> ToastNotification.show("Connection error: " + e.getMessage(),
                    ToastNotification.Type.ERROR));

            e.printStackTrace();
            return null;
        }
    }

}
