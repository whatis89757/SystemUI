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

/* Created by G Murali Madhav, 22-01-2018 */

package com.android.settings.gestures;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settings.R;

public class FlipToMutePreferenceController extends GesturePreferenceController {

    private static final String PREF_KEY_VIDEO = "gesture_double_twist_video";
    private final String mFlipToMuteKey;

    public FlipToMutePreferenceController(Context context, Lifecycle lifecycle, String key) {
        super(context, lifecycle);
         mFlipToMuteKey = key;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public String getPreferenceKey() {
        return mFlipToMuteKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.FLIP_TO_MUTE_GESTURE_ENABLED, enabled ? 1 : 0);
        return true;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                       Settings.Global.FLIP_TO_MUTE_GESTURE_ENABLED, 0) == 1;
    }
}

