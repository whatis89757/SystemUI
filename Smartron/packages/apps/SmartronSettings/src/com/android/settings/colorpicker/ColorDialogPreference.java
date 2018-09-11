/*
 * Copyright (C) 2018 The Android Open Source Project
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

/* Created by G Murali Madhav, 08-03-2018 */

package com.android.settings.colorpicker;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.PreferenceManager;
import android.content.ContextWrapper;
import android.support.annotation.Nullable;

import android.util.AttributeSet;
import android.widget.ImageView;

import com.android.settings.colorpicker.ColorDialogFragment.ColorUtils;
import com.android.settings.R;

public class ColorDialogPreference extends Preference implements ColorDialogFragment.OnColorSelectedListener {
    private int[] colorChoices = {};
    private int mValue = 0;
    private int itemLayoutId = R.layout.color_dialog_preference_widget;
    private int numColumns = 4;
    private boolean showDialog = true;
    private boolean mValueSet;
    private int mDefaultColor;

    public ColorDialogPreference(Context context) {
        super(context);
        initAttrs(null, 0);
    }

    public ColorDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs, 0);
    }

    public ColorDialogPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(attrs, defStyle);
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

    private void initAttrs(AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs, R.styleable.ColorPreference, defStyle, defStyle);

        try {
            numColumns = a.getInteger(R.styleable.ColorPreference_numColumns, numColumns);
            showDialog = a.getBoolean(R.styleable.ColorPreference_showDialog, true);
            int choicesResId = a.getResourceId(R.styleable.ColorPreference_colorChoices,
                    R.array.default_color_choice_values);
            colorChoices = ColorUtils.extractColorArray(choicesResId, getContext());

        } finally {
            a.recycle();
        }

        setWidgetLayoutResource(itemLayoutId);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ImageView previewView = (ImageView) holder.findViewById(R.id.color_view);
        ColorUtils.setColorViewValue(previewView, mValue, false);
    }

    public void setDefaultColor(int value) {
        mDefaultColor = value;
    }

    private void notifyValueChanged(int value) {
        if (mValue != value && callChangeListener(value)) {
            mValue = value;
            persistInt(value);
            notifyChanged();
        }
    }

    @Override
    protected void onClick() {
        super.onClick();
        if (showDialog) {
            ColorDialogFragment fragment = ColorDialogFragment.newInstance(numColumns, colorChoices, mValue, mDefaultColor);
            fragment.setOnColorSelectedListener(this);
            Activity activity = resolveContext(getContext());
            activity.getFragmentManager().beginTransaction()
                    .add(fragment, getFragmentTag())
                    .commit();
        }
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        //helps during activity re-creation
        if (showDialog) {
            Activity activity = resolveContext(getContext());
            ColorDialogFragment fragment = (ColorDialogFragment) activity
                    .getFragmentManager().findFragmentByTag(getFragmentTag());
            if (fragment != null) {
                // re-bind preference to fragment
                fragment.setOnColorSelectedListener(this);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(0) : (Integer) defaultValue);
    }

    public int getValue() {
        return mValue;
    }

    public void setValue(int value) {
        mValue = value;
    }

    public String getFragmentTag() {
        return "color_" + getKey();
    }

    @Override
    public void onColorSelected(int newColor) {
        notifyValueChanged(newColor);
    }
}
