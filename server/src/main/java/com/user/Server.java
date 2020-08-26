package com.user;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Server {
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    private List<ClientHandler> clients;
    private AuthService authService;
    private ExecutorService executorService;

    // константа клиент/серверного взаимодействия (контракт)
    private static final String CLIENT_LIST = "/clientlist ";

    public Server() {
        clients = new Vector<>();
        executorService = Executors.newFixedThreadPool(5000);
        if (!SQLHandler.connect()) {
            throw new RuntimeException("Не удалось подключиться к БД");
        }
        authService = new DBAuthService();
        startServer();
    }

    private void startServer() {
        ServerSocket server = null;
        try {
            final int PORT = 8189;
            server = new ServerSocket(PORT);
            LOG.info("Сервер запущен!");
            while (true) {
                Socket socket = server.accept();
                LOG.info("socket.getRemoteSocketAddress(): " + socket.getRemoteSocketAddress());
                LOG.info("socket.getLocalSocketAddress() " + socket.getLocalSocketAddress());
                LOG.info("Клиент подключился");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            end(server);
        }
    }

    private void end(ServerSocket server) {
        try {
            executorService.shutdown();
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    void broadcastMsg(ClientHandler sender, String msg) {
        String message = String.format("%s : %s", sender.getNick(), msg);
        SQLHandler.addMessage(sender.getNick(), "null", msg, getTimeStamp());
        for (ClientHandler client : clients) {
            client.sendMsg(message);
        }
    }

    void privateMsg(ClientHandler sender, String receiver, String msg) {
        String message = String.format("[%s] private [%s] : %s", sender.getNick(), receiver, msg);

        for (ClientHandler c : clients) {
            if (c.getNick().equals(receiver)) {
                c.sendMsg(message);
                SQLHandler.addMessage(sender.getNick(), receiver, msg, getTimeStamp());
                if (!sender.getNick().equals(receiver)) {
                    sender.sendMsg(message);
                }
                return;
            }
        }
        sender.sendMsg(String.format("Client %s not found", receiver));
    }

    private String getTimeStamp() {
        Date data = new Date(System.currentTimeMillis());
        return new SimpleDateFormat("HH:mm:ss").format(data);
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public boolean isLoginAuthorized(String login) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    void broadcastClientList() {
        StringBuilder sb = new StringBuilder(CLIENT_LIST);

        for (ClientHandler c : clients) {
            sb.append(c.getNick()).append(" ");
        }

        String msg = sb.toString();
        for (ClientHandler c : clients) {
            c.sendMsg(msg);
        }
    }
}