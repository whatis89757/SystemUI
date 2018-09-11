package com.android.settings.notification;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by G Murali Madhav on 21/07/17.
 */

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;
    SystemRingtone mSystemRingtone;
    CustomRingtone mCustomRingtone;

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
        mSystemRingtone = new SystemRingtone();
        mCustomRingtone = new CustomRingtone();
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                if (mSystemRingtone != null) {
                    return mSystemRingtone;
                } else {
                    mSystemRingtone = new SystemRingtone();
                    return mSystemRingtone;
                }
            case 1:
                if (mCustomRingtone != null) {
                    return mCustomRingtone;
                } else {
                    mCustomRingtone = new CustomRingtone();
                    return mCustomRingtone;
                }
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
