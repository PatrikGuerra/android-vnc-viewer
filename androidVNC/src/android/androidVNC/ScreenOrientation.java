package android.androidVNC;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;

/**
 * Handles screen orientation configuration, persistence, chooser dialog,
 * shared across activities (connection list and live session).
 */
final class ScreenOrientation {

    private static final String PREFERENCES = "screen_orientation";
    private static final String KEY = "requested_orientation";

    // Constants for screen orientation (compatible with Android 2.3+ / 4.2.2 Jelly Bean)
    static final int MODE_AUTO = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
            ? ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            : ActivityInfo.SCREEN_ORIENTATION_SENSOR;

    static final int MODE_LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    static final int MODE_REVERSE_LANDSCAPE = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
            ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    static final int MODE_PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    static final int MODE_REVERSE_PORTRAIT = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
            ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

    static final int[] MODES = {
        MODE_AUTO,
        MODE_LANDSCAPE,
        MODE_REVERSE_LANDSCAPE,
        MODE_PORTRAIT,
        MODE_REVERSE_PORTRAIT
    };

    private ScreenOrientation() { }

    static void apply(Activity activity) {
        activity.setRequestedOrientation(getMode(activity));
    }

    static int getMode(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        int saved = sp.getInt(KEY, MODE_AUTO);
        for (int mode : MODES) {
            if (saved == mode) return mode;
        }
        return MODE_AUTO;
    }

    static void setMode(Context context, int mode) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putInt(KEY, mode).commit();
    }

    static void showChooser(final Activity activity) {
        int selected = 0;
        int current = getMode(activity);
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i] == current) {
                selected = i;
                break;
            }
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.screen_orientation)
                .setSingleChoiceItems(R.array.screen_orientation_modes, selected,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (which >= 0 && which < MODES.length) {
                                    setMode(activity, MODES[which]);
                                    apply(activity);
                                }
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
