package yangtsao.pebblemessengerpro.activities;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.http.protocol.HTTP;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import yangtsao.pebblemessengerpro.Constants;
import yangtsao.pebblemessengerpro.R;

/**
 * Created by yunshansimon on 14-8-28.
 */
public class AppListPreference extends DialogPreference {

    private static final String CLASS_TAG="AppListClass";
    private Context _context;
    private ListView lvPackageInfo;
    private ProgressBar pbInworking;

    public AppListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        _context=context;
        setPersistent(false);
        setDialogLayoutResource(R.layout.app_list_main);
        setPositiveButtonText(R.string.ok);
        setNegativeButtonText(R.string.cancel);
    }

    @Override
    protected View onCreateDialogView() {

        return super.onCreateDialogView();

    }

    @Override
    protected void onBindDialogView(View view) {
        lvPackageInfo=(ListView)view.findViewById(R.id.listView);
        pbInworking=(ProgressBar) view.findViewById(R.id.progressBar);
        new LoadAppsTask().execute(LoadAppsTask.SORT_BY_NAME);
        super.onBindDialogView(view);
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
            ListViewHolder viewHolder;
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
            rowView.findViewById(R.id.chkEnabled).performClick();

        }
    }
    private class LoadAppsTask extends AsyncTask<Integer, Integer, List<PackageInfo>> {
        public ArrayList<String> selected;
        public static final int  SORT_BY_NAME      = 0;
        public static final int  SORT_BY_NAME_DESC = 3;

        @Override
        protected List<PackageInfo> doInBackground(Integer... params) {
            List<PackageInfo> pkgAppsList = _context.getPackageManager().getInstalledPackages(0);
            List<PackageInfo> selectedAppsList = new ArrayList<PackageInfo>();
            List<PackageInfo> suggestedAppsList = new ArrayList<PackageInfo>();
            selected = new ArrayList<String>();

            String packageList;
            packageList=getSharedPreferences().getString(Constants.PREFERENCE_PACKAGE_LIST,"");
            for (String strPackage : packageList.split(",")) {
                // only add the ones that are still installed, providing cleanup
                // and faster speeds all in one!
                Iterator<PackageInfo> iter = pkgAppsList.iterator();
                while (iter.hasNext()) {
                    PackageInfo info = iter.next();
                    if (info.packageName.equalsIgnoreCase(strPackage)) {
                        selectedAppsList.add(info);
                        selected.add(strPackage);
                        iter.remove();
                        break;
                    }
                }

            }

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            //
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Say hello to world!");
            sendIntent.setType(HTTP.PLAIN_TEXT_TYPE);
            List<ResolveInfo> activities = _context.getPackageManager().queryIntentActivities(sendIntent, 0);
            Constants.log(CLASS_TAG, "There are " + String.valueOf(activities.size()) + " packages were find!");
            for (ResolveInfo info : activities) {

                Constants.log(CLASS_TAG, "Suggested APP [" + info.activityInfo.packageName + "]");
                Iterator<PackageInfo> iter = pkgAppsList.iterator();
                while (iter.hasNext()) {
                    PackageInfo pinfo = iter.next();
                    if (pinfo.packageName.equalsIgnoreCase(info.activityInfo.packageName)) {
                        suggestedAppsList.add(pinfo);
                        iter.remove();
                        break;
                    }
                }
            }

            Constants.log(CLASS_TAG, "Suggested APP have " + String.valueOf(suggestedAppsList.size()) + "!");
            switch (params[0]) {
                case LoadAppsTask.SORT_BY_NAME:
                    PackageNameComparator comparer1 = new PackageNameComparator();
                    if (!selectedAppsList.isEmpty()) {
                        Collections.sort(selectedAppsList, comparer1);
                    }
                    if (!suggestedAppsList.isEmpty()) {
                        Collections.sort(suggestedAppsList, comparer1);
                    }
                    Collections.sort(pkgAppsList, comparer1);
                    break;
                case LoadAppsTask.SORT_BY_NAME_DESC:
                    PackageNameDescComparator comparer2 = new PackageNameDescComparator();
                    if (!selectedAppsList.isEmpty()) {
                        Collections.sort(selectedAppsList, comparer2);
                    }
                    if (!suggestedAppsList.isEmpty()) {
                        Collections.sort(suggestedAppsList, comparer2);
                    }
                    Collections.sort(pkgAppsList, comparer2);
                    break;

            }
            pkgAppsList.addAll(0, suggestedAppsList);
            pkgAppsList.addAll(0, selectedAppsList);
            return pkgAppsList;
        }

        @Override
        protected void onPostExecute(List<PackageInfo> pkgAppsList) {
            lvPackageInfo.setAdapter(new packageAdapter(_context, pkgAppsList
                    .toArray(new PackageInfo[pkgAppsList.size()]), selected));
            pbInworking.setVisibility(View.GONE);

        }
        protected class PackageNameComparator implements Comparator<PackageInfo> {

            @Override
            public int compare(PackageInfo leftPackage, PackageInfo rightPackage) {

                String leftName = leftPackage.applicationInfo.loadLabel(_context.getPackageManager()).toString();
                String rightName = rightPackage.applicationInfo.loadLabel(_context.getPackageManager()).toString();

                return leftName.compareToIgnoreCase(rightName);
            }
        }
        public class PackageNameDescComparator implements Comparator<PackageInfo> {

            @Override
            public int compare(PackageInfo leftPackage, PackageInfo rightPackage) {

                String leftName = leftPackage.applicationInfo.loadLabel(_context.getPackageManager()).toString();
                String rightName = rightPackage.applicationInfo.loadLabel(_context.getPackageManager()).toString();

                return -leftName.compareToIgnoreCase(rightName);
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String selectedPackages = "";
            ArrayList<String> tmpArray = new ArrayList<String>();
            if (lvPackageInfo == null || lvPackageInfo.getAdapter() == null) {
                return;
            }
            for (String strPackage : ((packageAdapter) lvPackageInfo.getAdapter()).selected) {
                if (!strPackage.isEmpty()) {
                    if (!tmpArray.contains(strPackage)) {
                        tmpArray.add(strPackage);
                        selectedPackages += strPackage + ",";
                    }
                }
            }
            PreferenceManager pm=getPreferenceManager();
            SharedPreferences.Editor editor = pm.getDefaultSharedPreferences(_context).edit();
            editor.putString
                    (Constants.PREFERENCE_PACKAGE_LIST, selectedPackages);

            editor.apply();
            File watchFile = new File(_context.getFilesDir() + "PrefsChanged.none");
            if (!watchFile.exists()) {
                try {
                    watchFile.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            watchFile.setLastModified(System.currentTimeMillis());
        }
        super.onDialogClosed(positiveResult);
    }


}
