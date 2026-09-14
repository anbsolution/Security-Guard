# Security Guard v12.3 — Release Candidate Hardening

## Step 116 — Release Candidate

### Identity
- Application ID: `com.securityguard.app`
- Version: `12.3.0`
- Version Code: `123000`
- App name: Security Guard

### Release gates
- [x] Launcher icon retained and manifest wiring checked
- [x] Debug/release build separation documented
- [x] R8/ProGuard release rules documented
- [x] Room destructive migration disabled
- [x] Backup/restore version gate documented
- [x] PIN plain-text storage prohibited
- [x] Critical notification path independent of app BGM/SFX mute
- [x] Animation does not control business timing
- [x] Audio/haptic does not control business timing
- [x] Historical records must not be rewritten by current settings

### Required machine-side verification
- [ ] `./gradlew assembleDebug`
- [ ] `./gradlew lint`
- [ ] `./gradlew test`
- [ ] `./gradlew connectedAndroidTest`
- [ ] Release build with signing configuration
- [ ] Install/uninstall/upgrade test
- [ ] Android 12+ notification/alarm permission behavior
- [ ] Reboot/background round-alert test
- [ ] Final APK smoke test on physical device

### Release policy
This archive is a source release candidate, not a verified APK. Do not publish until the unchecked machine-side verification gates pass.
