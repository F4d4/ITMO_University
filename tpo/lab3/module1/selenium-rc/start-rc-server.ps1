$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

mvn -q -DincludeScope=test dependency:build-classpath "-Dmdep.outputFile=target\rc-classpath.txt"
$classpath = Get-Content "target\rc-classpath.txt" -Raw

java -cp $classpath org.openqa.selenium.server.SeleniumServer -port 4444 -trustAllSSLCertificates
