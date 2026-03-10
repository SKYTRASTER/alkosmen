# Android Debug Quickstart

This project currently has desktop gameplay code (Swing/AWT).
Android side is bootstrapped in module `androidapp`.

## 0) Build and install debug APK

```powershell
.\gradlew.bat :androidapp:assembleDebug
.\gradlew.bat :androidapp:installDebug
```

Main launch command:

```powershell
.\scripts\android-debug.ps1 -Action launch -Package "alkosmen.android" -Activity ".MainActivity"
```

## 1) Device preparation

1. Enable Developer options on the phone.
2. Enable USB debugging.
3. Connect by USB (or pair over Wi-Fi).
4. Verify visibility:

```powershell
.\scripts\android-debug.ps1 -Action devices
```

## 2) Logcat (first line of defense)

Clear old logs:

```powershell
.\scripts\android-debug.ps1 -Action clear-logcat
```

Start filtered logs:

```powershell
.\scripts\android-debug.ps1 -Action logcat -Package "alkosmen.android"
```

## 3) Launch and wait for debugger

If your Android app id/activity are different, replace them.

```powershell
.\scripts\android-debug.ps1 -Action attach -Package "alkosmen.android" -Activity ".MainActivity"
```

This sets `am set-debug-app -w`, so app startup pauses until debugger is attached.

## 4) Attach from Android Studio

1. Open Android Studio.
2. Run `Attach debugger to Android process`.
3. Select process by package id.
4. Continue execution after attach.

## 5) Optional JDWP forward (manual tooling)

```powershell
.\scripts\android-debug.ps1 -Action forward-jdwp -Package "alkosmen.android"
```

Then attach debugger to `localhost:8700`.

## Commands summary

```powershell
.\scripts\android-debug.ps1 -Action devices
.\scripts\android-debug.ps1 -Action clear-logcat
.\scripts\android-debug.ps1 -Action logcat -Package "alkosmen.android"
.\scripts\android-debug.ps1 -Action launch -Package "alkosmen.android" -Activity ".MainActivity"
.\scripts\android-debug.ps1 -Action attach -Package "alkosmen.android" -Activity ".MainActivity"
.\scripts\android-debug.ps1 -Action pid -Package "alkosmen.android"
.\scripts\android-debug.ps1 -Action forward-jdwp -Package "alkosmen.android"
```
