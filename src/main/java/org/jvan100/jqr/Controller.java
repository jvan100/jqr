package org.jvan100.jqr;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.jvan100.jqr.qr.Constants;
import org.jvan100.jqr.qr.Level;
import org.jvan100.jqr.qr.QRGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Controller {

    @FXML
    private TextArea inputString;

    @FXML
    private ChoiceBox<String> ecDropdown;

    @FXML
    private Button generateBtn;

    @FXML
    private Canvas canvas;

    @FXML
    private AnchorPane previewCover;

    private List<byte[][]> qrStages;
    private Timeline timeline;
    private AtomicInteger timelineIndex;

    @FXML
    public void initialize() {
        this.qrStages = new ArrayList<>();
        this.timeline = new Timeline();
        this.timelineIndex = new AtomicInteger(0);

        ecDropdown.getItems().addAll("L", "M", "Q", "H");
        ecDropdown.setValue("M");
    }

    @FXML
    private void inputStringUpdate() {
        if (inputString.getText().isEmpty()) generateBtn.setDisable(true);
        else generateBtn.setDisable(false);
    }

    @FXML
    private void generateClicked() {
        Level level;

        switch (ecDropdown.getValue()) {
            case "L":
                level = Level.L;
                break;
            case "Q":
                level = Level.Q;
                break;
            case "H":
                level = Level.H;
                break;
            default:
                level = Level.M;
        }

        qrStages.clear();
        qrStages = QRGenerator.generateQR(inputString.getText(), level);

        final int numModules = qrStages.get(0).length;
        final int qrWidth = (int) canvas.getWidth();
        final int moduleWidth = qrWidth / numModules;

        final int buffer = (qrWidth - (moduleWidth * numModules)) / 2;

        final GraphicsContext graphicsContext = canvas.getGraphicsContext2D();

        timeline.stop();
        timeline.getKeyFrames().clear();
        timelineIndex.set(0);

        graphicsContext.clearRect(0, 0, qrWidth, qrWidth);

        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(300), actionEvent -> {
            final byte[][] qr = qrStages.get(timelineIndex.getAndIncrement());

            for (int row = 0; row < numModules; row++) {
                for (int col = 0; col < numModules; col++) {
                    final Color color;

                    switch (qr[row][col]) {
                        case Constants.PATTERN_DARK:
                            color = Color.RED;
                            break;
                        case Constants.PATTERN_LIGHT:
                            color = Color.ORANGE;
                            break;
                        case Constants.DATA_DARK:
                            color = Color.BLACK;
                            break;
                        case Constants.DATA_LIGHT:
                            color = Color.WHITE;
                            break;
                        case Constants.RESERVED:
                            color = Color.BLUE;
                            break;
                        default:
                            color = Color.valueOf("#444");
                    }

                    graphicsContext.setFill(color);
                    graphicsContext.fillRect(buffer + col * moduleWidth, buffer + row * moduleWidth, moduleWidth, moduleWidth);
                }
            }
        }));

        previewCover.setVisible(false);
        timeline.setCycleCount(qrStages.size());
        timeline.play();
    }

}
