# Test-Coverage Campaign, July 2026 — Context and Snapshots

**These documents are point-in-time snapshots, not living documentation.** They record
measurements taken while the test suite was being built, so that the numbers claimed at the
time can be reproduced and audited later. They are deliberately *not* kept up to date as the
code evolves: a later coverage figure will differ, and that is expected. For current numbers,
run the tooling yourself (see [Reproducing the numbers](#reproducing-the-numbers)).

## What this campaign was

MORTAR's non-GUI core — the fragmentation engine, import/export, and the data and utility
model — had no automated test coverage to speak of. The campaign's goal was to bring the
`model/`, `preference/` and `configuration/` layers to roughly 85% line coverage measured with
JaCoCo, and the `controller/` layer to at least 80% per class, without changing application
behaviour. It added JaCoCo to the build, a headless JavaFX test harness (TestFX + Monocle) so
the controllers could be driven without a display, and PIT mutation testing as an advisory
check on whether the covered lines were actually asserted.

The result is 55 test files, roughly 480 tests:

| Area | Test classes |
|------|-------------:|
| `controller` | 15 |
| `model/util` | 10 |
| `model/io` | 5 |
| `model/fragmentation` + `.../algorithm` | 8 |
| `model/data` | 3 |
| `preference` | 3 |
| `integration` (cross-layer flows) | 3 |
| `model/depict`, `model/settings`, `configuration`, `gui/util` | 4 |

## What the phase numbers in these documents mean

The documents refer to phase and requirement numbers (`Phase 12`, `COV-01`, `GATE-01`, …).
Those come from the working plan the campaign was executed against, which lives outside this
repository — `.planning/` is local-only by repo policy, so it is not committed here. The
numbering is recorded below so the references in these files resolve to something a reader can
follow, rather than pointing at a document that is not available:

| Phase | Scope | Completed |
|-------|-------|-----------|
| 1 | JaCoCo tooling and the initial baseline | 2026-06-24 |
| 2 | `model/util` coverage | 2026-06-24 |
| 3 | `model/io` coverage | 2026-06-24 |
| 4 | `model/fragmentation` coverage | 2026-06-25 |
| 5 | `model/data` coverage | 2026-06-25 |
| 6 | `model/depict` coverage | 2026-06-25 |
| 7 | `model/settings` and `configuration` coverage | 2026-06-25 |
| 8 | `preference` coverage | 2026-06-26 |
| 9 | Integration tests (cross-layer flows) | 2026-06-26 |
| 10 | Testability refactors | 2026-06-29 |
| 11 | Aggregate coverage and quality hardening | 2026-06-29 |
| 12 | JaCoCo widened to `controller/`, plus the pre-test baseline → `12-controller-coverage-baseline.md` | 2026-07-03 |
| 13 | Headless JavaFX test harness (TestFX + Monocle); `HARN-02` is the system-property recipe now in `build.gradle.kts` | 2026-07-03 |
| 14 | Controllers needing no toolkit (extract + unit test) | 2026-07-03 |
| 15 | Stage-taking controllers driven through the harness | 2026-07-04 |
| 16 | `MainViewController`, plus its unreachable-lines register → `16-mainviewcontroller-unreachable.md` | 2026-07-04 |
| 17 | Aggregate proof and CI hardening → `17-controller-coverage-final.md` | 2026-07-04 |

Phases 1–11 closed at **86.70%** aggregate line coverage for the non-GUI core, every in-scope
package at or above 85%, on a green build with 361 tests. Phases 12–17 brought all ten
`controller/` classes to at least 80% line coverage (**86.52%** aggregate), with 455 tests.
The suite stands at roughly 480 tests today.

The requirement tags work as follows: `COV-nn` are the per-class and per-package coverage
requirements (`COV-01` is the ≥80%-per-class controller target), `GATE-01`/`GATE-02` the
per-package build-failing coverage gate, `HARN-nn` the headless-harness work, and `QUAL-nn`
the quality criteria — including the allowance for GUI-bound error branches that cannot be
driven headlessly.

Two of these have since moved on and the snapshots are not rewritten to match:

- **`GATE-01`/`GATE-02`.** `17-controller-coverage-final.md` says coverage is "report-only,
  no gate". A gate was added later. It is deliberately **not** part of `check`/`build` —
  its per-package minimums are only reachable on a platform that runs the whole suite — so CI
  requests it explicitly (`./gradlew build jacocoTestCoverageVerification`).
- **CDK version.** The snapshots were captured against CDK 2.12; the project has since moved
  to 2.13, so re-running today will not reproduce these figures exactly.

## Platform matters

Coverage figures in these documents are from **Linux x86-64 (Linux Mint 22.3), Temurin JDK
21**, which is equivalent to the `ubuntu-latest` CI job.

This is not a formality. Six tests are skipped on Windows because the branches they drive do
not exist there — POSIX read-only directories, and the non-Windows resolution of the
application data directory. A Windows run therefore reports lower coverage for `model.util`
and `model.fragmentation` than these documents show, for identical source. That is why the
coverage gate runs only on Linux in CI.

## Reproducing the numbers

```bash
sh gradlew test jacocoTestReport      # report at build/reports/jacoco/test/html/index.html
sh gradlew jacocoTestCoverageVerification   # the per-package gate (not part of `build`)
sh gradlew pitest                     # advisory mutation testing, build/reports/pitest/index.html
```

Each snapshot's own "Reproducibility" section records the exact commit, tool version and
extraction method used for its figures.
