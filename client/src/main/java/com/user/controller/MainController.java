package com.user.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class MainController implements Initializable {
    @FXML
    public TextArea textArea;
    @FXML
    public TextField textField;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public HBox authPanel;
    @FXML
    public HBox msgPanel;
    @FXML
    public ListView<String> clientList;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String login;
    private String nick;
    private final String CHAT_TITLE_EMPTY = "Chat july 2020";

    private Stage stage;
    private RegController regController;

    // константы клиент/серверного взаимодействия (контракт)
    private static final String ATTACH = "/end";
    private static final String AUTH_EVENT = "/auth ";
    private static final String AUTHORIZATION_SUCCESS = "/authok ";
    private static final String REG_EVENT = "/reg ";
    private static final String REG_RESULT = "/regresult ";
    private static final String SUCCESS = "ok";
    private static final String GET_CLIENT_LIST = "/clientlist ";
    private static final String GET_MY_NICKS = "/yournickis ";
    private static final String SPECIAL_ACTIONS = "/";

    private final static Logger LOG = Logger.getLogger(MainController.class.getSimpleName());

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textField.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                LOG.warning("bye");
                if (socket != null && out != null && !socket.isClosed()) {
                    try {
                        out.writeUTF(ATTACH);
                        close(socket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        setAuthenticated(false);
    }

    private void setAuthenticated(boolean auth) {
        authPanel.setVisible(!auth);
        authPanel.setManaged(!auth);
        msgPanel.setVisible(auth);
        msgPanel.setManaged(auth);
        clientList.setVisible(auth);
        clientList.setManaged(auth);
        if (!auth) {
            nick = "";
        }
        setTitle(nick);
        textArea.clear();
    }

    private void connection() throws Exception {
        // подключаемся к серверу
        String serverIp = "localhost";
        int serverPort = 8189;
        socket = new Socket(serverIp, serverPort);
        initSocketStream(socket);
    }

    private void initSocketStream(Socket socket) {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    authentication(in);
                    communication(in);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    streamClose();
                }
            }).start();
        } catch (Exception ex) {
            ex.printStackTrace();
            streamClose();
        }
    }

    private void streamClose() {
        close(in);
        close(out);
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

    private void authentication(DataInputStream in) throws IOException {
        // цикл аутентификации
        while (true) {
            String str = in.readUTF();

            if (str.equals(ATTACH)) {
                throw new RuntimeException("Сервер нас отключил по таймауту");
            }
            if (str.startsWith(AUTHORIZATION_SUCCESS)) {
                nick = str.split("\\s")[1];
                setAuthenticated(true);
                break;
            }
            if (str.startsWith(REG_RESULT)) {
                String result = str.split("\\s")[1];
                if (result.equals(SUCCESS)) {
                    regController.clickCancelBtn();
                } else {
                    regController.addErrorMessage("Регистрация не получилась: - возможно логин и никнейм заняты");
                }
            }
            textArea.appendText(str + "\n");
        }
    }

    private void communication(DataInputStream in) throws IOException {
        // цикл работы
        while (true) {
            String str = in.readUTF();
            if (str.startsWith(SPECIAL_ACTIONS)) {
                if (str.equals(ATTACH)) {
                    setAuthenticated(false);
                    break;
                }
                if (str.startsWith(GET_CLIENT_LIST)) {
                    String[] token = str.split("\\s");
                    Platform.runLater(() -> {
                        clientList.getItems().clear();
                        for (int i = 1; i < token.length; i++) {
                            clientList.getItems().add(token[i]);
                        }
                    });
                }
                if (str.startsWith(GET_MY_NICKS)) {
                    nick = str.split("\\s")[1];
                    setTitle(nick);
                }
            } else {
                appendText(str);
            }
        }
    }

    private void setTitle(String nick) {
        Platform.runLater(() -> stage.setTitle(CHAT_TITLE_EMPTY + " : " + nick));
    }

    private void appendText(String str) {
        Platform.runLater(() -> textArea.appendText(str + "\n"));
    }

    public void sendMsg() {
        try {
            out.writeUTF(textField.getText());
            textField.requestFocus();
            textField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clickClientList(MouseEvent mouseEvent) {
        LOG.warning(clientList.getSelectionModel().getSelectedItem());
        String receiver = clientList.getSelectionModel().getSelectedItem();
        textField.setText(String.format("/w %s ", receiver));
        if (mouseEvent.isAltDown()) {
            LOG.warning("AltDown");
        }
    }

    public void showRegWindow() {
        Stage regStage = createRegWindow();
        regStage.show();
    }

    private Stage createRegWindow() {
        Stage stage = new Stage();
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();

            stage.setScene(new Scene(root));
            stage.initStyle(StageStyle.UTILITY);
            stage.initModality(Modality.APPLICATION_MODAL);

            regController = fxmlLoader.getController();
            regController.setMainController(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stage;
    }

    public void tryToAuth() {
        Platform.runLater(() -> {
            try {
                if (socket == null || socket.isClosed()) {
                    connection();
                }
                login = loginField.getText().trim();
                String password = passwordField.getText().trim();
                out.writeUTF(String.format(AUTH_EVENT + "%s %s", login, password));
                passwordField.clear();
            } catch (Exception e) {
                e.printStackTrace();
                close(socket);
            }
        });
    }

    public void tryToReg(String login, String password, String nickname) {
        Platform.runLater(() -> {
            try {
                if (socket == null || socket.isClosed()) {
                    connection();
                }
                out.writeUTF(String.format(REG_EVENT + "%s %s %s", login, password, nickname));
            } catch (Exception e) {
                e.printStackTrace();
                regController.addErrorMessage("Не удалось подключиться к серверу");
                close(socket);
            }
        });
    }
}