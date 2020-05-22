#!/bin/bash
exec java -jar $(dirname $0)/../kgb/build/libs/kgb-1.0.0.jar "$@"
