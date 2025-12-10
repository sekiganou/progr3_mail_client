package progr3.mail.client.models;

import javafx.beans.property.SimpleStringProperty;

public class StatusManager {

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

        statusLabelStyle.set(getStyle(ColorManager.DEFAULT));

        String style = getStyleForStatus(status);

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

        connectionLabelStyle.set(getStyle(ColorManager.DEFAULT));
        String style = getStyleForStatus(status);
        connectionLabelStyle.set(style);
    }

    public static void setConnectionStatus(Status status) {
        connectionLabelStyle.set(getStyle(ColorManager.DEFAULT));
        String style = getStyleForStatus(status);
        connectionLabelStyle.set(style);
    }

    private static String getStyleForStatus(Status status) {
        switch (status) {
            case SUCCESS:
                return getStyle(ColorManager.SUCCESS);
            case ERROR:
                return getStyle(ColorManager.ERROR);
            case WARNING:
                return getStyle(ColorManager.WARNING);
            case INFO:
            default:
                return getStyle(ColorManager.INFO);
        }
    }

    private static String getStyle(String color) {
        return "-fx-fill: " + color + "; -fx-text-fill: " + color + ";";
    }
}
