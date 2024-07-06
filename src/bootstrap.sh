#!/bin/bash
if [ -z "${OUT_ROOT}" ] || [ ! -d ${OUT_ROOT} ]; then
  OUT_ROOT="."
fi
echo OUT_ROOT = ${OUT_ROOT}
rm -rf ${OUT_ROOT}/symade?

java -ea -verify -Xms256M -Xmx256M -Xfuture -Xnoclassgc -classpath ../bin/symade-06.jar:../bin/xpp3-1.1.4c.jar kiev.Main -classpath ../bin/xpp3-1.1.4c.jar -d ${OUT_ROOT}/symade1 -verify -enable vnode -enable view -p k5.prj -prop k5.props -g 

java -server -ea -classpath ${OUT_ROOT}/symade1:../bin/xpp3-1.1.4c.jar -Xnoclassgc -Xms256M -Xmx256M kiev.Main -classpath ../bin/xpp3-1.1.4c.jar -d ${OUT_ROOT}/symade2 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g

java -server -ea -classpath ${OUT_ROOT}/symade2:../bin/xpp3-1.1.4c.jar -Xnoclassgc -Xms256M -Xmx256M kiev.Main -classpath ../bin/xpp3-1.1.4c.jar -d ${OUT_ROOT}/symade3 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g

jar cf symade-core.jar -C ${OUT_ROOT}/symade3 .
rm -rf ${OUT_ROOT}/symade?
