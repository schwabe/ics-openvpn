# Path of the sources

JNI_DIR := $(call my-dir)

include lzo/Android.mk

include openssl/Android.mk

include openvpn/Android.mk


LOCAL_PATH := $(JNI_DIR)

# The only real JNI library
include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog 
LOCAL_C_INCLUDES := openssl/include openssl/crypto openssl 
LOCAL_SRC_FILES:= jniglue.c jbcrypto.cpp
LOCAL_MODULE = opvpnutil
LOCAL_STATIC_LIBRARIES :=  libcrypto_static
include $(BUILD_SHARED_LIBRARY)



include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog 
LOCAL_SRC_FILES:= minivpn.c 
LOCAL_MODULE = minivp
LOCAL_SHARED_LIBRARIES=openvpn
include $(BUILD_EXECUTABLE)

