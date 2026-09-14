# Security Guard v12.4 — Build Instructions

## Step 117 — Build-Ready Project

The source archive is prepared for an Android build environment.

### Recommended Termux setup
Install/use a supported JDK and Gradle/Android SDK environment, then enter the extracted project directory.

Verify:
```text
java -version
gradle --version
adb version
```

Then:
```text
gradle assembleDebug
```

Expected APK location:
```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:
```text
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release verification
Before publishing:
```text
gradle lint
gradle test
gradle assembleRelease
```

Then perform the runtime matrix in `INTEGRATION_TEST_REPORT.md`.

### Important
This workspace does not have a usable Android SDK/Gradle build toolchain, so the APK is not claimed as compiled or device-tested here.
