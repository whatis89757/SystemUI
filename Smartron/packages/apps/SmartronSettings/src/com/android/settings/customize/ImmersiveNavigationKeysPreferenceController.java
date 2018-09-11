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

/* Created by G Murali Madhav, 02-02-2018 */

package com.android.settings.customize;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

public class ImmersiveNavigationKeysPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_IMMERSIVE_NAVIGATION_KEYS = "immersive_navigation_keys";

    public ImmersiveNavigationKeysPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_enableImmersiveNavigationKeys);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_IMMERSIVE_NAVIGATION_KEYS;
    }

    @Override
    public void updateState(Preference preference) {
        int value = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.IMMERSIVE_NAVIGATION_KEYS_ENABLED, 0);
        ((SwitchPreference) preference).setChecked(value == 1);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.IMMERSIVE_NAVIGATION_KEYS_ENABLED, value ? 1 : 0);
        if (!value) {
            Settings.Global.putString(mContext.getContentResolver(),
                    Settings.Global.POLICY_CONTROL, "");
        }
        return true;
    }
}
