/*
 * Copyright (c) 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import android.content.Intent;

import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.Messenger;
import android.os.IBinder;

import com.android.internal.app.NightDisplayController;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.R;
import com.android.systemui.R.drawable;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class ScreenshotTile extends QSTileImpl<BooleanState> {

    private QSTileHost mQsTileHost;

    public ScreenshotTile(QSHost host) {
        super(host);
        if(host instanceof QSTileHost){
            mQsTileHost = (QSTileHost) host;
        }
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleSetListening(boolean listening) {

    }

    @Override
    protected void handleClick() {
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mQsTileHost != null)
                    startScreenShot();
            }
        });
        MetricsLogger.action(mContext, getMetricsCategory(), true);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = mContext.getString(R.string.quick_settings_screen_shot_label);
        state.contentDescription = state.label;
        state.icon = ResourceIcon.get(R.drawable.ic_signal_screen_shot_animation);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_SCREENSHOT;
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent();//Nothing to do
    }

    private void startScreenShot(){
         //swipe up the page
        if (mQsTileHost == null)
            return;
        mQsTileHost.getStatusBar().onBackPressed(true);
        //take screenshot
        mUiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                takeScreenshot();
            }
        },750);
    }

    /**
     * functions needed for taking screenhots.
     */
    final Object mScreenshotLock = new Object();
    ServiceConnection mScreenshotConnection = null;
    final Runnable mScreenshotTimeout = new Runnable() {
        @Override public void run() {
            synchronized (mScreenshotLock) {
                if (mScreenshotConnection != null) {
                    mContext.unbindService(mScreenshotConnection);
                    mScreenshotConnection = null;
                }
            }
        }
    };
    private void takeScreenshot() {
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return;
            }
            ComponentName cn = new ComponentName("com.android.systemui",
                    "com.android.systemui.screenshot.TakeScreenshotService");
            Intent intent = new Intent();
            intent.setComponent(cn);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, 1);
                        final ServiceConnection myConn = this;
                        Handler h = new Handler(mHandler.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotConnection == myConn) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        mHandler.removeCallbacks(mScreenshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg1 = msg.arg2 = 0;
                        /*  remove for the time being
                        if (mStatusBar != null && mStatusBar.isVisibleLw())
                            msg.arg1 = 1;
                        if (mNavigationBar != null && mNavigationBar.isVisibleLw())
                            msg.arg2 = 1;
                         */
                        /* wait for the dialog box to close */
                       // try {
                         //   Thread.sleep(500);
                       // } catch (InterruptedException ie) {
                            // Do nothing
                       // }
                        /* take the screenshot */
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                            // Do nothing
                        }
                    }
                }
                @Override
                public void onServiceDisconnected(ComponentName name) {}
            };
            if (mContext.bindServiceAsUser(
                    intent, conn, Context.BIND_AUTO_CREATE, UserHandle.CURRENT)) {
                mScreenshotConnection = conn;
                mHandler.postDelayed(mScreenshotTimeout, 10000);
            }
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_screen_shot_label);
    }
}
