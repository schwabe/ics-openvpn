APP_ABI := arm64-v8a armeabi armeabi-v7a mips  x86 x86_64
APP_PLATFORM := android-14

APP_STL:=stlport_static
#APP_STL:=gnustl_shared


#APP_OPTIM := release

#NDK_TOOLCHAIN_VERSION=4.9
APP_CPPFLAGS += -std=c++1y
APP_CFLAGS += -funwind-tables


