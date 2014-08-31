package yangtsao.pebblemessengerpro.activities;



import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import yangtsao.pebblemessengerpro.R;
import yangtsao.pebblemessengerpro.db.FontDbHandler;
import yangtsao.pebblemessengerpro.db.MessageDbHandler;

/**
 * A simple {@link Fragment} subclass.
 * Use the factory method to
 * create an instance of this fragment.
 *
 */
public class ToolsFragment extends Fragment {

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

            }
        });


        return toolsView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((NavigationActivity) activity).onSectionAttached(positionIndex);
        _context=activity.getApplicationContext();
    }

}
