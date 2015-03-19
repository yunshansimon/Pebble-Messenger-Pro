
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

package com.yangtsaosoftware.pebblemessenger.activities;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;


import com.yangtsaosoftware.pebblemessenger.Constants;
import com.yangtsaosoftware.pebblemessenger.R;
import com.yangtsaosoftware.pebblemessenger.services.MessageProcessingService;
import com.yangtsaosoftware.pebblemessenger.services.NotificationService;
import com.yangtsaosoftware.pebblemessenger.services.PebbleCenter;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class SettingFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
private static final int positionIndex=0;
    private Context _context;
    private SharedPreferences prefs;
    private static String TAG_NAME="SettingFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs= PreferenceManager.getDefaultSharedPreferences(_context);
        addPreferencesFromResource(R.xml.preference);
        Preference butAccessbility=findPreference("pref_button_accessibility");
        butAccessbility.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                return true;
            }
        });
        Preference butPebbleApp=findPreference("pref_button_app");
        butPebbleApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(Constants.PEBBLE_APP_URL));
                startActivity(i);
                return true;
            }
        });

        Preference butSetuptts=findPreference("pref_button_setuptts");
        butSetuptts.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                return true;
            }
        });


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
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_BLACK_BACKGROUND)){
            Intent intent=new Intent(PebbleCenter.class.getName());
            intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);
            LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
        }else if(s.equalsIgnoreCase(Constants.PREFERENCE_READ_MESSAGE)){
            Intent intent=new Intent(MessageProcessingService.class.getName());
            intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);
            LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
        }

    }
}
