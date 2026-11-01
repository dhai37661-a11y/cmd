# Remote CMD — repository notes

Native Android app (Kotlin + Jetpack Compose + Room/KSP). There is **no web frontend**:
the app is an APK that runs on a phone and talks to a companion HTTP server the user starts
on their PC (see `app/src/main/java/com/example/data/remote/CompanionScriptProvider.kt`).

## Running it here

`docker-compose.base44.yml` is the executable runbook. It has a single long-running service:

- `preview` — image built from `.base44/android/Dockerfile` (Gradle 9.3.1 + JDK 21 + `python3` +
  Android SDK `platforms;android-36.1`, `build-tools;36.0.0`, `platform-tools`), with the checkout
  bind-mounted at `/workspace`. `.base44/preview/entrypoint.sh` starts
  `.base44/preview/server.py` on host port **3000** immediately and runs
  `gradle assembleDebug testDebugUnitTest` in the background, logging to `build/base44-build.log`.
  The page reports build/test state (and links the log on failure), serves the APK, and shows
  screenshots rendered from the real Compose UI.

Because the app is Android-only, the browser preview cannot render its UI. That is expected —
do not "fix" it by adding a web build; the preview page is the hand-off surface.

Rebuild after editing app code (the container stays up, the page shows "đang build" until done):

```bash
docker compose -f docker-compose.base44.yml restart preview
```

## Gotchas

- **No Gradle wrapper is committed** (`gradlew` and `gradle/wrapper/gradle-wrapper.jar` are
  absent, only `gradle-wrapper.properties`). Do not run `./gradlew`; use the `gradle` on PATH
  inside the container, which matches the wrapper's 9.3.1 distribution.
- **Debug keystore is gitignored and absent** in a fresh clone. The entrypoint generates
  `debug.keystore` with `keytool` when missing — without it `validateSigningDebug` fails.
- The container runs as root because the sandbox checkout is root-owned; Gradle must write
  `app/build`.
- `app/build.gradle.kts` also references a release keystore (`my-upload-key.jks`,
  `STORE_PASSWORD`/`KEY_PASSWORD`). It is only needed for `assembleRelease`.
- **No external credentials are required.** The app has no Gemini/API-key usage in code; the
  commented `GEMINI_API_KEY` line in `.env.example` is unused by the current sources.
  `google-services.json` is absent and `googleServices.missing.passthrough=true` makes that a
  warning; Firebase is a declared dependency but not exercised.
- AGP resolves SDK components at build time, so the first build needs network; the Gradle cache
  lives in the named volume `base44-gradle-home` and is reused across restarts.

## Verify

```bash
docker compose -f docker-compose.base44.yml up -d --build
docker compose -f docker-compose.base44.yml ps                 # expect preview healthy
curl -s localhost:3000/health                                  # expect "ok"
curl -s localhost:3000/ | grep 'unit test'                     # build/test state
curl -sI localhost:3000/app-debug.apk                          # expect 200, ~23 MB
```

`app/build/outputs/apk/debug/app-debug.apk` is the installable artifact.

## Screenshots for the preview

`app/src/test/java/com/example/PreviewScreenshotsTest.kt` renders `MainAppRoot` with Robolectric
(`@GraphicsMode(NATIVE)`) and draws the decor view to PNGs in `build/base44-screenshots`
(`BASE44_SHOT_DIR`). It is a no-op when that variable is unset, so a plain `test` task is
unaffected. `captureToImage()` does not work here — it times out forcing a redraw — so the test
draws the view hierarchy manually.

Unit tests (`app/src/test`) run on the JVM via Robolectric; instrumented tests (`app/src/androidTest`)
need a device/emulator and are not run here.
