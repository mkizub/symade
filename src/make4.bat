@if not exist classes4\stx-fmt mkdir classes4\stx-fmt
c:\java\jdk1.6.0\bin\java -server -ea -Xms256M -Xmx256M -Xnoclassgc -classpath classes3;..\bin\piccolo.jar kiev.Main -classpath classes4 -d classes4 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g -makeall %*
