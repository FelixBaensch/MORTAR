# Controller Package — Pre-Test Coverage Baseline

One-time, per-class coverage snapshot of the `de.unijena.cheminf.mortar.controller`
package, captured **before any controller test is written**. It records the starting
point against which progress toward the ≥80%-per-class target (Phases 14–16) is measured.

## Reproducibility

| Field | Value |
|-------|-------|
| Date captured | 2026-07-03 |
| Platform | Linux x86-64 (Linux Mint 22.3), Temurin JDK 21 — equivalent to the `ubuntu-latest` CI job. **Numbers are platform-specific:** six tests are skipped on Windows because the branches they drive do not exist there (POSIX read-only directories, the non-Windows application-data-directory resolution), so a Windows run reports lower coverage for `model.util` and `model.fragmentation`. |
| Coverage tool | JaCoCo 0.8.15 (`gradle/libs.versions.toml` → `jacoco = "0.8.15"`) |
| CDK release pin | 2.12 (`gradle/libs.versions.toml` → `cdk-version = "2.12"`) |
| Capture commit | `ea98fac4` (clean working tree; commit that widened the JaCoCo report to include `controller/**`) |
| Source of numbers | `build/reports/jacoco/test/jacocoTestReport.xml`, `<package name="de/unijena/cheminf/mortar/controller">` |
| Extraction method | Deterministic parse of each `<sourcefile>`'s `<counter>` elements via `python3` `xml.etree.ElementTree` (per-class numbers read at the `<sourcefile>` level, never by summing `<class>` nodes) |

**How to reproduce:** regenerate the report with `sh ./gradlew jacocoTestReport`, then for the
`controller` package read each `<sourcefile name="X.java">`'s `<counter type="LINE|INSTRUCTION|BRANCH">`
(`missed`/`covered`). Line % = `covered / (missed + covered)`; a self-closing sourcefile with no
LINE counter has no coverable lines (recorded as `n/a`).

## Per-class baseline

Line % is the primary metric (what the ≥80%-per-class target is measured against);
Instruction % and Branch % are secondary context. Line counts are shown as `covered/total`.

| Class (sourcefile) | Lines (cov/total) | Line % | Instruction % | Branch % |
|--------------------|-------------------|--------|---------------|----------|
| AboutViewController | 0/89 | 0.00% | 0.00% (0/386) | 0.00% (0/8) |
| FragmentationSettingsViewController | 0/58 | 0.00% | 0.00% (0/322) | 0.00% (0/20) |
| HistogramViewController | 0/485 | 0.00% | 0.00% (0/2143) | 0.00% (0/93) |
| IViewToolController | 0/0 | n/a (no coverable lines) | n/a | n/a |
| MainViewController | 0/778 | 0.00% | 0.00% (0/3764) | 0.00% (0/216) |
| OverviewViewController | 0/498 | 0.00% | 0.00% (0/2164) | 0.00% (0/195) |
| PipelineSettingsViewController | 0/190 | 0.00% | 0.00% (0/921) | 0.00% (0/56) |
| SettingsViewController | 0/55 | 0.00% | 0.00% (0/240) | 0.00% (0/10) |
| TabNames | 4/4 | 100.00% | 100.00% (21/21) | n/a (no branches) |
| ViewToolsManager | 0/77 | 0.00% | 0.00% (0/331) | 0.00% (0/26) |
| **`controller/**` (aggregate)** | **4/2234** | **0.18%** | **0.20% (21/10292)** | **0.00% (0/624)** |

The aggregate row is taken directly from the package-level `<counter>` elements (JaCoCo's own
aggregate); it is numerically identical to summing the per-`<sourcefile>` LINE rows because the
zero-line interface contributes nothing.

## Notes on the two special-case rows

- **`IViewToolController`** — a bodyless interface (only abstract method signatures, no default
  methods, no initialized constants, no static initializer). JaCoCo emits it as a self-closing
  `<sourcefile>` with **no LINE counter**, so it has no coverable lines. Recorded as
  `n/a (no coverable lines)`, **not** 0% (there is nothing to cover, and dividing by zero is
  guarded against). The class name *is* present in the report.
- **`TabNames`** — an enum already at `4/4 = 100%` line coverage because it is loaded transitively
  by existing model/integration tests that reference the enum. It has no branch counter (no
  branching bytecode), so Branch % is `n/a`. This is expected: a clean "all-0%" baseline is not
  the reality, and the real number is recorded.

Every other controller class is at 0% because no controller test exists yet; these classes still
appear in the report because it is assembled from the compiled `classDirectories` bytecode merged
with the execution data, independent of whether a test loaded them.
