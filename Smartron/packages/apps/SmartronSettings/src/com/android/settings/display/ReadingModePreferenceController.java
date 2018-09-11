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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.support.v7.preference.Preference;
import java.util.List;

import com.android.internal.app.NightDisplayController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Created by G Murali Madhav on 20/02/18.
 */

public class ReadingModePreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_READING_MODE = "reading_mode";
    private static final String ACTION_READING_MODE = "com.smartron.intent.action.READING_MODE";

    private NightDisplayController mController;

    public ReadingModePreferenceController(Context context) {
        super(context);
        mController = new NightDisplayController(context);
    }

    @Override
    public boolean isAvailable() {
        boolean showPreference = false;
        Intent intent = new Intent(ACTION_READING_MODE);
        List<ResolveInfo> infos = mContext.getPackageManager().queryIntentActivities(intent, 0);
        if (infos != null && !infos.isEmpty()) {
            showPreference = true;
        }
        return showPreference;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(!isNightDisplayEnabled());
    }

    @Override
    public String getPreferenceKey() {
        return KEY_READING_MODE;
    }

    private boolean isNightDisplayEnabled() {
        return mController.isActivated();
    }
}
