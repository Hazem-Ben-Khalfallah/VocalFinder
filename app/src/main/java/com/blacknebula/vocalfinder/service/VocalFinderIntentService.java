package com.blacknebula.vocalfinder.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.view.Display;

import com.blacknebula.vocalfinder.R;
import com.blacknebula.vocalfinder.VocalFinderApplication;
import com.blacknebula.vocalfinder.activity.MainActivity;
import com.blacknebula.vocalfinder.activity.SettingsActivity;
import com.blacknebula.vocalfinder.util.Logger;
import com.blacknebula.vocalfinder.util.PreferenceUtils;
import com.google.common.base.Strings;

import org.parceler.Parcels;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class VocalFinderIntentService extends NonStopIntentService {

    //Actions
    public static final String START_ACTION = "detect_sound";
    public static final String STOP_ACTION = "stop";
    public static final String SOUND_DETECTED_ACTION = "soundDetected";

    // Extras
    public static final String SOUND_PITCH_EXTRA = "reply";
    private static final int NOTIFICATION_ID = 1;

    public static boolean isRunning;
    /**
     * Start without a delay, Vibrate for 100 milliseconds, Sleep for 1000 milliseconds
     */
    long[] vibrationPattern = {0, 100, 1000};
    private CameraManager mCameraManager;
    private String mCameraId;
    private boolean isTorchOn;
    private Vibrator vibrator;
    private Ringtone ringtone;

    public VocalFinderIntentService() {
        super(VocalFinderIntentService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        isRunning = true;

        final Intent mainIntent = new Intent(VocalFinderApplication.getAppContext(), MainActivity.class);
        final PendingIntent pReceiverIntent = PendingIntent.getActivity(VocalFinderApplication.getAppContext(), 1, mainIntent, 0);

        final Intent stopIntent = new Intent(VocalFinderApplication.getAppContext(), VocalFinderIntentService.class);
        stopIntent.setAction(STOP_ACTION);
        final PendingIntent pStop = PendingIntent.getService(VocalFinderApplication.getAppContext(), 1, stopIntent, FLAG_CANCEL_CURRENT);

        final Intent settingsIntent = new Intent(VocalFinderApplication.getAppContext(), SettingsActivity.class);
        final PendingIntent pSettings = PendingIntent.getActivity(VocalFinderApplication.getAppContext(), 1, settingsIntent, 0);

        Notification notification = new Notification.Builder(VocalFinderApplication.getAppContext())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(Color.BLACK)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setOngoing(true)
                .setContentTitle("Vocal finder")
                .setContentText("Never lose your phone again!")
                .setContentIntent(pReceiverIntent)
                .addAction(R.mipmap.ic_stop, "Stop", pStop)
                .addAction(R.mipmap.ic_settings, "Settings", pSettings)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            final String action = intent.getAction();
            if (START_ACTION.equals(action)) {
                detectSound();
            } else if (STOP_ACTION.equals(action)) {
                stopForeground(true);
                stopSelf();
                final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.cancel(NOTIFICATION_ID);
            }

        } catch (Exception ex) {
            Logger.error(Logger.Type.VOCAL_FINDER, ex, "Error handling action");
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }

    // getting camera parameters
    private String getCamera() {
        if (mCameraId != null) {
            return mCameraId;
        }
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = mCameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            Logger.error(Logger.Type.VOCAL_FINDER, e);
            PreferenceUtils.getPreferences().edit().putBoolean("enableFlashLight", false).apply();
        }
        return mCameraId;
    }

    Vibrator getVibrator() {
        // Get instance of Vibrator from current Context
        if (vibrator == null) {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        return vibrator;

    }

    void detectSound() {
        final AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);

        PitchDetectionHandler pdh = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                if (!isRunning) {
                    dispatcher.stop();
                    Logger.warn(Logger.Type.VOCAL_FINDER, "Vocal finder service has been stopped");
                    return;
                }

                if (skipSoundDetection()) {
                    return;
                }

                final float pitchInHz = result.getPitch();

                sendReply(pitchInHz);

                final int minimalPitch = PreferenceUtils.asInt("audioSensitivity", 1400);
                if (pitchInHz > minimalPitch) {
                    turnOnFlashLight();
                    startVibration();
                    playRingtone();
                } else {
                    turnOffFlashLight();
                    stopVibration();
                    stopRingtone();
                }
            }
        };
        AudioProcessor p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pdh);
        dispatcher.addAudioProcessor(p);
        new Thread(dispatcher, "Audio Dispatcher").start();
    }

    private boolean skipSoundDetection() {
        final boolean isEnabled = PreferenceUtils.asBoolean("enableVocalFinder", false);
        final boolean isSaveEnergyMode = PreferenceUtils.asBoolean("enableSaveEnergyMode", false);
        return !isEnabled ||
                (isSaveEnergyMode && isScreenOn(VocalFinderApplication.getAppContext())) ||
                isCallActive(VocalFinderApplication.getAppContext());
    }

    /**
     * Check if a telephony call is established.
     *
     * @param context context
     * @return true if telephony call is established
     */
    public boolean isCallActive(Context context) {
        final AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return AudioManager.MODE_IN_CALL == manager.getMode();
    }

    /**
     * Is the screen of the device on.
     *
     * @param context the context
     * @return true when (at least one) screen is on
     */
    public boolean isScreenOn(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            boolean screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                }
            }
            return screenOn;
        } else {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            //noinspection deprecation
            return pm.isScreenOn();
        }
    }

    public void turnOnFlashLight() {
        final boolean enableFlashLight = PreferenceUtils.asBoolean("enableFlashLight", false);
        if (isTorchOn || !enableFlashLight)
            return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                final String cameraId = getCamera();
                if (cameraId != null) {
                    mCameraManager.setTorchMode(cameraId, true);
                    isTorchOn = true;
                }
            }
        } catch (Exception e) {
            Logger.error(Logger.Type.VOCAL_FINDER, e);
        }
    }

    public void turnOffFlashLight() {
        if (!isTorchOn)
            return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                final String cameraId = getCamera();
                if (cameraId != null) {
                    mCameraManager.setTorchMode(getCamera(), false);
                    isTorchOn = false;
                }
            }

        } catch (Exception e) {
            Logger.error(Logger.Type.VOCAL_FINDER, e);
        }
    }

    void startVibration() {
        final boolean enableVibration = PreferenceUtils.asBoolean("enableVibration", false);
        if (enableVibration) {
            final Vibrator vibrator = getVibrator();
            if (vibrator != null) {
                vibrator.vibrate(vibrationPattern, 0);
            }
        }
    }

    void stopVibration() {
        final Vibrator vibrator = getVibrator();
        if (vibrator != null) {
            vibrator.cancel();
        }
    }


    void playRingtone() {
        final String selectedRingtone = PreferenceUtils.asString("ringtone", "");
        if (!Strings.isNullOrEmpty(selectedRingtone) && (ringtone == null || !ringtone.isPlaying())) {
            final Uri path = Uri.parse(selectedRingtone);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), path);
            ringtone.play();
        }
    }

    void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private <T> void sendReply(T replyMessage) {
        final Intent intent = new Intent();
        intent.setAction(SOUND_DETECTED_ACTION);
        intent.putExtra(SOUND_PITCH_EXTRA, Parcels.wrap(replyMessage));
        sendBroadcast(intent);
    }

}