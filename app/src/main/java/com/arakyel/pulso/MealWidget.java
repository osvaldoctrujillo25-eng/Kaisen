package com.arakyel.pulso;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

/** Widget de comida: desayuno, almuerzo y cena de hoy, y un botón para registrar una comida. */
public class MealWidget extends AppWidgetProvider {

    static final String ADD = "com.arakyel.pulso.WIDGET_ADD_MEAL";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        render(c, m, ids);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        if (ADD.equals(i.getAction())) {
            String s = WidgetData.addMeal(c);
            String name = "d".equals(s) ? "Desayuno" : "a".equals(s) ? "Almuerzo" : "Cena";
            Toast.makeText(c, name + " registrado · " + WidgetData.nowTime(), Toast.LENGTH_SHORT).show();
        }
    }

    static void render(Context c, AppWidgetManager m, int[] ids) {
        int d = WidgetData.meals(c, "d"), a = WidgetData.meals(c, "a"), n = WidgetData.meals(c, "c");
        int total = d + a + n;
        String now = WidgetData.slotNow();

        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_meal);
        v.setTextViewText(R.id.m_total, total == 0 ? "Sin registrar" : total + (total == 1 ? " comida" : " comidas"));
        slot(v, R.id.m_d, R.id.m_d_l, d, "d".equals(now));
        slot(v, R.id.m_a, R.id.m_a_l, a, "a".equals(now));
        slot(v, R.id.m_c, R.id.m_c_l, n, "c".equals(now));
        String next = "d".equals(now) ? "desayuno" : "a".equals(now) ? "almuerzo" : "cena";
        v.setTextViewText(R.id.m_add, "+ Registrar " + next);

        Intent add = new Intent(c, MealWidget.class);
        add.setAction(ADD);
        v.setOnClickPendingIntent(R.id.m_add, PendingIntent.getBroadcast(c, 3201, add,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        v.setOnClickPendingIntent(R.id.m_root, WaterWidget.open(c, "alimentacion", 3200));
        m.updateAppWidget(ids, v);
    }

    private static void slot(RemoteViews v, int dot, int label, int count, boolean current) {
        v.setTextViewText(dot, count == 0 ? "—" : count == 1 ? "✓" : String.valueOf(count));
        v.setInt(dot, "setBackgroundResource", count > 0 ? R.drawable.widget_dot_on : R.drawable.widget_dot_off);
        v.setTextColor(dot, count > 0 ? 0xFFFFFFFF : 0xFF8E8E93);
        v.setTextColor(label, current ? 0xFFFFFFFF : 0xFF8E8E93);
    }
}
