package yangtsao.pebblemessengerpro.activities;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import yangtsao.pebblemessengerpro.Constants;
import yangtsao.pebblemessengerpro.R;
import yangtsao.pebblemessengerpro.db.FontDbHandler;
import yangtsao.pebblemessengerpro.db.MessageDbHandler;
import yangtsao.pebblemessengerpro.services.MessageProcessingService;

/**
 * A simple {@link Fragment} subclass.
 * Use the factory method to
 * create an instance of this fragment.
 *
 */
public class ToolsFragment extends Fragment {
    private static final int REQUEST_SEND_MESSAGE=1;
    private static final int REQUEST_SEND_CALL=2;

    private static final String MESSAGE_BODY="message";
    private static final String CALL_NAME="name";
    private static final String CALL_NUM="number";
    private final String LOG_TAG="TestMessage";

    private Messenger rMessageProcessHandler;
    private final ServiceConnection connToMessageProcess =new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            rMessageProcessHandler=new Messenger(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            rMessageProcessHandler=null;
        }
    };

    private Context _context;
    private static final int positionIndex=2;

    public ToolsFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View toolsView=inflater.inflate(R.layout.fragment_tools, container, false);
        toolsView.findViewById(R.id.button_rebuild_font).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(_context, R.string.tools_toast_rebuild_font, Toast.LENGTH_LONG).show();
                (new FontDbHandler(_context)).rebuild();
            }
        });
        toolsView.findViewById(R.id.button_clean_message_cache).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(_context, R.string.tools_toast_clean_cache, Toast.LENGTH_LONG).show();
                (new MessageDbHandler(_context)).cleanAll();
            }
        });
        toolsView.findViewById(R.id.button_test_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm=getFragmentManager();
                SendMessageFragment sf=new SendMessageFragment();
                sf.setTargetFragment(fm.findFragmentById(R.id.toolsfragment),REQUEST_SEND_MESSAGE);
                sf.show(fm,"Test Messenger");
            }
        });
        toolsView.findViewById(R.id.button_test_call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm=getFragmentManager();
                SendCallFragment sf=new SendCallFragment();
                sf.setTargetFragment(fm.findFragmentById(R.id.toolsfragment),REQUEST_SEND_CALL);
                sf.show(fm,"Test Call");
            }
        });

        return toolsView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((NavigationActivity) activity).onSectionAttached(positionIndex);
        _context=activity.getApplicationContext();
        _context.bindService(new Intent(activity, MessageProcessingService.class), connToMessageProcess,
                Context.BIND_AUTO_CREATE);
    }

    public static class SendMessageFragment extends DialogFragment{
        private TextView etMessage;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View dialogSendMessage=inflater.inflate(R.layout.dialog_send_message, null);
            etMessage=(TextView)dialogSendMessage.findViewById(R.id.TextMessage);
            builder.setView(dialogSendMessage)
                    .setPositiveButton(R.string.dialog_button_send, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                            Intent data=new Intent();
                            data.putExtra(MESSAGE_BODY,etMessage.getText().toString());
                            getTargetFragment().onActivityResult(getTargetRequestCode(),0,data);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            dialog.dismiss();
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }
    public static class SendCallFragment extends DialogFragment{
        private TextView etNumber;
        private TextView etName;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View dialogSendCall=inflater.inflate(R.layout.dialog_send_call, null);
            etNumber=(TextView)dialogSendCall.findViewById(R.id.callnumber);
            etName=(TextView)dialogSendCall.findViewById(R.id.callname);
            builder.setView(dialogSendCall)
                    .setPositiveButton(R.string.dialog_button_send, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                            Intent data=new Intent();
                            data.putExtra(CALL_NAME,etName.getText().toString());
                            data.putExtra(CALL_NUM,etNumber.getText().toString());
                            getTargetFragment().onActivityResult(getTargetRequestCode(),0,data);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            dialog.dismiss();
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Message msg=Message.obtain();
        Bundle b=new Bundle();
        switch (requestCode){
            case REQUEST_SEND_MESSAGE:
                msg.what=MessageProcessingService.MSG_NEW_MESSAGE;
                b.putString(MessageDbHandler.COL_MESSAGE_APP, LOG_TAG);
                b.putString(MessageDbHandler.COL_MESSAGE_CONTENT,data.getStringExtra(MESSAGE_BODY));
                Toast.makeText(_context,R.string.tools_toast_send_message,Toast.LENGTH_SHORT);
                break;
            case REQUEST_SEND_CALL:
                msg.what=MessageProcessingService.MSG_NEW_CALL;
                b.putString(MessageDbHandler.COL_CALL_NUMBER,data.getStringExtra(CALL_NUM));
                b.putString(MessageDbHandler.COL_CALL_NAME,data.getStringExtra(CALL_NAME));
                break;
        }
        try {
            rMessageProcessHandler.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            Constants.log(LOG_TAG,"Error when sending message to MessageProcessingService.");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
