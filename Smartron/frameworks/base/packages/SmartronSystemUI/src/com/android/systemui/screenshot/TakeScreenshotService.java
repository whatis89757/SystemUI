/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.screenshot;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserManager;
import android.util.Log;
import android.view.WindowManager;

public class TakeScreenshotService extends Service {
    private static final String TAG = "TakeScreenshotService";

    private static GlobalScreenshot mScreenshot;

    private boolean mIsScreenShotInProgress; //to block the screenshot from multiple places like from QS and hardkeys

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            final Messenger callback = msg.replyTo;
            final FinisherRunnable finisher = new FinisherRunnable(callback);

            // If the storage for this user is locked, we have no place to store
            // the screenshot, so skip taking it instead of showing a misleading
            // animation and error notification.
            if (!getSystemService(UserManager.class).isUserUnlocked()) {
                Log.w(TAG, "Skipping screenshot because storage is locked!");
                post(finisher);
                return;
            }
            Log.d(TAG,"ScreenShot State :"+mIsScreenShotInProgress);

            if(mIsScreenShotInProgress){
                Log.w(TAG, "Service busy in taking screenshot!");
                post(finisher);
                return;
            }


            if (mScreenshot == null) {
                mScreenshot = new GlobalScreenshot(TakeScreenshotService.this);
                takeScreenShot(msg,finisher);
            }else if(!mScreenshot.isAnimationEnd()){
                Log.d(TAG,"Animation End Screenshot");
                mScreenshot.clearAnimation();
                final Message message = new Message();
                message.copyFrom(msg);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG,"Late Screenshot"+message.what+" "+message.arg1+" "+message.arg2);
                        takeScreenShot(message,finisher);
                    }
                },100);
            } else{
                Log.d(TAG,"mScreenshot not null");
                takeScreenShot(msg,finisher);
            }
        }

        private void takeScreenShot(Message msg,FinisherRunnable finisher){
            mIsScreenShotInProgress = true;
            switch (msg.what) {
                case WindowManager.TAKE_SCREENSHOT_FULLSCREEN:
                    mScreenshot.takeScreenshot(finisher, msg.arg1 > 0, msg.arg2 > 0);
                    break;
                case WindowManager.TAKE_SCREENSHOT_SELECTED_REGION:
                    mScreenshot.takeScreenshotPartial(finisher, msg.arg1 > 0, msg.arg2 > 0);
                    break;
            }
        }
    };

    class FinisherRunnable implements Runnable{

        private Messenger mCallback;

        private boolean mIsClear;

        public FinisherRunnable(Messenger callback){
            mCallback = callback;
        }

        @Override
        public void run() {
            Message reply = Message.obtain(null, 1);
            try {
                if(mIsClear){
                    TakeScreenshotService.this.mIsScreenShotInProgress = false;
                }
                mCallback.send(reply);
            } catch (RemoteException e) {
            }
        }

        public void clear(boolean isClear){
            mIsClear = isClear;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Messenger(mHandler).getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mScreenshot != null) mScreenshot.stopScreenshot();
        return true;
    }
}
