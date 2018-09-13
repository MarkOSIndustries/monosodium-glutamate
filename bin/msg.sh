#!/bin/bash
exec node $(dirname $0)/../msg/index.js "$@"
