param([string]$Server='127.0.0.1', [int]$Port=6667, [switch]$Tls)
$ErrorActionPreference='Stop'
$taskRoot=Split-Path -Parent $PSScriptRoot
$taskClasses=Join-Path $taskRoot '.tools/smoke-classes'
New-Item -ItemType Directory -Path $taskClasses -Force | Out-Null
$taskJavac=if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { 'javac' }
$taskJava=if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$taskSources=Get-ChildItem -Path "$taskRoot/app/src/main/java/vn/edu/usth/connect/model/*.java","$taskRoot/app/src/main/java/vn/edu/usth/connect/irc/*.java" | Select-Object -ExpandProperty FullName
& $taskJavac -encoding UTF-8 -d $taskClasses @taskSources (Join-Path $PSScriptRoot 'NetworkSmoke.java')
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& $taskJava -cp $taskClasses NetworkSmoke $Server $Port ([string]$Tls.IsPresent).ToLower()
if ($LASTEXITCODE -ne 0) { throw 'Network smoke test failed' }
