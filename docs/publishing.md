# Publishing Fenix

Fenix is published as a plain Maven repository hosted on **GitHub Pages** —
free, public, and with no login required to consume. This page is for the
maintainer; mod authors only need [getting-started](getting-started.md).

## The repository

`https://d4emondev.github.io/Fenix/`

It holds the loader, every API module, the annotation processor, the installer,
Ember, and the `fr.d4emon.fenix.dev` Gradle plugin — all under the group
`fr.d4emon.fenix`.

## One-time setup

On the GitHub repository:

1. **Settings → Pages → Build and deployment → Source: GitHub Actions.**

That is the only manual step. Deployment uses the automatic `GITHUB_TOKEN`;
there is no account, secret, or key to configure.

## Cutting a release

The published version is whatever `gradle.properties` says. A tag triggers the
build; the tag and the property should match.

```bash
# 1. Set the version (drop -SNAPSHOT for a release)
#    edit gradle.properties: version=0.1.0

# 2. Commit it
git commit -am "Release 0.1.0"

# 3. Tag and push
git tag v0.1.0
git push origin main --tags
```

The **Publish** workflow then builds the Maven repository, adds this version to
everything published before it, and deploys the lot. It also builds the
installer on Windows and attaches it to the GitHub release. You can run the
Maven half by hand from the **Actions** tab (`Publish → Run workflow`); the
installer half needs a tag, since a release needs something to attach to.

## Why a `maven-repo` branch

A Maven repository accumulates. Every version ever published has to stay
reachable, because somewhere a mod's build file names it and will keep naming it
for years — an artifact that disappears is a mod that stops building, and its
author has no way to bring it back.

A Pages deployment replaces the whole site. So the repository lives on a branch
of its own: the workflow checks it out, copies the new version in, pushes it
back, and publishes the branch rather than the freshly built directory.

The branch is also the durable copy. If Pages were wiped or reconfigured,
nothing would be lost.

To build the repository locally without deploying:

```bash
./gradlew publishFenixRepo   # writes build/fenix-maven-repo
```

## Versioning notes

- Publish **releases**, not `-SNAPSHOT`s. A statically hosted repository has no
  place to record the rotating build numbers a snapshot needs, and a
  pre-release version sorts *below* its release — so a mod asking for
  `>=0.1.0` would reject `0.1.0-SNAPSHOT`.
- A released version is immutable in consumers' caches. Ship fixes as a new
  version rather than republishing an old one.

## A future upgrade: Maven Central

GitHub Pages is the pragmatic free option. Maven Central would add
discoverability and remove the need for consumers to add a repository at all,
at the cost of a Sonatype account, a namespace (`io.github.d4emondev`, verified
through GitHub), and GPG signing. Worth doing once the API stabilises.
