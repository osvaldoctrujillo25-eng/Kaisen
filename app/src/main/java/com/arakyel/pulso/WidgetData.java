package com.arakyel.pulso;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Datos que muestran los widgets de agua y comida.
 * La app (index.html) envía el resumen del día; lo que se registra desde un widget
 * se guarda como "pendiente" y la app lo incorpora la próxima vez que se abre.
 */
final class WidgetData {

    private WidgetData() {}

    static final String P = "kaizen_widget";

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    static String nowTime() {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
    }

    /** desayuno antes de las 12:00, almuerzo hasta las 18:00 y cena después (igual que la app). */
    static String slotNow() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return h < 12 ? "d" : h < 18 ? "a" : "c";
    }

    /** Valores del día de hoy: si lo guardado es de otro día, agua y comidas empiezan en cero. */
    static int water(Context c) {
        SharedPreferences p = prefs(c);
        return today().equals(p.getString("date", "")) ? p.getInt("water", 0) : 0;
    }

    static int meals(Context c, String slot) {
        SharedPreferences p = prefs(c);
        return today().equals(p.getString("date", "")) ? p.getInt("m_" + slot, 0) : 0;
    }

    static int goal(Context c) {
        return Math.max(1, prefs(c).getInt("goal", 3000));
    }

    static String unit(Context c) {
        return prefs(c).getString("unit", "L");
    }

    static int quick(Context c, int i) {
        return prefs(c).getInt("q" + i, i == 1 ? 250 : 500);
    }

    static String fmtWater(Context c, int ml) {
        if ("ml".equals(unit(c))) {
            return NumberFormat.getIntegerInstance(Locale.US).format(ml) + " ml";
        }
        double l = ml / 1000.0;
        return String.format(Locale.US, ml % 100 == 0 ? "%.1f L" : "%.2f L", l);
    }

    /** Resumen enviado por la app. Suma lo pendiente de hoy que la app aún no conoce. */
    static void setFromApp(Context c, String json) {
        try {
            JSONObject o = new JSONObject(json);
            String date = o.optString("date", today());
            int water = o.optInt("water", 0);
            int md = o.optInt("d", 0), ma = o.optInt("a", 0), mc = o.optInt("c", 0);
            JSONArray pend = new JSONArray(prefs(c).getString("pending", "[]"));
            for (int i = 0; i < pend.length(); i++) {
                JSONObject e = pend.getJSONObject(i);
                if (!date.equals(e.optString("date"))) continue;
                if ("w".equals(e.optString("t"))) water += e.optInt("ml", 0);
                else if ("m".equals(e.optString("t"))) {
                    String s = e.optString("slot", "d");
                    if ("d".equals(s)) md++;
                    else if ("a".equals(s)) ma++;
                    else mc++;
                }
            }
            prefs(c).edit()
                    .putString("date", date)
                    .putInt("water", water)
                    .putInt("goal", o.optInt("goal", 3000))
                    .putString("unit", o.optString("unit", "L"))
                    .putInt("m_d", md).putInt("m_a", ma).putInt("m_c", mc)
                    .putInt("q1", o.optInt("q1", 250))
                    .putInt("q2", o.optInt("q2", 500))
                    .apply();
        } catch (Exception ignored) {
        }
        updateAll(c);
    }

    /** Si lo guardado es de otro día, deja el día de hoy en cero (conserva meta y unidades). */
    private static SharedPreferences.Editor rollDay(Context c) {
        SharedPreferences p = prefs(c);
        SharedPreferences.Editor ed = p.edit();
        if (!today().equals(p.getString("date", ""))) {
            ed.putString("date", today()).putInt("water", 0).putInt("m_d", 0).putInt("m_a", 0).putInt("m_c", 0);
        }
        return ed;
    }

    private static void addPending(Context c, JSONObject e) {
        try {
            JSONArray a = new JSONArray(prefs(c).getString("pending", "[]"));
            a.put(e);
            prefs(c).edit().putString("pending", a.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    static void addWater(Context c, int ml) {
        if (ml <= 0) return;
        rollDay(c).apply();
        prefs(c).edit().putInt("water", water(c) + ml).apply();
        try {
            JSONObject e = new JSONObject();
            e.put("t", "w");
            e.put("id", "wg" + System.currentTimeMillis());
            e.put("date", today());
            e.put("time", nowTime());
            e.put("ml", ml);
            addPending(c, e);
        } catch (Exception ignored) {
        }
        updateAll(c);
    }

    /** Registra una comida a la hora actual y devuelve la franja ("d", "a" o "c"). */
    static String addMeal(Context c) {
        String slot = slotNow();
        rollDay(c).apply();
        prefs(c).edit().putInt("m_" + slot, meals(c, slot) + 1).apply();
        try {
            JSONObject e = new JSONObject();
            e.put("t", "m");
            e.put("id", "mg" + System.currentTimeMillis());
            e.put("date", today());
            e.put("time", nowTime());
            e.put("slot", slot);
            addPending(c, e);
        } catch (Exception ignored) {
        }
        updateAll(c);
        return slot;
    }

    /** Entrega a la app lo registrado desde los widgets y lo borra de aquí. */
    static String takePending(Context c) {
        String s = prefs(c).getString("pending", "[]");
        prefs(c).edit().putString("pending", "[]").apply();
        return s;
    }

    static void updateAll(Context c) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] w = m.getAppWidgetIds(new ComponentName(c, WaterWidget.class));
            if (w != null && w.length > 0) WaterWidget.render(c, m, w);
            int[] f = m.getAppWidgetIds(new ComponentName(c, MealWidget.class));
            if (f != null && f.length > 0) MealWidget.render(c, m, f);
        } catch (Exception ignored) {
        }
    }
}
