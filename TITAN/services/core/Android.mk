LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := services.core

LOCAL_SRC_FILES += \
    $(call all-java-files-under,java) \
    java/com/android/server/EventLogTags.logtags \
    java/com/android/server/am/EventLogTags.logtags

# DTS2015011302467 dingchao/d00280498 20150117 start
-include $(TOP)/vendor/huawei/Emui/frameworks/hwcust/base/services/core/hwcustInterface.mk
# DTS2015011302467 dingchao/d00280498 20150117 end
# DTS2014102002683 zhudengkui / 00180854 20141106 begin
-include $(TOP)/vendor/huawei/Emui/frameworks/hwCommInterface/base/services/core/hwCommInterface.mk
# DTS2014102002683 zhudengkui / 00180854 20141106 end
# DTS2014120501765 maxiufeng/00291265 20141205 begin
# DTS2014100804143 xiuhongju/x00181840 20141008 begin

services_extra_dirs := \
    ../../../../vendor/huawei/custFwk/frameworks/base/services/java \
    ../../../../vendor/huawei/custFwk/frameworks/hwCommInterface/base/services/java
LOCAL_SRC_FILES += $(call find-other-java-files, $(services_extra_dirs))
# DTS2014100804143 xiuhongju/x00181840 20141008 end
# DTS2014120501765 maxiufeng/00291265 20141205 end

LOCAL_JAVA_LIBRARIES := android.policy telephony-common org.codeaurora.Performance

# DTS2014102105031 litao/185177 20141103 begin
LOCAL_JAVA_LIBRARIES += hwframework
# DTS2014102105031 litao/185177 20141103 end

include $(BUILD_STATIC_JAVA_LIBRARY)
