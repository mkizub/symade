java -classpath "..\..\..\..\..\bin\javacc-7.0.13.jar" javacc kiev05.jj
@IF NOT ERRORLEVEL 0 exit /b %ERRORLEVEL%

@del /Q kiev050.kj
@del /Q kiev050Constants.kj
@rem del /Q kiev050TokenManager.kj
@ren kiev050.java               kiev050.kj
@ren kiev050Constants.java      kiev050Constants.kj
@rem ren kiev050TokenManager.java   kiev050TokenManager.kj
@del /Q ParseException.java
@del /Q SimpleCharStream.java
@del /Q Token.java
@del /Q TokenMgrError.java
@del /Q TokenManager.java
    