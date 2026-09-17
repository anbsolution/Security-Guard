# SSG - Security Guard Web App

HTML + CSS + JavaScript single-page application inspired by the supplied SSG Security Guard UI.

## Included
- Login / Sign Up authentication for offline prototype
- Password strength indicator (6-10 chars)
- Dashboard
- Day/Night shift selection
- Night-only patrol
- Punch In / Punch Out and duty timer
- Monthly attendance calendar
- Manual attendance filing
- Automatic work-hour and OT calculation
- Salary slip
- Profile with monthly salary input
- Daily report + WhatsApp share
- Reports
- Settings
- JSON backup export/import
- Responsive mobile/desktop UI
- No external CDN required

## Run
Open `index.html` in a browser.

## Important authentication note
The included authentication is an **offline/local prototype**. Credentials are stored in browser localStorage and are not suitable for a production multi-device security system.

For production authentication, replace `js/auth.js` with Firebase Auth, Supabase Auth, or a custom HTTPS backend. Passwords should never be stored as plaintext in localStorage.

## Next production modules
- Real secure Auth + password reset
- Server/database sync
- Role-based access
- Google Drive backup through OAuth
- PDF salary slip generation
- PWA install/offline cache
- Hindi/Hinglish/English language switch
- Indian payroll rule configuration

## Languages
The UI now supports English, Hindi (हिन्दी), and Hinglish. The selected language is stored locally and applies across the main screens.
