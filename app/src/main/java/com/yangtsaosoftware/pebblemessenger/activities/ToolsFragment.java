
/*
 * Pebble Messenger is used to display non-english message on Pebble.
 * Copyright (C) 2014  Yang Tsao
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.yangtsaosoftware.pebblemessenger.activities;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
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
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.yangtsaosoftware.pebblemessenger.Constants;
import com.yangtsaosoftware.pebblemessenger.R;
import com.yangtsaosoftware.pebblemessenger.db.FontDbHandler;
import com.yangtsaosoftware.pebblemessenger.db.MessageDbHandler;
import com.yangtsaosoftware.pebblemessenger.services.MessageProcessingService;
import com.yangtsaosoftware.pebblemessenger.services.NotificationService;
import com.yangtsaosoftware.pebblemessenger.services.PebbleCenter;

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
    private static final String LOG_TAG="TestMessage";

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
    private FontDbHandler fd;
    private MessageDbHandler md;
    private SendMessageFragment sf;
    private SendCallFragment sc;

    public ToolsFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View toolsView=inflater.inflate(R.layout.fragment_tools, container, false);
        fd=new FontDbHandler(_context);
        fd.open();
        md=new MessageDbHandler(_context);
        md.open();
        sf=new SendMessageFragment();
        sf.setTargetFragment(this,REQUEST_SEND_MESSAGE);
        sc=new SendCallFragment();
        sc.setTargetFragment(this, REQUEST_SEND_CALL);
        toolsView.findViewById(R.id.button_rebuild_font).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(_context, R.string.tools_toast_rebuild_font, Toast.LENGTH_LONG).show();
                fd.rebuild();
            }
        });
        toolsView.findViewById(R.id.button_clean_message_cache).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(_context, R.string.tools_toast_clean_cache, Toast.LENGTH_LONG).show();
                md.rebuildAll();
            }
        });
        toolsView.findViewById(R.id.button_test_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sf.show(getFragmentManager(),"Test Messenger");
            }
        });
        toolsView.findViewById(R.id.button_test_call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sc.show(getFragmentManager(), "Test Call");
            }
        });
        toolsView.findViewById(R.id.button_restart_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _context.stopService(new Intent(getActivity(), NotificationService.class));
                _context.stopService(new Intent(getActivity(), MessageProcessingService.class));
                _context.stopService(new Intent(getActivity(), PebbleCenter.class));
                Toast.makeText(_context,R.string.tools_toast_restart_service,Toast.LENGTH_LONG).show();
            }
        });

        return toolsView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((NavigationActivity) activity).onSectionAttached(positionIndex);
        _context=activity.getBaseContext();

    }

    @Override
    public void onResume() {
        super.onResume();
        _context.bindService(new Intent(getActivity(), MessageProcessingService.class), connToMessageProcess,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        _context.unbindService(connToMessageProcess);
    }

    @Override
    public void onDetach() {

        super.onDetach();
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
                            Constants.log(LOG_TAG,etMessage.getText().toString());
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

        switch (requestCode){
            case REQUEST_SEND_MESSAGE: {
                Message msg=Message.obtain();
                Bundle b=new Bundle();
                msg.what = MessageProcessingService.MSG_NEW_MESSAGE;
                b.putString(MessageDbHandler.COL_MESSAGE_APP, LOG_TAG);
                b.putString(MessageDbHandler.COL_MESSAGE_CONTENT, data.getStringExtra(MESSAGE_BODY));
                try {
                    msg.setData(b);
                    rMessageProcessHandler.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Constants.log(LOG_TAG,"Error when sending message to MessageProcessingService.");
                }
                Toast.makeText(_context, R.string.tools_toast_send_message, Toast.LENGTH_LONG).show();
            }
                break;
            case REQUEST_SEND_CALL:
                Intent inner_intent=new Intent(MessageProcessingService.class.getName());
                inner_intent.putExtra(Constants.BROADCAST_COMMAND,Constants.BROADCAST_CALL_INCOMING);
                inner_intent.putExtra(Constants.BROADCAST_PHONE_NUM,data.getStringExtra(CALL_NUM));
                inner_intent.putExtra(Constants.BROADCAST_NAME,data.getStringExtra(CALL_NAME));
                LocalBroadcastManager.getInstance(getActivity().getBaseContext()).sendBroadcast(inner_intent);
                Toast.makeText(_context,R.string.tools_toast_send_call,Toast.LENGTH_LONG).show();
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        fd.close();
        md.close();
        super.onDestroy();
    }
}
