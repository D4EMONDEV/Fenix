#!/usr/bin/env python3
"""Rebuild every maven-metadata.xml from the versions actually present.

Gradle writes metadata for the versions it just published and nothing else, so
copying a fresh build over the repository replaces a file listing every release
with one listing the newest. The directories survive -- an exact version still
resolves -- but a build asking for a range, or for `latest`, stops seeing
anything older, and `./gradlew dependencies` reports a repository with one
version in it.

Merging the old file into the new one would work until a file went missing. The
directories are the truth: whatever is on disk is what is published, so the
metadata is written from them.

Usage: rebuild-metadata.py <repository-root>
"""

import sys
import re
from pathlib import Path
from xml.sax.saxutils import escape

TEMPLATE = """<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>{group}</groupId>
  <artifactId>{artifact}</artifactId>
  <versioning>
    <latest>{latest}</latest>
    <release>{release}</release>
    <versions>
{versions}
    </versions>
    <lastUpdated>{updated}</lastUpdated>
  </versioning>
</metadata>
"""


def sort_key(version):
    """Order versions so numeric segments compare as numbers.

    A plain string sort puts 0.1.10 below 0.1.2, which would name the wrong
    release as latest. Build metadata (`+mc26.2`) is dropped: semver says it
    takes no part in precedence.
    """
    core = version.split("+", 1)[0]
    parts = re.split(r"[.\-]", core)
    return [(0, int(p), "") if p.isdigit() else (1, 0, p) for p in parts]


def rebuild(metadata_file):
    directory = metadata_file.parent
    versions = sorted(
        (child.name for child in directory.iterdir()
         if child.is_dir() and any(child.glob("*.pom"))),
        key=sort_key,
    )
    if not versions:
        print(f"  {directory}: no versions on disk, left alone")
        return False

    existing = metadata_file.read_text(encoding="utf-8")
    group = re.search(r"<groupId>(.*?)</groupId>", existing)
    artifact = re.search(r"<artifactId>(.*?)</artifactId>", existing)
    updated = re.search(r"<lastUpdated>(.*?)</lastUpdated>", existing)
    if not (group and artifact and updated):
        print(f"  {metadata_file}: unrecognised, left alone")
        return False

    before = re.findall(r"<version>(.*?)</version>", existing)
    metadata_file.write_text(TEMPLATE.format(
        group=escape(group.group(1)),
        artifact=escape(artifact.group(1)),
        latest=escape(versions[-1]),
        release=escape(versions[-1]),
        versions="\n".join(f"      <version>{escape(v)}</version>" for v in versions),
        updated=escape(updated.group(1)),
    ), encoding="utf-8")

    added = [v for v in versions if v not in before]
    if added:
        print(f"  {artifact.group(1)}: {', '.join(before)} + {', '.join(added)}")
    return bool(added)


def main():
    root = Path(sys.argv[1])
    files = sorted(root.rglob("maven-metadata.xml"))
    if not files:
        print(f"no metadata under {root} — nothing to rebuild")
        return
    print(f"rebuilding {len(files)} metadata files from the versions on disk")
    for metadata_file in files:
        rebuild(metadata_file)


if __name__ == "__main__":
    main()
