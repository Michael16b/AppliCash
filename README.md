# AppliCash

## Project Overview

AppliCash is a multi-module Kotlin project targeting Android and shared logic via Kotlin Multiplatform (KMP). The project follows modular architecture with layers: data, domain, and presentation (feature modules). Key technologies and tools used in the project:

- Kotlin Multiplatform (KMP) for shared logic
- Android (Jetpack Compose)
- Maestro (UI testing automation) — optional for UI/nightly runs
- Koin / Hilt (DI) — depending on modules
- Gradle Kotlin DSL (build.gradle.kts)

Team:
- Michaël
- Maxence
- David

---

## Test Strategy

A detailed, living test strategy is available in `TESTING.md`. It describes the coverage matrix (Data/Domain/Presentation), test naming conventions, local execution guide, CI configuration (GitHub Actions), coverage enforcement, where HTML reports are published (GitHub Pages) and how artifacts are uploaded.

Please see `TESTING.md` for the full testing strategy and CI details.
