package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import com.android.systemui.R;

import com.android.systemui.FontSizeUtils;

import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.android.systemui.statusbar.policy.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.Dependency;

/**
 * Created by smartron on 24/1/18.
 */

public class NetworkSpeed extends TextView implements DarkIconDispatcher.DarkReceiver,
        ConfigurationController.ConfigurationListener {

    private final boolean mShowDark;
    private long mNewTotal, mOldTotal, mLastUpdateTime;
    private boolean mShow = false;
    private Context mContext;
    private boolean mAttached;
    private static final float KB = 1024;
    private static final float MB = KB * KB, GB = MB * KB;
    private static final int REPEAT = 0;
    private static final int STATE_CHANGE = 1;

    private DecimalFormat mDecimalFormat = new DecimalFormat("0.#");

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mTrafficHandler.sendEmptyMessage(REPEAT);
        }
    };

    private Handler mTrafficHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long time = SystemClock.elapsedRealtime() - mLastUpdateTime;
            if (time < 500) {
                if (msg.what != STATE_CHANGE) {
                    return;
                }
                if (time < 1) {
                    time = Long.MAX_VALUE;
                }
            }
            mLastUpdateTime = SystemClock.elapsedRealtime();
            if (!isConnected() || !mShow) {
                mTrafficHandler.removeCallbacksAndMessages(null);
                setVisibility(View.GONE);
            } else {
                mNewTotal = TrafficStats.getTotalRxBytes();
                long speed = (long) ((mNewTotal - mOldTotal) / (time / 1000F));
                String output = new String();
                if (speed < KB * 100 )
                    output = mDecimalFormat.format((float)(speed / KB))+"KB/S";
                else if (speed < MB * 100 )
                    output = mDecimalFormat.format((float)(speed / MB))+"MB/S";
                else
                    output = mDecimalFormat.format((float)(speed / GB))+"GB/S";

                setText(output);
                setVisibility(View.VISIBLE);
            }
            mOldTotal = mNewTotal;
            mTrafficHandler.removeCallbacksAndMessages(null);
            mTrafficHandler.postDelayed(mRunnable, 1000);
        }

    };

    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                updateSettings();
            }
        }
    };

    public NetworkSpeed(Context context) {
        this(context, null);
    }


    public NetworkSpeed(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkSpeed(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.Clock,
                0, 0);
        try {
            mShowDark = a.getBoolean(R.styleable.Clock_showDark, true);
        } finally {
            a.recycle();
        }
        updateSettings();
    }

    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = (connectivityManager != null) ? connectivityManager.getActiveNetworkInfo() : null;
        return (networkInfo != null) ? networkInfo.isConnected() : false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mIntentReceiver, filter, null, getHandler());
            mContext.getContentResolver().registerContentObserver(Settings.Global.
                    getUriFor(Settings.Global.SHOW_STATUS_BAR_NETWORK_SPEED), false, mSettingsObserver);
            if (mShowDark)
                Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);

        }
        updateSettings();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mContext.unregisterReceiver(mIntentReceiver);
            mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
            if (mShowDark)
                Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
            mAttached = false;
        }
    }

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mShow = Settings.Global.getInt(resolver, Settings.Global.SHOW_STATUS_BAR_NETWORK_SPEED, 0) == 1;
        if (isConnected() && mShow) {
            if (mAttached) {
                mOldTotal = TrafficStats.getTotalRxBytes();
                mLastUpdateTime = SystemClock.elapsedRealtime();
                mTrafficHandler.sendEmptyMessage(STATE_CHANGE);
            }
            setVisibility(View.VISIBLE);
            return;
        }
        setVisibility(View.GONE);
    }

    @Override
    public void onDensityOrFontScaleChanged() {
        FontSizeUtils.updateFontSize(this, R.dimen.status_bar_clock_size);
        setPaddingRelative(
                mContext.getResources().getDimensionPixelSize(
                        R.dimen.status_bar_clock_starting_padding),
                0,
                mContext.getResources().getDimensionPixelSize(
                        R.dimen.status_bar_clock_end_padding),
                0);
    }

    @Override
    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        setTextColor(DarkIconDispatcher.getTint(area, this, tint));
    }
}
