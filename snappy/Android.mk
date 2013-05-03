
LOCAL_PATH:= $(call my-dir)


common_SRC_FILES:=  \
	snappy-c.cc \
	snappy-sinksource.cc \
	snappy-stubs-internal.cc \
	snappy.cc

common_C_INCLUDES += $(LOCAL_PATH)/ $(LOCAL_PATH)/conf

# static library
# =====================================================

include $(CLEAR_VARS)
LOCAL_CPP_EXTENSION := .cc
LOCAL_SRC_FILES:= $(common_SRC_FILES)
LOCAL_C_INCLUDES:= $(common_C_INCLUDES)
LOCAL_MODULE := snappy-static
LOCAL_PRELINK_MODULE:= false
LOCAL_MODULE_TAGS := optional
include $(BUILD_STATIC_LIBRARY)

# dynamic library
# =====================================================

# include $(CLEAR_VARS)
# LOCAL_SRC_FILES:= $(common_SRC_FILES)
# LOCAL_C_INCLUDES:= $(common_C_INCLUDES)
# LOCAL_MODULE := liblzo
# LOCAL_PRELINK_MODULE:= false
# LOCAL_MODULE_TAGS := optional
# include $(BUILD_SHARED_LIBRARY)

