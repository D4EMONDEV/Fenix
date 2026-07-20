# Security policy

## Supported versions

Fenix is in early development. Until a `1.0` release, only the latest version
receives fixes.

## Reporting a vulnerability

Please **do not open a public issue** for a security problem.

Report it through GitHub's private vulnerability reporting on this repository
(Security → Report a vulnerability), which keeps the discussion private until a
fix ships.

Include what you can: affected version, what an attacker can achieve, and the
steps to reproduce it.

## Scope

A mod loader runs untrusted third-party code inside the game process by design,
so "a mod can execute arbitrary code" is not in itself a vulnerability. What is
in scope:

- Fenix executing code from a source the user did not install — for example a
  jar picked up from outside the mods directory.
- The installer writing outside the `.minecraft` directory it was pointed at.
- The Gradle plugin executing code from a downloaded artifact whose checksum was
  never verified.
- Bypassing an isolation boundary Fenix claims to enforce.
