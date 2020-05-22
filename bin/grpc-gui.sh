#!/bin/bash
exec $(dirname $0)/../grpc-gui/node_modules/.bin/electron %dp0/../grpc-gui/main.js
