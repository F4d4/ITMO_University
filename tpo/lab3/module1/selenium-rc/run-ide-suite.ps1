$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$browser = if ($args.Count -gt 0) { $args[0] } else { "*firefox" }
$baseUrl = "http://stackoverflow.com"
$suite = (Resolve-Path "..\selenium-ide\legacy-rc\TestSuite.html").Path
$targetDir = "target"
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

$safeBrowserName = $browser -replace "[^\w.-]", ""
$result = Join-Path (Resolve-Path $targetDir).Path "selenium-rc-html-$safeBrowserName.html"

mvn -q -DincludeScope=test dependency:build-classpath "-Dmdep.outputFile=target\rc-classpath.txt"
$classpath = Get-Content "target\rc-classpath.txt" -Raw

java -cp $classpath org.openqa.selenium.server.SeleniumServer -trustAllSSLCertificates -htmlSuite $browser $baseUrl $suite $result
