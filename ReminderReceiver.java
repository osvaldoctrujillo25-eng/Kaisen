package com.arakyel.pulso;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Muestra la notificación de un recordatorio y programa las alarmas. */
public class ReminderReceiver extends BroadcastReceiver {

    static final String CHANNEL = "recordatorios";

    @Override
    public void onReceive(Context c, Intent i) {
        int code = i.getIntExtra("code", 0);
        String title = i.getStringExtra("title");
        String text = i.getStringExtra("text");
        ReminderStore.remove(c, code);
        show(c, code, (title == null || title.isEmpty()) ? "Recordatorio" : title, text == null ? "" : text);
    }

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Recordatorios", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Avisos de tus recordatorios de Kaizen");
            ch.enableVibration(true);
            nm.createNotificationChannel(ch);
        }
    }

    static void show(Context c, int code, String title, String text) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        ensureChannel(c);
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        open.putExtra("goto", "recordatorios");
        PendingIntent pi = PendingIntent.getActivity(c, code, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CHANNEL)
                : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat_kaizen)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setColor(0xFFE50914)
                .setAutoCancel(true)
                .setContentIntent(pi);
        if (Build.VERSION.SDK_INT < 26) {
            b.setPriority(Notification.PRIORITY_HIGH);
            b.setDefaults(Notification.DEFAULT_ALL);
        }
        try {
            nm.notify(code, b.build());
        } catch (SecurityException ignored) {
            // Sin permiso de notificaciones: no se muestra.
        }
    }

    private static PendingIntent alarmIntent(Context c, int code, String title, String text, int flags) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.putExtra("code", code);
        if (title != null) i.putExtra("title", title);
        if (text != null) i.putExtra("text", text);
        return PendingIntent.getBroadcast(c, code, i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    static void schedule(Context c, int code, long when, String title, String text) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = alarmIntent(c, code, title, text, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }

    static void cancel(Context c, int code) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = alarmIntent(c, code, null, null, PendingIntent.FLAG_NO_CREATE);
        if (pi != null) {
            if (am != null) am.cancel(pi);
            pi.cancel();
        }
    }
}
