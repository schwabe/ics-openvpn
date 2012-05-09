# Android openvpn JNI 
LOCAL_PATH:= $(call my-dir)/

include $(CLEAR_VARS)

LOCAL_LDLIBS := -lz -llog 
LOCAL_C_INCLUDES := openssl/include lzo/include openssl/crypto openssl openvpn/src/compat openvpn/src/openvpn openvpn/include



#LOCAL_SHARED_LIBRARIES :=  libssl  libcrypto  liblzo
LOCAL_STATIC_LIBRARIES :=  libssl_static libcrypto_static   liblzo-static


LOCAL_CFLAGS= -DHAVE_CONFIG_H
LOCAL_MODULE = openvpn

LOCAL_SRC_FILES:= src/openvpn/jniglue.c \
	src/compat/compat-basename.c \
	src/compat/compat-daemon.c \
	src/compat/compat-dirname.c \
	src/compat/compat-gettimeofday.c \
	src/compat/compat-inet_ntop.c \
	src/compat/compat-inet_pton.c \
	src/compat/compat-rsa_generate_key.c \
	src/openvpn/base64.c \
	src/openvpn/buffer.c \
	src/openvpn/clinat.c \
	src/openvpn/console.c \
	src/openvpn/crypto.c \
	src/openvpn/crypto_openssl.c \
	src/openvpn/cryptoapi.c \
	src/openvpn/dhcp.c \
	src/openvpn/error.c \
	src/openvpn/event.c \
	src/openvpn/fdmisc.c \
	src/openvpn/forward.c \
	src/openvpn/fragment.c \
	src/openvpn/gremlin.c \
	src/openvpn/helper.c \
	src/openvpn/httpdigest.c \
	src/openvpn/init.c \
	src/openvpn/interval.c \
	src/openvpn/list.c \
	src/openvpn/lladdr.c \
	src/openvpn/lzo.c \
	src/openvpn/manage.c \
	src/openvpn/mbuf.c \
	src/openvpn/misc.c \
	src/openvpn/mroute.c \
	src/openvpn/mss.c \
	src/openvpn/mstats.c \
	src/openvpn/mtcp.c \
	src/openvpn/mtu.c \
	src/openvpn/mudp.c \
	src/openvpn/multi.c \
	src/openvpn/ntlm.c \
	src/openvpn/occ.c \
	src/openvpn/openvpn.c \
	src/openvpn/options.c \
	src/openvpn/otime.c \
	src/openvpn/packet_id.c \
	src/openvpn/perf.c \
	src/openvpn/pf.c \
	src/openvpn/ping.c \
	src/openvpn/pkcs11.c \
	src/openvpn/pkcs11_openssl.c \
	src/openvpn/platform.c \
	src/openvpn/plugin.c \
	src/openvpn/pool.c \
	src/openvpn/proto.c \
	src/openvpn/proxy.c \
	src/openvpn/ps.c \
	src/openvpn/push.c \
	src/openvpn/reliable.c \
	src/openvpn/route.c \
	src/openvpn/schedule.c \
	src/openvpn/session_id.c \
	src/openvpn/shaper.c \
	src/openvpn/sig.c \
	src/openvpn/socket.c \
	src/openvpn/socks.c \
	src/openvpn/ssl.c \
	src/openvpn/ssl_openssl.c \
	src/openvpn/ssl_verify.c \
	src/openvpn/ssl_verify_openssl.c \
	src/openvpn/ssl_verify_polarssl.c \
	src/openvpn/status.c \
	src/openvpn/tun.c 

include $(BUILD_SHARED_LIBRARY)
#include $(BUILD_EXECUTABLE)


include $(CLEAR_VARS)

LOCAL_LDLIBS := -llog 
LOCAL_SRC_FILES:= src/openvpn/jniglue.c 
LOCAL_MODULE = opvpnutil
include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)


LOCAL_LDLIBS := -llog 
LOCAL_SRC_FILES:= src/openvpn/testmain.c
LOCAL_MODULE = minivpn
LOCAL_SHARED_LIBRARIES=openvpn
include $(BUILD_EXECUTABLE)


