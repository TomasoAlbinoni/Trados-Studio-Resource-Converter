@ECHO OFF
mkdir temp
7z x "%~1" -o.\temp
copy temp\Tm\nl-NL\*.*
rmdir temp /s /q