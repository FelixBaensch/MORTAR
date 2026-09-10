/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2026  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unijena.cheminf.mortar.model.fragmentation;

import de.unijena.cheminf.mortar.configuration.Configuration;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.util.ChemUtil;

import javafx.beans.property.Property;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Direct, headless unit tests for {@link FragmentationTask}, targeting the per-molecule exception-counter branch
 * (the generic {@code catch} in {@link FragmentationTask#call()}) that the {@link FragmentationService} happy path does
 * not reach. The branch is driven with a test-local {@link ThrowingFragmenter} — a <b>real</b> class implementing
 * {@link IMoleculeFragmenter} whose {@code fragmentMolecule} throws — not a Mockito mock (no Mockito is used anywhere in
 * MORTAR's tests). The task's {@code call()} blocks and returns the exception count synchronously, so the assertion
 * follows directly with no sleep/latch. The happy-path, filter-skip and preprocessing branches are covered transitively
 * via {@code FragmentationServiceTest} (plan 04-01); this test covers only the exception counter.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class FragmentationTaskTest {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor that sets the default locale to en-GB (for deterministic, message-bundle-resolved strings) and
     * bootstraps the Configuration singleton from the classpath (no data directory is touched by this).
     *
     * @throws Exception if the Configuration singleton cannot be initialized
     */
    public FragmentationTaskTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
        Configuration.getInstance();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Drives {@link FragmentationTask#call()} over a real molecule list with the test-local {@link ThrowingFragmenter}
     * (whose {@code fragmentMolecule} throws a {@link NullPointerException} for every molecule), and asserts the returned
     * exception count is {@literal >} 0 — one per molecule that threw — confirming the per-molecule generic catch branch
     * is exercised and the batch is not aborted by a single failure.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void exceptionCounterTest() throws Exception {
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(FragmentationTaskTest.buildMDM("c1ccccc1"));
        tmpMols.add(FragmentationTaskTest.buildMDM("O=C(O)CCCC(=O)O"));
        Map<String, FragmentDataModel> tmpFragmentMap = new ConcurrentHashMap<>();
        FragmentationTask tmpTask = new FragmentationTask(
                tmpMols, new ThrowingFragmenter(), tmpFragmentMap, "TaskTest", false);
        FragmentationTaskResult tmpResult = tmpTask.call();
        Assertions.assertNotNull(tmpResult);
        Assertions.assertTrue(tmpResult.exceptionsCount() > 0);
        Assertions.assertEquals(tmpMols.size(), tmpResult.exceptionsCount());
    }
    //
    /**
     * Drives the {@code getAtomContainer} CDKException branch of {@link FragmentationTask#call()} by passing a
     * {@link MoleculeDataModel} built from a syntactically invalid SMILES string (so re-creating its atom container from
     * the persisted SMILES throws a {@link org.openscience.cdk.exception.CDKException}). The task must count the failure
     * per molecule, not abort the batch, and return the count of molecules that failed atom-container creation.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void getAtomContainerExceptionTest() throws Exception {
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        //two MoleculeDataModels whose stored SMILES cannot be parsed back into an atom container -> CDKException branch
        tmpMols.add(new MoleculeDataModel("not_a_valid_smiles_$$$", "Bad1", new HashMap<>(0)));
        tmpMols.add(new MoleculeDataModel("also[invalid", "Bad2", new HashMap<>(0)));
        Map<String, FragmentDataModel> tmpFragmentMap = new ConcurrentHashMap<>();
        FragmentationTask tmpTask = new FragmentationTask(
                tmpMols, new SuccessfulFragmenter(), tmpFragmentMap, "BadSmilesTask", false);
        FragmentationTaskResult tmpResult = tmpTask.call();
        Assertions.assertNotNull(tmpResult);
        Assertions.assertEquals(tmpMols.size(), tmpResult.moleculeFailedGetAtomContainerCount());
        Assertions.assertEquals(0, tmpResult.exceptionsCount());
        //no fragments could be produced because every molecule failed atom-container creation
        Assertions.assertTrue(tmpFragmentMap.isEmpty());
    }
    //
    /**
     * Drives the {@code shouldBeFiltered} skip branch of {@link FragmentationTask#call()}: a fragmenter that reports every
     * molecule as to-be-filtered causes the task to store empty fragment lists and frequency maps for each molecule and to
     * never call {@code fragmentMolecule}. The task must return a zero exception count, store an empty fragment list under
     * the fragmentation name for each molecule, and leave the shared fragment map empty.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void filteredMoleculesAreSkippedTest() throws Exception {
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(FragmentationTaskTest.buildMDM("c1ccccc1"));
        tmpMols.add(FragmentationTaskTest.buildMDM("O=C(O)CCCC(=O)O"));
        Map<String, FragmentDataModel> tmpFragmentMap = new ConcurrentHashMap<>();
        FragmentationTask tmpTask = new FragmentationTask(
                tmpMols, new FilteringFragmenter(), tmpFragmentMap, "FilterTask", false);
        FragmentationTaskResult tmpResult = tmpTask.call();
        Assertions.assertNotNull(tmpResult);
        Assertions.assertEquals(0, tmpResult.exceptionsCount());
        Assertions.assertEquals(tmpMols.size(), tmpResult.filteredMoleculesCount());
        Assertions.assertTrue(tmpFragmentMap.isEmpty());
        for (MoleculeDataModel tmpMolecule : tmpMols) {
            Assertions.assertTrue(tmpMolecule.getAllFragments().containsKey("FilterTask"));
            Assertions.assertTrue(tmpMolecule.getAllFragments().get("FilterTask").isEmpty());
        }
    }
    //
    /**
     * Drives the {@code shouldBePreprocessed} branch of {@link FragmentationTask#call()}: a fragmenter that reports every
     * molecule as needing preprocessing causes the task to call {@code applyPreprocessing} before fragmenting. The
     * preprocessing fragmenter returns a single-fragment result for each molecule, so the task must complete with a zero
     * exception count and a non-empty shared fragment map, and must have invoked the preprocessing step.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void preprocessingBranchTest() throws Exception {
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(FragmentationTaskTest.buildMDM("c1ccccc1"));
        tmpMols.add(FragmentationTaskTest.buildMDM("O=C(O)CCCC(=O)O"));
        Map<String, FragmentDataModel> tmpFragmentMap = new ConcurrentHashMap<>();
        PreprocessingFragmenter tmpFragmenter = new PreprocessingFragmenter();
        FragmentationTask tmpTask = new FragmentationTask(
                tmpMols, tmpFragmenter, tmpFragmentMap, "PreprocessTask", false);
        FragmentationTaskResult tmpResult = tmpTask.call();
        Assertions.assertNotNull(tmpResult);
        Assertions.assertEquals(0, tmpResult.exceptionsCount());
        Assertions.assertEquals(tmpMols.size(), tmpResult.moleculeProducedFragmentsCount());
        Assertions.assertTrue(tmpFragmenter.wasPreprocessingApplied());
        Assertions.assertFalse(tmpFragmentMap.isEmpty());
    }
    //
    /**
     * Drives the generic (outer) {@code catch} branch of {@link FragmentationTask#call()}: a fragmenter whose
     * {@code fragmentMolecule} throws an {@link IllegalStateException} — a runtime exception NOT covered by the inner
     * typed catch ({@code NullPointerException | IllegalArgumentException | CloneNotSupportedException}) — so the exception
     * propagates to the per-molecule generic catch. The task must count one exception per molecule, store empty
     * fragment/frequency entries for each, and not abort the batch.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void genericCatchBranchTest() throws Exception {
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(FragmentationTaskTest.buildMDM("c1ccccc1"));
        tmpMols.add(FragmentationTaskTest.buildMDM("O=C(O)CCCC(=O)O"));
        Map<String, FragmentDataModel> tmpFragmentMap = new ConcurrentHashMap<>();
        FragmentationTask tmpTask = new FragmentationTask(
                tmpMols, new GenericThrowingFragmenter(), tmpFragmentMap, "GenericCatchTask", false);
        FragmentationTaskResult tmpResult = tmpTask.call();
        Assertions.assertNotNull(tmpResult);
        Assertions.assertEquals(tmpMols.size(), tmpResult.unexpectedExceptionsCount());
        for (MoleculeDataModel tmpMolecule : tmpMols) {
            Assertions.assertTrue(tmpMolecule.getAllFragments().containsKey("GenericCatchTask"));
            Assertions.assertTrue(tmpMolecule.getAllFragments().get("GenericCatchTask").isEmpty());
            Assertions.assertTrue(tmpMolecule.getFragmentFrequencies().containsKey("GenericCatchTask"));
        }
    }
    //
    /**
     * Drives the thread-interrupted early-return branch of {@link FragmentationTask#call()}: the current thread's interrupt
     * flag is set before the task is run, so after processing the first molecule the task observes the interruption and
     * returns {@code null}. The fragmenter used succeeds, so the early return is caused solely by the interrupt; the
     * interrupt flag is cleared in a finally block so sibling tests are unaffected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void threadInterruptedReturnsNullTest() throws Exception {
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(FragmentationTaskTest.buildMDM("c1ccccc1"));
        tmpMols.add(FragmentationTaskTest.buildMDM("O=C(O)CCCC(=O)O"));
        Map<String, FragmentDataModel> tmpFragmentMap = new ConcurrentHashMap<>();
        FragmentationTask tmpTask = new FragmentationTask(
                tmpMols, new SuccessfulFragmenter(), tmpFragmentMap, "InterruptTask", false);
        FragmentationTaskResult tmpResult;
        try {
            //set the interrupt flag so the task returns null after the first molecule
            Thread.currentThread().interrupt();
            tmpResult = tmpTask.call();
        } finally {
            //clear the interrupt status so subsequent tests are not affected
            Thread.interrupted();
        }
        Assertions.assertNull(tmpResult);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private static methods" defaultstate="collapsed">
    /**
     * Builds a {@link MoleculeDataModel} from the given SMILES string using {@link ChemUtil#parseSmilesToAtomContainer}
     * and the (IAtomContainer, boolean) constructor without stereochemistry encoding.
     *
     * @param aSmiles SMILES string to parse
     * @return a MoleculeDataModel wrapping the parsed atom container
     * @throws Exception if parsing or unique-SMILES creation fails
     */
    private static MoleculeDataModel buildMDM(String aSmiles) throws Exception {
        IAtomContainer tmpAtomContainer = ChemUtil.parseSmilesToAtomContainer(aSmiles, false, false);
        return new MoleculeDataModel(tmpAtomContainer, false);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test-local fragmenter" defaultstate="collapsed">
    /**
     * Real (non-mock) test-local {@link IMoleculeFragmenter} whose {@link #fragmentMolecule(IAtomContainer)} always
     * throws a {@link NullPointerException}, used to drive the exception-counter branch of {@link FragmentationTask}.
     * Every other interface method is implemented minimally so the task reaches {@code fragmentMolecule}: the molecule is
     * neither filtered nor preprocessed and is considered fragmentable.
     */
    private static final class ThrowingFragmenter implements IMoleculeFragmenter {
        /**
         * Constructor (no state).
         */
        private ThrowingFragmenter() {
        }
        //
        @Override
        public List<Property<?>> settingsProperties() {
            return new ArrayList<>(0);
        }
        //
        @Override
        public Map<String, String> getSettingNameToTooltipTextMap() {
            return new HashMap<>(0);
        }
        //
        @Override
        public Map<String, String> getSettingNameToDisplayNameMap() {
            return new HashMap<>(0);
        }
        //
        @Override
        public String getFragmentationAlgorithmName() {
            return "ThrowingFragmenter";
        }
        //
        @Override
        public String getFragmentationAlgorithmDisplayName() {
            return "Throwing Fragmenter";
        }
        //
        @Override
        public IMoleculeFragmenter copy() {
            return new ThrowingFragmenter();
        }
        //
        @Override
        public void restoreDefaultSettings() {
            //no-op: this test fragmenter has no settings
        }
        //
        @Override
        public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
                throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
            throw new NullPointerException("ThrowingFragmenter always throws to exercise the exception-counter branch");
        }
        //
        @Override
        public boolean shouldBeFiltered(IAtomContainer aMolecule) {
            return false;
        }
        //
        @Override
        public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
            return false;
        }
        //
        @Override
        public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
            return true;
        }
        //
        @Override
        public IAtomContainer applyPreprocessing(IAtomContainer aMolecule)
                throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
            return aMolecule;
        }
    }
    //
    /**
     * Real (non-mock) test-local {@link IMoleculeFragmenter} whose {@link #fragmentMolecule(IAtomContainer)} returns the
     * input molecule itself as a single fragment, so the task reaches the fragment-aggregation path and the shared map is
     * populated. The molecule is neither filtered nor preprocessed and is considered fragmentable.
     */
    private static class SuccessfulFragmenter implements IMoleculeFragmenter {
        /**
         * Constructor (no state).
         */
        private SuccessfulFragmenter() {
        }
        //
        @Override
        public List<Property<?>> settingsProperties() {
            return new ArrayList<>(0);
        }
        //
        @Override
        public Map<String, String> getSettingNameToTooltipTextMap() {
            return new HashMap<>(0);
        }
        //
        @Override
        public Map<String, String> getSettingNameToDisplayNameMap() {
            return new HashMap<>(0);
        }
        //
        @Override
        public String getFragmentationAlgorithmName() {
            return "SuccessfulFragmenter";
        }
        //
        @Override
        public String getFragmentationAlgorithmDisplayName() {
            return "Successful Fragmenter";
        }
        //
        @Override
        public IMoleculeFragmenter copy() {
            return new SuccessfulFragmenter();
        }
        //
        @Override
        public void restoreDefaultSettings() {
            //no-op: this test fragmenter has no settings
        }
        //
        @Override
        public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
                throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
            List<IAtomContainer> tmpFragments = new ArrayList<>(1);
            tmpFragments.add(aMolecule);
            return tmpFragments;
        }
        //
        @Override
        public boolean shouldBeFiltered(IAtomContainer aMolecule) {
            return false;
        }
        //
        @Override
        public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
            return false;
        }
        //
        @Override
        public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
            return true;
        }
        //
        @Override
        public IAtomContainer applyPreprocessing(IAtomContainer aMolecule)
                throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
            return aMolecule;
        }
    }
    //
    /**
     * Real (non-mock) test-local {@link IMoleculeFragmenter} that reports every molecule as to-be-filtered, driving the
     * filter-skip branch of {@link FragmentationTask}. {@code fragmentMolecule} must never be reached, so it throws if
     * called.
     */
    private static final class FilteringFragmenter extends SuccessfulFragmenter {
        /**
         * Constructor (no state).
         */
        private FilteringFragmenter() {
        }
        //
        @Override
        public boolean shouldBeFiltered(IAtomContainer aMolecule) {
            return true;
        }
        //
        @Override
        public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
                throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
            throw new IllegalStateException("fragmentMolecule must not be called for a filtered molecule");
        }
    }
    //
    /**
     * Real (non-mock) test-local {@link IMoleculeFragmenter} that reports every molecule as needing preprocessing, driving
     * the preprocessing branch of {@link FragmentationTask}. The applied flag records that {@code applyPreprocessing} was
     * invoked; the fragmenter returns the (preprocessed) molecule as a single fragment.
     */
    private static final class PreprocessingFragmenter extends SuccessfulFragmenter {
        /**
         * Whether applyPreprocessing has been invoked.
         */
        private boolean preprocessingApplied;
        //
        /**
         * Constructor (no state besides the applied flag).
         */
        private PreprocessingFragmenter() {
            this.preprocessingApplied = false;
        }
        //
        /**
         * Returns whether applyPreprocessing has been invoked.
         *
         * @return true if applyPreprocessing was called at least once
         */
        private boolean wasPreprocessingApplied() {
            return this.preprocessingApplied;
        }
        //
        @Override
        public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
            return true;
        }
        //
        @Override
        public IAtomContainer applyPreprocessing(IAtomContainer aMolecule)
                throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
            this.preprocessingApplied = true;
            return aMolecule;
        }
    }
    //
    /**
     * Real (non-mock) test-local {@link IMoleculeFragmenter} whose {@link #fragmentMolecule(IAtomContainer)} throws an
     * {@link IllegalStateException} — a runtime exception not covered by the inner typed catch of
     * {@link FragmentationTask#call()} — so the failure propagates to the per-molecule generic catch branch.
     */
    private static final class GenericThrowingFragmenter extends SuccessfulFragmenter {
        /**
         * Constructor (no state).
         */
        private GenericThrowingFragmenter() {
        }
        //
        @Override
        public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
                throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
            throw new IllegalStateException("GenericThrowingFragmenter throws an exception outside the typed catch");
        }
    }
    //</editor-fold>
}
