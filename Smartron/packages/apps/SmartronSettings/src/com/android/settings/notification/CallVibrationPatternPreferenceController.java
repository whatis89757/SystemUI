/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.notification;

import android.content.Context;
import android.provider.Settings;
import android.content.ContentResolver;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import android.provider.Settings;
import android.content.res.Resources;
import com.android.settings.notification.VibrationPreference;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.Lifecycle;
import android.support.v7.preference.PreferenceScreen;
import android.database.ContentObserver;
import android.os.Handler;

public class CallVibrationPatternPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause {

    private final String mCallVibrationKey;
    private Preference mPreference;
    private final ContentObserver mCallVibrateObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (mPreference != null) {
                boolean value = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.VIBRATE_WHEN_RINGING, 0) != 0;
                mPreference.setEnabled(value);
            }
        }
    };

    public CallVibrationPatternPreferenceController(Context context, String key, Lifecycle lifecycle) {
        super(context);
        mCallVibrationKey = key;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(com.android.settings.R.bool.config_enableCustomCallVibration);
    }

    @Override
    public String getPreferenceKey() {
        return mCallVibrationKey;
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int newMode = Integer.parseInt(newValue.toString());
        return Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.VIBRATION_PATTERN_CALL, newMode);
    }

    @Override
    public void updateState(Preference preference) {
        boolean value = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.VIBRATE_WHEN_RINGING, 0) != 0;
        preference.setEnabled(value);
        final int mode = Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.VIBRATION_PATTERN_CALL, 0);
        VibrationPreference vibrationPreference = (VibrationPreference) preference;
        vibrationPreference.setValue(mode);
        vibrationPreference.setSummary(vibrationPreference.getValue());
    }

    @Override
    public void onPause() {
        mContext.getContentResolver().unregisterContentObserver(mCallVibrateObserver);
    }

    @Override
    public void onResume() {
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.VIBRATE_WHEN_RINGING), false,
                mCallVibrateObserver);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            mPreference = preference;
        }
    }
}