package __PKG__;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import org.json.JSONObject;

public class BontoWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        SharedPreferences p = c.getSharedPreferences("bonto_widget", Context.MODE_PRIVATE);
        String title = p.getString("title", "Otwórz aplikację"), body = p.getString("body", "");
        String days = p.getString("days", null);
        if (days != null) {
            try {
                JSONObject o = new JSONObject(days);
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                String pick = null;
                Iterator<String> it = o.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    if (k.compareTo(today) >= 0 && (pick == null || k.compareTo(pick) < 0)) pick = k;
                }
                if (pick != null) {
                    JSONObject d = o.getJSONObject(pick);
                    title = (pick.equals(today) ? "" : "Najbliższy: ") + d.optString("title");
                    body = d.optString("body");
                } else {
                    title = "Brak danych";
                    body = "Otwórz aplikację, aby odświeżyć";
                }
            } catch (Exception e) { }
        }
        Intent launch = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        PendingIntent pi = PendingIntent.getActivity(c, 0, launch,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.bonto_widget);
            v.setTextViewText(R.id.bw_title, title);
            v.setTextViewText(R.id.bw_body, body);
            v.setOnClickPendingIntent(R.id.bw_root, pi);
            m.updateAppWidget(id, v);
        }
    }
}
