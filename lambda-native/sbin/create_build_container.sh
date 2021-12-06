#!/bin/sh

SCRIPT_DIR=$(cd "$(dirname $0)" && pwd)
DOCKER_FILE_PATH="${SCRIPT_DIR}/../buildcontainer.dockerfile"

cd "${SCRIPT_DIR}" || exit

BUILD_IMAGE_NAME="tapir-aws-lambda-build"

docker build -t ${BUILD_IMAGE_NAME} -f ${DOCKER_FILE_PATH} .