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

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.os.PowerManager;
import android.content.Context;
import android.os.SystemClock;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.R;
import com.android.systemui.R.drawable;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

/** Quick settings tile: Control Lock Screen **/
public class LockScreenTile extends QSTileImpl<BooleanState> {

    private final AnimationIcon mScreeLock
            = new AnimationIcon(R.drawable.ic_qs_settings_lockscreen,
            R.drawable.ic_qs_settings_lockscreen);
    private final PowerManager mPowerManager;

    public LockScreenTile(QSHost host) {
        super(host);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
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
        mPowerManager.goToSleep(SystemClock.uptimeMillis());
    }

    @Override
    protected void handleLongClick() {
        handleClick();
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_lock_screen_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = mHost.getContext().getString(R.string.quick_settings_lock_screen_label);
        state.icon = mScreeLock;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_LOCKSCREEN;
    }
}