/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.net.NetworkTemplate;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings.Global;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.PreferenceViewHolder;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.Phone;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.CustomDialogPreference;

import java.util.List;

public class CellDataPreference extends CustomDialogPreference implements TemplatePreference {

    private static final String TAG = "CellDataPreference";

    public int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    public boolean mChecked;
    public boolean mMultiSimDialog;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;

    public CellDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs, TypedArrayUtils.getAttr(context,
                android.support.v7.preference.R.attr.switchPreferenceStyle,
                android.R.attr.switchPreferenceStyle));
    }

    @Override
    protected void onRestoreInstanceState(Parcelable s) {
        CellDataState state = (CellDataState) s;
        super.onRestoreInstanceState(state.getSuperState());
        mTelephonyManager = TelephonyManager.from(getContext());
        mChecked = state.mChecked;
        mMultiSimDialog = state.mMultiSimDialog;
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mSubId = state.mSubId;
            setKey(getKey() + mSubId);
        }
        notifyChanged();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        CellDataState state = new CellDataState(super.onSaveInstanceState());
        state.mChecked = mChecked;
        state.mMultiSimDialog = mMultiSimDialog;
        state.mSubId = mSubId;
        return state;
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mListener.setListener(true, mSubId, getContext());
    }

    @Override
    public void onDetached() {
        mListener.setListener(false, mSubId, getContext());
        super.onDetached();
    }

    @Override
    public void setTemplate(NetworkTemplate template, int subId, NetworkServices services) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            throw new IllegalArgumentException("CellDataPreference needs a SubscriptionInfo");
        }
        mSubscriptionManager = SubscriptionManager.from(getContext());
        mTelephonyManager = TelephonyManager.from(getContext());
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mSubId = subId;
            setKey(getKey() + subId);
        }
        updateChecked();
    }

    private void updateChecked() {
        setChecked(mTelephonyManager.getDataEnabled(mSubId));
    }

    @Override
    protected void performClick(View view) {
        final Context context = getContext();
        FeatureFactory.getFactory(context).getMetricsFeatureProvider()
                .action(context, MetricsEvent.ACTION_CELL_DATA_TOGGLE, !mChecked);
        final SubscriptionInfo currentSir = mSubscriptionManager.getActiveSubscriptionInfo(
                    mSubId);
            final SubscriptionInfo nextSir = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        if (mChecked) {
            // disabling data; show confirmation dialog which eventually
            // calls setMobileDataEnabled() once user confirms.
            mMultiSimDialog = false;
            super.performClick(view);
        } else {
            // If we are showing the Sim Card tile then we are a Multi-Sim device.
            if (Utils.showSimCardTile(getContext())) {
                mMultiSimDialog = true;
                if (nextSir != null && currentSir != null &&
                        currentSir.getSubscriptionId() == nextSir.getSubscriptionId()) {
                    setMobileDataEnabled(true);
                   //disableDataForOtherSubscriptions(mSubId);
                    return;
                }
                super.performClick(view);
            } else {
                setMobileDataEnabled(true);
            }
        }
    }

    private void setMobileDataEnabled(boolean enabled) {
        if (DataUsageSummary.LOGD) Log.d(TAG, "setMobileDataEnabled(" + enabled + ","
                + mSubId + ")");
        mTelephonyManager.setDataEnabled(mSubId, enabled);
        setChecked(enabled);
    }

    private void setChecked(boolean checked) {
        if (mChecked == checked) return;
        mChecked = checked;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View switchView = holder.findViewById(android.R.id.switch_widget);
        switchView.setClickable(false);
        ((Checkable) switchView).setChecked(mChecked);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        if (mMultiSimDialog) {
            showMultiSimDialog(builder, listener);
        } else {
            showDisableDialog(builder, listener);
        }
    }

    private void showDisableDialog(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        builder.setTitle(null)
                .setMessage(R.string.data_usage_disable_mobile)
                .setPositiveButton(android.R.string.ok, listener)
                .setNegativeButton(android.R.string.cancel, null);
    }

    private void showMultiSimDialog(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        final SubscriptionInfo currentSir = mSubscriptionManager.getActiveSubscriptionInfo(mSubId);
        final SubscriptionInfo nextSir = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        final String previousName = (nextSir == null)
            ? getContext().getResources().getString(R.string.sim_selection_required_pref)
            : nextSir.getDisplayName().toString();
        builder.setTitle(R.string.sim_change_data_title);
        builder.setMessage(getContext().getString(R.string.sim_change_data_message,
                String.valueOf(currentSir != null ? currentSir.getDisplayName() : null),
                previousName));
        builder.setPositiveButton(R.string.okay, listener);
        builder.setNegativeButton(R.string.cancel, null);
    }

    //[Redmine #5407] It is not suggestable to disable data without user Intervention.
    /* private void disableDataForOtherSubscriptions(int subId) {
        List<SubscriptionInfo> subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (subInfo.getSubscriptionId() != subId) {
                    mTelephonyManager.setDataEnabled(subInfo.getSubscriptionId(), false);
                }
            }
        }
    } */

    @Override
    protected void onClick(DialogInterface dialog, int which) {
        if (which != DialogInterface.BUTTON_POSITIVE) {
            return;
        }
        if (mMultiSimDialog) {
            mSubscriptionManager.setDefaultDataSubId(mSubId);
            setMobileDataEnabled(true);
            updatePreferredNetworkMode(getContext(), mSubId);
            //disableDataForOtherSubscriptions(mSubId);
        } else {
            // TODO: extend to modify policy enabled flag.
            setMobileDataEnabled(false);
        }
    }

    //Set LTE/WCDA/GSM for data sim and change preferred n/w type of other sim to WCDMA/GSM if its previous n/w type is LTE/WCDA/GSM.
    private void updatePreferredNetworkMode(Context context, int dataSubId) {
        if (mSubscriptionManager != null) {
            List<SubscriptionInfo> sil = mSubscriptionManager.getActiveSubscriptionInfoList();
            if (sil == null && sil.size() < 1) {
                return;
            }
            for (SubscriptionInfo subInfo : sil) {
                if (subInfo != null) {
                    int subId = subInfo.getSubscriptionId();
                    if (subId == dataSubId) {
                        setPreferredNetworkMode(context, subId, Phone.NT_MODE_LTE_GSM_WCDMA);
                        continue;
                    }
                    if (getPreferredNetworkModeForSubId(context, subId) == Phone.NT_MODE_LTE_GSM_WCDMA) {
                        setPreferredNetworkMode(context, subId, Phone.NT_MODE_WCDMA_PREF);
                    }
                }
            }
        }
    }

    //Get preferred network mode based on subId
    private int getPreferredNetworkModeForSubId(Context context, int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        int phoneNwMode;
        try {
            phoneNwMode = android.telephony.TelephonyManager.getIntAtIndex(
                    context.getContentResolver(), Global.PREFERRED_NETWORK_MODE, phoneId);
        } catch (android.provider.Settings.SettingNotFoundException snfe) {
            Log.d(TAG,"getPreferredNetworkModeForSubId: Could not find PREFERRED_NETWORK_MODE");
            phoneNwMode = Phone.PREFERRED_NT_MODE;
        }
        return phoneNwMode;
    }

    // Update network mode in DB for both SubId  and phoneId
    private void setPreferredNetworkMode(Context context, int subId, int nwMode) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        Global.putInt(context.getContentResolver(),
                Global.PREFERRED_NETWORK_MODE + subId, nwMode);
        TelephonyManager.putIntAtIndex(context.getContentResolver(),
                Global.PREFERRED_NETWORK_MODE, phoneId, nwMode);

        //update in the Phone implementation too
        mTelephonyManager.setPreferredNetworkType(subId, nwMode);
        Log.d(TAG, "Update preferred network mode for phoneId : " + phoneId+", nwMode : " + nwMode);
    }

    private final DataStateListener mListener = new DataStateListener() {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (Global.getUriFor(Global.AIRPLANE_MODE_ON).equals(uri)) {
                setEnabled(!Utils.isAirplaneModeOn(getContext()));
            } else {
                updateChecked();
            }
        }
    };

    public abstract static class DataStateListener extends ContentObserver {
        public DataStateListener() {
            super(new Handler(Looper.getMainLooper()));
        }

        public void setListener(boolean listening, int subId, Context context) {
            if (listening) {
                Uri uri = Global.getUriFor(Global.MOBILE_DATA);
                Uri uriAir = Global.getUriFor(Global.AIRPLANE_MODE_ON);
                if (TelephonyManager.getDefault().getSimCount() != 1) {
                    uri = Global.getUriFor(Global.MOBILE_DATA + subId);
                }
                context.getContentResolver().registerContentObserver(uri, false, this);
                context.getContentResolver().registerContentObserver(uriAir, false, this);
            } else {
                context.getContentResolver().unregisterContentObserver(this);
            }
        }
    }

    public static class CellDataState extends BaseSavedState {
        public int mSubId;
        public boolean mChecked;
        public boolean mMultiSimDialog;

        public CellDataState(Parcelable base) {
            super(base);
        }

        public CellDataState(Parcel source) {
            super(source);
            mChecked = source.readByte() != 0;
            mMultiSimDialog = source.readByte() != 0;
            mSubId = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (mChecked ? 1 : 0));
            dest.writeByte((byte) (mMultiSimDialog ? 1 : 0));
            dest.writeInt(mSubId);
        }

        public static final Creator<CellDataState> CREATOR = new Creator<CellDataState>() {
            @Override
            public CellDataState createFromParcel(Parcel source) {
                return new CellDataState(source);
            }

            @Override
            public CellDataState[] newArray(int size) {
                return new CellDataState[size];
            }
        };
    }
}
