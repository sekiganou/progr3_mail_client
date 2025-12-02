package progr3.mail.client.models;

import javafx.beans.property.SimpleStringProperty;

public class StatusManager {

    static final String STYLE_SUCCESS = "-fx-fill: #4CAF50; -fx-text-fill: #4CAF50;";
    static final String STYLE_ERROR = "-fx-fill: #F44336; -fx-text-fill: #F44336;";
    static final String STYLE_INFO = "-fx-fill: #2196F3; -fx-text-fill: #2196F3;";
    static final String STYLE_WARNING = "-fx-fill: #FF9800; -fx-text-fill: #FF9800;";
    static final String STYLE_DEFAULT = "-fx-fill: #000000; -fx-text-fill: #000000;";

    private static SimpleStringProperty statusLabelText = new SimpleStringProperty("");
    private static SimpleStringProperty statusLabelStyle = new SimpleStringProperty("");

    private static SimpleStringProperty connectionLabelText = new SimpleStringProperty("");
    private static SimpleStringProperty connectionLabelStyle = new SimpleStringProperty("");

    public static SimpleStringProperty getStatusLabelText() {
        return statusLabelText;
    }

    public static SimpleStringProperty getStatusLabelStyle() {
        return statusLabelStyle;
    }

    public static void setStatus(String text, Status status) {
        statusLabelText.set(text);

        statusLabelStyle.set(STYLE_DEFAULT);

        String style;
        switch (status) {
            case Status.SUCCESS:
                style = STYLE_SUCCESS;
                break;
            case Status.ERROR:
                style = STYLE_ERROR;
                break;
            case Status.WARNING:
                style = STYLE_WARNING;
                break;
            case Status.INFO:
            default:
                style = STYLE_INFO;
                break;
        }

        statusLabelStyle.set(style);
    }

    public static SimpleStringProperty getConnectionLabelText() {
        return connectionLabelText;
    }

    public static SimpleStringProperty getConnectionLabelStyle() {
        return connectionLabelStyle;
    }

    public static void setConnectionStatus(String text, Status status) {
        connectionLabelText.set(text);

        connectionLabelStyle.set(STYLE_DEFAULT);

        String style;
        switch (status) {
            case Status.SUCCESS:
                style = STYLE_SUCCESS;
                break;
            case Status.ERROR:
                style = STYLE_ERROR;
                break;
            case Status.WARNING:
                style = STYLE_WARNING;
                break;
            case Status.INFO:
            default:
                style = STYLE_INFO;
                break;
        }

        connectionLabelStyle.set(style);
    }
}
