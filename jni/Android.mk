# Path of the sources
JNI_DIR := $(call my-dir)

include lzo/Android.mk

include openssl/Android.mk

ifneq ($(TARGET_ARCH),mips)
include google-breakpad/android/google_breakpad/Android.mk
endif

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

