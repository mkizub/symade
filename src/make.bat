@if not exist classes\stx-fmt mkdir classes\stx-fmt
c:\java\jdk1.6.0\bin\java -ea -verify -Xms320M -Xmx320M -Xfuture -Xnoclassgc -classpath ..\bin\symade-04g.jar;..\bin\piccolo.jar kiev.Main -classpath classes -verify -enable vnode -enable view -p k5.prj -prop k5.props -g -makeall %*
