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
 * limitations under the License.
 */

package com.android.settings.notification;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import com.android.settings.R;
import android.util.Log;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import com.android.settings.RingtonePreference;

/**
 * Created by G Murali Madhav on 21/07/17.
 */

public class SelectRingtoneSettings extends AppCompatActivity {

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_ringtone_settings);
        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); */
        mContext = this;

        int phoneId = getIntent().getIntExtra(RingtonePreference.PHONE_ID, 0);
        setTitle(phoneId == 0 ? R.string.first_sim_ringtone_title : R.string.second_sim_ringtone_title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
           // actionBar.setElevation(0);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.system_ringtone));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.custom_ringtone));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new PagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    public void savePreference(boolean pref) {
        SharedPreferences prefs = mContext.getSharedPreferences(RingtonePreference.RINGTONE_PREF, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("apply_both", pref).commit();
    }

    public boolean getPreference() {
        SharedPreferences prefs = mContext.getSharedPreferences(RingtonePreference.RINGTONE_PREF, Context.MODE_PRIVATE );
        return prefs.getBoolean("apply_both", false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
