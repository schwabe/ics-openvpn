//
//  jniglue.h
//  xcopenvpn
//
//  Created by Arne Schwabe on 29.03.12.
//  Copyright (c) 2012 Universität Paderborn. All rights reserved.
//

#ifndef xcopenvpn_jniglue_h
#define xcopenvpn_jniglue_h

void testmsg(char* m1, ...);
void addRouteInformation(const char* dest, const char* mask, const char* gw);
void addInterfaceInformation(int mtu,const char* ifconfig_local, const char* ifconfig_remote);
void android_openvpn_log(int level,const char* prefix,const char* prefix_sep,const char* m1);
void android_openvpn_exit(int status);
void android_set_dns(const char* dns);
#endif
