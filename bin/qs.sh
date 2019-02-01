#!/bin/bash
exec java -jar $(dirname $0)/../qs/build/libs/qs-1.0.0.jar "$@"
