package com.anglehud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** Übersetzt über die freie MyMemory-API (Deutsch <-> Englisch). */
public final class Translator {
    private static final int MAX_CHARS = 450;

    private Translator() {}

    public static void translate(String text, String from, String to, Consumer<String> onResult, Consumer<String> onError) {
        String input = text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
        Thread t = new Thread(() -> {
            try {
                String url = "https://api.mymemory.translated.net/get?q="
                        + URLEncoder.encode(input, StandardCharsets.UTF_8)
                        + "&langpair=" + from + "%7C" + to;
                HttpURLConnection con = (HttpURLConnection) URI.create(url).toURL().openConnection();
                con.setRequestProperty("User-Agent", "AngleHud/2.0");
                con.setConnectTimeout(6000);
                con.setReadTimeout(10000);
                String body;
                try (var in = con.getInputStream()) {
                    body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                String result = root.getAsJsonObject("responseData").get("translatedText").getAsString();
                String clean = unescape(result);
                Minecraft.getInstance().execute(() -> onResult.accept(clean));
            } catch (Exception e) {
                String msg = e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : "");
                Minecraft.getInstance().execute(() -> onError.accept(msg));
            }
        }, "anglehud-translate");
        t.setDaemon(true);
        t.start();
    }

    private static String unescape(String s) {
        return s.replace("&#39;", "'")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }
}
