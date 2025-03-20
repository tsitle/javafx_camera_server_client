#!/usr/bin/env bash

#
# Launcher for the Camera Server Client app.
#
# Requires that either env var JAVA_HOME is set or '$ command -v java' can find a Java executable (JDK or JRE).
# The env var JAVA_HOME can be set down below.
#
# Show list of available JDKs under Linux and change default if necessary:
#   $ sudo update-alternatives --config java
#
# by TS, Mar 2025
#

LCFG_JAVA_MIN_LANG_LEVEL=21

LCFG_OS_TYPE="linux"

# ----------------------------------------------------------------------------------------------------------------------

_getCpuArch() {
	case "$(uname -m)" in
		x86_64*)
				echo -n "x64"
				;;
		i686*)
				echo -n "x86"
				;;
		aarch64*)
				echo -n "aarch64"
				;;
		armv7*)
				echo -n "armhf"
				;;
		*)
				echo "Error: Unknown CPU architecture '$(uname -m)'" >>/dev/stderr
				return 1
				;;
	esac
	return 0
}

_getCpuArch >/dev/null || exit 1

LVAR_ARCH="$(_getCpuArch)"

# ----------------------------------------------------------------------------------------------------------------------

# examples for JAVA_HOME under Linux:
#export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-21.0.6.0.7-1.el9.x86_64
#export JAVA_HOME=/usr/lib/jvm/java-23-openjdk-23.0.2.0.7-1.rolling.el9.x86_64
#export JAVA_HOME=/opt/liberica-jdk-21.0.6-full-linux_x64
#export JAVA_HOME=/opt/graalvm-jdk-22.0.2-linux_x64

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

export JAVA_OPTS="--module-path lib --add-modules=javafx.controls,javafx.fxml"

./bin/javafx_camera_server_client
