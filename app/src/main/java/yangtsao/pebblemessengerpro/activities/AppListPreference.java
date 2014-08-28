package yangtsao.pebblemessengerpro.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.ListPreference;

/**
 * Created by yunshansimon on 14-8-28.
 */
public class AppListPreference extends ListPreference {
    public AppListPreference(Context context) {
        super(context);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
    }

    @Override
    public String getValue() {
        return super.getValue();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

    }
}
