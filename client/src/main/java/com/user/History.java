package com.user;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class History {
    private static PrintWriter out;
    private static final String DIR_NAME = "client/history";

    private static String getHistoryFilenameByLogin(String login) {
        return DIR_NAME + "/history_" + login + ".txt";
    }

    public static void start(String login) {
        try {
            Path pathDir = Paths.get(DIR_NAME);
            if (!Files.exists(pathDir)) {
                Files.createDirectory(pathDir);
            }
            out = new PrintWriter(new FileOutputStream(getHistoryFilenameByLogin(login), true), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        if (out != null) {
            out.close();
        }
    }

    public static void writeLine(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public static String getLast100LinesOfHistory(String login) {
        if (!Files.exists(Paths.get(getHistoryFilenameByLogin(login)))) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try {
            List<String> historyLines = Files.readAllLines(Paths.get(getHistoryFilenameByLogin(login)));
            int startPosition = 0;
            if (historyLines.size() > 100) {
                startPosition = historyLines.size() - 100;
            }
            for (int i = startPosition; i < historyLines.size(); i++) {
                sb.append(historyLines.get(i)).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}