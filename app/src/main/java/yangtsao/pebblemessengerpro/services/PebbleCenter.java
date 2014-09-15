package yangtsao.pebblemessengerpro.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
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
import android.text.format.Time;
import android.view.KeyEvent;

import com.android.internal.telephony.ITelephony;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


import yangtsao.pebblemessengerpro.Constants;
import yangtsao.pebblemessengerpro.R;
import yangtsao.pebblemessengerpro.db.MessageDbHandler;
import yangtsao.pebblemessengerpro.models.CharacterMatrix;
import yangtsao.pebblemessengerpro.models.PebbleCall;
import yangtsao.pebblemessengerpro.models.PebbleMessage;

public class PebbleCenter extends Service {
    public final static int PEBBLE_SEND_MESSAGE=1;
    public final static int PEBBLE_SEND_CALL=2;
    public final static int PEBBLE_CALL_IDLE=3;
    public final static int PEBBLE_SEND_MESSAGE_TABLE=4;
    public final static int PEBBLE_SEND_CALL_TABLE=5;

    public boolean isPebbleEnable=true;
    private static final int PREPARE_MESSAGE=1;
    private static final int PREPARE_CALL=2;
    private static final int PREPARE_MESSAGE_TABLE=3;
    private static final int PREPARE_CALL_TABLE=4;

    private static final int SEND_MESSAGE=1;
    private static final int SEND_NEXT_PAGE=2;
    private static final int SEND_CONTINUE=3;
    private static final int SEND_CALL=4;
    private static final int SEND_CALL_END=5;
    private static final int SEND_CLOSE_APP=6;

    private static final int TRANS_ID_COMMON=0;
    private static final int TRANS_ID_END=1;

    private int fChars;  //line contain chars
    private int fLines;  //page contain lines
    private int fLength; //package max length
    private byte char_scale;
    private Long timeOut;
    private boolean callEnable=true;
    private String sms1;
    private String sms2;


    private Deque<PebbleDictionary> sendQueue;
    private Deque<PebbleMessage> waitQueue;
    private boolean pebbleBusy=false;
    private Time busyBegin;
    private final static long MAX_WAITING_MILLIS=30000;
    private final static int MAX_CHARS_PACKAGE_CONTAIN=60;
    private int appStatue=0;
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
    private static final int ID_EXTRA_DATA=2;
    private static final int REQUEST_EXTRA_SPEAKER_ON=1;
    private static final int REQUEST_EXTRA_SPEAKER_OFF=2;


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
    private static final int ID_TOTAL_PAGES=1;
    private static final int ID_PAGE_NUM=2;
    private static final int ID_TOTAL_PACKAGES=3;
    private static final int ID_PACKAGE_NUM=4;
    private static final int ID_ASCSTR=5;
    private static final int ID_UNICHR_BYTES=6;
    private static final int ID_UNICHR_WIDTH=7;
    private static final int ID_UNICHR_POS=8;
    private static final int ID_CLOSE_DELAY_SEC=9;
    private static final int ID_CHAR_SCALE=10;
    private static final int ID_INFO_ID=11;
    private static final int ID_PHONE_NUM=12;

    private static final String TAG_NAME="PebbleCenter";

    private Handler prepareThreadHandler;
    private Handler sendMsgThreadHandler;

    private Thread prepareThread;
    private Thread sendMsgThread;


    private Context _contex;
    private Messenger rMessageProcessingHandler=null;


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
        loadPref();
        _contex=this;
        busyBegin=new Time();
        waitQueue=new ArrayDeque<PebbleMessage>();
        sendQueue=new ArrayDeque<PebbleDictionary>();
        bindService(new Intent(this, MessageProcessingService.class), connToMessageProcessing,
                Context.BIND_AUTO_CREATE);
        prepareThread=new PrepareThread();
        prepareThread.start();
        sendMsgThread=new SendMsgThread();
        sendMsgThread.start();
        isPebbleEnable=PebbleKit.isWatchConnected(_contex);
        PebbleKit.registerReceivedDataHandler(_contex,new PebbleKit.PebbleDataReceiver(Constants.PEBBLE_UUID) {
            @Override
            public void receiveData(Context context, int transactionId, PebbleDictionary data) {
                PebbleKit.sendAckToPebble(_contex, transactionId);
                appStatue++;
                Constants.log(TAG_NAME,"Received data form pebble");
                switch (data.getUnsignedInteger(ID_COMMAND).intValue()){
                    case REQUEST_TRANSID_CALL_TABLE: {
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
                        Message msg=Message.obtain();
                        msg.what=MessageProcessingService.MSG_GET_CALL;
                        Bundle b=new Bundle();
                        b.putString(MessageDbHandler.COL_CALL_ID, data.getString(ID_EXTRA_DATA));
                        msg.setData(b);
                        try {
                            rMessageProcessingHandler.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

                    }
                        break;
                    case REQUEST_TRANSID_MESSAGE:
                        Message msg=Message.obtain();
                        msg.what=MessageProcessingService.MSG_GET_MESSAGE;
                        Bundle b=new Bundle();
                        b.putString(MessageDbHandler.COL_MESSAGE_ID, data.getString(ID_EXTRA_DATA));
                        msg.setData(b);
                        try {
                            rMessageProcessingHandler.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                    case REQUEST_TRANSID_PICKUP_PHONE:
                        switch (data.getUnsignedInteger(ID_EXTRA_DATA).intValue()){
                            case REQUEST_EXTRA_SPEAKER_ON:
                                answerCall(true);
                                break;
                            case REQUEST_EXTRA_SPEAKER_OFF:
                                answerCall(false);
                                break;
                        }
                        pebbleBusy=false;
                        break;
                    case REQUEST_TRANSID_HANGOFF_PHONE:
                        endCall();
                        pebbleBusy=false;

                        break;
                    case REQUEST_TRANSID_HANGOFF_SMS1:
                        endCall();
                        doSendSMSTo(data.getString(ID_EXTRA_DATA),sms1);
                        pebbleBusy=false;

                        break;
                    case REQUEST_TRANSID_HANGOFF_SMS2:
                        endCall();
                        doSendSMSTo(data.getString(ID_EXTRA_DATA),sms2);
                        pebbleBusy=false;

                        break;
                    case REQUEST_TRANSID_CLOSE_APP:
                        Constants.log(TAG_NAME,"Get close app command.");
                       // sendMsgThreadHandler.sendEmptyMessage(SEND_CLOSE_APP);
                        pebbleBusy=false;
                        appStatue=0;
                        clean_SendQue();
                        break;
                    case REQUEST_TRANSID_NEXTPAGE:
                        sendMsgThreadHandler.sendEmptyMessage(SEND_NEXT_PAGE);
                        break;
                    case REQUEST_TRANSID_READ_NOTIFY:
                    {
                        Constants.log(TAG_NAME,"Get command to read msg");
                        Message read_msg=Message.obtain();
                        read_msg.what=MessageProcessingService.MSG_READ;
                        Bundle bd=new Bundle();
                        bd.putString(MessageDbHandler.COL_MESSAGE_ID,data.getUnsignedInteger(ID_EXTRA_DATA).toString());
                        read_msg.setData(bd);
                        try {
                            rMessageProcessingHandler.send(read_msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                        break;
                    case REQUEST_TRANSID_IM_FREE:
                        pebbleBusy=false;
                        clean_SendQue();
                        break;
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
                        sendMsgThreadHandler.sendEmptyMessage(SEND_CONTINUE);
                        break;
                    case TRANS_ID_END:

                        break;
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
                            break;
                        default:
                            appStatue=0;
                            pebbleBusy=false;
                            clean_SendQue();
                    }
                }else{
                    clean_SendQue();
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
                appStatue=0;
                pebbleBusy=false;
                clean_SendQue();
            }
        });

        BroadcastReceiver br= new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                Constants.log(TAG_NAME,"Get shared preference changed.");
                loadPref();
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
                    waitQueue.add((PebbleMessage) msg.getData().getSerializable(MessageProcessingService.PROCEED_MSG));
                    prepareThreadHandler.sendEmptyMessage(PREPARE_MESSAGE);
                }
                    break;
                case PEBBLE_SEND_CALL:{
                    Message callMsg=prepareThreadHandler.obtainMessage(PREPARE_CALL);
                    callMsg.setData(msg.getData());
                    prepareThreadHandler.sendMessage(callMsg);
                }
                    break;
                case PEBBLE_SEND_MESSAGE_TABLE:{
                    Message callMsg=prepareThreadHandler.obtainMessage(PREPARE_MESSAGE_TABLE);
                    callMsg.setData(msg.getData());
                    prepareThreadHandler.sendMessage(callMsg);
                }
                    break;
                case PEBBLE_SEND_CALL_TABLE:{
                    Message callMsg=prepareThreadHandler.obtainMessage(PREPARE_CALL_TABLE);
                    callMsg.setData(msg.getData());
                    prepareThreadHandler.sendMessage(callMsg);
                }
                break;
                case PEBBLE_CALL_IDLE:
                    sendMsgThreadHandler.sendEmptyMessage(SEND_CALL_END);
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }
    Messenger mPebbleCenterHandler=new Messenger(new PebbleCenterHandler());
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
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
                waitQueue.clear();
                return;
            }
            switch (msg.what){
                case PREPARE_MESSAGE:{
                    PebbleMessage dealPM=waitQueue.poll();
                    if(dealPM==null){
                        return;
                    }
                    while(pebbleBusy){
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Time nowTime=new Time();
                        nowTime.setToNow();
                        if( Math.abs(nowTime.toMillis(true)- busyBegin.toMillis(true))>MAX_WAITING_MILLIS){
                            clean_SendQue();
                        }
                    }
                    List<PebbleMessage> pages=splitPages(dealPM);
                    split_message_to_packages_add_to_sendQue(pages);
                    sendMsgThreadHandler.sendEmptyMessage(SEND_MESSAGE);
                }
                    break;
                case PREPARE_CALL:
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
            }



        }


    }
    private void split_string_to_package_add_to_sendQue(String strMsg, byte commandID){
        PebbleDictionary dataMsg;
        byte totalPackges=(byte) bigInt(strMsg.length()/MAX_CHARS_PACKAGE_CONTAIN);
        for (int pg=1;pg<=totalPackges;pg++){
            dataMsg=new PebbleDictionary();
            if (pg==1){
                dataMsg.addUint8(ID_COMMAND,commandID);
            }else{
                dataMsg.addUint8(ID_COMMAND,REMOTE_DISPLAY_CONTINUE);
            }
            dataMsg.addUint8(ID_TOTAL_PACKAGES,totalPackges);
            dataMsg.addUint8(ID_PACKAGE_NUM,(byte) pg);
            dataMsg.addString(ID_ASCSTR,strMsg.substring((pg-1)*MAX_CHARS_PACKAGE_CONTAIN,
                    (pg*MAX_CHARS_PACKAGE_CONTAIN> strMsg.length()? strMsg.length() : pg*MAX_CHARS_PACKAGE_CONTAIN)
            ));
            sendQueue.add(dataMsg);
        }
    }

    private List<PebbleMessage> splitPages(PebbleMessage tmpPM){
        List<PebbleMessage> pmPages=new ArrayList<PebbleMessage>();
        String[] splitString= tmpPM.getAscMsg().split("\n");
        Constants.log(TAG_NAME,tmpPM.getAscMsg());
        for(String str:splitString){
            Constants.log(TAG_NAME,"splitPages, deal:" + str);
        }
        int pageCount=bigInt(splitString.length /(float) fLines);
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
                    break;
                }
            }
            itemPm.setCharacterQueue(itemDqCM);
            pmPages.add(page-1,itemPm);
        }
        return pmPages;
    }

    private void split_message_to_packages_add_to_sendQue(List<PebbleMessage> listPM){
        PebbleDictionary dataMsg=new PebbleDictionary();
        dataMsg.addUint8(ID_COMMAND,REMOTE_EXCUTE_NEW_MESSAGE);

            dataMsg.addUint8(ID_CLOSE_DELAY_SEC, (byte) (timeOut / 1000));
            dataMsg.addUint8(ID_CHAR_SCALE,char_scale);
        Constants.log(TAG_NAME,"add char_scale:" + String.valueOf(char_scale));
            dataMsg.addUint32(ID_INFO_ID, listPM.get(0).get_id().intValue());
            dataMsg.addUint8(ID_TOTAL_PACKAGES,(byte) 2);
            dataMsg.addUint8(ID_PACKAGE_NUM,(byte) 1);

        sendQueue.add(dataMsg);
        Constants.log(TAG_NAME,"listpm.size:"+ String.valueOf(listPM.size()));
        for(int page=1;page<=listPM.size();page++){
            PebbleMessage dealPM=listPM.get(page-1);
            int strPackages=bigInt(dealPM.getAscMsg().length()/(float)MAX_CHARS_PACKAGE_CONTAIN);
            byte totalPackages=(byte) (dealPM.getCharacterQueue().size()+strPackages);
            Constants.log(TAG_NAME,"total Packages:" + String.valueOf(totalPackages));
            for (int pg=1;pg<=strPackages;pg++){
                dataMsg=new PebbleDictionary();
                dataMsg.addUint8(ID_COMMAND,REMOTE_EXCUTE_CONTINUE_MESSAGE);
                dataMsg.addUint8(ID_TOTAL_PAGES,(byte)listPM.size());
                dataMsg.addUint8(ID_PAGE_NUM,(byte)page);
                dataMsg.addUint8(ID_TOTAL_PACKAGES,totalPackages);
                dataMsg.addUint8(ID_PACKAGE_NUM,(byte) pg);
                dataMsg.addString(ID_ASCSTR,dealPM.getAscMsg().substring((pg-1)*MAX_CHARS_PACKAGE_CONTAIN,
                        (pg*MAX_CHARS_PACKAGE_CONTAIN> dealPM.getAscMsg().length()? (dealPM.getAscMsg().length()) : (pg*MAX_CHARS_PACKAGE_CONTAIN))
                ));
                Constants.log(TAG_NAME,"Add Queue a strmsg:[" + dataMsg.getString(ID_ASCSTR) +"]" + " packagenum=" + String.valueOf(dataMsg.getUnsignedInteger(ID_PACKAGE_NUM)));
                sendQueue.add(dataMsg);
            }
            for (int pg=strPackages+1;pg<=totalPackages;pg++){
                dataMsg=new PebbleDictionary();
                dataMsg.addUint8(ID_COMMAND,REMOTE_EXCUTE_CONTINUE_MESSAGE);
                dataMsg.addUint8(ID_TOTAL_PAGES,(byte)listPM.size());
                dataMsg.addUint8(ID_PAGE_NUM,(byte)page);
                dataMsg.addUint8(ID_TOTAL_PACKAGES,totalPackages);
                dataMsg.addUint8(ID_PACKAGE_NUM,(byte) pg);
                Constants.log(TAG_NAME,"There are " + String.valueOf(dealPM.getCharacterQueue().size()) + "unicode in queue");
                CharacterMatrix cm=dealPM.getCharacterQueue().pollFirst();
                int size = cm.getByteList().size();
                byte[] b2 = new byte[size];
                cm.getbyteArray(b2,size);
                dataMsg.addUint8(ID_UNICHR_WIDTH,(byte) cm.getWidthBytes());
                dataMsg.addBytes(ID_UNICHR_POS,cm.getPos());
                Constants.log(TAG_NAME,"row:" + String.valueOf(cm.getPos()[0]) + " col:" + String.valueOf(cm.getPos()[1]));
                dataMsg.addBytes(ID_UNICHR_BYTES, b2);
                Constants.log(TAG_NAME,"b2 length:" + String.valueOf(b2.length));
                Constants.log(TAG_NAME,"Add Queue a unimsg:" + dataMsg.getUnsignedInteger(ID_PACKAGE_NUM).toString());
                sendQueue.add(dataMsg);
            }

        }

    }

    private void split_call_to_package_add_to_sendQue(PebbleCall pbCall){
        byte totalPackages=(byte) (pbCall.getCharacterQueue().size() +1);
        send_empty_msg();
        PebbleDictionary dataMsg;
        for (int pg=totalPackages;pg>1;pg--){
            dataMsg=new PebbleDictionary();
            dataMsg.addUint8(ID_COMMAND,REMOTE_EXCUTE_CONTINUE_CALL);
            dataMsg.addUint8(ID_TOTAL_PACKAGES,totalPackages);
            dataMsg.addUint8(ID_PACKAGE_NUM,(byte) pg);
            CharacterMatrix cm= pbCall.getCharacterQueue().pollLast();
            int size=cm.getByteList().size();
            byte[] b2=new byte[size];
            cm.getbyteArray(b2,size);
            dataMsg.addBytes(ID_UNICHR_BYTES, b2);
            dataMsg.addUint8(ID_UNICHR_WIDTH,(byte) cm.getWidthBytes());
            dataMsg.addBytes(ID_UNICHR_POS,cm.getPos());
            sendQueue.addFirst(dataMsg);
        }
        dataMsg=new PebbleDictionary();
        dataMsg.addUint8(ID_COMMAND,REMOTE_EXCUTE_NEW_CALL);
        dataMsg.addUint8(ID_TOTAL_PACKAGES,totalPackages);
        dataMsg.addUint8(ID_PACKAGE_NUM, (byte) 1);
        dataMsg.addString(ID_ASCSTR, pbCall.getAscMsg());
        dataMsg.addString(ID_PHONE_NUM,pbCall.getPhoneNum());
        dataMsg.addUint32(ID_INFO_ID, pbCall.get_id().intValue());
        sendQueue.addFirst(dataMsg);
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
        pebbleBusy=false;
    }

    private void send_empty_msg(){
        PebbleDictionary pd=new PebbleDictionary();
        pd.addUint8(ID_COMMAND,REMOTE_EMPTY);
        PebbleKit.sendDataToPebbleWithTransactionId(_contex,Constants.PEBBLE_UUID,pd,TRANS_ID_END);
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
            pebbleBusy=true;
            busyBegin.setToNow();
            if (appStatue==0) {
                appStatue++;
                PebbleKit.startAppOnPebble(_contex, Constants.PEBBLE_UUID);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            switch (msg.what){
                case SEND_MESSAGE:
                    this.post(sendToPebble);
                    break;
                case SEND_CONTINUE:
                {
                    if(sendQueue.isEmpty()) {
                        Constants.log(TAG_NAME,"sendQueue is empty! Can not send");
                        return;
                    }

                }
                    this.post(sendToPebble);
                    break;
                case SEND_NEXT_PAGE: {
                    PebbleDictionary data=sendQueue.peekFirst();
                    if(sendQueue.isEmpty()) {
                        Constants.log(TAG_NAME,"sendQueue is empty! Can not send");
                        return;
                    }
                    this.post(sendToPebble);

                }
                    break;
                case SEND_CALL:
                    this.post(sendToPebble);
                    break;
                case SEND_CALL_END:
                {
                    PebbleDictionary callEnd=new PebbleDictionary();
                    callEnd.addUint8(ID_COMMAND,REMOTE_EXCUTE_CALL_END);
                    clean_SendQue();
                    sendQueue.addFirst(callEnd);
                    this.post(sendToPebble);
                }
                    break;
                case SEND_CLOSE_APP:
                    this.post(closeApp);
                    break;
            }

        }

    }

    Runnable sendToPebble=new Runnable() {
        @Override
        public void run() {
            Constants.log(TAG_NAME,"Data send to pebble, sendQueue length:" + String.valueOf(sendQueue.size()));
            PebbleDictionary tmpPD=sendQueue.poll();

            if (tmpPD.getUnsignedInteger(ID_PACKAGE_NUM).intValue() == tmpPD.getUnsignedInteger(ID_TOTAL_PACKAGES).intValue()) {
                PebbleKit.sendDataToPebbleWithTransactionId(_contex, Constants.PEBBLE_UUID, tmpPD, TRANS_ID_END);
                Constants.log(TAG_NAME,"Send last package.sendQueue length:" + String.valueOf(sendQueue.size()));
            } else {
                PebbleKit.sendDataToPebbleWithTransactionId(_contex, Constants.PEBBLE_UUID, tmpPD, TRANS_ID_COMMON);
                Constants.log(TAG_NAME,"Send common package. sendQueue length:" + String.valueOf(sendQueue.size()));
            }

        }
    };

    Runnable closeApp=new Runnable() {
        @Override
        public void run() {

            pebbleBusy=false;
            appStatue=0;
        }
    };

    private void loadPref(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        fLength=Constants.MAX_PACKAGE_LENGTH;
        timeOut=Long.parseLong(sharedPref.getString(Constants.PREFERENCE_MIN_NOTIFICATION_WAIT, "10000"));
        callEnable = sharedPref.getBoolean(Constants.PREFERENCE_CALL_ENABLE, true);
        char_scale=(byte)Integer.parseInt(sharedPref.getString(Constants.PREFERENCE_MESSAGE_SCALE,String.valueOf(Constants.MESSAGE_SCALE_SMALL)));
        switch ((int)char_scale){
            case Constants.MESSAGE_SCALE_SMALL:
                fChars=Constants.SMALL_LINE_CONTAIN_CHARS;
                fLines=Constants.SMALL_PAGE_CONTAIN_LINES;
                break;
            case Constants.MESSAGE_SCALE_MID:
                fChars=Constants.MID_LINE_CONTAIN_CHARS;
                fLines=Constants.MID_PAGE_CONTAIN_LINES;
                break;
            case Constants.MESSAGE_SCALE_LARGE:
                fChars=Constants.LARGE_LINE_CONTAIN_CHARS;
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

    }

    private void answerCall(boolean isSpeakon) {

        Context context = getApplicationContext();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // 判断是否插上了耳机
        if (!audioManager.isWiredHeadsetOn()) {
            // 4.1以上系统限制了部分权限， 使用三星4.1版本测试提示警告：Permission Denial: not allowed to
            // send broadcast android.intent.action.HEADSET_PLUG from pid=1324,
            // uid=10017
            // 这里需要注意一点，发送广播时加了权限“android.permission.CALL_PRIVLEGED”，则接受该广播时也需要增加该权限。但是4.1以上版本貌似这个权限只能系统应用才可以得到。测试的时候，自定义的接收器无法接受到此广播，后来去掉了这个权限，设为NULL便可以监听到了。

            if (android.os.Build.VERSION.SDK_INT >= 15) {
                Constants.log("speakerset", "AudioManager before mode:" + audioManager.getMode() + " speaker mod:"
                        + String.valueOf(audioManager.isSpeakerphoneOn()));
                Intent meidaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK);
                meidaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
                context.sendOrderedBroadcast(meidaButtonIntent, null);
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
                    Constants.log("speakerset", "AudioManager set mode:" + audioManager.getMode() + " speaker mod:"
                            + String.valueOf(audioManager.isSpeakerphoneOn()));

                }

            }
        } else {
            Constants.log(
                    "speakerset",
                    "AudioManager before mode:" + audioManager.getMode() + " speaker mod:"
                            + String.valueOf(audioManager.isSpeakerphoneOn()));

            Intent meidaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK);
            meidaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
            context.sendOrderedBroadcast(meidaButtonIntent, null);

        }
    }

    public void doSendSMSTo(String phoneNumber, String message) {
        if (PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)) {
            SmsManager smsmanager = SmsManager.getDefault();
            ArrayList<String> smsList = smsmanager.divideMessage(message);
            for (String sms : smsList) {
                smsmanager.sendTextMessage(phoneNumber, null, sms, null, null);
            }
            Constants.log("sendsms", "send:[" + message + "] to number:" + phoneNumber);
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

}
