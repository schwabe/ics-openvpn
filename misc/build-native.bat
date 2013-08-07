@echo on
call ndk-build -j 8

cd libs
mkdir ..\assets


for /D %%f in (*) do (
	copy %%f\minivpn ..\assets\minivpn.%%f
	del %%f\libcrypto.so
	del %%f\libssl.so
)

cd ..
