del /S /F /Q classes > nul
c:\java\jdk1.5.0\bin\java -verify -Xms32M -Xfuture -Xnoclassgc -classpath ..\..\trunk\src\classes kiev.Main -classpath classes -verify -g -p k2.prj -no-warn %1 %2 %3 %4 %5 %6 %7 %8 %9