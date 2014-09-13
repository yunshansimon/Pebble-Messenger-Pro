package yangtsao.pebblemessengerpro.services;

import android.app.Application;
import android.app.Service;
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
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.format.Time;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import yangtsao.pebblemessengerpro.Constants;
import yangtsao.pebblemessengerpro.R;
import yangtsao.pebblemessengerpro.db.FontDbHandler;
import yangtsao.pebblemessengerpro.db.MessageDbHandler;
import yangtsao.pebblemessengerpro.models.CharacterMatrix;
import yangtsao.pebblemessengerpro.models.Font;
import yangtsao.pebblemessengerpro.models.PebbleCall;
import yangtsao.pebblemessengerpro.models.PebbleMessage;

public class MessageProcessingService extends Service  {
    private static FontDbHandler fdb;
    private static MessageDbHandler mdb;
    private static Context _context;
    private static final String TAG_NAME ="MessageProcessingService";

    public static final int MSG_NEW_MESSAGE        =0;
    public static final int MSG_NEW_CALL           =1;
    public static final int MSG_MESSAGE_READY      =2;
    public static final int MSG_CALL_READY         =3;
    public static final int MSG_GET_MESSAGE_TABLE  =4;
    public static final int MSG_GET_CALL_TABLE     =5;
    public static final int MSG_GET_MESSAGE        =6;
    public static final int MSG_GET_CALL           =7;
    public static final int MSG_CLEAN              =8;

    private static final int INNER_MESSAGE_PROCEED=0;
    private static final int INNER_CALL_PROCEED=1;
    public static final String PROCEED_MSG="proceed_message";
    public static final String PROCEED_CALL="proceed_call";

    public MessageProcessingService() {
    }

    public Messenger mMessenger;
    private Handler processHandler;
    private Thread proceedthread;
    private Handler messageHandler;
    private File watchFile;
    private long      lastChange;
    private boolean   quiet_hours           = false;
    private Calendar quiet_hours_before    = null;
    private Calendar      quiet_hours_after     = null;
    private boolean   callQuietEnable       = false;
    private int fChars;  //line contain chars
    private int fLines;  //page contain lines

    private Messenger rPebbleCenter =null;
    private final ServiceConnection connToPebbleCenter =new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            rPebbleCenter=new Messenger(iBinder);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            rPebbleCenter=null;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");

        return mMessenger.getBinder();
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
        loadPrefs();
        proceedthread=new ProcessThread();
        proceedthread.start();
        messageHandler=new MessageHandler(Looper.getMainLooper());
        mMessenger=new Messenger(messageHandler);
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
        unbindService(connToPebbleCenter);
        super.onDestroy();
    }

    class MessageHandler extends Handler{
        public MessageHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (watchFile.lastModified() > lastChange) {
                loadPrefs();
            }
            Constants.log(TAG_NAME,"New msg arrived. what:" + String.valueOf(msg.what));
            switch (msg.what){
                case MSG_NEW_MESSAGE:

                    if (quiet_hours) {

                        Calendar c = Calendar.getInstance();
                        Calendar now = new GregorianCalendar(0, 0, 0, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
                        Constants.log(TAG_NAME, "Checking quiet hours. Now: " + now.toString() + " vs "
                                + quiet_hours_before.toString() + " and " + quiet_hours_after.toString());
                        if (now.before(quiet_hours_before) || now.after(quiet_hours_after)) {
                            Constants.log(TAG_NAME, "Time is before or after the quiet hours time. Returning.");
                            addNewMessage(msg.getData(), MessageDbHandler.NEW_ICON);
                        } else {

                            Bundle b= msg.getData();
                            b.putLong(MessageDbHandler.COL_MESSAGE_ID,addNewMessage(msg.getData(), MessageDbHandler.OLD_ICON));
                            Message innerMsg=processHandler.obtainMessage(INNER_MESSAGE_PROCEED);
                            innerMsg.setData(b);

                            processHandler.sendMessage(innerMsg);
                        }
                    }else{

                        Bundle b=msg.getData();
                        b.putLong(MessageDbHandler.COL_MESSAGE_ID,addNewMessage(msg.getData(), MessageDbHandler.OLD_ICON));
                        Message innerMsg=processHandler.obtainMessage(INNER_MESSAGE_PROCEED);
                        innerMsg.setData(b);

                        processHandler.sendMessage(innerMsg);

                    }
                    break;
                case MSG_NEW_CALL:
                    if (callQuietEnable) {

                        Calendar c = Calendar.getInstance();
                        Calendar now = new GregorianCalendar(0, 0, 0, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
                        Constants.log(TAG_NAME, "Checking quiet hours. Now: " + now.toString() + " vs "
                                + quiet_hours_before.toString() + " and " + quiet_hours_after.toString());
                        if (now.before(quiet_hours_before) || now.after(quiet_hours_after)) {
                            Constants.log(TAG_NAME, "Time is before or after the quiet hours time. Returning.");
                            addNewCall(msg.getData(),MessageDbHandler.NEW_ICON);
                        } else {
                            Bundle b=msg.getData();
                            b.putLong(MessageDbHandler.COL_CALL_ID, addNewCall(msg.getData(),MessageDbHandler.OLD_ICON));
                            Message innerMsg=processHandler.obtainMessage(INNER_CALL_PROCEED);
                            innerMsg.setData(msg.getData());
                            processHandler.sendMessage(innerMsg);
                        }
                    }else{
                        Bundle b=msg.getData();
                        b.putLong(MessageDbHandler.COL_CALL_ID, addNewCall(msg.getData(),MessageDbHandler.OLD_ICON));
                        Message innerMsg=processHandler.obtainMessage(INNER_CALL_PROCEED);
                        innerMsg.setData(msg.getData());
                        processHandler.sendMessage(innerMsg);
                    }
                    break;
                case MSG_MESSAGE_READY: {
                    Message msgToPebble = Message.obtain();
                    msgToPebble.what = PebbleCenter.PEBBLE_SEND_MESSAGE;
                    msgToPebble.setData(msg.getData());
                    try {
                        rPebbleCenter.send(msgToPebble);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        Constants.log(TAG_NAME, "Error with sending message to PebbleCenter.");
                    }
                }
                    break;
                case MSG_CALL_READY:{
                    Message msgToPebble=Message.obtain();
                    msgToPebble.what=PebbleCenter.PEBBLE_SEND_CALL;
                    msgToPebble.setData(msg.getData());
                    try {
                        rPebbleCenter.send(msgToPebble);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        Constants.log(TAG_NAME,"Error with sending message to PebbleCenter.");
                    }
                    }
                    break;
                case MSG_GET_MESSAGE_TABLE:
                {
                    Message msgToPebble=Message.obtain();
                    msgToPebble.what=PebbleCenter.PEBBLE_SEND_MESSAGE_TABLE;
                    Bundle b=new Bundle();
                    b.putString(MessageDbHandler.TABLE_MESSAGE_NAME, mdb.getTable(MessageDbHandler.TABLE_MESSAGE_NAME,1,10));
                    msgToPebble.setData(b);
                    try {
                        rPebbleCenter.send(msgToPebble);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                    break;
                case MSG_GET_CALL_TABLE:
                {
                    Message msgToPebble=Message.obtain();
                    msgToPebble.what=PebbleCenter.PEBBLE_SEND_CALL_TABLE;
                    Bundle b=new Bundle();
                    b.putString(MessageDbHandler.TABLE_CALL_NAME, mdb.getTable(MessageDbHandler.TABLE_CALL_NAME,1,10));
                    msgToPebble.setData(b);
                    try {
                        rPebbleCenter.send(msgToPebble);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                    break;
                case MSG_GET_MESSAGE:
                {
                    Message innerMsg=processHandler.obtainMessage(INNER_MESSAGE_PROCEED);
                    Bundle b=mdb.getColMessageContent(msg.getData().getString(MessageDbHandler.COL_MESSAGE_ID));
                    if (b==null){
                        b=new Bundle();
                        b.putString(MessageDbHandler.COL_MESSAGE_CONTENT,getString(R.string.messagedbhandler_message_no_more_keep));
                    }
                    innerMsg.setData(b);
                    processHandler.sendMessage(innerMsg);
                }
                    break;
                case MSG_GET_CALL:
                    Message innerMsg=processHandler.obtainMessage(INNER_CALL_PROCEED);
                    Bundle b=mdb.getCall(msg.getData().getString(MessageDbHandler.COL_CALL_ID));
                    if (b==null){
                        b=new Bundle();
                        b.putString(MessageDbHandler.COL_MESSAGE_CONTENT,getString(R.string.messagedbhandler_message_no_more_keep));
                        innerMsg.what=INNER_MESSAGE_PROCEED;
                    }
                    innerMsg.setData(b);
                    processHandler.sendMessage(innerMsg);
                    break;
                case MSG_CLEAN:
                    mdb.cleanAll();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

        private Long addNewMessage(Bundle b, String icon){
            Time nowTime= new Time();
            nowTime.setToNow();
            return mdb.addMessage(nowTime,b.getString(MessageDbHandler.COL_MESSAGE_APP),b.getString(MessageDbHandler.COL_MESSAGE_CONTENT),icon);
        }

        private Long addNewCall(Bundle b , String icon) {
            Time nowTime=new Time();
            nowTime.setToNow();
            return mdb.addCall(nowTime,b.getString(MessageDbHandler.COL_CALL_NUMBER),b.getString(MessageDbHandler.COL_CALL_NAME),icon);
        }
    }

    class InnerThreadHandler extends Handler{
        public InnerThreadHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case INNER_MESSAGE_PROCEED:
                {   PebbleMessage pMessage=processMessage(msg.getData());

                    Message msgReply= messageHandler.obtainMessage(MSG_MESSAGE_READY);
                    Bundle tmpB=new Bundle();
                    tmpB.putSerializable(PROCEED_MSG,pMessage);
                    msgReply.setData(tmpB);
                    messageHandler.sendMessage(msgReply);
                }
                    break;
                case INNER_CALL_PROCEED: {
                    PebbleCall pCall = processCall(msg.getData());
                    Message msgReply = messageHandler.obtainMessage(MSG_CALL_READY);
                    Bundle tmpB = new Bundle();
                    tmpB.putSerializable(PROCEED_CALL, pCall);
                    msgReply.setData(tmpB);
                    messageHandler.sendMessage(msgReply);
                }
                    break;
                default:
                    super.handleMessage(msg);
            }

        }

        private PebbleMessage processMessage(Bundle b){
            String originalMessage;
            if (!isFontDBReady()){
                Constants.log(TAG_NAME,"Font data base is not ready, processing stop!");
                originalMessage= getString(R.string.error_fontbase_notready);
            }else {

                originalMessage = b.getString(MessageDbHandler.COL_MESSAGE_CONTENT);
                Constants.log(TAG_NAME,"original:" + originalMessage + "\n"
                    + b.getString(MessageDbHandler.COL_MESSAGE_APP) + "\n"
                    + String.valueOf(b.getLong(MessageDbHandler.COL_MESSAGE_ID)));
                if (originalMessage.length()>=180){
                    originalMessage=originalMessage.substring(0,179);
                }
            }

            PebbleMessage message = new PebbleMessage();
            message.set_id(b.getLong(MessageDbHandler.COL_MESSAGE_ID));
            // Clear the characterQueue, just in case
            Deque<CharacterMatrix> characterQueue = new ArrayDeque<CharacterMatrix>();
            StringBuilder strBd=new StringBuilder();
            int row = 1;
            int col = 0;
            while(originalMessage.length()>0)
            {

                int codepoint = originalMessage.codePointAt(0);
                if (codepoint == 0) {
                    break;
                }
//                Constants.log("codepoint", "char='" + (char) codepoint + "' code=" + String.valueOf(codepoint));
                if (codepoint <= 127) {
                    if (codepoint == 10) {
                        row++;
                        col = 0;
                        strBd.append(originalMessage.charAt(0));
                    } else {
                        if (col < fChars) {
                            col++;
                            strBd.append(originalMessage.charAt(0));
                        } else {
                            strBd.append('\n');
                            strBd.append(originalMessage.charAt(0));
                            row++;
                            col = 1;
                        }
                    }

                } else {
                    String originalHex;
                    String codepointStr = Integer.toHexString(codepoint).toUpperCase();

                    if (codepointStr.length() < 4) {
                        codepointStr = ("0000" + codepointStr).substring(codepointStr.length());
                    }
//                    Constants.log(TAG_NAME, "codepoint=" + String.valueOf(codepoint) + " codeStr=" + codepointStr);
                    Font font = fdb.getFont(codepointStr);
                    if (font == null) {
                        Constants.log(TAG_NAME, "font is null! codepoint=[" + String.valueOf(codepoint) + "] char=["
                                + (char) codepoint + "]");
                        originalMessage = originalMessage.substring(1);
                        continue;

                    } else {
                        originalHex = font.getHex();
                    }

                    CharacterMatrix c = new CharacterMatrix(originalHex);

                    if (c.getWidthBytes() == 2) {
                        if (col < (fChars-1)) {
                            c.setPos(row, col + 1);
                            strBd.append ("  ");
                            col += 2;
                        } else {
                            strBd.append('\n');
                            strBd.append ("  ");

                            row++;
                            col = 0;
                            c.setPos(row, col + 1);
                            col += 2;
                        }

                    } else {
                        if (col < fChars) {
                            c.setPos(row, col + 1);
                            strBd.append(' ');
                            col++;
                        } else {
                            strBd.append('\n');
                            strBd.append(' ');
                            row++;
                            col = 0;
                            c.setPos(row, col + 1);
                            col++;
                        }

                    }

                    characterQueue.add(c);
                    Constants.log(TAG_NAME,"row:" + String.valueOf(c.getPos()[0]) + " col:" + String.valueOf(c.getPos()[1]));

                }
                originalMessage = originalMessage.substring(1);
            }
            message.setAscMsg(strBd.toString());
 //           Constants.log(TAG_NAME,"set msg:[" + message.getAscMsg() + "]");
            message.setCharacterQueue(characterQueue);

            return message;
        }

        private PebbleCall processCall(Bundle b) {
            // This method not only polls the database but waits for it to be
            // ready
            // in the event it is loading.
            String phone=b.getString(MessageDbHandler.COL_CALL_NUMBER);
            String originalMessage=b.getString(MessageDbHandler.COL_CALL_NAME);
            if (!isFontDBReady()) {
                Constants.log(TAG_NAME, "Database not ready after waiting!");
                originalMessage=phone;
            }

            PebbleCall message = new PebbleCall();
            message.set_id(b.getLong(MessageDbHandler.COL_CALL_ID));
            message.setPhoneNum(phone);
            // Clear the characterQueue, just in case
            Deque<CharacterMatrix> characterQueue = new ArrayDeque<CharacterMatrix>();
            int row = 1;
            int col = 0;

            StringBuilder strBd=new StringBuilder();
            while (originalMessage.length() > 0) {

                int codepoint = originalMessage.codePointAt(0);
                if (codepoint == 0) {
                    break;
                }
                Constants.log("codepoint", "char='" + (char) codepoint + "' code=" + String.valueOf(codepoint));
                if (codepoint <= 127) {
                    if (codepoint == 10) {
                        row++;
                        col = 0;
                        strBd.append(originalMessage.charAt(0));
                    } else {
                        if (col < 8) {
                            if (col == 7) {
                                if (message.getAscMsg().matches("\\w\\z") && originalMessage.matches("\\A\\w")) {
                                    strBd.append("-\n");
                                    row++;
                                    col = 0;
                                }
                            }

                            col++;
                            strBd.append(originalMessage.charAt(0));
                        } else {
                            strBd.append('\n');
                            strBd.append(originalMessage.charAt(0));
                            row++;
                            col = 1;
                        }
                    }

                } else {
                    String originalHex;
                    String codepointStr = Integer.toHexString(codepoint).toUpperCase();

                    if (codepointStr.length() < 4) {
                        codepointStr = ("0000" + codepointStr).substring(codepointStr.length());
                    }
                    Constants.log("codepoint", "codepoint=" + String.valueOf(codepoint) + " codeStr=" + codepointStr);
                    Font font = fdb.getFont(codepointStr);
                    if (font == null) {
                        Constants.log(TAG_NAME, "font is null! codepoint=[" + String.valueOf(codepoint) + "] char=["
                                + (char) codepoint + "]");
                        originalMessage = originalMessage.substring(1);
                        continue;
                    } else {
                        originalHex = font.getHex();
                    }

                    CharacterMatrix c = new CharacterMatrix(originalHex);

                    if (c.getWidthBytes() == 2) {
                        if (col < 7) {
                            c.setPos(row, col + 1);
                            strBd.append ("  ");
                            col += 2;
                        } else {
                            strBd.append('\n');
                            strBd.append ("  ");
                            row++;
                            col = 0;
                            c.setPos(row, col + 1);
                            col += 2;
                        }

                    } else {
                        if (col < 8) {
                            c.setPos(row, col + 1);
                            strBd.append(' ');
                            col++;
                        } else {
                            strBd.append('\n');
                            strBd.append(' ');
                            row++;
                            col = 0;
                            c.setPos(row, col + 1);
                            col++;
                        }

                    }

                    characterQueue.add(c);

                }

                if (row == 3 && (col > 4 || originalMessage.charAt(0) == '\n')) {
                    Constants.log("codepoint", "too many chars!the end char='" + (char) codepoint + "'");
                    strBd.append("...");

                    break;
                }
                originalMessage = originalMessage.substring(1);
            }

            message.setCharacterQueue(characterQueue);
            message.setAscMsg(strBd.toString());
            return message;
        }

        private boolean isFontDBReady(){
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(_context);
            return sharedPref.getBoolean(Constants.DATABASE_READY, false);
        }

    }

    class ProcessThread extends Thread{
        @Override
        public void run() {
            Looper.prepare();
            processHandler=new InnerThreadHandler(Looper.myLooper());
            Looper.loop();
        }
    }

    private void loadPrefs() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        quiet_hours = sharedPref.getBoolean(Constants.PREFERENCE_QUIET_HOURS, false);
        callQuietEnable = sharedPref.getBoolean(Constants.PREFERENCE_CALL_QUIET, false);
        if (quiet_hours) {
            String[] pieces = sharedPref.getString(Constants.PREFERENCE_QUIET_HOURS_BEFORE, "00:00").split(":");
            quiet_hours_before = new GregorianCalendar(0, 0, 0, Integer.parseInt(pieces[0]), Integer.parseInt(pieces[1]));
            pieces = sharedPref.getString(Constants.PREFERENCE_QUIET_HOURS_AFTER, "23:59").split(":");
            quiet_hours_after = new GregorianCalendar(0, 0, 0, Integer.parseInt(pieces[0]), Integer.parseInt(pieces[1]));
        }
        switch (Integer.parseInt(sharedPref.getString(Constants.PREFERENCE_MESSAGE_SCALE,String.valueOf(Constants.MESSAGE_SCALE_SMALL)))){
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }


}
