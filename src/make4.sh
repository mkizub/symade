java -server -ea -classpath classes3:../bin/piccolo.jar -Xnoclassgc -Xms256M -Xmx256M kiev.Main -classpath classes4 -d classes4 -verify -enable vnode -enable view -p k4x.prj -g -makeall $*
