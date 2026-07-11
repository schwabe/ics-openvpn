/* Adapted from OpenSSL's rsa_pss.c from OpenSSL 3.0.1 */

/*
 * Copyright 2005-2021 The OpenSSL Project Authors. All Rights Reserved.
 *
 * Licensed under the Apache License 2.0 (the "License").  You may not use
 * this file except in compliance with the License.  You can obtain a copy
 * in the file LICENSE in the source distribution or at
 * https://www.openssl.org/source/license.html
 */
#include "jni.h"

#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/rsa.h>
#include <openssl/opensslv.h>

#include <array>


extern "C" jstring Java_de_blinkt_openvpn_core_NativeUtils_getOpenSSLVersionString(JNIEnv *env, jclass jo)
{
  return env->NewStringUTF(OPENSSL_VERSION_TEXT);
}