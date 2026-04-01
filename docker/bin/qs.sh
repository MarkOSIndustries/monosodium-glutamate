#!/bin/bash
exec java $MSG_JAVA_AOT_OPTS/qs.aot $MSG_JAVA_OPTS -jar $MSG_HOME/bin/qs.jar "$@"
