# Elsewhere

**[Watch the 47-second demo](https://rrwwee.github.io/productivity/)** · [Download the video](https://github.com/rrwwee/productivity/releases/download/hackathon-demo/elsewhere-demo-simple.mp4)

A hackathon Android demo that offers something more interesting when you drift into a social feed.

Tap Start, then use your phone normally. About every 15 seconds, Gemma 4 E2B
applies the short rule “Nudge me if I'm doomscrolling: passively consuming a social feed or short videos. Otherwise leave me alone.” to a reduced-resolution
screen image. A `NUDGE` verdict generates a fresh invitation body for the next
prepared article under the fixed headline **STOP. Read this instead.** Checks continue after notification delivery. Further NUDGE
verdicts rotate through the saved articles and produce fresh copy with up to three previous invitations as context.
A `WAIT` verdict ends the episode and clears that context and its notification;
`UNCERTAIN` stays quiet and leaves the episode open.

The demo includes Stanford's brain-origins research, Microduck, and the original
*Attention Is All You Need* paper linked directly as a PDF. It does not fetch or summarize a website during the demo.

## Run the demo

1. Install the debug APK and its local model with `scripts/install-demo.sh /path/to/gemma-4-E2B-it.litertlm`.
2. Open **Elsewhere** and tap **Start**. Grant notifications and entire-screen capture.
3. Open X or x.com and scroll. The normal interval is 15 seconds. For rehearsal,
   open **Demo controls** and use **Check in 5 seconds** for an earlier real check.
4. Tap the notification to open the article. **Reset episode**, under Demo controls,
   discards the current episode without stopping checks. Articles rotate on each
   delivered nudge; the last delivered article is remembered across sessions.
   **Pause** releases capture and the model.

### Auto-resume during daily use

Tap **Enable auto-resume**, read the screenshot-access disclosure, and enable
**Elsewhere auto-resume** under Android **Accessibility → Installed apps**.
This optional mode uses Accessibility's screenshot API, not a MediaProjection
session. It takes the same reduced-resolution frames through the same local
Gemma pipeline; it does not request view-tree access or perform gestures.

Checks pause and release the model while the device is locked. Unlocking prepares
the model and resumes checks without another screen-sharing prompt. Swiping
Elsewhere away from Recents does not stop its foreground monitoring service.
Tap **Pause** in Elsewhere to stop; tapping **Start** resumes the selected mode.
Disable the Accessibility service to revoke screenshot access. **Use one-session
mode** in the auto-resume dialog restores the original capture flow.

Android's explicit **Force stop**, power-off, or permission removal can stop the
app. A persistent monitoring notification makes the background session visible.

In the original one-session mode, Android shows its screen-sharing indicator. Locking the phone,
starting another screen-sharing session, or stopping from Android ends capture;
starting again requires permission. There are no screenshot animations or gallery
writes. Screen images remain in memory and are discarded after processing.
The app has no Internet permission; the article opens in a separate browser.
On Samsung, select **Settings → Notifications → App notifications → Elsewhere →
Notification pop-up style → Detailed pop-up** for the larger banner. Its exact size
is controlled by the operating system; expanding the notification reveals the full body.
The article-only nudge supplies a public notification version so Android can show
the fixed headline and generated body during screen sharing. This does not expose captured
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
  when a check is due, and detach after a frame; alternatively request individual
  Accessibility screenshots with lock-aware pause/resume. No video encoding or saved frames.
- `AttentionAccessibilityService`: optional user-enabled screenshot access. No
  view-tree access, input injection, or gesture actions.
- `ModelRunner`: serialized model initialization/inference, bounded image tokens,
  rule-driven verdicts, article-independent copy generation, and GPU/CPU initialization fallback.
- `ArticleLibrary`: local fact briefs, URLs, and the prepared plain-language rule.
- `NudgeEpisode`: bounded invitation history, repeated attempts, and resolution/reset state.
- `MainActivity`: one Start/Pause control and collapsed demo tools. A debug-only `classify_file`
  intent allows testing an image staged inside the app's private directory.

Model latency, memory, and thermal behavior must be tested on the actual device.
The model applies the short doomscrolling rule to one screenshot; it cannot
establish elapsed scrolling time or actual intent from that observation alone. The [prepared demo diagram](docs/demo-v2.md) distinguishes
this from the future share-to-app flow and editable personal rules.

## Verified on the demo phone

The app is installed on the Samsung Galaxy S26 FE running Android 17.

- The live episode loop sent two different invitations on YouTube Shorts without
  a reset. Opening the article led to WAIT, removal of the notification, and
  completion of the episode. Returning to Shorts began another episode.
  See `docs/validation/repeated-loop-check.json`. Those checks used the writer
  immediately before the final headline/body style revision.
- The final writer uses the fixed headline **STOP. Read this instead.** and
  generates only the body. Three articles plus a follow-up were checked on-device
  in 4.3–4.8 seconds per generation. See
  `docs/validation/stop-headline-copy-check.json`.
- The six-word doomscrolling rule missed a YouTube Shorts screenshot. Adding the
  short definition “passively consuming a social feed or short videos” passed
  that Shorts case plus X, a composer, and an article. See
  `docs/validation/youtube-rule-check.json`. These are small exploratory samples,
  not general accuracy estimates.
- Twelve unit tests cover article rotation, episode history, completion/reset, stale-result rejection,
  and complete notification sentences. Build and read-only integration review passed.

Checks target 15-second slots. Long inference skips slots rather than overlapping
jobs. Android controls banner delivery, and a single screenshot cannot establish
elapsed scrolling time or actual intent.

With a nudge still posted, run `python3 scripts/check-live-notification.py` to
verify the app headline and Gemma body are visible during capture.

Earlier revision evidence is retained in `docs/validation/device-check.json`,
`prepared-demo-check.json`, `context-cache-check.json`, and
`short-rule-experiment.json`. Those cover earlier rules and the retired cached-copy
flow, not the current behavior.

See [the submission description and three-minute demo](docs/submission.md).

The current reading list is checked in `docs/validation/new-library-check.json`.
All three URLs return successfully, including the direct arXiv PDF. Generation
for these new briefs took 13.9–16.7 seconds in the recorded run; older 4–5 second
measurements above describe the prior library.
