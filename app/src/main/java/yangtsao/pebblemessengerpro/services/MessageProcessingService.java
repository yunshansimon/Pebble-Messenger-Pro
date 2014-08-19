package yangtsao.pebblemessengerpro.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.text.format.Time;

import java.util.ArrayDeque;
import java.util.Deque;

import yangtsao.pebblemessengerpro.Constants;
import yangtsao.pebblemessengerpro.db.FontDbHandler;
import yangtsao.pebblemessengerpro.db.MessageDbHandler;
import yangtsao.pebblemessengerpro.models.CharacterMatrix;
import yangtsao.pebblemessengerpro.models.Font;
import yangtsao.pebblemessengerpro.models.PebbleMessage;

public class MessageProcessingService extends Service {
    private static FontDbHandler fdb;
    private static MessageDbHandler mdb;
    private static Context _context;
    private static final String TAG_NAME ="MessageProcessingService";

    public static final int MSG_NEW_MESSAGE        =0;
    public static final int MSG_NEW_CALL           =1;
    public static final int MSG_MESSAGE_READY      =2;
    public static final int MSG_GET_MESSAGE_TABLE  =3;
    public static final int MSG_GET_CALL_TABLE     =4;
    public static final int MSG_GET_MESSAGE        =5;
    public static final int MSG_GET_CALL           =6;
    public static final int MSG_CLEAN              =7;


    public MessageProcessingService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MessageProcessingService._context = getApplicationContext();
        fdb=new FontDbHandler(_context);
        fdb.open();
        mdb=new MessageDbHandler(_context);
        mdb.open();
        bindService(new Intent(this, PebbleCenter.class), connToPebbleCenter,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        fdb.close();
        mdb.close();
        super.onDestroy();
    }

    final Messenger mMessengerHandler= new Messenger(new MessageHandler());
    Messenger mPebbleCenterHandler =null;
    private final ServiceConnection connToPebbleCenter =new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mPebbleCenterHandler=new Messenger(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mPebbleCenterHandler=null;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return mMessengerHandler.getBinder();
    }

    static class MessageHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_NEW_MESSAGE:
                    addNewMessage(msg.getData());

                    break;
                case MSG_NEW_CALL:
                    break;
                case MSG_MESSAGE_READY:
                    break;
                case MSG_GET_MESSAGE_TABLE:
                    break;
                case MSG_GET_CALL_TABLE:
                    break;
                case MSG_GET_MESSAGE:
                    break;
                case MSG_GET_CALL:
                    break;
                case MSG_CLEAN:
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

        private void addNewMessage(Bundle b){
            Time nowTime= new Time();
            nowTime.setToNow();
            mdb.addMessage(nowTime,b.getString(MessageDbHandler.COL_MESSAGE_APP),b.getString(MessageDbHandler.COL_MESSAGE_CONTENT));
        }

        private void processMessage(Bundle b){
            if (!isFontDBReady()){
                Constants.log(TAG_NAME,"Font data base is not ready, processing stop!");
                return;
            }
            final String tmpString=b.getString(MessageDbHandler.COL_MESSAGE_CONTENT).substring(0,179);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String originalMessage=tmpString;
                    PebbleMessage message = new PebbleMessage();
                    // Clear the characterQueue, just in case
                    Deque<CharacterMatrix> characterQueue = new ArrayDeque<CharacterMatrix>();
                    while(originalMessage.length()>0)
                    {
                        int row = 1;
                        int col = 0;
                        int codepoint = originalMessage.codePointAt(0);
                        if (codepoint == 0) {
                            break;
                        }
                        Constants.log("codepoint", "char='" + (char) codepoint + "' code=" + String.valueOf(codepoint));
                        if (codepoint <= 127) {
                            if (codepoint == 10) {
                                row++;
                                col = 0;
                                message.AddCharToAscMsg(originalMessage.charAt(0));
                            } else {
                                if (col < 16) {
                                    if (col == 15) {
                                        if (message.getAscMsg().matches("\\w\\z") && originalMessage.matches("\\A\\w")) {
                                            message.AddStringToAscMsg("-\n");
                                            row++;
                                            col = 0;
                                        }
                                    }

                                    col++;
                                    message.AddCharToAscMsg(originalMessage.charAt(0));
                                } else {
                                    message.AddCharToAscMsg('\n');
                                    message.AddCharToAscMsg(originalMessage.charAt(0));
                                    row++;
                                    col = 1;
                                }
                            }

                        } else {
                            String originalHex;
                            String codepointStr = Integer.toHexString(codepoint).toUpperCase();
                            // Constants.log("codepoint", "codepoint=" +
                            // String.valueOf(codepoint) + " codeStr=" + codepointStr);
                            if (codepointStr.length() < 4) {
                                codepointStr = ("0000" + codepointStr).substring(codepointStr.length());
                            }
                            Constants.log(TAG_NAME, "codepoint=" + String.valueOf(codepoint) + " codeStr=" + codepointStr);
                            Font font = fdb.getFont(codepointStr);
                            if (font == null) {
                                Constants.log(TAG_NAME, "font is null! codepoint=[" + String.valueOf(codepoint) + "] char=["
                                        + (char) codepoint + "]");
                                originalMessage = originalMessage.substring(1);
                                continue;
                                // originalHex =
                                // "004000E001F003F8071C0C061E473FE77FE7FFC7FF8F7F1F3F3F1FFF0FBE071C";
                            } else {
                                originalHex = font.getHex();
                            }
                            // Constants.log("codepoint", "char='" + (char) codepoint +
                            // "' hex=" + originalHex);
                            CharacterMatrix c = new CharacterMatrix(originalHex);

                            if (c.getWidthBytes() == 2) {
                                if (col < 15) {
                                    c.setPos(row, col + 1);
                                    message.AddStringToAscMsg("  ");
                                    col += 2;
                                } else {
                                    message.AddCharToAscMsg('\n');
                                    message.AddStringToAscMsg("  ");
                                    row++;
                                    col = 0;
                                    c.setPos(row, col + 1);
                                    col += 2;
                                }

                            } else {
                                if (col < 16) {
                                    c.setPos(row, col + 1);
                                    message.AddCharToAscMsg(' ');
                                    col++;
                                } else {
                                    message.AddCharToAscMsg('\n');
                                    message.AddCharToAscMsg(' ');
                                    row++;
                                    col = 0;
                                    c.setPos(row, col + 1);
                                    col++;
                                }

                            }

                            characterQueue.add(c);

                        }

                        if (row > 8 && (col > 10 || originalMessage.charAt(0) == '\n')) {
                            Constants.log("codepoint", "too many chars!the end char='" + (char) codepoint + "'");
                            message.AddStringToAscMsg("...");

                            break;
                        }
                        originalMessage = originalMessage.substring(1);
                    }

                    message.setCharacterQueue(characterQueue);
                }
            }).run();
        }

        private boolean isFontDBReady(){
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(_context);
            return sharedPref.getBoolean(Constants.DATABASE_READY, false);
        }
    }
}
