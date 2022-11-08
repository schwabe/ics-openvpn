//
// Created by pinkolik on 7/27/22.
//

#include <jni.h>

#ifndef ICS_OPENVPN_TORPROXY_H
#define ICS_OPENVPN_TORPROXY_H


static const int SRC_ADDR_OFFSET = 12;
static const int DST_ADDR_OFFSET = 16;

#endif //ICS_OPENVPN_TORPROXY_H


extern "C"
JNIEXPORT void JNICALL
Java_de_blinkt_openvpn_core_TorProxy_initTorPublicNodes(JNIEnv *env, jclass clazz,
                                                        jobjectArray nodes_addresses);
extern "C"
JNIEXPORT void JNICALL
Java_de_blinkt_openvpn_core_TorProxy_processIncomingPackets(JNIEnv *env, jclass clazz, jint in_fd,
                                                            jint out_fd);
extern "C"
JNIEXPORT void JNICALL
Java_de_blinkt_openvpn_core_TorProxy_processOutgoingPackets(JNIEnv *env, jclass clazz, jint in_fd,
                                                            jint out_fd);
extern "C"
JNIEXPORT void JNICALL
Java_de_blinkt_openvpn_core_TorProxy_setIsRunning(JNIEnv *env, jclass clazz, jint is_running);
extern "C"
JNIEXPORT void JNICALL
Java_de_blinkt_openvpn_core_TorProxy_writeToDevice(JNIEnv *env, jclass clazz, jbyteArray packet,
                                                   jint in_fd);