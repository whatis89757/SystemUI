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
package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.Intent;
import java.util.List;
import android.content.pm.ResolveInfo;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class ScheduledPowerOnOffPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String TAG = "ScheduledPowerPref";
    private static final String KEY_TIMER_SWITCH = "timer_switch_settings";
    private static final String ACTION_TIMER_SWITCH = "qualcomm.intent.action.TIMER_SWITCH";

    public ScheduledPowerOnOffPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        boolean isSettingEnabled = mContext.getResources().getBoolean(com.android.settings.R.bool.config_scheduled_power_on_and_off_enabled);
        if (isSettingEnabled) {
            boolean showTimerSwitch = false;
            Intent intent = new Intent(ACTION_TIMER_SWITCH);
            List<ResolveInfo> infos = mContext.getPackageManager()
                .queryIntentActivities(intent, 0);
                if (infos != null && !infos.isEmpty()) {
                    showTimerSwitch = true;
                }
                return showTimerSwitch;
        }

        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TIMER_SWITCH;
    }

}
