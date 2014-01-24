#!/bin/bash

# Exit on errors
set -e

# Generate git config if the openvpn directory is checked out from git
if [ -d openvpn/.git ]; then
    GIT=git
    cd openvpn
	CONFIGURE_GIT_CHFILES=$($GIT diff-files --name-status -r --ignore-submodules --quiet -- || echo "+")
	CONFIGURE_GIT_UNCOMMITTED=$($GIT diff-index --cached  --quiet --ignore-submodules HEAD || echo "*")
	CONFIGURE_GIT_REVISION=$($GIT rev-parse --symbolic-full-name HEAD | cut -d/ -f3)-$($GIT rev-parse --short=16 HEAD)
	echo "#define CONFIGURE_GIT_REVISION \"${CONFIGURE_GIT_REVISION}\"" > config-version.h.tmp; \
	echo "#define CONFIGURE_GIT_FLAGS \"${CONFIGURE_GIT_CHFILES}${CONFIGURE_GIT_UNCOMMITTED}\"" >> config-version.h.tmp

	if ! [ -f config-version.h ] || ! cmp -s config-version.h.tmp config-version.h; then \
		echo "replacing config-version.h"
		mv config-version.h.tmp config-version.h
	else 
		rm -f config-version.h.tmp
	fi
    cd ..
fi

ndk-build APP_API=all -j 8

if [ $? = 0 ]; then
	rm -rf build/native-libs/

	cd libs
	mkdir -p ../assets
	for i in *
	do
		cp -v $i/minivpn ../assets/minivpn.$i
	done
	# Removed compiled openssl libs, will use platform so libs 
	# Reduces size of apk
	rm -v */libcrypto.so */libssl.so

  	for arch in *
  	do
  	    builddir=../build/native-libs/$arch
  	    mkdir -p $builddir
  		cp -v $arch/*.so  $builddir
  	done
else
    exit $?
fi
