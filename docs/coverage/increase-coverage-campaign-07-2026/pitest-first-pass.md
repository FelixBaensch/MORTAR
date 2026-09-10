# PITest Mutation Testing — First-Pass Triage

**Date:** 2026-07-10
**Platform:** Linux x86-64 (Linux Mint 22.3), Temurin JDK 21 — equivalent to the `ubuntu-latest` CI job; results are platform-specific (see the campaign README).
**Tool:** PITest (PIT) via the `info.solidsoft.pitest` Gradle plugin — **report-only / advisory**, NOT wired into `check`/`build`.
**Run:** `./gradlew pitest` (locally: `sh gradlew pitest`, the wrapper is not executable). Report: `build/reports/pitest/index.html` (+ `mutations.xml`).

## Why this exists

The non-GUI core carries high JaCoCo *line* coverage (85%+ gate). Line coverage only proves a
line **executed** under test — not that any assertion **pins its behavior**. Mutation testing
seeds faults ("mutants") into the bytecode, re-runs the covering tests per mutant, and reports
which mutants **survived** (no test noticed). A surviving mutant on a JaCoCo-covered line is the
actionable signal: *covered but not asserted*. This is measurement + a backlog only — no tests
were changed in this pass.

## Scope of this pass

PIT re-runs the covering tests per mutant, so **scope is the dominant cost lever**. This first
pass targets the deterministic, fast, low-CDK core:

**Included** (`targetClasses` + `targetTests`):
`model.util.*`, `model.data.*`, `model.io.*`, `preference.*`, `configuration.*`

**Deferred** (CDK/algorithm-heavy — enable in a later pass once runtime budget allows):
`model.fragmentation.*`, `model.depict.*`, `model.settings.*`

**Excluded** (mutation-hostile / slow): `controller.*`, `gui.*`, `integration.*`, `main.*`, `message.*`.

> Note on `model.io`: the research flagged `ExporterTest` (Mockito `MockedStatic`) as a possible
> hang risk under PIT. Empirically it ran cleanly (slowest single test 723 ms, zero minion
> hangs), so **no test exclusion was needed** and `model.io` is part of this first pass.

## Headline result

| Metric | Value |
|--------|-------|
| Mutations generated | 1098 |
| Killed | 790 (72%) |
| **Test strength** (killed / (killed+survived), excl. no-coverage) | **80%** |
| Line coverage of mutated classes (PIT-internal) | 2208/2567 (86%) |
| Mutations with no coverage | 114 |
| Wall-clock | ~1 min 20 s (4 threads) |

## Empirical resolution of research assumptions

- **A1 — `pitest-junit5-plugin:1.2.3` vs JUnit Platform 1.11.x (Jupiter 5.11.4):** ✅ works. 60
  test classes discovered, 2098 tests run across mutants. No "no tests found" / `NoClassDefFound`.
  No fallback to 1.2.2 needed.
- **A2 — Kotlin-DSL `.set(...)` lazy `Property` accessors:** ✅ all fields
  (`targetClasses`, `outputFormats`, `threads`, `timeoutFactor`, `jvmArgs`, …) accept `.set(...)`.
  No plain-assignment fallback needed. Config-cache stored cleanly (A5 OK).
- **A4 — `ExporterTest` MockedStatic misbehaving under PIT:** did **not** materialize; no exclusion.

## Per-package mutation score (survivors = assertion gaps)

Score = killed / (killed + survived), excluding NO_COVERAGE.

| Package | Mutations | Killed | Survived | No-coverage | Score |
|---------|-----------|--------|----------|-------------|-------|
| `model.data` | 67 | 61 | 6 | 0 | **91.0%** |
| `preference` | 354 | 271 | 47 | 35 | **85.0%** |
| `model.util` | 321 | 244 | 44 | 32 | **84.4%** |
| `model.io` | 350 | 204 | 95 | 47 | **67.3%** |
| `configuration` | 6 | 4 | 2 | 0 | **66.7%** |

`model.io` (Importer/Exporter) is the weakest by test strength — many survivors are I/O side
effects and molecule-counter arithmetic that tests execute but never assert on. `configuration`
looks low only because it has a tiny mutant population (6).

Survivor counts by class (top): Importer 47, Exporter 42, PreferenceContainer 27, ChemUtil 21,
LogUtil 14, RGBColorPreference 7, FileUtil 5, FragmentDataModel 4.

## Ranked triage — top survivors to assert next

Ranked by *business/correctness value* (core cheminformatics + data-model contracts first;
cosmetic PDF layout and logging side-effects deprioritized).

| # | Class:Line | Method | Mutator | Why it likely survived / what to assert |
|---|-----------|--------|---------|------------------------------------------|
| 1 | `ChemUtil:436` | `fixRadicals` | Math (`-`→`+`) + ConditionalsBoundary + NegateConditionals | Radical/valency correction arithmetic runs but the corrected atom state is never asserted. Assert resulting valency/formal-neighbour-count on a known radical input. |
| 2 | `ChemUtil:506` | `fixAromaticNitrogenAndCreateSMILES` | VoidMethodCall (removed `SmilesParser::kekulise`) | Kekulization side-effect unverified. Assert the produced SMILES for an aromatic-N case differs when kekulise is/ isn't applied. |
| 3 | `ChemUtil:357/365/382/428/475` | `saturateWithHydrogen` / `checkAndCorrectElectronConfiguration` | NegateConditionals | Guard branches execute but neither branch outcome is asserted. Add inputs that exercise both sides of each guard and assert the container state. |
| 4 | `CollectionUtil:149` | `calculateInitialHashCollectionCapacity` | ConditionalsBoundary | Pure-logic boundary (capacity threshold) unasserted — cheap, high-value fix. Assert capacity at the exact boundary load factor. |
| 5 | `StringSortWrapper:149` | `hashCode` | Math (`+`→`-`, `*`→`/`) | hashCode formula unpinned. Assert equal wrappers share a hashCode and the value is stable/expected. |
| 6 | `FragmentDataModel:281/294/306/321` | `setAbsoluteFrequency` / `setMoleculeFrequency` / `setAbsolutePercentage` / `setMoleculePercentage` | ConditionalsBoundary | Validation boundaries in the frequency/percentage setters unasserted. Assert rejection/acceptance exactly at the boundary value. |
| 7 | `MoleculeDataModel:350` | `setKeepAtomContainer` | NegateConditionals | Guard flips silently. Assert the atom-container retention flag effect on both branches. |
| 8 | `PreferenceContainer:770/800/801` | `compareTo` / `hashCode` | PrimitiveReturns (→0) + Math | Ordering/equality contract not asserted. Assert `compareTo` sign for ordered pairs and hashCode consistency with `equals`. |
| 9 | `PreferenceContainer:454/477/500/518` | `add` / `replace` / `delete` | BooleanTrueReturn | Success/failure boolean return is ignored by tests. Assert the boolean result for both the success and the reject (duplicate/missing) paths. |
| 10 | `RGBColorPreference:257/259` | `setContent` | Math (`/`→`*`) | Colour-channel normalization arithmetic unasserted. Assert stored channel values for a known RGBA input. |
| 11 | `RGBColorPreference:269/282` | `setAlpha` | ConditionalsBoundary | Alpha range validation boundary unasserted. Assert accept/reject at alpha min/max. |
| 12 | `Importer:433/443/519 + 445` | `importSDFile` / `importPDBFile` | Increments / Math | Molecule-index/counter arithmetic runs but resulting molecule names/count aren't asserted. Assert imported molecule count and per-molecule naming for a multi-record SDF/PDB. |
| 13 | `Importer:537–545` | `findMoleculeName` (+ lambdas) | Boolean*Return / NegateConditionals | Name-detection predicates unpinned. Assert the chosen name across the fallback chain (title present / blank / absent). |
| 14 | `Importer:276` | `parse` | EmptyObjectReturnVals (→ emptyList) | Returning an empty list instead of parsed molecules goes unnoticed. Assert the parsed list is non-empty and has expected size. |
| 15 | `Exporter:1131` | `convertToITextImage` | NullReturnVals (→ null) | Null image return unasserted. Assert a non-null image is produced for a valid fragment depiction. |
| 16 | `FileUtil:207/233` | `createDirectory` / `createEmptyFile` | BooleanTrueReturn | Return value (created vs. failed) ignored. Assert the boolean result and the filesystem effect. |

### Deprioritized clusters (real survivors, low ROI)

- **`Exporter` PDF-layout `VoidMethodCall`s** (`setFixedHeight`, `setHorizontalAlignment`,
  `addElement`, `setWidths`, lines ~641–810): cosmetic PDF cell formatting; visually asserting
  these is expensive and brittle. Skip unless a layout regression is reported.
- **`LogUtil` (14 survivors):** logging-environment side effects (uncaught-handler install, log-file
  housekeeping). Low correctness value and awkward to assert; revisit only if log-rotation bugs surface.
- **`Exporter`/`Importer` CDK writer/reader `setSetting`/`setSkip`/`kekulize` VoidMethodCalls:** many
  are IO-writer configuration side effects; assert via round-trip output content where practical
  (covered partly by the deferred `integration.*` round-trip tests).

## Second pass — assertion strengthening (2026-07-10, task 260710-ewo)

The follow-up task added targeted, behaviour-pinning assertions for the ranked survivors
(test-only; no production code changed). Same PIT scope as the first pass.

### Headline before/after

| Metric | First pass | After 260710-ewo |
|--------|-----------|------------------|
| Mutations generated | 1098 | 1098 |
| Killed (incl. memory-error / timed-out as detected) | 790 | 819 |
| **Test strength** (killed / (killed+survived), excl. no-coverage) | **80%** | **83%** |
| Survivors (excl. no-coverage) | 194 | 166 |
| Mutations with no coverage | 114 | 113 |

### Per-package score before/after

| Package | Score (first pass) | Score (after) |
|---------|--------------------|---------------|
| `model.data` | 91.0% | **98.5%** |
| `preference` | 85.0% | **88.1%** |
| `model.util` | 84.4% | **87.2%** |
| `model.io` | 67.3% | **71.0%** |
| `configuration` | 66.7% | 66.7% (untouched — tiny population) |

### Ranked-row kill status

| # | Class / method | Status | Notes |
|---|----------------|--------|-------|
| 1 | `ChemUtil.fixRadicals` | **Partial** | electron-removal loop `ConditionalsBoundary` + `NegateConditionals` (L436) killed via `getSingleElectronCount()==0`; the `Math -→+` at L436 and the `setValency/setFormalNeighbourCount/setHybridization` `VoidMethodCall`s (L424–426) remain — re-perception overwrites those atom fields, so the golden-SMILES output is unchanged (likely equivalent). |
| 2 | `ChemUtil.fixAromaticNitrogenAndCreateSMILES` kekulise | Remaining | CDK-heavy; the kekulise `VoidMethodCall` (L506) does not alter the round-trip validation used by the existing tests. Deferred. |
| 3 | `ChemUtil.saturateWithHydrogen` / `checkAndCorrectElectronConfiguration` guards | Remaining | Guard-branch `NegateConditionals` on empty-container early-returns; both branches leave the observable container state identical (empty in/empty out). Deferred as effectively equivalent. |
| 4 | `CollectionUtil.calculateInitialHashCollectionCapacity` | **Killed** | load-factor `== 1.0f` boundary pinned. |
| 5 | `StringSortWrapper.hashCode` | **Killed** | exact formula value pinned (both `Math` mutants). |
| 6 | `FragmentDataModel` frequency/percentage setters | **Killed** | zero lower-boundary acceptance pinned (all 4). |
| 7 | `MoleculeDataModel.setKeepAtomContainer` | **Killed** | cache-clearing effect pinned via `assertNotSame`. |
| 8 | `PreferenceContainer.compareTo` / `hashCode` | **Killed** | compareTo sign + exact hashCode value pinned (4 mutants). |
| 9 | `PreferenceContainer.add` / `replace` / `delete` boolean return (L454/477/500/518) | **Equivalent** | the reject paths return `false` at *earlier* lines (already asserted by existing tests); the success-return lines are provably `true` when reached (GUID uniqueness guarantees the type/name-set adds succeed), so `BooleanTrueReturn` there is an equivalent mutant. Not contorted. |
| 10 | `RGBColorPreference.setContent` channel normalization | **Killed** | non-zero/non-max channels pin the `/255` division. |
| 11 | `RGBColorPreference.setAlpha` range boundaries | **Killed** | 0.0/1.0 (double) and 0 (int) inclusive boundaries pinned. |
| 12 | `Importer.importSDFile`/`importPDBFile` counters | **Killed** (SDF) / **Equivalent** (PDB L519, L445) | SDF counter increments — L433 (erroneous-entry skip branch) and L443 (added-molecule) — pinned via exact ordered fallback-name assertions on `MultiRecord.sdf` and the new `MultiRecordUnnamedWithError.sdf` fixture (the untitled records' fallback name embeds the counter value). PDB L519 `Increments` survives on the single-model deprecated path (the sole iteration's post-increment is never read again → equivalent, not contorted); Importer L445 `Math` feeds only a WARNING log with no observable behaviour → logging-only equivalent. |
| 13 | `Importer.findMoleculeName` | **Killed** (residual L544 gate = equivalent) | Distractor-property tests pin the name-branch filter (L538) and the id-branch filter (L545) — all four `Boolean*Return` mutants killed. The L544 `anyMatch` is only an existence gate whose outcome is masked by the L545 filter selection, so its residual gate-predicate mutants (one `NegateConditionals`, one `BooleanTrueReturn`) are effectively equivalent. |
| 14 | `Importer.parse` empty-list return (L276) | **Equivalent** | L276 is the null/empty-set guard `return new ArrayList<>()`; replacing it with `emptyList()` is semantically identical on that path. |
| 15 | `Exporter.convertToITextImage` null return | **Killed** | direct reflection test asserts a non-null `com.lowagie.text.Image` is produced for a valid, PNG-encodable `BufferedImage`, killing the `NullReturnVals` mutant (L1131) that the full PDF tests swallowed (a null image simply does not appear in the PDF). |
| 16 | `FileUtil.createDirectory` / `createEmptyFile` boolean return | **Killed** | failure paths pinned (mkdirs blocked by a file; createNewFile on an existing directory name). |

### Remaining next steps

1. Documented-equivalent `model.io` survivors (no further action, no production change): `Importer.importPDBFile`
   counter L519 (single-model deprecated path — the sole iteration's increment is never read again),
   `Importer.importSDFile` L445 `Math` (feeds a WARNING log only), and the `findMoleculeName` L544 `anyMatch`
   gate-predicate mutants (masked by the L545 filter selection).
2. Second PIT pass adding `model.fragmentation.*` / `model.depict.*` / `model.settings.*` once a
   runtime/CI budget is agreed (expect minutes, CDK-heavy). Keep PIT report-only and out of the gate.

## Third pass — model.io counter / name / image survivors (2026-07-10, task 260710-ghh)

Closed the three still-open ranked-triage rows (12, 13, 15) by pinning covered-but-unasserted
`model.io` behaviour (test-only; no production code changed). Same PIT scope as the prior passes.

### Headline before/after

| Metric | After 260710-ewo | After 260710-ghh |
|--------|------------------|------------------|
| Mutations generated | 1098 | 1098 |
| Killed (incl. memory-error / timed-out as detected) | 819 | 827 |
| **Test strength** (killed / (killed+survived), excl. no-coverage) | **83%** | **84%** |
| Mutations with no coverage | 113 | 113 |

All eight additional kills this pass landed in `model.io`.

### Per-package score before/after

| Package | Score (after ewo) | Score (after ghh) |
|---------|-------------------|-------------------|
| `model.data` | 98.5% | 98.5% |
| `preference` | 88.1% | 88.1% |
| `model.util` | 87.2% | 87.2% |
| `model.io` | 71.0% | **73.6%** |
| `configuration` | 66.7% | 66.7% |

### model.io survivor counts by class (after ghh)

`Importer` 33 survivors (was 47 at first pass), `Exporter` 41 survivors (44 incl. the `CSVSeparator` /
`FileExtension` nested-enum display-name mutants; was 42 at first pass). `DynamicSMILESFileReader` 3,
`DynamicSMILESFileFormat` 0.

### Targeted mutants flipped SURVIVED → KILLED

- `Importer` L433 `Increments` (SDF erroneous-entry skip counter) — **KILLED**
- `Importer` L443 `Increments` (SDF added-molecule counter) — **KILLED**
- `Importer` L538 `Boolean*Return` (findMoleculeName name-branch filter, ×2) — **KILLED**
- `Importer` L545 `Boolean*Return` (findMoleculeName id-branch filter, ×2) — **KILLED**
- `Importer` L544 `NegateConditionals` / `BooleanFalseReturn` (id-branch, partial) — **KILLED**
- `Exporter` L1131 `NullReturnVals` (convertToITextImage) — **KILLED**

Documented-equivalent (still SURVIVED, no production change): `Importer` L519 (PDB single-model),
`Importer` L445 (`Math`, logging-only), `Importer` L544 residual `anyMatch` gate-predicate (masked by L545).

## Next steps (first-pass backlog, superseded by the section above)

1. Strengthen assertions for triage rows 1–11 (core `model.util` / `model.data` / `preference`
   logic) — cheapest, highest-signal wins.
2. Address `model.io` counter/naming survivors (rows 12–15) with fixture-based round-trip assertions.
3. Second PIT pass adding `model.fragmentation.*` / `model.depict.*` / `model.settings.*` once a
   runtime/CI budget is agreed (expect minutes, CDK-heavy). Keep PIT report-only and out of the gate.
