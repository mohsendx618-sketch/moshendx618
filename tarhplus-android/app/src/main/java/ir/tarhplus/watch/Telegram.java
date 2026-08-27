package ir.tarhplus.watch;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;

final class Telegram {
    static boolean validToken(String value) { return value.matches("[0-9]{5,}:[A-Za-z0-9_-]{20,}"); }
    static boolean validChat(String value) { return value.matches("-?[0-9]+|@[A-Za-z0-9_]{5,}"); }

    static void send(String token, String chat, String text) throws Exception {
        if (!validToken(token) || !validChat(chat)) throw new IOException("Invalid Telegram settings");
        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://api.telegram.org/bot" + token + "/sendMessage").openConnection();
        try {
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(15_000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            byte[] body = new JSONObject().put("chat_id", chat).put("text", text).put("protect_content", true)
                    .toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) { out.write(body); }
            if (connection.getResponseCode() != 200) throw new IOException("Telegram delivery failed");
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    if (out.size() + n > 64 * 1024) throw new IOException("Invalid Telegram response");
                    out.write(buffer, 0, n);
                }
                if (!new JSONObject(out.toString("UTF-8")).optBoolean("ok")) throw new IOException("Telegram delivery failed");
            }
        } finally { connection.disconnect(); }
    }
}
