/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.hardware.fingerprint.FingerprintManager;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settings.R;

public class TapFingerprintToWakePreferenceController extends GesturePreferenceController {

    private static final String PREF_KEY_VIDEO = "gesture_swipe_down_fingerprint_video";
    private final String mFingerprintWakeKey;
    private FingerprintManager mFingerprintManager;

    public TapFingerprintToWakePreferenceController(Context context, Lifecycle lifecycle, String key) {
        super(context, lifecycle);
         mFingerprintWakeKey = key;
         mFingerprintManager = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_enableWakeWithFingerprintGesture) &&
                mFingerprintManager.isHardwareDetected();
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public String getPreferenceKey() {
        return mFingerprintWakeKey;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(!mFingerprintManager.hasEnrolledFingerprints());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.FINGERPRINT_WAKE_GESTURE_ENABLED, enabled ? 1 : 0);
        return true;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                       Settings.Global.FINGERPRINT_WAKE_GESTURE_ENABLED, 0) == 1;
    }
}

