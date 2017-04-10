package com.blacknebula.vocalfinder.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.WindowManager;

import com.blacknebula.vocalfinder.R;
import com.blacknebula.vocalfinder.VocalFinderApplication;
import com.blacknebula.vocalfinder.service.VocalFinderIntentService;

import butterknife.ButterKnife;
import butterknife.InjectView;
import ng.max.slideview.SlideView;

import static com.blacknebula.vocalfinder.service.VocalFinderIntentService.ALARM_STOP_ACTION;

/**
 * @author hazem
 */

public class AlarmActivity extends Activity {

    @InjectView(R.id.stopAlarm)
    SlideView stopAlarm;

    private BroadcastReceiver receiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        ButterKnife.inject(this);

        keepScreenOn();

        stopAlarm.setOnSlideCompleteListener(new SlideView.OnSlideCompleteListener() {
            @Override
            public void onSlideComplete(SlideView slideView) {
                // stop alarm
                final Intent stopAlarmIntent = new Intent(VocalFinderApplication.getAppContext(), VocalFinderIntentService.class);
                stopAlarmIntent.setAction(ALARM_STOP_ACTION);
                startService(stopAlarmIntent);
                finish();
            }
        });

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ALARM_STOP_ACTION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ALARM_STOP_ACTION.equals(intent.getAction())) {
                    finish();
                }
            }
        };
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        super.onDestroy();
    }

    private void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }
}
