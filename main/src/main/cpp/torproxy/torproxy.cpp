//
// Created by pinkolik on 7/27/22.
//
//https://www.dan.me.uk/torlist/
#include <sys/socket.h>
#include <android/log.h>
#include <pthread.h>
#include <unistd.h>
#include "torproxy.h"
#include <iostream>
#include <set>

using namespace std;

set<int> g_tor_nodes_hash;
int g_is_running;


int calc_hashcode_of_ip(char *ip) {
    int result = 0;
    for (int i = 0; i < 4; i++) {
        result = 337 * result + ip[i];
    }
    return result;
}

extern "C" void Java_de_blinkt_openvpn_core_TorProxy_initTorPublicNodes(JNIEnv *env, jclass clazz,
                                                                        jobjectArray nodes_addresses) {
    g_tor_nodes_hash.clear();

    jsize len = env->GetArrayLength(nodes_addresses);

    for (int i = 0; i < len; i++) {
        jbyteArray arr = (jbyteArray) env->GetObjectArrayElement(nodes_addresses, i);
        jbyte *vals = env->GetByteArrayElements(arr, nullptr);
        int hash = calc_hashcode_of_ip((char *) vals);
        g_tor_nodes_hash.insert(hash);
        env->ReleaseByteArrayElements(arr, vals, 0);
    }
}

extern "C" void
Java_de_blinkt_openvpn_core_TorProxy_processIncomingPackets(JNIEnv *env, jclass clazz, jint in_fd,
                                                            jint out_fd) {
    char buf[16384];
    size_t len = sizeof(buf);
    int max_fd = out_fd + 1;
    fd_set current_sockets, ready_sockets;
    FD_ZERO(&current_sockets);
    FD_SET(out_fd, &current_sockets);

    //1 second timeout
    struct timeval timeout = {1, 0};

    while (g_is_running != -1) {
        ready_sockets = current_sockets;

        select(max_fd, &ready_sockets, nullptr, nullptr, &timeout);
        if (FD_ISSET(out_fd, &ready_sockets)) {
            int n = read(out_fd, buf, len);
            if (n > 0) {
                int src_addr_hash = calc_hashcode_of_ip(buf + SRC_ADDR_OFFSET);
                auto it = g_tor_nodes_hash.find(src_addr_hash);
                if (it != g_tor_nodes_hash.end()) {
                    //Tor Packet
                    write(in_fd, buf, n);
                }
            }
        }
    }
    close(out_fd);
}

extern "C" void
Java_de_blinkt_openvpn_core_TorProxy_processOutgoingPackets(JNIEnv *env, jclass clazz, jint in_fd,
                                                            jint out_fd) {
    char buf[16384];
    size_t len = sizeof(buf);
    int max_fd = in_fd + 1;
    fd_set current_sockets, ready_sockets;
    FD_ZERO(&current_sockets);
    FD_SET(in_fd, &current_sockets);

    jmethodID jMethodId = env->GetStaticMethodID(clazz, "sendPacketsToTor", "([B)V");

    //1 second timeout
    struct timeval timeout = {1, 0};

    while (g_is_running != -1) {
        ready_sockets = current_sockets;

        select(max_fd, &ready_sockets, nullptr, nullptr, &timeout);
        if (FD_ISSET(in_fd, &ready_sockets)) {
            int n = read(in_fd, buf, len);
            if (n > 0) {
                int dst_addr_hash = calc_hashcode_of_ip(buf + DST_ADDR_OFFSET);
                auto it = g_tor_nodes_hash.find(dst_addr_hash);
                if (it != g_tor_nodes_hash.end()) {
                    //Tor Packet
                    write(out_fd, buf, n);
                } else if (g_is_running == 1) { //Tor Connected
                    jbyteArray jByteArray = env->NewByteArray(n);
                    env->SetByteArrayRegion(jByteArray, 0, n, (jbyte *) buf);
                    env->CallStaticVoidMethod(clazz, jMethodId, jByteArray);
                }
            }
        }
    }
    close(in_fd);
}

extern "C" void
Java_de_blinkt_openvpn_core_TorProxy_setIsRunning(JNIEnv *env, jclass clazz, jint is_running) {
    g_is_running = is_running;
}

extern "C" void
Java_de_blinkt_openvpn_core_TorProxy_writeToDevice(JNIEnv *env, jclass clazz, jbyteArray packet,
                                                   jint in_fd) {
    jsize len = env->GetArrayLength(packet);
    jbyte *vals = env->GetByteArrayElements(packet, nullptr);
    write(in_fd, vals, len);
    env->ReleaseByteArrayElements(packet, vals, 0);
}
