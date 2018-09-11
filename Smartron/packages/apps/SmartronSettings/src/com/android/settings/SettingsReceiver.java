package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.provider.Settings;
import android.os.Build;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Harish on 30/3/17.
 */
public class SettingsReceiver extends BroadcastReceiver {
    private static final String TAG = "SettingsReceiver";
    private static final String GESTURE_FILE = "/sys/class/touchscreen/gesture";
    private static final String ON = "7";
    private static final String OFF = "6";
    private static final String RIMOE_PATTERN = "RE\\d+";
    private static final String RIMOY_PATTERN = "RY\\d+";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, " SettingsReceiver "+intent.getAction());
        if(Intent.ACTION_SHUTDOWN.equals(intent.getAction())){
            writeSerialNumber(context);
        }
        else if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            // update socket value in seperate new thread
            new Thread (new Runnable () {
                @Override
                public void run() {
                    updateSocketValue(context);
                }
            }).start ();
        }
    }

    private void writeSerialNumber(Context context){
        String serialNumber = Build.getSerial();
        Log.d(TAG,"Serial Number current :"+serialNumber);
        if(!(serialNumber.matches(RIMOE_PATTERN) || serialNumber.matches(RIMOY_PATTERN))){
            Intent serviceIntent = new Intent(context, SettingsBootService.class);
            context.startService(serviceIntent);
        }
    }

    // Control value of "/sys/class/touchscreen/gesture" for On-Screen Gestures
    private void updateSocketValue(Context context){
        int overlayValue = Settings.Global.getInt(context.getContentResolver(), Settings.Global.DRAW_SCREEN_OVERLAY_C, 0);
        int doubleTapScreenValue = Settings.Global.getInt(context.getContentResolver(), Settings.Global.DOUBLE_TAP_SCREEN_TO_WAKE_ENABLED, 0);
        if(0 == overlayValue && 0 == doubleTapScreenValue){
            writeValue (OFF);
        } else {
            writeValue (ON);
        }
    }

    private void writeValue(String value) {
        File file = new File (GESTURE_FILE);
        if (file.exists ()) {
            LocalSocket s = new LocalSocket ();
            try {
                s.connect (new LocalSocketAddress ("smartd", LocalSocketAddress.Namespace.RESERVED));
                OutputStream outputStream = s.getOutputStream ();
                outputStream.write (value.getBytes ());
                outputStream.flush ();
                s.close ();
            } catch (Exception e) {
                Log.d (TAG, " DoubleTapScreenToWakePreferenceController : Exception while accessing file");
                e.printStackTrace ();
            } finally {
                try {
                    if (s != null) s.close ();
                } catch (IOException e) {
                    e.printStackTrace ();
                }
            }
        }
    }


}
