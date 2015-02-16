
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
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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
    private static final int positionIndex=1;

    private Context _context;
    private TextToSpeech myTTS;
    private TextView textInfo;
    private ScrollView svMyview;
    private static String RED_TXT_PANAL = "<font color=#C80000>%s</font>";
    private static String GREEN_TXT_PANAL="<font color=#00C800>%s</font>";

    public SetupFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View setupView=inflater.inflate(R.layout.fragment_setup, container, false);
        textInfo=(TextView)setupView.findViewById(R.id.setup_info_text);
        svMyview=(ScrollView)setupView.findViewById(R.id.setup_scroll);
        setHasOptionsMenu(true);
        return setupView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((NavigationActivity) activity).onSectionAttached(positionIndex);
        _context=activity.getApplicationContext();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_setup,menu);
        super.onCreateOptionsMenu(menu, inflater);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.setup_base_test:
                run_basic_test();
                return true;

            case R.id.setup_app_test:
                test_pebble_app(_context);
                return true;

            case R.id.setup_tts_test:
                run_tts_test();
                return true;

            case R.id.setup_all_test:
                run_basic_test();
                run_tts_test();
                try{
                    Thread.sleep(2000);
                }catch (Exception e){

                }
                test_pebble_app(_context);

                return true;

            case R.id.setup_report_author:
                send_report_to_author();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void run_basic_test(){
        SpannableStringBuilder ssb=new SpannableStringBuilder();
        ssb.append(_context.getString(R.string.setup_basic_test));
        ssb.append('\n');
        ssb.append(_context.getString(R.string.setup_test_pebble_title));
        ssb.append(test_Pebble_state(_context));
        ssb.append('\n');
        ssb.append(_context.getString(R.string.setup_test_pebble_firmware));
        ssb.append(test_PebbleFirmware_state(_context));
        ssb.append('\n');
        ssb.append(_context.getString(R.string.setup_accessibility_service_title));
        ssb.append(test_AccessService_state(_context));
        ssb.append('\n');
        ssb.append(_context.getString(R.string.setup_watch_list));
        ssb.append(test_WatchList_state(_context));
        ssb.append('\n');
        ssb.append(_context.getString(R.string.setup_font_base_status));
        ssb.append(test_FontBase_state(_context));
        ssb.append('\n');
        textInfo.setText(ssb);
        svMyview.fullScroll(View.FOCUS_DOWN);
    }
    private void send_report_to_author(){
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");
        String[] TO = {Constants.AUTHOR_EMAIL};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, _context.getString(R.string.setup_report_subject));
        emailIntent.putExtra(Intent.EXTRA_TEXT, textInfo.getText());
        startActivity(Intent.createChooser(emailIntent, _context.getString(R.string.setup_send_mail)));

    }
    private void run_tts_test(){
        SpannableStringBuilder ssb=new SpannableStringBuilder(textInfo.getText());
        ssb.append(_context.getString(R.string.setup_tts_test));
        ssb.append('\n');
        ssb.append(_context.getString(R.string.setup_tts_engine));
        textInfo.setText(ssb);
        svMyview.fullScroll(View.FOCUS_DOWN);
        Intent checkIntent=new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        getActivity().startActivityFromFragment(SetupFragment.this,checkIntent,3);
    }
    private Spanned test_Pebble_state(Context context){
        return (PebbleKit.isWatchConnected(context) ? greenText(R.string.setup_check_connected): redText(R.string.setup_check_disconnected));
    }

    private Spanned test_AccessService_state(Context context){

        int accessibilityEnabled;
        boolean accessibilityFound =false;
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {

            return redText(R.string.setup_check_bad);
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

        return (accessibilityFound? greenText(R.string.setup_check_opened):redText(R.string.setup_check_closed));
    }

    private Spanned greenText(int resId){
        return Html.fromHtml(String.format(GREEN_TXT_PANAL,_context.getString(resId)));
    }
    private Spanned redText(int resId){
        return Html.fromHtml(String.format(RED_TXT_PANAL,_context.getString(resId)));
    }

    private Spanned test_WatchList_state(Context context){
        String strAppList=PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREFERENCE_PACKAGE_LIST,"");
        if (strAppList.equalsIgnoreCase("")){
            return redText(R.string.setup_check_empty);
        }else{
            return greenText(R.string.setup_check_ok);
        }
    }

    private void test_pebble_app(Context context){
        BroadcastReceiver br= new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {

                byte[] command=intent.getByteArrayExtra(Constants.BROADCAST_VERSION);
                boolean result=true;
                if (command==null || command[0]< Constants.PEBBLE_VERSION[0]){
                    result=false;
                }else if(command[1]<Constants.PEBBLE_VERSION[1]){
                    result=false;
                }else if(command[2]<Constants.PEBBLE_VERSION[2]){
                    result=false;
                }
                SpannableStringBuilder ssb=new SpannableStringBuilder();
                ssb.append(textInfo.getText());
                ssb.append(context.getString(R.string.setup_app_test));
                ssb.append('\n');
                ssb.append(context.getString(R.string.setup_install_pebble_app));
                ssb.append((result?greenText(R.string.setup_check_ok):redText(R.string.setup_check_bad)));
                ssb.append('\n');
                textInfo.setText(ssb);
                svMyview.fullScroll(View.FOCUS_DOWN);
                LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
            }
        };
        IntentFilter intentFilter=new IntentFilter(SetupFragment.class.getName());
        LocalBroadcastManager.getInstance(context).registerReceiver(br,intentFilter);
        Intent inner_intent=new Intent(PebbleCenter.class.getName());
        inner_intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PEBBLE_TEST);
        LocalBroadcastManager.getInstance(context).sendBroadcast(inner_intent);
    }

    private Spanned test_FontBase_state(Context context){
        boolean result= PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.DATABASE_READY,false);
        return (result? greenText(R.string.setup_check_ok):redText(R.string.setup_check_bad));
    }

    private Spanned test_PebbleFirmware_state(Context context){
        PebbleKit.FirmwareVersionInfo info=PebbleKit.getWatchFWVersion(context);
      //  Constants.log("GetWatchVersion", info.getTag() + " major:"+ String.valueOf(info.getMajor())+ " minor:"+ String.valueOf(info.getMinor())+ " point:"+ String.valueOf(info.getPoint()));
        boolean result=false;
        if(info.getMajor()<(int)Constants.PEBBLE_FIRMWARE[0]){
            result= false;
        }else if(info.getMinor()<(int)Constants.PEBBLE_FIRMWARE[1]){
            result= false;
        }else if(info.getPoint()<(int)Constants.PEBBLE_FIRMWARE[2]){
            result= false;
        }else{
            result= true;
        }
        Spanned spText=Html.fromHtml(info.getTag()+'\t');
        return (Spanned)TextUtils.concat(spText,(result? greenText(R.string.setup_check_ok):redText(R.string.setup_check_result_need_update)));
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==3){
            if (resultCode==TextToSpeech.Engine.CHECK_VOICE_DATA_PASS){
                myTTS=new TextToSpeech(_context,this);



            }else{
                SpannableStringBuilder ssb=new SpannableStringBuilder();
                ssb.append(textInfo.getText());
                ssb.append(redText(R.string.setup_tts_failed));
                ssb.append('\n');
                textInfo.setText(ssb);
                svMyview.fullScroll(View.FOCUS_DOWN);
            }
        }
    }

    @Override
    public void onInit(int i) {
       if (i==TextToSpeech.SUCCESS){
           SpannableStringBuilder ssb=new SpannableStringBuilder();
           ssb.append(textInfo.getText());
           ssb.append(myTTS.getDefaultEngine()+ " ");
           ssb.append(greenText(R.string.setup_tts_default_locale));
           ssb.append(myTTS.getLanguage().getLanguage());
           ssb.append('\n');
           textInfo.setText(ssb);
           svMyview.fullScroll(View.FOCUS_DOWN);
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
