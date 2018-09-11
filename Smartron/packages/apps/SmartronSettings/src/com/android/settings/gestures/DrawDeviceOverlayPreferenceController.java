/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.gestures;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settings.R;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by vrakesh on 15/3/18.
 */

public class DrawDeviceOverlayPreferenceController extends GesturePreferenceController {

    // Fixme : Temporarily video pref added. Needs to be changed.
    private static final String PREF_KEY_VIDEO = "gesture_double_tap_power_video";
    private final String TAG = "DrawOverlayPreference";
    private final String mDrawScreenOverlayKey;
    private static final String GESTURE_FILE = "/sys/class/touchscreen/gesture";
    private Handler mBackgroundHandler;
    private static final String ON = "7";
    private static final String OFF = "6";

    public DrawDeviceOverlayPreferenceController(Context context, Lifecycle lifecycle, String key) {
        super(context, lifecycle);
        mDrawScreenOverlayKey = key;
        HandlerThread handlerThread = new HandlerThread ("SocketSmartd");
        handlerThread.start ();
        mBackgroundHandler = new Handler (handlerThread.getLooper ()) {
            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                switch (what){
                    case 1:
                        writeValue(ON);
                        break;
                    case 0:
                        updateSocketValue();
                        break;
                }
            }
        };
    }

    private void updateSocketValue(){
        int overlayValue = Settings.Global.getInt (mContext.getContentResolver(), Settings.Global.DRAW_SCREEN_OVERLAY_C,0);
        int doubleTapScreenValue = Settings.Global.getInt (mContext.getContentResolver (), Settings.Global.DOUBLE_TAP_SCREEN_TO_WAKE_ENABLED, 0);
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
                Log.d (TAG, " DrawDeviceOverlayPreferenceController : Exception while accessing file");
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

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_enableOnScreenGesture);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public String getPreferenceKey() {
        return mDrawScreenOverlayKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (boolean) newValue;
        Settings.Global.putInt (mContext.getContentResolver (),
                Settings.Global.DRAW_SCREEN_OVERLAY_C, enabled ? 1 : 0);
        if(mBackgroundHandler != null){
            if(enabled){
                mBackgroundHandler.obtainMessage (1).sendToTarget ();
            } else {
                mBackgroundHandler.obtainMessage (0).sendToTarget ();
            }
        }
        return true;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        return Settings.Global.getInt (mContext.getContentResolver (),
                Settings.Global.DRAW_SCREEN_OVERLAY_C, 0) == 1;
    }
}
