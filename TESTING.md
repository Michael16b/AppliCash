# Testing Strategy for AppliCash

This document documents the testing strategy, coverage matrix, naming conventions, local execution, CI configuration and publishing of test reports.

## 1. Goals and scope

- Provide fast, reliable feedback on Pull Requests (PRs).
- Provide detailed reporting and coverage on main/master pushes.
- Run instrumented Android UI tests nightly (long-running) and store their reports.
- Enforce minimum aggregated coverage (80%) and warn if coverage is between 80% and 95%.
- Publish an aggregated HTML coverage report to GitHub Pages and upload HTML artifacts for private download.

## 2. Coverage matrix (Data / Domain / Presentation)

- Data modules (`data/*`, `core/security`):
  - Unit tests (JVM) required.
  - Integration tests recommended (use in-memory DB or mock network).
  - Coverage target: minimum 80% (aggregated enforcement).

- Domain modules (`domain/*`):
  - Unit tests (JVM) required.
  - Coverage target: minimum 80%.

- Presentation / Feature modules (`feature/*`, `core/ui`, `app`):
  - ViewModel / Presenter unit tests required.
  - UI tests (Espresso / Maestro) executed nightly in CI.
  - Coverage target: minimum 80%.

Notes:
- Aggregated coverage across all modules is enforced by the Gradle task `coverageEnforce`.
- CI publishes the aggregated HTML report to GitHub Pages and stores the HTML as downloadable artifacts.

## 3. Test naming conventions (English comments)

- Test class: `ClassNameTest` (e.g. `LoginViewModelTest`).
- Test method: `given_<condition>_when_<action>_then_<expected>` (e.g. `givenNoNetwork_whenFetch_thenReturnCached`).
- Unit tests go in `src/test/kotlin`.
- Instrumented tests go in `src/androidTest`.
- Fakes/mocks: prefix with `Fake` / `Mock`.

## 4. Local execution guide

Prerequisites:
- JDK 11 (used for test workflows), JDK 21 for Android build jobs.
- Android SDK (for instrumented tests).
- Use the Gradle wrapper (`./gradlew`).

Common commands (PowerShell):

- Run all unit tests:
```powershell
./gradlew testAll
```

- Generate aggregated JaCoCo HTML report:
```powershell
./gradlew jacocoAggregate jacocoHtmlAggregate
```

- Enforce coverage thresholds (fail if < 80%):
```powershell
./gradlew coverageEnforce
```

- Run instrumented tests locally on an emulator:
```powershell
./gradlew connectedAndroidTest
```

- Open aggregated HTML report (Windows):
```powershell
start build\reports\jacoco\html\index.html
```

## 5. CI strategy (GitHub Actions)

Workflows present:
- `.github/workflows/android.yml` — PR-optimized quick checks: lint + assembleDebug. No unit tests to avoid duplication and speed PRs.
- `.github/workflows/test.yml` — Tests & Coverage: runs unit tests, aggregates coverage, uploads artifacts, publishes to GitHub Pages; also contains nightly instrumented test job.
- `.github/workflows/cache-warmup.yml` — Periodic warm-up of caches.

Key points to maximize PR speed and cache reuse:
- Use `actions/cache` to persist `~/.gradle/caches`, `~/.gradle/wrapper/` and `~/.gradle/caches/build-cache-1` between runs.
- Use `--build-cache` and `--configuration-cache` on test jobs where compatible. For PRs we enable `--configuration-cache` in `test.yml` fast path; `android.yml` keeps `--build-cache` without `--configuration-cache` to avoid AGP incompatibilities.
- Use small PR job (assembleDebug + lint) to quickly detect compilation, resource or formatting issues.
- Run full test suite + coverageEnforce on pushes to `main`/`master` and nightly schedule for instrumented UI tests.

CI artifact and Pages publishing:
- Unit tests job uploads:
  - `coverage-report-html` (module-level HTML)
  - `coverage-report-aggregate` (aggregated HTML index)
- The `publish-pages` job downloads `coverage-report-aggregate` and deploys it to `gh-pages` using `peaceiris/actions-gh-pages`.
- We also keep artifacts for manual download.
- We deliberately do not deploy Pages for PRs to avoid exposing in-progress code; only main/master pushes publish Pages.

## 6. Coverage enforcement & matrix automation

- Aggregation tasks: `jacocoAggregate` collects execution data from modules (`<module>/build/jacoco/jacoco.exec`). `jacocoHtmlAggregate` copies per-module HTMLs and builds `build/reports/jacoco/html/index.html`.
- Enforcement: `coverageEnforce` reads `build/reports/jacoco/jacoco.xml` and:
  - fails the build if aggregated coverage < 80%.
  - logs a warning if 80% <= coverage < 95%.
  - no alert if coverage >= 95%.

Planned enhancement (optional):
- Add per-module thresholds (fail build for module below threshold). I can implement this if desired.

## 7. Nightly instrumented tests

- `test.yml` contains `nightly-instrumented` job which installs an Android emulator and runs `connectedAndroidTest` (Maestro/Espresso).
- This job runs only on schedule (cron at 02:00 UTC) and uploads Android test reports as an artifact `instrumented-tests-report`.

## 8. Reports availability

- Public Pages: aggregated coverage is published to `gh-pages` branch. We configured the deploy action to create the branch if missing (`allow_empty_commit: true`).
- Private artifact: each test workflow uploads HTML reports as artifacts available for download from the workflow run.

You requested both publishing to Pages and storing artifacts: both are implemented.

## 9. Acceptance criteria mapping

- CA18: README.md test documentation: Done — `README.md` points to `TESTING.md`.
- CA19: Gradle scripts: Done — `testAll`, `jacocoAggregate`, `jacocoHtmlAggregate`, `coverageEnforce` exist in `gradle/test-tasks.gradle.kts`.
- CA20: GitHub Actions tests: Done — `.github/workflows/test.yml` executes unit tests and instrumented nightly tests.
- CA21: Automatic coverage matrix generation: Done — `jacocoAggregate` + `jacocoHtmlAggregate` and CI publish.
- CA22: CI manages all tests: Done — unit tests run in `test.yml`; instrumented tests in nightly job.
- CA23: Test report HTML available: Done — artifacts + GitHub Pages publishing.

## 10. Troubleshooting & tips

- If configuration-cache shows incompatibilities in local runs, run without it: `--no-configuration-cache`.
- If CI cache misses are frequent, consider using a more specific cache key that includes dependency lockfiles or Gradle version.
- To make PR runs even faster, a 'fast-tests' task can run only small module subsets or unit tests that changed; this requires more advanced git-aware Gradle logic.

---

If you want, I can now:
- Implement per-module coverage checks that fail the build per-module below 80%.
- Add a fast 'smoke' test task for PRs (shorter run).
- Configure a nightly job that publishes instrumented UI test HTML to Pages/another site.

Tell me which you'd like next and I will implement and validate it.

