# AGENTS.md

Docusaurus site for the WorldEaterNotifier wiki (English + Spanish), deployed to GitHub Pages from the `docs-page` branch.

- Content: EN docs in `docs/`, ES docs in `i18n/es/docusaurus-plugin-content-docs/current/`.
- The ES translations of the landing page and navbar/footer live under `i18n/es/` (`.json` files).
- New pages: add a `.md` file with `sidebar_position` frontmatter, then run `npm run write-translations -- --locale es` and translate the generated JSON if needed.

## Commands

- `npm install` — install deps
- `npm run start` — dev server
- `npm run build` — production build (fails on broken links)
- `npm run typecheck` — TypeScript check

## Conventions

- Keep both locales in sync when editing docs.
- Cross-locale links use relative paths (`es/foo` from EN docs, `../foo` from ES docs).
- Deploy is a GitHub Actions workflow (`.github/workflows/deploy.yml`) triggered on push to `docs-page`.
