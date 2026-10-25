# Shorter interruption rule experiment

The detailed rule remains available in `short-rule-experiment.json` as the baseline.
Only the interruption rule changed; the system prompt, output parser, GPU model,
and decoding settings stayed fixed. Each image ran in a fresh conversation on the phone.

| Prompt | Original feed / post composer / article | Scrolled feed / reply composer / article body |
| --- | --- | --- |
| Detailed baseline | NUDGE / WAIT / WAIT | NUDGE / WAIT / WAIT |
| Passive-consumption intent only | NUDGE / NUDGE / NUDGE | Not run |
| Mindless-scrolling judgment | NUDGE / WAIT / WAIT | WAIT / WAIT / WAIT |
| Short activity rule | NUDGE / WAIT / WAIT | NUDGE / WAIT / WAIT |

## Adopted rule

> Nudge me while I browse a social media feed. Stay quiet while I write, communicate with someone, or read an article. If unclear, stay quiet.

This avoids app names, composer-specific controls, and enumerated UI exceptions.
The six screenshots are a small exploratory set, not a held-out evaluation or
accuracy estimate. Prompts were refined after observing cases. All captures stay
out of the public repository. Timing and exact tested prompts are in the JSON.
The system still requires NUDGE/WAIT/UNCERTAIN and treats the screen as evidence,
not instructions. The adopted rule no longer restricts its scope to X.

The debug-only `classify_file` intent also accepts `classify_rule` so future
comparisons can vary instructions without changing production policy. Release
builds do not handle these debug extras. Screenshots must be staged privately
and resized to 540 x 1170 to match this phone's live capture pipeline.
