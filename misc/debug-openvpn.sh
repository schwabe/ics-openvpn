#!/bin/bash
#NDK_DEBUG=1 ndk-build -j8
# Quick and dirty from ndk-gdb

. ${ANDROID_NDK_HOME}/build/tools/ndk-common.sh

ANDROID_NDK_ROOT=${ANDROID_NDK_HOME}
PROJECT=.
APP_ABI=armeabi-v7a

AWK_SCRIPTS=${ANDROID_NDK_ROOT}/build/awk
PACKAGE_NAME=de.blinkt.openvpn
DEBUG_PORT=5039
JDB_PORT=65534

ADB_CMD=adb
AWK_CMD=awk

get_build_var ()
{
    if [ -z "$GNUMAKE" ] ; then
        GNUMAKE=make
    fi
    $GNUMAKE --no-print-dir -f $ANDROID_NDK_ROOT/build/core/build-local.mk -C $PROJECT DUMP_$1 | tail -1
}

get_build_var_for_abi ()
{
    if [ -z "$GNUMAKE" ] ; then
        GNUMAKE=make
    fi
    $GNUMAKE --no-print-dir -f $ANDROID_NDK_ROOT/build/core/build-local.mk -C $PROJECT DUMP_$1 APP_ABI=$2 | tail -1
}

native_path ()
{
    echo "$1"
}

adb_cmd ()
{
    if [ "x$DEVICE_SERIAL" = "x" ]; then
        "$ADB_CMD" $ADB_FLAGS "$@"
    else
        # NOTE: We escape $ADB_CMD and $DEVICE_SERIAL in case they contains spaces.
        "$ADB_CMD" $ADB_FLAGS "$DEVICE_SERIAL" "$@"
    fi
}

get_pid_of ()
{
    adb_cmd shell ps | $AWK_CMD -f $AWK_SCRIPTS/extract-pid.awk -v PACKAGE="$1"
}


adb_cmd push libs/armeabi-v7a/gdbserver /data/local/tmp/

PID=$(get_pid_of /data/data/de.blinkt.openvpn/cache/miniopenvpn)


DEBUG_SOCKET=localhost:7788
echo " GDB RUN: adb_cmd shell run-as $PACKAGE_NAME /data/local/tmp/gdbserver +$DEBUG_SOCKET --attach $PID" 
run adb_cmd shell run-as $PACKAGE_NAME /data/local/tmp/gdbserver +$DEBUG_SOCKET --attach $PID &

if [ $? != 0 ] ; then
    echo "ERROR: Could not launch gdbserver on the device?"
    exit 1
fi
log "Launched gdbserver succesfully."

# Setup network redirection
log "Setup network redirection"
run adb_cmd forward tcp:$DEBUG_PORT localhost:7788
if [ $? != 0 ] ; then
    echo "ERROR: Could not setup network redirection to gdbserver?"
    echo "       Maybe using --port=<port> to use a different TCP port might help?"
    exit 1
fi

#APP_OUT=`get_build_var_for_abi TARGET_OUT $COMPAT_ABI`
APP_OUT='obj/local/armeabi-v7a/'
log "Using app out directory: $APP_OUT"

TOOLCHAIN_PREFIX=`get_build_var_for_abi TOOLCHAIN_PREFIX $COMPAT_ABI`
log "Using toolchain prefix: $TOOLCHAIN_PREFIX"

#run adb_cmd pull /system/bin/app_process `native_path $APP_PROCESS`
#log "Pulled app_process from device/emulator."

run adb_cmd pull /system/bin/linker `native_path $APP_OUT/linker`
log "Pulled linker from device/emulator."

run adb_cmd pull /system/lib/libc.so `native_path $APP_OUT/libc.so`
log "Pulled libc.so from device/emulator."

GDBCLIENT=${TOOLCHAIN_PREFIX}gdb
GDBSETUP=$APP_OUT/gdb.setup
#cp -f $GDBSETUP_INIT $GDBSETUP

#uncomment the following to debug the remote connection only
#echo "set debug remote 1" >> $GDBSETUP

#echo "file `native_path $APP_PROCESS`" >> $GDBSETUP
echo "file `native_path ${APP_OUT}/minivpn`" >> $GDBSETUP
echo "target remote :$DEBUG_PORT" >> $GDBSETUP
#if [ -n "$OPTION_EXEC" ] ; then
#    cat $OPTION_EXEC >> $GDBSETUP
#fi
$GDBCLIENT -x `native_path $GDBSETUP`
