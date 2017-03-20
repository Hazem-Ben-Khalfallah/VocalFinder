package com.blacknebula.vocalfinder.util;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.blacknebula.vocalfinder.R;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hazem
 */
public class ViewUtils {
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    private static final float NORMAL = 0.1f;
    private static final float FOCUS = 1f;

    /**
     * Generate a value suitable for use in {@link View#setId(int)}.
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    public static int generateViewId() {
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void setFocus(View view, Boolean isFocused) {
        if (isFocused) {
            view.setAlpha(FOCUS);
        } else {
            view.setAlpha(NORMAL);
        }
    }

    public static void showToast(final Context context, final String message) {
        showToast(context, message, false);
    }

    public static void showToast(final Context context, final String message, final Boolean isOnTop) {
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
                if (isOnTop)
                    toast.setGravity(Gravity.TOP | Gravity.END, 0, 0);
                else
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
            }
        });
    }

    public static AlertDialog openDialog(Context context, int title, int message, onClickListener onClickListener) {
        return openDialog(context, title, message, android.R.string.ok, android.R.string.no, R.mipmap.ic_action_info, onClickListener);
    }

    public static AlertDialog openDialog(Context context, int title, int message, int positiveText, int negativeText, onClickListener onClickListener) {
        return openDialog(context, title, message, positiveText, negativeText, R.mipmap.ic_action_info, onClickListener);
    }

    public static AlertDialog openDialog(Context context, int title, int message, int positiveText, int negativeText, int icon, final onClickListener onClickListener) {
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onClickListener.onPositiveClick();
                    }
                })
                .setNegativeButton(negativeText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onClickListener.onNegativeClick();
                    }
                })
                .setIcon(icon)
                .show();
    }

    public interface onClickListener {
        void onPositiveClick();

        void onNegativeClick();
    }
}