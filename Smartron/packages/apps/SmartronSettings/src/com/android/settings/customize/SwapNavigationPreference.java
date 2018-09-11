package com.android.settings.customize;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.provider.Settings;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.RadioButton;
import com.android.settings.R;

public class SwapNavigationPreference extends Preference {


    private static final int BACK_HOME_RECENT = 0;
    private static final int RECENT_HOME_BACK = 1;
    private RadioButton mBackHomeRecent;
    private RadioButton mRecentHomeBack;
    private RelativeLayout mBHRlayout;
    private RelativeLayout mRHBlayout;
    private Context mContext;


    public SwapNavigationPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SwapNavigationPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SwapNavigationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setLayoutResource(R.layout.swap_navigation);
    }

    public SwapNavigationPreference(Context context) {
        super(context);
    }

    public boolean isSelectable(){
        return false;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mBackHomeRecent = (RadioButton) view.findViewById(R.id.back_home_recents_check);
        mRecentHomeBack = (RadioButton) view.findViewById(R.id.recents_home_back_check);
        mBHRlayout = (RelativeLayout) view.findViewById (R.id.bhr_layout_preference);
        mRHBlayout = (RelativeLayout) view.findViewById (R.id.rhb_layout_preference);
        mBHRlayout.setOnClickListener (mLayoutListener);
        mRHBlayout.setOnClickListener (mLayoutListener);
        int value = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.SWAP_SOFT_NAVIGATION_KEYS_ENABLED, 0);
        updateButtonState(value);
    }

    public void updateState(int value) {
        mBHRlayout.setOnClickListener (null);
        mRHBlayout.setOnClickListener (null);
        updateButtonState(value);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.SWAP_SOFT_NAVIGATION_KEYS_ENABLED, value);
        mBHRlayout.setOnClickListener(mLayoutListener);
        mRHBlayout.setOnClickListener(mLayoutListener);
    }

    public void updateButtonState(int value){
        if (value == BACK_HOME_RECENT) {
            mBackHomeRecent.setChecked(true);
            mRecentHomeBack.setChecked(false);
        } else if (value == RECENT_HOME_BACK) {
            mBackHomeRecent.setChecked(false);
            mRecentHomeBack.setChecked(true);
        }
    }

    RelativeLayout.OnClickListener mLayoutListener = new RelativeLayout.OnClickListener (){

        @Override
        public void onClick(View view) {
            int id = view.getId ();
            if(id == R.id.bhr_layout_preference){
                updateState(BACK_HOME_RECENT);
            } else if (id == R.id.rhb_layout_preference){
                updateState(RECENT_HOME_BACK);
            }
        }
    };
}
