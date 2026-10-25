# Elsewhere — submission and three-minute demo

Repository: https://github.com/rrwwee/productivity

Save the repository link and description before 7 p.m. Pacific; final submission
is due before 8 p.m. Main track: Mobile AI. The project uses LiteRT-LM with GPU
acceleration and also fits the LiteRT category. Confirm any Pixel-specific
eligibility with organizers; the demo phone is a Samsung.

## Submission description

**Elsewhere — a better next thing**

Screen-time limits tell you to stop. Elsewhere gives you something worth starting.

During a session you choose to start, Gemma 4 E2B checks the screen locally,
applying a short instruction about doomscrolling. When it sees a reason to
interrupt, it writes a fresh invitation to a saved activity. Ignore the invitation
and keep scrolling, and it can try another approach. Move on to reading, and the
current episode ends. The choice stays with you.

Built in Kotlin for Android, Elsewhere runs Gemma through LiteRT-LM with GPU
acceleration on a Samsung Galaxy S26 FE. Screen images are processed on the phone
and discarded. A bounded history contains only the app's own public invitations.
The demo has three prepared article briefs; sharing and web extraction are future
work. The app has no Internet permission; articles open separately in the browser.

## Three-minute presentation

| Time | Action and point |
| --- | --- |
| 0:00–0:25 | Explain the problem: you opened the phone for a reason, then drifted into a feed. Elsewhere offers a better next activity. |
| 0:25–0:50 | Show a reading screen or composer and explain restraint. Gemma decides from the screen and the user's rule. |
| 0:50–1:30 | Switch to a short-video feed or X feed. Allow an actual inference and fresh invitation. Explain local processing during the wait. |
| 1:30–2:00 | Leave the first invitation unanswered. Show another attempt if it arrives; there is no reset between attempts. |
| 2:00–2:30 | Tap an invitation and read the article. The next quiet verdict ends the episode; monitoring continues. |
| 2:30–3:00 | Explain Gemma, LiteRT GPU execution, privacy, and the next step: sharing your own articles and videos into the library. |

The headline is fixed at **STOP. Read this instead.** Gemma writes the factual
body afresh on each attempt. The final writer took 4.3–4.8 seconds in four
on-device checks (three articles plus a follow-up). Do not imply it generates the fixed headline.

Checks target 15-second slots. Only one inference job runs at a time; missed
slots are skipped when generation takes longer. Do not promise a notification
exactly every 15 seconds. Keep the phone unlocked and rehearse the current feed.
A screenshot cannot establish actual intent or time spent scrolling.

## Verified behavior and limits

- `validation/repeated-loop-check.json`: two different generated invitations in
  one episode on YouTube Shorts without reset; correct article opened; WAIT
  ended the episode and removed the notification; returning to Shorts began a
  new episode. These checks preceded the final body-only copy revision.
- `validation/youtube-rule-check.json`: the six-word rule missed a Shorts
  screenshot. A short definition of doomscrolling passed the same Shorts, X,
  composer and article cases. This is an exploratory sample, not an accuracy score.
- Unit tests cover bounded episode history, completion/reset, rejection of late
  results, and removal of an unfinished final phrase from generated copy.
- Android owns banner prominence and sound. The loaner's interactive timeout is
  30 seconds; restore its prior Default setting after the demo.
- Browser destinations may require Internet. No offline article reader exists.
- The app does not diagnose unhealthy use or prove that reading caused the change.

## Rehearsal checklist

1. Open Elsewhere, select the prepared article under Demo controls, and Start.
2. Approve Entire screen. Leave the app for X or YouTube Shorts.
3. Ignore the first nudge to check a second attempt, then tap the current nudge.
4. Confirm the next quiet verdict ends the episode. Return to the feed to start
   another naturally, or use Reset episode to discard the current context.
5. Pause when finished. Capture and model resources should release.

A backup video should be an uninterrupted recording of the actual device,
clearly labeled as recorded. An external camera avoids replacing MediaProjection.
No backup video has been recorded by this task.
