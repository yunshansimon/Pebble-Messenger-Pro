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

package com.yangtsaosoftware.pebblemessenger.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;

import com.yangtsaosoftware.pebblemessenger.Constants;
import com.yangtsaosoftware.pebblemessenger.services.MessageProcessingService;

/**
 * Created by yunshansimon on 15/3/17.
 */
public class SMSHandler extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sp= PreferenceManager.getDefaultSharedPreferences(context);
        String address,body;
        if (sp.getBoolean(Constants.PREFERENCE_SMS_ENABLE,true)){
            SmsMessage [] messages= Telephony.Sms.Intents.getMessagesFromIntent(intent);
            if (messages != null){
                address=messages[0].getOriginatingAddress();
                address=Constants.queryNameByNum(context,address);
                body=messages[0].getMessageBody();
                Intent inner_intent=new Intent(MessageProcessingService.class.getName());
                inner_intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_SMS_INCOMING);
                inner_intent.putExtra(Constants.BROADCAST_SMS_BODY,address + ":" + body);
                LocalBroadcastManager.getInstance(context).sendBroadcast(inner_intent);
            }

        }

    }


}
