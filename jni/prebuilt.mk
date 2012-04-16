# Build curl

include $(CLEAR_VARS)
LOCAL_MODULE := curl
LOCAL_SRC_FILES := prebuilt/libcurl.a
LOCAL_PATH = $(CURRENT_DIR)

include $(PREBUILT_STATIC_LIBRARY)

# SIGC 

include $(CLEAR_VARS)
LOCAL_MODULE := sigc
LOCAL_SRC_FILES := prebuilt/libsigc.a
LOCAL_PATH = $(CURRENT_DIR)

include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
# Torrent library
LOCAL_MODULE := rtorrent
LOCAL_SRC_FILES := prebuilt/librtorrent.a
LOCAL_PATH = $(CURRENT_DIR)

LOCAL_STATIC_LIBRARIES := sigc

include $(PREBUILT_STATIC_LIBRARY)
