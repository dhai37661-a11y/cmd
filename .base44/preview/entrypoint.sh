#!/bin/sh
# Preview entrypoint: build the app in the background, serve the status page immediately.
#
# The status server is the container's main process so it is reachable within seconds;
# Gradle keeps running as a child and writes its output to build/base44-build.log, which
# the status page reads.
set -e
mkdir -p /workspace/build

# Drop the previous APK so the page never reports a stale build as current.
rm -f /workspace/app/build/outputs/apk/debug/app-debug.apk

(
  cd /workspace
  # DEBUG builds sign with a local debug keystore, which is gitignored and absent in a
  # fresh clone - generate it before the first build.
  if [ ! -f debug.keystore ]; then
    keytool -genkeypair -v -keystore debug.keystore -storepass android \
      -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 \
      -validity 10000 -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
  fi
  gradle --no-daemon --console=plain assembleDebug testDebugUnitTest
) > /workspace/build/base44-build.log 2>&1 &

exec python3 /workspace/.base44/preview/server.py
