@if not exist classes2\stx-fmt mkdir classes2\stx-fmt
c:\java\jdk1.6.0\bin\java -server -ea -classpath classes;..\bin\piccolo.jar -Xnoclassgc -Xms256M -Xmx256M kiev.Main -classpath classes2 -d classes2 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g -makeall %*
