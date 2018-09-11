package com.android.settings.faceunlock;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.os.ServiceManager;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class FaceUnlockSettings extends SettingsPreferenceFragment{
	private static final String TAG = "FaceUnlockSettings";

    ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
        }
    };

    @Override
    public int getMetricsCategory() {
       return MetricsEvent.ACCESSIBILITY;
    }

    @Override
    public void onCreate(Bundle icicle) {

        super.onCreate(icicle);

        Log.e(TAG, "FaceUnlockSettings.onCreate");

        List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
        boolean findPackage = false;
        ComponentName componentName = null;
        for (int i = 0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
            if (p.packageName != null && p.packageName.startsWith("com.elephanttek.faceunlock")) {
                componentName = new ComponentName(p.packageName,"com.elephanttek.faceunlock.activity.HomeActivity");
                findPackage = true;
                break;
            }
        }
        if (findPackage) {
            Intent intent = new Intent();
            intent.setComponent(componentName);
            startActivity(intent);
        }
    }

    @Override
    public void onPause() {
        finish();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

}
