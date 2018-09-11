/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.systemui.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.android.internal.app.AlertActivity;
import com.android.systemui.R;

/** A dialog that provides controls for adjusting the screen brightness. */
public class BrightnessDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private static final String TAG = "BrightnessDialog";
    private BrightnessController mBrightnessController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = View.inflate(this, R.layout.quick_settings_brightness_dialog, null);

        mAlertParams.mTitle = getText(R.string.quick_settings_brightness_dialog_title);
        mAlertParams.mPositiveButtonText = getText(android.R.string.ok);
        mAlertParams.mPositiveButtonListener = this;
        mAlertParams.mNegativeButtonText = getText(android.R.string.cancel);
        mAlertParams.mNegativeButtonListener = this;
        mAlertParams.mView = view;
        setupAlert();

        final Window window = getWindow();

        window.setCloseOnTouchOutside(false);

        final ImageView icon = (ImageView) findViewById(R.id.brightness_icon);
        final ToggleSlider slider = (ToggleSlider) findViewById(R.id.brightness_slider);
        slider.setMode(ToggleSlider.MODE_TOGGLE_CHECKBOX);
        mBrightnessController = new BrightnessController(this, icon, slider);
        Log.d(TAG, "onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBrightnessController.registerCallbacks();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBrightnessController.unregisterCallbacks();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {        
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onCancel();
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        try {
            if (DialogInterface.BUTTON_NEGATIVE == which) {
                onCancel();
            }
        } catch (Exception e) {
            Log.e(TAG, "onClick", e);
        }
    }

    private void onCancel() {
        if (mBrightnessController != null) {
            mBrightnessController.onCancel();
        }
    }
}
