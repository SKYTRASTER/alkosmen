param(
    [ValidateSet("devices", "logcat", "clear-logcat", "pid", "attach", "launch", "forward-jdwp")]
    [string]$Action = "devices",
    [string]$Package = "alkosmen.android",
    [string]$Activity = ".MainActivity",
    [string]$Device = ""
)

$ErrorActionPreference = "Stop"

function Get-AdbPath {
    $local = "C:\Users\Nagibator777\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $local) { return $local }

    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    throw "adb was not found. Install Android platform-tools or add adb to PATH."
}

function Invoke-Adb {
    param([string[]]$Args)
    $adb = Get-AdbPath
    $prefix = @()
    if ($Device -and $Device.Trim().Length -gt 0) {
        $prefix = @("-s", $Device.Trim())
    }
    & $adb @prefix @Args
}

switch ($Action) {
    "devices" {
        Invoke-Adb @("devices", "-l")
        break
    }
    "clear-logcat" {
        Invoke-Adb @("logcat", "-c")
        break
    }
    "logcat" {
        Invoke-Adb @("logcat", "-v", "time", "ActivityManager:I", "AndroidRuntime:E", "$Package:D", "*:S")
        break
    }
    "pid" {
        Invoke-Adb @("shell", "pidof", $Package)
        break
    }
    "attach" {
        Invoke-Adb @("shell", "am", "set-debug-app", "-w", $Package)
        Invoke-Adb @("shell", "am", "start", "-n", "$Package/$Activity")
        break
    }
    "launch" {
        Invoke-Adb @("shell", "am", "start", "-n", "$Package/$Activity")
        break
    }
    "forward-jdwp" {
        $pid = (& (Get-AdbPath) shell pidof $Package).Trim()
        if (-not $pid) {
            throw "Could not resolve PID for package '$Package'. Start app first."
        }
        Invoke-Adb @("forward", "tcp:8700", "jdwp:$pid")
        Write-Output "JDWP forwarded: localhost:8700 -> pid $pid"
        break
    }
}
