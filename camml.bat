@echo off
REM Batch file to run CaMML GUI under windows

REM set JAVA="c:\Program Files (x86)\Java\jre7\bin\java.exe"
set JAVA=java
set BASE=%~dp0

%JAVA% -cp %BASE%\Camml camml.core.Has64

if ERRORLEVEL 1 goto BITS32

REM echo Running 64 bit version
path=%path%;%BASE%\jar\NeticaJ-Win\x64
%JAVA% -Xmx512m -Djava.library.path=%BASE%\jar\NeticaJ-Win\x64 -classpath %BASE%\Camml;%BASE%\jar\cdms.jar;%BASE%\jar\NeticaJ-Win\x64\NeticaJ.jar;%BASE%\jar\junit.jar;%BASE%\jar\weka.jar;%BASE%\;%BASE%\jar\tetrad4.jar camml.core.newgui.RunCmd %*
goto END

:BITS32

REM echo Running 32 bit version
path=%path%;%BASE%\jar\NeticaJ-Win
%JAVA% -Xmx512m -Djava.library.path=%BASE%\jar\NeticaJ-Win -classpath %BASE%\Camml;%BASE%\jar\cdms.jar;%BASE%\jar\NeticaJ-Win\NeticaJ.jar;%BASE%\jar\junit.jar;%BASE%\jar\weka.jar;%BASE%\;%BASE%\jar\tetrad4.jar camml.core.newgui.RunCmd %*

:END
