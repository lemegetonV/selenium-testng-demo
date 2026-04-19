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

## Pending

The following per-prompt narrative files are authored manually and are not
present in the repository at the time of Prompt 07:

- 01-planning.md
- 02-scaffold.md
- 03-locator-exploration.md
- 04-page-objects.md
- 05-tests.md
- 06-stabilize.md
- 06.5-rich-extent-report.md

`CLAUDE.md` at the project root holds the authoritative Progress Log for
every prompt in the interim; the pending files will be placed here alongside
`07-readme-and-ai-docs.md`.
