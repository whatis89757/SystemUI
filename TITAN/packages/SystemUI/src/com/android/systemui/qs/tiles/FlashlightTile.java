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

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.FlashlightController;

/** Quick settings tile: Control flashlight **/
public class FlashlightTile extends QSTile<QSTile.BooleanState> implements
        FlashlightController.FlashlightListener {

    /** Grace period for which we consider the flashlight
     * still available because it was recently on. */
    private static final long RECENTLY_ON_DURATION_MILLIS = 500;
    
    LightObserver observer;
    private boolean mListening = false;

    private final AnimationIcon mEnable
            = new AnimationIcon(R.drawable.ic_signal_flashlight_enable_animation);
    private final AnimationIcon mDisable
            = new AnimationIcon(R.drawable.ic_signal_flashlight_disable_animation);
    private final FlashlightController mFlashlightController;
    private long mWasLastOn;

    public FlashlightTile(Host host) {
        super(host);
        mFlashlightController = host.getFlashlightController();
        mFlashlightController.addListener(this);
        observer = new LightObserver(mHandler);
        observer.startObserving();
        
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        mFlashlightController.removeListener(this);
        observer.endObserving();
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
    }

    @Override
    protected void handleClick() {
        if (ActivityManager.isUserAMonkey()) {
            return;
        }
      
        boolean newState = !mState.value;
        mFlashlightController.setFlashlight(newState);
        refreshState(newState ? UserBoolean.USER_TRUE : UserBoolean.USER_FALSE);
        android.util.Log.d("ljj", "handleClick()  newState  :"+newState);
        saveFlashLightStatus(newState);
        
        //android.util.Log.i("ljj", "saveFlashLightStatus:"+newState);
        
    }
    
    public void saveFlashLightStatus(boolean newState){
       ContentResolver resolver = mContext.getContentResolver();
       if(newState){
               Settings.Global.putInt(resolver,Settings.Global.FLASH_LIGHT_ON,1);
       }else {
               Settings.Global.putInt(resolver,Settings.Global.FLASH_LIGHT_ON,0);
               }
       android.util.Log.i("ljj", "saveFlashLightStatus:"+newState);
       
    }
    
    
    public boolean getFlashLightStatus(){
        ContentResolver resolver = mContext.getContentResolver();
        int i = Settings.Global.getInt(resolver,Settings.Global.FLASH_LIGHT_ON,0);
        android.util.Log.d("ljj", "get  i  :"+i);
        if( i == 1){
        	return true;
        }else {
        	return false;
		}
     }
     


    
    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (state.value) {
            mWasLastOn = SystemClock.uptimeMillis();
        }

        if (arg instanceof UserBoolean) {
            state.value = ((UserBoolean) arg).value;
        }

        if (state.value) {
            mWasLastOn = SystemClock.uptimeMillis();
        }

        if (!state.value && mWasLastOn != 0) {
            if (SystemClock.uptimeMillis() > mWasLastOn + RECENTLY_ON_DURATION_MILLIS) {
                mWasLastOn = 0;
            } else {
                mHandler.removeCallbacks(mRecentlyOnTimeout);
                mHandler.postAtTime(mRecentlyOnTimeout, mWasLastOn + RECENTLY_ON_DURATION_MILLIS);
            }
        }

        // Always show the tile when the flashlight is or was recently on. This is needed because
        // the camera is not available while it is being used for the flashlight.
        state.visible = (!state.value || mWasLastOn != 0) || mFlashlightController.isAvailable();
        state.label = mHost.getContext().getString(R.string.quick_settings_flashlight_label);
        final AnimationIcon icon = state.value ? mEnable : mDisable;
        icon.setAllowAnimation(arg instanceof UserBoolean && ((UserBoolean) arg).userInitiated);
        state.icon = icon;
        int onOrOffId = state.value
                ? R.string.accessibility_quick_settings_flashlight_on
                : R.string.accessibility_quick_settings_flashlight_off;
        state.contentDescription = mContext.getString(onOrOffId);
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_off);
        }
    }

    /** ContentObserver to watch mobile data on/off **/
    private class LightObserver extends ContentObserver {
        public LightObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (DEBUG) Log.i(TAG, "flashlight state change");
            android.util.Log.d("ljj", "flashlight state change  :"+getFlashLightStatus());
            boolean newState = getFlashLightStatus();
            mFlashlightController.setFlashlight(newState);
            refreshState(newState ? UserBoolean.USER_TRUE : UserBoolean.USER_FALSE);
            
        }

        public void startObserving() {
        	android.util.Log.d("ljj", "startObserving");
            mContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.FLASH_LIGHT_ON),
                    false, this);
        }

        public void endObserving() {
        	android.util.Log.d("ljj", "endObserving");
            mContext.getContentResolver().unregisterContentObserver(this);
        }
    }
    
    
    @Override
    public void onFlashlightOff() {
        refreshState(UserBoolean.BACKGROUND_FALSE);
    }

    @Override
    public void onFlashlightError() {
        refreshState(UserBoolean.BACKGROUND_FALSE);
    }

    @Override
    public void onFlashlightAvailabilityChanged(boolean available) {
        refreshState();
    }

    private Runnable mRecentlyOnTimeout = new Runnable() {
        @Override
        public void run() {
            refreshState();
        }
    };
}
