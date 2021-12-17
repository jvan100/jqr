package org.jvan100.jqr.qr;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class QR extends Application {

    int stageIndex = 0;
    int noStages;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        final int QR_WIDTH = 500;

        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter text:");
        
        final String rawText = scanner.nextLine();
        //final byte[][] QR = QRGenerator.generateQR(rawText, Level.Q);
        final List<byte[][]> stages = QRGenerator.generateQR(rawText, Level.M, true);
        noStages = stages.size();

        final int modules = stages.get(0).length;
        final int moduleWidth = QR_WIDTH / modules;

        final List<Canvas> canvases = new ArrayList<>(noStages);

        for (final byte[][] QR : stages) {
            final Canvas canvas = new Canvas(QR_WIDTH, QR_WIDTH);
            final GraphicsContext graphicsContext = canvas.getGraphicsContext2D();

            for (int row = 0; row < modules; row++) {
                for (int col = 0; col < modules; col++) {
                    final byte val = QR[row][col];

                    final Color color;

                    switch (val) {
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
                            color = Color.GRAY;
                    }

                    graphicsContext.setFill(color);
                    graphicsContext.fillRect(col * moduleWidth, row * moduleWidth, moduleWidth, moduleWidth);
                }
            }

            canvas.setVisible(false);
            canvases.add(canvas);
        }

        canvases.get(0).setVisible(true);

        final Group root = new Group();
        root.getChildren().addAll(canvases);

        final Scene scene = new Scene(root, QR_WIDTH, QR_WIDTH);

        scene.setOnKeyPressed(mouseEvent -> {
            if (stageIndex != noStages - 1) {
                canvases.get(stageIndex++).setVisible(false);
                canvases.get(stageIndex).setVisible(true);
            }
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("QR Generator");
        primaryStage.show();
    }
}
