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
fi
