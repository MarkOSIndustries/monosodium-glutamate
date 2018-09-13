#!/bin/bash
exec node $(dirname $0)/../grpc-gui/index.js "$@"
