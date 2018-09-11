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

/* Created by G Murali Madhav, 29-01-2018 */

package com.android.settings.customize;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.graphics.Color;

import com.android.settings.colorpicker.ColorDialogPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class NotificationLedPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_NOTIFICATION_LED_LIGHT = "notification_led_light";

    public NotificationLedPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NOTIFICATION_LED_LIGHT;
    }

    private int getDefaultLedColor() {
        return mContext.getResources().getColor(com.android.internal.R.color.config_defaultNotificationColor);
    }

    @Override
    public void updateState(Preference preference) {
        int value = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.NOTIFICATION_LED_COLOR, getDefaultLedColor());
        ColorDialogPreference colorPreference = (ColorDialogPreference) preference;
        colorPreference.setValue(value);
        colorPreference.setDefaultColor(getDefaultLedColor());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int value = Integer.parseInt(newValue.toString());
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.NOTIFICATION_LED_COLOR, value);
        return true;
    }
}
