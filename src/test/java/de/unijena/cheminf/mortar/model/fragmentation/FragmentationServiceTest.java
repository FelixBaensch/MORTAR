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
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ScaffoldGeneratorFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.SugarRemovalUtilityFragmenter;
import de.unijena.cheminf.mortar.model.util.AppDirTestUtil;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.TestUtil;

import javafx.beans.property.Property;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Direct, headless unit tests for {@link FragmentationService}. The service orchestrates single and pipeline
 * fragmentation and persists/reloads its settings. All fragmentation drives here are synchronous: the
 * {@code startSingleFragmentation}/{@code startPipelineFragmentation}/{@code startPipelineFragmentationMolByMol}
 * methods block (via {@code ExecutorService.invokeAll} + {@code Future.get}) and return with the fragment map fully
 * populated, so assertions are made directly after the call with no sleep, latch, or {@code Platform.runLater} wait.
 * The persist/reload round-trip is isolated to a JUnit {@link TempDir} by redirecting the {@code user.home} system
 * property and reflectively resetting the private static {@code appDirPath} cache of {@link FileUtil}, with the
 * original state always restored in a finally block, so the real {@code ~/MORTAR} directory is never touched. Most
 * drives use only real CDK objects and real fragmenters.
 * <p>
 * A small set of tests at the end of this class use {@link org.mockito.MockedStatic} to neutralize the static
 * {@code GuiUtil.guiExceptionAlert}/{@code GuiUtil.guiMessageAlert} calls in the service's error-handling branches.
 * Those alert calls construct a JavaFX {@code Alert}, which throws "Toolkit not initialized" when run headless, so the
 * catch bodies never complete without the static stub. Targeted Mockito is authorized for this package (the project's
 * no-mock default was extended to {@code model/fragmentation} to close the coverage gap on these GUI-bound error
 * branches); mocking is kept minimal and used only where a real object cannot drive the branch headless.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class FragmentationServiceTest {
    //<editor-fold desc="Locale setup and teardown" defaultstate="collapsed">
    /**
     * Default locale before this test class ran, restored after all tests.
     */
    private static Locale originalLocale;
    //
    /**
     * Sets the default locale to British English for this test class, remembering the original default locale, so
     * that the fragmenter settings tooltips and display names, which are resolved from the message bundle when a
     * fragmenter is instantiated, are deterministic.
     */
    @BeforeAll
    public static void setLocale() {
        FragmentationServiceTest.originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //
    /**
     * Restores the default locale that was in place before this test class ran.
     */
    @AfterAll
    public static void restoreLocale() {
        Locale.setDefault(FragmentationServiceTest.originalLocale);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor that bootstraps the Configuration singleton from the classpath (the service reads config; no data
     * directory is touched by this).
     *
     * @throws Exception if the Configuration singleton cannot be initialized
     */
    public FragmentationServiceTest() throws Exception {
        Configuration.getInstance();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Tests single-algorithm fragmentation: after {@code startSingleFragmentation} returns, the fragment map is
     * non-null and non-empty, every fragment has an absolute frequency {@literal >=} 1 and an absolute percentage in
     * (0.0, 1.0], and the current fragmentation name is set.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void singleFragmentationTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(TestUtil.buildMDM("c1ccccc1"));
        tmpMols.add(TestUtil.buildMDM("O=C(O)CCCC(=O)O"));
        tmpService.setSelectedFragmenter(FragmentationServiceTest.displayName(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME));
        tmpService.startSingleFragmentation(tmpMols, 1, false);
        Map<String, FragmentDataModel> tmpFragments = tmpService.getFragments();
        Assertions.assertNotNull(tmpFragments);
        Assertions.assertFalse(tmpFragments.isEmpty());
        for (FragmentDataModel tmpFragment : tmpFragments.values()) {
            Assertions.assertTrue(tmpFragment.getAbsoluteFrequency() >= 1);
            Assertions.assertTrue(tmpFragment.getAbsolutePercentage() > 0.0 && tmpFragment.getAbsolutePercentage() <= 1.0);
        }
        Assertions.assertNotNull(tmpService.getCurrentFragmentationName());
        Assertions.assertFalse(tmpService.getCurrentFragmentationName().isEmpty());
    }
    //
    /**
     * Tests the task-split logic of the service by driving four (molecule count, task count) combinations that exercise
     * the even-split, modulo (remainder {@literal >} 0) and clamp (more tasks than molecules) branches in the private
     * {@code startFragmentation} method. Every molecule carries exactly one carboxylic-acid group, so the most frequent
     * fragment of each drive must have an absolute and a molecule frequency equal to the number of molecules: a split
     * that drops or duplicates a molecule changes that count. Each multi-task result must also equal the single-task
     * result over the same molecules fragment by fragment (same fragment set, same absolute and molecule frequencies).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void multiTaskSplitTest() throws Exception {
        List<MoleculeDataModel> tmpFourMols = new ArrayList<>(4);
        tmpFourMols.add(TestUtil.buildMDM("O=C(O)CC"));
        tmpFourMols.add(TestUtil.buildMDM("O=C(O)CCC"));
        tmpFourMols.add(TestUtil.buildMDM("O=C(O)CCCC"));
        tmpFourMols.add(TestUtil.buildMDM("O=C(O)CCCCC"));
        //size=4, tasks=1 (single task)
        Map<String, FragmentDataModel> tmpFourSingleTask = this.runSingleFragmentation(tmpFourMols, 1);
        FragmentationServiceTest.assertMostFrequentFragmentCoversEveryMolecule(tmpFourSingleTask, 4);
        //size=4, tasks=2 (even split)
        Map<String, FragmentDataModel> tmpFourTwoTasks = this.runSingleFragmentation(tmpFourMols, 2);
        FragmentationServiceTest.assertMostFrequentFragmentCoversEveryMolecule(tmpFourTwoTasks, 4);
        FragmentationServiceTest.assertSameFragmentFrequencies(tmpFourSingleTask, tmpFourTwoTasks);
        //size=3, tasks=2 (modulo remainder > 0)
        List<MoleculeDataModel> tmpThreeMols = new ArrayList<>(tmpFourMols.subList(0, 3));
        Map<String, FragmentDataModel> tmpThreeTwoTasks = this.runSingleFragmentation(tmpThreeMols, 2);
        FragmentationServiceTest.assertMostFrequentFragmentCoversEveryMolecule(tmpThreeTwoTasks, 3);
        FragmentationServiceTest.assertSameFragmentFrequencies(this.runSingleFragmentation(tmpThreeMols, 1), tmpThreeTwoTasks);
        //size=2, tasks=4 (clamp: more tasks than molecules)
        List<MoleculeDataModel> tmpTwoMols = new ArrayList<>(tmpFourMols.subList(0, 2));
        Map<String, FragmentDataModel> tmpTwoFourTasks = this.runSingleFragmentation(tmpTwoMols, 4);
        FragmentationServiceTest.assertMostFrequentFragmentCoversEveryMolecule(tmpTwoFourTasks, 2);
        FragmentationServiceTest.assertSameFragmentFrequencies(this.runSingleFragmentation(tmpTwoMols, 1), tmpTwoFourTasks);
    }
    //
    /**
     * Tests the duplicate-fragment merge/aggregation path (the concurrency-sensitive part of the service) with two
     * tasks. A molecule set is built where two distinct molecules share a fragment (both carry a carboxylic-acid
     * functional group fragmented by the Ertl fragmenter). Invariants are asserted rather than a hard-coded fragment
     * SMILES key: the maximum-absolute-frequency fragment must have a molecule frequency equal to the number of
     * distinct parent molecules and an absolute frequency {@literal >=} 2, and the sum of absolute percentages across
     * all fragments must be approximately 1.0.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void mergeAggregationTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        //two different molecules that both bear a carboxylic acid group -> shared Ertl functional group fragment
        tmpMols.add(TestUtil.buildMDM("O=C(O)CCC"));
        tmpMols.add(TestUtil.buildMDM("O=C(O)CCCCCC"));
        //select the Ertl functional groups finder fragmenter (the default, first registered)
        tmpService.setSelectedFragmenter(FragmentationServiceTest.displayName(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME));
        tmpService.startSingleFragmentation(tmpMols, 2, false);
        Map<String, FragmentDataModel> tmpFragments = tmpService.getFragments();
        Assertions.assertNotNull(tmpFragments);
        Assertions.assertFalse(tmpFragments.isEmpty());
        FragmentDataModel tmpMaxFrequencyFragment = FragmentationServiceTest.findMaxAbsoluteFrequencyFragment(tmpFragments);
        Assertions.assertNotNull(tmpMaxFrequencyFragment);
        //the shared fragment appears in both molecules: molecule frequency == number of distinct parent molecules
        Assertions.assertEquals(tmpMaxFrequencyFragment.getParentMolecules().size(), tmpMaxFrequencyFragment.getMoleculeFrequency());
        Assertions.assertEquals(2, tmpMaxFrequencyFragment.getMoleculeFrequency());
        Assertions.assertTrue(tmpMaxFrequencyFragment.getAbsoluteFrequency() >= 2);
        //percentages across all fragments must sum to approximately 1.0
        double tmpPercentageSum = 0.0;
        for (FragmentDataModel tmpFragment : tmpFragments.values()) {
            tmpPercentageSum += tmpFragment.getAbsolutePercentage();
        }
        Assertions.assertEquals(1.0, tmpPercentageSum, 1e-9);
    }
    //
    /**
     * Tests pipeline fragmentation: a two-fragmenter pipeline (two independent copies of the default fragmenter) is
     * configured and {@code startPipelineFragmentation} is driven on a small molecule set; the resulting fragment map
     * is non-empty with valid frequency/percentage values and the current fragmentation name equals the configured
     * pipeline name. The deprecated {@code startPipelineFragmentationMolByMol} is then driven on a fresh service over
     * the same input and must complete without throwing.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void pipelineFragmentationTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                FragmentationServiceTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME),
                FragmentationServiceTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME)
        });
        String tmpPipelineName = "TestPipeline";
        tmpService.setPipeliningFragmentationName(tmpPipelineName);
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(TestUtil.buildMDM("O=C(O)CCC"));
        tmpMols.add(TestUtil.buildMDM("O=C(O)CCCCCC"));
        tmpService.startPipelineFragmentation(tmpMols, 1, false, false);
        Map<String, FragmentDataModel> tmpFragments = tmpService.getFragments();
        Assertions.assertNotNull(tmpFragments);
        Assertions.assertFalse(tmpFragments.isEmpty());
        for (FragmentDataModel tmpFragment : tmpFragments.values()) {
            Assertions.assertTrue(tmpFragment.getAbsoluteFrequency() >= 1);
            Assertions.assertTrue(tmpFragment.getAbsolutePercentage() > 0.0 && tmpFragment.getAbsolutePercentage() <= 1.0);
        }
        Assertions.assertEquals(tmpPipelineName, tmpService.getCurrentFragmentationName());
        //drive the deprecated mol-by-mol pipeline on a fresh service over the same input
        FragmentationService tmpMolByMolService = new FragmentationService();
        tmpMolByMolService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                FragmentationServiceTest.fragmenterCopy(tmpMolByMolService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME),
                FragmentationServiceTest.fragmenterCopy(tmpMolByMolService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME)
        });
        tmpMolByMolService.setPipeliningFragmentationName("MolByMolPipeline");
        List<MoleculeDataModel> tmpMolsForMolByMol = new ArrayList<>(2);
        tmpMolsForMolByMol.add(TestUtil.buildMDM("O=C(O)CCC"));
        tmpMolsForMolByMol.add(TestUtil.buildMDM("O=C(O)CCCCCC"));
        Assertions.assertDoesNotThrow(() -> tmpMolByMolService.startPipelineFragmentationMolByMol(tmpMolsForMolByMol, 1, false));
        Assertions.assertNotNull(tmpMolByMolService.getFragments());
    }
    //
    /**
     * Tests the trivial getters/setters and cache management methods: the accessors return/round-trip sensibly,
     * {@code createNewFragmenterObjectByAlgorithmName} returns a fragmenter for a valid algorithm name and throws an
     * IllegalArgumentException for a bogus name, running two single fragmentations exercises the duplicate-name append
     * branch in the private {@code createAndCheckFragmentationName}, and {@code clearCache} as well as the happy-path
     * {@code abortExecutor} do not throw.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void gettersAndCacheTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        Assertions.assertNotNull(tmpService.getFragmenters());
        //five registered fragmenters: ErtlFGF, SugarRemovalUtility, ScaffoldGenerator, MolWURCS, CDKCircular
        Assertions.assertEquals(5, tmpService.getFragmenters().length);
        Assertions.assertNotNull(tmpService.getSelectedFragmenter());
        Assertions.assertNotNull(tmpService.getPipelineFragmenter());
        Assertions.assertNotNull(tmpService.getPipeliningFragmentationName());
        Assertions.assertNotNull(tmpService.selectedFragmenterDisplayNameProperty());
        //setPipeliningFragmentationName round-trip
        tmpService.setPipeliningFragmentationName("CustomName");
        Assertions.assertEquals("CustomName", tmpService.getPipeliningFragmentationName());
        //setSelectedFragmenter + display-name property round-trip
        String tmpSecondDisplayName = FragmentationServiceTest.displayName(tmpService, SugarRemovalUtilityFragmenter.ALGORITHM_NAME);
        tmpService.setSelectedFragmenter(tmpSecondDisplayName);
        Assertions.assertEquals(SugarRemovalUtilityFragmenter.ALGORITHM_NAME,
                tmpService.getSelectedFragmenter().getFragmentationAlgorithmName());
        tmpService.setSelectedFragmenterDisplayName(tmpSecondDisplayName);
        Assertions.assertEquals(tmpSecondDisplayName, tmpService.getSelectedFragmenterDisplayName());
        //createNewFragmenterObjectByAlgorithmName: valid name returns a fragmenter
        String tmpValidAlgorithmName = ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME;
        IMoleculeFragmenter tmpNewFragmenter = tmpService.createNewFragmenterObjectByAlgorithmName(tmpValidAlgorithmName);
        Assertions.assertNotNull(tmpNewFragmenter);
        Assertions.assertEquals(tmpValidAlgorithmName, tmpNewFragmenter.getFragmentationAlgorithmName());
        //createNewFragmenterObjectByAlgorithmName: bogus name throws IllegalArgumentException
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> tmpService.createNewFragmenterObjectByAlgorithmName("this-is-not-a-real-algorithm-name"));
        //two single fragmentations -> duplicate-name append branch in createAndCheckFragmentationName
        tmpService.setSelectedFragmenter(FragmentationServiceTest.displayName(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME));
        List<MoleculeDataModel> tmpMols = new ArrayList<>(1);
        tmpMols.add(TestUtil.buildMDM("O=C(O)CCC"));
        tmpService.startSingleFragmentation(tmpMols, 1, false);
        String tmpFirstName = tmpService.getCurrentFragmentationName();
        List<MoleculeDataModel> tmpMols2 = new ArrayList<>(1);
        tmpMols2.add(TestUtil.buildMDM("O=C(O)CCCC"));
        tmpService.startSingleFragmentation(tmpMols2, 1, false);
        String tmpSecondName = tmpService.getCurrentFragmentationName();
        Assertions.assertNotEquals(tmpFirstName, tmpSecondName);
        //clearCache and happy-path abortExecutor do not throw
        Assertions.assertDoesNotThrow(tmpService::abortExecutor);
        Assertions.assertDoesNotThrow(tmpService::clearCache);
        Assertions.assertNull(tmpService.getFragments());
        Assertions.assertNull(tmpService.getCurrentFragmentationName());
    }
    //
    /**
     * Tests the settings persist/reload round-trip under {@link TempDir} isolation: the {@code user.home} system
     * property is redirected to a temporary directory and the private static {@code appDirPath} cache of
     * {@link FileUtil} is reflectively reset so that the settings directory resolves under the temporary directory. A
     * service mutates its selected fragmenter and pipeline name, persists them, and a fresh service reloads them; the
     * reloaded selected-fragmenter algorithm name and pipelining name must match. The original {@code user.home} and
     * the {@code appDirPath} cache are always restored and the log manager is reset in a finally block, so the real
     * {@code ~/MORTAR} directory is never touched and no logger handler leaks into sibling tests.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void persistAndReloadRoundTrip(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            FragmentationService tmpService = new FragmentationService();
            //mutate the selected fragmenter to a non-default one and set a pipeline + name
            String tmpSelectedDisplayName = FragmentationServiceTest.displayName(tmpService, SugarRemovalUtilityFragmenter.ALGORITHM_NAME);
            tmpService.setSelectedFragmenter(tmpSelectedDisplayName);
            tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                    FragmentationServiceTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME),
                    FragmentationServiceTest.fragmenterCopy(tmpService, SugarRemovalUtilityFragmenter.ALGORITHM_NAME)
            });
            tmpService.setPipeliningFragmentationName("PersistedPipeline");
            tmpService.persistFragmenterSettings();
            tmpService.persistSelectedFragmenterAndPipeline();
            //fresh service reloads from the temp-dir-isolated settings directory
            FragmentationService tmpReloaded = new FragmentationService();
            tmpReloaded.reloadFragmenterSettings();
            tmpReloaded.reloadActiveFragmenterAndPipeline();
            Assertions.assertEquals(tmpService.getSelectedFragmenter().getFragmentationAlgorithmName(),
                    tmpReloaded.getSelectedFragmenter().getFragmentationAlgorithmName());
            Assertions.assertEquals("PersistedPipeline", tmpReloaded.getPipeliningFragmentationName());
            //the reloaded pipeline must round-trip its size (number of fragmenters)
            Assertions.assertEquals(tmpService.getPipelineFragmenter().length, tmpReloaded.getPipelineFragmenter().length);
            for (int i = 0; i < tmpService.getPipelineFragmenter().length; i++) {
                Assertions.assertEquals(tmpService.getPipelineFragmenter()[i].getFragmentationAlgorithmName(),
                        tmpReloaded.getPipelineFragmenter()[i].getFragmentationAlgorithmName());
            }
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests that persisting the fragmenter settings and the selected-fragmenter/pipeline settings twice in a row
     * exercises the directory-already-exists branch (which deletes the previous files before re-writing) in both
     * {@code persistFragmenterSettings} and {@code persistSelectedFragmenterAndPipeline}. The round-trip is isolated to a
     * {@link TempDir} exactly as in {@link #persistAndReloadRoundTrip(Path)} so the real {@code ~/MORTAR} directory is
     * never touched. After the second persist a fresh service must still reload the selected fragmenter, the pipeline
     * name and the pipeline size unchanged.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void persistTwiceOverwritesExistingSettings(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            FragmentationService tmpService = new FragmentationService();
            tmpService.setSelectedFragmenter(FragmentationServiceTest.displayName(tmpService, SugarRemovalUtilityFragmenter.ALGORITHM_NAME));
            tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                    FragmentationServiceTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME),
                    FragmentationServiceTest.fragmenterCopy(tmpService, ScaffoldGeneratorFragmenter.ALGORITHM_NAME)
            });
            tmpService.setPipeliningFragmentationName("OverwritePipeline");
            //first persist creates the directories
            tmpService.persistFragmenterSettings();
            tmpService.persistSelectedFragmenterAndPipeline();
            //second persist hits the directory-already-exists / delete-then-rewrite branch
            Assertions.assertDoesNotThrow(tmpService::persistFragmenterSettings);
            Assertions.assertDoesNotThrow(tmpService::persistSelectedFragmenterAndPipeline);
            FragmentationService tmpReloaded = new FragmentationService();
            tmpReloaded.reloadFragmenterSettings();
            tmpReloaded.reloadActiveFragmenterAndPipeline();
            Assertions.assertEquals(tmpService.getSelectedFragmenter().getFragmentationAlgorithmName(),
                    tmpReloaded.getSelectedFragmenter().getFragmentationAlgorithmName());
            Assertions.assertEquals("OverwritePipeline", tmpReloaded.getPipeliningFragmentationName());
            Assertions.assertEquals(2, tmpReloaded.getPipelineFragmenter().length);
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests the pipeline-name normalisation branches of {@code persistSelectedFragmenterAndPipeline}: persisting with a
     * null pipeline name must fall back to the default pipeline name (the null/empty branch), and persisting with a
     * pipeline name that contains a character disallowed by the preference content pattern (a tab) must reset the name to
     * the default (the invalid-content branch). Both drives are isolated to a {@link TempDir}; after each the persisted
     * pipeline name reloaded by a fresh service must equal the default pipeline name.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void persistResetsNullAndInvalidPipelineName(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            //null pipeline name -> default-name fallback branch
            FragmentationService tmpNullNameService = new FragmentationService();
            tmpNullNameService.setPipeliningFragmentationName(null);
            Assertions.assertDoesNotThrow(tmpNullNameService::persistSelectedFragmenterAndPipeline);
            FragmentationService tmpReloadedFromNull = new FragmentationService();
            tmpReloadedFromNull.reloadActiveFragmenterAndPipeline();
            Assertions.assertEquals(FragmentationService.DEFAULT_PIPELINE_NAME, tmpReloadedFromNull.getPipeliningFragmentationName());
            //invalid pipeline name (contains a tab) -> invalid-content reset branch
            FragmentationService tmpInvalidNameService = new FragmentationService();
            tmpInvalidNameService.setPipeliningFragmentationName("Invalid\tName");
            Assertions.assertDoesNotThrow(tmpInvalidNameService::persistSelectedFragmenterAndPipeline);
            FragmentationService tmpReloadedFromInvalid = new FragmentationService();
            tmpReloadedFromInvalid.reloadActiveFragmenterAndPipeline();
            Assertions.assertEquals(FragmentationService.DEFAULT_PIPELINE_NAME, tmpReloadedFromInvalid.getPipeliningFragmentationName());
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests the no-persisted-settings reload branches: a fresh service is created against a temporary, empty fake home
     * (so no settings files exist) and both {@code reloadFragmenterSettings} and {@code reloadActiveFragmenterAndPipeline}
     * are driven. Neither must throw; the fragmenter settings remain in their defaults (the warning-only "no persisted
     * settings" branch) and the selected fragmenter / pipeline stay at their construction defaults (the "settings file
     * not found" branch). Isolated to a {@link TempDir} so the real {@code ~/MORTAR} directory is never touched.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void reloadWithoutPersistedSettingsKeepsDefaults(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            FragmentationService tmpService = new FragmentationService();
            String tmpDefaultSelected = tmpService.getSelectedFragmenter().getFragmentationAlgorithmName();
            String tmpDefaultPipelineName = tmpService.getPipeliningFragmentationName();
            int tmpDefaultPipelineSize = tmpService.getPipelineFragmenter().length;
            Assertions.assertDoesNotThrow(tmpService::reloadFragmenterSettings);
            Assertions.assertDoesNotThrow(tmpService::reloadActiveFragmenterAndPipeline);
            //nothing was persisted, so everything stays at the construction defaults
            Assertions.assertEquals(tmpDefaultSelected, tmpService.getSelectedFragmenter().getFragmentationAlgorithmName());
            Assertions.assertEquals(tmpDefaultPipelineName, tmpService.getPipeliningFragmentationName());
            Assertions.assertEquals(tmpDefaultPipelineSize, tmpService.getPipelineFragmenter().length);
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests the corrupt-fragmenter-settings-file branch of {@code reloadFragmenterSettings}: a garbage (non-compressed)
     * file is written under the {@link TempDir}-isolated fragmenter settings directory using the simple class name of the
     * first available fragmenter, so that constructing a {@code PreferenceContainer} from it throws and the reload logs a
     * warning and leaves that fragmenter in its default settings (the catch/continue branch). The reload must not throw
     * and the fragmenter must retain a valid (default) algorithm name.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void reloadWithCorruptFragmenterSettingsFile(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            FragmentationService tmpService = new FragmentationService();
            String tmpFragmenterSettingsDirPath = FileUtil.getSettingsDirPath()
                    + FragmentationService.FRAGMENTER_SETTINGS_SUBFOLDER_NAME + File.separator;
            File tmpFragmenterSettingsDir = new File(tmpFragmenterSettingsDirPath);
            Assertions.assertTrue(tmpFragmenterSettingsDir.exists() || tmpFragmenterSettingsDir.mkdirs());
            //write a garbage (non-compressed) settings file named after the Ertl fragmenter's simple class name
            IMoleculeFragmenter tmpErtlFragmenter = FragmentationServiceTest.fragmenter(
                    tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME);
            File tmpCorruptFile = new File(tmpFragmenterSettingsDirPath
                    + tmpErtlFragmenter.getClass().getSimpleName()
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            Files.writeString(tmpCorruptFile.toPath(), "this is not a valid preference container");
            Assertions.assertTrue(tmpCorruptFile.exists() && tmpCorruptFile.canRead());
            //reload must swallow the corrupt-file exception and leave the fragmenter at its defaults
            Assertions.assertDoesNotThrow(tmpService::reloadFragmenterSettings);
            IMoleculeFragmenter tmpPristineErtlFragmenter = FragmentationServiceTest.fragmenter(
                    new FragmentationService(), ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME);
            List<Property<?>> tmpReloadedSettings = tmpErtlFragmenter.settingsProperties();
            List<Property<?>> tmpPristineSettings = tmpPristineErtlFragmenter.settingsProperties();
            Assertions.assertEquals(tmpPristineSettings.size(), tmpReloadedSettings.size());
            for (int i = 0; i < tmpPristineSettings.size(); i++) {
                Assertions.assertEquals(tmpPristineSettings.get(i).getValue(), tmpReloadedSettings.get(i).getValue(),
                        "a corrupt settings file must leave setting '" + tmpPristineSettings.get(i).getName()
                                + "' at its default");
            }
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests the missing-pipeline-fragmenter-file branch of {@code reloadActiveFragmenterAndPipeline}: a two-fragmenter
     * pipeline is persisted under {@link TempDir} isolation, then one of the persisted pipeline-fragmenter files is
     * deleted before a fresh service reloads. The reload must not throw; the persisted pipeline size still declares two
     * fragmenters, but only the one whose file survived can be reconstructed, so the reloaded pipeline has exactly one
     * fragmenter. The selected fragmenter and pipeline name are still reloaded from the surviving service settings file.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void reloadWithMissingPipelineFragmenterFile(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            FragmentationService tmpService = new FragmentationService();
            tmpService.setSelectedFragmenter(FragmentationServiceTest.displayName(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME));
            tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                    FragmentationServiceTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME),
                    FragmentationServiceTest.fragmenterCopy(tmpService, SugarRemovalUtilityFragmenter.ALGORITHM_NAME)
            });
            tmpService.setPipeliningFragmentationName("MissingFilePipeline");
            tmpService.persistSelectedFragmenterAndPipeline();
            //delete the second persisted pipeline fragmenter file so its reload branch reports a missing file
            String tmpServiceSettingsDirPath = FileUtil.getSettingsDirPath()
                    + FragmentationService.FRAGMENTATION_SERVICE_SETTINGS_SUBFOLDER_NAME + File.separator;
            File tmpSecondFragmenterFile = new File(tmpServiceSettingsDirPath
                    + FragmentationService.PIPELINE_FRAGMENTER_FILE_NAME_PREFIX + 1
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            Assertions.assertTrue(tmpSecondFragmenterFile.delete());
            FragmentationService tmpReloaded = new FragmentationService();
            Assertions.assertDoesNotThrow(tmpReloaded::reloadActiveFragmenterAndPipeline);
            Assertions.assertEquals("MissingFilePipeline", tmpReloaded.getPipeliningFragmentationName());
            //only the surviving pipeline fragmenter file could be reconstructed
            Assertions.assertEquals(1, tmpReloaded.getPipelineFragmenter().length);
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests the zero-task normalisation and the empty-molecule-list early return: driving {@code startSingleFragmentation}
     * with {@code aNumberOfTasks == 0} must internally clamp the task count to one and still produce a non-empty fragment
     * map, while driving it over an empty molecule list (also with zero tasks) exercises the early-return branch of the
     * private {@code startFragmentation} and yields an empty (but non-null) fragment map.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void zeroTaskAndEmptyInputTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        tmpService.setSelectedFragmenter(FragmentationServiceTest.displayName(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME));
        List<MoleculeDataModel> tmpMols = new ArrayList<>(1);
        tmpMols.add(TestUtil.buildMDM("O=C(O)CCC"));
        //aNumberOfTasks == 0 -> normalised to 1
        tmpService.startSingleFragmentation(tmpMols, 0, false);
        Assertions.assertNotNull(tmpService.getFragments());
        Assertions.assertFalse(tmpService.getFragments().isEmpty());
        //empty molecule list -> early-return branch of startFragmentation, empty but non-null map
        FragmentationService tmpEmptyService = new FragmentationService();
        tmpEmptyService.setSelectedFragmenter(FragmentationServiceTest.displayName(tmpEmptyService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME));
        tmpEmptyService.startSingleFragmentation(new ArrayList<>(0), 0, false);
        Assertions.assertNotNull(tmpEmptyService.getFragments());
        Assertions.assertTrue(tmpEmptyService.getFragments().isEmpty());
    }
    //
    /**
     * Tests the task-loop modulo / final-task boundary branches of the private {@code startFragmentation} by driving a
     * single fragmentation over five molecules split across three parallel tasks (5 % 3 == 2, so the first two tasks get
     * an extra molecule and the final-task index clamp applies). Every molecule carries exactly one carboxylic-acid
     * group, so the most frequent fragment must be counted once per molecule (absolute and molecule frequency five),
     * and the three-task result must equal the single-task result over the same molecules fragment by fragment. A task
     * boundary that skips or repeats a molecule fails both checks.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void multiTaskModuloBoundaryTest() throws Exception {
        List<String> tmpSmilesList = List.of("O=C(O)CC", "O=C(O)CCC", "O=C(O)CCCC", "O=C(O)CCCCC", "O=C(O)CCCCCC");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(tmpSmilesList.size());
        for (String tmpSmiles : tmpSmilesList) {
            tmpMols.add(TestUtil.buildMDM(tmpSmiles));
        }
        Map<String, FragmentDataModel> tmpThreeTaskFragments = this.runSingleFragmentation(tmpMols, 3);
        FragmentationServiceTest.assertMostFrequentFragmentCoversEveryMolecule(tmpThreeTaskFragments, tmpSmilesList.size());
        FragmentationServiceTest.assertSameFragmentFrequencies(this.runSingleFragmentation(tmpMols, 1), tmpThreeTaskFragments);
    }
    //
    /**
     * Tests the zero-task normalisation and the default-pipeline-name fallback branches of
     * {@code startPipelineFragmentation}: a two-fragmenter pipeline is configured with the pipeline name explicitly set
     * to the empty string and driven with {@code aNumberOfTasks == 0}. The service must normalise the task count to one,
     * fall back to the default pipeline name, and still produce a non-empty fragment map; the current fragmentation name
     * must therefore start with the default pipeline name.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void pipelineZeroTaskAndDefaultNameTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                FragmentationServiceTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME),
                FragmentationServiceTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME)
        });
        //empty pipeline name -> default-name fallback branch
        tmpService.setPipeliningFragmentationName("");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(TestUtil.buildMDM("O=C(O)CCC"));
        tmpMols.add(TestUtil.buildMDM("O=C(O)CCCCCC"));
        //aNumberOfTasks == 0 -> normalised to 1
        tmpService.startPipelineFragmentation(tmpMols, 0, false, false);
        Assertions.assertNotNull(tmpService.getFragments());
        Assertions.assertFalse(tmpService.getFragments().isEmpty());
        Assertions.assertNotNull(tmpService.getCurrentFragmentationName());
        Assertions.assertTrue(tmpService.getCurrentFragmentationName().startsWith(FragmentationService.DEFAULT_PIPELINE_NAME));
    }
    //
    /**
     * Tests the {@code isKeepLastFragmentSetting} flag of {@code startPipelineFragmentation} with a pipeline of the Ertl
     * functional groups finder followed by the Scaffold Generator. The Ertl stage splits each molecule into functional
     * groups and alkane remnants; the Scaffold stage re-fragments only the ring-bearing remnants and yields nothing for
     * the acyclic ones. With the flag {@code false} those acyclic stage-one fragments are dropped; with the flag
     * {@code true} they are kept. So the kept result must be a strict superset of the discarded one, and every fragment
     * only the kept run reports must be a fragment the Ertl stage produces on its own.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void pipelineKeepLastFragmentBranchTest() throws Exception {
        //non-aromatic input: the Scaffold Generator fails on un-kekulized aromatic input (see TestUtil.buildMDM)
        List<String> tmpSmilesList = List.of("O=C(O)CCC1CCCCC1", "OCCCC1CCC1", "O=C(O)CCCC");
        Map<String, FragmentDataModel> tmpKept = this.runErtlThenScaffoldPipeline(tmpSmilesList, true);
        Map<String, FragmentDataModel> tmpDiscarded = this.runErtlThenScaffoldPipeline(tmpSmilesList, false);
        Assertions.assertFalse(tmpDiscarded.isEmpty(), "the ring-bearing remnants must still yield scaffolds");
        Assertions.assertTrue(tmpKept.keySet().containsAll(tmpDiscarded.keySet()));
        Set<String> tmpOnlyKept = new HashSet<>(tmpKept.keySet());
        tmpOnlyKept.removeAll(tmpDiscarded.keySet());
        Assertions.assertFalse(tmpOnlyKept.isEmpty(),
                "keeping the last fragments must retain the stage-one fragments the Scaffold stage cannot re-fragment");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(tmpSmilesList.size());
        for (String tmpSmiles : tmpSmilesList) {
            tmpMols.add(TestUtil.buildMDM(tmpSmiles));
        }
        Set<String> tmpErtlOnlyFragments = FragmentationServiceTest.singleStageFragmentSmiles(
                ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME, tmpMols);
        Assertions.assertTrue(tmpErtlOnlyFragments.containsAll(tmpOnlyKept),
                "the additionally kept fragments must be stage-one (Ertl) fragments");
    }
    //
    /**
     * Tests the nested parent/child fragment-merge path of {@code startPipelineFragmentation} with a three-stage pipeline
     * (Sugar Removal Utility, then the Ertl functional groups finder, then the Sugar Removal Utility again) driven over
     * glycoside molecules. The later stages re-fragment the fragments of earlier stages, so a molecule's stored parent
     * fragments themselves carry child fragments, exercising the parent-has-children branch of the merge loop. The drive
     * must complete without throwing and produce a non-empty fragment map whose absolute frequencies and percentages are
     * valid. Because those range checks would also hold for a single-stage run, the distinct-fragment set is additionally
     * required to differ from the set the first stage alone produces over the same molecules (as in the mol-by-mol
     * sibling test below).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void pipelineThreeStageNestedFragmentsTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                FragmentationServiceTest.fragmenterCopy(tmpService, SugarRemovalUtilityFragmenter.ALGORITHM_NAME),
                FragmentationServiceTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME),
                FragmentationServiceTest.fragmenterCopy(tmpService, SugarRemovalUtilityFragmenter.ALGORITHM_NAME)
        });
        tmpService.setPipeliningFragmentationName("ThreeStagePipeline");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(TestUtil.buildMDM("OCC1OC(O)C(O)C(O)C1OC2OC(CO)C(O)C(O)C2OCCC(=O)O"));
        tmpMols.add(TestUtil.buildMDM("O=C(O)CCCCCCc1ccc(OC2OC(CO)C(O)C(O)C2O)cc1"));
        tmpService.startPipelineFragmentation(tmpMols, 1, false, false);
        Map<String, FragmentDataModel> tmpFragments = tmpService.getFragments();
        Assertions.assertNotNull(tmpFragments);
        Assertions.assertFalse(tmpFragments.isEmpty());
        for (FragmentDataModel tmpFragment : tmpFragments.values()) {
            Assertions.assertTrue(tmpFragment.getAbsoluteFrequency() >= 1);
            Assertions.assertTrue(tmpFragment.getAbsolutePercentage() > 0.0 && tmpFragment.getAbsolutePercentage() <= 1.0);
        }
        //discriminating cross-stage check: the three-stage result must differ from what stage one produces alone
        Set<String> tmpFirstStageOnlyFragments = FragmentationServiceTest.singleStageFragmentSmiles(
                SugarRemovalUtilityFragmenter.ALGORITHM_NAME, tmpMols);
        Assertions.assertFalse(tmpFirstStageOnlyFragments.isEmpty());
        Assertions.assertNotEquals(tmpFirstStageOnlyFragments, tmpFragments.keySet(),
                "the downstream stages must re-fragment the first stage's output");
    }
    //
    /**
     * Tests the nested mol-by-mol path with a three-stage pipeline (Sugar Removal Utility, Ertl functional groups finder,
     * Sugar Removal Utility) driven over glycoside molecules, so the deprecated {@code startPipelineFragmentationMolByMol}
     * re-fragments each molecule's stage fragments across two consecutive stage-loop iterations.
     * <p>
     * Asserting only that the drive does not throw would pass for a single-stage run as well, so the resulting
     * distinct-fragment set is additionally required to differ from the set the first stage alone produces over the
     * same molecules. That comparison is between two live results and never against a golden SMILES literal, so it
     * stays robust against CDK snapshot drift.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void molByMolThreeStageNestedFragmentsTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                FragmentationServiceTest.fragmenterCopy(tmpService, SugarRemovalUtilityFragmenter.ALGORITHM_NAME),
                FragmentationServiceTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME),
                FragmentationServiceTest.fragmenterCopy(tmpService, SugarRemovalUtilityFragmenter.ALGORITHM_NAME)
        });
        tmpService.setPipeliningFragmentationName("ThreeStageMolByMol");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(TestUtil.buildMDM("OCC1OC(O)C(O)C(O)C1OC2OC(CO)C(O)C(O)C2OCCC(=O)O"));
        tmpMols.add(TestUtil.buildMDM("O=C(O)CCCCCCc1ccc(OC2OC(CO)C(O)C(O)C2O)cc1"));
        Assertions.assertDoesNotThrow(() -> tmpService.startPipelineFragmentationMolByMol(tmpMols, 1, false));
        Assertions.assertNotNull(tmpService.getFragments());
        Assertions.assertFalse(tmpService.getFragments().isEmpty());
        Assertions.assertEquals("ThreeStageMolByMol", tmpService.getCurrentFragmentationName());
        //discriminating cross-stage check: the three-stage result must differ from what stage one produces alone
        Set<String> tmpFirstStageOnlyFragments = FragmentationServiceTest.singleStageFragmentSmiles(
                SugarRemovalUtilityFragmenter.ALGORITHM_NAME, tmpMols);
        Assertions.assertFalse(tmpFirstStageOnlyFragments.isEmpty());
        Assertions.assertNotEquals(tmpFirstStageOnlyFragments, tmpService.getFragments().keySet(),
                "the downstream stages must re-fragment the first stage's output");
    }
    //
    /**
     * Tests the zero-total-frequency branches: a Scaffold-Generator pipeline is driven over acyclic molecules, which
     * have no scaffold, so the sum of absolute frequencies is zero and the percentage-calculation step is skipped (the
     * warning-only branch in {@code startPipelineFragmentation}). A direct {@link FragmentationTask} run first proves
     * that the empty result is genuine: every molecule is counted as having produced no fragments, and none failed with
     * an exception or a SMILES-generation error. The pipeline drive must complete without throwing and leave a non-null,
     * empty fragment map.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void pipelineZeroFrequencyTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                FragmentationServiceTest.fragmenterCopy(tmpService, ScaffoldGeneratorFragmenter.ALGORITHM_NAME),
                FragmentationServiceTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME)
        });
        tmpService.setPipeliningFragmentationName("ZeroFrequencyPipeline");
        List<String> tmpSmilesList = List.of("CCCCCC", "O=C(O)CCCCO");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(tmpSmilesList.size());
        List<MoleculeDataModel> tmpPreconditionMols = new ArrayList<>(tmpSmilesList.size());
        for (String tmpSmiles : tmpSmilesList) {
            tmpMols.add(TestUtil.buildMDM(tmpSmiles));
            tmpPreconditionMols.add(TestUtil.buildMDM(tmpSmiles));
        }
        //precondition: the Scaffold Generator yields no fragments for these molecules because they have no rings, not
        //because fragmentation failed; a failure would leave the map empty just the same and hide a broken stage
        FragmentationTaskResult tmpPrecondition = new FragmentationTask(tmpPreconditionMols,
                FragmentationServiceTest.fragmenterCopy(tmpService, ScaffoldGeneratorFragmenter.ALGORITHM_NAME),
                new ConcurrentHashMap<>(), "ZeroFrequencyPrecondition", false).call();
        Assertions.assertEquals(tmpSmilesList.size(), tmpPrecondition.moleculeProducedNoFragmentsCount());
        Assertions.assertEquals(0, tmpPrecondition.exceptionsCount());
        Assertions.assertEquals(0, tmpPrecondition.unexpectedExceptionsCount());
        Assertions.assertEquals(0, tmpPrecondition.fragmentFailedSmilesGenerationCount());
        Assertions.assertDoesNotThrow(() -> tmpService.startPipelineFragmentation(tmpMols, 1, false, false));
        Assertions.assertNotNull(tmpService.getFragments());
        Assertions.assertTrue(tmpService.getFragments().isEmpty());
    }
    //
    /**
     * Tests the deprecated mol-by-mol pipeline over a genuine two-stage pipeline with two distinct fragmenters (the Sugar
     * Removal Utility followed by the Ertl functional groups finder), driving the {@code i == 1} stage loop body that
     * re-fragments each molecule's stage-one fragments. As in the three-stage test, the result is required to differ
     * from the first stage's own output so the assertion cannot be satisfied by a single-stage run, and the
     * empty-pipeline-name fallback branch is exercised by leaving the pipeline name empty — the fallback name is
     * asserted to be non-blank rather than merely non-null.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void molByMolTwoStagePipelineTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        //Sugar Removal Utility first then the Ertl functional groups finder so the i == 1 stage loop re-fragments the
        //stage-one fragments of each molecule
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                FragmentationServiceTest.fragmenterCopy(tmpService, SugarRemovalUtilityFragmenter.ALGORITHM_NAME),
                FragmentationServiceTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME)
        });
        //empty pipeline name -> default-name fallback branch in the mol-by-mol path
        tmpService.setPipeliningFragmentationName("");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(3);
        tmpMols.add(TestUtil.buildMDM("OCC1OC(O)C(O)C(O)C1OC2OC(CO)C(O)C(O)C2O"));
        tmpMols.add(TestUtil.buildMDM("O=C(O)CCCCCCc1ccccc1O"));
        tmpMols.add(TestUtil.buildMDM("OCC1OC(O)C(O)C(O)C1O"));
        //aNumberOfTasks == 0 -> normalised to 1
        Assertions.assertDoesNotThrow(() -> tmpService.startPipelineFragmentationMolByMol(tmpMols, 0, false));
        Assertions.assertNotNull(tmpService.getFragments());
        Assertions.assertFalse(tmpService.getFragments().isEmpty());
        //the empty pipeline name must have been replaced by the fallback, not merely stored as the empty string
        Assertions.assertNotNull(tmpService.getCurrentFragmentationName());
        Assertions.assertFalse(tmpService.getCurrentFragmentationName().isBlank());
        //discriminating cross-stage check, as in the three-stage test above
        Set<String> tmpFirstStageOnlyFragments = FragmentationServiceTest.singleStageFragmentSmiles(
                SugarRemovalUtilityFragmenter.ALGORITHM_NAME, tmpMols);
        Assertions.assertFalse(tmpFirstStageOnlyFragments.isEmpty());
        Assertions.assertNotEquals(tmpFirstStageOnlyFragments, tmpService.getFragments().keySet(),
                "the second stage must re-fragment the first stage's output");
    }
    //
    /**
     * Tests the directory-not-writable error branches of {@code persistFragmenterSettings} and
     * {@code persistSelectedFragmenterAndPipeline}: the application data directory is made read-only under {@link TempDir}
     * isolation so that the {@code mkdirs} of the settings subfolders fails, driving the {@code !canWrite() ||
     * !mkdirsSuccessful} alert branch in both persist methods. The static {@code GuiUtil} alert is neutralized with a
     * {@link MockedStatic} (it would otherwise throw "Toolkit not initialized" headless); the test asserts that the
     * warning alert is requested at least once and that neither persist method throws. The data directory is made writable
     * again in a finally block so {@link TempDir} cleanup succeeds.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void persistWithUnwritableDirectoryShowsAlert(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        File tmpAppDir = null;
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            FragmentationService tmpService = new FragmentationService();
            //resolve (and thereby create) the application data directory, then make it read-only so the settings
            //subfolder mkdirs fails
            tmpAppDir = new File(FileUtil.getAppDirPath());
            Assertions.assertTrue(tmpAppDir.exists());
            //Windows/NTFS ignores the POSIX write bit for directories and File.setWritable(false, false) returns
            //false there, so the guarded branch cannot be driven at all on that platform; skip instead of failing
            Assumptions.assumeTrue(tmpAppDir.setWritable(false, false),
                    "The application data directory could not be made non-writable (e.g. on Windows); "
                            + "cannot exercise the not-writable branch.");
            //skip the test if the filesystem ignores the read-only bit (e.g. running as root) so it does not give a
            //false pass
            Assumptions.assumeFalse(tmpAppDir.canWrite(),
                    "Application data directory is still writable; cannot drive the not-writable branch on this filesystem.");
            AtomicInteger tmpAlertCount = new AtomicInteger(0);
            try (MockedStatic<GuiUtil> tmpGuiUtilMock = Mockito.mockStatic(GuiUtil.class)) {
                tmpGuiUtilMock.when(() -> GuiUtil.guiMessageAlert(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                        .thenAnswer(anInvocation -> {
                            tmpAlertCount.incrementAndGet();
                            return Optional.empty();
                        });
                Assertions.assertDoesNotThrow(tmpService::persistFragmenterSettings);
                Assertions.assertDoesNotThrow(tmpService::persistSelectedFragmenterAndPipeline);
            }
            //both persist methods hit their not-writable alert branch
            Assertions.assertEquals(2, tmpAlertCount.get());
        } finally {
            if (tmpAppDir != null) {
                tmpAppDir.setWritable(true, false);
            }
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests the corrupt-service-settings-file catch branch of {@code reloadActiveFragmenterAndPipeline}: a garbage
     * (non-compressed) {@code FragmentationServiceSettings} file is written under {@link TempDir} isolation, so
     * constructing a {@code PreferenceContainer} from it throws and the reload runs its catch body (which logs a warning
     * and calls {@code GuiUtil.guiExceptionAlert}). The static {@code GuiUtil} alert is neutralized with a
     * {@link MockedStatic}; the test asserts the exception alert is requested exactly once and that the reload does not
     * throw.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void reloadWithCorruptServiceSettingsFileShowsAlert(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            FragmentationService tmpService = new FragmentationService();
            //write a garbage service settings file so PreferenceContainer construction throws on reload
            String tmpServiceSettingsDirPath = FileUtil.getSettingsDirPath()
                    + FragmentationService.FRAGMENTATION_SERVICE_SETTINGS_SUBFOLDER_NAME + File.separator;
            File tmpServiceSettingsDir = new File(tmpServiceSettingsDirPath);
            Assertions.assertTrue(tmpServiceSettingsDir.exists() || tmpServiceSettingsDir.mkdirs());
            File tmpCorruptServiceFile = new File(tmpServiceSettingsDirPath
                    + FragmentationService.FRAGMENTATION_SERVICE_SETTINGS_FILE_NAME
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            java.nio.file.Files.writeString(tmpCorruptServiceFile.toPath(), "this is not a valid preference container");
            Assertions.assertTrue(tmpCorruptServiceFile.exists() && tmpCorruptServiceFile.canRead());
            AtomicInteger tmpAlertCount = new AtomicInteger(0);
            try (MockedStatic<GuiUtil> tmpGuiUtilMock = Mockito.mockStatic(GuiUtil.class)) {
                tmpGuiUtilMock.when(() -> GuiUtil.guiExceptionAlert(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                        .thenAnswer(anInvocation -> {
                            tmpAlertCount.incrementAndGet();
                            return null;
                        });
                Assertions.assertDoesNotThrow(tmpService::reloadActiveFragmenterAndPipeline);
            }
            Assertions.assertEquals(1, tmpAlertCount.get());
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests the corrupt-pipeline-fragmenter-file catch branch of {@code reloadActiveFragmenterAndPipeline}: a valid
     * service settings file (declaring a two-fragmenter pipeline) is persisted, then one of the persisted pipeline
     * fragmenter files is overwritten with garbage, so reconstructing that pipeline fragmenter throws inside the per-file
     * try and the inner Exception catch body runs (logging a warning and calling {@code GuiUtil.guiExceptionAlert}). The
     * static {@code GuiUtil} alert is neutralized with a {@link MockedStatic}; the test asserts the exception alert is
     * requested at least once, the reload does not throw, and only the surviving fragmenter is reconstructed.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void reloadWithCorruptPipelineFragmenterFileShowsAlert(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            FragmentationService tmpService = new FragmentationService();
            tmpService.setSelectedFragmenter(FragmentationServiceTest.displayName(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME));
            tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                    FragmentationServiceTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME),
                    FragmentationServiceTest.fragmenterCopy(tmpService, SugarRemovalUtilityFragmenter.ALGORITHM_NAME)
            });
            tmpService.setPipeliningFragmentationName("CorruptPipelineFile");
            tmpService.persistSelectedFragmenterAndPipeline();
            //overwrite the second persisted pipeline fragmenter file with garbage so its reconstruction throws
            String tmpServiceSettingsDirPath = FileUtil.getSettingsDirPath()
                    + FragmentationService.FRAGMENTATION_SERVICE_SETTINGS_SUBFOLDER_NAME + File.separator;
            File tmpSecondFragmenterFile = new File(tmpServiceSettingsDirPath
                    + FragmentationService.PIPELINE_FRAGMENTER_FILE_NAME_PREFIX + 1
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            Assertions.assertTrue(tmpSecondFragmenterFile.exists());
            java.nio.file.Files.writeString(tmpSecondFragmenterFile.toPath(), "this is not a valid preference container");
            AtomicInteger tmpAlertCount = new AtomicInteger(0);
            FragmentationService tmpReloaded = new FragmentationService();
            try (MockedStatic<GuiUtil> tmpGuiUtilMock = Mockito.mockStatic(GuiUtil.class)) {
                tmpGuiUtilMock.when(() -> GuiUtil.guiExceptionAlert(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                        .thenAnswer(anInvocation -> {
                            tmpAlertCount.incrementAndGet();
                            return null;
                        });
                Assertions.assertDoesNotThrow(tmpReloaded::reloadActiveFragmenterAndPipeline);
            }
            Assertions.assertTrue(tmpAlertCount.get() >= 1);
            //only the first pipeline fragmenter file could be reconstructed
            Assertions.assertEquals(1, tmpReloaded.getPipelineFragmenter().length);
            Assertions.assertEquals("CorruptPipelineFile", tmpReloaded.getPipeliningFragmentationName());
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests the interrupted-thread branch of {@code abortExecutor}. {@code awaitTermination} only throws an
     * {@link InterruptedException} for a pool that has not terminated yet; a pool whose tasks have finished returns
     * {@code true} without looking at the interrupt flag. So a fragmentation is started on a background thread with a
     * test-local {@link BlockingFragmenter} pipeline stage that blocks inside {@code fragmentMolecule} until released, and
     * {@code abortExecutor} is called with the interrupt flag set only once that task is known to be running. The catch
     * body is the only place in {@code abortExecutor} that logs a warning carrying an {@link InterruptedException}, so
     * such a log record is asserted, together with the re-set interrupt flag and the interruption of the blocked worker by
     * the subsequent {@code shutdownNow}. The interrupt flag is cleared, the worker released and the log handler removed
     * in a finally block so sibling tests are unaffected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void abortExecutorInterruptedTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        BlockingFragmenter tmpBlockingFragmenter = new BlockingFragmenter();
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {tmpBlockingFragmenter});
        tmpService.setPipeliningFragmentationName("BlockingPipeline");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(1);
        tmpMols.add(TestUtil.buildMDM("O=C(O)CCC"));
        Thread tmpFragmentationThread = new Thread(() -> {
            try {
                tmpService.startPipelineFragmentation(tmpMols, 1, false, false);
            } catch (Exception anException) {
                //the aborted drive may end in an exception; only abortExecutor is under test here
            }
        });
        Logger tmpServiceLogger = Logger.getLogger(FragmentationService.class.getName());
        List<LogRecord> tmpRecords = new CopyOnWriteArrayList<>();
        Handler tmpHandler = new Handler() {
            @Override
            public void publish(LogRecord aRecord) {
                tmpRecords.add(aRecord);
            }
            @Override
            public void flush() {
                //nothing buffered
            }
            @Override
            public void close() {
                //nothing to release
            }
        };
        tmpServiceLogger.addHandler(tmpHandler);
        try {
            tmpFragmentationThread.start();
            Assertions.assertTrue(tmpBlockingFragmenter.started.await(30, TimeUnit.SECONDS),
                    "the blocking fragmentation task never started");
            //the pool now has a running task, so awaitTermination inside abortExecutor must throw InterruptedException
            Thread.currentThread().interrupt();
            Assertions.assertDoesNotThrow(tmpService::abortExecutor);
            //abortExecutor re-sets the interrupt flag in its catch body
            Assertions.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            //clear the interrupt status so subsequent tests are not affected
            Thread.interrupted();
            tmpBlockingFragmenter.release.countDown();
            tmpFragmentationThread.join(30000);
            tmpServiceLogger.removeHandler(tmpHandler);
        }
        Assertions.assertTrue(tmpRecords.stream().anyMatch(aRecord -> aRecord.getLevel() == Level.WARNING
                        && aRecord.getThrown() instanceof InterruptedException),
                "abortExecutor must log the InterruptedException from its catch body");
        //the catch body's shutdownNow interrupts the blocked worker
        Assertions.assertTrue(tmpBlockingFragmenter.wasInterrupted);
        Assertions.assertFalse(tmpFragmentationThread.isAlive());
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods" defaultstate="collapsed">
    /**
     * Runs a single-stage fragmentation of the given molecules with the named algorithm through a fresh service and
     * returns the resulting distinct-fragment unique-SMILES set (the fragment map is keyed by unique SMILES, so its key
     * set is exactly that). Used as the discriminating baseline for the multi-stage pipeline tests: comparing a live
     * single-stage result against a live multi-stage result proves the later stages did something, without pinning any
     * golden SMILES literal that the moving CDK snapshot could invalidate.
     *
     * @param anAlgorithmName algorithm name of the single fragmenter to drive
     * @param aMolecules the molecules to fragment; fresh copies are made so the caller's models keep their own results
     * @return the distinct-fragment unique-SMILES set of the single-stage run
     * @throws Exception if anything goes wrong while copying the molecules or driving the fragmentation
     */
    private static Set<String> singleStageFragmentSmiles(String anAlgorithmName, List<MoleculeDataModel> aMolecules)
            throws Exception {
        FragmentationService tmpSingleStageService = new FragmentationService();
        tmpSingleStageService.setSelectedFragmenter(
                FragmentationServiceTest.displayName(tmpSingleStageService, anAlgorithmName));
        List<MoleculeDataModel> tmpFreshMolecules = new ArrayList<>(aMolecules.size());
        for (MoleculeDataModel tmpMolecule : aMolecules) {
            tmpFreshMolecules.add(TestUtil.buildMDM(tmpMolecule.getUniqueSmiles()));
        }
        //synchronous-blocking: the map is fully populated on return
        tmpSingleStageService.startSingleFragmentation(tmpFreshMolecules, 1, false);
        return new HashSet<>(tmpSingleStageService.getFragments().keySet());
    }
    //
    /**
     * Returns the registered fragmenter with the given algorithm name. Looking the fragmenter up by name rather than by
     * its position in {@code getFragmenters()} keeps these tests pinned to the algorithm they mean to drive: the array
     * order is an implementation detail of the {@code FragmentationService} constructor, so registering a further
     * algorithm would otherwise silently repoint every positional access.
     *
     * @param aService the service whose registered fragmenters are searched
     * @param anAlgorithmName the algorithm name to look up
     * @return the registered fragmenter carrying that algorithm name
     */
    private static IMoleculeFragmenter fragmenter(FragmentationService aService, String anAlgorithmName) {
        for (IMoleculeFragmenter tmpFragmenter : aService.getFragmenters()) {
            if (tmpFragmenter.getFragmentationAlgorithmName().equals(anAlgorithmName)) {
                return tmpFragmenter;
            }
        }
        throw new IllegalStateException("no fragmenter is registered under the algorithm name " + anAlgorithmName);
    }
    //
    /**
     * Returns a fresh copy of the registered fragmenter with the given algorithm name; see {@link
     * #fragmenter(FragmentationService, String)} for why the lookup is by name.
     *
     * @param aService the service whose registered fragmenters are searched
     * @param anAlgorithmName the algorithm name to look up
     * @return an independent copy of that fragmenter
     */
    private static IMoleculeFragmenter fragmenterCopy(FragmentationService aService, String anAlgorithmName) {
        return FragmentationServiceTest.fragmenter(aService, anAlgorithmName).copy();
    }
    //
    /**
     * Returns the display name of the registered fragmenter with the given algorithm name; see {@link
     * #fragmenter(FragmentationService, String)} for why the lookup is by name.
     *
     * @param aService the service whose registered fragmenters are searched
     * @param anAlgorithmName the algorithm name to look up
     * @return the display name of that fragmenter
     */
    private static String displayName(FragmentationService aService, String anAlgorithmName) {
        return FragmentationServiceTest.fragmenter(aService, anAlgorithmName).getFragmentationAlgorithmDisplayName();
    }
    //
    /**
     * Returns the fragment with the maximum absolute frequency from the given fragment map, or null if the map is
     * empty.
     *
     * @param aFragmentsMap map of unique SMILES to FragmentDataModel
     * @return the FragmentDataModel with the highest absolute frequency
     */
    private static FragmentDataModel findMaxAbsoluteFrequencyFragment(Map<String, FragmentDataModel> aFragmentsMap) {
        FragmentDataModel tmpMaxFragment = null;
        for (FragmentDataModel tmpFragment : aFragmentsMap.values()) {
            if (tmpMaxFragment == null || tmpFragment.getAbsoluteFrequency() > tmpMaxFragment.getAbsoluteFrequency()) {
                tmpMaxFragment = tmpFragment;
            }
        }
        return tmpMaxFragment;
    }
    //
    /**
     * Asserts that the most frequent fragment of the given map is counted exactly once per molecule: its absolute
     * frequency and its molecule frequency both equal the given number of molecules. Used with molecule sets in which
     * every molecule carries exactly one carboxylic-acid group, so a fragmentation that drops or double-counts a
     * molecule changes the count.
     *
     * @param aFragmentsMap map of unique SMILES to FragmentDataModel
     * @param aNumberOfMolecules the number of molecules that were fragmented
     */
    private static void assertMostFrequentFragmentCoversEveryMolecule(Map<String, FragmentDataModel> aFragmentsMap,
                                                                      int aNumberOfMolecules) {
        Assertions.assertNotNull(aFragmentsMap);
        Assertions.assertFalse(aFragmentsMap.isEmpty());
        FragmentDataModel tmpMaxFrequencyFragment = FragmentationServiceTest.findMaxAbsoluteFrequencyFragment(aFragmentsMap);
        Assertions.assertEquals(aNumberOfMolecules, tmpMaxFrequencyFragment.getAbsoluteFrequency());
        Assertions.assertEquals(aNumberOfMolecules, tmpMaxFrequencyFragment.getMoleculeFrequency());
    }
    //
    /**
     * Asserts that two fragment maps hold the same fragments with the same absolute and molecule frequencies.
     *
     * @param anExpected the reference fragment map
     * @param anActual the fragment map to compare
     */
    private static void assertSameFragmentFrequencies(Map<String, FragmentDataModel> anExpected,
                                                      Map<String, FragmentDataModel> anActual) {
        Assertions.assertEquals(anExpected.keySet(), anActual.keySet());
        for (Map.Entry<String, FragmentDataModel> tmpEntry : anExpected.entrySet()) {
            FragmentDataModel tmpActual = anActual.get(tmpEntry.getKey());
            Assertions.assertEquals(tmpEntry.getValue().getAbsoluteFrequency(), tmpActual.getAbsoluteFrequency(),
                    "absolute frequency of " + tmpEntry.getKey());
            Assertions.assertEquals(tmpEntry.getValue().getMoleculeFrequency(), tmpActual.getMoleculeFrequency(),
                    "molecule frequency of " + tmpEntry.getKey());
        }
    }
    //
    /**
     * Runs a single Ertl fragmentation on a fresh service over fresh copies of the given molecules with the given number
     * of tasks and returns the resulting fragment map.
     *
     * @param aListOfMolecules molecules to fragment
     * @param aNumberOfTasks number of parallel tasks
     * @return the resulting fragment map
     * @throws Exception if anything goes wrong
     */
    private Map<String, FragmentDataModel> runSingleFragmentation(List<MoleculeDataModel> aListOfMolecules,
                                                                  int aNumberOfTasks) throws Exception {
        FragmentationService tmpService = new FragmentationService();
        tmpService.setSelectedFragmenter(FragmentationServiceTest.displayName(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME));
        //fresh molecule data models per drive so previous fragmentation state does not interfere
        List<MoleculeDataModel> tmpFreshMols = new ArrayList<>(aListOfMolecules.size());
        for (MoleculeDataModel tmpMolecule : aListOfMolecules) {
            tmpFreshMols.add(TestUtil.buildMDM(tmpMolecule.getUniqueSmiles()));
        }
        tmpService.startSingleFragmentation(tmpFreshMols, aNumberOfTasks, false);
        Map<String, FragmentDataModel> tmpFragments = tmpService.getFragments();
        Assertions.assertNotNull(tmpFragments);
        return tmpFragments;
    }
    //
    /**
     * Runs a two-stage pipeline (the Ertl functional groups finder followed by the Scaffold Generator) on a fresh service
     * over the given molecules and returns the resulting fragment map. The keep-last-fragment flag is passed through so
     * both its branches can be exercised by the caller.
     *
     * @param aSmilesList SMILES strings of the molecules to fragment
     * @param isKeepLastFragment whether stage-one fragments that the next stage does not re-fragment are kept
     * @return the resulting fragment map
     * @throws Exception if anything goes wrong
     */
    private Map<String, FragmentDataModel> runErtlThenScaffoldPipeline(List<String> aSmilesList,
                                                                       boolean isKeepLastFragment) throws Exception {
        FragmentationService tmpService = new FragmentationService();
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                FragmentationServiceTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME),
                FragmentationServiceTest.fragmenterCopy(tmpService, ScaffoldGeneratorFragmenter.ALGORITHM_NAME)
        });
        tmpService.setPipeliningFragmentationName("ErtlThenScaffoldPipeline");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(aSmilesList.size());
        for (String tmpSmiles : aSmilesList) {
            tmpMols.add(TestUtil.buildMDM(tmpSmiles));
        }
        tmpService.startPipelineFragmentation(tmpMols, 1, false, isKeepLastFragment);
        Map<String, FragmentDataModel> tmpFragments = tmpService.getFragments();
        Assertions.assertNotNull(tmpFragments);
        return tmpFragments;
    }
    //
    //</editor-fold>
    //
    //<editor-fold desc="Test-local fragmenter" defaultstate="collapsed">
    /**
     * Real (non-mock) test-local {@link IMoleculeFragmenter} whose {@link #fragmentMolecule(IAtomContainer)} signals
     * that it has started and then blocks until it is released or its thread is interrupted, so a fragmentation task can
     * be held running while {@code abortExecutor} is called. {@link #copy()} returns this instance, so the copy the
     * service makes per task shares the latches and the interruption flag with the test.
     */
    private static final class BlockingFragmenter implements IMoleculeFragmenter {
        /**
         * Counted down once {@code fragmentMolecule} has been entered.
         */
        private final CountDownLatch started = new CountDownLatch(1);
        /**
         * Counted down by the test to release a blocked {@code fragmentMolecule} call.
         */
        private final CountDownLatch release = new CountDownLatch(1);
        /**
         * Whether the blocked {@code fragmentMolecule} call was interrupted.
         */
        private volatile boolean wasInterrupted = false;
        //
        @Override
        public List<Property<?>> settingsProperties() {
            return new ArrayList<>(0);
        }
        //
        @Override
        public Map<String, String> getSettingNameToTooltipTextMap() {
            return Map.of();
        }
        //
        @Override
        public Map<String, String> getSettingNameToDisplayNameMap() {
            return Map.of();
        }
        //
        @Override
        public String getFragmentationAlgorithmName() {
            return "BlockingFragmenter";
        }
        //
        @Override
        public String getFragmentationAlgorithmDisplayName() {
            return "Blocking Fragmenter";
        }
        //
        @Override
        public IMoleculeFragmenter copy() {
            return this;
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
            this.started.countDown();
            try {
                if (!this.release.await(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("BlockingFragmenter was neither released nor interrupted");
                }
            } catch (InterruptedException anException) {
                this.wasInterrupted = true;
                Thread.currentThread().interrupt();
            }
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
    //</editor-fold>
}
