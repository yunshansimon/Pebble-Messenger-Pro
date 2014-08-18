package yangtsao.pebblemessengerpro.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import yangtsao.pebblemessengerpro.Constants;

/**
 * Created by yunshan on 8/17/14.
 */
public class MessageDbHandler extends SQLiteOpenHelper {
    public static final String DBNAME   ="messagedb.sqlite";
    public static final int VERSION     =1;
    public static final String TABLE_MESSAGE_NAME="messages";
    public static final String TABLE_CALL_NAME="calls";

    private final Context _context;

    private SQLiteDatabase _db;

    public MessageDbHandler(Context context) {
        super(context, DBNAME, null, VERSION);
        this._context = context;
    }

    public void onCreate(SQLiteDatabase db) {
        final SQLiteDatabase dbThread = db;

        new Thread(new Runnable() {
            @Override
            public void run() {
                Constants.log("Database", "Starting messagedb creation");
                String CREATE_FONT_TABLE = "CREATE TABLE " + TABLE_HEX + "(" + KEY_CODEPOINT + " TEXT PRIMARY KEY,"
                        + KEY_HEX + " TEXT " + ")";
            }

        }).start();
    }
}
