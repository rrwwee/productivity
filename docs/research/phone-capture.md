# Phone capture feasibility

Research for [Choose a viable phone and cross-app capture path](https://github.com/rrwwee/productivity/issues/2), 2026-10-24.

## Answer and decision gate

Prefer the now-available loaner Pixel with Android MediaProjection, subject to a short physical-device test and Android toolchain setup. The known iPhone configuration is not yet a proven route: on iOS 26.6.1, cross-app ReplayKit capture introduces a separate extension with tight resource limits, while the containing application's sustained background inference remains unproven. An actual upgrade to iOS 27 would make the iPhone ScreenCaptureKit route credible enough for a test, but the owner reports unreliable Wi-Fi. An offered update or an iOS 27 SDK alone is insufficient; avoid making the demo depend on completing that upgrade.

Use one bounded integration test to choose the platform: while X remains foregrounded, obtain a frame, classify it with a local Gemma model, deliver a local notification, and tap that notification to open the fixed article URL. Record the OS, model/runtime, elapsed inference time, and whether capture continues. No physical-device execution has been demonstrated by this research.

## Established environment

- Owner reports iPhone 15 Pro running iOS 26.6.1, with an offered iOS 27.0.1 update but unreliable Wi-Fi. The owner now has a loaner Pixel; its model and Android version remain unresolved.
- Parent agent checked Xcode 27.1 and the installed iPhoneOS SDK: ScreenCaptureKit's relevant stream and picker declarations require iOS 27. This is local toolchain evidence, not evidence that the phone runs iOS 27.
- Parent agent found neither `adb` in PATH nor `~/Library/Android/sdk`. Android setup adds work; no duration guarantee is justified.
- Owner's target is a solo hackathon demo in under four hours: scroll X, have local Gemma inspect the screen, then deliver a notification opening one fixed Wikipedia/article URL.

## iPhone capture and execution

Apple's [iOS ScreenCaptureKit sample](https://developer.apple.com/documentation/screencapturekit/capturing-screen-content-on-ios) requires iOS 27+. It presents a system picker for full-display capture and declares `screen-capture` background execution so that streaming survives when the app is no longer frontmost. Its additional `audio` mode serves its microphone feature; this does not justify adding audio solely to keep this project alive. Apple marks this documentation as preliminary. **Inference:** iOS 27 enables a substantially simpler candidate architecture, but the capture sample does not demonstrate Gemma inference.

For the current iOS 26 phone, [Apple's ReplayKit security documentation](https://support.apple.com/en-ca/guide/security/seca5fc039dd/web) describes user-started system broadcasts delivering raw samples to a Broadcast Upload Extension. This is cross-app capture, not silent periodic access. [Apple's extension guide](https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/ExtensionCreation.html) states that extensions have substantially lower memory limits and can be aggressively terminated. No current Apple source found here establishes a universal exact 50 MB limit. **Inference:** loading a multi-gigabyte model in the extension is an unsuitable assumption; moving samples to the main process still leaves background execution to prove.

[Apple's Metal background guide](https://developer.apple.com/documentation/metal/preparing-your-metal-app-to-run-in-the-background) says new Metal command buffers are rejected after backgrounding under the general iOS execution rules. A screen-capture background declaration is not documented there as permission for arbitrary GPU inference. For an iOS 27 experiment, test CPU vision and CPU inference first, or independently establish an applicable background-GPU mechanism. Neither CPU throughput nor continued execution is guaranteed by this report.

## Pixel capture and execution

[Android MediaProjection](https://developer.android.com/media/grow/media-projection) supports full-display or single-app capture, with frames delivered through a Surface such as ImageReader. Android 14+ targets require the media-projection foreground service type and consent for each capture session. A session can yield multiple frames; consent is not required for every sampled screenshot. Full-display and single-app selections have different coverage, so choose deliberately.

[Android 15 QPR1+](https://developer.android.com/about/versions/15/behavior-changes-all) shows a prominent capture indicator and stops projection when the screen locks. Starting another session requires fresh consent. [Secure windows](https://developer.android.com/security/fraud-prevention/activities) can be absent or blank in capture. An unavailable frame must be treated as unknown, not evidence of healthy behavior.

**Inference:** a user-started foreground-service session is the shortest documented Pixel candidate. It still requires a real device test of concurrent capture and model execution; foreground service support is not a benchmark or guarantee against memory pressure.

## Local model candidate

Google documents [Gemma 4 E2B and E4B support in LiteRT-LM](https://developers.google.com/edge/litert-lm/models/gemma-4), including mobile benchmarks. Published model bundles are 2.58 GB and 3.65 GB. The listed iPhone and Android benchmark devices differ from the owner's iPhone 15 Pro and unknown Pixel. Bundle size is not total runtime memory, and text performance is not screenshot-classification performance. E2B is the smaller candidate to test first; this is a recommendation, not a measured result.

The [Swift API](https://developers.google.com/edge/litert-lm/swift) exposes CPU/GPU backend configuration, a vision backend, and image input including raw bytes. This permits a design that need not write captured images to disk. Image understanding, OS capture, memory pressure, background execution, notification permission, and URL opening remain separate integration checks.

## Shortest physical test

1. Establish an actual supported phone: Pixel model/OS, or an iPhone confirmed to run iOS 27 after any owner-directed update. Confirm provisioning and a minimal app launch.
2. Start an explicit capture session, switch to X, and verify that a recent frame arrives while X remains frontmost. Discard frames in memory after use.
3. Run one screenshot through local Gemma E2B. On iOS, begin with CPU backends because background Metal permission is unresolved. Do not substitute a cloud call.
4. Request notification permission and deliver a local notification based on the result. Tap it to open the fixed article URL.
5. Repeat the complete loop while X is foregrounded, and test cancellation/lock behavior. Choose the route only after this passes; otherwise report which stage failed.

The event's [organizer listing](https://luma.com/6ccl0jw5) requires a Gemma-based, entirely on-device project and lists 10 AM–10 PM PT on October 24. It does not establish the actual submission cutoff or loaner device inventory.
