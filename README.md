# Elsewhere

A hackathon Android demo that offers something more interesting when you drift into X.

Start a monitoring session, then open the X app or x.com. About every 30 seconds,
Gemma 4 E2B classifies a reduced-resolution screen image locally. An X feed produces
one notification that opens the James Webb Space Telescope Wikipedia article.
Other screens and uncertain results produce no nudge.

## Run the demo

1. Install the debug APK and its local model with `scripts/install-demo.sh /path/to/gemma-4-E2B-it.litertlm`.
2. Open **Elsewhere** and tap **Start a quiet session**. Grant notifications and screen capture.
3. Open X or x.com and scroll. The normal interval is 30 seconds. For rehearsal,
   **Check in 5 seconds** schedules an earlier real image check.
4. Tap the notification to open the article. **Reset the nudge** permits another
   nudge within the session. **End session** releases capture and the model.

Android shows its screen-sharing indicator during a session. Locking the phone,
starting another screen-sharing session, or stopping from Android ends capture;
starting again requires permission. There are no screenshot animations or gallery
writes. Screen images remain in memory and are discarded after processing.
The app has no Internet permission; the article opens in a separate browser.

## Build

The project uses Java 21, Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.4.0,
Android SDK 36, and LiteRT-LM 0.18.1. Android Studio is optional.

On the hackathon Mac:

```sh
. scripts/android-env.sh
./gradlew :app:assembleDebug
```

For another computer, install an Android SDK and Java 21, set `JAVA_HOME` and
`ANDROID_HOME`, and put `sdk.dir=/path/to/android-sdk` in an untracked
`local.properties` file. The Gradle wrapper fetches Gradle itself.

The Gemma model is intentionally not in Git. Use the standard E2B `.litertlm`
artifact, not a board-specific NPU model. The debug installer copies it into the
app's private files directory.

## Implementation

- `MonitoringService`: one MediaProjection session; attach the image output only
  when a check is due, and detach after a frame. No video encoding or saved frames.
- `ModelRunner`: serialized model initialization/inference, bounded image tokens,
  short classification output, and GPU/CPU initialization fallback.
- `MainActivity`: session controls and local status. A debug-only `classify_file`
  intent allows testing an image staged inside the app's private directory.

Model latency, memory, and thermal behavior must be tested on the actual device.
This demo recognizes X; it does not establish that all X use is unhealthy or offer
general behavior assessment. Current work is tracked in
[Build and verify the Android X-to-article demo](https://github.com/rrwwee/productivity/issues/5).
