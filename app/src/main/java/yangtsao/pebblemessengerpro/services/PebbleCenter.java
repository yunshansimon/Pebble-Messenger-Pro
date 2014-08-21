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

    private int fChars;  //line contain chars
    private int fLines;  //page contain lines
    private int fLength; //package max length

    private Queue<PebbleDictionary> sendQueue;
    private Queue<PebbleMessage> waitQueue;
    private boolean pebbleBusy=false;
    private Time busyBegin;

    private Handler prepareThreadHandler;

    private Messenger rMessageProcessingHandler=null;


    private ServiceConnection connToMessagePreocessing=new ServiceConnection() {
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
        bindService(new Intent(this, MessageProcessingService.class), connToMessagePreocessing,
                Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onDestroy() {
        unbindService(connToMessagePreocessing);
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
            super.handleMessage(msg);
        }
    }
}
