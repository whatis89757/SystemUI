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
package com.android.settings.display;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import java.util.List;

import com.android.internal.app.NightDisplayController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settings.R;

/**
 * Created by G Murali Madhav on 20/02/18.
 */

public class ScreenCalibrationPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    private static final String KEY_SCREEN_CALIBRATION = "screen_calibration";
    private static final String ACTION_DISPLAY_MODE_SELECTION = "com.smartron.intent.action.DISPLAY_MODE_SELECTION";

    private NightDisplayController mController;
    private SimulateColorSpaceObserver mSimulateColorSpaceObserver;

    public ScreenCalibrationPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mController = new NightDisplayController(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(KEY_SCREEN_CALIBRATION);
        if (preference != null) {
            mSimulateColorSpaceObserver = new SimulateColorSpaceObserver(preference);
        }
    }

    @Override
    public boolean isAvailable() {
        boolean showPreference = false;
        Intent intent = new Intent(ACTION_DISPLAY_MODE_SELECTION);
        List<ResolveInfo> infos = mContext.getPackageManager().queryIntentActivities(intent, 0);
        if (infos != null && !infos.isEmpty()) {
            showPreference = true;
        }
        return showPreference;
    }


    @Override
    public void onResume() {
        if (mSimulateColorSpaceObserver != null) {
            mSimulateColorSpaceObserver.register(mContext.getContentResolver(), true /* register */);
        }
    }

    @Override
    public void onPause() {
        if (mSimulateColorSpaceObserver != null) {
            mSimulateColorSpaceObserver.register(mContext.getContentResolver(), false /* register */);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(!isNightDisplayEnabled() && !isReadingModeEnabled());
        preference.setSummary(getDisplayModeSummary());
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SCREEN_CALIBRATION;
    }

    private boolean isNightDisplayEnabled() {
        return mController.isActivated();
    }

    private boolean isReadingModeEnabled() {
        return ((Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0) == 1) ||
                        (Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.FULL_READING_MODE_ENABLED, 0) == 1));
    }

    private String getDisplayModeSummary() {
        String mode = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.DISPLAY_SCREEN_CALIBRATION_MODE);
        if (mode == null) {
            return mContext.getResources().getString(R.string.default_value);
        } else {
            return mode;
        }
    }

    class SimulateColorSpaceObserver extends ContentObserver {

        private final Uri DISPLAY_DALTONIZER_URI =
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED);

        private final Preference mPreference;

        public SimulateColorSpaceObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr, boolean register) {
            if (register) {
                cr.registerContentObserver(DISPLAY_DALTONIZER_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (DISPLAY_DALTONIZER_URI.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
