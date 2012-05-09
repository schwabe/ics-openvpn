# Path of the sources
CURRENT_DIR := $(call my-dir)


include lzo/Android.mk

include openssl/Android.mk

include openvpn/Android.mk
