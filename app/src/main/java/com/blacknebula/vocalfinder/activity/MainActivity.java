package com.blacknebula.vocalfinder.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.blacknebula.vocalfinder.R;
import com.blacknebula.vocalfinder.util.Logger;
import com.blacknebula.vocalfinder.util.ViewUtils;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import butterknife.ButterKnife;
import butterknife.InjectView;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    @InjectView(R.id.textView)
    TextView textView;

    private boolean hasFlash;
    private CameraManager mCameraManager;
    private String mCameraId;
    private boolean isTorchOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestRecordAudioPermission();
        detectFlashSupport();
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
                detectSound();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Logger.info(Logger.Type.MAIN, "Permission Granted!");
                    detectSound();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Logger.warn(Logger.Type.MAIN, "Permission Denied!");
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    void detectSound() {
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);

        PitchDetectionHandler pdh = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                final float pitchInHz = result.getPitch();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText("" + pitchInHz);
                        if (pitchInHz > 1300) {
                            turnOnFlashLight();
                        } else {
                            turnOffFlashLight();
                        }
                    }
                });
            }
        };
        AudioProcessor p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pdh);
        dispatcher.addAudioProcessor(p);
        new Thread(dispatcher, "Audio Dispatcher").start();
    }

    void detectFlashSupport() {
        hasFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (!hasFlash) {
            // device doesn't support flash
            ViewUtils.openDialog(this, R.string.flash_not_supported_title, R.string.flash_not_supported_message, new ViewUtils.onClickListener() {
                @Override
                public void onPositiveClick() {

                }

                @Override
                public void onNegativeClick() {

                }
            });
            return;
        } else {
            getCamera();
        }
    }

    // getting camera parameters
    private void getCamera() {
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = mCameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            Logger.error(Logger.Type.MAIN, e);
        }
    }

    public void turnOnFlashLight() {
        if (isTorchOn)
            return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCameraManager.setTorchMode(mCameraId, true);
                isTorchOn = true;
            }
        } catch (Exception e) {
            Logger.error(Logger.Type.MAIN, e);
        }
    }

    public void turnOffFlashLight() {
        if (!isTorchOn)
            return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCameraManager.setTorchMode(mCameraId, false);
                isTorchOn = false;
            }

        } catch (Exception e) {
            Logger.error(Logger.Type.MAIN, e);
        }
    }

}
