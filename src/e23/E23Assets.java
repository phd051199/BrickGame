package e23;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class E23Assets {
    private static final E23Assets INSTANCE = new E23Assets();

    private E23Assets() {
    }

    static byte[] read(String path) throws IOException {
        InputStream input = INSTANCE.getClass().getResourceAsStream(path);
        if (input == null) {
            StringBuffer message = new StringBuffer("Missing resource: ");
            message.append(path);
            throw new IOException(message.toString());
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) {
                    output.write(buffer, 0, count);
                }
            }
            return output.toByteArray();
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {
            }
        }
    }
}
