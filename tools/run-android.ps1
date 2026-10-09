param(
    [switch]$BuildOnly,
    [switch]$CheckOnly
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $taskRoot

# Use the JDK/SDK prepared in this workspace, without changing global Windows settings.
$taskJdk = Get-ChildItem -LiteralPath (Join-Path $taskRoot '.tools/jdk') -Directory -ErrorAction SilentlyContinue |
    Where-Object Name -Like 'jdk-17*' | Select-Object -First 1
if ($taskJdk) { $env:JAVA_HOME = $taskJdk.FullName }
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
    throw 'Can JDK 17. Dat JAVA_HOME den thu muc JDK 17 truoc khi chay.'
}
$taskSdk = Join-Path $taskRoot '.tools/android-sdk'
if (-not (Test-Path -LiteralPath $taskSdk)) { $taskSdk = $env:ANDROID_HOME }
if (-not $taskSdk -or -not (Test-Path -LiteralPath (Join-Path $taskSdk 'platform-tools/adb.exe'))) {
    throw 'Can Android SDK voi platform-tools. Dat ANDROID_HOME den thu muc SDK.'
}
$env:ANDROID_HOME = $taskSdk
$env:GRADLE_USER_HOME = Join-Path $taskRoot '.tools/gradle-home'
$taskAdb = Join-Path $taskSdk 'platform-tools/adb.exe'
$taskEmulator = Join-Path $taskSdk 'emulator/emulator.exe'

if ($BuildOnly) {
    & (Join-Path $taskRoot 'gradlew.bat') assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Build APK that bai. Xem loi Gradle phia tren.' }
    exit 0
}
if (-not (Test-Path -LiteralPath $taskEmulator)) { throw 'Chua cai Android Emulator trong SDK.' }
if (-not (Test-Path -LiteralPath (Join-Path $taskRoot '.tools/avd/usth_api35.ini'))) {
    throw 'Chua co AVD usth_api35. Xem docs/VSCODE_RUN.md de chuan bi may ao.'
}
$env:ANDROID_USER_HOME = Join-Path $taskRoot '.tools/android-user'
$env:ANDROID_AVD_HOME = Join-Path $taskRoot '.tools/avd'

$taskAcceleration = & $taskEmulator -accel-check 2>&1
$taskAccelerationExit = $LASTEXITCODE
Write-Host ($taskAcceleration -join [Environment]::NewLine)
if ($taskAccelerationExit -ne 0) {
    throw 'Hay bat Windows Hypervisor Platform trong Turn Windows features on or off, roi Restart may tinh. Xem docs/VSCODE_RUN.md.'
}
if ($CheckOnly) { Write-Host 'JDK, SDK, AVD va hypervisor da san sang.'; exit 0 }

# Start the interactive emulator only after hardware acceleration is ready.
$taskDevices = & $taskAdb devices
if (-not ($taskDevices -match '^emulator-5554\s+device\s*$')) {
    if ($taskDevices -match '^emulator-5554\s+offline\s*$') {
        throw 'Emulator-5554 dang offline. Dong cua so may ao cu, roi chay lai task.'
    }
    Start-Process -FilePath $taskEmulator -ArgumentList '-avd','usth_api35','-port','5554','-gpu','auto','-no-snapshot-load','-no-boot-anim' `
        -WindowStyle Normal -RedirectStandardOutput (Join-Path $taskRoot '.tools/vscode-emulator-stdout.log') `
        -RedirectStandardError (Join-Path $taskRoot '.tools/vscode-emulator-stderr.log') | Out-Null
}

Write-Host 'Dang doi may ao khoi dong (toi da 3 phut)...'
$taskDeadline = (Get-Date).AddMinutes(3)
$taskBooted = $false
while ((Get-Date) -lt $taskDeadline) {
    # Windows PowerShell 5.1 turns native stderr into ErrorRecord. Offline is expected during boot.
    $taskPollingPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $taskBoot = & $taskAdb -s emulator-5554 shell getprop sys.boot_completed 2>&1
        $taskBootExit = $LASTEXITCODE
    } finally { $ErrorActionPreference = $taskPollingPreference }
    if ($taskBootExit -eq 0 -and (($taskBoot -join '').Trim() -eq '1')) { $taskBooted = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $taskBooted) {
    throw 'May ao chua khoi dong xong. Xem .tools/vscode-emulator-stderr.log hoac dong emulator va chay lai task.'
}

& (Join-Path $taskRoot 'gradlew.bat') assembleDebug --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Build APK that bai. Xem loi Gradle phia tren.' }
& $taskAdb -s emulator-5554 install -r (Join-Path $taskRoot 'app/build/outputs/apk/debug/app-debug.apk')
if ($LASTEXITCODE -ne 0) { throw 'Cai APK vao may ao that bai.' }
& $taskAdb -s emulator-5554 shell am start -n 'vn.edu.usth.connect/.ui.ConnectActivity'
if ($LASTEXITCODE -ne 0) { throw 'Mo USTH Connect that bai.' }
Write-Host 'USTH Connect da mo trong cua so may ao Android.'
