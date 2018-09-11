/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkPolicyManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.provider.Settings;
import android.util.Log;

import com.android.ims.ImsManager;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.PhoneConstants;
import com.android.settingslib.RestrictedLockUtils;
import com.android.internal.telephony.Phone;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/**
 * Confirm and execute a reset of the network settings to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL RESET EVERYTHING"
 * prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the confirmation screen.
 */
public class ResetNetworkConfirm extends OptionsMenuFragment {

    private static final String TAG = "ResetNetworkConfirm";
    private View mContentView;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private ProgressDialog mProgressDialog;
    private Toast mToast;
    private boolean isReseting = false;

    /**
     * The user has gone through the multiple confirmation, so now we go ahead
     * and reset the network settings to its factory-default state.
     */
    private Button.OnClickListener mFinalClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (Utils.isMonkeyRunning() || isReseting) {
                return;
            }
            isReseting = true;
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    // TODO maybe show a progress dialog if this ends up taking a while
                    Context context = getActivity();

                    ConnectivityManager connectivityManager = (ConnectivityManager)
                            context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (connectivityManager != null) {
                        connectivityManager.factoryReset();
                    }

                    WifiManager wifiManager = (WifiManager)
                            context.getSystemService(Context.WIFI_SERVICE);
                    if (wifiManager != null) {
                        wifiManager.factoryReset();
                    }

                    TelephonyManager telephonyManager = (TelephonyManager)
                            context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (telephonyManager != null) {
                        telephonyManager.factoryReset(mSubId);
                    }

                    NetworkPolicyManager policyManager = (NetworkPolicyManager)
                            context.getSystemService(Context.NETWORK_POLICY_SERVICE);
                    if (policyManager != null) {
                        String subscriberId = telephonyManager.getSubscriberId(mSubId);
                        policyManager.factoryReset(subscriberId);
                    }

                    BluetoothManager btManager = (BluetoothManager)
                            context.getSystemService(Context.BLUETOOTH_SERVICE);
                    if (btManager != null) {
                        BluetoothAdapter btAdapter = btManager.getAdapter();
                        if (btAdapter != null) {
                            btAdapter.factoryReset();
                            LocalBluetoothManager mLocalBtManager =
                                    LocalBluetoothManager.getInstance(context, null);
                            if (mLocalBtManager != null) {
                                CachedBluetoothDeviceManager cachedDeviceManager =
                                        mLocalBtManager.getCachedDeviceManager();
                                cachedDeviceManager.clearAllDevices();
                            }
                        }
                    }

                    ImsManager.getInstance(context,
                            SubscriptionManager.getPhoneId(mSubId)).factoryResetSlot();
                    restoreDefaultApn(context);
                    /*if(mToast == null){
                        mToast = Toast.makeText(context, R.string.reset_network_complete_toast, Toast.LENGTH_SHORT);
                    }
                    mToast.show();*/
                    setPreferredNetworkMode(context, telephonyManager, mSubId);
                    return null;
                }

                @Override
                protected void onPreExecute() {
                    showProgressDialog();
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    dismissProgressDialog();
                    isReseting = false;
                }
            }.execute();
        }
    };

    private void setPreferredNetworkMode(Context context, TelephonyManager telephonyManager, int subId) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        int phoneNwMode;
        if (subId == subscriptionManager.getDefaultDataSubscriptionId()) {
            Log.d(TAG, "NetworkSettings reset, update LTE/WCDMA/GSM to default data sub : "+subId);
            phoneNwMode = Phone.NT_MODE_LTE_GSM_WCDMA;
        } else {
            Log.d(TAG, "NetworkSettings reset, update WCDMA/GSM to non-data sub : "+subId);
            phoneNwMode = Phone.NT_MODE_WCDMA_PREF;
        }
        telephonyManager.setPreferredNetworkType(subId, phoneNwMode);
    }

    /**
     * Restore APN settings to default.
     */
    private void restoreDefaultApn(Context context) {
        Uri uri = Uri.parse(ApnSettings.RESTORE_CARRIERS_URI);

        if (SubscriptionManager.isUsableSubIdValue(mSubId)) {
            uri = Uri.withAppendedPath(uri, "subId/" + String.valueOf(mSubId));
        }

        ContentResolver resolver = context.getContentResolver();
        resolver.delete(uri, null, null);
    }

    private void showProgressDialog() {
        Context context = getActivity();
        if (context != null) {
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(context.getString(R.string.resetting_network_msg));
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    /**
     * Configure the UI for the final confirmation interaction
     */
    private void establishFinalConfirmationState() {
        mContentView.findViewById(R.id.execute_reset_network)
                .setOnClickListener(mFinalClickListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(
                getActivity(), UserManager.DISALLOW_NETWORK_RESET, UserHandle.myUserId());
        if (RestrictedLockUtils.hasBaseUserRestriction(getActivity(),
                UserManager.DISALLOW_NETWORK_RESET, UserHandle.myUserId())) {
            return inflater.inflate(R.layout.network_reset_disallowed_screen, null);
        } else if (admin != null) {
            View view = inflater.inflate(R.layout.admin_support_details_empty_view, null);
            ShowAdminSupportDetailsDialog.setAdminSupportDetails(getActivity(), view, admin, false);
            view.setVisibility(View.VISIBLE);
            return view;
        }
        mContentView = inflater.inflate(R.layout.reset_network_confirm, null);
        establishFinalConfirmationState();
        return mContentView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mSubId = args.getInt(PhoneConstants.SUBSCRIPTION_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        dismissProgressDialog();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESET_NETWORK_CONFIRM;
    }
}
