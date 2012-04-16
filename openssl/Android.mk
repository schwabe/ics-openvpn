LOCAL_PATH := $(call my-dir)

subdirs := $(addprefix $(LOCAL_PATH)/,$(addsuffix /Android.mk, \
		crypto \
		ssl \
	))

include $(subdirs)
