#
# These flags represent the build-time configuration of OpenSSL for android
#
# The value of $(openssl_cflags) was pruned from the Makefile generated
# by running ./Configure from import_openssl.sh.
#
# This script performs minor but required patching for the Android build.
#

# Intentionally excluded http://b/7079965
ifneq (,$(filter -DZLIB, $(openssl_cflags_32) $(openssl_cflags_64) \
    $(openssl_cflags_static_32) $(openssl_cflags_static_64)))
$(error ZLIB should not be enabled in openssl configuration)
endif

LOCAL_CFLAGS_32 += $(openssl_cflags_32)
LOCAL_CFLAGS_64 += $(openssl_cflags_64)

LOCAL_CFLAGS_32 := $(filter-out -DTERMIO, $(LOCAL_CFLAGS_32))
LOCAL_CFLAGS_64 := $(filter-out -DTERMIO, $(LOCAL_CFLAGS_64))
# filter out static flags too
openssl_cflags_static_32 := $(filter-out -DTERMIO, $(openssl_cflags_static_32))
openssl_cflags_static_64 := $(filter-out -DTERMIO, $(openssl_cflags_static_64))

ifeq ($(HOST_OS),windows)
LOCAL_CFLAGS_32 := $(filter-out -DDSO_DLFCN -DHAVE_DLFCN_H,$(LOCAL_CFLAGS_32))
LOCAL_CFLAGS_64 := $(filter-out -DDSO_DLFCN -DHAVE_DLFCN_H,$(LOCAL_CFLAGS_64))
endif

# Directories
LOCAL_CFLAGS += \
  -DOPENSSLDIR="\"/system/lib/ssl\"" \
  -DENGINESDIR="\"/system/lib/ssl/engines\""

# Debug
# LOCAL_CFLAGS += -DCIPHER_DEBUG

# Add clang here when it works on host
# LOCAL_CLANG := true
