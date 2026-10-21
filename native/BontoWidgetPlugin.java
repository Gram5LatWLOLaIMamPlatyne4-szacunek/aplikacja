package __PKG__;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "BontoWidget")
public class BontoWidgetPlugin extends Plugin {
    private static final int JOB_ID = 4711;

    private void refresh(Context c) {
        int[] ids = AppWidgetManager.getInstance(c)
            .getAppWidgetIds(new ComponentName(c, BontoWidgetProvider.class));
        Intent i = new Intent(c, BontoWidgetProvider.class);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        c.sendBroadcast(i);
    }

    @PluginMethod
    public void setWidget(PluginCall call) {
        Context c = getContext();
        c.getSharedPreferences("bonto_widget", Context.MODE_PRIVATE).edit()
            .remove("days")
            .putString("title", call.getString("title", ""))
            .putString("body", call.getString("body", "")).apply();
        refresh(c);
        call.resolve();
    }

    @PluginMethod
    public void setWidgetDays(PluginCall call) {
        JSObject d = call.getObject("days");
        if (d == null) { call.reject("brak days"); return; }
        Context c = getContext();
        c.getSharedPreferences("bonto_widget", Context.MODE_PRIVATE).edit().putString("days", d.toString()).apply();
        refresh(c);
        call.resolve();
    }

    @PluginMethod
    public void watchEdupage(PluginCall call) {
        final Context c = getContext();
        c.getSharedPreferences("bonto_edupage", Context.MODE_PRIVATE).edit()
            .putString("base", call.getString("base", ""))
            .putString("cls", call.getString("cls", ""))
            .putString("group", call.getString("group", "")).apply();
        try {
            JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            boolean pending = Build.VERSION.SDK_INT >= 24 && js.getPendingJob(JOB_ID) != null;
            if (!pending) {
                js.schedule(new JobInfo.Builder(JOB_ID, new ComponentName(c, EdupageJobService.class))
                    .setPeriodic(15 * 60 * 1000L)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true).build());
            }
        } catch (Exception e) { }
        new Thread(new Runnable() {
            public void run() { try { EdupageWatcher.run(c); } catch (Exception e) { } }
        }).start();
        call.resolve();
    }

    @PluginMethod
    public void stopEdupage(PluginCall call) {
        try { ((JobScheduler) getContext().getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancel(JOB_ID); } catch (Exception e) { }
        call.resolve();
    }
}
