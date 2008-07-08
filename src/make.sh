#!/bin/bash
if [ -z "${OUT_ROOT}" ] || [ ! -d ${OUT_ROOT} ]; then
  OUT_ROOT="."
fi
echo OUT_ROOT = ${OUT_ROOT}
rm -rf ${OUT_ROOT}/classes

java -ea -verify -Xms256M -Xmx256M -Xfuture -Xnoclassgc -classpath ../bin/symade-05.jar:../bin/xpp3-1.1.4c.jar -Dsymade.dump.old=false kiev.Main -classpath ${OUT_ROOT}/classes:../bin/xpp3-1.1.4c.jar -d ${OUT_ROOT}/classes -verify -enable vnode -enable view -p k5.prj -prop k5.props -g $* 
javac -classpath ${OUT_ROOT}/classes:../bin/xpp3-1.1.4c.jar -d ${OUT_ROOT}/classes -encoding "UTF-8" -g kiev/gui/*.java kiev/gui/event/*.java kiev/gui/swing/*.java
