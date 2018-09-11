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
 * limitations under the License
 */

package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.R.drawable;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.SecureSetting;
import com.android.systemui.qs.tileimpl.QSTileImpl;

/** Quick settings tile: Control Reading Mode **/
public class ReadingModeTile extends QSTileImpl<BooleanState> {

    private final Icon mEnable
            = ResourceIcon.get(R.drawable.ic_signal_reading_mode_enable_animation);
    private final Icon mDisable
            = ResourceIcon.get(R.drawable.ic_signal_reading_mode_disable_animation);
    private static final String ACTION_READING_MODE = "com.smartron.intent.action.READING_MODE";
    private final SecureSetting mSetting;

    public ReadingModeTile(QSHost host) {
        super(host);
        mSetting = new SecureSetting (mContext, mHandler,
                Settings.Secure.FULL_READING_MODE_ENABLED) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(value);
            }
        };
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleSetListening(boolean listening) {

    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(); // Nothing to do
    }

    @Override
    protected void handleClick() {
        MetricsLogger.action(mContext, getMetricsCategory(), !mState.value);
        mSetting.setValue(mState.value ? 0 : 1);
        refreshState ();
    }

    @Override
    protected void handleLongClick() {
        mContext.startActivity (new Intent(ACTION_READING_MODE));
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_reading_mode_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final int value = arg instanceof Integer ? (Integer) arg : mSetting.getValue();
        final boolean enabled = value == 1;
        state.value = enabled;
        state.label = mContext.getString(R.string.quick_settings_reading_mode_label);
        state.icon = enabled ? mEnable : mDisable;
        state.contentDescription = state.label;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_READING_MODE;
    }
}
