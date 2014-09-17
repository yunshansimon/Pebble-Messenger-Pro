package yangtsao.pebblemessengerpro.db;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.app.Notification;
import android.content.Intent;

import yangtsao.pebblemessengerpro.R;

/**
 * Created by yunshan on 8/17/14.
 */
public class FontDbLoadNotifier {
    private final Notification.Builder mBuilder;

    private final NotificationManager mNotificationManager;

    private final Context _context;

    private int pro_now=0;

    public FontDbLoadNotifier(Context context) {
        mBuilder = new Notification.Builder(context).setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getResources().getText(R.string.notif_started_loading)).setAutoCancel(true)
                .setOngoing(true);

        this._context = context;

        mNotificationManager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void changeProgress(int progress, int max) {
        // mBuilder.setContentIntent(PendingIntent.getActivity(context, 0, new
        // Intent(), 0));
        int percent=progress*100/max;
        if (percent>pro_now){
            pro_now=percent;
            int static_max = 100;
            mBuilder.setProgress(static_max, pro_now, false);
            mNotificationManager.notify(1, mBuilder.build());
        }
    }

    public void finish(int progress, int max, final int timeout) {
        mBuilder.setContentText(_context.getResources().getText(R.string.notif_finished_loading));
        mBuilder.setContentIntent(PendingIntent.getActivity(_context, 0, new Intent(), 0));
        mBuilder.setOngoing(false);

        changeProgress(progress, max);

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.currentThread().sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mNotificationManager.cancel(1);
            }
        }).run();
    }
}
