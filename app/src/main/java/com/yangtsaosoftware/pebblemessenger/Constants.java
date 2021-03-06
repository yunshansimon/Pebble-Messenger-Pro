
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

package com.yangtsaosoftware.pebblemessenger;



import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.UUID;

public class Constants {
    private Constants() {
        throw new UnsupportedOperationException("This class is non-instantiable, so stop trying!");
    }
    // Accessibility specific items
    public static final String  ACCESSIBILITY_SERVICE                 = "com.yangtsaosoftware.pebblemessenger/com.yangtsaosoftware.pebblemessenger.services.NotificationService";
    //----------------------------------------

    //system
    public static final boolean IS_LOGGABLE                           = false;
    public static final String  DATABASE_READY                        = "status_database_ready";
    //----------------------------------------

    // Shared preferences
    public static final String  PREFERENCE_PACKAGE_LIST               = "pref_package_list";
    public static final String  PREFERENCE_NOTIF_SCREEN_ON            = "pref_notif_screen_on";
    public static final String  PREFERENCE_SMS_ENABLE                 = "pref_sms_enable";
    public static final String  PREFERENCE_MESSAGE_SCALE              = "pref_message_scale";
    public static final String  PREFERENCE_CALL_ENABLE                = "pref_call_enable";
    public static final String  PREFERENCE_CALL_QUIET                 = "pref_call_quiet";
    public static final String  PREFERENCE_CALL_SMS_SHORT             = "pref_call_sms_short";
    public static final String  PREFERENCE_CALL_SMS_LONG              = "pref_call_sms_long";
    public static final String  PREFERENCE_QUIET_HOURS                = "pref_dnd_time_enabled";
    public static final String  PREFERENCE_QUIET_HOURS_BEFORE         = "pref_dnd_time_before";
    public static final String  PREFERENCE_QUIET_HOURS_AFTER          = "pref_dnd_time_after";
    public static final String  PREFERENCE_MIN_NOTIFICATION_WAIT      = "pref_notif_timeout";
    public static final String  PREFERENCE_BLACK_BACKGROUND           = "pref_black_background";
    public static final String  PREFERENCE_READ_MESSAGE               = "pref_notif_readmessage";
    //----------------------------------------

    //two sets parameters of message characters
    public static final int MESSAGE_SCALE_SMALL=0;
    public static final int MESSAGE_SCALE_MID=1;
    public static final int MESSAGE_SCALE_LARGE=2;
//    public static final int MAX_PACKAGE_LENGTH=120;
    public static final int SMALL_LINE_CONTAIN_CHARS=16;
    public static final int SMALL_PAGE_CONTAIN_LINES=9;
    public static final int MID_LINE_CONTAIN_CHARS=12;
    public static final int MID_PAGE_CONTAIN_LINES=6;
    public static final int LARGE_LINE_CONTAIN_CHARS=9;
    public static final int LARGE_PAGE_CONTAIN_LINES=4;
    //-----------------------------------------

    public static final String BROADCAST_COMMAND="command";
    public static final int BROADCAST_PREFER_CHANGED=1;
    public static final int BROADCAST_CALL_INCOMING=2;
    public static final int BROADCAST_CALL_IDLE=3;
    public static final int BROADCAST_CALL_HOOK=4;
    public static final int BROADCAST_PEBBLE_TEST=5;
   // public static final int BROADCAST_PEBBLE_VERSION=6;
    public static final int BROADCAST_SMS_INCOMING=7;
   // public static final int BROADCAST_MMS_INCOMING=8;
    public static final String BROADCAST_PHONE_NUM="phonenum";
    public static final String BROADCAST_NAME="name";
    public static final String BROADCAST_SMS_BODY="body";
    public static final String BROADCAST_VERSION="version";
    public static final byte[] PEBBLE_VERSION={2,2,0};
    public static final byte[] PEBBLE_FIRMWARE={2,7,0};

    public static final String AUTHOR_EMAIL="yunshansimon@gmail.com";

    //Pebble app communication
    public static final UUID PEBBLE_UUID= UUID.fromString("2d0b18b1-0ee6-41ca-9579-a0c4c6568d93");
//    public static final String  INTENT_SEND_PEBBLE_NOTIFICATION       = "com.getpebble.action.SEND_NOTIFICATION";
    public static final String  PEBBLE_APP_URL="pebble://appstore/541f9df92969d09cc400006d";
    //Shared function
    public static void log(String tag, String message) {
        if (Constants.IS_LOGGABLE) {
            Log.i(tag, message);
        }
    }

    public static String queryNameByNum(Context context, String num) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(num));
        Cursor cursor = context.getContentResolver().query(uri, new String[] {
                ContactsContract.PhoneLookup.DISPLAY_NAME
        }, null, null, null);
        String nameString=context.getString(R.string.notificationservice_unknownperson);
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
