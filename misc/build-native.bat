
@echo on
call ndk-build APP_API=all -j 8

cd libs
mkdir ..\assets
mkdir ..\build\

for /D %%f in (*) do (
	copy %%f\minivpn ..\assets\minivpn.%%f
	del %%f\libcrypto.so
	del %%f\libssl.so

	mkdir ..\build\native-libs\%%f\
	copy %%f\*.so  ..\build\native-libs\%%f\
)

cd ..
