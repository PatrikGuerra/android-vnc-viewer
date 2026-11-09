package android.androidVNC;

import java.util.ArrayList;
import java.util.Collections;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

final class AutoConnectSettings {
    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("connect_on_open", Context.MODE_PRIVATE);
    }

    static boolean enabled(Context context) { return preferences(context).getBoolean("enabled", false); }
    static long connectionId(Context context) { return preferences(context).getLong("connection_id", -1); }
    static int interval(Context context) {
        int seconds = preferences(context).getInt("interval_seconds", 10);
        return seconds >= 1 && seconds <= 3600 ? seconds : 10;
    }
    static void disable(Context context) { preferences(context).edit().putBoolean("enabled", false).commit(); }

    static boolean valid(ConnectionBean bean) {
        if (bean == null || bean.isNew() || bean.getAddress() == null
                || bean.getAddress().trim().length() == 0 || bean.getPort() < 1 || bean.getPort() > 65535) return false;
        try { COLORMODEL.valueOf(bean.getColorModel()); bean.getScaleMode(); }
        catch (RuntimeException invalid) { return false; }
        return true;
    }

    static String label(ConnectionBean profile) {
        String address = profile.getAddress() + ":" + profile.getPort();
        String name = profile.getNickname() == null ? "" : profile.getNickname().trim();
        return name.length() == 0 ? address : name + " (" + address + ")";
    }

    static void show(final androidVNC activity) {
        final ArrayList<ConnectionBean> profiles = new ArrayList<ConnectionBean>();
        ConnectionBean.getAll(activity.getDatabaseHelper().getReadableDatabase(), ConnectionBean.GEN_TABLE_NAME,
                profiles, ConnectionBean.newInstance);
        Collections.sort(profiles);
        final ArrayList<ConnectionBean> saved = new ArrayList<ConnectionBean>();
        ArrayList<String> labels = new ArrayList<String>();
        int selected = 0;
        for (ConnectionBean profile : profiles) {
            if (!valid(profile)) continue;
            if (profile.get_Id() == connectionId(activity)) selected = saved.size();
            saved.add(profile);
            labels.add(label(profile));
        }
        final CheckBox enabled = new CheckBox(activity);
        enabled.setText(R.string.auto_enable);
        enabled.setChecked(enabled(activity) && !saved.isEmpty());
        enabled.setEnabled(!saved.isEmpty());
        final Spinner connections = new Spinner(activity);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        connections.setAdapter(adapter);
        connections.setSelection(selected);
        final EditText interval = new EditText(activity);
        interval.setInputType(InputType.TYPE_CLASS_NUMBER);
        interval.setSingleLine(true);
        interval.setText(Integer.toString(interval(activity)));
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(16 * activity.getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(enabled);
        TextView connectionLabel = new TextView(activity);
        connectionLabel.setText(saved.isEmpty() ? R.string.auto_no_profiles : R.string.auto_profile);
        layout.addView(connectionLabel);
        layout.addView(connections);
        TextView intervalLabel = new TextView(activity);
        intervalLabel.setText(R.string.auto_interval);
        layout.addView(intervalLabel);
        layout.addView(interval);
        TextView help = new TextView(activity);
        help.setText(R.string.auto_settings_help);
        layout.addView(help);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(layout);
        final AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(R.string.auto_title)
                .setView(scroll).setPositiveButton(R.string.auto_save, null)
                .setNegativeButton(android.R.string.cancel, null).create();
        dialog.show();
        Button save = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                boolean turnOn = enabled.isEnabled() && enabled.isChecked();
                int seconds;
                try { seconds = Integer.parseInt(interval.getText().toString().trim()); }
                catch (NumberFormatException error) { seconds = 0; }
                if (seconds < 1 || seconds > 3600) {
                    if (turnOn) {
                        interval.setError(activity.getString(R.string.auto_invalid_interval));
                        return;
                    }
                    seconds = AutoConnectSettings.interval(activity);
                }
                int index = connections.getSelectedItemPosition();
                if (turnOn && (index < 0 || index >= saved.size())) return;
                preferences(activity).edit().putBoolean("enabled", turnOn)
                        .putLong("connection_id", turnOn ? saved.get(index).get_Id() : connectionId(activity))
                        .putInt("interval_seconds", seconds).commit();
                if (!turnOn) activity.cancelAutoConnect();
                dialog.dismiss();
            }
        });
    }

    private AutoConnectSettings() { }
}
