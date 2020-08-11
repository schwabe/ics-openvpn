#!/bin/bash

if [ ! -d "/opt/sdk" ] || [ -z "$(ls -A /opt/sdk)" ];then
    mkdir -p /opt/sdk && cd /opt/sdk
    wget https://dl.google.com/android/repository/commandlinetools-linux-6609375_latest.zip
    unzip -qq commandlinetools-linux-6609375_latest.zip
    rm commandlinetools-linux-6609375_latest.zip
    sync
    mkdir licenses
    echo "8933bad161af4178b1185d1a37fbf41ea5269c55" > licenses/android-sdk-license
    echo "d56f5187479451eabf01fb78af6dfcb131a6481e" >> licenses/android-sdk-license
    echo "24333f8a63b6825ea9c5514f83c2829b004d1fee" >> licenses/android-sdk-license
    echo "84831b9409646a918e30573bab4c9c91346d8abd" > licenses/android-sdk-preview-license

    cd -

    export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/sdk/tools:/opt/ndk
    export ANDROID_HOME=/opt/sdk
    export ANDROID_SDK=/opt/sdk
fi
