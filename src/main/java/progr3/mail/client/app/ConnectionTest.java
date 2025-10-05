package progr3.mail.client.app;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;

import progr3.mail.server.model.Request;
import progr3.mail.server.model.Response;
import progr3.mail.server.model.MailRequest.LoginBodyIn;
import progr3.mail.server.model.MailResponse.LoginBodyOut;
import progr3.mail.server.model.Request.Command;

public class ConnectionTest {
    public static void main(String[] args) {
        System.out.println("=== Starting Connection Test ===");

        try {
            // Step 1: Connect
            System.out.println("\n1. Connecting to server...");
            Socket socket = new Socket("localhost", 8080);
            System.out.println("✓ Connected: " + socket.isConnected());
            System.out.println("  Local: " + socket.getLocalSocketAddress());
            System.out.println("  Remote: " + socket.getRemoteSocketAddress());

            // Step 2: Prepare request
            System.out.println("\n2. Preparing request...");
            ObjectMapper mapper = new ObjectMapper();

            LoginBodyIn loginBodyIn = new LoginBodyIn();
            loginBodyIn.setEmail("luca.bianchi@unito.com");

            Request request = new Request();
            request.setCommand(Command.LOGIN);
            request.setBody(mapper.writeValueAsString(loginBodyIn));

            String requestJson = mapper.writeValueAsString(request);
            System.out.println("✓ Request JSON:");
            System.out.println("  " + requestJson);
            System.out.println("  Length: " + requestJson.length() + " chars");

            // Step 3: Send request
            System.out.println("\n3. Sending request...");
            OutputStream os = socket.getOutputStream();
            byte[] requestBytes = requestJson.getBytes(StandardCharsets.UTF_8);
            System.out.println("  Bytes to send: " + requestBytes.length);

            os.write(requestBytes);
            os.flush();
            System.out.println("✓ Request sent");

            // Step 4: Signal end of writing
            System.out.println("\n4. Closing output stream...");
            socket.shutdownOutput();
            System.out.println("✓ Output stream closed");

            // Step 5: Wait for response
            System.out.println("\n5. Waiting for response...");
            InputStream is = socket.getInputStream();

            System.out.println("  Reading response...");
            byte[] responseBytes = is.readAllBytes();
            System.out.println("✓ Response received: " + responseBytes.length + " bytes");

            if (responseBytes.length == 0) {
                System.out.println("✗ ERROR: Empty response!");
                socket.close();
                return;
            }

            String responseJson = new String(responseBytes, StandardCharsets.UTF_8);
            System.out.println("  Response JSON:");
            System.out.println("  " + responseJson);

            // Step 6: Parse response
            System.out.println("\n6. Parsing response...");
            Response response = mapper.readValue(responseJson, Response.class);
            System.out.println("✓ Response parsed successfully");
            System.out.println("  Status: " + response.getStatus());
            System.out.println("  Message: " + response.getMessage());
            LoginBodyOut loginBodyOut = mapper.readValue(response.getBody(), LoginBodyOut.class);
            System.out.println("  Body: " + response.getBody());
            System.out.println("Email: " + loginBodyOut.getEmail());

            // Step 7: Close
            System.out.println("\n7. Closing connection...");
            socket.close();
            System.out.println("✓ Connection closed");

            System.out.println("\n=== Test Completed Successfully ===");

        } catch (Exception e) {
            System.out.println("\n✗ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}