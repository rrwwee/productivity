# Prepared-content demo

The demo uses local, prepared article facts so a presentation never depends on
live website fetching or article extraction. The same writer accepts any
`PreparedArticle`; the bundle contains MONIAC, Voyager's Golden Record, and the
Antikythera mechanism. Sources are recorded alongside each fact brief in
`app/src/main/assets/demo-articles.json`.

Gemma receives the plain-language instruction from
`app/src/main/assets/demo-rule.txt`: offer an alternative when the user is on X,
otherwise stay quiet. It returns `NUDGE`, `WAIT`, or `UNCERTAIN`. The app enforces
one job at a time and one nudge per session/reset, while Gemma interprets the rule.

```mermaid
flowchart TD
    Start[Start and approve capture] --> Service[Background service]
    Service --> Frame[Sample one frame every 30 seconds]
    Rule[Prepared plain-language demo rule] --> Judge[Gemma applies rule to frame]
    Frame --> Judge
    Judge -->|WAIT or UNCERTAIN| Service
    Judge -->|NUDGE| Writer[Gemma writes title and body]
    Article[Selected prepared article facts] --> Writer
    Writer --> Validate[Validate structured output]
    Validate -->|Valid| Notify[Public text notification]
    Validate -->|Invalid| Service
    Notify -->|Tap| Link[Open the same article's URL]
    Notify --> Quiet[Wait until reset]
```

The article object is captured when generation starts. Changing the selected
article later cannot pair an already generated title with the wrong URL.

The main screen has a single Start/Pause control. Prepared article selection and
rehearsal controls are collapsed under **Demo controls**. After starting a session,
the screen can be left; the foreground service continues until stopped, the phone
locks, or Android ends projection.

## Later product version

The user's intended product flow is: share a post/article into Elsewhere, resolve
its link, obtain the article text, generate a local factual brief, and save it for
future invitations. A user-entered prompt supplies the interruption rule. Recent
activity and cooldowns can supply context for more nuanced judgment.

Share intake, live URL fetching, and editable personal rules are deliberately
outside this demo. No network permission has been added. The current prompt and
library are prepared assets, while judgment and notification writing happen live
on the phone.
