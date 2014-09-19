package yangtsao.pebblemessengerpro.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;

import yangtsao.pebblemessengerpro.Constants;
import yangtsao.pebblemessengerpro.R;
import yangtsao.pebblemessengerpro.services.MessageProcessingService;
import yangtsao.pebblemessengerpro.services.PebbleCenter;

/**
 * Created by yunshansimon on 14-9-17.
 */
public class CallStateHandler extends BroadcastReceiver {
    final static String LOG_TAG ="CallStateHandler";
    @Override
    public void onReceive(Context context, Intent intent) {

        String state=intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if(TelephonyManager.EXTRA_STATE_RINGING.equals(state)){
            String incomingNumber=intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Constants.log(LOG_TAG, "A new call is coming:" + incomingNumber);
            Intent inner_intent=new Intent(MessageProcessingService.class.getName());
            inner_intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_CALL_INCOMING);
            if (incomingNumber != null && !incomingNumber.isEmpty()) {
                inner_intent.putExtra(Constants.BROADCAST_PHONE_NUM,incomingNumber);
                inner_intent.putExtra(Constants.BROADCAST_NAME,queryNameByNum(context,incomingNumber));
            } else {
                inner_intent.putExtra(Constants.BROADCAST_PHONE_NUM,"0");
                inner_intent.putExtra(Constants.BROADCAST_NAME,context.getString(R.string.notificationservice_privateNumber));
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(inner_intent);
        }else if(TelephonyManager.EXTRA_STATE_IDLE.equals(state)){
            Constants.log(LOG_TAG, "Call is idle");

            Intent inner_intent=new Intent(PebbleCenter.class.getName());
            inner_intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_CALL_IDLE);
            LocalBroadcastManager.getInstance(context).sendBroadcast(inner_intent);
        }else if(TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)){
            Constants.log(LOG_TAG, "Call is off hook");

            Intent inner_intent=new Intent(PebbleCenter.class.getName());
            inner_intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_CALL_HOOK);
            LocalBroadcastManager.getInstance(context).sendBroadcast(inner_intent);
        }
    }

    public  String queryNameByNum(Context context, String num) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(num));
        Cursor cursor = context.getContentResolver().query(uri, new String[] {
                ContactsContract.PhoneLookup.DISPLAY_NAME
        }, null, null, null);
        String nameString=context.getString(R.string.notificationservice_unknownperson);
        if (cursor!=null){
            if (cursor.moveToFirst()){
                int columnNumberId=cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                nameString=cursor.getString(columnNumberId);
            }
            cursor.close();
        }
        return nameString;
    }
}
