/*
 * Copyright (C) 2008 The Android Open Source Project
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
/* <DTS2015032600843 chenjunguo/wx273349 20140327 create */

package huawei.android.cover;

import huawei.cust.HwCustUtils;

/**
 * Cust interface for CoverManager and CovermanagerServices.
 */
public class HwCustCoverManagerUtils {

    private static HwCustCoverManagerUtils mHwCustCoverManagerUtils = null;

    public HwCustCoverManagerUtils() {}

    public static synchronized HwCustCoverManagerUtils getDefault() {
        if (mHwCustCoverManagerUtils == null) {
            mHwCustCoverManagerUtils = (HwCustCoverManagerUtils)HwCustUtils.createObj(HwCustCoverManagerUtils.class);
        }
        return mHwCustCoverManagerUtils;
    }

    /**
    * support smartcover or not 
    * @return boolean true: support; false: not support
    */
    public boolean isSupportSmartCover(){
        return false;
    }
}