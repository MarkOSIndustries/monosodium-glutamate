#!/bin/bash
exec java $MSG_JAVA_AOT_OPTS/proto.aot $MSG_JAVA_OPTS -jar $MSG_HOME/bin/proto2.jar "$@"
