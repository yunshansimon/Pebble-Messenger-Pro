package yangtsao.pebblemessengerpro.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

public class PebbleCenter extends Service {
    public final static int PEBBLE_SEND_MESSAGE=1;
    public final static int PEBBLE_SEND_CALL=2;

    public PebbleCenter() {
    }

    static class PebbleCenterHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
    final Messenger mPebbleCenterHandler=new Messenger(new PebbleCenterHandler());
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return mPebbleCenterHandler.getBinder();
    }


}
