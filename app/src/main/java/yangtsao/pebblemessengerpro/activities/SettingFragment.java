package yangtsao.pebblemessengerpro.activities;



import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import yangtsao.pebblemessengerpro.Constants;
import yangtsao.pebblemessengerpro.R;
import yangtsao.pebblemessengerpro.services.MessageProcessingService;
import yangtsao.pebblemessengerpro.services.NotificationService;
import yangtsao.pebblemessengerpro.services.PebbleCenter;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class SettingFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
private static final int positionIndex=1;
    private Context _context;
    private SharedPreferences prefs;
    private static String TAG_NAME="SettingFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs= PreferenceManager.getDefaultSharedPreferences(_context);
        addPreferencesFromResource(R.xml.preference);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((NavigationActivity) activity).onSectionAttached(positionIndex);
        _context=activity.getApplicationContext();
    }

    @Override
    public void onResume() {
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Constants.log(TAG_NAME,"Preferenc key:" + s + "changed.");

        if (s.equalsIgnoreCase(Constants.PREFERENCE_NOTIFICATIONS_ONLY)){
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(NotificationService.class.getName()));
        }else if (s.equalsIgnoreCase(Constants.PREFERENCE_NO_ONGOING_NOTIF)) {
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(NotificationService.class.getName()));
        }else if (s.equalsIgnoreCase(Constants.PREFERENCE_PACKAGE_LIST)) {
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(NotificationService.class.getName()));
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_NOTIF_SCREEN_ON)){
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(NotificationService.class.getName()));
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_MESSAGE_SCALE)){
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(MessageProcessingService.class.getName()));
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(PebbleCenter.class.getName()));
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_CALL_ENABLE)){
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(NotificationService.class.getName()));
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(PebbleCenter.class.getName()));
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_CALL_QUIET)){
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(MessageProcessingService.class.getName()));
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_CALL_SMS_SHORT)){
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(PebbleCenter.class.getName()));
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_CALL_SMS_LONG)){
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(PebbleCenter.class.getName()));
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_QUIET_HOURS)){
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(MessageProcessingService.class.getName()));
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_QUIET_HOURS_AFTER)){
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(MessageProcessingService.class.getName()));
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_QUIET_HOURS_BEFORE)){
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(MessageProcessingService.class.getName()));
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_MIN_NOTIFICATION_WAIT)){
            LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(PebbleCenter.class.getName()));

        }

    }
}
