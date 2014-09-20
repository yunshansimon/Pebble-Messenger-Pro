
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

package yangtsao.pebblemessengerpro.activities;



import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.getpebble.android.kit.PebbleKit;

import yangtsao.pebblemessengerpro.Constants;
import yangtsao.pebblemessengerpro.R;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class SetupFragment extends Fragment {
    private static final int positionIndex=0;
    private TextView tvPebbleStatus;
    private TextView tvAccessStatus;
    private TextView tvWatchList;
    private TextView tvFontStatus;
    private Button btGotoSetting;
    private Button btGotoPebble;
    private Context _context;

    public SetupFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View setupView=inflater.inflate(R.layout.fragment_setup, container, false);
        tvPebbleStatus=(TextView) setupView.findViewById(R.id.text_test_pebble_result);
        tvAccessStatus=(TextView) setupView.findViewById(R.id.text_accessibility_service_result);
        tvWatchList=(TextView) setupView.findViewById(R.id.text_watch_list);
        tvFontStatus=(TextView) setupView.findViewById(R.id.text_font_base);
        btGotoSetting=(Button) setupView.findViewById(R.id.button_goto_setting);
        btGotoSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        btGotoPebble=(Button) setupView.findViewById(R.id.button_goto_pebble);
        btGotoPebble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(Constants.PEBBLE_APP_URL));
                startActivity(i);
            }
        });
        return setupView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((NavigationActivity) activity).onSectionAttached(positionIndex);
        _context=activity.getApplicationContext();
    }

    private boolean isPebbleOk(Context context){
    return PebbleKit.isWatchConnected(context);
    }

    private boolean isAccessServiceOk(Context context){
        int accessibilityEnabled = 0;
        boolean accessibilityFound = false;
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            return accessibilityFound;
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessabilityService = splitter.next();
                    if (accessabilityService.equalsIgnoreCase(Constants.ACCESSIBILITY_SERVICE)) {
                        accessibilityFound = true;
                        break;
                    }
                }
            }
        }
        return accessibilityFound;
    }

    private boolean isWatchListEmpty(Context context){
        String strAppList=PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREFERENCE_PACKAGE_LIST,"");
        if (strAppList.equalsIgnoreCase("")){
            return false;
        }else{
            return true;
        }

    }

    private boolean isFontBaseOk(Context context){

        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.DATABASE_READY,false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isPebbleOk(_context)){
            tvPebbleStatus.setText(R.string.setup_check_ok);
            tvPebbleStatus.setTextColor(Color.WHITE);
        }else{
            tvPebbleStatus.setText(R.string.setup_check_bad);
            tvAccessStatus.setTextColor(Color.RED);
        }
        if (isAccessServiceOk(_context)){
            tvAccessStatus.setText(R.string.setup_check_ok);
            tvAccessStatus.setTextColor(Color.WHITE);
        }else{
            tvAccessStatus.setText(R.string.setup_check_bad);
            tvAccessStatus.setTextColor(Color.RED);
        }
        if (isWatchListEmpty(_context)){
            tvWatchList.setText(R.string.setup_check_ok);
            tvWatchList.setTextColor(Color.WHITE);
        }else{
            tvWatchList.setText(R.string.setup_check_empty);
            tvWatchList.setTextColor(Color.RED);
        }
        if (isFontBaseOk(_context)){
            tvFontStatus.setText(R.string.setup_check_ok);
            tvFontStatus.setTextColor(Color.WHITE);
        }else{
            tvFontStatus.setText(R.string.setup_check_bad);
            tvFontStatus.setTextColor(Color.RED);
        }
    }
}
