#!/usr/bin/env sh

#
# Standard Gradle wrapper script (excerpt — full version ships with the
# gradle-wrapper.jar, which you'll need to drop into gradle/wrapper/).
#

DEFAULT_JVM_OPTS=""
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

# Resolve script dir
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=$(ls -ld "$PRG")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=$(dirname "$PRG")"/$link"
    fi
done
APP_HOME=$(cd "$(dirname \"$PRG\")" && pwd -P)

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

JAVACMD="${JAVA_HOME:-}/bin/java"
[ -z "$JAVA_HOME" ] && JAVACMD="java"

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
