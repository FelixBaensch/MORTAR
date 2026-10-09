# Controller Package — Final Aggregate Coverage Proof (Milestone Close)

Single-run, per-class coverage snapshot of the `de.unijena.cheminf.mortar.controller`
package, captured **after** the Phases 12–16 controller-test work. It is the milestone's
closing, reproducible proof that **all 10 `controller/` classes reached the ≥80%-per-class
line-coverage target** (QUAL-01 / v1.1 milestone completeness). Unlike the Phase 16 register
(which documents `MainViewController`'s residual unreachable lines) this doc is the single
aggregate proof for the whole package, produced from **one fresh** `sh gradlew test
jacocoTestReport` run — the numbers are not stitched together from the individual phase docs.

Coverage remains **report-only**: no `jacocoTestCoverageVerification` / build-failing coverage
gate is wired into the build or CI (GATE-01/02 are deferred to v2).

## Reproducibility

| Field | Value |
|-------|-------|
| Date captured | 2026-07-04 |
| Platform | Linux x86-64 (Linux Mint 22.3), Temurin JDK 21 — equivalent to the `ubuntu-latest` CI job. **Numbers are platform-specific:** six tests are skipped on Windows because the branches they drive do not exist there (POSIX read-only directories, the non-Windows application-data-directory resolution), so a Windows run reports lower coverage for `model.util` and `model.fragmentation`. |
| Coverage tool | JaCoCo 0.8.15 (`gradle/libs.versions.toml` → `jacoco = "0.8.15"`) |
| CDK release pin | 2.12 (`gradle/libs.versions.toml` → `cdk-version = "2.12"`) |
| Capture commit | `9170db7a` (clean working tree at the start of Phase 17 Plan 1) |
| Source of numbers | `build/reports/jacoco/test/jacocoTestReport.xml`, `<package name="de/unijena/cheminf/mortar/controller">` |
| Extraction method | Deterministic parse of each `<sourcefile>`'s `<counter>` elements via `python3` `xml.etree.ElementTree` (per-class numbers read at the `<sourcefile>` level, never by summing `<class>` nodes) |

**How to reproduce:** regenerate the report with `sh gradlew test jacocoTestReport`, then for the
`controller` package read each `<sourcefile name="X.java">`'s `<counter type="LINE|INSTRUCTION|BRANCH">`
(`missed`/`covered`). Line % = `covered / (missed + covered)`; a self-closing sourcefile with no
LINE counter has no coverable lines (recorded as `n/a`). This is the identical method used for the
Phase 12 baseline (`docs/coverage/increase-coverage-campaign-07-2026/12-controller-coverage-baseline.md`).

## Per-class final coverage

Line % is the primary metric (what the ≥80%-per-class target is measured against);
Instruction % and Branch % are secondary context. Line counts are shown as `covered/total`.

| Class (sourcefile) | Lines (cov/total) | Line % | Instruction % | Branch % | Result |
|--------------------|-------------------|--------|---------------|----------|--------|
| AboutViewController | 77/89 | 86.52% | 87.05% (336/386) | 62.50% (5/8) | Met |
| FragmentationSettingsViewController | 58/58 | 100.00% | 100.00% (322/322) | 90.00% (18/20) | Met |
| HistogramViewController | 449/488 | 92.01% | 91.78% (1976/2153) | 84.95% (79/93) | Met |
| IViewToolController | 0/0 | n/a (no coverable lines) | n/a | n/a | vacuous (excepted) |
| MainViewController | 639/788 | 81.09% | 84.65% (3188/3766) | 62.84% (137/218) | Met |
| OverviewViewController | 423/498 | 84.94% | 86.65% (1875/2164) | 58.46% (114/195) | Met |
| PipelineSettingsViewController | 171/190 | 90.00% | 86.75% (799/921) | 73.21% (41/56) | Met |
| SettingsViewController | 55/55 | 100.00% | 99.58% (239/240) | 70.00% (7/10) | Met |
| TabNames | 4/4 | 100.00% | 100.00% (21/21) | n/a (no branches) | Met (enum) |
| ViewToolsManager | 68/77 | 88.31% | 87.61% (290/331) | 73.08% (19/26) | Met |
| **`controller/**` (aggregate)** | **1944/2247** | **86.52%** | — | — | — |

The aggregate line row is the sum of the per-`<sourcefile>` LINE counters (the zero-line interface
contributes nothing); it matches JaCoCo's own package-level `<counter type="LINE">` aggregate.

### Note on measured vs. expected reference values

Every measured class matches the planning reference within rounding **except `ViewToolsManager`**,
measured here at **88.31%** (68/77) versus a ~83.1% planning reference. The measured value is
recorded as-is — the divergence is upward (more coverage, well above the ≥80% target), so it is
reported rather than forced to the reference number. All other classes land on their references:
`MainViewController` 81.09%, `OverviewViewController` 84.94%, `HistogramViewController` 92.01%,
`PipelineSettingsViewController` 90.00%, `AboutViewController` 86.52%, `SettingsViewController` 100%,
`FragmentationSettingsViewController` 100%, `TabNames` 100%.

## Notes on the two special-case rows

- **`IViewToolController`** — a bodyless interface (only abstract method signatures, no default
  methods, no initialized constants, no static initializer). JaCoCo emits it as a self-closing
  `<sourcefile>` with **no LINE counter**, so it has no coverable lines. Recorded as
  `n/a (no coverable lines)`, **not** 0%, and **not** counted against the ≥80% target (there is
  nothing to cover, and division by zero is guarded against). The class name *is* present in the
  report.
- **`TabNames`** — an enum at `4/4 = 100%` line coverage, loaded transitively by existing
  model/integration and controller tests that reference the enum. It has no branch counter (no
  branching bytecode), so Branch % is `n/a`.

## Milestone statement

In this single fresh `sh gradlew test jacocoTestReport` run, **all 10 `controller/` classes meet the
≥80% line-coverage target** — the vacuous interface (`IViewToolController`) excepted as having no
coverable lines. The lowest LINE-counted class is `MainViewController` at 81.09% (its residual
uncovered lines are the genuinely headless-unreachable set documented in
`docs/coverage/increase-coverage-campaign-07-2026/16-mainviewcontroller-unreachable.md`); the package aggregate is 1944/2247 = 86.52%.

## CI Hardening / Bounded Wall-Clock (QUAL-03)

The headless controller suite is protected from hanging CI by two independent layers, so a wedged
test fails CI within a bounded wall-clock instead of consuming the runner indefinitely.

### 1. Job-level guard (CI config)

`.github/workflows/gradle.yml` sets **`timeout-minutes: 45`** on the `build` job (a sibling of
`runs-on:` under `jobs.build:`). This caps the total build wall-clock: if a headless fork wedges,
GitHub Actions cancels the job at 45 minutes and fails CI rather than hanging. 45 minutes comfortably
exceeds the real ~2–5 minute suite runtime, so it never flakes a healthy build. Coverage stays
report-only — no coverage-verification gate was added.

### 2. In-suite harness bounds (already in place, no code change)

The test harness itself bounds every blocking FX interaction so a stuck fork fails fast well before
the job-level cap:

- **`AbstractFxTestCase.FX_TIMEOUT_SECONDS = 10s`** — a 10-second bound applied to the once-per-JVM
  toolkit boot latch and to **every `runAndWait` latch**. If the FX thread stalls, `runAndWait`
  throws `IllegalStateException("FX runnable did not complete within 10 seconds")` instead of blocking.
- **`FxTestUtil.runAndDriveModal`** — drives a blocking `showAndWait` construct headlessly via a
  `Window.getWindows()` `ListChangeListener` that fires the driver and **always closes the stage in a
  `finally`**, bounded by the same **10-second** latch, with a best-effort timeout recovery that
  removes the listener and closes any still-showing detected stage so a single stuck modal cannot
  poison sibling tests in the fork.
- **`AbstractFxTestCase.waitForFxEvents()`** — drains the FX event queue (via TestFX
  `WaitForAsyncUtils.waitForFxEvents()`) so pending `runLater`/pulse work is processed before
  assertions, avoiding indefinite waits on unflushed events.
- **`AbstractFxTestCase.FX_UNCAUGHT`** — a slot capturing throwables raised on the JavaFX Application
  Thread; `runAndWait` rethrows a captured throwable on the test thread, so a failing `runLater`
  surfaces as a test failure instead of being silently swallowed by the FX event loop (which could
  otherwise leave a test hanging or falsely green).

Together the 10-second in-suite bounds catch a wedged interaction in seconds; the 45-minute job-level
`timeout-minutes` is the outer backstop for anything the in-suite bounds cannot reach.
