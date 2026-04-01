#!/bin/bash
exec java $MSG_JAVA_AOT_OPTS/kgb.aot $MSG_JAVA_OPTS -jar $MSG_HOME/bin/kgb.jar "$@"
