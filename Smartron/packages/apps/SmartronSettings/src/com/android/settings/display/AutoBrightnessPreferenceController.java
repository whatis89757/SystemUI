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
import android.content.Intent;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.support.v7.preference.PreferenceScreen;
import android.os.Handler;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import com.android.settings.DisplaySettings;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;


public class AutoBrightnessPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener, LifecycleObserver, OnResume, OnPause {

    private final String mAutoBrightnessKey;
    private AutoBrightnessObserver mAutoBrightnessObserver;

    private final String SYSTEM_KEY = SCREEN_BRIGHTNESS_MODE;
    private final int DEFAULT_VALUE = SCREEN_BRIGHTNESS_MODE_MANUAL;

    public AutoBrightnessPreferenceController(Context context, String key, Lifecycle lifecycle) {
        super(context);
        mAutoBrightnessKey = key;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(mAutoBrightnessKey);
        if (preference != null) {
            mAutoBrightnessObserver = new AutoBrightnessObserver(preference);
        }
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
    }

    @Override
    public String getPreferenceKey() {
        return mAutoBrightnessKey;
    }

    @Override
    public void onResume() {
        if (mAutoBrightnessObserver != null) {
            mAutoBrightnessObserver.register(mContext.getContentResolver(), true /* register */);
        }
    }

    @Override
    public void onPause() {
        if (mAutoBrightnessObserver != null) {
            mAutoBrightnessObserver.register(mContext.getContentResolver(), false /* register */);
        }
    }

    @Override
    public void updateState(Preference preference) {
        int brightnessMode = Settings.System.getInt(mContext.getContentResolver(),
                SYSTEM_KEY, DEFAULT_VALUE);
        ((SwitchPreference) preference).setChecked(brightnessMode != DEFAULT_VALUE);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean auto = (Boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(), SYSTEM_KEY,
                auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : DEFAULT_VALUE);
        return true;
    }

    @Override
    public ResultPayload getResultPayload() {
        final Intent intent = DatabaseIndexingUtils.buildSubsettingIntent(mContext,
                DisplaySettings.class.getName(), mAutoBrightnessKey,
                mContext.getString(R.string.display_settings));

        return new InlineSwitchPayload(SYSTEM_KEY,
                ResultPayload.SettingsSource.SYSTEM, SCREEN_BRIGHTNESS_MODE_AUTOMATIC, intent,
                isAvailable(), DEFAULT_VALUE);
    }

    class AutoBrightnessObserver extends ContentObserver {

        private final Uri AUTO_BRIGHTNESS_URI =
                Settings.System.getUriFor(SCREEN_BRIGHTNESS_MODE);

        private final Preference mPreference;

        public AutoBrightnessObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr, boolean register) {
            if (register) {
                cr.registerContentObserver(AUTO_BRIGHTNESS_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (AUTO_BRIGHTNESS_URI.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}