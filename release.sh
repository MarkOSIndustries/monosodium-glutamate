#!/bin/bash
die() { echo "$*" 1>&2 ; exit 1; }

echo $1 | grep -E -q '^([0-9]+).([0-9]+).([0-9]+)$' || die "Version must be of the form N.N.N, $1 provided"
VERSION=$1

# docker buildx create --name=multi-platform-builder --driver=docker-container --use --bootstrap

BUILD_COMMAND="build"
if [[ $2 == "push" ]]; then
  BUILD_COMMAND="buildx build --platform linux/amd64,linux/arm64 --push"
fi

docker $BUILD_COMMAND \
  -f docker/Dockerfile \
  --pull \
  --build-arg MSG_VERSION="${VERSION}" \
  -t markosindustries/monosodium-glutamate:latest \
  -t markosindustries/monosodium-glutamate:$VERSION \
  .
