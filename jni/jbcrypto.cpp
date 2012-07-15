//
//  JBCyrpto.cpp
//  xcopenvpn
//
//  Created by Arne Schwabe on 12.07.12.
//  Copyright (c) 2012 Universität Paderborn. All rights reserved.
//

#include <jni.h>

#include <openssl/ssl.h>
#include <openssl/rsa.h>
#include <openssl/objects.h>
#include <openssl/md5.h>

extern "C" {
jbyteArray Java_de_blinkt_openvpn_OpenVpnManagementThread_rsasign(JNIEnv* env, jclass, jbyteArray from, jint pkeyRef);
}


jbyteArray Java_de_blinkt_openvpn_OpenVpnManagementThread_rsasign(JNIEnv* env, jclass, jbyteArray from, jint pkeyRef) {

	//	EVP_MD_CTX* ctx = reinterpret_cast<EVP_MD_CTX*>(ctxRef);
	EVP_PKEY* pkey = reinterpret_cast<EVP_PKEY*>(pkeyRef);


	if (pkey == NULL || from == NULL) {
		jniThrowException(env, "java/lang/NullPointerException", "EVP_KEY is null");
		return NULL;
	}

	jbyte* data =  env-> GetByteArrayElements (from, NULL);
	int  datalen = env-> GetArrayLength(from);

	if(data==NULL || datalen == )

		unsigned int siglen;
	unsigned char* sigret = (unsigned char*)malloc(RSA_size(pkey->pkey.rsa));


	//int RSA_sign(int type, const unsigned char *m, unsigned int m_len,
	//           unsigned char *sigret, unsigned int *siglen, RSA *rsa);

	// adapted from s3_clnt.c
	if (RSA_sign(NID_md5_sha1, (unsigned char*) data, datalen,
			sigret, &siglen, pkey->pkey.rsa) <= 0 )
	{

		ERR_print_errors(errbio);
		jniThrowException(env, "java/security/InvalidKeyException", "rsa_sign went wrong, see logcat");

		ERR_print_errors_fp(stderr);
		return NULL;


	}


	jbyteArray jb;

	jb =env->NewByteArray(siglen);

	env->SetByteArrayRegion(jb, 0, siglen, (jbyte *) sigret);
	free(sigret);
	return jb;

}
