package com.arakyel.pulso;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

/** Widget de agua: progreso del día y dos botones para sumar sin abrir la app. */
public class WaterWidget extends AppWidgetProvider {

    static final String ADD = "com.arakyel.pulso.WIDGET_ADD_WATER";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        render(c, m, ids);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        if (ADD.equals(i.getAction())) {
            int ml = i.getIntExtra("ml", 0);
            WidgetData.addWater(c, ml);
            Toast.makeText(c, "+" + WidgetData.fmtWater(c, ml) + " de agua", Toast.LENGTH_SHORT).show();
        }
    }

    static void render(Context c, AppWidgetManager m, int[] ids) {
        int ml = WidgetData.water(c), goal = WidgetData.goal(c);
        int pct = (int) Math.min(100, Math.round(ml * 100.0 / goal));
        boolean done = ml >= goal;
        int q1 = WidgetData.quick(c, 1), q2 = WidgetData.quick(c, 2);

        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_water);
        v.setTextViewText(R.id.w_amount, WidgetData.fmtWater(c, ml));
        v.setTextViewText(R.id.w_goal, done ? "¡Meta cumplida!" : "de " + WidgetData.fmtWater(c, goal) + " · faltan " + WidgetData.fmtWater(c, goal - ml));
        v.setTextViewText(R.id.w_pct, pct + "%");
        v.setTextColor(R.id.w_pct, done ? 0xFF10B981 : 0xFFFFFFFF);
        v.setTextColor(R.id.w_goal, done ? 0xFF10B981 : 0xFF8E8E93);
        v.setProgressBar(R.id.w_bar, 100, pct, false);
        v.setProgressBar(R.id.w_bar_ok, 100, pct, false);
        v.setViewVisibility(R.id.w_bar, done ? View.GONE : View.VISIBLE);
        v.setViewVisibility(R.id.w_bar_ok, done ? View.VISIBLE : View.GONE);
        v.setTextViewText(R.id.w_add1, "+" + WidgetData.fmtWater(c, q1));
        v.setTextViewText(R.id.w_add2, "+" + WidgetData.fmtWater(c, q2));
        v.setOnClickPendingIntent(R.id.w_add1, add(c, q1, 3101));
        v.setOnClickPendingIntent(R.id.w_add2, add(c, q2, 3102));
        v.setOnClickPendingIntent(R.id.w_root, open(c, "hidratacion", 3100));
        m.updateAppWidget(ids, v);
    }

    static PendingIntent add(Context c, int ml, int code) {
        Intent i = new Intent(c, WaterWidget.class);
        i.setAction(ADD);
        i.putExtra("ml", ml);
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent open(Context c, String go, int code) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.putExtra("goto", go);
        return PendingIntent.getActivity(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
