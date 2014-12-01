
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.getpebble.android.kit.PebbleKit;

import com.yangtsaosoftware.pebblemessenger.Constants;
import com.yangtsaosoftware.pebblemessenger.R;
import com.yangtsaosoftware.pebblemessenger.services.PebbleCenter;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class SetupFragment extends Fragment implements TextToSpeech.OnInitListener {
    private static final int positionIndex=0;
    private TextView tvPebbleStatus;
    private TextView tvAccessStatus;
    private TextView tvWatchList;
    private TextView tvFontStatus;
    private TextView tvSpeechEngine;
    private TextView tvCheckPebbleFirmwareResult;
    private TextView tvCheckPmpResult;
    private Button btGotoSpeech;
    private Button btGotoSetting;
    private Button btGotoPebble;
    private Button btCheckPmp;
    private Context _context;
    private TextToSpeech myTTS;
    private ProgressBar myProgress;

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
        tvSpeechEngine=(TextView) setupView.findViewById(R.id.text_speech_engine);
        tvCheckPebbleFirmwareResult=(TextView) setupView.findViewById(R.id.text_test_firmware_result);
        tvCheckPmpResult=(TextView) setupView.findViewById(R.id.text_check_pmp_result);
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
        btGotoSpeech=(Button) setupView.findViewById(R.id.button_goto_speech_setting);
        btGotoSpeech.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent checkIntent=new Intent();
                checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                getActivity().startActivityFromFragment(SetupFragment.this,checkIntent,3);
            }
        });
        myProgress=(ProgressBar) setupView.findViewById(R.id.progressBar);
        btCheckPmp=(Button) setupView.findViewById(R.id.button_check_pmp);
        btCheckPmp.setOnClickListener(new View.OnClickListener(){
            @Override
        public void onClick(View view){
                myProgress.setVisibility(ProgressBar.VISIBLE);
                Intent inner_intent=new Intent(PebbleCenter.class.getName());
                inner_intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PEBBLE_TEST);
                LocalBroadcastManager.getInstance(_context).sendBroadcast(inner_intent);
            }
        });

        return setupView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((NavigationActivity) activity).onSectionAttached(positionIndex);
        _context=activity.getApplicationContext();
        BroadcastReceiver br= new BroadcastReceiver(){
          @Override
        public void onReceive(Context context, Intent intent) {
              myProgress.setVisibility(ProgressBar.GONE);
              byte[] command=intent.getByteArrayExtra(Constants.BROADCAST_VERSION);
              boolean result=true;
              if (command==null || command[0]< Constants.PEBBLE_VERSION[0]){
                result=false;
              }else if(command[1]<Constants.PEBBLE_VERSION[1]){
                  result=false;
              }else if(command[2]<Constants.PEBBLE_VERSION[2]){
                  result=false;
              }
              tvCheckPmpResult.setTextColor(result? Color.WHITE: Color.RED);
              tvCheckPmpResult.setText(result? R.string.setup_check_ok :R.string.setup_check_result_need_update);
          }
        };
        IntentFilter intentFilter=new IntentFilter(SetupFragment.class.getName());
        LocalBroadcastManager.getInstance(_context).registerReceiver(br,intentFilter);
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

    private boolean isPebbleFirmwareOk(Context context){
        PebbleKit.FirmwareVersionInfo info=PebbleKit.getWatchFWVersion(context);
      //  Constants.log("GetWatchVersion", info.getTag() + " major:"+ String.valueOf(info.getMajor())+ " minor:"+ String.valueOf(info.getMinor())+ " point:"+ String.valueOf(info.getPoint()));

        if(info.getMajor()<(int)Constants.PEBBLE_FIRMWARE[0]){
            return false;
        }else if(info.getMinor()<(int)Constants.PEBBLE_FIRMWARE[1]){
            return false;
        }else if(info.getPoint()<(int)Constants.PEBBLE_FIRMWARE[2]){
            return false;
        }else{
            return true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isPebbleOk(_context)){
            tvPebbleStatus.setText(R.string.setup_check_ok);
            tvPebbleStatus.setTextColor(Color.WHITE);
        }else{
            tvPebbleStatus.setText(R.string.setup_check_disconnected);
            tvPebbleStatus.setTextColor(Color.RED);
        }
        if (isAccessServiceOk(_context)){
            tvAccessStatus.setText(R.string.setup_check_ok);
            tvAccessStatus.setTextColor(Color.WHITE);
        }else{
            tvAccessStatus.setText(R.string.setup_check_bad);
            tvAccessStatus.setTextColor(Color.RED);
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        boolean result=isPebbleFirmwareOk(_context);
        tvCheckPebbleFirmwareResult.setTextColor(result? Color.WHITE:Color.RED);
        tvCheckPebbleFirmwareResult.setText(result? R.string.setup_check_ok:R.string.setup_check_result_need_update);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==3){
            if (resultCode==TextToSpeech.Engine.CHECK_VOICE_DATA_PASS){
                myTTS=new TextToSpeech(_context,this);



            }else{
                tvSpeechEngine.setText(getString(R.string.setup_tts_failed));
                tvSpeechEngine.setTextColor(Color.RED);
                btGotoSpeech.setVisibility(Button.VISIBLE);
            }
        }
    }

    @Override
    public void onInit(int i) {
       if (i==TextToSpeech.SUCCESS){
           tvSpeechEngine.setText(myTTS.getDefaultEngine()+ " " + getString(R.string.setup_tts_default_locale) + myTTS.getLanguage().getLanguage());
           tvSpeechEngine.setTextColor(Color.WHITE);
       }else {
           myTTS.shutdown();
           myTTS=null;
       }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        super.onPause();
        if(myTTS!=null) myTTS.shutdown();
    }
}
