#!/bin/bash
die() { echo "$*" 1>&2 ; exit 1; }

VERSION=$1

# docker buildx create --name=multi-platform-builder --driver=docker-container --use --bootstrap

# Needed for jna and protobuf-java, respectively
MSG_JAVA_OPTS='--sun-misc-unsafe-memory-access=allow --enable-native-access=ALL-UNNAMED'

if [[ $@ =~ "--jfr" ]]; then
  # Turn on Java Flight Recorder - we need to capture its state in the AOT compilation files to match runtime
  MSG_JAVA_OPTS="${MSG_JAVA_OPTS} -XX:StartFlightRecording=jdk.ExecutionSample#period=1ms,dumponexit=true,filename=/tmp/recording.jfr"
fi

BUILD_COMMAND="build"
BUILD_TAGS="-t markosindustries/monosodium-glutamate:$VERSION"
if [[ $@ =~ "--push-docker" ]]; then
  echo $VERSION | grep -E -q '^([0-9]+).([0-9]+).([0-9]+)$' || die "Version must be of the form N.N.N, $VERSION provided"
  COMMIT_SHA=$(git rev-parse $VERSION) || die "You haven't set up the git release tag yet."
  [[ "${COMMIT_SHA}" == "$(git rev-parse HEAD)" ]] || die "Check out the tagged commit first."
  [[ -z $(git status --short) ]] || die "Working directory is dirty"

  BUILD_COMMAND="buildx build --platform linux/amd64,linux/arm64 --push"
  BUILD_TAGS="${BUILD_TAGS} -t markosindustries/monosodium-glutamate:latest"
fi

docker $BUILD_COMMAND \
  -f docker/Dockerfile \
  --progress plain \
  --pull \
  --build-arg MSG_VERSION="${VERSION}" \
  --build-arg MSG_JAVA_OPTS="${MSG_JAVA_OPTS}" \
  ${BUILD_TAGS} \
  .
