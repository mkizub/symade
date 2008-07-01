#! /bin/bash
if [ -z "${OUT_ROOT}" ] || [ ! -d ${OUT_ROOT} ]; then
  OUT_ROOT="."
fi
echo OUT_ROOT = ${OUT_ROOT}

java -server -ea -classpath ${OUT_ROOT}/classes3:../bin/xpp3-1.1.4c.jar -Xnoclassgc -Xms256M -Xmx256M kiev.Main -classpath ${OUT_ROOT}/classes4:../bin/xpp3-1.1.4c.jar -d ${OUT_ROOT}/classes4 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g $*
javac -classpath ${OUT_ROOT}/classes4:../bin/xpp3-1.1.4c.jar -d ${OUT_ROOT}/classes4 -encoding "UTF-8" -g kiev/gui/*.java
