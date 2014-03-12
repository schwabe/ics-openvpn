# Path of the sources
JNI_DIR := $(call my-dir)

#optional arguments
#WITH_POLAR=1
#WITH_OPENVPN3=1
# Build openvpn with polar (OpenVPN3 core is always build with polar)
#WITH_BREAKPAD=0


include lzo/Android.mk
include snappy/Android.mk

include openssl/Android.mk

ifneq ($(USE_BREAKPAD),0)
	ifneq ($(TARGET_ARCH),mips)
	WITH_BREAKPAD=1
	include google-breakpad/android/google_breakpad/Android.mk
	else
	WITH_BREAKPAD=0
	endif
else
WITH_BREAKPAD=0
endif

ifeq ($(WITH_POLAR),1)
	USE_POLAR=1
endif
ifeq ($(WITH_OPENVPN3),1)
	USE_POLAR=1
endif

ifeq ($(USE_POLAR),1)
	include polarssl/Android.mk
endif

include openvpn/Android.mk

ifeq ($(WITH_OPENVPN3),1)
	include ovpn3/Android.mk
endif

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
LOCAL_SRC_FILES:= minivpn.c dummy.cpp
LOCAL_MODULE = minivpn
include $(BUILD_EXECUTABLE)

