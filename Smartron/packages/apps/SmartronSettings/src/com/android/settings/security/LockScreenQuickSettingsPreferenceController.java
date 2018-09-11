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

/* Created by G Murali Madhav, 10-01-2018 */

package com.android.settings.security;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.RestrictedSwitchPreference;

public class LockScreenQuickSettingsPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_LOCK_SCREEN_QS = "lock_screen_quick_settings";
    private LockPatternUtils mLockPatternUtils;

    public LockScreenQuickSettingsPreferenceController(Context context) {
        super(context);
        mLockPatternUtils = new LockPatternUtils(context);
    }

    @Override
    public boolean isAvailable() {
        return mLockPatternUtils.isSecure(UserHandle.myUserId());
    }

    @Override
    public String getPreferenceKey() {
        return KEY_LOCK_SCREEN_QS;
    }

    @Override
    public void updateState(Preference preference) {
        int value = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_QS_PANEL_ENABLED, 0);
        ((RestrictedSwitchPreference) preference).setChecked(value == 1);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_QS_PANEL_ENABLED, value ? 1 : 0);
        return true;
    }
}
