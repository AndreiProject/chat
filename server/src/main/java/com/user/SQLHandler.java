package com.user;

import java.sql.*;

public class SQLHandler {
    private static Connection connection;
    private static PreparedStatement psChangeNick;
    private static PreparedStatement psGetNickName;
    private static PreparedStatement psRegistration;

    private static PreparedStatement psAddMessage;
    private static PreparedStatement psGetMessageForNick;

    public static boolean connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:server/main.db");
            prepareAllStatements();
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void prepareAllStatements() throws SQLException {
        psGetNickName = connection.prepareStatement("SELECT nickname FROM users WHERE login = ? AND password = ?;");
        psRegistration = connection.prepareStatement("INSERT INTO users(login, password, nickname) VALUES (? ,? ,? );");
        psChangeNick = connection.prepareStatement("UPDATE users SET nickname = ? WHERE nickname = ?;");

        psAddMessage = connection.prepareStatement("INSERT INTO messages (sender, receiver, text, date) VALUES (\n" +
                "(SELECT id FROM users WHERE nickname=?),\n" +
                "(SELECT id FROM users WHERE nickname=?),\n" +
                "?, ?)");

        psGetMessageForNick = connection.prepareStatement("SELECT (SELECT nickname FROM users WHERE id = sender), \n" +
                "       (SELECT nickname FROM users WHERE id = receiver),\n" +
                "       text,\n" +
                "       date \n" +
                "FROM messages \n" +
                "WHERE sender = (SELECT id FROM users WHERE nickname = ?)\n" +
                "OR receiver = (SELECT id FROM users WHERE nickname = ?)\n" +
                "OR receiver is null");
    }

    public static String getNicknameByLoginAndPassword(String login, String password) {
        String nick = null;
        try {
            psGetNickName.setString(1, login);
            psGetNickName.setString(2, password);
            ResultSet rs = psGetNickName.executeQuery();
            if (rs.next()) {
                nick = rs.getString(1);
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nick;
    }

    public static boolean registration(String login, String password, String nickname) {
        try {
            psRegistration.setString(1, login);
            psRegistration.setString(2, password);
            psRegistration.setString(3, nickname);
            psRegistration.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean changeNick(String oldNickname, String newNickname) {
        try {
            psChangeNick.setString(1, newNickname);
            psChangeNick.setString(2, oldNickname);
            psChangeNick.executeUpdate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * метод добавления сообщения в БД
     * @param sender ник отправителя
     * @param receiver ник получателя "null" если всем пользователям
     * @param text текст сообщения
     * @param date дата и время сообщения в текстовом виде
     */
    public static boolean addMessage(String sender, String receiver, String text, String date) {
        try {
            psAddMessage.setString(1, sender);
            psAddMessage.setString(2, receiver);
            psAddMessage.setString(3, text);
            psAddMessage.setString(4, date);
            psAddMessage.executeUpdate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * метод извлечения сообщений из БД
     * всех сообщений пользователя с ником nick,
     * отправленные им и приходящие к нему
     * @param nick ник пользователя, сообщения которого извлекаются
     * @return возвращает сроку сформированную из всех сообщений, которые должен увидеть данный пользователь
     */
    public static String getMessageForNick(String nick) {
        StringBuilder sb = new StringBuilder();

        try {
            psGetMessageForNick.setString(1, nick);
            psGetMessageForNick.setString(2, nick);
            ResultSet rs = psGetMessageForNick.executeQuery();

            while (rs.next()) {
                String sender = rs.getString(1);
                String receiver = rs.getString(2);
                String text = rs.getString(3);
                String date = rs.getString(4);
                //всем сообщение
                if (receiver == null) {
                    sb.append(String.format("%s : %s : %s\n", sender, text, date));
                } else {
                    sb.append(String.format("[ %s ] private [ %s ]: %s : %s\n", sender, receiver, text, date));
                }
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static void disconnect() {
        close(psRegistration);
        close(psGetNickName);
        close(psChangeNick);
        close(psAddMessage);
        close(psGetMessageForNick);
        close(connection);
    }

    private static void close(AutoCloseable ob) {
        if (ob != null) {
            try {
                ob.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
