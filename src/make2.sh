java -server -ea -classpath classes:../bin/piccolo.jar -Xnoclassgc -Xms256M -Xmx256M kiev.Main -classpath classes2 -d classes2 -verify -enable vnode -enable view -p k4x.prj -g -makeall $*
