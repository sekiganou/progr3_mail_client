package progr3.mail.client.util;

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

public class ToastNotification {

    public enum Type {
        SUCCESS, ERROR, INFO, WARNING
    }

    public static void show(String message, Type type) {
        Stage toastStage = new Stage();
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);

        Text text = new Text(message);
        text.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        text.setFill(Color.WHITE);

        StackPane root = new StackPane(text);
        root.setStyle(getStyleForType(type));
        root.setPrefHeight(50);
        root.setAlignment(Pos.CENTER);
        root.setOpacity(0);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);

        // Position at top center of screen
        toastStage.setX((javafx.stage.Screen.getPrimary().getVisualBounds().getWidth() - 300) / 2);
        toastStage.setY(50);

        toastStage.show();

        // Fade in
        Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(root.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(300), new KeyValue(root.opacityProperty(), 0.95)));

        // Fade out and close
        Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(root.opacityProperty(), 0.95)),
                new KeyFrame(Duration.millis(300), new KeyValue(root.opacityProperty(), 0)));
        fadeOut.setDelay(Duration.seconds(3));
        fadeOut.setOnFinished(e -> toastStage.close());

        fadeIn.play();
        fadeIn.setOnFinished(e -> fadeOut.play());
    }

    private static String getStyleForType(Type type) {
        switch (type) {
            case SUCCESS:
                return "-fx-background-color: #4CAF50; -fx-background-radius: 5px; -fx-padding: 15px 30px;";
            case ERROR:
                return "-fx-background-color: #F44336; -fx-background-radius: 5px; -fx-padding: 15px 30px;";
            case WARNING:
                return "-fx-background-color: #FF9800; -fx-background-radius: 5px; -fx-padding: 15px 30px;";
            case INFO:
            default:
                return "-fx-background-color: #2196F3; -fx-background-radius: 5px; -fx-padding: 15px 30px;";
        }
    }
}