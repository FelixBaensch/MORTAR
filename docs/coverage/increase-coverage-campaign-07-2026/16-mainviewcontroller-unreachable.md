# MainViewController — Documented-Unreachable Lines Register (COV-01)

Companion to the Phase 16 controller-coverage work. Records the final measured
`MainViewController` line coverage and the residual uncovered lines that are
**genuinely unreachable under the headless (Monocle/TestFX) test harness**, with a
per-line justification restricted to the sanctioned categories: native OS
file/directory choosers, the `System.exit`/`Platform.exit` termination tail, and the
OS drag-and-drop gesture bodies (plus two exhaustive-`switch` defensive `default`
branches that no valid enum value can reach).

## Outcome

**≥80% was reached — this register is documentation, not a shortfall justification.**

| Field | Value |
|-------|-------|
| Measured LINE coverage | **639 / 788 = 81.09%** |
| Target | ≥ 80% (COV-01 / ROADMAP success criterion 1) |
| Result | **Met** (81.09% ≥ 80%) |
| Platform | Linux x86-64 (Linux Mint 22.3), Temurin JDK 21 — equivalent to the `ubuntu-latest` CI job; coverage numbers are platform-specific (see the campaign README). |

### Reproducibility

Per `docs/coverage/increase-coverage-campaign-07-2026/12-controller-coverage-baseline.md`:

```bash
sh gradlew test jacocoTestReport
```

Then read `build/reports/jacoco/test/jacocoTestReport.xml`,
`<sourcefile name="MainViewController.java">`, the `<counter type="LINE">` element:
`covered / (covered + missed)` = `639 / (639 + 149)` = `81.09%`. Line total is 788
after the Phase 16 export/persist (16-01) and `applyGlobalSettingsChanges` (16-03,
seam E5) behavior-preserving extractions.

## Register of genuinely-unreachable lines

Line numbers are as of the state of `MainViewController.java` at the end of Phase 16
Plan 3. Each entry is uncovered because exercising it requires an OS dialog Monocle
cannot render, would terminate the JUnit fork, is an OS drag gesture that cannot be
synthesized headlessly, or is a defensive `default` no valid enum value reaches.

### 1. Native file/directory choosers (Monocle cannot render an OS dialog)

| Line(s) | Method | Justification |
|---------|--------|---------------|
| 490–491 | `chooseAndImportMoleculeFile` | Reached only after `Importer.openFile(stage)` (the native `FileChooser`) returns a non-null file; headless the chooser yields null, so the delegation to `importMoleculeFile(File, Importer)` never runs. The import logic itself is covered directly via `importMoleculeFile`. |
| 618–620, 623–626, 628–636, 643–644, 646, 649–651 | `exportFile` | The whole post-guard body is gated on `Exporter.openFileChooserForExportFileOrDir` (the native `FileChooser`/`DirectoryChooser`, lines 623–624). Headless the chooser yields null, so execution returns at 625–626 and the 2D-coordinate decision (628–646) and the `launchExportTask` dispatch (650) never run. The extracted `areExportPreconditionsMet`, `buildExportResult` and `launchExportTask` seams are unit-tested directly with a resolved temp file, so the export precondition, dispatch and callback logic is covered — only the in-method native-chooser plumbing is unreachable. |

### 2. `System.exit` / `Platform.exit` termination tail (cannot terminate the fork in-suite)

| Line(s) | Method | Justification |
|---------|--------|---------------|
| 423–425 | `closeApplication` | Past the guard, `persistSettingsAndStopTasks()` is followed by `Platform.exit()` and `System.exit(aStatus)`, which would kill the JUnit fork mid-suite. Never invoked past the guard; the persist/interrupt sequence is unit-tested via the extracted `persistSettingsAndStopTasks()`. |
| 475–477 | `closeWindowEvent` | Delegates to `closeApplication(0)`, i.e. the `System.exit` tail above; firing it would terminate the fork. Never fired (`Stage.hide()` does not fire the window close-request handler). |

### 3. OS drag-and-drop gesture bodies (not synthesizable headlessly)

| Line(s) | Handler | Justification |
|---------|---------|---------------|
| 390–393, 395–396, 398, 400–401 | `mainCenterPane.setOnDragOver` | Requires a real OS drag gesture carrying a `Dragboard` with files; a valid `DragEvent`/`Dragboard` cannot be constructed under Monocle without brittle reflection. The underlying import that a drop would trigger is covered via the direct `importMoleculeFile` path. |
| 403–407, 409–411 | `mainCenterPane.setOnDragDropped` | Same OS drag-gesture boundary as above. |

### 4. Exhaustive-`switch` defensive `default` branches (no valid enum value reaches them)

| Line | Method | Justification |
|------|--------|---------------|
| 768 | `buildExportResult` | `switch` over `Exporter.ExportTypes` handles all seven constants; the `default -> throw new UnsupportedOperationException(...)` is dead defensive code no valid enum value can reach (a `null` argument throws `NullPointerException` before the default). |
| 1534 | `getStatusMessageByThreadType` | `switch` over `MainViewController.ThreadType` handles all three constants; the `default -> "Could not find message"` is dead defensive code no valid enum value can reach. |

## Note on the remaining uncovered lines

The uncovered set also contains lines that ARE reachable headlessly but are not driven
by the current suite (e.g. the import/fragmentation `Task` failure and cancellation
callbacks, some molecules-tab node-building handler lambdas, and the post-overview
selection-restore block, which needs the view-tools manager to report a cached
structure index). These are **not** listed above and are **not** claimed unreachable —
they are simply beyond the ≥80% target reached for COV-01 and remain available for a
future top-up. Only the categories above are asserted to be genuinely
headless-unreachable.
