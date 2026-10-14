# KharchaBook — Native Android starter

This is a native Android Studio project (Kotlin + Jetpack Compose). It is not an HTML page and does not need a browser to run after it is built and installed.

**App name:** KharchaBook  
**Developer:** Safdar Masu

## Included
- Native Android interface with bottom navigation
- Income and expense entries
- Bank/cash/wallet accounts
- Monthly category budgets
- Local app storage
- Manual JSON backup export and restore
- App info showing the app name and developer

## Not included yet
- Google Drive automatic sync
- User sign-in or shared family cloud accounts
- Encrypted financial database / biometric lock
- A compiled or signed APK

## Build an installable APK
1. Install Android Studio on a computer from https://developer.android.com/studio
2. Extract this ZIP.
3. In Android Studio choose **Open** and select the extracted `KharchaBookAndroid` folder.
4. Allow Gradle to sync and install the Android SDK components it requests (Android SDK 35).
5. Use **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. Android Studio will show the APK output location (normally `app/build/outputs/apk/debug/app-debug.apk`).
7. Transfer that APK to your Android phone. Open it and allow your file manager to install apps from that source if Android asks.
8. Send the same APK file to friends/family. Each installation stores its data locally on that person's device.

A debug APK is suitable for testing and private sharing. Before wider distribution, build a release APK and sign it. Keep backups: uninstalling the app or losing the phone can remove local data. Automatic Google Drive backup needs separate OAuth/API configuration and implementation.
