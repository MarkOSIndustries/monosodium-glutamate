#!/bin/bash
die() { echo "$*" 1>&2 ; exit 1; }

echo $1 | grep -E -q '^([0-9]+).([0-9]+).([0-9]+)$' || die "Version must be of the form N.N.N, $1 provided"
VERSION=$1
docker build -f docker/Dockerfile --pull \
 --build-arg MSG_VERSION="${VERSION}" \
 -t markosindustries/monosodium-glutamate:latest \
 -t markosindustries/monosodium-glutamate:$VERSION \
 .

if [[ $2 == "push" ]]; then
  docker push markosindustries/monosodium-glutamate:latest
  docker push markosindustries/monosodium-glutamate:$VERSION
fi
