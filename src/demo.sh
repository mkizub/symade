#!/bin/bash
if [ -z "${OUT_ROOT}" ] || [ ! -d ${OUT_ROOT} ]; then
  OUT_ROOT="."
fi
echo OUT_ROOT = ${OUT_ROOT}
rm -rf ${OUT_ROOT}/classes

java -ea -noverify -Xms256M -Xmx256M -Xfuture -Xnoclassgc -classpath ../bin/symade-06.jar:../bin/xpp3-1.1.4c.jar kiev.Main -d tmp -verify -enable vnode -enable view -p demo.prj -prop k5.props -g -gui:swing $* 

