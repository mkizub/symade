java -server -ea -classpath classes2:../bin/piccolo.jar -Xnoclassgc -Xms256M -Xmx256M kiev.Main -classpath classes3 -d classes3 -verify -enable vnode -enable view -p k4x.prj -g -makeall $*
