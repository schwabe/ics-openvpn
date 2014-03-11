#!/bin/bash


function dumplib()
{
	LIB=$1
	for arch in armeabi-v7a armeabi; do
		VER=$(dump_syms obj/local/$arch/$LIB |grep MODULE | cut -d " " -f 4)
		mkdir -p symbols/$LIB/$VER
		dump_syms obj/local/armeabi-v7a/$LIB > symbols/$LIB/$VER/$LIB.sym
	done
}

dumplib libopenvpn.so



