#!/bin/bash
if [ -z "${OUT_ROOT}" ] || [ ! -d ${OUT_ROOT} ]; then
  OUT_ROOT="."
fi
echo OUT_ROOT = ${OUT_ROOT}

java -ea -Xmx256M -classpath ${OUT_ROOT}/classes4:../bin/xpp3-1.1.4c.jar -verify kiev.Main $*
