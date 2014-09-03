package yangtsao.pebblemessengerpro.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import yangtsao.pebblemessengerpro.Constants;
import yangtsao.pebblemessengerpro.R;
import yangtsao.pebblemessengerpro.db.MessageDbHandler;

import android.app.Notification;
import android.os.Parcelable;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import static android.content.Context.*;

public class NotificationService extends AccessibilityService {
    private static final String LOG_TAG="NotificationService";
    private long      lastChange;
    private boolean   notifications_only    = false;
    private boolean   no_ongoing_notifs     = false;
    private boolean   notifScreenOn         = true;
    private String[]  packages              = null;
    private File      watchFile;
    private boolean   callMessengerEnable   = false;

    private Messenger rMessageProcessHandler=null;
    private Messenger rPebbleCenterHandler=null;

    private final ServiceConnection connToPebbleCenter=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            rPebbleCenterHandler=new Messenger(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            rPebbleCenterHandler=null;
        }
    };

    private final ServiceConnection connToMessageProcess =new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            rMessageProcessHandler=new Messenger(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            rMessageProcessHandler=null;
        }
    };

    public NotificationService() {
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        PowerManager powMan = (PowerManager) this.getSystemService(POWER_SERVICE);
        if (!notifScreenOn && powMan.isScreenOn()) {
            return;
        }
        if (watchFile.lastModified() > lastChange) {
            loadPrefs();
        }

        if (notifications_only) {
            if (event != null) {
                Parcelable parcelable = event.getParcelableData();
                if (!(parcelable instanceof Notification)) {

                    Constants.log(LOG_TAG,
                            "Event is not a notification and notifications only is enabled. Returning.");
                    return;
                }
            }
        }
        if (no_ongoing_notifs) {
            Parcelable parcelable = event.getParcelableData();
            if (parcelable instanceof Notification) {
                Notification notif = (Notification) parcelable;
                Constants.log(
                        LOG_TAG,
                        "Looking at " + String.valueOf(notif.flags) + " vs "
                                + String.valueOf(Notification.FLAG_ONGOING_EVENT));
                if ((notif.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
                    Constants
                            .log(LOG_TAG,
                                    "Event is a notification, notification flag contains ongoing, and no ongoing notification is true. Returning.");
                    return;
                }
            } else {
                Constants.log(LOG_TAG, "Event is not a notification.");
            }
        }
        PackageManager pm = getPackageManager();
        String eventPackageName;
        if (event.getPackageName() != null) {
            eventPackageName = event.getPackageName().toString();
        } else {
            Constants.log(LOG_TAG, "Can't get event package name. Returning.");
            return;
        }
        boolean found = false;
        for (String packageName : packages) {
            if (packageName.equalsIgnoreCase(eventPackageName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            Constants.log(LOG_TAG, eventPackageName + " was not found in the include list. Returning.");
            return;
        }
        String title = eventPackageName.substring(eventPackageName.lastIndexOf(".")+1);
        // get the notification text
        String notificationText = event.getText().toString();
        // strip the first and last characters which are [ and ]
        notificationText = notificationText.substring(1, notificationText.length() - 1);
        if (!eventPackageName.contentEquals("com.android.mms")) {

            Constants.log(LOG_TAG, "Fetching extras from notification");
            Parcelable parcelable = event.getParcelableData();
            if (parcelable instanceof Notification) {
                String str = getExtraBigData((Notification) parcelable, notificationText.trim());
                if (notificationText.contains(str)) {

                } else {
                    notificationText += "\n" + str;
                }
            }
        }
        if (notificationText.length() == 0) {
            return;
        }
        Message msg=Message.obtain();
        msg.what=MessageProcessingService.MSG_NEW_MESSAGE;
        Bundle b=new Bundle();
        b.putString(MessageDbHandler.COL_MESSAGE_APP,title);
        b.putString(MessageDbHandler.COL_MESSAGE_CONTENT,notificationText);
        msg.setData(b);
        try {
            rMessageProcessHandler.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            Constants.log(LOG_TAG,"Error when sending message to MessageProcessingService.");
        }

    }

    private String getExtraBigData(Notification notification, String existing_text) {
        RemoteViews views = null;
        try {
            views = notification.bigContentView;
        } catch (NoSuchFieldError e) {
            return getExtraData(notification, existing_text);
        }
        if (views == null) {
            Constants.log(LOG_TAG, "bigContentView was empty, running normal");

            return getExtraData(notification, existing_text);
        }
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        try {
            ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
            views.reapply(getApplicationContext(), localView);
            return dumpViewGroup(0, localView, existing_text);
        } catch (android.content.res.Resources.NotFoundException e) {
            return "";
        }
    }

    private String getExtraData(Notification notification, String existing_text) {
        Constants.log(LOG_TAG, "I am running extra data");

        RemoteViews views = notification.contentView;
        if (views == null) {
            Constants.log(LOG_TAG, "ContentView was empty, returning a blank string");

            return "";
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        try {
            ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
            views.reapply(getApplicationContext(), localView);
            return dumpViewGroup(0, localView, existing_text);
        } catch (android.content.res.Resources.NotFoundException e) {
            return "";
        } catch (RemoteViews.ActionException e) {
            return "";
        }
    }

    private String dumpViewGroup(int depth, ViewGroup vg, String existing_text) {
        String text = "";
        Constants.log(LOG_TAG, "root view, depth:" + depth + "; view: " + vg);
        for (int i = 0; i < vg.getChildCount(); ++i) {

            View v = vg.getChildAt(i);
            Constants.log(LOG_TAG, "depth: " + depth + "; " + v.getClass().toString() + "; view: " + v);

            if (v.getId() == android.R.id.title || v instanceof android.widget.Button
                    || v.getClass().toString().contains("android.widget.DateTimeView")) {
                Constants.log(LOG_TAG, "I am going to skip this, but if I didn't, the text would be: "
                            + ((TextView) v).getText().toString());
                if (existing_text.isEmpty() && v.getId() == android.R.id.title) {
                    Constants.log(LOG_TAG,
                                "I was going to skip this, but the existing text was empty, and I need something.");
                } else {
                    continue;
                }
            }

            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                if (tv.getText().toString() == "..." || tv.getText().toString() == "�"
                        || isInteger(tv.getText().toString()) || existing_text.contains(tv.getText().toString().trim())) {
                    Constants.log(LOG_TAG, "Text is: " + tv.getText().toString()
                                + " but I am going to skip this");
                    continue;
                }
                text += tv.getText().toString() + "\n";
                Constants.log(LOG_TAG, tv.getText().toString());
            }
            if (v instanceof ViewGroup) {
                text += dumpViewGroup(depth + 1, (ViewGroup) v, existing_text);
            }
        }
        return text;
    }

    public boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void loadPrefs() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        packages = sharedPref.getString(Constants.PREFERENCE_PACKAGE_LIST, "").split(",");
        notifications_only = sharedPref.getBoolean(Constants.PREFERENCE_NOTIFICATIONS_ONLY, true);
        no_ongoing_notifs = sharedPref.getBoolean(Constants.PREFERENCE_NO_ONGOING_NOTIF, false);
        notifScreenOn = sharedPref.getBoolean(Constants.PREFERENCE_NOTIF_SCREEN_ON, true);
        callMessengerEnable=sharedPref.getBoolean(Constants.PREFERENCE_CALL_ENABLE,false);
        lastChange = watchFile.lastModified();
    }
    @Override
    public void onInterrupt() {

    }
  /*  @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    */

    @Override
    public void onServiceConnected() {
        // get inital preferences
        watchFile = new File(getFilesDir() + "PrefsChanged.none");
        if (!watchFile.exists()) {
            try {
                watchFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            watchFile.setLastModified(System.currentTimeMillis());
        }
        loadPrefs();
        bindService(new Intent(this, MessageProcessingService.class), connToMessageProcess,
                Context.BIND_AUTO_CREATE);
        bindService(new Intent(this,PebbleCenter.class),connToPebbleCenter,Context.BIND_AUTO_CREATE);
    }
    @Override
    public void onDestroy() {
        unbindService(connToPebbleCenter);
        unbindService(connToMessageProcess);

    }

    private class MyPhoneListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (!callMessengerEnable) {
                return;
            }

            switch (state) {

                case TelephonyManager.CALL_STATE_RINGING: {
                    Message msg = Message.obtain();
                    msg.what = MessageProcessingService.MSG_NEW_CALL;
                    Bundle b = new Bundle();
                    if (incomingNumber != null && incomingNumber != "") {
                        b.putString(MessageDbHandler.COL_CALL_NUMBER, incomingNumber);
                        b.putString(MessageDbHandler.COL_CALL_NAME,queryNameByNum(incomingNumber));
                    } else {
                        b.putString(MessageDbHandler.COL_CALL_NUMBER, "0");
                        b.putString(MessageDbHandler.COL_CALL_NAME,getString(R.string.notificationservice_privateNumber));
                    }
                    msg.setData(b);
                    try {
                        rMessageProcessHandler.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                {
                    Message msg=Message.obtain();
                    msg.what=PebbleCenter.PEBBLE_CALL_IDLE;
                    try {
                        rPebbleCenterHandler.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        Constants.log(LOG_TAG,"Error when sending message to PebbleCenter.");
                    }
                }
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:

                    break;
            }
            ;
        }
        public  String queryNameByNum(String num) {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(num));

            Cursor cursor = getContentResolver().query(uri, new String[] {
                    ContactsContract.PhoneLookup.DISPLAY_NAME
            }, null, null, null);
            String nameString=getString(R.string.notificationservice_unknownperson);
            if (cursor!=null){
                if (cursor.moveToFirst()){
                    int columnNumberId=cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                    nameString=cursor.getString(columnNumberId);
                }
                cursor.close();
            }
            return nameString;
            }


    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
