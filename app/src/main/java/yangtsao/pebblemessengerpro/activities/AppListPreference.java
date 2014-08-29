package yangtsao.pebblemessengerpro.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;

import yangtsao.pebblemessengerpro.Constants;
import yangtsao.pebblemessengerpro.R;

/**
 * Created by yunshansimon on 14-8-28.
 */
public class AppListPreference extends DialogPreference {
    private String _appList;
    private static final String CLASS_TAG="AppListClass";
    private Context _context;


    public AppListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        _context=context;
        setPositiveButtonText(R.string.ok);
        setNegativeButtonText(R.string.cancel);
    }

    @Override
    protected View onCreateDialogView() {

        return super.onCreateDialogView();

    }
    private class packageAdapter extends ArrayAdapter<PackageInfo> implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {
        private final Context       context;
        private final PackageInfo[] packages;
        public ArrayList<String>    selected;

        public packageAdapter(Context context, PackageInfo[] packages, ArrayList<String> selected) {
            super(context, R.layout.list_preference_layout, packages);
            this.context = context;
            this.packages = packages;
            this.selected = selected;
        }

        @Override
        public View getView(int position, View rowView, ViewGroup parent) {
            ListViewHolder viewHolder = null;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.list_preference_layout, parent, false);

                viewHolder = new ListViewHolder();

                viewHolder.textView = (TextView) rowView.findViewById(R.id.tvPackage);
                viewHolder.imageView = (ImageView) rowView.findViewById(R.id.ivIcon);
                viewHolder.chkEnabled = (CheckBox) rowView.findViewById(R.id.chkEnabled);
                viewHolder.chkEnabled.setOnCheckedChangeListener(this);

                rowView.setOnClickListener(this);
                rowView.setTag(viewHolder);
            } else {
                viewHolder = (ListViewHolder) rowView.getTag();
                // viewHolder.chkEnabled.rem
            }
            PackageInfo info = packages[position];

            viewHolder.textView.setText(info.applicationInfo.loadLabel(_context.getPackageManager()).toString());
            viewHolder.imageView.setImageDrawable(info.applicationInfo.loadIcon(_context.getPackageManager()));
            viewHolder.chkEnabled.setTag(info.packageName);

            boolean boolSelected = false;

            for (String strPackage : selected) {
                if (info.packageName.equalsIgnoreCase(strPackage)) {

                    boolSelected = true;
                    break;
                }
            }
            viewHolder.chkEnabled.setChecked(boolSelected);

            return rowView;
        }
        public class ListViewHolder {
            public TextView textView;
            public CheckBox chkEnabled;
            public ImageView imageView;
        }

        public class PackageNameComparator implements Comparator<PackageInfo> {

            @Override
            public int compare(PackageInfo leftPackage, PackageInfo rightPackage) {

                String leftName = leftPackage.applicationInfo.loadLabel(_context.getPackageManager()).toString();
                String rightName = rightPackage.applicationInfo.loadLabel(_context.getPackageManager()).toString();

                return leftName.compareToIgnoreCase(rightName);
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton chkEnabled, boolean newState) {

            String strPackage = (String) chkEnabled.getTag();

            if (strPackage.isEmpty()) {
                return;
            }
            Constants.log(CLASS_TAG, "Check changed on " + strPackage);

            if (newState) {
                if (!selected.contains(strPackage)) {
                    selected.add(strPackage);
                }
            } else {
                while (selected.contains(strPackage)) {
                    selected.remove(strPackage);
                }
            }
            Constants.log(CLASS_TAG, "Selected count is: " + String.valueOf(selected.size()));

        }

        @Override
        public void onClick(View rowView) {
            ((CheckBox) rowView.findViewById(R.id.chkEnabled)).performClick();

        }
    }
}
