package org.jvan100.jqr.qr;

import org.jvan100.jqr.util.Pair;
import org.paukov.combinatorics3.Generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class QRGenerator {

    public static void main(String[] args) {
        final List<byte[][]> stages = generateQR("www.google.com", Level.M);
        System.out.println(Arrays.deepToString(stages.get(stages.size() - 1)));
        //generateQR("hello there", Level.L, true);
    }

    public static List<byte[][]> generateQR(String rawText, Level errorCorrectionLevel) {
        return generateQR(rawText, errorCorrectionLevel, false);
    }

    public static List<byte[][]> generateQR(String rawText, Level errorCorrectionLevel, boolean displayDebugInfo) {
        // Determine version to use
        final int[] LEVEL_CAPACITIES = Constants.CharacterCapacities.getCharacterCapacities(errorCorrectionLevel);

        int version = 1;

        for (int i = 0; i < 41; i++) {
            if (LEVEL_CAPACITIES[i] > rawText.length()) {
                version = i + 1;
                break;
            }
        }

        if (displayDebugInfo)
            System.out.printf("QR version: %d\n", version);

        // Generate QR message
        final String QRMessage = generateQRMessage(rawText, errorCorrectionLevel, version, displayDebugInfo);

        final int numModules = ((version - 1) * 4) + 21;

        final List<byte[][]> stages = new ArrayList<>();

        byte[][] QR = new byte[numModules][numModules];

        addPatterns(QR, version, stages);
        addData(QR, QRMessage);

        stages.add(Utils.arrayCopy(QR));

        final Pair<Integer, byte[][]> maskedQRPair = Mask.mask(QR);
        final int maskNo = maskedQRPair.getFirst();
        final byte[][] maskedQR = maskedQRPair.getSecond();

        stages.add(Utils.arrayCopy(maskedQR));

        addFormatInfo(maskedQR, errorCorrectionLevel, maskNo);

        stages.add(Utils.arrayCopy(maskedQR));

        if (version > 6) {
            addVersionInfo(maskedQR, version);
            stages.add(Utils.arrayCopy(maskedQR));
        }

        return stages;
    }

    private static void addVersionInfo(byte[][] QR, int version) {
        final byte[] versionInfo = Constants.VERSION_INFO[version - 7];

        final int shift = QR.length - 11;

        for (int i = 0; i < 18; i++) {
            int row = (17 - i) / 3;
            int col = (17 - i) % 3;

            QR[row        ][shift + col] = versionInfo[i];
            QR[shift + col][row        ] = versionInfo[i];
        }
    }

    private static void addFormatInfo(byte[][] QR, Level errorCorrectionLevel, int maskNo) {
        final byte[] formatInfo = Constants.FormatInfo.getFormatInfo(errorCorrectionLevel)[maskNo];

        for (int i = 0; i < 7; i++) {
            final int j = i != 6 ? i : 7;

            QR[8                ][j] = formatInfo[i];
            QR[QR.length - 1 - i][8] = formatInfo[i];
        }

        for (int i = 7; i < 15; i++) {
            final int j = i < 9 ? i : i + 1;

            QR[15 - j][8                 ] = formatInfo[i];
            QR[8     ][QR.length - 15 + i] = formatInfo[i];
        }
    }

    private static void addData(byte[][] QR, String QRMessage) {
        int messageIndex = 0;
        int dRow = -1;
        int dCol = -1;

        for (int col = QR.length - 1; col >= 0; col -= 2) {
            if (col == 6) {
                col = 7;
                continue;
            }

            int row = dRow == 1 ? 0 : QR.length - 1;

            while (row >= 0 && row < QR.length) {
                if (QR[row][col] == Constants.EMPTY) {
                    final char bit = QRMessage.charAt(messageIndex++);
                    QR[row][col] = bit == '1' ? Constants.DATA_DARK : Constants.DATA_LIGHT;
                }

                col += dCol;

                if (dCol == 1)
                    row += dRow;

                dCol = -dCol;
            }

            dRow = -dRow;
        }
    }

    private static void addPatterns(byte[][] QR, int version, List<byte[][]> stages) {
        int shift = QR.length - 7;

        // Add finder rings
        byte colour = Constants.PATTERN_LIGHT;

        for (int i = 0; i < 3; i++) {
            colour = (byte) (colour % 2 + 1);

            final int boundary = 6 - i;

            for (int j = i; j < boundary; j++) {
                // Top left
                QR[j       ][i       ] = colour;
                QR[i       ][j + 1   ] = colour;
                QR[j + 1   ][boundary] = colour;
                QR[boundary][j       ] = colour;

                // Top right
                QR[j       ][shift + i       ] = colour;
                QR[i       ][shift + j + 1   ] = colour;
                QR[j + 1   ][shift + boundary] = colour;
                QR[boundary][shift + j       ] = colour;

                // Bottom left
                QR[shift + j       ][i       ] = colour;
                QR[shift + i       ][j + 1   ] = colour;
                QR[shift + j + 1   ][boundary] = colour;
                QR[shift + boundary][j       ] = colour;
            }
        }

        // Add finder centres
        QR[3        ][3        ] = Constants.PATTERN_DARK;
        QR[3        ][shift + 3] = Constants.PATTERN_DARK;
        QR[shift + 3][3        ] = Constants.PATTERN_DARK;

        stages.add(Utils.arrayCopy(QR));

        shift--;

        // Add separators
        for (int i = 0; i < 8; i++) {
            // Top left
            QR[i][7] = Constants.PATTERN_LIGHT;
            QR[7][i] = Constants.PATTERN_LIGHT;

            // Top right
            QR[i][shift    ] = Constants.PATTERN_LIGHT;
            QR[7][shift + i] = Constants.PATTERN_LIGHT;

            // Bottom left
            QR[shift + i][7] = Constants.PATTERN_LIGHT;
            QR[shift    ][i] = Constants.PATTERN_LIGHT;
        }

        stages.add(Utils.arrayCopy(QR));

        shift--;

        // Add alignment patterns
        if (version > 1) {
            final List<Integer> versionCoords = Arrays.stream(Constants.ALIGNMENT_LOCATIONS[version - 2]).boxed().collect(Collectors.toList());

            final List<List<Integer>> alignmentLocations = Generator.permutation(versionCoords).withRepetitions(2).stream().collect(Collectors.toList());

            for (final List<Integer> alignmentLocation : alignmentLocations) {
                final int row = alignmentLocation.get(0);
                final int col = alignmentLocation.get(1);

                // Check if overlapping finders and separators
                // Top left   ---   Top right   ---   Bottom left
                if (!(((row - 2 < 8) && (col - 2 < 8)) || ((row - 2 < 8) && (col + 2 > shift)) || ((row + 2 > shift) && (col - 2 < 8)))) {
                    // Add alignment pattern
                    final int rowOffset = row - 2;
                    final int colOffset = col - 2;

                    for (int i = 0; i < 2; i++) {
                        final int boundary = 4 - i;

                        for (int j = i; j < boundary; j++) {
                            QR[rowOffset + j       ][colOffset + i       ] = colour;
                            QR[rowOffset + i       ][colOffset + j + 1   ] = colour;
                            QR[rowOffset + j + 1   ][colOffset + boundary] = colour;
                            QR[rowOffset + boundary][colOffset + j       ] = colour;
                        }

                        colour = (byte) (colour % 2 + 1);
                    }

                    // Add centre
                    QR[row][col] = Constants.PATTERN_DARK;
                }
            }

            stages.add(Utils.arrayCopy(QR));
        }

        // Reserve format info area
        for (int i = 0; i < 9; i++) {
            // Top left
            QR[i][8] = Constants.RESERVED;
            QR[8][i] = Constants.RESERVED;

            // Top right
            if (i > 0) {
                QR[8][shift + i] = Constants.RESERVED;

                // Bottom left
                if (i > 1)
                    QR[shift + i][8] = Constants.RESERVED;
            }
        }

        stages.add(Utils.arrayCopy(QR));

        // Add timing patterns
        for (int i = 8; i <= shift; i++) {
            // Top
            QR[6][i] = colour;

            // Left
            QR[i][6] = colour;

            colour = (byte) (colour % 2 + 1);
        }

        stages.add(Utils.arrayCopy(QR));

        // Add dark module
        QR[shift + 1][8] = Constants.PATTERN_DARK;

        stages.add(Utils.arrayCopy(QR));

        // Reserve version info area
        if (version > 6) {
            for (int i = shift; i > shift - 3; i--) {
                for (int j = 0; j < 6; j++) {
                    // Top right
                    QR[j][i] = Constants.RESERVED;

                    // Bottom left
                    QR[i][j] = Constants.RESERVED;
                }
            }

            stages.add(Utils.arrayCopy(QR));
        }
    }

    public static void printArr(byte[][] arr) {
        for (byte[] row : arr)
            System.out.println(Arrays.toString(row));

        System.out.println();
    }

    public static String generateQRMessage(String rawText, Level errorCorrectionLevel, int version) {
        return generateQRMessage(rawText, errorCorrectionLevel, version, false);
    }

    public static String generateQRMessage(String rawText, Level errorCorrectionLevel, int version, boolean displayDebugInfo) {
        if (displayDebugInfo)
            System.out.println("Debug information\n-----------------");

        // Initialise bit stream
        final StringBuilder bitStream = new StringBuilder(Constants.MODE_INDICATOR);

        int padding = version <= 9 ? 8 : 16;
        bitStream.append(Utils.toPaddedBinaryString(rawText.length(), padding));

        // Add raw character bytes
        for (final char c : rawText.toCharArray())
            bitStream.append(Utils.to8BitBinaryString(c));

        // Get codewords data
        final Constants.Data data = Constants.CodewordsData.getCodewordsData(errorCorrectionLevel)[version - 1];

        // Add terminator
        int maxCapacity = 8 * data.getTotal();
        int terminator = maxCapacity - bitStream.length();
        bitStream.append("0".repeat(Math.min(terminator, 4)));

        if (displayDebugInfo)
            System.out.printf("Input string as bytes:\n%s\n", Utils.toByteString(bitStream.toString()));

        // Ensure length is a multiple of 8
        final int remainder = bitStream.length() % 8;

        if (remainder != 0)
            bitStream.append("0".repeat(8 - remainder));

        // Add pad bytes if too short
        int k = 0;
        while (bitStream.length() < maxCapacity) {
            bitStream.append(Constants.PAD_BYTES[k]);
            k = (k + 1) % 2;
        }

        //Break into data codewords
        String bitString = bitStream.toString();

        final int totalBlocks = data.getGroup1Blocks() + data.getGroup2Blocks();

        String[][] dataCodewords = new String[totalBlocks][];

        int bitStringIndex = 0;

        for (int i = 0; i < data.getGroup1Blocks(); i++) {
            String[] block = new String[data.getGroup1CodewordsPerBlock()];

            for (int j = 0; j < block.length; j++) {
                block[j] = bitString.substring(bitStringIndex, bitStringIndex + 8);
                bitStringIndex += 8;
            }

            dataCodewords[i] = block;
        }

        for (int i = data.getGroup1Blocks(); i < totalBlocks; i++) {
            String[] block = new String[data.getGroup2CodewordsPerBlock()];

            for (int j = 0; j < block.length; j++) {
                block[j] = bitString.substring(bitStringIndex, bitStringIndex + 8);
                bitStringIndex += 8;
            }

            dataCodewords[i] = block;
        }

        if (displayDebugInfo)
            System.out.printf("Data codewords (%d*%d + %d*%d = %d total):\n%s\n", data.getGroup1CodewordsPerBlock(), data.getGroup1Blocks(), data.getGroup2CodewordsPerBlock(), data.getGroup2Blocks(), data.getTotal(), Arrays.deepToString(dataCodewords));

        // Get generator polynomial
        final int[] generatorPolynomial = getGeneratorPolynomial(data.getECCodewords());

        // Create error codewords
        String[][] errorCodewords = new String[dataCodewords.length][];

        for (int i = 0; i < dataCodewords.length; i++) {
            final String[] block = dataCodewords[i];

            int[] messagePolynomial = new int[block.length];

            for (int j = 0; j < block.length; j++)
                messagePolynomial[j] = Integer.parseInt(block[j], 2);

            errorCodewords[i] = getErrorCodewords(messagePolynomial, generatorPolynomial, data.getECCodewords());
        }

        if (displayDebugInfo)
            System.out.printf("Error correction codewords (%d*%d = %d total):\n%s\n", data.getECCodewords(), totalBlocks, data.getECCodewords() * totalBlocks, Arrays.deepToString(errorCodewords));

        // Structure final message
        final StringBuilder finalMessage = new StringBuilder();

        for (int i = 0; i < dataCodewords[0].length + 1; i++) {
            for (final String[] block : dataCodewords) {
                try {
                    finalMessage.append(block[i]);
                } catch (ArrayIndexOutOfBoundsException ignored) {}
            }
        }

        for (int i = 0; i < errorCodewords[0].length; i++) {
            for (final String[] block : errorCodewords)
                finalMessage.append(block[i]);
        }

        finalMessage.append("0".repeat(Constants.REMAINDER_BITS[version - 1]));

        if (displayDebugInfo)
            System.out.printf("Final message as bytes:\n%s", Utils.toByteString(finalMessage.toString()));

        return finalMessage.toString();
    }

    private static String[] getErrorCodewords(int[] messagePolynomial, int[] generatorPolynomial, int ECCodewords) {
        int[] newMessagePolynomial = new int[messagePolynomial.length + ECCodewords];
        System.arraycopy(messagePolynomial, 0, newMessagePolynomial, 0, messagePolynomial.length);

        int i;

        for (i = 0; i < messagePolynomial.length; i++) {
            final int lead_term = Constants.ANTILOG[newMessagePolynomial[i]];

            for (int j = 0; j < generatorPolynomial.length; j++)
                newMessagePolynomial[i + j] ^= Constants.LOG[(generatorPolynomial[j] + lead_term) % 255];
        }

        String[] errorCodewords = new String[ECCodewords];

        for (int j = 0; j < ECCodewords; j++)
            errorCodewords[j] = Utils.to8BitBinaryString(newMessagePolynomial[i + j]);

        return errorCodewords;
    }

    private static int[] getGeneratorPolynomial(int ECCodewords) {
        int[] generatorPolynomial = {0, 0};

        for (int i = 1; i < ECCodewords; i++) {
            int[] toMultiply = {0, i};
            int[] nextGeneratorPolynomial = new int[i + 2];

            int j;

            for (j = 0; j < generatorPolynomial.length; j++) {
                for (int k = 0; k < 2; k++) {
                    final int temp = (generatorPolynomial[j] + toMultiply[k]) % 255;
                    nextGeneratorPolynomial[j + k] ^= Constants.LOG[temp];
                }

                nextGeneratorPolynomial[j] = Constants.ANTILOG[nextGeneratorPolynomial[j]] % 255;
            }

            nextGeneratorPolynomial[j] = Constants.ANTILOG[nextGeneratorPolynomial[j]] % 255;

            generatorPolynomial = nextGeneratorPolynomial;
        }

        return generatorPolynomial;
    }

}
