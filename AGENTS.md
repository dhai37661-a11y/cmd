# Remote CMD — repository notes

Native Android app (Kotlin + Jetpack Compose + Room/KSP). There is **no web frontend**:
the app is an APK that runs on a phone and talks to a companion HTTP server the user starts
on their PC (see `app/src/main/java/com/example/data/remote/CompanionScriptProvider.kt`).

## Running it here

`docker-compose.base44.yml` is the executable runbook:

- `android-build` — one-shot job. Uses the `base44-android-sdk` image from
  `.base44/android/Dockerfile` (Gradle 9.3.1 + JDK 21 + Android SDK `platforms;android-36.1`,
  `build-tools;36.0.0`, `platform-tools`), bind-mounts the checkout at `/workspace`, then runs
  `gradle assembleDebug testDebugUnitTest`.
- `preview` — small status server on host port **3000** (`.base44/preview/server.py`). It reports
  build/test state and serves the APK, plus screenshots rendered from the real Compose UI.

Because the app is Android-only, the browser preview cannot render its UI. That is expected —
do not "fix" it by adding a web build; the preview page is the hand-off surface.

## Gotchas

- **No Gradle wrapper is committed** (`gradlew` and `gradle/wrapper/gradle-wrapper.jar` are
  absent, only `gradle-wrapper.properties`). Do not run `./gradlew`; use the `gradle` on PATH
  inside the container, which matches the wrapper's 9.3.1 distribution.
- **Debug keystore is gitignored and absent** in a fresh clone. The `android-build` command
  generates `debug.keystore` with `keytool` when missing — without it `validateSigningDebug`
  fails.
- `app/build.gradle.kts` also references a release keystore (`my-upload-key.jks`,
  `STORE_PASSWORD`/`KEY_PASSWORD`). It is only needed for `assembleRelease`, not for this setup.
- **No external credentials are required.** The app has no Gemini/API-key usage in code; the
  commented `GEMINI_API_KEY` line in `.env.example` is unused by the current sources.
  `google-services.json` is absent and `googleServices.missing.passthrough=true` makes that a
  warning; Firebase is a declared dependency but not exercised.
- AGP downloads/inspects SDK components at build time, so the build needs network on first run;
  the Gradle cache lives in the named volume `base44-gradle-home`.

## Verify

```bash
docker compose -f docker-compose.base44.yml up -d --build
docker compose -f docker-compose.base44.yml logs android-build   # expect BUILD SUCCESSFUL
curl -s localhost:3000/health                                    # expect "ok"
curl -sI localhost:3000/app-debug.apk                            # expect 200, ~23 MB
```

`app/build/outputs/apk/debug/app-debug.apk` is the installable artifact.

## Screenshots for the preview

`app/src/test/java/com/example/PreviewScreenshotsTest.kt` renders `MainAppRoot` with Robolectric
(`@GraphicsMode(NATIVE)`) and draws the decor view to PNGs. It is a no-op unless `BASE44_SHOT_DIR`
is set (the compose job sets it to `build/base44-screenshots`). `captureToImage()` does not work
here — it times out forcing a redraw — so the test draws the view hierarchy manually.
Run it alone with:

```bash
docker compose -f docker-compose.base44.yml run --rm -T android-build \
  sh -ec 'gradle --no-daemon testDebugUnitTest --tests "com.example.PreviewScreenshotsTest"'
```

Unit tests (`app/src/test`) run on the JVM via Robolectric; instrumented tests (`app/src/androidTest`)
need a device/emulator and are not run here.
