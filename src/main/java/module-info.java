module progr3.mail.client.app {
    requires javafx.controls;
    requires javafx.fxml;


    opens progr3.mail.client.app to javafx.fxml;
    exports progr3.mail.client.app;
}