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

package com.android.settings.gestures;

import android.content.Context;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SmartronGestureSettings extends DashboardFragment {

    private static final String TAG = "GestureSettings";

    private static final String KEY_ASSIST = "gesture_assist_input_summary";
    private static final String KEY_SWIPE_DOWN = "gesture_swipe_down_fingerprint";
    private static final String KEY_DOUBLE_TAP_POWER = "gesture_double_tap_power";
    private static final String KEY_DOUBLE_TWIST = "gesture_double_twist";
    private static final String KEY_DOUBLE_TAP_SCREEN = "gesture_double_tap_screen";
    private static final String KEY_PICK_UP = "gesture_pick_up";
    private static final String KEY_THREE_FINGER_SCREENSHOT = "gesture_three_finger_screenshot";
    private static final String KEY_FINGERPRINT_WAKE = "gesture_fingerprint_wake";
    private static final String KEY_FLIP_TO_MUTE = "gesture_flip_to_mute";
    private static final String KEY_FINGER_PRINT_LOCK = "gesture_fingerprint_lock";
    private static final String KEY_PICK_UP_CALL = "gesture_Pick_up_call";
    private static final String KEY_DEVICE_SCREEN_TO_WAKE ="double_tap_device_screen_to_wake";
    private static final String KEY_DRAW_SCREEN_OVERLAY_C = "draw_screen_overlay_c";

    private AmbientDisplayConfiguration mAmbientDisplayConfig;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_GESTURES;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.smartron_gestures;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        if (mAmbientDisplayConfig == null) {
            mAmbientDisplayConfig = new AmbientDisplayConfiguration(context);
        }

        return buildPreferenceControllers(context, getLifecycle(), mAmbientDisplayConfig);
    }

    static List<AbstractPreferenceController> buildPreferenceControllers(
            @NonNull Context context, @Nullable Lifecycle lifecycle,
            @NonNull AmbientDisplayConfiguration ambientDisplayConfiguration) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new AssistGestureSettingsPreferenceController(context, lifecycle,
                KEY_ASSIST, false /* assistOnly */));
        controllers.add(new SwipeToNotificationPreferenceController(context, lifecycle,
                KEY_SWIPE_DOWN));
        controllers.add(new DoubleTwistPreferenceController(context, lifecycle, KEY_DOUBLE_TWIST));
        controllers.add(new DoubleTapPowerPreferenceController(context, lifecycle,
                KEY_DOUBLE_TAP_POWER));
        controllers.add(new PickupGesturePreferenceController(context, lifecycle,
                ambientDisplayConfiguration, UserHandle.myUserId(), KEY_PICK_UP));
        controllers.add(new DoubleTapScreenPreferenceController(context, lifecycle,
                ambientDisplayConfiguration, UserHandle.myUserId(), KEY_DOUBLE_TAP_SCREEN));
        controllers.add(new FingerPrintLockPreferenceController(context, lifecycle,
                KEY_FINGER_PRINT_LOCK));
        controllers.add(new ThreeFingerScreenshotPreferenceController(context, lifecycle,
                KEY_THREE_FINGER_SCREENSHOT));
        controllers.add(new TapFingerprintToWakePreferenceController(context, lifecycle,
                KEY_FINGERPRINT_WAKE));
        controllers.add(new FlipToMutePreferenceController(context, lifecycle,
                KEY_FLIP_TO_MUTE));
        controllers.add(new PickupCallPreferenceController(context, lifecycle,
                KEY_PICK_UP_CALL));
        controllers.add(new DoubleTapScreenToWakePreferenceController(context, lifecycle,
                KEY_DEVICE_SCREEN_TO_WAKE));
        controllers.add(new DrawDeviceOverlayPreferenceController(context, lifecycle,
                KEY_DRAW_SCREEN_OVERLAY_C));
        return controllers;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.smartron_gestures;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null,
                            new AmbientDisplayConfiguration(context));
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    // Duplicates in summary and details pages.
                    keys.add(KEY_ASSIST);
                    keys.add(KEY_SWIPE_DOWN);
                    keys.add(KEY_DOUBLE_TWIST);
                    keys.add(KEY_DOUBLE_TAP_SCREEN);
                    keys.add(KEY_PICK_UP);
                    keys.add(KEY_THREE_FINGER_SCREENSHOT);
                    return keys;
                }
            };
}
