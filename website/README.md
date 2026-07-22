# The Fenix website

React, Vite and TypeScript. A static site — `npm run build` writes `dist/`,
which is everything the server needs.

```bash
npm install
npm run dev      # http://localhost:5173
npm run build
```

## Where the content lives

Every page is a Markdown file under `content/<version>/`, and its path is its
route: `content/0.1/guides/getting-started.md` is served at
`/docs/0.1/guides/getting-started`. Nothing registers a page anywhere — a file
that exists is a page.

Front matter is three fields, all optional except in effect the first:

```markdown
---
title: Getting started
description: One line, shown under the title.
order: 1
---
```

The sidebar groups pages by their directory. Group labels and their order are in
`src/lib/content.ts`; a directory that is not listed there still appears, named
after itself.

## Versions

A version is a directory. Cutting `0.2` means copying `content/0.1` to
`content/0.2` and editing it; the version picker finds it on its own, and the
newest is the default.

## The API reference

`content/<version>/api/` is **generated**. Do not edit it by hand:

```bash
./gradlew apiDocsSite     # from the repository root
```

It is written by a doclet from the javadoc in the source, so the reference
cannot describe an API the compiler does not have.

## Hosting

Any static host. There is no server side, and no build step beyond `npm run
build` — point a web server at `dist/`, with a rewrite so unknown paths serve
`index.html` (the router handles them).
