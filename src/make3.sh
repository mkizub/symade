#! /bin/bash
if [ -z "${OUT_ROOT}" ] || [ ! -d ${OUT_ROOT} ]; then
  OUT_ROOT="."
fi
echo OUT_ROOT = ${OUT_ROOT}

java -server -ea -classpath ${OUT_ROOT}/classes2:../bin/piccolo.jar -Xnoclassgc -Xms256M -Xmx256M kiev.Main -classpath ${OUT_ROOT}/classes3 -d ${OUT_ROOT}/classes3 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g -makeall $*
