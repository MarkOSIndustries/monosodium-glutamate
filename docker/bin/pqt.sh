#!/bin/bash
exec java $MSG_JAVA_AOT_OPTS/pqt.aot $MSG_JAVA_OPTS -jar $MSG_HOME/bin/pqt.jar "$@"
