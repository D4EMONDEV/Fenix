# Architecture decision records

Short notes on decisions that were not obvious, so nobody has to relitigate them
from scratch — including the author, six months later.

Write one when a choice closes off a reasonable alternative. Skip it when there
was only ever one sensible option.

Format: numbered, immutable once merged. A decision that gets reversed earns a
new record that supersedes the old one; the old one stays.

| #                                                       | Decision                                     | Status   |
|---------------------------------------------------------|----------------------------------------------|----------|
| [0001](0001-monorepo-with-small-api-modules.md)          | Monorepo, with the API split into small modules | Accepted |
| [0002](0002-compile-time-mod-index.md)                   | Discover mods at compile time, not at launch | Accepted |
