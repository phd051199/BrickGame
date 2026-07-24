package brickgame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.lcdui.Image;

/** Loads the original desktop bitmap assets packed as PNG/base64 resources. */
final class BitmapAssets {

    final Image[] digits = new Image[10];
    Image soundOn;
    Image soundOff;
    Image pauseOn;
    Image pauseOff;

    BitmapAssets() {
        InputStream input = getClass().getResourceAsStream("/ui/assets.b64");
        if (input == null) {
            throw new IllegalStateException("Missing /ui/assets.b64");
        }
        try {
            String header = readLine(input);
            if (!"BRICKGAME-ASSETS-1".equals(header)) {
                throw new IllegalStateException("Invalid bitmap asset header");
            }
            String line;
            while ((line = readLine(input)) != null) {
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String name = line.substring(0, separator);
                Image image = decodeImage(line.substring(separator + 1));
                assign(name, image);
            }
        } catch (IOException error) {
            throw new IllegalStateException(error.toString());
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void assign(String name, Image image) {
        if (name.length() == 6 && name.startsWith("digit")) {
            int index = name.charAt(5) - '0';
            if (index >= 0 && index < digits.length) {
                digits[index] = image;
            }
        } else if ("sound_on".equals(name)) {
            soundOn = image;
        } else if ("sound_off".equals(name)) {
            soundOff = image;
        } else if ("pause_on".equals(name)) {
            pauseOn = image;
        } else if ("pause_off".equals(name)) {
            pauseOff = image;
        }
    }

    private static Image decodeImage(String encoded) {
        byte[] bytes = decodeBase64(encoded);
        return Image.createImage(bytes, 0, bytes.length);
    }

    private static byte[] decodeBase64(String value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(value.length() * 3 / 4);
        int accumulator = 0;
        int bits = 0;
        int i;
        for (i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '=') {
                break;
            }
            int digit = base64Digit(c);
            if (digit < 0) {
                continue;
            }
            accumulator = (accumulator << 6) | digit;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                output.write((accumulator >> bits) & 0xFF);
            }
        }
        return output.toByteArray();
    }

    private static int base64Digit(char c) {
        if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        }
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 26;
        }
        if (c >= '0' && c <= '9') {
            return c - '0' + 52;
        }
        if (c == '+') {
            return 62;
        }
        if (c == '/') {
            return 63;
        }
        return -1;
    }

    private static String readLine(InputStream input) throws IOException {
        StringBuffer buffer = new StringBuffer();
        int value;
        boolean found = false;
        while ((value = input.read()) != -1) {
            found = true;
            if (value == '\n') {
                break;
            }
            if (value != '\r') {
                buffer.append((char) value);
            }
        }
        if (!found && buffer.length() == 0) {
            return null;
        }
        return buffer.toString();
    }
}
