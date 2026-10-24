# Gemma runtime decision for the hackathon demo

Research date: 2026-10-24. Target supplied by owner: Samsung Galaxy S26 FE, Android 17. This research does not establish successful inference on that device.

## Recommendation

Try **Gemma 4 E2B with LiteRT-LM's Android Kotlin API**, GPU language and vision backends. Use the standard `gemma-4-E2B-it.litertlm` artifact, not the board-specific Qualcomm QCS8275 NPU artifact. This is a recommendation from documented capabilities, not a device compatibility guarantee.

The official [Gemma 4 runtime page](https://developers.google.com/edge/litert-lm/models/gemma-4) lists E2B and E4B support, with E2B at 2.58 GB versus E4B at 3.65 GB. Its Android benchmarks are on S26 Ultra, not S26 FE; neither their latency nor memory figures establish our target performance. E2B is the smaller first candidate. Keep output to a short X / NOT_X decision and validate with an actual screenshot before integrating capture.

## Integration

The [official Kotlin guide](https://developers.google.com/edge/litert-lm/android) provides the Maven dependency `com.google.ai.edge.litertlm:litertlm-android`; resolve and pin a concrete released version during implementation. Configure an `EngineConfig` with a local `modelPath`, `backend = Backend.GPU()` and `visionBackend = Backend.GPU()`. The API accepts `Content.ImageBytes`, avoiding persistent screenshot files. Initialize away from the UI thread; engine initialization can take significant time. Declare the documented optional OpenCL and vndksupport native libraries. Use a fresh short conversation per image rather than accumulating screenshots in history. Start with CPU language fallback only if GPU initialization fails, keeping a supported vision backend; test rather than assume that fallback combination works. Avoid NPU integration under this time limit because its runtime libraries add device-specific setup.

## Download and offline readiness

[The publisher's model card](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) is public, Apache-2.0, and supplies the standard artifact for mobile deployment. It describes image input and loading vision components on demand. The separately named QCS8275 artifact targets a particular board; it is not the Android default.

Standard download candidate:

`https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm`

Download the approximately 2.58 GB file once on the best available connection, then transfer it by USB and import/copy it into app-readable local storage. Ask organizers for an already downloaded copy if Wi-Fi is slow. No inference server is needed after model installation. The model page exposed no gated-access acceptance prompt during research, but an actual binary download was not completed, so network/authentication success remains unverified. Resolve Android build dependencies while network access is available; a local model alone does not make the first build offline-ready.

## Decision gate

Before building the complete UI, require the real S26 FE to load the model and classify one known X screenshot and one non-X screenshot, then repeat while the capture foreground service is running. Record initialization time, inference time, and failure behavior. A text-only response is not proof of image processing; make the prompt depend on supplied pixels. Neither a model benchmark nor foreground inference proves background capture/inference coexistence. Do not substitute a timer or package-name check for Gemma while describing the demo as vision-based.

The iPhone remains a fallback only after separately validating its capture path and background execution; LiteRT-LM Swift image support alone does not prove that pipeline.
