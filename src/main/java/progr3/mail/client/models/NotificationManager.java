package progr3.mail.client.models;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class NotificationManager {

    private static int activeNotifications = 0;
    private static final int NOTIFICATION_HEIGHT = 50;
    private static final int NOTIFICATION_WIDTH = 300;
    private static final int SPACING = 10;
    private static final int ANIMATION_DURATION_MS = 300;
    private static final int DISPLAY_DURATION_MS = 3000;
    private static final float OPACITY = 0.95f;

    public static void show(String message, Status status) {
        Stage toastStage = new Stage();
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);

        Text text = new Text(message);
        text.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        text.setFill(Color.WHITE);

        StackPane root = new StackPane(text);
        root.setStyle(getStyleForStatus(status));
        root.setPrefHeight(NOTIFICATION_HEIGHT);
        root.setAlignment(Pos.CENTER);
        root.setOpacity(0);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);

        // Position at top center of screen
        toastStage.setX((javafx.stage.Screen.getPrimary().getVisualBounds().getWidth() - NOTIFICATION_WIDTH) / 2);
        toastStage.setY(NOTIFICATION_HEIGHT + (activeNotifications * (NOTIFICATION_HEIGHT + SPACING)));

        toastStage.show();
        activeNotifications++;

        // Fade in
        Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(root.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(ANIMATION_DURATION_MS), new KeyValue(root.opacityProperty(), OPACITY)));

        // Fade out and close
        Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(root.opacityProperty(), OPACITY)),
                new KeyFrame(Duration.millis(ANIMATION_DURATION_MS), new KeyValue(root.opacityProperty(), 0)));
        fadeOut.setDelay(Duration.millis(DISPLAY_DURATION_MS));
        fadeOut.setOnFinished(e -> {
            toastStage.close();
            activeNotifications = Math.max(0, activeNotifications - 1);
        });

        fadeIn.play();
        fadeIn.setOnFinished(e -> fadeOut.play());
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
        return "-fx-background-color: " + color + "; -fx-background-radius: 5px; -fx-padding: 15px 30px;";
    }

}