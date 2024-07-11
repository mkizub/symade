#!/bin/bash
if [ -z "${JAVA_HOME}" ]; then
  JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
fi
if [ -z "${OUT_ROOT}" ] || [ ! -d ${OUT_ROOT} ]; then
  OUT_ROOT="build"
fi
echo JAVA_HOME = ${JAVA_HOME}
echo OUT_ROOT = ${OUT_ROOT}
rm -rf ${OUT_ROOT}/classes

"${JAVA_HOME}/bin/java" -ea -verify -Xms256M -Xmx256M -Xfuture -Xnoclassgc -classpath ../bin/symade-06.jar:../bin/xpp3-1.1.4c.jar -Dsymade.dump.old=false kiev.Main -classpath ${OUT_ROOT}/classes:../bin/xpp3-1.1.4c.jar -d ${OUT_ROOT}/classes -verify -enable vnode -enable view -p k5.prj -prop k5.props -g -target 8 $*
#javac -classpath ${OUT_ROOT}/classes:../bin/xpp3-1.1.4c.jar:../bin/swt-linux.jar:../bin/org.eclipse.draw2d.jar -d ${OUT_ROOT}/classes -encoding "UTF-8" -g kiev/gui/*.java kiev/gui/event/*.java kiev/gui/swing/*.java kiev/gui/swt/*.java
