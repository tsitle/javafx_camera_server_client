#!/usr/bin/env bash

#
# Build and install OpenCV with Homebrew under macoS
#
# by TS, Mar 2025
#

# shellcheck disable=SC2034
CFG_COMPILE_WITH_FFMPEG=true
CFG_COMPILE_WITH_GSTREAMER=false
CFG_COMPILE_WITH_PYTHON3=false
CFG_COMPILE_WITH_JAVA=true

# ----------------------------------------------------------------------------------------------------------------------

# use a specific JDK (>= 21) to compile OpenCV such that it will be compatible with the Kotlin JVM and JavaFX
LCFG_JAVA_MIN_LANG_LEVEL=21

LCFG_OS_TYPE=""
case "${OSTYPE}" in
	linux*)
		LCFG_OS_TYPE="linux"
		;;
	darwin*)
		LCFG_OS_TYPE="macos"
		;;
	*)
		echo "Error: Unknown OSTYPE '${OSTYPE}'" >>/dev/stderr
		exit 1
		;;
esac

if [ "${LCFG_OS_TYPE}" != "macos" ]; then
	echo "Error: this script is only for macOS" >>/dev/stderr
	exit 1
fi

export JAVA_HOME="$(/usr/libexec/java_home -v ${LCFG_JAVA_MIN_LANG_LEVEL})"

# ----------------------------------------------------------------------------------------------------------------------

TMP_JAVA_PATH=""
if [ -n "${JAVA_HOME}" ]; then
	TMP_JAVA_PATH="${JAVA_HOME}/bin/"
	if [ ! -x "${TMP_JAVA_PATH}java" ]; then
		echo "Error: Could not find Java executable in JAVA_HOME ('${JAVA_HOME}')" >>/dev/stderr
		exit 1
	fi
else
	command -v java >/dev/null 2>&1 || {
		echo "Error: Could not find Java executable" >>/dev/stderr
		exit 1
	}
fi
TMP_JAVA_VERSION="$("${TMP_JAVA_PATH}java" --version | head -n1 | cut -f2 -d\  | cut -f1 -d.)"
if [ -z "${TMP_JAVA_VERSION}" ]; then
	echo "Error: Could not check Java version" >>/dev/stderr
	exit 1
fi
# shellcheck disable=SC2086
if [ ${TMP_JAVA_VERSION} -lt ${LCFG_JAVA_MIN_LANG_LEVEL} ]; then
	{
		echo "Error: Java version needs to be >= ${LCFG_JAVA_MIN_LANG_LEVEL} (is ${TMP_JAVA_VERSION})"
		echo -e "\nUse JAVA_HOME in this script to point '\$ java' to a valid JDK/JRE"
		echo "or use something like"
		if [ "${LCFG_OS_TYPE}" = "macos" ]; then
			echo "  $ JAVA_HOME=\"\$(/usr/libexec/java_home -v ${LCFG_JAVA_MIN_LANG_LEVEL})\" ./launcher-${LCFG_OS_TYPE}.sh"
		else
			echo "  $ sudo update-alternatives --config java"
		fi
	} >>/dev/stderr
	exit 1
fi

# ----------------------------------------------------------------------------------------------------------------------

. .build-opencv-brew.sh
