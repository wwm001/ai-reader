package com.example.chatgptreader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public final class DiagnosticStore {
    private static final int MAX_EVENTS = 80;
    private static final int MAX_ERRORS = 20;
    private static final int MAX_SNIPPETS = 20;
    private static final DiagnosticStore INSTANCE = new DiagnosticStore();

    private final ArrayDeque<JSONObject> events = new ArrayDeque<>();
    private final ArrayDeque<String> errors = new ArrayDeque<>();
    private final ArrayDeque<String> snippets = new ArrayDeque<>();
    private final Map<String, Integer> eventTypeCounts = new LinkedHashMap<>();

    private DiagnosticStore() {
    }

    public static DiagnosticStore get() {
        return INSTANCE;
    }

    public synchronized void event(String type, String detail) {
        eventTypeCounts.put(type, eventTypeCounts.containsKey(type) ? eventTypeCounts.get(type) + 1 : 1);
        JSONObject event = new JSONObject();
        try {
            event.put("at", isoNow());
            event.put("type", type);
            event.put("detail", detail == null ? "" : detail);
        } catch (JSONException ignored) {
            return;
        }
        events.addLast(event);
        while (events.size() > MAX_EVENTS) {
            events.removeFirst();
        }
    }

    public synchronized void error(Throwable throwable) {
        String message = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
        errors.addLast(message);
        while (errors.size() > MAX_ERRORS) {
            errors.removeFirst();
        }
        event("error", message);
    }

    public synchronized void candidateSnippet(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        snippets.addLast(limitSnippet(text));
        while (snippets.size() > MAX_SNIPPETS) {
            snippets.removeFirst();
        }
    }

    public synchronized void clear() {
        events.clear();
        errors.clear();
        snippets.clear();
        eventTypeCounts.clear();
    }

    public synchronized JSONObject eventTypeCountsJson() throws JSONException {
        JSONObject object = new JSONObject();
        for (Map.Entry<String, Integer> entry : eventTypeCounts.entrySet()) {
            object.put(entry.getKey(), entry.getValue());
        }
        return object;
    }

    public synchronized JSONArray recentEventsJson() {
        return new JSONArray(events);
    }

    public synchronized JSONArray recentErrorsJson() {
        return new JSONArray(errors);
    }

    public synchronized JSONArray candidateSnippetsJson() {
        return new JSONArray(snippets);
    }

    public static String isoNow() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    static String limitSnippet(String text) {
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() > 80 ? compact.substring(0, 80) : compact;
    }
}
