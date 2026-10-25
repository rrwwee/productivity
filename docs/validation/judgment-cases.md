# Context-sensitive interruption rule

Historical detailed-rule cases: the current shorter policy and its comparison
are recorded in `short-rule-experiment.md`. The expectations below describe
the former rule, not guarantees of the new broader policy.

Status: empty X composer, X feed (twice), and Wikipedia article passed on the
installed build. See `context-cache-check.json`. Other cases in the table remain
expectations, not verified claims. The earlier `prepared-demo-check.json` exercised
the previous rule.

## Expected cases

| Visible screen | Expected verdict | Why |
| --- | --- | --- |
| X For you or Following feed | NUDGE | Visible passive feed browsing |
| X profile with a list of posts | NUDGE | Visible passive feed browsing |
| X feed with floating compose button and reply icons | NUDGE | Buttons do not mean an editor is open |
| X full-screen new-post composer, even empty | WAIT | Protect writing |
| X reply composer with draft text | WAIT | Protect writing |
| X direct-message conversation or message composer | WAIT | Protect conversations |
| X search field or results | WAIT | Outside the demo's feed-only trigger |
| X individual post or thread | WAIT | Outside the demo's feed-only trigger |
| NASA or Wikipedia article in browser | WAIT | Protect reading |
| Other app mentioning X or embedding a post | WAIT | Mentioning X is not browsing its feed |
| Lock screen, blank frame, or obscuring panel | UNCERTAIN | Insufficient visual evidence |

## Phone acceptance sequence

Use the same installed build and monitoring session. Reset the nudge between
cases; the service deliberately stops sampling after one nudge until reset.

1. Open X's new-post composer with a harmless unsent draft. Trigger a check and
   confirm WAIT in diagnostics and no nudge. Do not publish the draft.
2. Dismiss the composer, return to the feed, reset, and trigger a check. Confirm
   NUDGE and the article notification.
3. Open the linked article, reset, and trigger a check. Confirm WAIT and no nudge.
4. If time allows, repeat the feed/composer contrast once, and exercise a reply
   composer and a direct-message view without sending anything.

Record the verdict, image inference latency, notification presence, and relevant
view for each check. Keep test screenshots local and out of the public repository;
they may contain personal feed or message contents. A verdict is evidence for the
view tested, not a general accuracy estimate.

## Limits and integration notes

- The model sees one 540-pixel-wide frame with a visual token budget of 280. Small
  editor controls and partially obscured views may be hard to distinguish.
- One screenshot cannot establish scrolling duration or a person's actual
  intent. This rule recognizes interface states, not unhealthy behavior.
- The classifier already supports NUDGE, WAIT, and UNCERTAIN. Both quiet verdicts
  suppress notifications, so no classifier or service API change is needed.
- X interface variations, dark mode, browser views, and keyboards need separate
  evidence before claiming broad support.
- The negative cases must run before the positive case, or reset must be used:
  after a nudge, monitoring remains active but sampling is paused by design.
