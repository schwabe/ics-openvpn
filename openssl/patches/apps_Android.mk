# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)

local_src_files:= \
	app_rand.c \
	apps.c \
	asn1pars.c \
	ca.c \
	ciphers.c \
	crl.c \
	crl2p7.c \
	dgst.c \
	dh.c \
	dhparam.c \
	dsa.c \
	dsaparam.c \
	ecparam.c \
	ec.c \
	enc.c \
	engine.c \
	errstr.c \
	gendh.c \
	gendsa.c \
	genpkey.c \
	genrsa.c \
	nseq.c \
	ocsp.c \
	openssl.c \
	passwd.c \
	pkcs12.c \
	pkcs7.c \
	pkcs8.c \
	pkey.c \
	pkeyparam.c \
	pkeyutl.c \
	prime.c \
	rand.c \
	req.c \
	rsa.c \
	rsautl.c \
	s_cb.c \
	s_client.c \
	s_server.c \
	s_socket.c \
	s_time.c \
	sess_id.c \
	smime.c \
	speed.c \
	spkac.c \
	verify.c \
	version.c \
	x509.c

local_shared_libraries := \
	libssl \
	libcrypto

local_c_includes := \
	external/openssl \
	external/openssl/include

local_cflags := -DMONOLITH

# These flags omit whole features from the commandline "openssl".
# However, portions of these features are actually turned on.
local_cflags += -DOPENSSL_NO_DTLS1

include $(CLEAR_VARS)
LOCAL_MODULE:= openssl
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(local_src_files)
LOCAL_SHARED_LIBRARIES := $(local_shared_libraries)
LOCAL_C_INCLUDES := $(local_c_includes)
LOCAL_CFLAGS := $(local_cflags)
include $(LOCAL_PATH)/../android-config.mk
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_MODULE:= openssl
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(local_src_files)
LOCAL_SHARED_LIBRARIES := $(local_shared_libraries)
LOCAL_C_INCLUDES := $(local_c_includes)
LOCAL_CFLAGS := $(local_cflags)
include $(LOCAL_PATH)/../android-config.mk
include $(BUILD_HOST_EXECUTABLE)
