#
# CMakeLists.txt --- a simple "CMake" file for building LZO
#
# This file is part of the LZO data compression library.
#
# Copyright (C) 1996-2015 Markus Franz Xaver Johannes Oberhumer
# All Rights Reserved.
#

#
# simple usage:
#     mkdir -p build && cd build && cmake .. && make
#
# another usage example:
#     mkdir -p build/release-i686
#     cd       build/release-i686
#     cmake ../.. -DENABLE_STATIC=0 -DENABLE_SHARED=1 \
#         -DCMAKE_C_COMPILER=gcc -DCMAKE_C_FLAGS="-m32 -march=i686" \
#         -DCMAKE_INSTALL_PREFIX=/opt/local/prefix-i686
#     make VERBOSE=1
#     make install
#
# see http://www.cmake.org/ for more info
#

#
# init
#

cmake_minimum_required(VERSION 2.6 FATAL_ERROR)

# Disallow in-source builds. Note that you will still have to manually
# clean up a few files if you accidentally try an in-source build.
set(CMAKE_DISABLE_IN_SOURCE_BUILD ON)
set(CMAKE_DISABLE_SOURCE_CHANGES  ON)
if(",${CMAKE_SOURCE_DIR}," STREQUAL ",${CMAKE_BINARY_DIR},")
    message(FATAL_ERROR "ERROR: In-source builds are not allowed.")
endif()

project(lzo C)

#
# configuration options
#

option(ENABLE_STATIC "Build static LZO library." ON)
option(ENABLE_SHARED "Build shared LZO library." OFF)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE "Release" CACHE STRING "" FORCE)
endif()
if(NOT CMAKE_INSTALL_PREFIX)
    set(CMAKE_INSTALL_PREFIX "/usr/local" CACHE PATH "" FORCE)
endif()

#
# targets
#

file(GLOB lzo_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/src/*.c")
list(SORT lzo_SOURCES)

# LZO library
if(NOT ENABLE_STATIC AND NOT ENABLE_SHARED)
    set(ENABLE_STATIC ON)
endif()
if(ENABLE_STATIC)
    add_library(lzo_static STATIC ${lzo_SOURCES})
    set_target_properties(lzo_static PROPERTIES OUTPUT_NAME lzo2)
endif()
if(ENABLE_SHARED)
    add_library(lzo_shared SHARED ${lzo_SOURCES})
    set_target_properties(lzo_shared PROPERTIES OUTPUT_NAME lzo2)
    set_target_properties(lzo_shared PROPERTIES SOVERSION 2 VERSION 2.0.0)
endif()

# tests & examples
macro(lzo_add_executable t)
    add_executable(${t} ${ARGN})
    if(ENABLE_STATIC)
        target_link_libraries(${t} lzo_static)
    else()
        target_link_libraries(${t} lzo_shared)
    endif()
endmacro()
# main test driver
lzo_add_executable(lzotest  lzotest/lzotest.c)
# examples
lzo_add_executable(dict     examples/dict.c)
lzo_add_executable(lzopack  examples/lzopack.c)
lzo_add_executable(overlap  examples/overlap.c)
lzo_add_executable(precomp  examples/precomp.c)
lzo_add_executable(precomp2 examples/precomp2.c)
lzo_add_executable(simple   examples/simple.c)
# some boring internal test programs
if(0)
    lzo_add_executable(align    tests/align.c)
    lzo_add_executable(chksum   tests/chksum.c)
    lzo_add_executable(promote  tests/promote.c)
    lzo_add_executable(sizes    tests/sizes.c)
endif()

# miniLZO
if(1)
    add_executable(testmini minilzo/testmini.c minilzo/minilzo.c)
    include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include/lzo) # needed for "lzoconf.h"
endif()

#
# compilation flags
#

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

include(CheckFunctionExists)
include(CheckIncludeFile)
include(CheckLibraryExists)
include(CheckSymbolExists)
include(CheckTypeSize)
include(TestBigEndian)

# Checks for header files
macro(mfx_check_include_file f var)
    check_include_file("${f}" "mfx_${var}")
    if(NOT ",${mfx_${var}}," STREQUAL ",,")
        add_definitions(-D${var}=1)
        set(mfx_${var} 1)
    else()
        set(mfx_${var} 0)
    endif()
endmacro()
# mfx_ACC_CHECK_HEADERS
set(l assert.h ctype.h dirent.h errno.h fcntl.h float.h limits.h malloc.h memory.h setjmp.h signal.h stdarg.h stddef.h stdint.h stdio.h stdlib.h string.h strings.h time.h unistd.h utime.h sys/mman.h sys/resource.h sys/stat.h sys/time.h sys/types.h sys/wait.h)
foreach(f ${l})
    string(TOUPPER "${f}" var)
    string(REGEX REPLACE "[^0-9A-Z_]" "_" var "${var}")
    mfx_check_include_file("${f}" "HAVE_${var}")
endforeach()

# Checks for typedefs and structures
macro(mfx_check_type_size type var)
    check_type_size("${type}" "mfx_${var}")
    if("${mfx_${var}}" MATCHES "^[1-9][0-9]*$")
        add_definitions(-D${var}=${mfx_${var}})
    else()
        set(mfx_${var} 0)
    endif()
endmacro()
# mfx_ACC_CHECK_SIZEOF + mfx_CHECK_SIZEOF
set(l short int long "long long" __int16 __int32 __int64 "void *" size_t ptrdiff_t intmax_t uintmax_t intptr_t uintptr_t float double "long double" dev_t fpos_t mode_t off_t ssize_t time_t)
foreach(f ${l})
    string(TOUPPER "${f}" var)
    string(REGEX REPLACE " \\*" "_P" var "${var}")
    string(REGEX REPLACE "[^0-9A-Z_]" "_" var "${var}")
    mfx_check_type_size("${f}" "SIZEOF_${var}")
endforeach()

# Checks for library functions
macro(mfx_check_function_exists func var)
    check_function_exists("${func}" "mfx_${var}")
    if(NOT ",${mfx_${var}}," STREQUAL ",,")
        add_definitions(-D${var}=1)
        set(mfx_${var} 1)
    else()
        set(mfx_${var} 0)
    endif()
endmacro()
# mfx_ACC_CHECK_FUNCS
set(l access alloca atexit atoi atol chmod chown clock_getcpuclockid clock_getres clock_gettime ctime difftime fstat getenv getpagesize getrusage gettimeofday gmtime isatty localtime longjmp lstat memcmp memcpy memmove memset mkdir mktime mmap mprotect munmap qsort raise rmdir setjmp signal snprintf strcasecmp strchr strdup strerror strftime stricmp strncasecmp strnicmp strrchr strstr time umask utime vsnprintf)
foreach(f ${l})
    string(TOUPPER "${f}" var)
    string(REGEX REPLACE "[^0-9A-Z_]" "_" var "${var}")
    mfx_check_function_exists("${f}" "HAVE_${var}")
endforeach()

# mfx_LZO_CHECK_ENDIAN
TEST_BIG_ENDIAN(big_endian)
if ("${big_endian}" MATCHES "^1$")
    add_definitions(-DLZO_ABI_BIG_ENDIAN=1)
elseif ("${big_endian}" MATCHES "^0$")
    add_definitions(-DLZO_ABI_LITTLE_ENDIAN=1)
else()
    message(FATAL_ERROR "ERROR: TEST_BIG_ENDIAN failed with result '${big_endian}'.")
endif()

# LZO_HAVE_CONFIG_H
add_definitions(-DLZO_CFG_NO_CONFIG_HEADER=1)

#
# "make install"
#

# these subdirs are relative to CMAKE_INSTALL_PREFIX
if(NOT DEFINED install_doc_subdir)
    set(install_doc_subdir "share/doc/lzo")
endif()
if(NOT DEFINED install_include_subdir)
    set(install_include_subdir "include/lzo")
endif()
if(NOT DEFINED install_lib_subdir)
    set(install_lib_subdir "lib")
endif()
if(NOT DEFINED install_examples_subdir)
    set(install_examples_subdir "libexec/lzo/examples")
endif()

set(doc_DATA AUTHORS COPYING NEWS THANKS doc/LZO.FAQ doc/LZO.TXT doc/LZOAPI.TXT)
set(pkginclude_HEADERS
    include/lzo/lzo1.h include/lzo/lzo1a.h include/lzo/lzo1b.h
    include/lzo/lzo1c.h include/lzo/lzo1f.h include/lzo/lzo1x.h
    include/lzo/lzo1y.h include/lzo/lzo1z.h include/lzo/lzo2a.h
    include/lzo/lzo_asm.h include/lzo/lzoconf.h include/lzo/lzodefs.h
    include/lzo/lzoutil.h
)

install(FILES ${doc_DATA} DESTINATION "${install_doc_subdir}")
install(FILES ${pkginclude_HEADERS} DESTINATION "${install_include_subdir}")
if(ENABLE_STATIC)
    install(TARGETS lzo_static DESTINATION "${install_lib_subdir}")
endif()
if(ENABLE_SHARED)
    install(TARGETS lzo_shared DESTINATION "${install_lib_subdir}")
endif()
if(0)
    set(lzo_EXAMPLES lzopack lzotest simple)
    if(NOT ENABLE_STATIC)
        set(d "${CMAKE_INSTALL_PREFIX}/${install_lib_subdir}")
        set_target_properties(${lzo_EXAMPLES} PROPERTIES INSTALL_RPATH "${d}")
    endif()
    install(TARGETS ${lzo_EXAMPLES} DESTINATION "${install_examples_subdir}")
endif()

# vim:set ft=cmake ts=4 sw=4 tw=0 et:
