---
name: review-checklist
description: How to review an agent's pull request in SwEnt
---
<!-- Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com> -->
Review against AGENTS.md and these points:
- the tests pass, and they actually assert behaviour, not just counts or empty checks;
- every test has a KDoc starting with "Breaks if" that names the production break turning it red (a missing or vague one is an ISSUE);
- the diff is bounded and reviewable; a small feature should not touch unrelated files;
- error handling and edge cases are covered;
- the code follows MVVM and does not touch generated code;
- contributors are acknowledged at the top of the file.
  Report each point as OK or ISSUE, and refuse to approve what you cannot review