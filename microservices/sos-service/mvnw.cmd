@echo off
setlocal
set "CACHED_MAVEN=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd"
if exist "%CACHED_MAVEN%" (
    call "%CACHED_MAVEN%" %*
) else (
    call mvn %*
)
endlocal
