# midnightgone
to stop  a person from doomscrolling midnight and actually go to selep

Android app: blocks phone use during a daily window (set the start and end times). The dialer and the emergency
apps you pick stay available.

## Get an APK

1. Push to `main` (or open a pull request). The **Build APK** workflow runs the unit tests and builds the APK.
2. Open the repo's **Actions** tab, then the latest run, then download the `phonelock-apk` artifact.
3. For a public download link, tag a release: `git tag v0.1 && git push --tags`. The APK is attached to the repo's
   **Releases** page, and anyone can open that link on their phone.

Or build locally with Android Studio: **Build > Build APK(s)**, or `./gradlew assembleDebug`
(output: `app/build/outputs/apk/debug/app-debug.apk`).

## Install on a phone

1. Open the APK on the phone. Allow "Install unknown apps" for your browser or file manager when asked.
2. Open the app, set the lock times, tick your emergency apps.
3. Tap the amber banner, then turn on **PhoneLock blocker** in Accessibility settings.
   On Android 13+, if the toggle is greyed out: Settings > Apps > PhoneLock > ⋮ > **Allow restricted settings**, then retry.
