#! /bin/bash
if [ -z "${JAVA_HOME}" ]; then
  JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 
fi
if [ -z "${OUT_ROOT}" ] || [ ! -d ${OUT_ROOT} ]; then
  OUT_ROOT="build"
fi
echo JAVA_HOME = ${JAVA_HOME}
echo OUT_ROOT = ${OUT_ROOT}

"${JAVA_HOME}/bin/java" -server -ea -classpath ${OUT_ROOT}/classes:../bin/xpp3-1.1.4c.jar -Xnoclassgc -Xms256M -Xmx256M kiev.Main -classpath ../bin/xpp3-1.1.4c.jar -d ${OUT_ROOT}/classes2 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g -target 8 $*
"${JAVA_HOME}/bin/javac" -classpath ${OUT_ROOT}/classes2:../bin/xpp3-1.1.4c.jar:../bin/swt-linux.jar:../bin/org.eclipse.draw2d.jar -d ${OUT_ROOT}/classes2 -encoding "UTF-8" -g kiev/gui/*.java kiev/gui/event/*.java kiev/gui/swing/*.java kiev/gui/swt/*.java
