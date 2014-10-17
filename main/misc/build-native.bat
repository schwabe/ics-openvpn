
@echo on
echo Currently broken, feel free to fix and send me a patch, see the build-native.sh file how native libraries are build on UNIX
exit 1

call ndk-build APP_ABI=x86_64 -j 8 USE_BREAKPAD=0


cd libs
mkdir ..\ovpnlibs
mkdir ..\ovpnlibs\assets

for /D %%f in (*) do (
	copy %%f\nopievpn ..\ovpnlibs\assets\nopievpn.%%f
	copy %%f\pievpn ..\ovpnlibs\assets\pievpn.%%f

	del %%f\libcrypto.so
	del %%f\libssl.so

    mkdir ..\ovpnlibs\jniLibs
	mkdir ..\ovpnlibs\jniLibs\%%f\
	copy %%f\*.so  ..\ovpnlibs\jniLibs\%%f\
)

cd ..
