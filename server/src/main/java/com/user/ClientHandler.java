package com.user;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

public class ClientHandler {
    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getSimpleName());

    private String nick;
    private String login;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    // константы клиент/серверного взаимодействия (контракт)
    private static final String ATTACH = "/end";
    private static final String AUTHORIZATION = "/auth ";
    private static final String AUTHORIZATION_SUCCESS = "/authok ";
    private static final String REGISTRATION = "/reg ";
    private static final String REGISTRATION_SUCCESS = "/regresult ok";
    private static final String REGISTRATION_FAILED = "/regresult failed";
    private static final String YOUR_NICKS = "/yournickis ";
    private static final String SPECIAL_ACTIONS = "/";
    private static final String PRIVATE_CHANNEL = "/w ";
    private static final String CHANGE_NICK = "/chnick ";

    public ClientHandler(Server server, Socket socket) {
        try {
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            server.getExecutorService().execute(() -> {
                try {
                    socket.setSoTimeout(120000);
                    authentication(server, socket, in);
                    communication(server, in);
                } catch (SocketTimeoutException e) {
                    sendMsg(ATTACH);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    LOG.info("Клиент отключился");
                    server.unsubscribe(this);
                    connectionClose();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            connectionClose();
        }
    }

    private void connectionClose() {
        close(in);
        close(out);
        close(socket);
    }

    private void close(Closeable ob) {
        if (ob != null) {
            try {
                ob.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void authentication(Server server, Socket socket, DataInputStream in) throws IOException {
        // цикл аутентификации
        while (true) {
            String str = in.readUTF();

            if (str.startsWith(AUTHORIZATION)) {
                String[] token = str.split("\\s");
                if (token.length < 3) {
                    continue;
                }
                String newNick = server
                        .getAuthService()
                        .getNicknameByLoginAndPassword(token[1], token[2]);
                login = token[1];
                if (newNick != null) {
                    if (!server.isLoginAuthorized(login)) {
                        sendMsg(AUTHORIZATION_SUCCESS + newNick);
                        nick = newNick;
                        server.subscribe(this);
                        LOG.info(String.format("Клиент %s подключился \n", nick));
                        socket.setSoTimeout(0);
                        sendMsg(SQLHandler.getMessageForNick(nick));
                        break;
                    } else {
                        sendMsg("С этим логином уже авторизовались");
                    }
                } else {
                    sendMsg("Неверный логин/пароль");
                }
            }

            if (str.startsWith(REGISTRATION)) {
                String[] token = str.split("\\s");
                if (token.length < 4) {
                    continue;
                }
                boolean ok = server.getAuthService().registration(token[1], token[2], token[3]);
                if (ok) {
                    sendMsg(REGISTRATION_SUCCESS);
                } else {
                    sendMsg(REGISTRATION_FAILED);
                }
            }
        }
    }

    private void communication(Server server, DataInputStream in) throws IOException {
        // цикл работы
        while (true) {
            String str = in.readUTF();

            if (str.startsWith(SPECIAL_ACTIONS)) {
                if (str.equals(ATTACH)) {
                    out.writeUTF(ATTACH);
                    break;
                }

                if (str.startsWith(PRIVATE_CHANNEL)) {
                    String[] token = str.split("\\s", 3);
                    if (token.length < 3) {
                        continue;
                    }
                    server.privateMsg(this, token[1], token[2]);
                }

                if (str.startsWith(CHANGE_NICK)) {
                    String[] token = str.split("\\s", 2);
                    if (token.length < 2) {
                        continue;
                    }
                    if (token[1].trim().isEmpty()) {
                        sendMsg("Ник не может содержать пробелов");
                        continue;
                    }
                    if (server.getAuthService().changeNick(this.nick, token[1])) {
                        sendMsg(YOUR_NICKS + token[1]);
                        sendMsg("Ваш ник изменен на " + token[1]);
                        this.nick = token[1];
                        server.broadcastClientList();
                    } else {
                        sendMsg("Не удалось изменить ник. Ник " + token[1] + " уже существует");
                    }
                }
            } else {
                server.broadcastMsg(this, str);
            }
        }
    }

    void sendMsg(String str) {
        try {
            out.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }

    public String getLogin() {
        return login;
    }
}