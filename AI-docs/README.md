# AI-docs/

Full record of the AI-assisted build process. One markdown file per prompt
phase, in execution order. Tools used: Claude Code (Anthropic) for framework
and test generation; Playwright CLI for headless locator exploration.

| Phase | File | Summary |
|-------|------|---------|
| 01    | 01-planning.md | Design blueprint: folder structure, dependencies, class inventory, open questions |
| 02    | 02-scaffold.md | Maven project, all utility classes, page object stubs, config, logging, reporting |
| 03    | 03-locator-exploration.md | Playwright-driven headless exploration; produced locators.md |
| 04    | 04-page-objects.md | Implemented all page objects using locators.md |
| 05    | 05-tests.md | EndToEndPurchaseTest with four checkpoint assertions |
| 06    | 06-stabilize.md | First run; AJAX race fix; committed first green extent-report.html |
| 06.5  | 06.5-rich-extent-report.md | Custom Log4j2 appender forwarding SLF4J logs into Extent |
| 07    | 07-readme-and-ai-docs.md | README and AI-docs finalization (this phase) |

Plus:

- `PLAN.md` — locked design blueprint referenced by every prompt
- `locators.md` — per-page Selenium By locators with dropdown value catalogs
  harvested by `exploration/explore.js`

Each prompt file carries the verbatim user prompt text as it was sent, plus
a short summary and an Outcome pointer to the matching entry in the
project-root `CLAUDE.md` Progress Log — where the implementation narrative,
nuances, and corrections live in full.

Prompts 01-05, 06.5, and 07 were structured prompts sent as a single
message. Prompt 06 was not — stabilization happened as two short
conversational follow-ups inside the Prompt 05 session after the first
suite run failed on the billing state dropdown; `06-stabilize.md` captures
those messages verbatim.
