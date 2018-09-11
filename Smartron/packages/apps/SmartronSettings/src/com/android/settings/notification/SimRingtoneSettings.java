/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.Intent;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import com.android.settings.SettingsActivity;
import com.android.settings.DefaultRingtonePreference;
import com.android.settings.RingtonePreference;
import com.android.settings.SettingsPreferenceFragment;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.net.Uri;
import android.util.Log;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;

/**
 * Created by G Murali Madhav on 05/06/17.
 */

public class SimRingtoneSettings extends SettingsPreferenceFragment {

    private static final String TAG = "SimRingtoneSettings";
    private static final String KEY_FIRST_SIM_RINGTONE = "first_sim_ringtone_preference";
    private static final String KEY_SECOND_SIM_RINGTONE = "second_sim_ringtone_preference";
    private static final String SELECTED_PREFERENCE_KEY = "selected_preference";
    private static final int REQUEST_CODE = 200;

    private Context mContext;
    private final H mHandler = new H();
    private DefaultRingtonePreference mFirstSimRingtonePreference;
    private DefaultRingtonePreference mSecondSimRingtonePreference;
    private RingtonePreference mRequestPreference;
    private String[] mTitle = new String[2];

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SOUND;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        addPreferencesFromResource(R.xml.sim_ringtone_settings);

        mFirstSimRingtonePreference = (DefaultRingtonePreference) getPreferenceScreen().findPreference(KEY_FIRST_SIM_RINGTONE);
        mFirstSimRingtonePreference.setPhoneId(0);

        mSecondSimRingtonePreference = (DefaultRingtonePreference) getPreferenceScreen().findPreference(KEY_SECOND_SIM_RINGTONE);
        mSecondSimRingtonePreference.setPhoneId(1);


        if (savedInstanceState != null) {
            String selectedPreference = savedInstanceState.getString(SELECTED_PREFERENCE_KEY, null);
            if (!TextUtils.isEmpty(selectedPreference)) {
                mRequestPreference = (RingtonePreference) findPreference(selectedPreference);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        lookupRingtoneNames();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && data.getData() != null) {
            data.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, data.getData());
        }
        if (mRequestPreference != null) {
            mRequestPreference.onActivityResult(requestCode, resultCode, data);
            mRequestPreference = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mRequestPreference != null) {
            outState.putString(SELECTED_PREFERENCE_KEY, mRequestPreference.getKey());
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RingtonePreference) {
            mRequestPreference = (RingtonePreference) preference;
            /*Intent intent = new Intent(mContext, SelectRingtoneSettings.class);
            mRequestPreference.onPrepareRingtonePickerIntent(intent);
            intent.putExtra("title", mTitle[mRequestPreference.getPhoneId()]);
            startActivityForResult(intent, REQUEST_CODE);*/
            // Using AOSP Ringtone settings(As per UX/UI team)
            mRequestPreference.onPrepareRingtonePickerIntent(mRequestPreference.getIntent());
            startActivityForResult(preference.getIntent(), REQUEST_CODE);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private final class H extends Handler {
        private static final int UPDATE_FIRSTSIM_RINGTONE = 1;
        private static final int UPDATE_SECONDSIM_RINGTONE = 2;

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_FIRSTSIM_RINGTONE:
                    mFirstSimRingtonePreference.setSummary((CharSequence) msg.obj);
                    mTitle[0] = (String) msg.obj;
                    break;
                case UPDATE_SECONDSIM_RINGTONE:
                    mSecondSimRingtonePreference.setSummary((CharSequence) msg.obj);
                    mTitle[1] = (String) msg.obj;
                    break;
            }
        }
    }

    private void lookupRingtoneNames() {
        AsyncTask.execute(mLookupRingtoneNames);
    }

    private final Runnable mLookupRingtoneNames = new Runnable() {
        @Override
        public void run() {
            if (mFirstSimRingtonePreference != null) {
                final CharSequence summaryOne = updateRingtoneName(mContext, 0);
                if (summaryOne != null) {
                    mHandler.obtainMessage(H.UPDATE_FIRSTSIM_RINGTONE, summaryOne).sendToTarget();
                }
            }
            if (mSecondSimRingtonePreference != null) {
                final CharSequence summaryTwo = updateRingtoneName(mContext, 1);
                if (summaryTwo != null) {
                    mHandler.obtainMessage(H.UPDATE_SECONDSIM_RINGTONE, summaryTwo).sendToTarget();
                }
            }
        }
    };

    private CharSequence updateRingtoneName(Context context, int phoneId) {
        if (context == null) {
            Log.e(TAG, "Unable to update ringtone name, no context provided");
            return null;
        }
        Uri ringtoneUri = RingtoneManager.getActualRingtoneUriByPhoneId(context, phoneId);
        return Ringtone.getTitle(
            context, ringtoneUri, false /* followSettingsUri */, true /* allowRemote */);
    }

}
