package android.androidVNC;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Handles screen orientation configuration, persistence, chooser dialog,
 * and 90-degree step rotation across activities (connection list and live session).
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

    // 90-degree clockwise cycle: Landscape -> Portrait -> Reverse Landscape -> Reverse Portrait -> Landscape
    static final int[] ROTATE_CYCLE = {
        MODE_LANDSCAPE,
        MODE_PORTRAIT,
        MODE_REVERSE_LANDSCAPE,
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

    /**
     * Rotates 90 degrees clockwise to the next orientation in the cycle:
     * Landscape -> Portrait -> Reverse Landscape -> Reverse Portrait -> Landscape.
     */
    static void rotate90(Activity activity) {
        int current = getMode(activity);
        if (current == MODE_AUTO) {
            int orientation = activity.getResources().getConfiguration().orientation;
            Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int rotation = Surface.ROTATION_0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                rotation = display.getRotation();
            } else {
                rotation = display.getOrientation();
            }

            current = ROTATE_CYCLE[RotationCycle.currentIndex(rotation,
                    orientation == Configuration.ORIENTATION_LANDSCAPE)];
        }
        int nextIndex = 0;
        for (int i = 0; i < ROTATE_CYCLE.length; i++) {
            if (ROTATE_CYCLE[i] == current) {
                nextIndex = (i + 1) % ROTATE_CYCLE.length;
                break;
            }
        }

        int nextMode = ROTATE_CYCLE[nextIndex];
        setMode(activity, nextMode);
        apply(activity);

        String[] modeStrings = activity.getResources().getStringArray(R.array.screen_orientation_modes);
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i] == nextMode && i < modeStrings.length) {
                Toast.makeText(activity, modeStrings[i], Toast.LENGTH_SHORT).show();
                break;
            }
        }
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
