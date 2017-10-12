# Path of the sources
JNI_DIR := $(call my-dir)

#optional arguments
#WITH_MBEDTLS=1
#WITH_OPENVPN3=1
# Build openvpn with mbedTLS (OpenVPN3 core is always build with mbedTLS)
#USE_BREAKPAD=0

include lzo/Android.mk
include openssl/Android.mk

ifeq ($(TARGET_ARCH),mips)
	WITH_BREAKPAD=0
else ifeq ($(TARGET_ARCH),mips64)
	WITH_BREAKPAD=0
else ifeq ($(USE_BREAKPAD),1)
	WITH_BREAKPAD=1
else
	WITH_BREAKPAD=0
endif

#ifeq ($(TARGET_ARCH),x86)
#	USE_BREAKPAD=0
#endif


ifeq ($(WITH_BREAKPAD),1)
	include breakpad/android/google_breakpad/Android.mk 
endif

ifeq ($(WITH_MBEDTLS),1)
	USE_MBEDTLS=1
endif
ifeq ($(WITH_OPENVPN3),1)
	USE_MBEDTLS=1
endif

ifeq ($(USE_MBEDTLS),1)
	include mbedtls/Android.mk
endif

include openvpn/Android.mk

ifeq ($(WITH_OPENVPN3),1)
	include ovpn3/Android.mk
endif

LOCAL_PATH := $(JNI_DIR)

# The only real JNI libraries
include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog  -lz
LOCAL_CFLAGS =  -DTARGET_ARCH_ABI=\"${TARGET_ARCH_ABI}\"
LOCAL_SRC_FILES:= jniglue.c  scan_ifs.c
LOCAL_MODULE = opvpnutil
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog  -lz
LOCAL_CFLAGS = 
LOCAL_C_INCLUDES := openssl/include openssl/crypto openssl openssl/crypto/include
LOCAL_SRC_FILES:=  jbcrypto.cpp
LOCAL_MODULE = jbcrypto
LOCAL_SHARED_LIBRARIES :=  libcrypto
include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_LDLIBS := -lz  -lc 
LOCAL_STATIC_LIBRARIES := libssl_static libcrypto_static openvpn
LOCAL_SRC_FILES:= minivpn.c dummy.cpp
LOCAL_MODULE = nopie_openvpn
include $(BUILD_EXECUTABLE)


include $(CLEAR_VARS)
LOCAL_LDLIBS := -lz  -lc 
LOCAL_CFLAGS= -fPIE -pie
LOCAL_CFLAGS = -fPIE
LOCAL_LDFLAGS = -fPIE -pie
LOCAL_STATIC_LIBRARIES := libssl_static libcrypto_static openvpn
LOCAL_SRC_FILES:= minivpn.c dummy.cpp
LOCAL_MODULE = pie_openvpn
include $(BUILD_EXECUTABLE)

