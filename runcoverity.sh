#!/bin/bash -xe
export PATH=$PATH:/Applications/cov-analysis-macosx-2020.09/bin

if [ -n ${COVERITY_CONNECT_HOST} ]; then
    echo  COVERITY_CONNECT_HOST not set
    exit 1
fi

./gradlew -b build.gradle.kts --no-daemon clean
cov-build --dir .coverity/idir --config .coverity/cfg.xml  ./gradlew -b build.gradle.kts --no-daemon assembleUiRelease

cov-analyze --dir .coverity/idir --all --strip-path ${PWD}/main/src/main/cpp --strip-path ${PWD}/main/src --strip-path ${PWD} --strip-path /Users/arne/oss/android-sdk-macosx/ndk/21.1.6352462/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/

cov-commit-defects --dir .coverity/idir --ssl -host ${COVERITY_CONNECT_HOST} --stream icsopenvpn-styx-master --auth-key-file ~/.coverity/auth-key.txt
