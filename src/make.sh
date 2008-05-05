#!/bin/bash
if [ -z "${OUT_ROOT}" ] || [ ! -d ${OUT_ROOT} ]; then
  OUT_ROOT="."
fi
echo OUT_ROOT = ${OUT_ROOT}
rm -rf ${OUT_ROOT}/classes

java -ea -verify -Xms320M -Xmx320M -Xfuture -Xnoclassgc -classpath ../bin/symade-04g.jar:../bin/piccolo.jar kiev.Main -classpath ${OUT_ROOT}/classes -d ${OUT_ROOT}/classes -verify -enable vnode -enable view -p k5.prj -prop k5.props -g $* 
