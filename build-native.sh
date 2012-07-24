ndk-build APP_API=all -j 8
if [ $? = 0 ]; then
	cd libs
	for i in *
	do
		cp -v $i/minivpn ../assets/minivpn.$i
	done
	# Removed compiled openssl libs, will use platform so libs 
	# Reduces size of apk
	rm -v */libcrypto.so */libssl.so
fi