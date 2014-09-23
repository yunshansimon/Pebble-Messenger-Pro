
/*
 * Pebble Messenger is used to display non-english message on Pebble.
 * Copyright (C) 2014  Yang Tsao
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.yangtsaosoftware.pebblemessenger.db;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.app.Notification;
import android.content.Intent;

import com.yangtsaosoftware.pebblemessenger.R;

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
