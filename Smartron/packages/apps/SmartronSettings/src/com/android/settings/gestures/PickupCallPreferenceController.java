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

/* Created by G Murali Madhav, 01-03-2018 */

package com.android.settings.gestures;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.support.v7.preference.PreferenceScreen;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import com.android.settings.sim.SimDialogActivity;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settings.R;

public class PickupCallPreferenceController extends GesturePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    private static final String PREF_KEY_VIDEO = "gesture_pick_up_video";
    private final String mPickupCallKey;
    private final SubscriptionManager mSubscriptionManager ;
    private Preference mPreference;

    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (mPreference != null) {
                updateState(mPreference);
            }
        }
    };

    public PickupCallPreferenceController(Context context, Lifecycle lifecycle, String key) {
        super(context, lifecycle);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mSubscriptionManager = SubscriptionManager.from(context);
        mPickupCallKey = key;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_enablePickupCallGesture);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public String getPreferenceKey() {
        return mPickupCallKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.PICK_UP_CALL_GESTURE_ENABLED, enabled ? 1 : 0);
        if (enabled) {
            TelecomManager telecomManager = TelecomManager.from(mContext);
            PhoneAccountHandle phoneAccount =
                    telecomManager.getUserSelectedOutgoingPhoneAccount();
            if (phoneAccount == null) {
                Intent simSelectIntent = new Intent(mContext, SimDialogActivity.class);
                simSelectIntent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.CALLS_PICK);
                mContext.startActivity(simSelectIntent);
            }
        }
        return true;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                       Settings.Global.PICK_UP_CALL_GESTURE_ENABLED, 0) == 1;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mSubscriptionManager != null && mSubscriptionManager.getActiveSubscriptionInfoCount() > 0) {
            preference.setEnabled(true);
        } else {
            preference.setEnabled(false);
        }
    }

    @Override
    public void onResume() {
        if (mSubscriptionManager != null) {
            mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        }
    }

    @Override
    public void onPause() {
        if (mSubscriptionManager != null) {
            mSubscriptionManager.removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(mPickupCallKey);
        if (preference != null) {
            mPreference = preference;
        }
    }

}

