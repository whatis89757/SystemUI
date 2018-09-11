package com.android.systemui.tuner;

import android.os.Bundle;
import android.view.MenuItem;

import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.systemui.Dependency;
import com.android.systemui.R;

/**
 * Created by smartron on 26/2/18.
 */

public class StatusBarTunerActivity extends SettingsDrawerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.status_icons);
        Dependency.initDependencies(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onBackPressed() {
        if (!getFragmentManager().popBackStackImmediate()) {
            super.onBackPressed();
        }
    }
}

