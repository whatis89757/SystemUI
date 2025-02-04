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
 * limitations under the License
 */

package com.android.systemui.statusbar;

/**
 * Class to encapsulate all possible status bar states regarding Keyguard.
 * StatusBarState类用于封装在锁屏界面时Statusbar的所有可能的状态
 */
public class StatusBarState {

    /**
     * The status bar is in the "normal" shade mode.
     */
    public static final int SHADE = 0; //此模式下，已经解锁，Keyguard的StatusBar处于隐藏状态，不显示，此时的状态栏显示的正常的StatusBar

    /**
     * Status bar is currently the Keyguard.
     */
    public static final int KEYGUARD = 1;//锁屏界面的StatusBar

    /**
     * Status bar is in the special mode, where it is fully interactive but still locked. So
     * dismissing the shade will still show the bouncer.
     * This statetion is :the user dragged down on the lockscreen
     * 用户在锁屏界面下拉状态栏，此时，状态下拉
     */
    public static final int SHADE_LOCKED = 2; 

    /**
     * Status bar is locked and shows the full screen user switcher.
     * 
     */
    public static final int FULLSCREEN_USER_SWITCHER = 3;


    public static String toShortString(int x) {
        switch (x) {
            case SHADE:
                return "SHD";
            case SHADE_LOCKED:
                return "SHD_LCK";
            case KEYGUARD:
                return "KGRD";
            case FULLSCREEN_USER_SWITCHER:
                return "FS_USRSW";
            default:
                return "bad_value_" + x;
        }
    }
}
