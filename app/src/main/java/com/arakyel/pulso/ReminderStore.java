package com.arakyel.pulso;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Guarda los recordatorios programados para volver a programarlos al reiniciar el teléfono. */
final class ReminderStore {

    private ReminderStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("kaizen_reminders", Context.MODE_PRIVATE);
    }

    static void put(Context c, int code, long when, String title, String text) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", when);
            o.put("ti", title == null ? "" : title);
            o.put("tx", text == null ? "" : text);
            prefs(c).edit().putString(String.valueOf(code), o.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    static void remove(Context c, int code) {
        prefs(c).edit().remove(String.valueOf(code)).apply();
    }

    static void rescheduleAll(Context c) {
        long now = System.currentTimeMillis();
        List<String> old = new ArrayList<>();
        for (Map.Entry<String, ?> e : prefs(c).getAll().entrySet()) {
            try {
                JSONObject o = new JSONObject(String.valueOf(e.getValue()));
                long t = o.getLong("t");
                int code = Integer.parseInt(e.getKey());
                if (t > now) ReminderReceiver.schedule(c, code, t, o.optString("ti"), o.optString("tx"));
                else old.add(e.getKey());
            } catch (Exception ex) {
                old.add(e.getKey());
            }
        }
        if (!old.isEmpty()) {
            SharedPreferences.Editor ed = prefs(c).edit();
            for (String k : old) ed.remove(k);
            ed.apply();
        }
    }
}
