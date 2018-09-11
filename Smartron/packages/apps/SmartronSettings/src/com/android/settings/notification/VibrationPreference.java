package com.android.settings.notification;

import android.app.Activity;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.PreferenceManager;
import android.content.ContextWrapper;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import com.android.settings.R;

public class VibrationPreference extends Preference implements VibrationDialogFragment.OnPatternSelectedListener {

    private static final String VIBRATION_CALL_PATTERN = "vibration_call_pattern";
    private static final String VIBRATION_NOTIFICATION_PATTERN = "vibration_notification_pattern";
    private static final int CALL_VIBRATION_TYPE = 1;
    private static final int NOTIFICATION_VIBRATION_TYPE = 2;
    private String[] mEntries;
    private int mValue;
    private Context mContext;
    private int mType;

    public VibrationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        getValues();
    }

    public VibrationPreference(Context context) {
        this(context, null);
    }

    @Nullable
    public static Activity resolveContext(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return resolveContext(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
    }

    private void notifyValueChanged(int value) {
        if (mValue != value && callChangeListener(value)) {
            mValue = value;
            persistInt(value);
            notifyChanged();
            setSummary(mEntries[value]);
        }
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
    }

    @Override
    protected void onClick() {
        super.onClick();
        if(mEntries == null)
            return;
        VibrationDialogFragment vibrationDialogFragment = VibrationDialogFragment.newInstance(mEntries, mValue, mType);
        vibrationDialogFragment.setOnPatternSelectedListener(this);
        Activity activity = resolveContext(getContext());
        activity.getFragmentManager().beginTransaction().add(vibrationDialogFragment, getFragmentTag()).commit();

    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(0) : (Integer) defaultValue);
    }

    public String getValue() {
        return (String) mEntries[mValue];
    }

    public void setValue(int value) {
        mValue = value;
    }


    public String getFragmentTag() {
        return "vibration_" + getKey();
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        //helps during activity re-creation
        Activity activity = resolveContext(getContext());
        VibrationDialogFragment fragment = (VibrationDialogFragment) activity
                .getFragmentManager().findFragmentByTag(getFragmentTag());
        if (fragment != null) {
            // re-bind preference to fragment
            fragment.setOnPatternSelectedListener(this);
        }
    }

    @Override
    public void onPatternSelected(int newPattern) {
        notifyValueChanged(newPattern);
    }

    public void getValues() {
        if (VIBRATION_CALL_PATTERN.equals(getFragmentTag())){
            mEntries = mContext.getResources().getStringArray(R.array.call_vibration_pattern_entries);
            mType = CALL_VIBRATION_TYPE;
        }
        else if (VIBRATION_NOTIFICATION_PATTERN.equals(getFragmentTag())){
            mEntries = mContext.getResources().getStringArray(R.array.notification_vibration_pattern_entries);
            mType = NOTIFICATION_VIBRATION_TYPE;
        }
        else{
            mEntries = null;
            mType = 0;
        }
    }
}