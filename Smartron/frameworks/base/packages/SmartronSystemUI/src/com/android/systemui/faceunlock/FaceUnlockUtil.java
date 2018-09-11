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

package com.android.systemui.faceunlock;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.policy.IFaceUnlockCallback;
import com.android.internal.telephony.IccCardConstants;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.phone.KeyguardBottomAreaView;
import com.android.systemui.statusbar.phone.LockIcon;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import android.widget.ImageView;

/**
 * Created by lirenqi on 2018/02/01.
 */
public class FaceUnlockUtil {
    private static final boolean DEBUG = true;
    public final static String TAG = "FaceUnlockUtil";
    public final static String FACE_UNLOCK_VERSION = "v2.7-20180425";

    // used for handler messages
    private static final int FACE_UNLOCK_SHOW_UNLOCK_SCREEN = 0x01;
    private static final int FACE_UNLOCK_SHOW_MESSAGE = 0x02;
    private static final int FACE_UNLOCK_RETRY = 0x03;
    private static final int FACE_UNLOCK_LAUNCH_CAMERA = 0x04;
    private static final int FACE_UNLOCK_KEYGUARD_DONE = 0x05;
    private static final int FACE_UNLOCK_UNPDATE_LOCK_ICON = 0x06;
    
    private static final int FACE_UNLOCK_DELAY_TIME_MS = 500;
	
    private static final int FACE_UNLOCK_STATUS_NONE = 0x11;
    public static final int FACE_UNLOCK_STATUS_UNLOCK = 0x12;
    public static final int FACE_UNLOCK_STATUS_FAILED = 0x13;
    public static final int FACE_UNLOCK_STATUS_COUNT = 0x14;
    
    public static final int FACE_UNLOCK_RETRY_IN_KEYGUARD = 0x01;
    public static final int FACE_UNLOCK_RETRY_IN_PASSWORD = 0x02;
    
    private static final int SHOW_TIPS_REASON_START = 0x01;
    private static final int SHOW_TIPS_REASON_FAILED = 0x02;
    private static final int SHOW_TIPS_REASON_COUNT = 0x03;
    
    private static FaceUnlockUtil mInstance;
    private static Context mContext;
    private static IFaceUnlockCallback mFaceUnlockCallback=null;
    private static String mShowString=null;
    
    private static StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private static KeyguardIndicationController mKeyguardIndicationController;
    private static KeyguardBottomAreaView mKeyguardBottomArea;
    private static LockIcon mLockIcon;
    
    private static int mFaceUnlockStatus = FACE_UNLOCK_STATUS_NONE;
    private static int mFaceUnlockRetryCount = 0;
    private static final int FACE_UNLOCK_MAX_NUM = 3;

    private static String mLastCameraLaunchSource = KeyguardBottomAreaView.CAMERA_LAUNCH_SOURCE_AFFORDANCE;
    
    private static int mTouchTargetSize = -1;
    private static boolean mOnCenterIcon = false;
    private static boolean mKeyguardUnlocked = false;
    private static AnimationDrawable animationDrawable;
    private static ImageView mFaceImage = null;
    
    private static long mLastTime = 0L;
    private static PowerManager.WakeLock mWakeLock = null;
    
	
    private FaceUnlockUtil(){
    }

    public static FaceUnlockUtil getInstance(){
        if(mInstance==null){
            mInstance=new FaceUnlockUtil();
        }
        return mInstance;
    }

    public void doUnlock() {
        if (DEBUG) Slog.d(TAG, "doUnlock");        
        mHandler.sendEmptyMessage(FACE_UNLOCK_KEYGUARD_DONE);           
    }
    
    private void keyguardDone() {
        if (null != mStatusBarKeyguardViewManager) {
            mStatusBarKeyguardViewManager.notifyKeyguardAuthenticated(false);
        }
    }
    
    public void registerFaceUnlockCallback(IFaceUnlockCallback callback) {
        if (DEBUG) Slog.d(TAG, "registerFaceUnlockCallback, callback = " + callback);
        mFaceUnlockCallback = callback;
    }
    
    public void showTipsOrAnimation(int reason, String tips) {
        if (DEBUG) Slog.d(TAG, "showTipsOrAnimation reason = " + reason + "; tips = " + tips);
        if (reason == SHOW_TIPS_REASON_START) {
        	mFaceUnlockStatus = FACE_UNLOCK_STATUS_UNLOCK;
            mShowString = "";
            mHandler.sendEmptyMessageDelayed(FACE_UNLOCK_SHOW_MESSAGE,FACE_UNLOCK_DELAY_TIME_MS);
        } else if (reason == SHOW_TIPS_REASON_FAILED) {
            mFaceUnlockStatus = FACE_UNLOCK_STATUS_FAILED;
            mShowString = tips;
            mHandler.sendEmptyMessage(FACE_UNLOCK_SHOW_MESSAGE);
        } else if (reason == SHOW_TIPS_REASON_COUNT) {
        	mFaceUnlockStatus = FACE_UNLOCK_STATUS_COUNT;
            mHandler.sendEmptyMessage(FACE_UNLOCK_UNPDATE_LOCK_ICON);
            mShowString = tips;
        }
    }

    private void showUnLockScreenAgain() {
        if (DEBUG) Slog.d(TAG, "showUnLockScreenAgain mFaceUnlockStatus = " + mFaceUnlockStatus);        
        mHandler.sendEmptyMessage(FACE_UNLOCK_SHOW_MESSAGE);
    }
    
    private void updateMsgIcon(String message, int color) {
    	if (DEBUG) Slog.d(TAG, "updateMsgIcon message = " + message);
        if(mStatusBarKeyguardViewManager != null && mStatusBarKeyguardViewManager.isBouncerShowing()) {
            mStatusBarKeyguardViewManager.showBouncerMessage(message, color);
            runKeyguardAnimate();
        } else {
            if (mKeyguardIndicationController != null && message!=null && !message.isEmpty()) {
                mKeyguardIndicationController.showTransientIndication(message, color);
            } else {
                mKeyguardIndicationController.hideTransientIndication();
            }
        	
            if (mLockIcon != null) {
                mLockIcon.update(true);
            }
        }
    }

    /**
     * @see #FACE_UNLOCK_SHOW_UNLOCK_SCREEN
     */
    private void handleShowUnlockScreen() {
        synchronized(this) {
            if (DEBUG) Slog.d(TAG, "handleShowUnlockScreen");
            if (null != mStatusBarKeyguardViewManager && !mStatusBarKeyguardViewManager.isBouncerShowing()) {
                mStatusBarKeyguardViewManager.animateCollapsePanels(1.3f);
            }
            mHandler.removeMessages(FACE_UNLOCK_SHOW_UNLOCK_SCREEN);
            if (null != mShowString) {
                mHandler.sendEmptyMessageDelayed(FACE_UNLOCK_SHOW_MESSAGE, FACE_UNLOCK_DELAY_TIME_MS);
            }
        }
    }

    /**
     * @see #FACE_UNLOCK_SHOW_MESSAGE
     */
    private void handleShowMessage() {
        synchronized(this) {
            if (mShowString != null) {    
            	int mColor = Color.WHITE;
                updateMsgIcon(mShowString,mColor);
            }
            mHandler.removeMessages(FACE_UNLOCK_SHOW_MESSAGE);
        }
    }

    /**
     * @see #FACE_UNLOCK_LAUNCH_CAMERA
     */
    private void handleLaunchCamera() {
        synchronized(this) {
            if (mKeyguardBottomArea != null && mLastCameraLaunchSource != null) {    
                mKeyguardBottomArea.launchCamera(mLastCameraLaunchSource);
            }
            mHandler.removeMessages(FACE_UNLOCK_LAUNCH_CAMERA);
        }
    }

    /**
     * @see #FACE_UNLOCK_UNPDATE_LOCK_ICON
     */
    private void handleUpdateLockIcon() {
        synchronized(this) {
            if (mLockIcon != null) {
                mLockIcon.update(true);
            }
            mHandler.removeMessages(FACE_UNLOCK_UNPDATE_LOCK_ICON);
            mHandler.sendEmptyMessage(FACE_UNLOCK_SHOW_UNLOCK_SCREEN);
        }
    }

    /**
     * This handler will be associated with the policy thread, which will also
     * be the UI thread of the keyguard.  Since the apis of the policy, and therefore
     * this class, can be called by other threads, any action that directly
     * interacts with the keyguard ui should be posted to this handler, rather
     * than called directly.
     */
    private Handler mHandler = new Handler(Looper.myLooper(), null, true /*async*/) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FACE_UNLOCK_SHOW_UNLOCK_SCREEN:
                    handleShowUnlockScreen();
                    break;
                case FACE_UNLOCK_SHOW_MESSAGE:
                	handleShowMessage();
                    break;
                case FACE_UNLOCK_RETRY:
                    startFaceUnlockBefore();
                    break;                    
                case FACE_UNLOCK_LAUNCH_CAMERA:
                	handleLaunchCamera();
                    break;
                case FACE_UNLOCK_KEYGUARD_DONE:
                	keyguardDone();
                    break;
                case FACE_UNLOCK_UNPDATE_LOCK_ICON:
                    handleUpdateLockIcon();
                	break;
            }
        }
    };
    
    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        if (DEBUG) Slog.d(TAG, "setStatusBarKeyguardViewManager");
        mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }
    
    public void setKeyguardIndicationController(KeyguardIndicationController keyguardIndicationController) {
        if (DEBUG) Slog.d(TAG, "setKeyguardIndicationController");
        mKeyguardIndicationController = keyguardIndicationController;
    }
    
    public void setKeyguardBottomAreaView(KeyguardBottomAreaView keyguardBottomAreaView) {
        if (DEBUG) Slog.d(TAG, "setKeyguardBottomAreaView");
        mKeyguardBottomArea = keyguardBottomAreaView;
        setLockIcon(keyguardBottomAreaView.getLockIcon());
    }
    
    public void setLockIcon(LockIcon lockIcon) {
        if (DEBUG) Slog.d(TAG, "setLockIcon");
        mLockIcon = lockIcon;
    }
    
    public void setViewGroup(ViewGroup root) {
        if (DEBUG) Slog.d(TAG, "setViewGroup");
        if (root != null) {
        	mFaceImage = (ImageView) root.findViewById(R.id.face_unlock_retry);
        	mFaceImage.setVisibility(View.GONE);
        }
    }

    public void setContext(Context context) {
        if (mContext == null && context != null) {
            if (DEBUG) Slog.d(TAG, "setContext");
            mContext = context;
            PowerManager mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FaceUnlock Lock");
			Settings.System.putString(mContext.getContentResolver(), "base_ver", FACE_UNLOCK_VERSION);
            mTouchTargetSize = mContext.getResources().getDimensionPixelSize(
                    R.dimen.keyguard_affordance_touch_target_size);
        }
    }

    private void startFaceUnlock() {
        boolean unlockingAllowed = unlockingAllowed();
        boolean isOwner = isOwner();
        if (DEBUG) Slog.d(TAG, "startFaceUnlock mFaceUnlockCallback = " + mFaceUnlockCallback + "; unlockingAllowed = " + unlockingAllowed + "; isOwner = " + isOwner);
        if (mFaceUnlockCallback != null && isOwner && unlockingAllowed) {
            try {
                mLastTime = System.currentTimeMillis();
                mWakeLock.acquire(30 * 1000);
                mFaceUnlockCallback.doScreenOn();
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception calling doScreenOn():", e);
            }
        }
    }
    
    private void startFaceUnlockBefore() {
    	long mTimeGap = System.currentTimeMillis() - mLastTime;
        if (DEBUG) Slog.d(TAG, "startFaceUnlockBefore mFaceUnlockRetryCount = " + mFaceUnlockRetryCount + "; mTimeGap = " + mTimeGap + "; isOccluded = " + mStatusBarKeyguardViewManager.isOccluded());
        if (mTimeGap < FACE_UNLOCK_DELAY_TIME_MS*3) {
        	mHandler.sendEmptyMessageDelayed(FACE_UNLOCK_RETRY,FACE_UNLOCK_DELAY_TIME_MS);
        } else if (mStatusBarKeyguardViewManager != null && !mStatusBarKeyguardViewManager.isOccluded()) {
            startFaceUnlock();
        } else if (mFaceUnlockRetryCount < FACE_UNLOCK_MAX_NUM) {
            mFaceUnlockRetryCount ++;
            mHandler.sendEmptyMessageDelayed(FACE_UNLOCK_RETRY,FACE_UNLOCK_DELAY_TIME_MS);
        } else {
            Slog.d(TAG, "face unlock failed!");
        }
    }

    public void resetPowerKey() {
        if (mContext != null) {
            Settings.System.putInt(mContext.getContentResolver(), "faceunlock_start", 0);
        }
    }

    private boolean getPowerUnlock() {
        if (mContext != null) {
        	return (Settings.System.getInt(mContext.getContentResolver(), "power_unlock", 0) == 1);
        }
    	return false;
    }

    public void doScreenOn() {
        boolean mStart = false;
        mFaceUnlockRetryCount = 0;
        if (mContext != null) {
            mStart = (Settings.System.getInt(mContext.getContentResolver(), "faceunlock_start", 0) == 1);
            resetPowerKey();
        }
        boolean mPowerUnlock = getPowerUnlock();
        if (DEBUG) Slog.d(TAG, "doScreenOn mStart = " + mStart + "; mFaceUnlockStatus = " + mFaceUnlockStatus + "; isShowing = " + mStatusBarKeyguardViewManager.isShowing() + "; mPowerUnlock = " + mPowerUnlock);
        if (mStart && mPowerUnlock && mFaceUnlockStatus == FACE_UNLOCK_STATUS_NONE && mStatusBarKeyguardViewManager.isShowing()) {
            startFaceUnlockBefore(); 
        } else {
            if (DEBUG) Slog.d(TAG, "cann't start face unlock!!");
        }
    }

    public void doScreenOff() {
        if (DEBUG) Slog.d(TAG, "doScreenOff " + FACE_UNLOCK_VERSION);
        if (mFaceUnlockStatus != FACE_UNLOCK_STATUS_COUNT) {
            mFaceUnlockStatus = FACE_UNLOCK_STATUS_NONE;
        }
        if (mFaceUnlockCallback != null) {
            try {
                mFaceUnlockCallback.doScreenOff();
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception calling doScreenOff():", e);
            }
        }
        mKeyguardUnlocked = false;
        resetPowerKey();
        if (mLockIcon != null) {
            mLockIcon.update(true);
        }
    }

    public void doUnlockSuccess() {
        if (DEBUG) Slog.d(TAG, "doUnlockSuccess");
        if (mFaceUnlockCallback != null) {
            try {
                mFaceUnlockCallback.doUnlockSuccess();
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception calling doUnlockSuccess():", e);
            }
        }
        mFaceUnlockStatus = FACE_UNLOCK_STATUS_NONE;
        resetPowerKey();
        if (mLockIcon != null) {
            mLockIcon.update(true);
        }
    }

    public void startFaceUnlockBySlide() {
        if (DEBUG) Slog.d(TAG, "startFaceUnlockBySlide mFaceUnlockStatus = " + mFaceUnlockStatus);
        if (mFaceUnlockStatus == FACE_UNLOCK_STATUS_NONE) {
            startFaceUnlock();
        }
    }

    public boolean startFaceUnlockByRetry(int style) {
        if (DEBUG) Slog.d(TAG, "startFaceUnlockByRetry mFaceUnlockStatus = " + mFaceUnlockStatus + "; mOnCenterIcon = " + mOnCenterIcon+ "; style = " + style);        
        if (faceUnlockFailed() 
            && ((mOnCenterIcon && (style == FACE_UNLOCK_RETRY_IN_KEYGUARD))
                || (style == FACE_UNLOCK_RETRY_IN_PASSWORD))) {
            startFaceUnlock();
            return true;
        }
        return false;
    }

    public static boolean isFaceUnlockSupport() {
        String support = SystemProperties.get("ro.faceunlock.support", "0");
        return "1".equals(support);
    }

    public void startFaceUnlockService(Context context) {
        if (DEBUG) Slog.d(TAG, "startFaceUnlockService");
    	setContext(context);
        if (isFaceUnlockSupport()) {
            Intent intent = new Intent(context, FaceUnlockService.class); 
            context.startServiceAsUser(intent, UserHandle.CURRENT);
        }
    }
    
    public boolean faceUnlockBeginning() {
        if (mFaceUnlockStatus == FACE_UNLOCK_STATUS_NONE) {
            return true;
        }
        return false;
    }
    
    private boolean faceUnlockRunning() {
        if (mFaceUnlockStatus == FACE_UNLOCK_STATUS_UNLOCK) {
            return true;
        }
        return false;
    }

    private boolean faceUnlockFailed() {
        if (mFaceUnlockStatus == FACE_UNLOCK_STATUS_FAILED) {
            return true;
        }
        return false;
    }

    private boolean unlockingAllowed() {
        return (mContext != null)
                && (KeyguardUpdateMonitor.getInstance(mContext).isUnlockingWithFingerprintAllowed());
    }

    public boolean launchCamera(String source) {
        if (DEBUG) Slog.d(TAG, "launchCamera source = " + source);
        doUnlockSuccess();
        mLastCameraLaunchSource = source;
        mHandler.sendEmptyMessageDelayed(FACE_UNLOCK_LAUNCH_CAMERA,FACE_UNLOCK_DELAY_TIME_MS);
        return true;
    }

    private static boolean isOwner() {
        boolean isSecondaryUser = (UserHandle.myUserId() != UserHandle.USER_OWNER) ||
                (ActivityManager.getCurrentUser() != UserHandle.USER_OWNER);
        return !isSecondaryUser;
    }

    public void setOnCenterIcon(float x, float y) {
        if (mLockIcon != null && mLockIcon.getVisibility() == View.VISIBLE) {
            float iconX = mLockIcon.getX() + mLockIcon.getWidth() / 2.0f;
            float iconY = mLockIcon.getY() + mLockIcon.getHeight() / 2.0f;
            double distance = Math.hypot(x - iconX, y - iconY);
            if (distance <= mTouchTargetSize / 2) {
                mOnCenterIcon = true;
                return;
            }
        }
        mOnCenterIcon = false;
    }

    public boolean isKeyguardUnlocked(boolean locked) {
        if (locked || isFaceUnlockSupport()) {
            return false;
        } else if (mKeyguardUnlocked) {
            return true;
        } else {
            mKeyguardUnlocked = true;
            return false;
        }
    }

    private boolean animationRun(boolean isLockIcon) {
        if (faceUnlockRunning() && mContext != null) {
            animationDrawable = (AnimationDrawable) mContext
                    .getDrawable(R.drawable.face_unlock_anim);
            if (isLockIcon) {
                mLockIcon.setImageDrawable(animationDrawable);
            } else {
                mFaceImage.setImageDrawable(animationDrawable);
                if (mLockIcon != null) {
                    mLockIcon.update(true);
                }
            }
            
            if (animationDrawable != null
                    && !animationDrawable.isRunning()) {
                animationDrawable.start();
            }
            return true;
        }
        return false;
    }

    private boolean animationStop(boolean isLockIcon) {
        if (faceUnlockFailed() && mContext != null) {
            if (isLockIcon) {
            	mLockIcon.setImageDrawable(mContext
                        .getDrawable(R.drawable.faceunlock_bg));
            } else {
            	mFaceImage.setImageDrawable(mContext
                        .getDrawable(R.drawable.faceunlock_bg));
            }
            
            if (animationDrawable != null
                    && animationDrawable.isRunning()) {
                animationDrawable.stop();
            }
            return true;
        }
        return false;
    }

    public boolean setLockIconBg(boolean showMsg) {
        if (!unlockingAllowed()) {
            if (null != mFaceImage) {
                mFaceImage.setVisibility(View.GONE);
            }
            mFaceUnlockStatus = FACE_UNLOCK_STATUS_NONE;
            return false;
        }

        if (null != mFaceImage && mFaceImage.getVisibility() == View.VISIBLE) {
            return false;
        }

        if (getPowerUnlock() && mLockIcon != null) {
            if (animationRun(true)) {
                return true;
            } else if (animationStop(true)) {
                if (showMsg) {
                    showUnLockScreenAgain();
                }
                return true;
            }
        }
        return false;
    }

    public void runKeyguardAnimate() {
        if (mFaceImage != null) {
            mFaceImage.setVisibility(View.VISIBLE);
            if (animationRun(false)) {

            } else if (animationStop(false)) {
                showUnLockScreenAgain() ;
                mFaceImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startFaceUnlockByRetry(FACE_UNLOCK_RETRY_IN_PASSWORD);
                    }
                });
            } else if (mFaceUnlockStatus == FACE_UNLOCK_STATUS_COUNT) {
                showUnLockScreenAgain();
                mFaceImage.setVisibility(View.GONE);
            } else {
                mFaceImage.setVisibility(View.GONE);
            }
        }        
    }

    public void hideKeyguardBouncer() {
        if (mFaceImage != null) {
            mFaceImage.setVisibility(View.INVISIBLE);
            mShowString = "";
            showUnLockScreenAgain();
        }
    }
        
}
