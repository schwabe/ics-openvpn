//
//  JBCyrpto.cpp
//  xcopenvpn
//
//  Created by Arne Schwabe on 12.07.12.
//  Copyright (c) 2012 Universität Paderborn. All rights reserved.
//

#include <jni.h>


#include <internal/cryptlib.h>
#include <openssl/ssl.h>
#include <openssl/rsa.h>
#include <openssl/objects.h>
#include <openssl/md5.h>
#include <android/log.h>
#include <openssl/err.h>

#include <internal/evp_int.h>

extern "C" {
jbyteArray Java_de_blinkt_openvpn_core_NativeUtils_rsasign(JNIEnv* env, jclass, jbyteArray from, jint pkeyRef);
}

int jniThrowException(JNIEnv* env, const char* className, const char* msg) {

    jclass exceptionClass = env->FindClass(className);

    if (exceptionClass == NULL) {
        __android_log_print(ANDROID_LOG_DEBUG,"openvpn","Unable to find exception class %s", className);
        /* ClassNotFoundException now pending */
        return -1;
    }

    if (env->ThrowNew( exceptionClass, msg) != JNI_OK) {
    	__android_log_print(ANDROID_LOG_DEBUG,"openvpn","Failed throwing '%s' '%s'", className, msg);
        /* an exception, most likely OOM, will now be pending */
        return -1;
    }

    env->DeleteLocalRef(exceptionClass);
    return 0;
}

static char opensslerr[1024];
jbyteArray Java_de_blinkt_openvpn_core_NativeUtils_rsasign (JNIEnv* env, jclass, jbyteArray from, jint pkeyRef) {

	//	EVP_MD_CTX* ctx = reinterpret_cast<EVP_MD_CTX*>(ctxRef);
	EVP_PKEY* pkey = reinterpret_cast<EVP_PKEY*>(pkeyRef);


	if (pkey == NULL || from == NULL) {
		jniThrowException(env, "java/lang/NullPointerException", "EVP_KEY is null");
		return NULL;
	}

	jbyte* data =  env-> GetByteArrayElements (from, NULL);
	int  datalen = env-> GetArrayLength(from);

	if(data==NULL )
		jniThrowException(env, "java/lang/NullPointerException", "data is null");

    int siglen;
	unsigned char* sigret = (unsigned char*)malloc(RSA_size(pkey->pkey.rsa));


	//int RSA_sign(int type, const unsigned char *m, unsigned int m_len,
	//           unsigned char *sigret, unsigned int *siglen, RSA *rsa);

	// adapted from s3_clnt.c
    /*	if (RSA_sign(NID_md5_sha1, (unsigned char*) data, datalen,
        sigret, &siglen, pkey->pkey.rsa) <= 0 ) */

    siglen = RSA_private_encrypt(datalen,(unsigned char*) data,sigret,pkey->pkey.rsa,RSA_PKCS1_PADDING);

    if (siglen < 0)
	{

        ERR_error_string_n(ERR_get_error(), opensslerr ,1024);
		jniThrowException(env, "java/security/InvalidKeyException", opensslerr);

		ERR_print_errors_fp(stderr);
		return NULL;


	}


	jbyteArray jb;

	jb =env->NewByteArray(siglen);

	env->SetByteArrayRegion(jb, 0, siglen, (jbyte *) sigret);
	free(sigret);
	return jb;

}
