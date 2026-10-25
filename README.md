# Elsewhere

A hackathon Android demo that offers something more interesting when you drift into X.

Tap Start, then open the X app or x.com. About every 30 seconds, Gemma 4 E2B
applies the prepared interruption rule to a reduced-resolution screen image:
offer an alternative for an X feed, but leave writing, conversations, articles,
and other activities alone. A `NUDGE` verdict produces one text notification for
the selected prepared article. Gemma writes its title and body locally before
screen sharing starts; validated invitations are cached for reuse. `WAIT` and
`UNCERTAIN` stay quiet.

The demo includes the MONIAC water computer, Voyager's Golden Record, and the
Antikythera mechanism. It does not fetch or summarize a website during the demo.

## Run the demo

1. Install the debug APK and its local model with `scripts/install-demo.sh /path/to/gemma-4-E2B-it.litertlm`.
2. Open **Elsewhere** and tap **Start**. Allow the first invitation to prepare,
   then grant notifications and entire-screen capture. Future starts reuse cached copy.
3. Open X or x.com and scroll. The normal interval is 30 seconds. For rehearsal,
   open **Demo controls** and use **Check in 5 seconds** for an earlier real check.
4. Tap the notification to open the article. **Reset nudge**, under Demo controls,
   permits another nudge. The article selector chooses the next prepared reading.
   **Pause** releases capture and the model.

Android shows its screen-sharing indicator during a session. Locking the phone,
starting another screen-sharing session, or stopping from Android ends capture;
starting again requires permission. There are no screenshot animations or gallery
writes. Screen images remain in memory and are discarded after processing.
The app has no Internet permission; the article opens in a separate browser.
On Samsung, select **Settings → Notifications → App notifications → Elsewhere →
Notification pop-up style → Detailed pop-up** for the larger banner. Its exact size
is controlled by the operating system; expanding the notification reveals the full body.
The article-only nudge supplies a public notification version so Android can show
its generated title and body during screen sharing. This does not expose captured
screen content or change other applications' notification privacy.

For a longer-lived demo banner, the loaner's interactive accessibility timeout
is set to 30 seconds (**Time to take action**). Android owns banner duration;
this is a device setting, not an app-controlled timer. It can also affect other
temporary controls. Restore **Default** in that setting after the demo; the prior
ADB setting was unset (`accessibility_interactive_ui_timeout_ms`).

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
  rule-driven verdicts, article-independent copy generation, and GPU/CPU initialization fallback.
- `ArticleLibrary`: local fact briefs, URLs, and the prepared plain-language rule.
- `NudgeCache`: persistent validated Gemma copy keyed by the article and writer prompt.
- `MainActivity`: one Start/Pause control and collapsed demo tools. A debug-only `classify_file`
  intent allows testing an image staged inside the app's private directory.

Model latency, memory, and thermal behavior must be tested on the actual device.
The demo rule distinguishes passive X feeds from composing and conversations;
it does not establish whether any activity is healthy or infer time spent. The [prepared demo diagram](docs/demo-v2.md) distinguishes
this from the future share-to-app flow and editable personal rules.

## Verified on the demo phone

The debug build is installed on the Samsung Galaxy S26 FE running Android 17.
Live X recognition, locally generated title/body, visible text during capture,
article opening, a quiet non-X screen, and stopping the session have been checked.
See `docs/validation/device-check.json` for measurements and user confirmations.
The saved measurements are for the earlier single-article version; additional
prepared-library checks are recorded separately. Timings vary. The 30-second
interval is when a check starts, not a promise of an instant notification.

With a real nudge still posted, run `python3 scripts/check-live-notification.py`
to verify that its title/body match the model output and remain public during
capture. Run this before tapping or dismissing the notification.

The prepared-content revision is verified in
`docs/validation/prepared-demo-check.json`: live `NUDGE` on X, `WAIT` on the NASA
article, generic copy generation for both Voyager and MONIAC, article-link
confirmation, and Pause cleanup. The third prepared topic is bundled but has
not had its own on-device copy-generation run.

The context-sensitive rule and invitation cache are verified in
`docs/validation/context-cache-check.json`: an empty X composer stayed quiet,
the feed produced a nudge twice, and its linked article stayed quiet. Feed
judgment took 2.4–3.7 seconds in those runs; cached copy lookup took 6–7 ms.
The first invitation still requires generation before capture starts. These
are individual measurements, not performance guarantees.

See [the submission description and three-minute demo](docs/submission.md).
