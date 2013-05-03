# Path of the sources
JNI_DIR := $(call my-dir)

#USE_POLAR=1

include lzo/Android.mk
include snappy/Android.mk

include openssl/Android.mk

ifneq ($(TARGET_ARCH),mips)
WITH_BREAKPAD=1
include google-breakpad/android/google_breakpad/Android.mk
else
WITH_BREAKPAD=0
endif


#include polarssl/Android.mk

include openvpn/Android.mk


LOCAL_PATH := $(JNI_DIR)

# The only real JNI library
include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog  -lz
LOCAL_C_INCLUDES := openssl/include openssl/crypto openssl 
LOCAL_SRC_FILES:= jniglue.c jbcrypto.cpp
LOCAL_MODULE = opvpnutil
LOCAL_SHARED_LIBRARIES :=  libcrypto
include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_LDLIBS := -lz  -lc 
LOCAL_SHARED_LIBRARIES := libssl libcrypto openvpn
LOCAL_SRC_FILES:= minivpn.c 
LOCAL_MODULE = minivpn
include $(BUILD_EXECUTABLE)

