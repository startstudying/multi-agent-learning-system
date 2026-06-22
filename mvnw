#!/bin/sh
# Maven Wrapper for Unix/Linux/macOS

# Use as: ./mvnw <maven-args>
# First run will download Maven automatically.

if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: JAVA_HOME is not set. Please install JDK 21 and set JAVA_HOME."
    echo "  Example: export JAVA_HOME=/usr/lib/jvm/java-21-openjdk"
    exit 1
fi

MAVEN_OPTS="${MAVEN_OPTS} -Xmx1024m"
export MAVEN_OPTS

exec "$JAVA_HOME/bin/java" \
    -classpath "$(dirname "$0")/.mvn/wrapper/maven-wrapper.jar" \
    -Dmaven.multiModuleProjectDirectory="$(cd "$(dirname "$0")" && pwd)" \
    org.apache.maven.wrapper.MavenWrapperMain \
    "$@"
