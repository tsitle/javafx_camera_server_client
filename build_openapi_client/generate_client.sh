#!/usr/bin/env bash

LCFG_OUTPUT_DIR="pnp_openapi_client"

if [ -d "${LCFG_OUTPUT_DIR}" ]; then
	rm -r "${LCFG_OUTPUT_DIR}" || exit 1
fi

./codegen-wrapper.sh generate -i api-camera_server.yaml -g kotlin -o "${LCFG_OUTPUT_DIR}"
