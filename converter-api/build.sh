#!/usr/bin/env sh
set -eu

mvn -q -f desktop/pom.xml install -N -DskipTests
mvn -q -f common/TuxGuitar-lib/pom.xml install -DskipTests
mvn -q -f common/TuxGuitar-gm-utils/pom.xml install -DskipTests
mvn -q -f common/TuxGuitar-editor-utils/pom.xml install -DskipTests
mvn -q -f common/TuxGuitar-compat/pom.xml install -DskipTests
mvn -q -f common/TuxGuitar-gpx/pom.xml install -DskipTests
mvn -q -f common/TuxGuitar-gtp/pom.xml install -DskipTests
mvn -q -f common/TuxGuitar-midi/pom.xml install -DskipTests
mvn -q -f common/TuxGuitar-ptb/pom.xml install -DskipTests
mvn -q -f desktop/TuxGuitar-ui-toolkit/pom.xml install -DskipTests
mvn -q -f desktop/TuxGuitar/pom.xml install -DskipTests
mvn -q -f desktop/TuxGuitar-tef/pom.xml install -DskipTests
mvn -q -f desktop/TuxGuitar-musicxml/pom.xml install -DskipTests
mvn -q -f converter-api/pom.xml package -DskipTests