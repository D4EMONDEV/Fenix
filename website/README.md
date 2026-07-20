# Fenix website

The public site and mod-author documentation, built with
[Astro](https://astro.build) and [Starlight](https://starlight.astro.build).

Developer documentation for people working *on* Fenix stays in
[`../docs`](../docs) — this site is for people writing mods *with* it.

## Running it

```bash
npm install
npm run dev      # http://localhost:4321
npm run build    # static output in dist/
npm run preview
```

Dependencies are not vendored; `npm install` is required on a fresh checkout.

## Layout

| Path                        | What it is                                   |
|-----------------------------|----------------------------------------------|
| `src/content/docs/`         | Every page. Markdown, routed by file path.   |
| `src/content/docs/guides/`  | Task-oriented pages, auto-added to the sidebar |
| `src/content/docs/reference/` | Reference pages, auto-added to the sidebar |
| `src/styles/fenix.css`      | Accent colours                               |
| `astro.config.mjs`          | Site config, navigation, social links        |
| `public/`                   | Files served as-is: favicon, images          |

Adding a page to `guides/` or `reference/` puts it in the sidebar
automatically — no navigation file to edit.
