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

/* Created by G Murali Madhav, 08-03-2018 */

package com.android.settings.customize;

import android.content.Context;
import android.provider.Settings;
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

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class LedSettingsPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    private static final String LED_SETTINGS_KEY = "led_settings";
    private NotificationPulseObserver mNotificationPulseObserver;

    public LedSettingsPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(LED_SETTINGS_KEY);
        if (preference != null) {
            mNotificationPulseObserver = new NotificationPulseObserver(preference);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return LED_SETTINGS_KEY;
    }

    @Override
    public void onResume() {
        if (mNotificationPulseObserver != null) {
            mNotificationPulseObserver.register(mContext.getContentResolver(), true /* register */);
        }
    }

    @Override
    public void onPause() {
        if (mNotificationPulseObserver != null) {
            mNotificationPulseObserver.register(mContext.getContentResolver(), false /* register */);
        }
    }

    @Override
    public void updateState(Preference preference) {
        boolean notificationPulseEnabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 1) == 1;
        preference.setEnabled(notificationPulseEnabled);
    }

    class NotificationPulseObserver extends ContentObserver {

        private final Uri NOTIFICATION_PULSE_URI =
                Settings.System.getUriFor(Settings.System.NOTIFICATION_LIGHT_PULSE);

        private final Preference mPreference;

        public NotificationPulseObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr, boolean register) {
            if (register) {
                cr.registerContentObserver(NOTIFICATION_PULSE_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (NOTIFICATION_PULSE_URI.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
