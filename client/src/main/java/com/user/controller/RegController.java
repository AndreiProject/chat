package com.user.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegController {
    @FXML
    public Label errorReg;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public TextField nicknameField;

    private MainController mainController;
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void tryToReg() {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String nickname = nicknameField.getText().trim();
        mainController.tryToReg(login, password, nickname);
    }

    public void clickCancelBtn() {
        Platform.runLater(() -> ((Stage) loginField.getScene().getWindow()).close());
    }

    public void addErrorMessage(String msg) {
        Platform.runLater(() -> {
            errorReg.setVisible(true);
            errorReg.setText(msg);
        });
    }
}