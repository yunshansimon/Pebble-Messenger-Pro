package yangtsao.pebblemessengerpro.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.text.format.Time;

import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.Queue;

import yangtsao.pebblemessengerpro.models.PebbleMessage;

public class PebbleCenter extends Service {
    public final static int PEBBLE_SEND_MESSAGE=1;
    public final static int PEBBLE_SEND_CALL=2;
    public final static int PEBBLE_CALL_IDLE=3;
    public final static int PEBBLE_SEND_MESSAGE_TABLE=4;
    public final static int PEBBLE_SEND_CALL_TABLE=5;

    private static final int PREPARE_MESSAGE=1;
    private static final int PREPARE_CALL=2;

    private int fChars;  //line contain chars
    private int fLines;  //page contain lines
    private int fLength; //package max length

    private Queue<PebbleDictionary> sendQueue;
    private Queue<PebbleMessage> waitQueue;
    private boolean pebbleBusy=false;
    private boolean endOfPage=false;
    private Time busyBegin;
    private final static long MAX_WAITING_MILLIS=20000;

    private Handler prepareThreadHandler;
    private Handler sendMsgThreadHandler;

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
        bindService(new Intent(this, MessageProcessingService.class), connToMessageProcessing,
                Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onDestroy() {
        unbindService(connToMessageProcessing);
        super.onDestroy();
    }

    class PebbleCenterHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case PEBBLE_SEND_MESSAGE:
                    break;
                case PEBBLE_SEND_CALL:
                    break;
                case PEBBLE_SEND_MESSAGE_TABLE:
                    break;
                case PEBBLE_SEND_CALL_TABLE:
                    break;
                case PEBBLE_CALL_IDLE:
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
                            sendQueue.clear();
                            pebbleBusy=false;
                        }
                    }


                }
                    break;
                case PREPARE_CALL:
                    break;
            }



        }
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
            super.handleMessage(msg);
        }
    }
}
