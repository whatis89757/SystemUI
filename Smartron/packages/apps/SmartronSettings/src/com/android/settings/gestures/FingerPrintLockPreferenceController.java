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
import android.provider.Settings.Global;
import android.support.v7.preference.ListPreference;
import android.hardware.fingerprint.FingerprintManager;
import com.android.settings.R;

import com.android.settingslib.core.lifecycle.Lifecycle;

public class FingerPrintLockPreferenceController extends GesturePreferenceController {

    // Fixme : Temporarily video pref added. Needs to be changed.
    private static final String PREF_KEY_VIDEO = "gesture_double_tap_power_video";
    private final String mFingerPrintLockKey;

    public FingerPrintLockPreferenceController(Context context, Lifecycle lifecycle, String key) {
        super(context, lifecycle);
        mFingerPrintLockKey = key;
    }

    @Override
    public boolean isAvailable() {
        FingerprintManager fingerprintManager = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
        return fingerprintManager != null && fingerprintManager.isHardwareDetected() && mContext.getResources().getBoolean(R.bool.config_enableLockWithFingerprintGesture);   
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public String getPreferenceKey() {
        return mFingerPrintLockKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int value = Integer.parseInt((String) newValue);
            Global.putInt(mContext.getContentResolver(), Global.FINGERPRINT_LOCK_ENABLED, value);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        int value = Global.getInt(mContext.getContentResolver(), Global.FINGERPRINT_LOCK_ENABLED, 2);
        if(preference instanceof ListPreference)
            ((ListPreference) preference).setValue (String.valueOf (value));
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        return true;
    }

}
