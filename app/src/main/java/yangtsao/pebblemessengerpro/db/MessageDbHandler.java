package yangtsao.pebblemessengerpro.db;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.os.Bundle;
import android.text.format.Time;
import yangtsao.pebblemessengerpro.R;

import yangtsao.pebblemessengerpro.Constants;

/**
 * Created by yunshan on 8/17/14.
 */
public class MessageDbHandler extends SQLiteOpenHelper {
    private static final String DBNAME   ="messagedb.sqlite";
    private static final int VERSION     =1;
    //message table
    public static final String NEW_ICON="!";
    public static final String OLD_ICON=".";
    public static final String TABLE_MESSAGE_NAME="MESSAGES";
    public static final String COL_MESSAGE_ID="ID";
    public static final String COL_MESSAGE_TIME="RTIME";
    public static final String COL_MESSAGE_APP="APPNAME";
    public static final String COL_MESSAGE_CONTENT="CONTENT";
    public static final String COL_MESSAGE_NEW="NEW";

    //call table
    public static final String TABLE_CALL_NAME="CALLS";
    public static final String COL_CALL_ID="ID";
    public static final String COL_CALL_TIME="RTIME";
    public static final String COL_CALL_NUMBER="NUMBER";
    public static final String COL_CALL_NAME="NAME";
    public static final String COL_CALL_NEW="NEW";

    private final Context _context;
    private final int MAXSTORE=10;
    private SQLiteDatabase _db;

    public MessageDbHandler(Context context) {
        super(context, DBNAME, null, VERSION);
        this._context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final SQLiteDatabase dbThread = db;

        new Thread(new Runnable() {
            @Override
            public void run() {
                Constants.log("Database", "Starting messagedb creation");
                String CREATE_FONT_TABLE = "CREATE TABLE " + TABLE_MESSAGE_NAME + "(" + COL_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                        + COL_MESSAGE_TIME + " TEXT," + COL_MESSAGE_APP + " TEXT," + COL_MESSAGE_CONTENT + " TEXT "  + COL_MESSAGE_NEW  +  " TEXT)";
                dbThread.execSQL(CREATE_FONT_TABLE);
                CREATE_FONT_TABLE ="CREATE TABLE " + TABLE_CALL_NAME + "(" + COL_CALL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                        + COL_CALL_TIME + " TEXT," + COL_CALL_NUMBER + " TEXT," + COL_CALL_NAME + " TEXT "  + COL_CALL_NEW  + " TEXT)";
                dbThread.execSQL(CREATE_FONT_TABLE);
            }

        }).start();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALL_NAME);

        // Create tables again
        onCreate(db);
    }


    public void open() throws SQLException {
        _db = this.getWritableDatabase();

        // verifyIntegrity();
    }

    @Override
    public void close() {
        _db.close();
        super.close();
    }

    private void keepMaxRecord(String tableName, int maxRecord){
        String countQuery = "SELECT  * FROM " + tableName;
        Cursor cursor = _db.rawQuery(countQuery, null);
        if (cursor==null){
            return ;
        }
        int cnt = cursor.getCount();
        if (cnt>maxRecord) {
            cursor.moveToFirst();
            int did=cursor.getInt(0);
            cursor.close();
            String deleteQuery="DELETE FROM " + tableName + " WHERE ID =" + String.valueOf(did);
            _db.execSQL(deleteQuery);
        }else {
            cursor.close();
        }
    }

    public long addMessage(Time timeStamp, String appName, String messageBody, String newIcon){
        keepMaxRecord(TABLE_MESSAGE_NAME,MAXSTORE);
        ContentValues values = new ContentValues();
        values.put(COL_MESSAGE_TIME,timeStamp.format2445());
        values.put(COL_MESSAGE_APP,appName);
        values.put(COL_MESSAGE_CONTENT,messageBody);
        values.put(COL_MESSAGE_NEW, newIcon);
        return _db.insertOrThrow(TABLE_MESSAGE_NAME,null,values);

    }

    public long addCall(Time timeStamp, String callNumber, String name , String newIcon){
        keepMaxRecord(TABLE_CALL_NAME,MAXSTORE);
        ContentValues values = new ContentValues();
        values.put(COL_CALL_TIME,timeStamp.format2445());
        values.put(COL_CALL_NUMBER,callNumber);
        values.put(COL_CALL_NAME,name);
        values.put(COL_CALL_NEW, newIcon);
        return _db.insertOrThrow(TABLE_CALL_NAME,null,values);
    }

    public String getTable(String tableName, int fromPos,int toPos){
        StringBuilder messageTable= new StringBuilder();
        String selectQuery="SELECT * FROM " + tableName + " ORDER BY ID DESC";
        Cursor cursor=_db.rawQuery(selectQuery,null);
        if (cursor==null){
            return "";
        }
        if (cursor.getCount()>0){
            cursor.moveToFirst();
            int counter=1;
            do{
                if (counter>=fromPos && counter<= toPos) {
                    messageTable.append(cursor.getInt(0));
                    messageTable.append(" ");
                    Time rTime=new Time();
                    rTime.parse(cursor.getString(1));
                    Time nowTime=new Time();
                    nowTime.setToNow();
                    long diff=(nowTime.toMillis(true)-rTime.toMillis(true))/1000;
                    if ((int)(diff/(24*3600))>0){
                        messageTable.append("[>" + String.valueOf((int)(diff/(24*3600))) + " Days]");
                    }else if ((int)(diff/3600)>0){
                        messageTable.append("[>" + String.valueOf((int)(diff/(3600))) + " Hours]");
                    }else if ((int)(diff/60)>0){
                        messageTable.append("[>" + String.valueOf((int)(diff/(60))) + " Mins]");
                    }else {
                        messageTable.append("[<1 Mins]");
                    }
                    messageTable.append(" " + cursor.getString(2) + cursor.getString(4));
                    messageTable.append("\n");
                }else if (counter>toPos){
                   break;
                }
                counter++;
            } while (cursor.moveToNext());

        }
        cursor.close();
        return messageTable.toString();
    }

    public Bundle getColMessageContent(String ID){
        String selectQuery="SELECT * FROM " + TABLE_MESSAGE_NAME + " WHERE ID=" + ID;
        Cursor cursor=_db.rawQuery(selectQuery,null);
        Bundle content=new Bundle();
        if (cursor==null){

            return null;
        }
        if (cursor.getCount()>0){
            cursor.moveToFirst();
            content.putString(MessageDbHandler.COL_MESSAGE_CONTENT,cursor.getString(3));
            if (cursor.getString(4).equalsIgnoreCase(NEW_ICON)) {
                ContentValues values = new ContentValues();
                values.put(COL_MESSAGE_NEW, OLD_ICON);
                _db.update(TABLE_MESSAGE_NAME, values, "ID =" + String.valueOf(ID), null);
            }
        }else{
            content=null;
        }
        cursor.close();
        return content;
    }

    public Bundle getCall(String ID){
        String selectQuery="SELECT * FROM " + TABLE_CALL_NAME + " WHERE ID=" + ID;
        Cursor cursor=_db.rawQuery(selectQuery,null);
        Bundle content=new Bundle();
        if (cursor==null){
            return null;
        }
        if (cursor.getCount()>0){
            cursor.moveToFirst();
            content.putString(MessageDbHandler.COL_CALL_NUMBER,cursor.getString(2));
            content.putString(MessageDbHandler.COL_CALL_NAME,cursor.getString(3));
            if (cursor.getString(4).equalsIgnoreCase(NEW_ICON)) {
                ContentValues values = new ContentValues();
                values.put(COL_CALL_NEW, OLD_ICON);
                _db.update(TABLE_CALL_NAME, values, "ID =" + String.valueOf(ID), null);
            }
        }else{
            content=null;
        }
        cursor.close();
        return content;
    }

    public void cleanAll(){
        onUpgrade(_db,VERSION,VERSION);
    }
}
