#!/bin/bash
exec java $MSG_JAVA_AOT_OPTS/kat.aot $MSG_JAVA_OPTS -jar $MSG_HOME/bin/kat.jar "$@"
