package yangtsao.pebblemessengerpro;

/**
 * Created by yunshan on 8/16/14.
 */

import android.util.Log;

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
    //----------------------------------------


    //Shared function
    public static void log(String tag, String message) {
        if (Constants.IS_LOGGABLE) {
            Log.d(tag, message);
        }
    }
}
