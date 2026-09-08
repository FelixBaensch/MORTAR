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
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.util.AppDirTestUtil;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.TestUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor that sets the default locale to en-GB (so the fragmenter settings tooltips and display names, which
     * are resolved from the message.properties file during fragmenter instantiation, are deterministic) and bootstraps
     * the Configuration singleton from the classpath (the service reads config; no data directory is touched by this).
     *
     * @throws Exception if the Configuration singleton cannot be initialized
     */
    public FragmentationServiceTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
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
        tmpMols.add(FragmentationServiceTest.buildMDM("c1ccccc1"));
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCCC(=O)O"));
        tmpService.setSelectedFragmenter(tmpService.getFragmenters()[0].getFragmentationAlgorithmDisplayName());
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
     * {@code startFragmentation} method. Each drive must complete without throwing and populate a non-empty fragment
     * map, and the result must be shaped identically regardless of the task count (single task and two tasks over the
     * same four molecules yield the same number of distinct fragments).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void multiTaskSplitTest() throws Exception {
        List<MoleculeDataModel> tmpFourMols = new ArrayList<>(4);
        tmpFourMols.add(FragmentationServiceTest.buildMDM("O=C(O)CC"));
        tmpFourMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCC"));
        tmpFourMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCCC"));
        tmpFourMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCCCC"));
        //size=4, tasks=1 (single task)
        int tmpDistinctSingleTask = this.runSingleAndCountFragments(tmpFourMols, 1);
        Assertions.assertTrue(tmpDistinctSingleTask > 0);
        //size=4, tasks=2 (even split)
        int tmpDistinctTwoTasks = this.runSingleAndCountFragments(tmpFourMols, 2);
        Assertions.assertTrue(tmpDistinctTwoTasks > 0);
        //identical-shaped result regardless of task count
        Assertions.assertEquals(tmpDistinctSingleTask, tmpDistinctTwoTasks);
        //size=3, tasks=2 (modulo remainder > 0)
        List<MoleculeDataModel> tmpThreeMols = new ArrayList<>(tmpFourMols.subList(0, 3));
        Assertions.assertTrue(this.runSingleAndCountFragments(tmpThreeMols, 2) > 0);
        //size=2, tasks=4 (clamp: more tasks than molecules)
        List<MoleculeDataModel> tmpTwoMols = new ArrayList<>(tmpFourMols.subList(0, 2));
        Assertions.assertTrue(this.runSingleAndCountFragments(tmpTwoMols, 4) > 0);
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
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCC"));
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCCCCC"));
        //select the Ertl functional groups finder fragmenter (the default, first registered)
        tmpService.setSelectedFragmenter(tmpService.getFragmenters()[0].getFragmentationAlgorithmDisplayName());
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
                tmpService.getFragmenters()[0].copy(),
                tmpService.getFragmenters()[0].copy()
        });
        String tmpPipelineName = "TestPipeline";
        tmpService.setPipeliningFragmentationName(tmpPipelineName);
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCC"));
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCCCCC"));
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
                tmpMolByMolService.getFragmenters()[0].copy(),
                tmpMolByMolService.getFragmenters()[0].copy()
        });
        tmpMolByMolService.setPipeliningFragmentationName("MolByMolPipeline");
        List<MoleculeDataModel> tmpMolsForMolByMol = new ArrayList<>(2);
        tmpMolsForMolByMol.add(FragmentationServiceTest.buildMDM("O=C(O)CCC"));
        tmpMolsForMolByMol.add(FragmentationServiceTest.buildMDM("O=C(O)CCCCCC"));
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
        String tmpSecondDisplayName = tmpService.getFragmenters()[1].getFragmentationAlgorithmDisplayName();
        tmpService.setSelectedFragmenter(tmpSecondDisplayName);
        Assertions.assertEquals(tmpService.getFragmenters()[1].getFragmentationAlgorithmName(),
                tmpService.getSelectedFragmenter().getFragmentationAlgorithmName());
        tmpService.setSelectedFragmenterDisplayName(tmpSecondDisplayName);
        Assertions.assertEquals(tmpSecondDisplayName, tmpService.getSelectedFragmenterDisplayName());
        //createNewFragmenterObjectByAlgorithmName: valid name returns a fragmenter
        String tmpValidAlgorithmName = tmpService.getFragmenters()[0].getFragmentationAlgorithmName();
        IMoleculeFragmenter tmpNewFragmenter = tmpService.createNewFragmenterObjectByAlgorithmName(tmpValidAlgorithmName);
        Assertions.assertNotNull(tmpNewFragmenter);
        Assertions.assertEquals(tmpValidAlgorithmName, tmpNewFragmenter.getFragmentationAlgorithmName());
        //createNewFragmenterObjectByAlgorithmName: bogus name throws IllegalArgumentException
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> tmpService.createNewFragmenterObjectByAlgorithmName("this-is-not-a-real-algorithm-name"));
        //two single fragmentations -> duplicate-name append branch in createAndCheckFragmentationName
        tmpService.setSelectedFragmenter(tmpService.getFragmenters()[0].getFragmentationAlgorithmDisplayName());
        List<MoleculeDataModel> tmpMols = new ArrayList<>(1);
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCC"));
        tmpService.startSingleFragmentation(tmpMols, 1, false);
        String tmpFirstName = tmpService.getCurrentFragmentationName();
        List<MoleculeDataModel> tmpMols2 = new ArrayList<>(1);
        tmpMols2.add(FragmentationServiceTest.buildMDM("O=C(O)CCCC"));
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
            String tmpSelectedDisplayName = tmpService.getFragmenters()[1].getFragmentationAlgorithmDisplayName();
            tmpService.setSelectedFragmenter(tmpSelectedDisplayName);
            tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                    tmpService.getFragmenters()[0].copy(),
                    tmpService.getFragmenters()[1].copy()
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
            tmpService.setSelectedFragmenter(tmpService.getFragmenters()[1].getFragmentationAlgorithmDisplayName());
            tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                    tmpService.getFragmenters()[0].copy(),
                    tmpService.getFragmenters()[2].copy()
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
            //null pipeline name -> default-name fallback branch (693)
            FragmentationService tmpNullNameService = new FragmentationService();
            tmpNullNameService.setPipeliningFragmentationName(null);
            Assertions.assertDoesNotThrow(tmpNullNameService::persistSelectedFragmenterAndPipeline);
            FragmentationService tmpReloadedFromNull = new FragmentationService();
            tmpReloadedFromNull.reloadActiveFragmenterAndPipeline();
            Assertions.assertEquals(FragmentationService.DEFAULT_PIPELINE_NAME, tmpReloadedFromNull.getPipeliningFragmentationName());
            //invalid pipeline name (contains a tab) -> invalid-content reset branch (696-698)
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
            //write a garbage (non-compressed) settings file named after the first fragmenter's simple class name
            File tmpCorruptFile = new File(tmpFragmenterSettingsDirPath
                    + tmpService.getFragmenters()[0].getClass().getSimpleName()
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            java.nio.file.Files.writeString(tmpCorruptFile.toPath(), "this is not a valid preference container");
            Assertions.assertTrue(tmpCorruptFile.exists() && tmpCorruptFile.canRead());
            //reload must swallow the corrupt-file exception and leave the fragmenter in default
            Assertions.assertDoesNotThrow(tmpService::reloadFragmenterSettings);
            Assertions.assertNotNull(tmpService.getFragmenters()[0].getFragmentationAlgorithmName());
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
            tmpService.setSelectedFragmenter(tmpService.getFragmenters()[0].getFragmentationAlgorithmDisplayName());
            tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                    tmpService.getFragmenters()[0].copy(),
                    tmpService.getFragmenters()[1].copy()
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
        tmpService.setSelectedFragmenter(tmpService.getFragmenters()[0].getFragmentationAlgorithmDisplayName());
        List<MoleculeDataModel> tmpMols = new ArrayList<>(1);
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCC"));
        //aNumberOfTasks == 0 -> normalised to 1
        tmpService.startSingleFragmentation(tmpMols, 0, false);
        Assertions.assertNotNull(tmpService.getFragments());
        Assertions.assertFalse(tmpService.getFragments().isEmpty());
        //empty molecule list -> early-return branch of startFragmentation, empty but non-null map
        FragmentationService tmpEmptyService = new FragmentationService();
        tmpEmptyService.setSelectedFragmenter(tmpEmptyService.getFragmenters()[0].getFragmentationAlgorithmDisplayName());
        tmpEmptyService.startSingleFragmentation(new ArrayList<>(0), 0, false);
        Assertions.assertNotNull(tmpEmptyService.getFragments());
        Assertions.assertTrue(tmpEmptyService.getFragments().isEmpty());
    }
    //
    /**
     * Tests the task-loop modulo / final-task boundary branches of the private {@code startFragmentation} by driving a
     * single fragmentation over five molecules split across three parallel tasks (5 % 3 == 2, so the first two tasks get
     * an extra molecule and the final-task index clamp applies). The drive must complete without throwing and produce a
     * non-empty fragment map whose absolute percentages sum to approximately 1.0, and the result must be shaped
     * identically to the same five molecules fragmented in a single task.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void multiTaskModuloBoundaryTest() throws Exception {
        List<String> tmpSmilesList = List.of("O=C(O)CC", "O=C(O)CCC", "O=C(O)CCCC", "O=C(O)CCCCC", "O=C(O)CCCCCC");
        FragmentationService tmpThreeTaskService = new FragmentationService();
        tmpThreeTaskService.setSelectedFragmenter(tmpThreeTaskService.getFragmenters()[0].getFragmentationAlgorithmDisplayName());
        List<MoleculeDataModel> tmpMolsThreeTasks = new ArrayList<>(tmpSmilesList.size());
        for (String tmpSmiles : tmpSmilesList) {
            tmpMolsThreeTasks.add(FragmentationServiceTest.buildMDM(tmpSmiles));
        }
        tmpThreeTaskService.startSingleFragmentation(tmpMolsThreeTasks, 3, false);
        Map<String, FragmentDataModel> tmpThreeTaskFragments = tmpThreeTaskService.getFragments();
        Assertions.assertNotNull(tmpThreeTaskFragments);
        Assertions.assertFalse(tmpThreeTaskFragments.isEmpty());
        double tmpPercentageSum = 0.0;
        for (FragmentDataModel tmpFragment : tmpThreeTaskFragments.values()) {
            tmpPercentageSum += tmpFragment.getAbsolutePercentage();
        }
        Assertions.assertEquals(1.0, tmpPercentageSum, 1e-9);
        //single-task drive over the same molecules must yield the same number of distinct fragments
        FragmentationService tmpSingleTaskService = new FragmentationService();
        tmpSingleTaskService.setSelectedFragmenter(tmpSingleTaskService.getFragmenters()[0].getFragmentationAlgorithmDisplayName());
        List<MoleculeDataModel> tmpMolsSingleTask = new ArrayList<>(tmpSmilesList.size());
        for (String tmpSmiles : tmpSmilesList) {
            tmpMolsSingleTask.add(FragmentationServiceTest.buildMDM(tmpSmiles));
        }
        tmpSingleTaskService.startSingleFragmentation(tmpMolsSingleTask, 1, false);
        Assertions.assertEquals(tmpSingleTaskService.getFragments().size(), tmpThreeTaskFragments.size());
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
                tmpService.getFragmenters()[0].copy(),
                tmpService.getFragmenters()[0].copy()
        });
        //empty pipeline name -> default-name fallback branch
        tmpService.setPipeliningFragmentationName("");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCC"));
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCCCCC"));
        //aNumberOfTasks == 0 -> normalised to 1
        tmpService.startPipelineFragmentation(tmpMols, 0, false, false);
        Assertions.assertNotNull(tmpService.getFragments());
        Assertions.assertFalse(tmpService.getFragments().isEmpty());
        Assertions.assertNotNull(tmpService.getCurrentFragmentationName());
        Assertions.assertTrue(tmpService.getCurrentFragmentationName().startsWith(FragmentationService.DEFAULT_PIPELINE_NAME));
    }
    //
    /**
     * Tests the {@code isKeepLastFragmentSetting == true} branch of {@code startPipelineFragmentation}: the same
     * two-fragmenter pipeline is driven once with the keep-last-fragment flag {@code true} and once {@code false} over
     * equivalent molecule sets. Both drives must complete without throwing and produce a non-empty fragment map with
     * valid absolute frequencies and percentages; the keep-last-fragment drive must yield at least as many distinct
     * fragments as the discard drive (keeping last fragments can only retain, never drop, results).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void pipelineKeepLastFragmentBranchTest() throws Exception {
        int tmpKeptCount = this.runTwoStagePipelineAndCountFragments(true);
        int tmpDiscardedCount = this.runTwoStagePipelineAndCountFragments(false);
        Assertions.assertTrue(tmpKeptCount > 0);
        Assertions.assertTrue(tmpDiscardedCount > 0);
        Assertions.assertTrue(tmpKeptCount >= tmpDiscardedCount);
    }
    //
    /**
     * Tests the nested parent/child fragment-merge path of {@code startPipelineFragmentation} with a three-stage pipeline
     * (Sugar Removal Utility, then the Ertl functional groups finder, then the Sugar Removal Utility again) driven over
     * glycoside molecules. The later stages re-fragment the fragments of earlier stages, so a molecule's stored parent
     * fragments themselves carry child fragments, exercising the parent-has-children branch of the merge loop. The drive
     * must complete without throwing and produce a non-empty fragment map whose absolute frequencies and percentages are
     * valid and whose absolute percentages sum to approximately 1.0.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void pipelineThreeStageNestedFragmentsTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                tmpService.getFragmenters()[1].copy(),
                tmpService.getFragmenters()[0].copy(),
                tmpService.getFragmenters()[1].copy()
        });
        tmpService.setPipeliningFragmentationName("ThreeStagePipeline");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(FragmentationServiceTest.buildMDM("OCC1OC(O)C(O)C(O)C1OC2OC(CO)C(O)C(O)C2OCCC(=O)O"));
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCCCCCc1ccc(OC2OC(CO)C(O)C(O)C2O)cc1"));
        tmpService.startPipelineFragmentation(tmpMols, 1, false, false);
        Map<String, FragmentDataModel> tmpFragments = tmpService.getFragments();
        Assertions.assertNotNull(tmpFragments);
        Assertions.assertFalse(tmpFragments.isEmpty());
        double tmpPercentageSum = 0.0;
        for (FragmentDataModel tmpFragment : tmpFragments.values()) {
            Assertions.assertTrue(tmpFragment.getAbsoluteFrequency() >= 1);
            Assertions.assertTrue(tmpFragment.getAbsolutePercentage() > 0.0 && tmpFragment.getAbsolutePercentage() <= 1.0);
            tmpPercentageSum += tmpFragment.getAbsolutePercentage();
        }
        Assertions.assertEquals(1.0, tmpPercentageSum, 1e-9);
    }
    //
    /**
     * Tests the nested mol-by-mol path with a three-stage pipeline (Sugar Removal Utility, Ertl functional groups finder,
     * Sugar Removal Utility) driven over glycoside molecules, so the deprecated {@code startPipelineFragmentationMolByMol}
     * re-fragments each molecule's stage fragments across two consecutive stage-loop iterations. The drive must complete
     * without throwing and leave a non-null fragment map.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void molByMolThreeStageNestedFragmentsTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                tmpService.getFragmenters()[1].copy(),
                tmpService.getFragmenters()[0].copy(),
                tmpService.getFragmenters()[1].copy()
        });
        tmpService.setPipeliningFragmentationName("ThreeStageMolByMol");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(FragmentationServiceTest.buildMDM("OCC1OC(O)C(O)C(O)C1OC2OC(CO)C(O)C(O)C2OCCC(=O)O"));
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCCCCCc1ccc(OC2OC(CO)C(O)C(O)C2O)cc1"));
        Assertions.assertDoesNotThrow(() -> tmpService.startPipelineFragmentationMolByMol(tmpMols, 1, false));
        Assertions.assertNotNull(tmpService.getFragments());
    }
    //
    /**
     * Tests the zero-total-frequency branches: a Scaffold-Generator pipeline is driven over molecules that yield no
     * fragments, so the sum of absolute frequencies is zero and the percentage-calculation step is skipped (the
     * warning-only branch in {@code startPipelineFragmentation}). The drive must complete without throwing and leave a
     * non-null, empty fragment map.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void pipelineZeroFrequencyTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        //the Scaffold Generator (index 2) yields no fragments for these molecules, driving the zero-total-frequency branch
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                tmpService.getFragmenters()[2].copy(),
                tmpService.getFragmenters()[0].copy()
        });
        tmpService.setPipeliningFragmentationName("ZeroFrequencyPipeline");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(FragmentationServiceTest.buildMDM("c1ccc2c(c1)ccc3c2cccc3O"));
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCCCc1ccc(O)cc1"));
        Assertions.assertDoesNotThrow(() -> tmpService.startPipelineFragmentation(tmpMols, 1, false, false));
        Assertions.assertNotNull(tmpService.getFragments());
        Assertions.assertTrue(tmpService.getFragments().isEmpty());
    }
    //
    /**
     * Tests the deprecated mol-by-mol pipeline over a genuine two-stage pipeline with two distinct fragmenters (the Ertl
     * functional groups finder followed by the Sugar Removal Utility), driving the {@code i == 1} stage loop body that
     * re-fragments each molecule's stage-one fragments. The drive must complete without throwing, the resulting fragment
     * map must be non-null, and the empty-pipeline-name fallback branch is exercised by leaving the pipeline name empty.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void molByMolTwoStagePipelineTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        //Sugar Removal Utility first then the Ertl functional groups finder so the i == 1 stage loop re-fragments the
        //stage-one fragments of each molecule
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                tmpService.getFragmenters()[1].copy(),
                tmpService.getFragmenters()[0].copy()
        });
        //empty pipeline name -> default-name fallback branch in the mol-by-mol path
        tmpService.setPipeliningFragmentationName("");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(3);
        tmpMols.add(FragmentationServiceTest.buildMDM("OCC1OC(O)C(O)C(O)C1OC2OC(CO)C(O)C(O)C2O"));
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCCCCCc1ccccc1O"));
        tmpMols.add(FragmentationServiceTest.buildMDM("OCC1OC(O)C(O)C(O)C1O"));
        //aNumberOfTasks == 0 -> normalised to 1
        Assertions.assertDoesNotThrow(() -> tmpService.startPipelineFragmentationMolByMol(tmpMols, 0, false));
        Assertions.assertNotNull(tmpService.getFragments());
        Assertions.assertNotNull(tmpService.getCurrentFragmentationName());
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
                            return java.util.Optional.empty();
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
            tmpService.setSelectedFragmenter(tmpService.getFragmenters()[0].getFragmentationAlgorithmDisplayName());
            tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                    tmpService.getFragmenters()[0].copy(),
                    tmpService.getFragmenters()[1].copy()
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
     * Tests the interrupted-thread branch of {@code abortExecutor}: a single fragmentation is run first so the service's
     * internal {@code ExecutorService} is instantiated, then the current thread's interrupt flag is set so that the
     * {@code executorService.awaitTermination} call inside {@code abortExecutor} throws an {@link InterruptedException},
     * driving the catch body (which logs a warning, force-shuts down the executor, and re-sets the interrupt flag). The
     * interrupt flag is cleared in a finally block so sibling tests are unaffected. The method must not throw.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void abortExecutorInterruptedTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        tmpService.setSelectedFragmenter(tmpService.getFragmenters()[0].getFragmentationAlgorithmDisplayName());
        List<MoleculeDataModel> tmpMols = new ArrayList<>(1);
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCC"));
        //run a fragmentation so the internal executor service exists
        tmpService.startSingleFragmentation(tmpMols, 1, false);
        try {
            //set the interrupt flag so awaitTermination inside abortExecutor throws InterruptedException
            Thread.currentThread().interrupt();
            Assertions.assertDoesNotThrow(tmpService::abortExecutor);
            //abortExecutor re-sets the interrupt flag in its catch body
            Assertions.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            //clear the interrupt status so subsequent tests are not affected
            Thread.interrupted();
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods" defaultstate="collapsed">
    /**
     * Builds a MoleculeDataModel from a SMILES string using the verified (IAtomContainer, boolean) constructor.
     *
     * @param aSmiles SMILES string of the molecule
     * @return MoleculeDataModel for the given SMILES
     * @throws Exception if the SMILES cannot be parsed
     */
    private static MoleculeDataModel buildMDM(String aSmiles) throws Exception {
        IAtomContainer tmpAtomContainer = ChemUtil.parseSmilesToAtomContainer(aSmiles, false, false);
        return new MoleculeDataModel(tmpAtomContainer, false);
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
     * Runs a single fragmentation on a fresh service over the given molecules with the given number of tasks and
     * returns the number of distinct fragments produced.
     *
     * @param aListOfMolecules molecules to fragment
     * @param aNumberOfTasks number of parallel tasks
     * @return number of distinct fragments in the result map
     * @throws Exception if anything goes wrong
     */
    private int runSingleAndCountFragments(List<MoleculeDataModel> aListOfMolecules, int aNumberOfTasks) throws Exception {
        FragmentationService tmpService = new FragmentationService();
        tmpService.setSelectedFragmenter(tmpService.getFragmenters()[0].getFragmentationAlgorithmDisplayName());
        //fresh molecule data models per drive so previous fragmentation state does not interfere
        List<MoleculeDataModel> tmpFreshMols = new ArrayList<>(aListOfMolecules.size());
        for (MoleculeDataModel tmpMolecule : aListOfMolecules) {
            tmpFreshMols.add(FragmentationServiceTest.buildMDM(tmpMolecule.getUniqueSmiles()));
        }
        tmpService.startSingleFragmentation(tmpFreshMols, aNumberOfTasks, false);
        Map<String, FragmentDataModel> tmpFragments = tmpService.getFragments();
        Assertions.assertNotNull(tmpFragments);
        Assertions.assertFalse(tmpFragments.isEmpty());
        return tmpFragments.size();
    }
    //
    /**
     * Runs a genuine two-stage pipeline (the Ertl functional groups finder followed by the Sugar Removal Utility) on a
     * fresh service over a small molecule set and returns the number of distinct fragments produced. The keep-last-fragment
     * flag is passed through so both its branches can be exercised by the caller.
     *
     * @param isKeepLastFragment whether the last pipeline fragments should be kept when a molecule produces no new fragment
     * @return number of distinct fragments in the result map
     * @throws Exception if anything goes wrong
     */
    private int runTwoStagePipelineAndCountFragments(boolean isKeepLastFragment) throws Exception {
        FragmentationService tmpService = new FragmentationService();
        //Sugar Removal Utility first (splits sugar moieties from aglycone), then the Ertl functional groups finder
        //re-fragments those stage-one fragments, producing the nested parent/child fragment structure
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                tmpService.getFragmenters()[1].copy(),
                tmpService.getFragmenters()[0].copy()
        });
        tmpService.setPipeliningFragmentationName("TwoStagePipeline");
        List<MoleculeDataModel> tmpMols = new ArrayList<>(4);
        //a disaccharide and a glycoside both bear sugar moieties that the SRU stage splits, whose aglycone children the
        //Ertl stage fragments further, exercising the parent-has-children path of the pipeline merge
        tmpMols.add(FragmentationServiceTest.buildMDM("OCC1OC(O)C(O)C(O)C1OC2OC(CO)C(O)C(O)C2O"));
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCCCCCc1ccccc1O"));
        tmpMols.add(FragmentationServiceTest.buildMDM("OCC1OC(O)C(O)C(O)C1O"));
        tmpMols.add(FragmentationServiceTest.buildMDM("O=C(O)CCC"));
        tmpService.startPipelineFragmentation(tmpMols, 1, false, isKeepLastFragment);
        Map<String, FragmentDataModel> tmpFragments = tmpService.getFragments();
        Assertions.assertNotNull(tmpFragments);
        Assertions.assertFalse(tmpFragments.isEmpty());
        for (FragmentDataModel tmpFragment : tmpFragments.values()) {
            Assertions.assertTrue(tmpFragment.getAbsoluteFrequency() >= 1);
            Assertions.assertTrue(tmpFragment.getAbsolutePercentage() > 0.0 && tmpFragment.getAbsolutePercentage() <= 1.0);
        }
        return tmpFragments.size();
    }
    //
    //</editor-fold>
}
