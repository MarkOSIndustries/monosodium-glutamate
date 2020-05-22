#!/bin/bash
exec java -jar $(dirname $0)/../kat/build/libs/kat-1.0.0.jar "$@"
