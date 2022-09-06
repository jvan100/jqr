package org.jvan100.jqr.qr;

import java.util.Arrays;

abstract class Utils {

    static String to8BitBinaryString(int base10Int) {
        return toPaddedBinaryString(base10Int, 8);
    }

    static String toPaddedBinaryString(int base10Int, int padding) {
        return String.format("%" + padding + "s", Integer.toBinaryString(base10Int)).replaceAll(" ", "0");
    }


    static String toByteString(String s) {
        final StringBuilder toReturn = new StringBuilder();

        int quotient = s.length() / 8;
        int remainder = s.length() % 8;

        int i;

        for (i = 0; i < quotient * 8; i += 8)
            toReturn.append(s, i, i + 8).append(" ");

        toReturn.append(s, i, i + remainder);

        return toReturn.toString();
    }

    static byte[][] arrayCopy(byte[][] arr) {
        return Arrays.stream(arr).map(byte[]::clone).toArray(byte[][]::new);
    }

}
