/*
 * Copyright (C) 2007-2008 The Android Open Source Project
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

package com.android.server.ram;

import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Slog;
import android.util.TimeUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;

import com.android.internal.util.MemInfoReader;

import dalvik.system.VMRuntime;

/**
 * This class implements a service to monitor the amount of RAM
 * on the device.  If the free RAM is less
 * than a tunable threshold value a low memory notification is displayed
 * to alert the user. If the user clicks on the low memory notification the
 * Application Manager application gets launched to let the user free
 * RAM.
 */
public class DeviceRamMonitorService extends SystemService {
    static final String TAG = "DeviceRamMonitorService";

    static final boolean localLOGV = false;

    static final int DEVICE_RAM_WHAT = 1;
    static final int DEVICE_SENT_NOTIFICATION_WHAT = 2;
    private static final int MONITOR_INTERVAL = 1; //in minutes
    private static final int SENT_NOTIFICATION_INTERVAL = 5; //in minutes
    private static final int LOW_RAM_NOTIFICATION_ID = 100;

    private static final long DEFAULT_CHECK_INTERVAL = MONITOR_INTERVAL*60*1000;
    private static final long DEFAULT_SENT_NOTIFICATION_INTERVAL = SENT_NOTIFICATION_INTERVAL * 60 * 1000;

    private long mFreeRam;
    boolean mLowRamFlag = false;
    boolean mCanSentNotification = true;

    // This is the raw threshold that has been set at which we consider
    // storage to be low.
    long mRamLowThreshold = 10 * 1024 * 1024; // 10M

    MemInfoReader mMemInfoReader = new MemInfoReader();

    /**
    * Handler that checks the amount of Ram on the device and sends a
    * notification if the device runs low RAM.
    */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DEVICE_SENT_NOTIFICATION_WHAT:
                    mCanSentNotification = true;
                case DEVICE_RAM_WHAT:
                    checkRamMemory();
                    break;
            }
        }
    };

    //read current free ram.
    private void restatRam() {
        mMemInfoReader.readMemInfo();
        mFreeRam = mMemInfoReader.getFreeSize() + mMemInfoReader.getCachedSize();
        // Allow mFreeRam to be overridden by debug.freeram for testing
        String debugFreeRam = SystemProperties.get("debug.freeram");
        if (!"".equals(debugFreeRam)) {
            mFreeRam = Long.parseLong(debugFreeRam);
        }
    }

    void checkRamMemory() {
            restatRam();
            if (localLOGV)  Slog.v(TAG, "freeRam="+mFreeRam);

            //post intent to NotificationManager to display icon if necessary
            if (mFreeRam < mRamLowThreshold) {
                if (mCanSentNotification) {
                    Slog.i(TAG, "Running low on memory. Sending notification");
                    sendNotification();
                    mLowRamFlag = true;
                    postSentNotificationMsg(DEFAULT_SENT_NOTIFICATION_INTERVAL);
                } else {
                    if (localLOGV) Slog.v(TAG, "Running low on memory " +
                            "notification already sent. do nothing");
                }
            } else {
                if (mLowRamFlag) {
                    Slog.i(TAG, "Memory available. Cancelling notification");
                    cancelNotification();
                    mLowRamFlag = false;
                    mCanSentNotification = true;
                    mHandler.removeMessages(DEVICE_SENT_NOTIFICATION_WHAT);
                }
            }
        if(localLOGV) Slog.i(TAG, "Posting Message again");
        //keep posting messages to itself periodically
        postCheckMemoryMsg(true, DEFAULT_CHECK_INTERVAL);
    }

    void postCheckMemoryMsg(boolean clearCache, long delay) {
        // Remove queued messages
        mHandler.removeMessages(DEVICE_RAM_WHAT);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(DEVICE_RAM_WHAT),
                delay);
    }

    void postSentNotificationMsg(long delay) {
        mCanSentNotification = false;
        // Remove queued messages
        mHandler.removeMessages(DEVICE_SENT_NOTIFICATION_WHAT);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(DEVICE_SENT_NOTIFICATION_WHAT),
                delay);
    }

    public DeviceRamMonitorService(Context context) {
        super(context);
    }

    /**
    * Initializes the RAM threshold value and posts an empty message to
    * kickstart the process.
    */
    @Override
    public void onStart() {
        checkRamMemory();
    }

    /**
    * This method sends a notification to NotificationManager to display
    * an error dialog indicating low RAM and lead user to free up RAM.
    */
    private void sendNotification() {
        final Context context = getContext();
        context.getSystemService("statusbar");
        if(localLOGV) Slog.i(TAG, "Sending low RAM notification");
        //  Pack up the values and broadcast them to everyone
        Intent lowRamIntent = new Intent();
        lowRamIntent.setClassName("com.android.systemui",
                "com.android.systemui.recents.RecentsActivity");
        lowRamIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        NotificationManager mNotificationMgr =
                (NotificationManager)context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        CharSequence title = context.getText(
                com.android.internal.R.string.low_ram_memory_view_title);
        CharSequence details = context.getText(
                com.android.internal.R.string.low_ram_memory_view_text);
        PendingIntent intent = PendingIntent.getActivityAsUser(context, 0,  lowRamIntent, 0,
                null, UserHandle.CURRENT);
        Notification notification = new Notification.Builder(context)
                .setSmallIcon(com.android.internal.R.drawable.stat_notify_disk_full)
                .setTicker(title)
                .setColor(context.getResources().getColor(
                    com.android.internal.R.color.system_notification_accent_color))
                .setContentTitle(title)
                .setContentText(details)
                .setContentIntent(intent)
                .setStyle(new Notification.BigTextStyle()
                      .bigText(details))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_SYSTEM)
                .build();
        mNotificationMgr.notifyAsUser(null, LOW_RAM_NOTIFICATION_ID, notification,
                UserHandle.ALL);
    }

    /**
     * Cancels low Ram notification.
     */
    private void cancelNotification() {
        final Context context = getContext();
        if(localLOGV) Slog.i(TAG, "Canceling low RAM notification");
        NotificationManager mNotificationMgr =
                (NotificationManager)context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        //cancel notification since memory has been freed
        mNotificationMgr.cancelAsUser(null, LOW_RAM_NOTIFICATION_ID, UserHandle.ALL);
    }
}