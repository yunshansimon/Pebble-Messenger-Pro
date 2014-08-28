package yangtsao.pebblemessengerpro.activities;



import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceFragment;
import yangtsao.pebblemessengerpro.R;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class SettingFragment extends PreferenceFragment {

    public SettingFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
    }
}
