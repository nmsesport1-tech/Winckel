package com.anglehud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class ChatHistory {
    private static final int MAX = 40;
    private static final ArrayDeque<String> MESSAGES = new ArrayDeque<>();

    private ChatHistory() {}

    public static void add(String text) {
        if (text == null) return;
        String t = text.trim();
        if (t.isEmpty() || t.startsWith("[Translation]")) return;
        MESSAGES.addLast(t);
        while (MESSAGES.size() > MAX) MESSAGES.removeFirst();
    }

    /** Die letzten n Nachrichten, älteste zuerst. */
    public static List<String> last(int n) {
        List<String> all = new ArrayList<>(MESSAGES);
        int from = Math.max(0, all.size() - n);
        return new ArrayList<>(all.subList(from, all.size()));
    }
}
