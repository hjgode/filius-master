@echo off
REM create a release package with doc and JRE
REM FILES needed:
REM filius-master.jar
REM filius-start.cmd
REM beispiele/
REM info/
REM java-runtime/
REM GPLv2.txt
REM GPLv3.txt
REM Changelog.md
REM Readme.md
REM LICENSE
REM liesmich.txt

C:\tools\7-ZipPortable\App\7-Zip64\7z.exe a -tzip filius_2.6.1.zip filius-master.jar filius-start.cmd beispiele/ info/ java-runtime/ GPLv2.txt GPLv3.txt Changelog.md Readme.md LICENSE liesmich.txt
