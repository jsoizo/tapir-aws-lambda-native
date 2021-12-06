#!/bin/sh

SCRIPT_DIR=$(cd "$(dirname $0)" && pwd)
PROJECT_DIR=$(cd "${SCRIPT_DIR}/../../" && pwd)
TARGET_DIR="${PROJECT_DIR}/lambda-native"

BUILD_IMAGE_NAME="tapir-aws-lambda-build"

MOUNT_DIR="/app"

# clean targets
rm -rf "${TARGET_DIR}/dist"
rm -rf "${TARGET_DIR}/target"

# build linux binary
SBT_OPTS="-Xmx4096m"
docker run \
  -ti \
  --rm \
  -v "${PROJECT_DIR}:${MOUNT_DIR}" \
  -v "${PROJECT_DIR}/.cache:/root/.cache" \
  -e "SBT_OPTS=${SBT_OPTS}" \
  -w "${MOUNT_DIR}" \
  "${BUILD_IMAGE_NAME}" \
  sbt lambdaNative/nativeImage