#!/usr/bin/env bash

sudo docker run -v ${SNAP_WORKIN_DIR}:/opt/kieker kieker/kieker-build:openjdk6 /bin/bash -c "cd /opt/kieker; ./gradlew -S compileJava compileTestJava"

exit $?
