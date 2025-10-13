module progr3.mail.client.app {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires java.compiler;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    // requires atlantafx.base;
    requires javafx.graphics;

    opens progr3.mail.client.app to javafx.fxml;

    exports progr3.mail.client.app;
    exports progr3.mail.client.model;
}