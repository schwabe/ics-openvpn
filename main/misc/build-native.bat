@echo off

setlocal EnableDelayedExpansion

if not exist openvpn\.git (
    set lasterror=Cannot find .git directory in openvpn, aborting
    goto :error
) else (
    cd openvpn

    set head=HEAD
    for /F "tokens=3 delims=/" %%F in ('git rev-parse --symbolic-full-name HEAD') do (
        set head=%%F
    )

    git rev-parse --short=16 HEAD > commit.tmp
    set /p commit=<commit.tmp
    del commit.tmp

    set revision=%head%-%commit%

    echo #define CONFIGURE_GIT_REVISION "%revision%"> config-version.h.tmp

    git diff-files --name-status -r --ignore-submodules --quiet -- > flags.tmp || echo +> flags.tmp
    git diff-index --cached  --quiet --ignore-submodules HEAD > flags.tmp || echo *>> flags.tmp
    set /p flags=<flags.tmp
    del flags.tmp

    echo #define CONFIGURE_GIT_FLAGS "%flags%">> config-version.h.tmp

    fc /b config-version.h.tmp config-version.h 1>nul 2>nul
    if not errorlevel 1 goto keep

    echo replacing config-version.h
    copy config-version.h.tmp config-version.h

:keep
    del config-version.h.tmp

    cd ..
)

if [%1] == [] (
    call ndk-build USE_SHORT_COMMANDS=1 -j 8 USE_BREAKPAD=0
) else (
    call ndk-build USE_SHORT_COMMANDS=1 %*
)

if not errorlevel 0 goto error

if exist ovpnlibs rmdir /Q /S ovpnlibs

cd libs
mkdir ..\ovpnlibs
mkdir ..\ovpnlibs\assets
mkdir ..\ovpnlibs\jniLibs

for /D %%f in (*) do (
    copy %%f\nopie_openvpn ..\ovpnlibs\assets\nopie_openvpn.%%f
    copy %%f\pie_openvpn ..\ovpnlibs\assets\pie_openvpn.%%f

    rem Remove compiled openssl libs, will use platform .so libs
    rem Reduces size of apk
    del /Q %%f\libcrypto.so
    del /Q %%f\libssl.so

    mkdir ..\ovpnlibs\jniLibs\%%f\
    copy %%f\*.so  ..\ovpnlibs\jniLibs\%%f\
)

goto :exit

:error
echo(%lasterror%
exit /b %errorlevel%

:exit