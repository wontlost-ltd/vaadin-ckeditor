#!/usr/bin/env bash
set -euo pipefail

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
  export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

mvn clean install -Pdirectory
