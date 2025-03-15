#
# by TS, Mar 2025
#

VAR_MYNAME="$(basename "$0")"

#

[[ -z "${CFG_COMPILE_WITH_FFMPEG}" ]] && {
	echo "${VAR_MYNAME}: Missing CFG_COMPILE_WITH_FFMPEG. Aborting." >>/dev/stderr
	exit 1
}
[[ -z "${CFG_COMPILE_WITH_GSTREAMER}" ]] && {
	echo "${VAR_MYNAME}: Missing CFG_COMPILE_WITH_GSTREAMER. Aborting." >>/dev/stderr
	exit 1
}
[[ -z "${CFG_COMPILE_WITH_PYTHON3}" ]] && {
	echo "${VAR_MYNAME}: Missing CFG_COMPILE_WITH_PYTHON3. Aborting." >>/dev/stderr
	exit 1
}
[[ -z "${CFG_COMPILE_WITH_JAVA}" ]] && {
	echo "${VAR_MYNAME}: Missing CFG_COMPILE_WITH_JAVA. Aborting." >>/dev/stderr
	exit 1
}

LCFG_OPENCV_VERSION="4.11.0"

#

LVAR_FORM_ORG="opencv-customized-org.rb"
LVAR_FORM_ED="opencv-customized-ed.rb"
LVAR_FORM_INSTALL="opencv.rb"

if [ ! -f "${LVAR_FORM_ORG}" ]; then
	wget \
			https://raw.githubusercontent.com/Homebrew/homebrew-core/refs/heads/master/Formula/o/opencv.rb \
			-O ${LVAR_FORM_ORG} \
			|| exit 1
fi

test -f "${LVAR_FORM_ED}" && rm "${LVAR_FORM_ED}"

if ! grep -q "url \"https://github.com/opencv/opencv/archive/refs/tags/${LCFG_OPENCV_VERSION}.tar.gz\"" "${LVAR_FORM_ORG}"; then
	echo -e "\n${VAR_MYNAME}: Warning: Current Homebrew formula uses a different OpenCV version than ${LCFG_OPENCV_VERSION}\n" >>/dev/stderr
fi

#

TMP_OPT_FFMPEG="OFF"
if [ "${CFG_COMPILE_WITH_FFMPEG}" = "true" ]; then
	TMP_OPT_FFMPEG="ON"

	if ! command -v ffmpeg >/dev/null 2>&1; then
		echo "${VAR_MYNAME}: running 'brew install [...]'"
		brew install \
				ffmpeg \
				|| exit 1
	fi
fi

TMP_OPT_GSTREAMER="OFF"
if [ "${CFG_COMPILE_WITH_GSTREAMER}" = "true" ]; then
	TMP_OPT_GSTREAMER="ON"
fi

TMP_OPT_PY3="OFF"
if [ "${CFG_COMPILE_WITH_PYTHON3}" = "true" ]; then
	TMP_OPT_PY3="ON"
fi

TMP_OPT_JAVA="OFF"
if [ "${CFG_COMPILE_WITH_JAVA}" = "true" ]; then
	TMP_OPT_JAVA="ON"

	if ! command -v ant >/dev/null 2>&1; then
		echo "${VAR_MYNAME}: running 'brew install [...]'"
		brew install \
				ant \
				|| exit 1
	fi
fi

if ! command -v cmake >/dev/null 2>&1; then
	echo "${VAR_MYNAME}: running 'brew install [...]'"
	brew install \
			cmake \
			|| exit 1
fi

#

##
echo "${VAR_MYNAME}: editing formula"
sed -r \
		-e "s; depends_on \"openexr\"; ;g" \
		-e "s; depends_on \"openvino\"; ;g" \
		-e "s; depends_on \"protobuf\"; ;g" \
		-e "s; -DWITH_FFMPEG=(ON|OFF)$; -DWITH_FFMPEG=${TMP_OPT_FFMPEG};g" \
		-e "s; -DWITH_GSTREAMER=(ON|OFF)$; -DWITH_GSTREAMER=${TMP_OPT_GSTREAMER};g" \
		-e "s; -DWITH_OPENEXR=(ON|OFF)$; -DWITH_OPENEXR=OFF;g" \
		-e "s; -DWITH_OPENVINO=(ON|OFF)$; -DWITH_OPENVINO=OFF;g" \
		-e "s; -DBUILD_opencv_java=(ON|OFF)$; -DBUILD_opencv_java=${TMP_OPT_JAVA} -DOPENCV_JAVA_TARGET_VERSION=1.8;g" \
		-e "s; -DBUILD_opencv_python3=(ON|OFF)$; -DBUILD_opencv_python3=${TMP_OPT_PY3};g" \
		"${LVAR_FORM_ORG}" > "${LVAR_FORM_ED}" \
		|| exit 1
## disable Python 2 support
sed -r \
		-e "s; -DBUILD_opencv_python2=(ON|OFF)$; -DBUILD_opencv_python2=OFF;g" \
		"${LVAR_FORM_ED}" > "${LVAR_FORM_ED}-" \
		|| exit 1
mv "${LVAR_FORM_ED}-" "${LVAR_FORM_ED}" || exit 1
## disable dnn support
sed -r \
		-e "s; -DBUILD_opencv_python2=OFF$; -DBUILD_opencv_python2=OFF -DBUILD_opencv_dnn=OFF;g" \
		"${LVAR_FORM_ED}" > "${LVAR_FORM_ED}-" \
		|| exit 1
mv "${LVAR_FORM_ED}-" "${LVAR_FORM_ED}" || exit 1
## remove all Python related build args if necessary
if [ "${TMP_OPT_PY3}" != "true" ]; then
	sed -r \
			-e "s; depends_on \"python-setuptools\" => :build; ;g" \
			-e "s; depends_on \"numpy\"; ;g" \
			-e "s; depends_on \"python@.*\"; ;g" \
			-e "s; -DPYTHON3_EXECUTABLE=.*; ;g" \
			"${LVAR_FORM_ED}" > "${LVAR_FORM_ED}-" \
			|| exit 1
	mv "${LVAR_FORM_ED}-" "${LVAR_FORM_ED}" || exit 1
fi

#

echo "${VAR_MYNAME}: compiling and installing OpenCV"
mv "${LVAR_FORM_ED}" "${LVAR_FORM_INSTALL}" || exit 1
brew install --build-from-source "./${LVAR_FORM_INSTALL}" || exit 1

rm "${LVAR_FORM_ORG}" "${LVAR_FORM_INSTALL}"

#

echo -e "\n${VAR_MYNAME}: (run '\$ brew uninstall opencv' to uninstall it again)"
echo -e "\n${VAR_MYNAME}: Note: Homebrew cannot update the package 'opencv' on its own."
echo "${VAR_MYNAME}:       In order to update the package it needs to be uninstalled first"
echo "${VAR_MYNAME}:       and then installed again using this script."
