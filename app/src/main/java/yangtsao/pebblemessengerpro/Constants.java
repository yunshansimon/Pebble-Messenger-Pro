package yangtsao.pebblemessengerpro;

/**
 * Created by yunshan on 8/16/14.
 */

import android.util.Log;

import java.util.UUID;

public class Constants {
    private Constants() {
        throw new UnsupportedOperationException("This class is non-instantiable, so stop trying!");
    }
    // Accessibility specific items
    public static final String  ACCESSIBILITY_SERVICE                 = "yangtsao.pebblemessengerpro/yangtsao.pebblemessengerpro.services.NotificationService";
    //----------------------------------------

    //system
    public static final boolean IS_LOGGABLE                           = true;
    public static final String  DATABASE_READY                        = "status_database_ready";
    //----------------------------------------

    // Shared preferences
    public static final String  PREFERENCE_NOTIFICATIONS_ONLY         = "pref_notif_only";
    public static final String  PREFERENCE_NO_ONGOING_NOTIF           = "pref_no_ongoing_notif";
    public static final String  PREFERENCE_PACKAGE_LIST               = "pref_package_list";
    public static final String  PREFERENCE_NOTIF_SCREEN_ON            = "pref_notif_screen_on";
    public static final String  PREFERENCE_MESSAGE_SCALE              = "pref_message_scale";
    public static final String  PREFERENCE_CALL_ENABLE                = "pref_call_enable";
    public static final String  PREFERENCE_CALL_QUIET                 = "pref_call_quiet";
    public static final String  PREFERENCE_CALL_SMS_SHORT             = "pref_call_sms_short";
    public static final String  PREFERENCE_CALL_SMS_LONG              = "pref_call_sms_long";
    public static final String  PREFERENCE_QUIET_HOURS                = "pref_dnd_time_enabled";
    public static final String  PREFERENCE_QUIET_HOURS_BEFORE         = "pref_dnd_time_before";
    public static final String  PREFERENCE_QUIET_HOURS_AFTER          = "pref_dnd_time_after";
    public static final String  PREFERENCE_MIN_NOTIFICATION_WAIT      = "pref_notif_timeout";
    //----------------------------------------

    //two sets parameters of message characters
    public static final int MESSAGE_SCALE_SMALL=0;
    public static final int MESSAGE_SCALE_MID=1;
    public static final int MESSAGE_SCALE_LARGE=2;
    public static final int MAX_PACKAGE_LENGTH=120;
    public static final int SMALL_LINE_CONTAIN_CHARS=16;
    public static final int SMALL_PAGE_CONTAIN_LINES=9;
    public static final int MID_LINE_CONTAIN_CHARS=12;
    public static final int MID_PAGE_CONTAIN_LINES=6;
    public static final int LARGE_LINE_CONTAIN_CHARS=9;
    public static final int LARGE_PAGE_CONTAIN_LINES=4;
    //-----------------------------------------
    public static final int RELOAD_PREFERENCE=99;

    //Pebble app communication
    public static final UUID PEBBLE_UUID= UUID.fromString("2d0b18b1-0ee6-41ca-9579-a0c4c6568d93");
    public static final String  INTENT_SEND_PEBBLE_NOTIFICATION       = "com.getpebble.action.SEND_NOTIFICATION";
    public static final String  PEBBLE_APP_URL="";
    //Shared function
    public static void log(String tag, String message) {
        if (Constants.IS_LOGGABLE) {
            Log.d(tag, message);
        }
    }
}
