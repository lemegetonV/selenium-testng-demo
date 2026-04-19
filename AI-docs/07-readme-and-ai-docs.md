# Prompt 07 — README and AI-docs finalization

**Date:** 2026-04-19
**Scope:** Documentation only. No Java source changes. No test execution.
No report regeneration.

---

## Goal

Make the repository legible to a senior QA / SDET reviewer who clones it
cold and has five to ten minutes to form an opinion. The framework is
already stable, the test is green, the extent report with the forwarded-log
narrative is committed. This prompt delivers reviewer-facing documentation.

## Files created

- `README.md` — full reviewer-focused rewrite; replaced a one-line
  placeholder with a ten-section document covering what the test does,
  quick start, how to view the committed report, project structure,
  eight design decisions with reasoning, run-variation overrides, the
  real-orders caveat, AI-assisted development notes, and project
  metadata. Target under 400 lines; prose over bullet walls; no filler
  language.
- `AI-docs/README.md` — one-page index to the folder. Table of prompt
  phases with one-line summaries, plus a Pending section listing the
  per-prompt narrative files that the user authors manually.
- `AI-docs/07-readme-and-ai-docs.md` — this file.

## Files intentionally not modified

- All Java sources under `src/` — the prompt constraint was explicit: no
  code changes to page objects, ElementActions, tests, listeners, or the
  appender. Verified by inspection; every assertion and interaction is
  already in place from Prompts 04–06.5.
- `reports/extent-report.html` — committed green run evidence, kept as-is.
- `AI-docs/PLAN.md` and `AI-docs/locators.md` — already complete.
- `testng.xml` — listener registration confirmed correct
  (`com.demowebshop.listeners.ExtentReportListener`); suite params
  `browser` and `baseUrl` intact; sequential execution (`parallel="none"`,
  `thread-count="1"`).
- `.gitignore` — confirmed to ignore `target/`, `test-output/`, `logs/`,
  `exploration/node_modules/`, IDE/OS cruft; does **not** ignore
  `reports/` or `exploration/screenshots/`. No changes needed.

## Polish pass findings

- No `TODO` / `FIXME` literals in `src/`. Grep returned zero matches.
- No `System.out.println` calls. Grep returned zero matches.
- No commented-out code blocks in source files. The inline comments that
  do exist are load-bearing (e.g. AJAX-race explanation in
  `CheckoutPage.fillBillingAddress`, WARN-level rationale in
  `ElementActions`, country-hardcoding rationale in `TestDataFactory`).
  None were removed.

## Flagged for user — not fixed

- `src/main/java/com/demowebshop/pages/ProductDetailsPage.java` is a dead
  stub. PLAN.md §3 (Amendment at Prompt 03) explicitly removes
  `ProductDetailsPage` from the class inventory — exploration confirmed
  add-to-cart is a tile-level action on the search results page, no PDP
  navigation required. The file was not deleted and still contains a
  forward-reference comment
  (`// Locators and actions implemented in Prompt 05.`).

  This is not a runtime bug — the test is green and the class is never
  instantiated. The prompt constraint disallows code changes to page
  objects, so I did not modify or delete it in Prompt 07. Flagged to the
  user for a decision in a follow-up: either delete the file outright or
  accept the dead stub and move on. Either is safe for submission.

## AI-docs folder state

Present:

- `PLAN.md`
- `locators.md`
- `README.md` (created this prompt)
- `07-readme-and-ai-docs.md` (created this prompt)

Pending (user authors manually):

- `01-planning.md`
- `02-scaffold.md`
- `03-locator-exploration.md`
- `04-page-objects.md`
- `05-tests.md`
- `06-stabilize.md`
- `06.5-rich-extent-report.md`

The CLAUDE.md Progress Log at the project root carries the authoritative
per-prompt narrative for every phase in the interim.

## Decisions made this prompt

- **Single commit for the documentation phase.** One conventional-commit
  `docs: finalize readme and ai-docs for submission` — the README, the
  AI-docs index, and this file are the same unit of work and belong
  together in history.
- **No rebuild, no rerun.** The committed Prompt 06 / 06.5 green run is
  the reference evidence. Re-running would place another real order on
  the shared test account for no documentation benefit.
- **README sections ordered by reviewer priority.** Title and one-line
  summary up top, quick start and committed-report viewing next so a
  reviewer can see evidence within the first minute, design decisions
  in the middle for the deeper read, metadata at the bottom.

## Corrections from user

None — this is the final submission prompt, and Auto Mode is active.

## Project status

Framework stable, test green, committed report evidence present,
documentation complete. Ready for submission.

---

**Build complete.** No next prompt.
