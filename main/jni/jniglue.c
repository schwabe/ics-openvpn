#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <unistd.h>


#include "jniglue.h"

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_write(ANDROID_LOG_DEBUG,"openvpn", "Loading openvpn native library $id$ compiled on "   __DATE__ " " __TIME__ );
    return JNI_VERSION_1_2;
}


void android_openvpn_log(int level,const char* prefix,const char* prefix_sep,const char* m1)
{
    __android_log_print(ANDROID_LOG_DEBUG,"openvpn","%s%s%s",prefix,prefix_sep,m1);
}

void Java_de_blinkt_openvpn_core_NativeUtils_jniclose(JNIEnv *env,jclass jo, jint fd) {
	int ret = close(fd);
}
