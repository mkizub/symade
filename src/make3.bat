@if not exist classes3\stx-fmt mkdir classes3\stx-fmt
c:\java\jdk1.6.0\bin\java -server -ea -classpath classes2;..\bin\piccolo.jar -Xnoclassgc -Xms256M -Xmx256M kiev.Main -classpath classes3 -d classes3 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g -makeall %*
