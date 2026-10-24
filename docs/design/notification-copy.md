# A nudge worth opening

The notification competes for attention through its writing. It is text-only.
Gemma generates the title and body on the phone after detecting X; they are not
fixed marketing strings. The current destination is the
[MONIAC / Phillips Machine article](https://en.wikipedia.org/wiki/MONIAC).

The model receives a small factual brief about the hydraulic computer. It does
not receive the user's feed content when writing the notification. The prompt
asks for concrete surprise and an unanswered “how,” without scolding, fake
urgency, unsupported claims, or generic break reminders. Its authoritative prompt
and fact brief are in `ModelRunner.generateNudge`.

Constrained JSON generation supplies `title` and `body`, with limits suitable for
a notification. Incomplete or invalid output is retried and then suppressed;
the app does not substitute a canned notification while claiming it was generated.

The notification uses Android's expanded text style, a direct article action,
and a high-importance channel with a short vibration. Samsung's **Detailed
pop-up** setting is enabled for the test device. The operating system controls
banner dimensions.

Android redacts private notifications during active screen sharing. Because this
nudge contains only public article facts, it explicitly supplies a public version
with the same title, body, and article link. Captured feed content is never placed
in that version, and other apps' notifications retain their own privacy settings.

The demo phone uses a 30-second interactive accessibility timeout to give the
reader more time to act on the banner. This is a phone-wide user setting; the app
does not claim it can enforce that duration on arbitrary phones.
