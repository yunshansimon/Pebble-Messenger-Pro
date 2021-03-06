
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

package com.yangtsaosoftware.pebblemessenger.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
// import android.text.format.Time;
import android.view.KeyEvent;

import com.android.internal.telephony.ITelephony;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import com.yangtsaosoftware.pebblemessenger.Constants;
import com.yangtsaosoftware.pebblemessenger.R;
import com.yangtsaosoftware.pebblemessenger.activities.SetupFragment;
import com.yangtsaosoftware.pebblemessenger.db.MessageDbHandler;
import com.yangtsaosoftware.pebblemessenger.models.CharacterMatrix;
import com.yangtsaosoftware.pebblemessenger.models.PebbleCall;
import com.yangtsaosoftware.pebblemessenger.models.PebbleMessage;

public class PebbleCenter extends Service {
    public final static int PEBBLE_SEND_MESSAGE=1;
    public final static int PEBBLE_SEND_CALL=2;

    public final static int PEBBLE_SEND_MESSAGE_TABLE=4;
    public final static int PEBBLE_SEND_CALL_TABLE=5;


    public boolean isPebbleEnable=true;

    public Messenger mPebbleCenterHandler;
    private static final int PREPARE_MESSAGE=1;
    private static final int PREPARE_CALL=2;
    private static final int PREPARE_MESSAGE_TABLE=3;
    private static final int PREPARE_CALL_TABLE=4;
    private static final int PREPARE_TEST=5;



    private static final int SEND_MESSAGE=1;
    private static final int SEND_NEXT_PAGE=2;
    private static final int SEND_CONTINUE=3;
    private static final int SEND_CALL=4;
    private static final int SEND_CALL_END=5;
    private static final int SEND_CLOSE_APP=6;
    private static final int SEND_CALL_HOOK=7;
    private static final int SEND_OPEN_APP=8;


    private static final int TRANS_ID_COMMON=0;
    private static final int TRANS_ID_END=1;
    private static final int TRANS_ID_EMPTY=13;
    private static final int TRANS_ID_TEST=99;

 //   private int fChars;  //line contain chars
    private int fLines;  //page contain lines
 //   private int fLength; //package max length
    private byte char_scale;
    private Long timeOut;
    private boolean callEnable=true;
    private boolean whiteBackground=true;
    private String sms1;
    private String sms2;


    private Deque<PebbleDictionary> sendQueue;
//    private Deque<PebbleMessage> waitQueue;
//    private boolean pebbleBusy=false;
 //   private Time busyBegin;
 //   private boolean need_delay=true;
    //private final static long MAX_WAITING_MILLIS=30000;
    private final static int MAX_CHARS_PACKAGE_CONTAIN=60;
 //   private int appStatue=0;
    //pebble request. Use transaction id to receive command, and extra data for the addition information.
    private static final int REQUEST_TRANSID_MESSAGE=1;
    private static final int REQUEST_TRANSID_CALL=2;
    private static final int REQUEST_TRANSID_MESSAGE_TABLE=3;
    private static final int REQUEST_TRANSID_CALL_TABLE=4;
    private static final int REQUEST_TRANSID_CLOSE_APP=5;
    private static final int REQUEST_TRANSID_PICKUP_PHONE=6;
    private static final int REQUEST_TRANSID_HANGOFF_PHONE=7;
    private static final int REQUEST_TRANSID_HANGOFF_SMS1=8;
    private static final int REQUEST_TRANSID_HANGOFF_SMS2=9;
    private static final int REQUEST_TRANSID_NEXTPAGE=10;
    private static final int REQUEST_TRANSID_READ_NOTIFY=11;
    private static final int REQUEST_TRANSID_IM_FREE=12;
    private static final int REQUEST_TRANSID_VERSION=13;
    private static final int ID_EXTRA_DATA=2;
    private static final int REQUEST_EXTRA_SPEAKER_ON=1;
    private static final int REQUEST_EXTRA_SPEAKER_OFF=2;
 //   private static final int REQUEST_EXTRA_DELAY_OFF=0;
    private static final int REQUEST_EXTRA_DELAY_ON=1;
    private static final int ID_EXTRA_DATA2=3;



    //pebble command
    private static final int ID_COMMAND=0;
    private static final byte REMOTE_EMPTY=0;
    private static final byte REMOTE_EXCUTE_NEW_MESSAGE=1;
    private static final byte REMOTE_EXCUTE_NEW_CALL=2;
    private static final byte REMOTE_EXCUTE_CONTINUE_MESSAGE=3;
    private static final byte REMOTE_EXCUTE_CONTINUE_CALL=4;
    private static final byte REMOTE_DISPLAY_MESSAGE_TABLE=5;
    private static final byte REMOTE_DISPLAY_CALL_TABLE=6;
    private static final byte REMOTE_EXCUTE_CALL_END=7;
    private static final byte REMOTE_DISPLAY_CONTINUE=8;
    private static final byte REMOTE_EXCUTE_CALL_HOOK=9;
    private static final byte REMOTE_EXCUTE_TEST=10;
  //  private static final int ID_TOTAL_PAGES=1;
    private static final int ID_PAGE_INFO=1;
  //  private static final int ID_TOTAL_PACKAGES=3;
  //  private static final int ID_PACKAGE_NUM=4;
    /*
    ID_PAGE_INFO contain a byte array with 4 elements:
    index 0: total pages
    index 1: current pages number
    index 2: total packages
    index 3: current package number
     */
    private static final int ID_ASCSTR=5;
    private static final int ID_UNICHR_BYTES=6;
    private static final int ID_UNICHR_INFO=7;
    /*
    ID_UNICHAR_INFO contain a byte array with 3 elements:
    index 0: unichar width (1 for one byte char 2 for two byte char)
    index 1: unichar position X(row index);
    index 2: unichar position Y(colon index);
     */
 //   private static final int ID_UNICHR_POS=8;
    private static final int ID_CLOSE_DELAY_SEC=9;
    private static final int ID_CHAR_SCALE=10;
    private static final int ID_INFO_ID=11;
    private static final int ID_PHONE_NUM=12;
    private static final int ID_WHITE_BACKGROUND=13;
    private static final int ID_EXTRA_POS_NUM=100;


    private static final String TAG_NAME="PebbleCenter";

    private Handler prepareThreadHandler;
    private Handler sendMsgThreadHandler;
    private boolean send_full_page=true;

    private boolean send_test_get_return=false;

    private Context _contex;
    private Messenger rMessageProcessingHandler=null;

    private static Lock sendLock=new ReentrantLock();

    private ServiceConnection connToMessageProcessing=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            rMessageProcessingHandler=new Messenger(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            rMessageProcessingHandler=null;
        }
    };

    public PebbleCenter() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Handler pebbleCenterHandler = new PebbleCenterHandler();
        mPebbleCenterHandler=new Messenger(pebbleCenterHandler);
        Constants.log(TAG_NAME,"Create PebbleCenter Messenger.");
        loadPref();
        _contex=this;
     //   busyBegin=new Time();
//        waitQueue=new ArrayDeque<PebbleMessage>();
        sendQueue=new ArrayDeque<PebbleDictionary>();
        bindService(new Intent(this, MessageProcessingService.class), connToMessageProcessing,
                Context.BIND_AUTO_CREATE);
        Thread prepareThread = new PrepareThread();
        prepareThread.start();
        Thread sendMsgThread = new SendMsgThread();
        sendMsgThread.start();
        isPebbleEnable=PebbleKit.isWatchConnected(_contex);
        PebbleKit.registerReceivedDataHandler(_contex,new PebbleKit.PebbleDataReceiver(Constants.PEBBLE_UUID) {
            @Override
            public void receiveData(Context context, int transactionId, PebbleDictionary data) {
                PebbleKit.sendAckToPebble(_contex, transactionId);
      //          appStatue++;
                Constants.log(TAG_NAME,"Received data form pebble");
                switch (data.getUnsignedIntegerAsLong(ID_COMMAND).intValue()){
                    case REQUEST_TRANSID_CALL_TABLE: {
                        Constants.log(TAG_NAME,"Request call table.");
                        clean_SendQue();
                        Message msg = Message.obtain();
                        msg.what = MessageProcessingService.MSG_GET_CALL_TABLE;
                        try {
                            rMessageProcessingHandler.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                        break;
                    case REQUEST_TRANSID_MESSAGE_TABLE: {
                        Constants.log(TAG_NAME,"Request message table.");
                        clean_SendQue();
                        Message msg = Message.obtain();
                        msg.what = MessageProcessingService.MSG_GET_MESSAGE_TABLE;
                        try {
                            rMessageProcessingHandler.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                        break;
                    case REQUEST_TRANSID_CALL:
                    {
                        clean_SendQue();
                        Message msg=Message.obtain();
                        msg.what=MessageProcessingService.MSG_GET_CALL;
                        Bundle b=new Bundle();
                        b.putString(MessageDbHandler.COL_CALL_ID, data.getString(ID_EXTRA_DATA));
                        Constants.log(TAG_NAME,"Request call id:"+ data.getString(ID_EXTRA_DATA));

                        msg.setData(b);
                        try {
                            rMessageProcessingHandler.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

                    }
                        break;
                    case REQUEST_TRANSID_MESSAGE:
                        clean_SendQue();
                        Message msg=Message.obtain();
                        msg.what=MessageProcessingService.MSG_GET_MESSAGE;
                        Bundle b=new Bundle();
                        b.putString(MessageDbHandler.COL_MESSAGE_ID, data.getString(ID_EXTRA_DATA));
                        Constants.log(TAG_NAME,"Request message id:"+ data.getString(ID_EXTRA_DATA));

                        msg.setData(b);
                        try {
                            rMessageProcessingHandler.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                    case REQUEST_TRANSID_PICKUP_PHONE:
                        TelephonyManager telMag = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                        Constants.log("Receivephone","Receive phone:"+ data.getString(ID_EXTRA_DATA2));
                        if (telMag.getCallState()==TelephonyManager.CALL_STATE_RINGING){
                            switch (data.getUnsignedIntegerAsLong(ID_EXTRA_DATA).intValue()) {
                                case REQUEST_EXTRA_SPEAKER_ON:
                                    answerCall(true);
                                    break;
                                case REQUEST_EXTRA_SPEAKER_OFF:
                                    answerCall(false);
                                    break;
                            }
                        }else{
                            switch (data.getUnsignedIntegerAsLong(ID_EXTRA_DATA).intValue()) {
                                case REQUEST_EXTRA_SPEAKER_ON:
                                    dialNumber(data.getString(ID_EXTRA_DATA2), true);
                                    break;
                                case REQUEST_EXTRA_SPEAKER_OFF:
                                    dialNumber(data.getString(ID_EXTRA_DATA2), false);
                                    break;
                            }

                        }
        //                pebbleBusy = false;
                        break;
                    case REQUEST_TRANSID_HANGOFF_PHONE:
                        endCall();
        //                pebbleBusy=false;

                        break;
                    case REQUEST_TRANSID_HANGOFF_SMS1:
                        Constants.log(TAG_NAME,"Request hangoff and send sms1");

                        endCall();
                        doSendSMSTo(data.getString(ID_EXTRA_DATA),sms1);
         //               pebbleBusy=false;

                        break;
                    case REQUEST_TRANSID_HANGOFF_SMS2:
                        Constants.log(TAG_NAME,"Request hangoff and send sms2");

                        endCall();
                        doSendSMSTo(data.getString(ID_EXTRA_DATA),sms2);
          //              pebbleBusy=false;

                        break;
                    case REQUEST_TRANSID_CLOSE_APP:
                        Constants.log(TAG_NAME,"Request close app command.");
                        sendMsgThreadHandler.sendEmptyMessage(SEND_CLOSE_APP);
         //               need_delay=true;

                        break;
                    case REQUEST_TRANSID_NEXTPAGE:
                        Constants.log(TAG_NAME,"Request send next page.");

                        sendMsgThreadHandler.sendEmptyMessage(SEND_NEXT_PAGE);
                        break;
                    case REQUEST_TRANSID_READ_NOTIFY:
                    {
                        Constants.log(TAG_NAME,"Request  read msg");
                        Message read_msg=Message.obtain();
                        read_msg.what=MessageProcessingService.MSG_READ;
                        Bundle bd=new Bundle();
                        bd.putString(MessageDbHandler.COL_MESSAGE_ID,data.getUnsignedIntegerAsLong(ID_EXTRA_DATA).toString());
                        read_msg.setData(bd);
                        try {
                            rMessageProcessingHandler.send(read_msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                        break;
                    case REQUEST_TRANSID_IM_FREE:
                        Constants.log(TAG_NAME,"Request pebble app is free to receive data.");

         //               need_delay = data.getUnsignedInteger(ID_EXTRA_DATA).intValue() == REQUEST_EXTRA_DELAY_ON ;
         //               clean_SendQue();
                        break;
                    case REQUEST_TRANSID_VERSION:
                    {
                        send_test_get_return=true;
                        String result=data.getString(ID_EXTRA_DATA);
                        Constants.log("PmpVersion",result);
                        StringTokenizer tokens=new StringTokenizer(result,".");

                        Intent inner_intent = new Intent(SetupFragment.class.getName());
                        inner_intent.putExtra(Constants.BROADCAST_VERSION, new byte[]{Byte.parseByte(tokens.nextToken()), Byte.parseByte(tokens.nextToken()), Byte.parseByte(tokens.nextToken())});
                        LocalBroadcastManager.getInstance(context).sendBroadcast(inner_intent);
                        sendMsgThreadHandler.sendEmptyMessage(SEND_CLOSE_APP);
                        break;
                    }
                }
            }
        });

        PebbleKit.registerReceivedAckHandler(_contex,new PebbleKit.PebbleAckReceiver(Constants.PEBBLE_UUID) {
            @Override
            public void receiveAck(Context context, int transactionId) {
                Constants.log(TAG_NAME,"Get a receiveAck:" + String.valueOf(transactionId));
                switch (transactionId){
                    case TRANS_ID_COMMON:
                        Constants.log(TAG_NAME,"Send continue...");
      //                  pebbleBusy=true;
                        sendMsgThreadHandler.sendEmptyMessage(SEND_CONTINUE);
                        break;
                    case TRANS_ID_END:
                        send_full_page=true;
                        break;
                    case TRANS_ID_EMPTY:
       //                 pebbleBusy=true;
                        sendMsgThreadHandler.sendEmptyMessage(SEND_CONTINUE);
                        break;
                    case TRANS_ID_TEST: {
                       break;
                    }
                }
            }
        });

        PebbleKit.registerReceivedNackHandler(_contex, new PebbleKit.PebbleNackReceiver(Constants.PEBBLE_UUID) {
            @Override
            public void receiveNack(Context context, int transactionId) {
                Constants.log(TAG_NAME,"Get a receivedNack:" + String.valueOf(transactionId));
                if (PebbleKit.isWatchConnected(_contex)){
                    switch (transactionId){
                        case TRANS_ID_COMMON:
                            sendMsgThreadHandler.sendEmptyMessage(SEND_CONTINUE);
                            break;
                        case TRANS_ID_END:
                            send_full_page=true;
                            break;
                        case TRANS_ID_EMPTY:
         //                   appStatue=0;
                            sendMsgThreadHandler.sendEmptyMessage(SEND_OPEN_APP);
                            break;
                        case TRANS_ID_TEST: {
                            Intent inner_intent = new Intent(SetupFragment.class.getName());
                            inner_intent.putExtra(Constants.BROADCAST_VERSION,new byte[]{0,0,0});
                            LocalBroadcastManager.getInstance(context).sendBroadcast(inner_intent);
                        }
                    }
                }else{
                    sendMsgThreadHandler.sendEmptyMessage(SEND_CLOSE_APP);
                }

            }
        });

        PebbleKit.registerPebbleConnectedReceiver(_contex,new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                isPebbleEnable=true;
            }
        });

        PebbleKit.registerPebbleDisconnectedReceiver(_contex, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                isPebbleEnable=false;
                sendMsgThreadHandler.sendEmptyMessage(SEND_CLOSE_APP);
            }
        });

        BroadcastReceiver br= new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                int command=intent.getIntExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_PREFER_CHANGED);

                switch (command){
                    case Constants.BROADCAST_PREFER_CHANGED:
                        loadPref();
                        break;
                    case Constants.BROADCAST_CALL_IDLE:
                        if(callEnable) {
                            sendMsgThreadHandler.sendEmptyMessage(SEND_CALL_END);
                        }
                        break;
                    case Constants.BROADCAST_CALL_HOOK:
                        if(callEnable) {
                            sendMsgThreadHandler.sendEmptyMessage(SEND_CALL_HOOK);
                        }
                        break;
                    case Constants.BROADCAST_PEBBLE_TEST:
                        if(isPebbleEnable) {
                            prepareThreadHandler.sendEmptyMessage(PREPARE_TEST);
                        }
                        break;
                }
            }
        };
        IntentFilter intentFilter=new IntentFilter(PebbleCenter.class.getName());
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(br,intentFilter);
    }

    @Override
    public void onDestroy() {
        unbindService(connToMessageProcessing);
        super.onDestroy();
    }

    class PebbleCenterHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            Constants.log(TAG_NAME,"Get a msg. Pebble is " + String.valueOf(isPebbleEnable) + " what:" + String.valueOf(msg.what));
            if(!isPebbleEnable) return;
            switch (msg.what){
                case PEBBLE_SEND_MESSAGE: {
  //                  waitQueue.add((PebbleMessage) msg.getData().getSerializable(MessageProcessingService.PROCEED_MSG));
                    Message pebbleMsg=prepareThreadHandler.obtainMessage(PREPARE_MESSAGE);
                    pebbleMsg.setData(msg.getData());
                    prepareThreadHandler.sendMessage(pebbleMsg);
                }
                    break;
                case PEBBLE_SEND_CALL:{
                    if (callEnable == false) return;
                    Message callMsg=prepareThreadHandler.obtainMessage(PREPARE_CALL);
                    callMsg.setData(msg.getData());
                    prepareThreadHandler.sendMessageAtFrontOfQueue(callMsg);
                }
                    break;
                case PEBBLE_SEND_MESSAGE_TABLE:{
                    Message ptMsg=prepareThreadHandler.obtainMessage(PREPARE_MESSAGE_TABLE);
                    ptMsg.setData(msg.getData());
                    prepareThreadHandler.sendMessage(ptMsg);
                }
                    break;
                case PEBBLE_SEND_CALL_TABLE:{
                    Message ctMsg=prepareThreadHandler.obtainMessage(PREPARE_CALL_TABLE);
                    ctMsg.setData(msg.getData());
                    prepareThreadHandler.sendMessage(ctMsg);
                }
                break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        //throw new UnsupportedOperationException("Not yet implemented");
        return mPebbleCenterHandler.getBinder();
    }

    class PrepareThread extends Thread{
        @Override
        public void run() {
            Looper.prepare();
            prepareThreadHandler=new PrepareHandler(Looper.myLooper());
            Looper.loop();
        }
    }

    class PrepareHandler extends Handler{
        public PrepareHandler (Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            //super.handleMessage(msg);
            if(!isPebbleEnable){
                return;
            }

            switch (msg.what){
                case PREPARE_MESSAGE:{

                    if(!send_full_page){
                        Constants.log("PREPARE","pebble is busy.");
                        Message pmMsg=this.obtainMessage(PREPARE_MESSAGE);
                        pmMsg.setData(msg.getData());
                        this.sendMessageDelayed(pmMsg,3000);
                        return;
                    }
                    clean_SendQue();
                    List<PebbleMessage> pages=splitPages((PebbleMessage) msg.getData().getSerializable(MessageProcessingService.PROCEED_MSG));
                    split_message_to_packages_add_to_sendQue(pages);
                    sendMsgThreadHandler.sendEmptyMessage(SEND_MESSAGE);
                }
                    break;
                case PREPARE_CALL:
                    if(!send_full_page){
                        Message pcMsg=this.obtainMessage(PREPARE_CALL);
                        pcMsg.setData(msg.getData());
                        this.sendMessageDelayed(pcMsg,1000);
                        return;
                    }
                    PebbleCall pbCall=(PebbleCall) msg.getData().getSerializable(MessageProcessingService.PROCEED_CALL);
                    split_call_to_package_add_to_sendQue(pbCall);
                    sendMsgThreadHandler.sendEmptyMessage(SEND_CALL);
                    break;
                case PREPARE_MESSAGE_TABLE:
                    clean_SendQue();
                    split_string_to_package_add_to_sendQue(msg.getData().getString(MessageDbHandler.TABLE_MESSAGE_NAME), REMOTE_DISPLAY_MESSAGE_TABLE);
                    sendMsgThreadHandler.sendEmptyMessage(SEND_CONTINUE);
                    break;
                case PREPARE_CALL_TABLE:
                    clean_SendQue();
                    split_string_to_package_add_to_sendQue(msg.getData().getString(MessageDbHandler.TABLE_CALL_NAME), REMOTE_DISPLAY_CALL_TABLE);
                    sendMsgThreadHandler.sendEmptyMessage(SEND_CONTINUE);
                    break;
                case PREPARE_TEST: {
                    if(!send_full_page){
                        this.sendEmptyMessageDelayed(PREPARE_TEST,3000);
                        return;
                    }
                    clean_SendQue();
                    PebbleDictionary testCmd=new PebbleDictionary();
                    testCmd.addUint8(ID_COMMAND, REMOTE_EXCUTE_TEST);
                    sendQueue.add(testCmd);
                    sendMsgThreadHandler.sendEmptyMessage(SEND_MESSAGE);
                    send_test_get_return=false;
                    this.postDelayed(check_test_return,3000);
                }
                    break;
            }



        }

        Runnable check_test_return=new Runnable() {
            @Override
            public void run() {
                //         appStatue=0;
                if(!send_test_get_return) {
                    Intent inner_intent = new Intent(SetupFragment.class.getName());
                    inner_intent.putExtra(Constants.BROADCAST_VERSION, new byte[]{0, 0, 0});
                    LocalBroadcastManager.getInstance(_contex).sendBroadcast(inner_intent);
                }
            }
        };


    }
    private void split_string_to_package_add_to_sendQue(String strMsg, byte commandID){
        sendLock.lock();
        PebbleDictionary dataMsg;

        byte totalPackges=(byte) bigInt((float)strMsg.length()/(float)MAX_CHARS_PACKAGE_CONTAIN);
        Constants.log(TAG_NAME,"Send Table:\n" + strMsg + "\ntotalpackages:" + String.valueOf(totalPackges)+ " strlen:" + String.valueOf(strMsg.length()));
        for (int pg=1;pg<=totalPackges;pg++){
            dataMsg=new PebbleDictionary();
            if (pg==1){
                dataMsg.addUint8(ID_COMMAND,commandID);
            }else{
                dataMsg.addUint8(ID_COMMAND,REMOTE_DISPLAY_CONTINUE);
            }
            byte [] pageInfo=new byte[4];
            pageInfo[0]=1;
            pageInfo[1]=1;
            pageInfo[2]=totalPackges;
            pageInfo[3]=(byte)pg;
            dataMsg.addBytes(ID_PAGE_INFO,pageInfo);
            dataMsg.addString(ID_ASCSTR, strMsg.substring((pg - 1) * MAX_CHARS_PACKAGE_CONTAIN,
                    (pg * MAX_CHARS_PACKAGE_CONTAIN > strMsg.length() ? strMsg.length() : pg * MAX_CHARS_PACKAGE_CONTAIN)
            ));

            sendQueue.add(dataMsg);

        }
        sendLock.unlock();
    }

    private List<PebbleMessage> splitPages(PebbleMessage tmpPM){
        List<PebbleMessage> pmPages=new ArrayList<PebbleMessage>();
        String[] splitString= tmpPM.getAscMsg().split("\n");
        Constants.log(TAG_NAME,tmpPM.getAscMsg());
        for(String str:splitString){
            Constants.log(TAG_NAME,"splitPages, deal:" + str);
        }
        int pageCount=bigInt((float)splitString.length /(float) fLines);
        for (int page=1;page<=pageCount;page++){
            PebbleMessage itemPm=new PebbleMessage();
            itemPm.set_id(tmpPM.get_id());
            StringBuilder tmpSB=new StringBuilder();
            for (int line=(page-1)*fLines;line<(page*fLines> splitString.length? splitString.length:page*fLines);line++){
                tmpSB.append(splitString[line]);
                tmpSB.append('\n');
            }
            itemPm.setAscMsg(tmpSB.toString());
            Deque<CharacterMatrix> itemDqCM=new ArrayDeque<CharacterMatrix>();
            while(!tmpPM.getCharacterQueue().isEmpty()){
                CharacterMatrix tmpCM=tmpPM.getCharacterQueue().pollFirst();
                if(tmpCM.getPos()[0]>=(page-1)*fLines && tmpCM.getPos()[0]<=page*fLines){
                    itemDqCM.add(tmpCM);
                }else{
                    tmpPM.getCharacterQueue().addFirst(tmpCM);
                    break;
                }
            }
            itemPm.setCharacterQueue(itemDqCM);
            pmPages.add(page-1,itemPm);
        }
        return pmPages;
    }

    private void split_message_to_packages_add_to_sendQue(List<PebbleMessage> listPM){
        sendLock.lock();
        PebbleDictionary dataMsg=new PebbleDictionary();
        dataMsg.addUint8(ID_COMMAND,REMOTE_EXCUTE_NEW_MESSAGE);

        dataMsg.addUint8(ID_CLOSE_DELAY_SEC, (byte) (timeOut / 1000));
        dataMsg.addUint8(ID_CHAR_SCALE,char_scale);
        Constants.log(TAG_NAME,"add char_scale:" + String.valueOf(char_scale));
        dataMsg.addUint32(ID_INFO_ID, listPM.get(0).get_id().intValue());
        byte[]packInfo=new byte[]{1,1,2,1};
        dataMsg.addBytes(ID_PAGE_INFO,packInfo);
        dataMsg.addUint8(ID_WHITE_BACKGROUND,whiteBackground?(byte)1:(byte)0);
        sendQueue.add(dataMsg);
        HashMap  unicodeMap=new HashMap();

        Constants.log(TAG_NAME,"listpm.size:"+ String.valueOf(listPM.size()));
        for(int page=1;page<=listPM.size();page++){
            PebbleMessage dealPM=listPM.get(page-1);
            int strPackages=bigInt((float)dealPM.getAscMsg().length()/(float)MAX_CHARS_PACKAGE_CONTAIN);
            byte totalPackages=(byte) (dealPM.getCharacterQueue().size()+strPackages);
            Constants.log(TAG_NAME,"total Packages:" + String.valueOf(totalPackages));
            for (int pg=1;pg<=strPackages;pg++){
                dataMsg=new PebbleDictionary();
                dataMsg.addUint8(ID_COMMAND,REMOTE_EXCUTE_CONTINUE_MESSAGE);
                byte[] strPackInfo=new byte[]{(byte)listPM.size(),(byte)page,0,0};
                dataMsg.addBytes(ID_PAGE_INFO,strPackInfo);
         //       dataMsg.addUint8(ID_TOTAL_PAGES,(byte)listPM.size());
         //       dataMsg.addUint8(ID_PAGE_NUM,(byte)page);
         //       dataMsg.addUint8(ID_TOTAL_PACKAGES,totalPackages);
         //       dataMsg.addUint8(ID_PACKAGE_NUM,(byte) pg);
                dataMsg.addString(ID_ASCSTR,dealPM.getAscMsg().substring((pg-1)*MAX_CHARS_PACKAGE_CONTAIN,
                        (pg*MAX_CHARS_PACKAGE_CONTAIN> dealPM.getAscMsg().length()? (dealPM.getAscMsg().length()) : (pg*MAX_CHARS_PACKAGE_CONTAIN))
                ));
                Constants.log(TAG_NAME,"Add Queue a strmsg:<" + dataMsg.getString(ID_ASCSTR) +">");
                unicodeMap.put(pg,dataMsg);
            }
            for (int pg=strPackages+1;pg<=totalPackages;pg++){
                CharacterMatrix cm=dealPM.getCharacterQueue().pollFirst();
                PebbleDictionary duoPD=(PebbleDictionary)unicodeMap.get(cm.getCode());
                if (duoPD==null){
                    dataMsg=new PebbleDictionary();
                    dataMsg.addUint8(ID_COMMAND,REMOTE_EXCUTE_CONTINUE_MESSAGE);
                    byte[] uniPackInfo=new byte[]{(byte)listPM.size(),(byte)page,0,0};
                    dataMsg.addBytes(ID_PAGE_INFO,uniPackInfo);
                   // dataMsg.addUint8(ID_TOTAL_PAGES,(byte)listPM.size());
                   // dataMsg.addUint8(ID_PAGE_NUM,(byte)page);
                    //        dataMsg.addUint8(ID_TOTAL_PACKAGES,totalPackages);
                    //dataMsg.addUint8(ID_PACKAGE_NUM,(byte) pg);
                    Constants.log(TAG_NAME,"There are " + String.valueOf(dealPM.getCharacterQueue().size()) + "unicode in queue");
                    int size = cm.getByteList().size();
                    byte[] b2 = new byte[size];
                    cm.getbyteArray(b2,size);
                    byte[] uniInfo=new byte[]{(byte) cm.getWidthBytes(),cm.getPos()[0],cm.getPos()[1]};
                    dataMsg.addBytes(ID_UNICHR_INFO,uniInfo);
                    //dataMsg.addUint8(ID_UNICHR_WIDTH,(byte) cm.getWidthBytes());
                    //dataMsg.addBytes(ID_UNICHR_POS,cm.getPos());
                    Constants.log(TAG_NAME,"row:" + String.valueOf(cm.getPos()[0]) + " col:" + String.valueOf(cm.getPos()[1]));
                    dataMsg.addBytes(ID_UNICHR_BYTES, b2);
                    Constants.log(TAG_NAME,"b2 length:" + String.valueOf(b2.length));
            //        Constants.log(TAG_NAME,"Add Queue a unimsg:" + dataMsg.getUnsignedInteger(ID_PACKAGE_NUM).toString());
                    unicodeMap.put(cm.getCode(),dataMsg);
                }else{
                    if(duoPD.contains(ID_EXTRA_POS_NUM)) {
                        byte [] soubyte=duoPD.getBytes(ID_EXTRA_POS_NUM);
                        byte num=soubyte[0];
                        byte [] tarbyte=new byte[num+2];
                        tarbyte[0]= (byte) (num+1);
                        for (int i=1;i<=num;i++){
                            tarbyte[i]=soubyte[i];
                        }
                        tarbyte[num+1] |=(cm.getPos()[1]-1)<<4;
                        tarbyte[num+1] |=(cm.getPos()[0]-1);
                        duoPD.addBytes(ID_EXTRA_POS_NUM,tarbyte);

                    }else{
                        byte[] tmpbyte=new byte[2];
                        tmpbyte[0]=1;
                        tmpbyte[1] |=(cm.getPos()[1]-1)<<4;
                        tmpbyte[1] |=(cm.getPos()[0]-1);
                        duoPD.addBytes(ID_EXTRA_POS_NUM,tmpbyte);
                    }
                }

            }

            totalPackages=(byte)unicodeMap.size();
            Iterator it=unicodeMap.keySet().iterator();
            int i=0;
            while(it.hasNext()){
                PebbleDictionary tmpPD=(PebbleDictionary)unicodeMap.get(it.next());
                byte[] tmpPackInfo=tmpPD.getBytes(ID_PAGE_INFO);
                tmpPackInfo[2]=totalPackages;
                tmpPackInfo[3]=(byte)++i;
                tmpPD.addBytes(ID_PAGE_INFO,tmpPackInfo);
                //tmpPD.addUint8(ID_TOTAL_PACKAGES, totalPackages);
                //tmpPD.addUint8(ID_PACKAGE_NUM,(byte)++i);
                sendQueue.add(tmpPD);
            }
            unicodeMap.clear();
        }
        sendLock.unlock();
    }

    private void split_call_to_package_add_to_sendQue(PebbleCall pbCall){
        sendLock.lock();
        byte totalPackages=(byte) (pbCall.getCharacterQueue().size() +1);
        PebbleDictionary dataMsg;
        for (int pg=totalPackages;pg>1;pg--){
            dataMsg=new PebbleDictionary();
            dataMsg.addUint8(ID_COMMAND,REMOTE_EXCUTE_CONTINUE_CALL);
            byte[] packInfo=new byte[]{1,1,totalPackages,(byte)pg};
            dataMsg.addBytes(ID_PAGE_INFO,packInfo);
         //   dataMsg.addUint8(ID_TOTAL_PACKAGES,totalPackages);
         //   dataMsg.addUint8(ID_PACKAGE_NUM,(byte) pg);
            CharacterMatrix cm= pbCall.getCharacterQueue().pollLast();
            int size=cm.getByteList().size();
            byte[] b2=new byte[size];
            cm.getbyteArray(b2,size);
            dataMsg.addBytes(ID_UNICHR_BYTES, b2);
            byte[] uniInfo=new byte[]{(byte) cm.getWidthBytes(),cm.getPos()[0],cm.getPos()[1]};
            dataMsg.addBytes(ID_UNICHR_INFO,uniInfo);
        //    dataMsg.addUint8(ID_UNICHR_WIDTH,(byte) cm.getWidthBytes());
        //    dataMsg.addBytes(ID_UNICHR_POS,cm.getPos());
            sendQueue.addFirst(dataMsg);
        }
        dataMsg=new PebbleDictionary();
        dataMsg.addUint8(ID_COMMAND,REMOTE_EXCUTE_NEW_CALL);
        byte[] packInfo=new byte[]{1,1,totalPackages,1};
        dataMsg.addBytes(ID_PAGE_INFO,packInfo);
        //dataMsg.addUint8(ID_TOTAL_PACKAGES,totalPackages);
        //dataMsg.addUint8(ID_PACKAGE_NUM, (byte) 1);
        dataMsg.addString(ID_ASCSTR, pbCall.getAscMsg());
        dataMsg.addString(ID_PHONE_NUM,pbCall.getPhoneNum());
        dataMsg.addUint32(ID_INFO_ID, pbCall.get_id().intValue());
        dataMsg.addUint8(ID_WHITE_BACKGROUND,whiteBackground?(byte)1:(byte)0);
        sendQueue.addFirst(dataMsg);
        sendLock.unlock();
    }

    private int bigInt(float d){
        Constants.log(TAG_NAME,"get big int:" + String.valueOf(d));
        if((d- ((int)d))>0.01){
            return (int)d +1;
        }else{
            return (int)d;
        }
    }

    private void clean_SendQue(){

        sendQueue.clear();
        send_full_page=true;
        //pebbleBusy=false;
    }

    class SendMsgThread extends Thread{
        @Override
        public void run() {
            Looper.prepare();
            sendMsgThreadHandler=new SendMsgHandler(Looper.myLooper());
            Looper.loop();
        }
    }
    class SendMsgHandler extends Handler{
        public SendMsgHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Constants.log(TAG_NAME,"get send command: what:" + String.valueOf(msg.what));
            if(!isPebbleEnable){
                clean_SendQue();
                Constants.log(TAG_NAME,"Pebble is disable, clean!");
                return;
            }
            switch (msg.what){
                case SEND_MESSAGE:
                    this.post(sendEmpty);
                    break;
                case SEND_CONTINUE:
                {
     //               if(appStatue==0) clean_SendQue();
                    if(sendQueue.isEmpty()) {
                        Constants.log(TAG_NAME,"sendQueue is empty! Can not send");
                        return;
                    }
                    this.post(sendToPebble);
                }
                    break;
                case SEND_NEXT_PAGE: {
       //             if(appStatue==0) clean_SendQue();
                    if(sendQueue.isEmpty()) {
                        Constants.log(TAG_NAME,"sendQueue is empty! Can not send");
                        return;
                    }
                    this.post(sendToPebble);

                }
                    break;
                case SEND_CALL:
                    this.post(sendEmpty);
                    break;
                case SEND_CALL_END:
                {
      //              if(appStatue==0) return;
                    sendLock.lock();
                    PebbleDictionary callEnd=new PebbleDictionary();
                    byte[] packInfo=new byte[]{1,1,1,1};
                    callEnd.addBytes(ID_PAGE_INFO,packInfo);
                   // callEnd.addUint8(ID_TOTAL_PACKAGES,(byte)1);
                   // callEnd.addUint8(ID_PACKAGE_NUM,(byte)1);
                    callEnd.addUint8(ID_COMMAND,REMOTE_EXCUTE_CALL_END);
                    sendQueue.addFirst(callEnd);
                    this.postAtFrontOfQueue(sendToPebble);
                    sendLock.unlock();
                }
                    break;
                case SEND_CLOSE_APP:
                    this.postAtFrontOfQueue(closeApp);
                    break;
                case SEND_CALL_HOOK:
      //              if(appStatue==0) return;
                    sendLock.lock();
                    PebbleDictionary callHook=new PebbleDictionary();
                    byte[] packInfo=new byte[]{1,1,1,1};
                    callHook.addBytes(ID_PAGE_INFO,packInfo);
                    //callHook.addUint8(ID_TOTAL_PACKAGES,(byte)1);
                    //callHook.addUint8(ID_PACKAGE_NUM,(byte)1);
                    callHook.addUint8(ID_COMMAND,REMOTE_EXCUTE_CALL_HOOK);
                    sendQueue.addFirst(callHook);
                    this.postAtFrontOfQueue(sendToPebble);
                    sendLock.unlock();
                    break;
                case SEND_OPEN_APP:
                    wait_for_open_pebble_app();
                    this.post(sendToPebble);
                    break;

            }

        }

        void wait_for_open_pebble_app(){

                PebbleKit.startAppOnPebble(_contex, Constants.PEBBLE_UUID);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

        }

    }

    Runnable sendToPebble=new Runnable() {
        @Override
        public void run() {
            Constants.log(TAG_NAME,"Data send to pebble, sendQueue length:" + String.valueOf(sendQueue.size()));
            if (sendQueue.size()==0){
                return;
            }
            PebbleDictionary tmpPD=sendQueue.poll();
            if (tmpPD.getUnsignedIntegerAsLong(ID_COMMAND)==REMOTE_EXCUTE_TEST){
                PebbleKit.sendDataToPebbleWithTransactionId(_contex, Constants.PEBBLE_UUID, tmpPD, TRANS_ID_TEST);
                return;
            }
            byte[] tmpPackInfo=tmpPD.getBytes(ID_PAGE_INFO);
            if (tmpPackInfo[3] == tmpPackInfo[2]) {
                PebbleKit.sendDataToPebbleWithTransactionId(_contex, Constants.PEBBLE_UUID, tmpPD, TRANS_ID_END);

                Constants.log(TAG_NAME,"Send last package.sendQueue length:" + String.valueOf(sendQueue.size()));
   /*             if(tmpPD.getUnsignedInteger(ID_COMMAND).byteValue()==REMOTE_EXCUTE_CONTINUE_MESSAGE ){
                    busyBegin.setToNow();
                }*/
            } else {
                send_full_page=false;
                PebbleKit.sendDataToPebbleWithTransactionId(_contex, Constants.PEBBLE_UUID, tmpPD, TRANS_ID_COMMON);
                Constants.log(TAG_NAME,"Send common package. sendQueue length:" + String.valueOf(sendQueue.size()));

            }


        }
    };

    Runnable closeApp=new Runnable() {
        @Override
        public void run() {
   //         appStatue=0;
            PebbleKit.closeAppOnPebble(_contex,Constants.PEBBLE_UUID);
            clean_SendQue();
        }
    };

    Runnable sendEmpty=new Runnable() {
        @Override
        public void run() {
            PebbleDictionary empPd=new PebbleDictionary();
            empPd.addUint8(ID_COMMAND,REMOTE_EMPTY);
            PebbleKit.sendDataToPebbleWithTransactionId(_contex, Constants.PEBBLE_UUID, empPd, TRANS_ID_EMPTY);
        }
    };

    private void loadPref(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
   //     fLength=Constants.MAX_PACKAGE_LENGTH;
        timeOut=Long.parseLong(sharedPref.getString(Constants.PREFERENCE_MIN_NOTIFICATION_WAIT, "10000"));
        callEnable = sharedPref.getBoolean(Constants.PREFERENCE_CALL_ENABLE, true);
  //      Constants.log("callstatue", "callEnable is " + String.valueOf(callEnable));
        char_scale=(byte)Integer.parseInt(sharedPref.getString(Constants.PREFERENCE_MESSAGE_SCALE,String.valueOf(Constants.MESSAGE_SCALE_SMALL)));
        whiteBackground=!sharedPref.getBoolean(Constants.PREFERENCE_BLACK_BACKGROUND,false);
        switch ((int)char_scale){
            case Constants.MESSAGE_SCALE_SMALL:
   //             fChars=Constants.SMALL_LINE_CONTAIN_CHARS;
                fLines=Constants.SMALL_PAGE_CONTAIN_LINES;
                break;
            case Constants.MESSAGE_SCALE_MID:
   //             fChars=Constants.MID_LINE_CONTAIN_CHARS;
                fLines=Constants.MID_PAGE_CONTAIN_LINES;
                break;
            case Constants.MESSAGE_SCALE_LARGE:
  //              fChars=Constants.LARGE_LINE_CONTAIN_CHARS;
                fLines=Constants.LARGE_PAGE_CONTAIN_LINES;
                break;
        }
        if (callEnable) {
            sms1 = sharedPref.getString(Constants.PREFERENCE_CALL_SMS_SHORT,
                    getString(R.string.pref_call_sms_short_default));
            sms2 = sharedPref.getString(Constants.PREFERENCE_CALL_SMS_LONG,
                    getString(R.string.pref_call_sms_long_default));
        }
    }

    private void endCall() {
        TelephonyManager telMag = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        Class<TelephonyManager> c = TelephonyManager.class;
        Method mthEndCall;
        try {
            mthEndCall = c.getDeclaredMethod("getITelephony", (Class[]) null);
            mthEndCall.setAccessible(true);
            ITelephony iTel = (ITelephony) mthEndCall.invoke(telMag, (Object[]) null);
            iTel.endCall();

        } catch (Exception e) {
            e.printStackTrace();
        }

        /*Context context = getApplicationContext();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Constants.log("speakerset", String.format("AudioManager before mode:%d speaker mod:%s", audioManager.getMode(), String.valueOf(audioManager.isSpeakerphoneOn())));
        Intent meidaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK);
        meidaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
        context.sendOrderedBroadcast(meidaButtonIntent, "android.permission.CALL_PRIVILEGED");*/

    }

    private void answerCall(boolean isSpeakon) {

        Context context = getApplicationContext();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // 判断是否插上了耳机
        if (!(audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn())) {
            // 4.1以上系统限制了部分权限， 使用三星4.1版本测试提示警告：Permission Denial: not allowed to
            // send broadcast android.intent.action.HEADSET_PLUG from pid=1324,
            // uid=10017
            // 这里需要注意一点，发送广播时加了权限“android.permission.CALL_PRIVLEGED”，则接受该广播时也需要增加该权限。但是4.1以上版本貌似这个权限只能系统应用才可以得到。测试的时候，自定义的接收器无法接受到此广播，后来去掉了这个权限，设为NULL便可以监听到了。


            Constants.log("speakerset", String.format("AudioManager before mode:%d speaker mod:%s", audioManager.getMode(), String.valueOf(audioManager.isSpeakerphoneOn())));
            Intent meidaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK);
            meidaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
            context.sendOrderedBroadcast(meidaButtonIntent, "android.permission.CALL_PRIVILEGED");

            /*TelephonyManager telMag = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            Class<TelephonyManager> c = TelephonyManager.class;
            Method mthAnswerCall;
            try {
                mthAnswerCall = c.getDeclaredMethod("getITelephony", (Class[]) null);
                mthAnswerCall.setAccessible(true);
                ITelephony iTel = (ITelephony) mthAnswerCall.invoke(telMag, (Object[]) null);
                iTel.answerRingingCall();

            } catch (Exception e) {
                e.printStackTrace();
            }*/
            if (isSpeakon) {
                try {
                    Thread.sleep(500);

                } catch (InterruptedException e) {
                    Constants.log("speakerset", "Problem while sleeping");
                }
                Constants.log("speakerset", "AudioManager answer mode:" + audioManager.getMode() + " speaker mod:"
                        + String.valueOf(audioManager.isSpeakerphoneOn()));
                while (audioManager.getMode() != AudioManager.MODE_IN_CALL) {
                    try {
                        Thread.sleep(300);

                    } catch (InterruptedException e) {
                        Constants.log("speakerset", "Problem while sleeping");
                    }
                }
                // audioManager.setMicrophoneMute(true);
                audioManager.setSpeakerphoneOn(true);
                // audioManager.setMode(AudioManager.MODE_IN_CALL);
                Constants.log("speakerset", String.format("AudioManager set mode:%d speaker mod:%s", audioManager.getMode(), String.valueOf(audioManager.isSpeakerphoneOn())));

            }
        } else {
            Constants.log(
                    "speakerset",
                    String.format("AudioManager before mode:%d speaker mod:%s", audioManager.getMode(), String.valueOf(audioManager.isSpeakerphoneOn())));

            Intent meidaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK);
            meidaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
            context.sendOrderedBroadcast(meidaButtonIntent, "android.permission.CALL_PRIVILEGED");

        }
    }

    private void dialNumber(String phoneNumber, boolean isSpeakon){
        Context context = getApplicationContext();

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Constants.log(TAG_NAME, String.format("Dial phone:%s", phoneNumber));
        // 判断是否插上了耳机
        Intent callIntent=new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse(String.format("tel:%s", phoneNumber)));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        callIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
        startActivity(callIntent);

        if (!(audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn())) {
            // 4.1以上系统限制了部分权限， 使用三星4.1版本测试提示警告：Permission Denial: not allowed to
            // send broadcast android.intent.action.HEADSET_PLUG from pid=1324,
            // uid=10017
            // 这里需要注意一点，发送广播时加了权限“android.permission.CALL_PRIVLEGED”，则接受该广播时也需要增加该权限。但是4.1以上版本貌似这个权限只能系统应用才可以得到。测试的时候，自定义的接收器无法接受到此广播，后来去掉了这个权限，设为NULL便可以监听到了。


            Constants.log("speakerset", String.format("AudioManager before mode:%d speaker mod:%s", audioManager.getMode(), String.valueOf(audioManager.isSpeakerphoneOn())));

            if (isSpeakon) {
                try {
                    Thread.sleep(500);

                } catch (InterruptedException e) {
                    Constants.log("speakerset", "Problem while sleeping");
                }
                Constants.log("speakerset", String.format("AudioManager answer mode:%d speaker mod:%s", audioManager.getMode(), String.valueOf(audioManager.isSpeakerphoneOn())));
                while (audioManager.getMode() != AudioManager.MODE_IN_CALL) {
                    try {
                        Thread.sleep(300);

                    } catch (InterruptedException e) {
                        Constants.log("speakerset", "Problem while sleeping");
                    }
                }
                // audioManager.setMicrophoneMute(true);
                audioManager.setSpeakerphoneOn(true);
                // audioManager.setMode(AudioManager.MODE_IN_CALL);
                Constants.log("speakerset", String.format("AudioManager set mode:%d speaker mod:%s", audioManager.getMode(), String.valueOf(audioManager.isSpeakerphoneOn())));

            }
        }
    }

    public void doSendSMSTo(String phoneNumber, String message) {
        if (PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)) {
            SmsManager smsmanager = SmsManager.getDefault();
            ArrayList<String> smsList = smsmanager.divideMessage(message);
            for (String sms : smsList) {
                smsmanager.sendTextMessage(phoneNumber, null, sms, null, null);
            }
            Constants.log("sendsms", String.format("send:[%s] to number:%s", message, phoneNumber));
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

}
