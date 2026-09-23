<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/70e7aaf7-db91-4aff-9f82-97b682549841

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
7. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.

## What changed in this pass (real login, no Firebase + redesign)

**Real accounts, self-hosted — no Firebase.** Sign-in/sign-up go through a
small backend you run yourself (`/steeldraft-server`, a sibling folder to
this app): plain Node/Express + SQLite, bcrypt-hashed passwords, JWT
sessions. There's no Firebase SDK, no Google Cloud billing account, nothing
metered — the only cost is wherever you choose to host that Node app (many
options have a free tier; see that folder's README).

**Before you run the Android app:**
1. Get `/steeldraft-server` running somewhere reachable from your phone —
   locally for development, or deployed (Render/Railway/Fly.io/your own VPS)
   for real use. Full instructions are in `/steeldraft-server/README.md`.
2. Set `API_BASE_URL` in this project's `.env` (copy from `.env.example`) to
   point at it. Defaults to `http://10.0.2.2:4000/`, which is what the
   Android **emulator** uses to mean "my computer's localhost" — a physical
   phone needs your computer's real LAN IP instead.
3. *(Optional — only if you want the "Continue with Google" button):* create
   an OAuth Web Client ID in Google Cloud Console, then set it in **both**
   `GOOGLE_WEB_CLIENT_ID` in `app/src/main/java/com/example/data/AuthRepository.kt`
   *and* `GOOGLE_WEB_CLIENT_ID` in `/steeldraft-server/.env`. Email/password
   login works fully without this step.

Plain HTTP is only allowed to local addresses (`10.0.2.2`, `localhost`,
`127.0.0.1`) via `network_security_config.xml` — a real deployed server
should be HTTPS, which those hosts provide automatically.

**Redesign.** Colors/type/shapes now live in `app/src/main/java/com/example/ui/theme/`
(`Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`) as a single design-token source —
a dark, iOS-inspired palette with large corner radii, a refined type scale, and
semantic accent colors. Applied so far: the top app bar, the mode switcher
(now a segmented pill control), and a full rebuild of the account/login sheet
(`AuthAndAccountDialog.kt`) with loading/error states, password visibility
toggle, and forgot-password. The toolbar, layer manager, measurement controls,
3D panels, and print/title-block screens still use the original styling and
are good next candidates — they're straightforward to bring onto the same
token system since it's centralized in `ui/theme/`.
