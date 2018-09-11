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
package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;
import android.content.ContentResolver;
import android.support.v7.preference.Preference;
import android.support.v7.preference.ListPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import android.view.accessibility.AccessibilityManager;

public class SimulateColorDisplayPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String DEFAULT_VALUE = "Default";
    private final String mSimulateColorSpaceKey;

    public SimulateColorDisplayPreferenceController(Context context, String key) {
        super(context);
        mSimulateColorSpaceKey = key;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return mSimulateColorSpaceKey;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean enabled = Settings.Secure.getInt(
                mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0) != 0;
        if (enabled) {
            final String mode = Integer.toString(Settings.Secure.getInt(
                    mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                    AccessibilityManager.DALTONIZER_DISABLED));
            ((ListPreference) preference).setValue (String.valueOf (mode));
           final int index = ((ListPreference) preference).findIndexOfValue(mode);
            if (index < 0) {
                // We're using a mode controlled by accessibility preferences.
                preference.setSummary(mContext.getString(com.android.settings.R.string.daltonizer_type_overridden,
                        mContext.getString(com.android.settings.R.string.accessibility_display_daltonizer_preference_title)));
            } else {
                preference.setSummary("%s");
            }
        } else {
            ((ListPreference) preference).setValue(
                    Integer.toString(AccessibilityManager.DALTONIZER_DISABLED));
        }

        preference.setEnabled(!isScreenCalibrationEnabled());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int newMode = Integer.parseInt(newValue.toString());
        if (newMode < 0) {
            Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0);
        } else {
            Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.FULL_READING_MODE_ENABLED, 0);
            Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 1);
            Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, newMode);
        }
        return true;
    }

    private boolean isScreenCalibrationEnabled() {
        String mode = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.DISPLAY_SCREEN_CALIBRATION_MODE);
        if (mode == null || DEFAULT_VALUE.equals(mode)) {
            return false;
        }
        return true;
    }

}
