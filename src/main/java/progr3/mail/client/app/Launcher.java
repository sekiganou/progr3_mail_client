package progr3.mail.client.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Application;
import progr3.mail.client.api.MessageApi;
import progr3.mail.client.api.UserApi;
import progr3.mail.client.hooks.Auth;

public class Launcher {
    public static void main(String[] args) {
        var apiHandler = new ApiHandler();
        var userApi = new UserApi(apiHandler);
        var messageApi = new MessageApi(apiHandler);

        userApi.login("alessio-bagno@unito.com");
        if (Auth.isAuthenticated())
            System.out.println("User is authenticated");
        else
            System.out.println("User is NOT authenticated");

        var messages = messageApi.getMessagesForUser();
        System.out.println("Messages count: " + messages.length);
        for (var msg : messages) {
            System.out.println(" - " + msg.getSubject());
        }
    }
}