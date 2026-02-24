# Contributing to ever2e-jvm

Thanks for your interest in improving ever2e-jvm.

## Development Setup

1. Review architecture and runtime notes:
   - `README.md`
2. Build and run basic checks:
   - `./gradlew classes`
   - `./gradlew test`

## Coding Guidelines

- Keep changes focused and small.
- Preserve Apple IIe compatibility assumptions documented in `README.md`.
- Add or update tests when behavior changes.
- Keep docs and implementation details in sync.

## Pull Requests

- Use clear commit messages.
- Describe what changed and why.
- Include test evidence (commands run and outcomes).
- If behavior changes, update `README.md` and related docs.

## Before Opening a PR

- Local validation for changed components is complete.
- New scripts/build steps are documented.
- Memory map, vector, and soft-switch assumptions remain consistent.
