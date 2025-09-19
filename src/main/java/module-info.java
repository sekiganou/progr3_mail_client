module progr3.mail.client.progr3_mail_client {
    requires javafx.controls;
    requires javafx.fxml;


    opens progr3.mail.client.progr3_mail_client to javafx.fxml;
    exports progr3.mail.client.progr3_mail_client;
}