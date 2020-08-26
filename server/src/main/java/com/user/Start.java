package com.user;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class Start {
    public static void main(String[] args) {
        prepareLog();
        new Server();
    }

    private static void prepareLog() {
        try {
            Path logDirPath = Paths.get("server/log");
            if (!Files.exists(logDirPath)) {
                Files.createDirectory(logDirPath);
            }
            Logger logger = Logger.getLogger("");

            Handler fileHandler = new FileHandler("server/log/log_%g.log", true);
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                    String date = format.format(new Date(record.getMillis()));
                    return String.format("%s: %s - %s%n", record.getLevel(), date, record.getMessage());
                }
            });
            logger.addHandler(fileHandler);
            // logger.setLevel(Level.OFF);
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }
}