package progr3.mail.client.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Application;
import progr3.mail.server.model.Request;
import progr3.mail.server.model.Response;
import progr3.mail.server.model.MailRequest.LoginBodyIn;
import progr3.mail.server.model.Request.Command;

public class Launcher {
    public static void main(String[] args) {
        /**
         * 1. connect to server
         * 2. login using login-body-in request
         * 3. retrieve response with login-body-out
         * 4. print result (response status + message)
         */
        try {
            Socket socket = new Socket("localhost", 8080);
            if (socket.isConnected())
                System.out.println("Connected to server");

            ObjectMapper mapper = new ObjectMapper();

            // Create login request
            var loginBodyIn = new LoginBodyIn();
            loginBodyIn.setEmail("alessio-bagno@unito.com");

            var request = new Request();
            request.setCommand(Command.LOGIN);
            request.setBody(mapper.writeValueAsString(loginBodyIn));

            // Get streams
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();

            // Send request - serialize to JSON
            String requestJson = mapper.writeValueAsString(request);
            os.write(requestJson.getBytes(StandardCharsets.UTF_8));
            os.flush();

            System.out.println("Request sent: " + requestJson);

            // Read response - convert bytes to string properly
            byte[] responseBytes = is.readAllBytes();
            String responseJson = new String(responseBytes, StandardCharsets.UTF_8);

            System.out.println("Response received: " + responseJson);

            // Deserialize response
            Response response = mapper.readValue(responseJson, Response.class);

            System.out.println("Response Status: " + response.getStatus());
            System.out.println("Response Message: " + response.getMessage());
            System.out.println("Response Body: " + response.getBody());

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}