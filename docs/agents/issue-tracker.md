# Issue tracker: GitHub

Issues and specs live in https://github.com/rrwwee/productivity/issues.
Use the gh CLI from this repository.

## Operations

- Create: `gh issue create --title "..." --body-file <file>`
- Read: `gh issue view <number> --json number,title,body,labels,comments`
- List: `gh issue list --state open --json number,title,body,labels`
- Comment: `gh issue comment <number> --body-file <file>`
- Label: `gh issue edit <number> --add-label "<label>"`
- Remove label: `gh issue edit <number> --remove-label "<label>"`
- Close: `gh issue close <number> --comment "..."`

Use temporary files for multiline bodies. Apply label mappings from
docs/agents/triage-labels.md.

When a skill says “publish to the issue tracker,” create a GitHub issue.
When it says “fetch the relevant ticket,” read the GitHub issue.

## Related work

Use native GitHub sub-issues and issue dependencies when available.
Otherwise, link children with “Part of #<parent>” and record blockers
with “Blocked by: #<number>”.

## Pull requests as a triage surface

PRs as a request surface: no.
