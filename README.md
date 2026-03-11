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

This document explains the project's testing strategy, naming conventions, local execution, CI integration, and coverage reporting.

Acceptance criteria covered:
- Comprehensive tests README
- Gradle scripts to execute test suites (`testAll`, `jacocoHtmlAggregate`)
- GitHub Actions configuration for automated tests
- Automated generation of coverage matrix (aggregated JaCoCo)
- CI automates running and reporting all tests
- Test reports available as HTML documents

### Coverage Matrix (Data / Domain / Presentation)

- Data modules (`data/*`, `core/security`):
  - Unit tests (JVM): required
  - Integration tests: recommended (use in-memory DB or mocked network)
  - Coverage target: minimum 80% (alert if below, fail build if <80%)

- Domain modules (`domain/*`):
  - Unit tests (JVM): required
  - Coverage target: minimum 80%

- Presentation / Feature modules (`feature/*`, `core/ui`, `app`):
  - ViewModel / Presenter unit tests: required
  - UI tests (Espresso/Maestro): included in CI but scheduled for nightly runs
  - Coverage target: minimum 80%

Notes:
- Instrumented UI tests are included in CI but run in a dedicated nightly job (longer running).
- Aggregated coverage is computed across all modules and enforced by Gradle task (fail if <80%).

### Test Naming Conventions

- Test class name: `<ClassName>Test` (e.g., `LoginViewModelTest.kt`)
- Test method convention: `given_<condition>_when_<action>_then_<expectedResult>` (e.g., `givenNoNetwork_whenFetch_thenReturnCached()`)
- Place unit tests in `src/test/kotlin` and instrumented tests in `src/androidTest`.
- Fake and mock classes: prefix with `Fake` / `Mock`.

### Local Execution Guide

Prerequisites: JDK 11, Android SDK (for instrumented tests), Gradle wrapper.

Run all unit tests (multi-module):

```powershell
./gradlew testAll
```

Generate aggregated JaCoCo HTML report:

```powershell
./gradlew jacocoHtmlAggregate
```

Enforce coverage thresholds (runs jacocoAggregate then checks threshold):

```powershell
./gradlew coverageEnforce
```

Generated HTML reports:
- Aggregated HTML index: `build/reports/jacoco/html/index.html`
- Per-module HTML in `build/reports/jacoco/html/<module>/index.html`

### CI/CD — GitHub Actions

The repository includes a workflow at `.github/workflows/test.yml` which:
- Runs unit tests and generates aggregated coverage
- Uploads per-module and aggregated HTML reports as artifacts
- Deploys aggregated HTML to GitHub Pages for public access
- Runs instrumented UI tests in a nightly job using an Android emulator / Maestro

Artifacts produced by the workflow:
- `coverage-report-html` (module-level HTML)
- `coverage-report-aggregate` (aggregated HTML)

### Reporting & Publication

- The `jacocoHtmlAggregate` task produces an `index.html` which serves as a coverage matrix summary.
- CI uploads the HTML reports as artifacts and publishes the aggregated report to GitHub Pages for easy browsing.

---

## Next steps and options

If you want I can:
- Add per-module coverage thresholds and fail the build per module
- Add a nightly job that runs instrumented UI tests (Maestro / Espresso) and publishes its own report
- Improve aggregation to handle Android variant-specific classes and exclusions
