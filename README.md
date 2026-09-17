# SSG Security Guard v2

Offline-first Security Guard Android app.

## Stack
- HTML
- CSS
- JavaScript
- Java WebView shell
- LocalStorage for the first offline version
- GitHub Actions for APK builds
- No Flutter / Dart

## Flow
Signup -> Login -> Shift -> Dashboard -> Attendance -> Salary -> Profile

Day shift: no Patrol menu.
Night shift: Patrol menu enabled.

## GitHub
Push this project to `main`. GitHub Actions builds debug and release APKs.

## Visual design lock
The UI uses the supplied SSG references as the design baseline:
- Navy SSG header
- Green Dashboard
- Blue Attendance
- Orange Salary
- Purple Profile
- Green/Red/Yellow/Purple attendance states
- Rounded cards and strong section headers
- SSG branding image included in WebView assets

The visual system should not be replaced with a generic Android theme.
