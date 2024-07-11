#! /bin/bash

function CheckClass {
    echo "Check class $1"
    "${JAVA_HOME}/bin/java" -cp build/classes3:../bin/xpp3-1.1.4c.jar:../bin/swt-linux.jar:../bin/asm-9.8-SNAPSHOT.jar:../bin/asm-tree-9.8-SNAPSHOT.jar:../bin/asm-analysis-9.8-SNAPSHOT.jar:../bin/asm-util-9.8-SNAPSHOT.jar org.objectweb.asm.util.CheckClassAdapter $1
}


if [ -z "${JAVA_HOME}" ]; then
  JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 
fi
if [ -z "${OUT_ROOT}" ] || [ ! -d ${OUT_ROOT} ]; then
  OUT_ROOT="build"
fi
echo JAVA_HOME = ${JAVA_HOME}
echo OUT_ROOT = ${OUT_ROOT}

find ${OUT_ROOT}/classes3 -name "*.class" -print |grep '^[^$]*\.class' | while read file; do
  CheckClass "$file"
done
