LOCAL_PATH:= $(call my-dir)/

include $(CLEAR_VARS)

LOCAL_LDLIBS := -lz 
LOCAL_C_INCLUDES := openssl/include lzo/include openssl/crypto openssl openvpn/src/compat openvpn/src/openvpn openvpn/include google-breakpad/src google-breakpad/src/common/android/include polarssl/include snappy




LOCAL_CFLAGS= -DHAVE_CONFIG_H -DTARGET_ABI=\"${TARGET_ABI}\"
LOCAL_STATIC_LIBRARIES :=  liblzo-static snappy-static

ifeq ($(WITH_POLAR),1)
LOCAL_STATIC_LIBRARIES +=  polarssl-static
LOCAL_CFLAGS += -DENABLE_CRYPTO_POLARSSL=1
else
#LOCAL_SHARED_LIBRARIES :=  libssl libcrypto 
LOCAL_STATIC_LIBRARIES +=  libssl_static libcrypto_static  
LOCAL_CFLAGS += -DENABLE_CRYPTO_OPENSSL=1
endif

ifeq ($(WITH_BREAKPAD),1)
LOCAL_STATIC_LIBRARIES += breakpad_client
LOCAL_CFLAGS += -DGOOGLE_BREAKPAD=1
endif

LOCAL_MODULE = openvpn



LOCAL_SRC_FILES:= \
	src/compat/compat-basename.c \
	src/compat/compat-daemon.c \
	src/compat/compat-dirname.c \
	src/compat/compat-gettimeofday.c \
	src/compat/compat-inet_ntop.c \
	src/compat/compat-inet_pton.c \
	src/compat/compat-lz4.c \
	src/openvpn/base64.c \
	src/openvpn/buffer.c \
	src/openvpn/clinat.c \
	src/openvpn/console.c \
	src/openvpn/crypto.c \
	src/openvpn/crypto_openssl.c \
	src/openvpn/crypto_polarssl.c \
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
	src/openvpn/ssl_polarssl.c \
	src/openvpn/ssl_verify.c \
	src/openvpn/ssl_verify_openssl.c \
	src/openvpn/ssl_verify_polarssl.c \
	src/openvpn/status.c \
	src/openvpn/tun.c \
	src/openvpn/snappy.c \
	src/openvpn/comp-lz4.c \
	src/openvpn/comp.c \
	src/openvpn/compstub.c \


ifeq ($(WITH_BREAKPAD),1)
LOCAL_SRC_FILES+=src/openvpn/breakpad.cpp
endif


include $(BUILD_SHARED_LIBRARY)
#include $(BUILD_EXECUTABLE)



