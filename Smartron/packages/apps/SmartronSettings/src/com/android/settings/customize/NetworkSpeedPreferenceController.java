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
package com.android.settings.customize;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.provider.Settings;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;


/**
 * Created by smartron on 29/1/18.
 */

public class NetworkSpeedPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_SHOW_STATUS_BAR_NETWORK_SPEED = "show_status_bar_network_speed";

    public NetworkSpeedPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SHOW_STATUS_BAR_NETWORK_SPEED;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.SHOW_STATUS_BAR_NETWORK_SPEED, value ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        int value = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.SHOW_STATUS_BAR_NETWORK_SPEED, 0);
        ((SwitchPreference) preference).setChecked(value == 1);
    }
}
