package __PKG__;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;

public class EdupageJobService extends JobService {
    @Override
    public boolean onStartJob(final JobParameters params) {
        final Context ctx = getApplicationContext();
        new Thread(new Runnable() {
            public void run() {
                try { EdupageWatcher.run(ctx); } catch (Exception e) { }
                jobFinished(params, false);
            }
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) { return true; }
}
