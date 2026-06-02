 @echo off
setlocal

set "REPO=%USERPROFILE%\.m2\repository\org\openjfx"
set "VER=25.0.1"

set "MODPATH=%REPO%\javafx-base\%VER%\javafx-base-%VER%-win.jar"
set "MODPATH=%MODPATH%;%REPO%\javafx-graphics\%VER%\javafx-graphics-%VER%-win.jar"
set "MODPATH=%MODPATH%;%REPO%\javafx-controls\%VER%\javafx-controls-%VER%-win.jar"
set "MODPATH=%MODPATH%;%REPO%\javafx-fxml\%VER%\javafx-fxml-%VER%-win.jar"
set "MODPATH=%MODPATH%;%REPO%\javafx-web\%VER%\javafx-web-%VER%-win.jar"
set "MODPATH=%MODPATH%;%REPO%\javafx-media\%VER%\javafx-media-%VER%-win.jar"
set "MODPATH=%MODPATH%;%REPO%\jdk-jsobject\%VER%\jdk-jsobject-%VER%-win.jar"

set "MODS=javafx.controls,javafx.fxml,javafx.web,jdk.jsobject"

if not exist "target\classes\com\routeflow\Main.class" (
    echo [INFO] Compilando el proyecto...
    call mvnw.cmd compile -q
    if errorlevel 1 (
        echo [ERROR] Fallo la compilacion.
        pause
        exit /b 1
    )
)

java --module-path "%MODPATH%" --add-modules %MODS% -cp "target\classes;target\routeflow-gps-1.0-SNAPSHOT.jar" com.routeflow.Main
if errorlevel 1 pause

endlocal
