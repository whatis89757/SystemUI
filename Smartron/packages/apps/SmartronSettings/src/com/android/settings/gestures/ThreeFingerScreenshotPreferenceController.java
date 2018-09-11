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
import com.android.settings.R;

import com.android.settingslib.core.lifecycle.Lifecycle;

public class ThreeFingerScreenshotPreferenceController extends GesturePreferenceController {

    // Fixme : Temporarily video pref added. Need to be changed.
    private static final String PREF_KEY_VIDEO = "gesture_double_tap_power_video";
    private final String mThreeFingerScreenshotKey;

    public ThreeFingerScreenshotPreferenceController(Context context, Lifecycle lifecycle, String key) {
        super(context, lifecycle);
        mThreeFingerScreenshotKey = key;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_enableThreeFingerScreenshotGesture);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public String getPreferenceKey() {
        return mThreeFingerScreenshotKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (boolean) newValue;
        Global.putInt(mContext.getContentResolver(), Global.THREE_FINGER_SCREENSHOT_GESTURE_ENABLED, enabled ? 1 : 0);
        return true;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        return Global.getInt(mContext.getContentResolver(), Global.THREE_FINGER_SCREENSHOT_GESTURE_ENABLED, 0) == 1;
    }

}
