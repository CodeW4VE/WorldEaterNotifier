# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project context

All project context — tech stack, architecture, project structure, conventions,
commands, and build/run instructions — lives in **[AGENTS.md](AGENTS.md)**. Read it
first before making changes.

## Current execution / tasks

Active and planned work is tracked in **[PLAN.md](PLAN.md)**. Check it to see what is
in progress, what is done, and what remains.

## Working agreement

- **Keep PLAN.md in sync.** Whenever the plan changes — a task is started, finished,
  added, dropped, or its approach changes — update PLAN.md in the same change so it
  always reflects reality. Mark task status (pending / in progress / done) and note
  the commit when work lands.
- Treat AGENTS.md as the source of truth for how the project is structured; if a change
  alters architecture or conventions, update AGENTS.md too.
- Build with `./gradlew clean build` to verify changes compile (see AGENTS.md).
