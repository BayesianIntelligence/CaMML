@echo off
REM Batch file to run CaMML GUI under Windows

REM set JAVA="c:\Program Files (x86)\Java\jre7\bin\java.exe"
set "JAVA=java"
set "BASE=%~dp0"

%JAVA% -cp "%BASE%\Camml" camml.core.Has64

if ERRORLEVEL 1 goto BITS32

echo Running 64 bit version
set "path=%path%;%BASE%\jar\NeticaJ-Win\x64"
%JAVA% -Xmx512m -Djava.library.path="%BASE%\jar\NeticaJ-Win\x64" -classpath "%BASE%\Camml;%BASE%\jar\cdms.jar;%BASE%\jar\NeticaJ-Win\x64\NeticaJ.jar;%BASE%\jar\junit.jar;%BASE%\jar\weka.jar;%BASE%\;%BASE%\jar\tetrad4.jar" camml.core.newgui.RunGUI
goto END

:BITS32

echo Running 32 bit version
set "path=%path%;%BASE%\jar\NeticaJ-Win"
%JAVA% -Xmx512m -Djava.library.path="%BASE%\jar\NeticaJ-Win" -classpath "%BASE%\Camml;%BASE%\jar\cdms.jar;%BASE%\jar\NeticaJ-Win\NeticaJ.jar;%BASE%\jar\junit.jar;%BASE%\jar\weka.jar;%BASE%\;%BASE%\jar\tetrad4.jar" camml.core.newgui.RunGUI

:END
