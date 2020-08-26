package com.user;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        prepareLog();
        Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
        primaryStage.setTitle("Chat july 2020");
        primaryStage.setScene(new Scene(root, 315, 335));
        primaryStage.getScene().getStylesheets().add("/css/style.css");
        primaryStage.getIcons().add(getImageFromResource("chat.jpg"));
        primaryStage.show();
    }

    private static Image getImageFromResource(String name) {
        Image image = new Image(Main.class.getClassLoader().getResourceAsStream("images/" + name));
        return image;
    }

    private void prepareLog() {
        Logger logger = Logger.getLogger("");
        for (Handler handler : logger.getHandlers()) {
            handler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                    String date = format.format(new Date(record.getMillis()));
                    return String.format("%s: %s - %s%n", record.getLevel(), date, record.getMessage());
                }
            });
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}