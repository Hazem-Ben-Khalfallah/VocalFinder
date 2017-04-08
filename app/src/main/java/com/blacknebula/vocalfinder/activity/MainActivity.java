package com.blacknebula.vocalfinder.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.blacknebula.vocalfinder.R;
import com.blacknebula.vocalfinder.service.VocalFinderIntentService;
import com.blacknebula.vocalfinder.util.Logger;
import com.blacknebula.vocalfinder.util.PreferenceUtils;
import com.blacknebula.vocalfinder.util.ViewUtils;
import com.txusballesteros.SnakeView;

import org.parceler.Parcels;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.blacknebula.vocalfinder.service.VocalFinderIntentService.SOUND_DETECTED_ACTION;
import static com.blacknebula.vocalfinder.service.VocalFinderIntentService.SOUND_PITCH_EXTRA;
import static com.blacknebula.vocalfinder.service.VocalFinderIntentService.START_ACTION;
import static com.blacknebula.vocalfinder.service.VocalFinderIntentService.isRunning;
import static com.blacknebula.vocalfinder.util.Logger.Type.VOCAL_FINDER;

public class MainActivity extends AppCompatActivity {

    @InjectView(R.id.pitchText)
    TextView textView;
    @InjectView(R.id.snake)
    SnakeView snakeView;

    float maxPitch = 200;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(SOUND_DETECTED_ACTION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SOUND_DETECTED_ACTION.equals(intent.getAction())) {
                    visualizeAudioData(intent);
                }
            }
        };
        registerReceiver(receiver, filter);

        detectFlashSupport();
        requestRecordAudioPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent myIntent = new Intent(this, SettingsActivity.class);
                this.startActivity(myIntent);
                break;

        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isRunning) {
                        return;
                    }
                    // permission was granted, yay! set app default params & launch detection
                    launchAudioDetection();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Logger.warn(VOCAL_FINDER, "Permission Denied!");
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void requestRecordAudioPermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    ViewUtils.openDialog(this, R.string.request_permission_title, R.string.request_permission_message, new ViewUtils.onClickListener() {
                        @Override
                        public void onPositiveClick() {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                        }

                        @Override
                        public void onNegativeClick() {

                        }
                    });

                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            } else {
                // Permission already granted
                launchAudioDetection();
            }
        }
    }

    void detectFlashSupport() {
        boolean hasFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (hasFlash) {
            PreferenceUtils.getPreferences().edit().putBoolean("enableFlashLight", true).apply();
            return;
        }

        // device doesn't support flash
        ViewUtils.openDialog(this, R.string.flash_not_supported_title, R.string.flash_not_supported_message, new ViewUtils.onClickListener() {
            @Override
            public void onPositiveClick() {

            }

            @Override
            public void onNegativeClick() {

            }
        });

        // disable flash light
        PreferenceUtils.getPreferences().edit().putBoolean("enableFlashLight", false).apply();
    }

    public void visualizeAudioData(Intent data) {
        final Parcelable result = data.getParcelableExtra(SOUND_PITCH_EXTRA);
        final float pitchInHz = Parcels.unwrap(result);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final long roundedPitch = Math.round(pitchInHz);
                final String value = pitchInHz <= 0 ? "0" : "" + roundedPitch;
                textView.setText(value);

                if (pitchInHz > maxPitch) {
                    snakeView.setMaxValue(pitchInHz);
                    maxPitch = pitchInHz;
                }
                snakeView.addValue(pitchInHz);
            }
        });

    }

    private void launchAudioDetection() {
        if (isRunning) {
            return;
        }
        final Intent intent = new Intent(this, VocalFinderIntentService.class);
        intent.setAction(START_ACTION);
        startService(intent);
    }


}
