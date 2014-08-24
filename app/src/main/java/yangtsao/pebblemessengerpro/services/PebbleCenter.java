package yangtsao.pebblemessengerpro.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.format.Time;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;

import yangtsao.pebblemessengerpro.Constants;
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
    private File watchFile;
    private Long lastChange;
    private Long timeOut;

    private Deque<PebbleDictionary> sendQueue;
    private Queue<PebbleMessage> waitQueue;
    private boolean pebbleBusy=false;
    private Time busyBegin;
    private final static long MAX_WAITING_MILLIS=30000;
    private final static int MAX_CHARS_PACKAGE_CONTAIN=80;
    private int appStatue=0;
    //pebble request. Use transaction id to receive command, and extra data for the addition information.
    private static final int REQUEST_TRANSID_MESSAGE=1;
    private static final int REQUEST_TRANSID_CALL=2;
    private static final int REQUEST_TRANSID_MESSAGE_TABLE=3;
    private static final int REQUEST_TRANSID_CALL_TABLE=4;
    private static final int REQUEST_TRANSID_CLOSE_APP=5;
    private static final int REQUEST_TRANSID_PICKUP_PHONE=6;
    private static final int REQUEST_TRANSID_HANGOFF_PHONE=7;
    private static final int REQUEST_TRANSID_NEXTPAGE=8;
    private static final int ID_EXTRA_DATA=2;

    //pebble command
    private static final int ID_COMMAND=0;
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
        watchFile = new File(getFilesDir() + "PrefsChanged.none");
        if (!watchFile.exists()) {
            try {
                watchFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            watchFile.setLastModified(System.currentTimeMillis());
        }
        loadPref();
        prepareThread=new PrepareThread();
        prepareThread.start();
        sendMsgThread=new SendMsgThread();
        sendMsgThread.start();
        bindService(new Intent(this, MessageProcessingService.class), connToMessageProcessing,
                Context.BIND_AUTO_CREATE);
        _contex=this;
        busyBegin=new Time();
        isPebbleEnable=PebbleKit.isWatchConnected(_contex);
        PebbleKit.registerReceivedDataHandler(_contex,new PebbleKit.PebbleDataReceiver(Constants.PEBBLE_UUID) {
            @Override
            public void receiveData(Context context, int transactionId, PebbleDictionary data) {
                PebbleKit.sendAckToPebble(_contex, transactionId);
                switch (transactionId){
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
                        break;
                    case REQUEST_TRANSID_HANGOFF_PHONE:
                        break;
                    case REQUEST_TRANSID_CLOSE_APP:
                        sendMsgThreadHandler.sendEmptyMessage(SEND_CLOSE_APP);
                        break;
                    case REQUEST_TRANSID_NEXTPAGE:
                        sendMsgThreadHandler.sendEmptyMessage(SEND_NEXT_PAGE);
                        break;
                }
            }
        });

        PebbleKit.registerReceivedAckHandler(_contex,new PebbleKit.PebbleAckReceiver(Constants.PEBBLE_UUID) {
            @Override
            public void receiveAck(Context context, int transactionId) {
                switch (transactionId){
                    case TRANS_ID_COMMON:
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
                if (PebbleKit.isWatchConnected(_contex)){
                    switch (transactionId){
                        case TRANS_ID_COMMON:
                            sendMsgThreadHandler.sendEmptyMessage(SEND_CONTINUE);
                            break;
                        case TRANS_ID_END:
                            break;
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
            }
        });
    }

    @Override
    public void onDestroy() {
        unbindService(connToMessageProcessing);
        super.onDestroy();
    }

    class PebbleCenterHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            if(!isPebbleEnable) return;
            if (watchFile.lastModified() > lastChange) {
                loadPref();
            }
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
                    clean_SendQue();
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
                dataMsg.addInt8(ID_COMMAND,commandID);
            }else{
                dataMsg.addInt8(ID_COMMAND,REMOTE_DISPLAY_CONTINUE);
            }
            dataMsg.addInt8(ID_TOTAL_PACKAGES,totalPackges);
            dataMsg.addInt8(ID_PACKAGE_NUM,(byte) pg);
            dataMsg.addString(ID_ASCSTR,strMsg.substring(pg*MAX_CHARS_PACKAGE_CONTAIN,MAX_CHARS_PACKAGE_CONTAIN-1));
            sendQueue.add(dataMsg);
        }
    }

    private List<PebbleMessage> splitPages(PebbleMessage tmpPM){
        List<PebbleMessage> pmPages=new ArrayList<PebbleMessage>();
        String[] splitString= tmpPM.getAscMsg().split("\n");
        int pageCount=bigInt(splitString.length / fLines);
        for (int page=1;page<=pageCount;page++){
            PebbleMessage itemPm=new PebbleMessage();
            StringBuilder tmpSB=new StringBuilder();
            for (int line=1;line<=fLines;line++){
                tmpSB.append(splitString[(page-1)*fLines+line-1]);
            }
            itemPm.setAscMsg(tmpSB.toString());
            Deque<CharacterMatrix> itemDqCM=new ArrayDeque<CharacterMatrix>();
            while(!tmpPM.getCharacterQueue().isEmpty()){
                CharacterMatrix tmpCM=tmpPM.getCharacterQueue().pollFirst();
                if(tmpCM.getPos()[0]>=(page-1)*fLines && tmpCM.getPos()[0]<page*fLines){
                    itemDqCM.add(tmpCM);
                }else{
                    break;
                }
            }
            itemPm.setCharacterQueue(itemDqCM);
            pmPages.add(page,itemPm);
        }
        return pmPages;
    }

    private void split_message_to_packages_add_to_sendQue(List<PebbleMessage> listPM){
        PebbleDictionary dataMsg=new PebbleDictionary();
        dataMsg.addInt8(ID_COMMAND,REMOTE_EXCUTE_NEW_MESSAGE);
        if (appStatue==0) {
            dataMsg.addInt8(ID_CLOSE_DELAY_SEC, (byte) (timeOut / 1000));
        }
        sendQueue.add(dataMsg);
        for(int page=1;page<=listPM.size();page++){
            PebbleMessage dealPM=listPM.get(page);
            int strPackages=bigInt(dealPM.getAscMsg().length()/MAX_CHARS_PACKAGE_CONTAIN);
            byte totalPackages=(byte) (dealPM.getCharacterQueue().size()+strPackages);
            for (int pg=1;pg<=strPackages;pg++){
                dataMsg=new PebbleDictionary();
                dataMsg.addInt8(ID_COMMAND,REMOTE_EXCUTE_CONTINUE_MESSAGE);
                dataMsg.addInt8(ID_TOTAL_PAGES,(byte)listPM.size());
                dataMsg.addInt8(ID_PAGE_NUM,(byte)page);
                dataMsg.addInt8(ID_TOTAL_PACKAGES,totalPackages);
                dataMsg.addInt8(ID_PACKAGE_NUM,(byte) pg);
                dataMsg.addString(ID_ASCSTR,dealPM.getAscMsg().substring(pg*MAX_CHARS_PACKAGE_CONTAIN,MAX_CHARS_PACKAGE_CONTAIN-1));
                sendQueue.add(dataMsg);
            }
            for (int pg=strPackages+1;pg<=totalPackages;pg++){
                dataMsg=new PebbleDictionary();
                dataMsg.addInt8(ID_COMMAND,REMOTE_EXCUTE_CONTINUE_MESSAGE);
                dataMsg.addInt8(ID_TOTAL_PAGES,(byte)listPM.size());
                dataMsg.addInt8(ID_PAGE_NUM,(byte)page);
                dataMsg.addInt8(ID_TOTAL_PACKAGES,totalPackages);
                dataMsg.addInt8(ID_PACKAGE_NUM,(byte) pg);
                CharacterMatrix cm=dealPM.getCharacterQueue().pollFirst();
                int size = cm.getByteList().size();
                byte[] b2 = new byte[size];
                cm.getbyteArray(b2,size);
                dataMsg.addBytes(ID_UNICHR_BYTES, b2);
                dataMsg.addInt8(ID_UNICHR_WIDTH,(byte) cm.getWidthBytes());
                dataMsg.addBytes(ID_UNICHR_POS,cm.getPos());
                sendQueue.add(dataMsg);
            }

        }

    }

    private void split_call_to_package_add_to_sendQue(PebbleCall pbCall){
        byte totalPackages=(byte) (pbCall.getCharacterQueue().size() +1);
        PebbleDictionary dataMsg;
        for (int pg=totalPackages;pg>1;pg--){
            dataMsg=new PebbleDictionary();
            dataMsg.addInt8(ID_COMMAND,REMOTE_EXCUTE_CONTINUE_CALL);
            dataMsg.addInt8(ID_TOTAL_PACKAGES,totalPackages);
            dataMsg.addInt8(ID_PACKAGE_NUM,(byte) pg);
            CharacterMatrix cm= pbCall.getCharacterQueue().pollLast();
            int size=cm.getByteList().size();
            byte[] b2=new byte[size];
            cm.getbyteArray(b2,size);
            dataMsg.addBytes(ID_UNICHR_BYTES, b2);
            dataMsg.addInt8(ID_UNICHR_WIDTH,(byte) cm.getWidthBytes());
            dataMsg.addBytes(ID_UNICHR_POS,cm.getPos());
            sendQueue.addFirst(dataMsg);
        }
        dataMsg=new PebbleDictionary();
        dataMsg.addInt8(ID_COMMAND,REMOTE_EXCUTE_NEW_CALL);
        if (appStatue==0) {
            dataMsg.addInt8(ID_CLOSE_DELAY_SEC, (byte) 0);
        }
        dataMsg.addInt8(ID_TOTAL_PACKAGES,totalPackages);
        dataMsg.addInt8(ID_PACKAGE_NUM, (byte) 1);
        dataMsg.addString(ID_ASCSTR,pbCall.getAscMsg());
        sendQueue.addFirst(dataMsg);
    }

    private int bigInt(float d){
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
            if(!isPebbleEnable){
                clean_SendQue();
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
                    this.post(sendToPebble);
                    break;
                case SEND_NEXT_PAGE:
                    this.post(sendToPebble);
                    break;
                case SEND_CALL:
                    this.post(sendToPebble);
                    break;
                case SEND_CALL_END:
                {
                    PebbleDictionary callEnd=new PebbleDictionary();
                    callEnd.addInt8(ID_COMMAND,REMOTE_EXCUTE_CALL_END);
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
            PebbleDictionary tmpPD=sendQueue.poll();
            if (tmpPD.getInteger(ID_PACKAGE_NUM) == tmpPD.getInteger(ID_TOTAL_PACKAGES)) {
                PebbleKit.sendDataToPebbleWithTransactionId(_contex, Constants.PEBBLE_UUID, tmpPD, TRANS_ID_END);
            } else {
                PebbleKit.sendDataToPebbleWithTransactionId(_contex, Constants.PEBBLE_UUID, tmpPD, TRANS_ID_COMMON);
            }

        }
    };

    Runnable closeApp=new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            PebbleKit.closeAppOnPebble(_contex,Constants.PEBBLE_UUID);
            pebbleBusy=false;
            appStatue=0;
        }
    };

    private void loadPref(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        fLength=Constants.MAX_PACKAGE_LENGTH;
        timeOut=sharedPref.getLong(Constants.PREFERENCE_MIN_NOTIFICATION_WAIT, 5000);
        switch (sharedPref.getInt(Constants.PREFERENCE_MESSAGE_SCALE,Constants.MESSAGE_SCALE_SMALL)){
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
        lastChange = watchFile.lastModified();
    }

    private void endCall() {
        TelephonyManager telMag = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        Class<TelephonyManager> c = TelephonyManager.class;
        Method mthEndCall = null;
        try {
            mthEndCall = c.getDeclaredMethod("getITelephony", (Class[]) null);
            mthEndCall.setAccessible(true);
            ITelephony iTel = (ITelephony) mthEndCall.invoke(telMag, (Object[]) null);
            iTel.endCall();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
