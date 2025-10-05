module progr3.mail.client.app {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.compiler;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

    opens progr3.mail.client.app to javafx.fxml;

    exports progr3.mail.client.app;
}