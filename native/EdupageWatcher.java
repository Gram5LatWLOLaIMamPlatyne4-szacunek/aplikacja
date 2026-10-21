package __PKG__;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

public class EdupageWatcher {
    static final Object LOCK = new Object();

    static String http(String url, String body) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) PlanApp");
            if (body != null) {
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                try (OutputStream o = c.getOutputStream()) { o.write(body.getBytes(StandardCharsets.UTF_8)); }
            }
            if (c.getResponseCode() >= 400) throw new IOException("HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return out.toString("UTF-8");
            }
        } finally { c.disconnect(); }
    }

    /** Pobiera zastępstwa na 5 najbliższych dni szkolnych i powiadamia o nowych odwołaniach/zmianach. */
    public static void run(Context ctx) throws Exception {
        synchronized (LOCK) {
            SharedPreferences p = ctx.getSharedPreferences("bonto_edupage", Context.MODE_PRIVATE);
            String base = p.getString("base", ""), cls = p.getString("cls", ""), group = p.getString("group", "");
            if (base.isEmpty() || cls.isEmpty()) return;
            String page = http(base + "/substitution/", null);
            Matcher gm = Pattern.compile("gsh[\"']?\\s*[:=]\\s*[\"']([0-9a-f]+)[\"']", Pattern.CASE_INSENSITIVE).matcher(page);
            String gsh = gm.find() ? gm.group(1) : "00000000";
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat lf = new SimpleDateFormat("EEE d.MM", new Locale("pl"));
            boolean first = !p.contains("seen");
            Set<String> seen = p.getStringSet("seen", new HashSet<String>());
            Set<String> keep = new HashSet<>();
            List<String> fresh = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            int days = 0;
            for (int i = 0; i < 12 && days < 5; i++, cal.add(Calendar.DAY_OF_MONTH, 1)) {
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) continue;
                days++;
                String date = f.format(cal.getTime());
                String payload = "{\"__args\":[null,{\"date\":\"" + date + "\",\"mode\":\"classes\"}],\"__gpid\":\"\",\"__gsh\":\"" + gsh + "\"}";
                String resp = http(base + "/substitution/server/viewer.js?__func=getSubstViewerDayDataHtml", payload);
                String html = new JSONObject(resp).optString("r", "");
                for (String[] row : EdupageParse.rows(html, cls)) {
                    if (EdupageParse.mentionsOtherGroup(row[1], group)) continue;
                    String key = date + "|" + Integer.toHexString((row[0] + "|" + row[1]).hashCode());
                    keep.add(key);
                    if (!seen.contains(key))
                        fresh.add(("1".equals(row[2]) ? "❌ " : "🔁 ") + lf.format(cal.getTime()) + " " + (row[0].isEmpty() ? "" : row[0] + ". ") + row[1]);
                }
            }
            p.edit().putStringSet("seen", keep).apply();
            if (!first && !fresh.isEmpty()) notifyUser(ctx, cls, fresh);
        }
    }

    static void notifyUser(Context ctx, String cls, List<String> lines) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        String ch = "bonto_subs";
        if (Build.VERSION.SDK_INT >= 26)
            nm.createNotificationChannel(new NotificationChannel(ch, "Zmiany w planie", NotificationManager.IMPORTANCE_HIGH));
        Intent li = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, li, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(ctx, ch) : new Notification.Builder(ctx);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Plan " + cls + " – zmiany (" + lines.size() + ")")
            .setContentText(lines.get(0))
            .setStyle(new Notification.BigTextStyle().bigText(sb.toString().trim()))
            .setContentIntent(pi)
            .setAutoCancel(true);
        nm.notify((int) (System.currentTimeMillis() & 0x7fffffff), b.build());
    }
}
