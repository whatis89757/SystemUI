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

public class NotificationVibrationPatternPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private final String mNotificationVibrationKey;

    public NotificationVibrationPatternPreferenceController(Context context, String key) {
        super(context);
        mNotificationVibrationKey = key;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(com.android.settings.R.bool.config_enableCustomNotificationVibration);
    }

    @Override
    public String getPreferenceKey() {
        return mNotificationVibrationKey;
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int newMode = Integer.parseInt(newValue.toString());
        return Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.VIBRATION_PATTERN_NOTIFICATION, newMode);
    }

    @Override
    public void updateState(Preference preference) {
        final int mode = Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.VIBRATION_PATTERN_NOTIFICATION, 0);
        VibrationPreference vibrationPreference = (VibrationPreference) preference;
        vibrationPreference.setValue(mode);
        vibrationPreference.setSummary(vibrationPreference.getValue());
    }
}