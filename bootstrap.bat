@setlocal enableextensions
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.412.8-hotspot
@echo JAVA_HOME = %JAVA_HOME%
set OUT_ROOT=buildBootstrap
@echo OUT_ROOT = %OUT_ROOT%
rmdir /s /q %OUT_ROOT%

"%JAVA_HOME%\bin\java" -ea -verify -Xfuture -classpath  bin\symade-06.jar kiev.Main -d %OUT_ROOT%\symade1 -verify -enable vnode -enable view -p k6.prj -prop k6.props -g -target 8 -no-btd
@if %errorlevel% NEQ 0 (exit /b %errorlevel%)

"%JAVA_HOME%\bin\java" -ea -verify -Xfuture -classpath %OUT_ROOT%\symade1 kiev.Main -d %OUT_ROOT%\symade2 -verify -enable vnode -enable view -p k6.prj -prop k6.props -g -target 8 -no-btd
@if %errorlevel% NEQ 0 (exit /b %errorlevel%)

"%JAVA_HOME%\bin\java" -ea -verify -Xfuture -classpath %OUT_ROOT%\symade2 kiev.Main -d %OUT_ROOT%\symade3 -verify -enable vnode -enable view -p k6.prj -prop k6.props -g -target 8 -no-btd
@if %errorlevel% NEQ 0 (exit /b %errorlevel%)

"%JAVA_HOME%\bin\jar" cf symade-core.jar -C %OUT_ROOT%\symade3 .
"%JAVA_HOME%\bin\jar" cf symade-core-sources.jar -C kiev-stdlib\src\main .
"%JAVA_HOME%\bin\jar" uf symade-core-sources.jar -C kiev-core\src\main .
"%JAVA_HOME%\bin\jar" uf symade-core-sources.jar -C kiev-compiler\src\main .
"%JAVA_HOME%\bin\jar" uf symade-core-sources.jar -C kiev-dump\src\main\java .
"%JAVA_HOME%\bin\jar" uf symade-core-sources.jar -C symade-fmt\src\main .

set OUT_ROOT=
@rem rmdir /s /q %OUT_ROOT%
@endlocal