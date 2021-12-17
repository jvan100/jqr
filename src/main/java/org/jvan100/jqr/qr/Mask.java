package org.jvan100.jqr.qr;

import java.util.function.BiPredicate;

import org.jvan100.jqr.util.Pair;

abstract class Mask {

    @SuppressWarnings("unchecked")
    private static BiPredicate<Integer, Integer>[] masks = new BiPredicate[8];

    static {
        masks[0] = ((row, col) -> (row + col) % 2 == 0);
        masks[1] = ((row, col) -> row % 2 == 0);
        masks[2] = ((row, col) -> col % 3 == 0);
        masks[3] = ((row, col) -> (row + col) % 3 == 0);
        masks[4] = ((row, col) -> ((row / 2) + (col / 3)) % 2 == 0);
        masks[5] = ((row, col) -> (((row * col) % 2) + ((row * col) % 3)) == 0);
        masks[6] = ((row, col) -> (((row * col) % 2) + ((row * col) % 3)) % 2 == 0);
        masks[7] = ((row, col) -> (((row + col) % 2) + ((row * col) % 3)) % 2 == 0);
    }

    static Pair<Integer, byte[][]> mask(byte[][] QR) {
        int bestMaskNo = 0;
        double bestPenalty = Double.POSITIVE_INFINITY;
        byte[][] bestQR = null;

        for (int i = 0; i < 8; i++) {
            final byte[][] newQR = applyMask(QR, i);
            final int penalty = evaluateMask(newQR);

            if (penalty < bestPenalty) {
                bestMaskNo = i;
                bestPenalty = penalty;
                bestQR = newQR;
            }
        }

        return new Pair<>(bestMaskNo, bestQR);
    }

    private static byte[][] applyMask(byte[][] QR, int maskNo) {
        byte[][] newQR = new byte[QR.length][QR.length];

        for (int row = 0; row < QR.length; row++) {
            for (int col = 0; col < QR.length; col++) {
                final byte val = QR[row][col];

                // Should swap bit
                if (val >= Constants.DATA_DARK && masks[maskNo].test(row, col)) {
                    newQR[row][col] = val == Constants.DATA_DARK ? Constants.DATA_LIGHT : Constants.DATA_DARK;
                } else {
                    newQR[row][col] = toCorrectColour(val);
                }
            }
        }

        return newQR;
    }

    private static int evaluateMask(byte[][] QR) {
        int totalPenalty = 0;

        // Condition 1
        for (int i = 0; i < QR.length; i++) {
            byte rowVal = Constants.EMPTY;
            byte colVal = Constants.EMPTY;
            int rowCount = 0;
            int colCount = 0;

            for (int j = 0; j < QR.length; j++) {
                final byte currentRowVal = QR[i][j];
                final byte currentColVal = QR[j][i];

                // Row
                if (rowVal != currentRowVal) {
                    if (rowCount >= 5)
                        totalPenalty += rowCount - 2;

                    if (currentRowVal != Constants.RESERVED) {
                        rowVal = currentRowVal;
                        rowCount = 1;
                    } else {
                        rowCount = 0;
                    }
                } else {
                    rowCount++;
                }

                // Column
                if (colVal != currentColVal) {
                    if (colCount >= 5)
                        totalPenalty += colCount - 2;

                    if (currentColVal != Constants.RESERVED) {
                        colVal = currentColVal;
                        colCount = 1;
                    } else {
                        colCount = 0;
                    }
                } else {
                    colCount++;
                }
            }

            if (rowCount >= 5)
                totalPenalty += rowCount - 2;

            if (colCount >= 5)
                totalPenalty += colCount - 2;
        }

        // Condition 2
        for (int row = 0; row < QR.length - 1; row++) {
            for (int col = 0; col < QR.length - 1; col++) {
                final byte currentVal = QR[row][col];

                if ((currentVal == QR[row][col + 1]) && (currentVal == QR[row + 1][col]) && currentVal == QR[row + 1][col + 1])
                    totalPenalty += 3;
            }
        }

        // Condition 3
        final byte[] pattern1 = {Constants.DATA_DARK, Constants.DATA_LIGHT, Constants.DATA_DARK, Constants.DATA_DARK, Constants.DATA_DARK, Constants.DATA_LIGHT, Constants.DATA_DARK, Constants.DATA_LIGHT, Constants.DATA_LIGHT, Constants.DATA_LIGHT, Constants.DATA_LIGHT};
        final byte[] pattern2 = {Constants.DATA_LIGHT, Constants.DATA_LIGHT, Constants.DATA_LIGHT, Constants.DATA_LIGHT, Constants.DATA_DARK, Constants.DATA_LIGHT, Constants.DATA_DARK, Constants.DATA_DARK, Constants.DATA_DARK, Constants.DATA_LIGHT, Constants.DATA_DARK};

        for (int i = 0; i < QR.length; i++) {
            for (int j = 0; j < QR.length - 10; j++) {
                boolean rowMatch1 = true;
                boolean rowMatch2 = true;
                boolean colMatch1 = true;
                boolean colMatch2 = true;

                for (int k = 0; k < 11; k++) {
                    if (QR[i][j + k] != pattern1[k]) {
                        rowMatch1 = false;
                        break;
                    }
                }

                if (rowMatch1) {
                    totalPenalty += 40;
                } else {
                    for (int k = 0; k < 11; k++) {
                        if (QR[i][j + k] != pattern2[k]) {
                            rowMatch2 = false;
                            break;
                        }
                    }

                    if (rowMatch2)
                        totalPenalty += 40;
                }

                for (int k = 0; k < 11; k++) {
                    if (QR[j + k][i] != pattern1[k]) {
                        colMatch1 = false;
                        break;
                    }
                }

                if (colMatch1) {
                    totalPenalty += 40;
                } else {
                    for (int k = 0; k < 11; k++) {
                        if (QR[j + k][i] != pattern2[k]) {
                            colMatch2 = false;
                            break;
                        }
                    }

                    if (colMatch2)
                        totalPenalty += 40;
                }
            }
        }

        // Condition 4
        int darkTotal = 0;

        for (final byte[] row : QR) {
            for (final byte val : row) {
                if (val == Constants.DATA_DARK)
                    darkTotal++;
            }
        }

        final int percentage = 100 * darkTotal / (QR.length * QR.length);

        totalPenalty += 10 * Math.min(Math.abs(5 * (percentage / 5) - 50) / 5, Math.abs(5 * (percentage / 5 + 1) - 50) / 5);

        return totalPenalty;
    }

    private static byte toCorrectColour(byte val) {
        if (val == Constants.RESERVED)
            return val;

        if (val == Constants.PATTERN_DARK || val == Constants.DATA_DARK)
            return Constants.DATA_DARK;

        return Constants.DATA_LIGHT;
    }

}
