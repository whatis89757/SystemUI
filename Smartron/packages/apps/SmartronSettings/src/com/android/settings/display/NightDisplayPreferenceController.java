/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.support.v7.preference.Preference;
import android.provider.Settings;

import com.android.internal.app.NightDisplayController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class NightDisplayPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_NIGHT_DISPLAY = "night_display";
    private static final String DEFAULT_VALUE = "Default";

    public NightDisplayPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return NightDisplayController.isAvailable(mContext);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NIGHT_DISPLAY;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(!isScreenCalibrationEnabled() && !isReadingModeEnabled());
    }

    private boolean isReadingModeEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.FULL_READING_MODE_ENABLED, 0) == 1;
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
