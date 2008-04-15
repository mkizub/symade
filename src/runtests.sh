#!/bin/bash
if [ -z "${OUT_ROOT}" ] || [ ! -d ${OUT_ROOT} ]; then
  OUT_ROOT="."
fi
echo OUT_ROOT = ${OUT_ROOT}

if [ -z "$*" ]; then
  java -classpath ${OUT_ROOT}/classes4 kiev.SOPTest test
else
  java -classpath ${OUT_ROOT}/classes4 kiev.SOPTest $*
fi

