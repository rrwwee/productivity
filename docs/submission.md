# Elsewhere — hackathon submission and demo

Repo: https://github.com/rrwwee/productivity

Save the repository link and description as a project draft before **7 p.m. Pacific**. Submit the final project before **8 p.m.** Allow time for the form; do not press final submit while edits are still needed. The [event instructions](https://www.agihouse.org/events/the-google-gemma-hardware-hackathon) say submission locks editing and demonstrations are three minutes.

Main track: **Mobile AI**. Also identify the actual use of **LiteRT-LM with GPU acceleration** for the LiteRT award. The event permits multiple awards. Confirm Samsung eligibility with organizers before making a Best in Show / Pixel claim.

## Submission-ready description

**Elsewhere — a better next thing**

Screen-time limits count minutes. Elsewhere offers a reason to spend the next minute differently.

During a monitoring session, Elsewhere samples the screen every 30 seconds and runs Gemma 4 E2B locally to apply a short plain-language interruption rule. The demo distinguishes an X feed from an open post composer: it offers an alternative for the feed and stays quiet while writing. Gemma writes the article invitation locally ahead of time and caches it; live visual judgment decides when to offer it. One tap opens the source. Three prepared article briefs supply the reading choices rather than live web extraction.

Built in Kotlin for Android, Elsewhere runs Gemma through LiteRT-LM with GPU acceleration on a Samsung Galaxy S26 FE. Screen images are processed on the phone and discarded. The app has no Internet permission; opening an article uses the browser and may require connectivity.

## Three-minute pitch

The empty X composer, X feed, and linked Wikipedia article have passed on the installed build. Rehearse this contrast before presenting.

| Time | Action | Suggested words |
| --- | --- | --- |
| 0:00–0:20 | Hold up the phone, with a session already running. | “I often open my phone for a reason and end up scrolling. Screen-time limits tell me to stop. Elsewhere offers something I actually want to start.” |
| 0:20–0:45 | Show an empty X post composer; run a real check. | “It observes only during a session I start. Gemma applies an interruption rule to the screen. When I'm writing, it leaves me alone.” |
| 0:45–1:25 | Switch to X and scroll. The host can schedule a five-second check using Demo controls before switching. | “Now I've drifted into the feed. The model is running here on the phone. There is no cloud model deciding what I'm doing.” |
| 1:25–1:50 | Read the notification and tap it to open its matching article. | “The invitation gives me a reason to choose something else. Gemma wrote this invitation ahead of time from the article's factual brief. Its live judgment decides when to offer it.” |
| 1:50–2:25 | Explain while the article is visible. | “Gemma 4 E2B supplies the visual judgment and the writing. LiteRT-LM runs it with GPU acceleration. The app keeps screen images in memory for processing, then discards them.” |
| 2:25–2:50 | State the implemented boundary. | “For today's demo, the articles and rule are prepared. Next, I want to share articles into Elsewhere and write my own rule—so the alternative and the decision reflect my intentions.” |
| 2:50–3:00 | End on the useful outcome. | “Elsewhere turns an unwanted scroll into a better next thing. The choice stays with me.” |

If a notification arrives earlier, tap it immediately and continue the explanation on the article. Do not wait artificially for the scripted timestamp. If inference is slow, explain the local model while it runs; do not claim the timer makes a pre-scripted decision.

## Claims and evidence

### Verified baseline

The evidence below is recorded in [prepared-demo-check.json](validation/prepared-demo-check.json) and the repository README. Recheck the final installed build before presenting.

- Samsung Galaxy S26 FE / Android 17; Gemma 4 E2B; GPU backend.
- Live X screen produced `NUDGE` in 3,401 ms; generated Voyager copy took 14,254 ms. These are individual measurements, not averages or guarantees.
- A NASA article produced `WAIT` in 2,929 ms.
- The user confirmed tapping the Voyager notification opened NASA.
- The actual generated title and body display during screen sharing; the user confirmed the longer device-configured banner duration worked.
- Copy generation has run for Voyager and MONIAC. The Antikythera brief is bundled but does not yet have its own recorded generation check.
- The minimal Start/Pause UI and Pause cleanup were checked. The foreground service continues after leaving the app while its capture session remains active.

### Verified integrated build

Evidence: [context-cache-check.json](validation/context-cache-check.json).

- Empty X composer: `WAIT`, 2,154 ms, no nudge.
- X feed: `NUDGE`, 3,714 ms; cached invitation lookup 6 ms.
- Repeated X feed after reset: `NUDGE`, 2,441 ms; cached lookup 7 ms.
- Linked Wikipedia article: `WAIT`, 2,448 ms, no nudge.
- First MONIAC invitation generation took 7,538 ms before capture began.
- Notification title/body matched model output, remained public during capture, and tapping opened the correct article (Wikipedia redirects MONIAC to Phillips Machine).

These are individual measurements. Replies and direct messages still need separate checks. A backup video has not yet been recorded. Say **“Gemma writes the invitation ahead of time; its live screen judgment decides when to offer it.”**

### Product direction, not implemented features

Share-to-app article ingestion, live article extraction, editable personal rules, and a local offline article reader are future work. The current external article destination may need the Internet. Do not demonstrate airplane mode and imply that the complete reading flow works offline without first checking it.

## Repeatable demo checklist

### Before each run

1. Confirm the intended APK is installed, the model is ready, and the chosen article matches the intended story. Use Voyager as the already confirmed link unless another article passes a fresh check.
2. Ensure the phone is unlocked, has adequate charge, and shows the intended X account and a usable feed. Avoid showing personal conversations or credentials.
3. Check that the phone's Detailed pop-up setting and 30-second “Time to take action” setting still permit a readable banner.
4. Start a monitoring session and grant **entire-screen** sharing. Keep the phone unlocked. Another capture session can end this one.
5. Under Demo controls, reset the previous nudge. For a scheduled live check, tap **Check in 5 seconds**, then switch promptly to the intended screen.
6. If caching is integrated, wait for verified article preparation before beginning; confirm the cache behavior using diagnostics rather than assuming it is warm.

### Acceptance run on the final build

1. Quiet case: check a readable non-X article. Confirm `WAIT` and no new nudge. For the richer pitch, also check an X composer with draft text; confirm `WAIT` without sending or publishing anything.
2. Positive case: reset, show the X feed, and run a check. Confirm `NUDGE`, a readable title/body, and the correct selected article.
3. Before tapping, run `python3 scripts/check-live-notification.py` to compare the posted notification with the recorded model output.
4. Tap and verify the destination. Return to Elsewhere, reset, and repeat the positive case once to establish a reliable reset path.
5. Pause. Confirm capture and model resources release. Start and authorize a fresh session for the presentation.

### Backup recording

Record one uninterrupted successful run of the **final installed build**, including the quiet case, feed, notification, and tap. An external camera avoids starting another phone capture session and makes the physical device visible. Keep any private content out of frame. Check playback and sound locally before relying on it.

Have the video ready on the laptop. If the live run fails, say **“Here is a recording of this build running on the phone”** and play it. Label recorded footage as recorded; retain the actual timing rather than speeding inference up without disclosure.

## Likely questions

- **Why a model instead of detecting the foreground app?** The intended distinction is the activity and the user's rule. The demo has verified different decisions for an X feed and an empty X composer. It does not infer elapsed scrolling time or establish whether an activity is healthy.
- **Is everything offline?** Judgment and notification writing are local. The app has no Internet permission. The linked article opens separately in a browser and may need connectivity.
- **Does it run all day?** The prototype runs while a user-authorized monitoring session remains active. All-day battery and thermal behavior have not been established.
- **Are these arbitrary articles from the web?** Today there are three prepared factual briefs. Share ingestion and extraction are next steps.
- **Does it block X?** No. A nudge offers an alternative; the person decides whether to open it.

## Short-rule update

The installed rule now refers to social feeds, writing, communicating, and reading.
It contains no X-specific composer instructions. A six-screenshot exploratory
comparison matched the detailed rule, including a post composer, reply composer,
two feed views, and two article views. See `validation/short-rule-experiment.md`.
This is limited evidence, not a general claim of accurate intent recognition.
