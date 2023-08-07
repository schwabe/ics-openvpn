#!/bin/bash

VERSION=r21d

if [ ! -d "/opt/ndk" ] || [ -z "$(ls -A /opt/ndk)" ];then
    mkdir -p /opt/ndk && cd /opt/ndk
    wget "https://dl.google.com/android/repository/android-ndk-$VERSION-linux-x86_64.zip"
    unzip -qq "android-ndk-$VERSION-linux-x86_64.zip"
    mv "android-ndk-$VERSION/"* .
    rm "android-ndk-$VERSION-linux-x86_64.zip"
    rm -r "android-ndk-$VERSION/"
    export ANDROID_NDK=/opt/ndk
    export ANDROID_NDK_HOME=/opt/ndk
    cd -
fi
