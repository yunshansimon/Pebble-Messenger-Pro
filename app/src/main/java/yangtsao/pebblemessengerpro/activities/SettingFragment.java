package yangtsao.pebblemessengerpro.activities;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;


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

        if(s.equalsIgnoreCase(Constants.PREFERENCE_NOTIF_SCREEN_ON)){
            Intent intent=new Intent(NotificationService.class.getName());
            intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);
            LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_MESSAGE_SCALE)){
            Intent intent=new Intent(MessageProcessingService.class.getName());
            intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);
            LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);

            intent=new Intent(PebbleCenter.class.getName());
            intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);
            LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_CALL_ENABLE)){
            Intent intent=new Intent(PebbleCenter.class.getName());
            intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);
            LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);

        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_CALL_QUIET)){
            Intent intent=new Intent(MessageProcessingService.class.getName());
            intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);
            LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_CALL_SMS_SHORT)){
            Intent intent=new Intent(PebbleCenter.class.getName());
            intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);
            LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_CALL_SMS_LONG)){
            Intent intent=new Intent(PebbleCenter.class.getName());
            intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);
            LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_QUIET_HOURS)){
            Intent intent=new Intent(MessageProcessingService.class.getName());
            intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);
            LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_QUIET_HOURS_AFTER)){
            Intent intent=new Intent(MessageProcessingService.class.getName());
            intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);
            LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_QUIET_HOURS_BEFORE)){
            Intent intent=new Intent(MessageProcessingService.class.getName());
            intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);
            LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_MIN_NOTIFICATION_WAIT)){
            Intent intent=new Intent(PebbleCenter.class.getName());
            intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);
            LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
        }

    }
}
