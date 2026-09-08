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

package de.unijena.cheminf.mortar.integration;

import de.unijena.cheminf.mortar.configuration.Configuration;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.fragmentation.FragmentationService;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.SugarRemovalUtilityFragmenter;
import de.unijena.cheminf.mortar.model.util.ChemUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Integration test for requirement INT-02: a multi-algorithm pipeline driving
 * {@link FragmentationService#startPipelineFragmentation(List, int, boolean, boolean)} end-to-end with a
 * SugarRemovalUtility -&gt; ErtlFunctionalGroupsFinder chain (deglycosylate, then extract functional groups), exercising
 * the service orchestration (executor split, stage chaining, fragment merge/aggregation) that the direct-fragmenter
 * INT-01 test deliberately bypasses.
 * <p>
 * The pipeline drive is synchronous-blocking: although the service is backed by an {@code ExecutorService}, it joins via
 * {@code invokeAll} + {@code Future.get} and returns only after the fragment map is fully populated, so the assertions
 * run on the line immediately after the call with no latch, sleep, or {@code Platform.runLater} wait.
 * <p>
 * The accumulation across both stages is asserted strictly by invariant (no golden SMILES, no exact fragment counts):
 * the result map is non-empty, every produced {@link FragmentDataModel} has an absolute frequency {@literal >=} 1 and an
 * absolute percentage in (0.0, 1.0], the sum of all absolute percentages is approximately 1.0, and the current
 * fragmentation name matches the configured pipeline name. In addition, a discriminating cross-stage invariant proves
 * that the second stage actually operated on the first stage's output rather than the test passing for a single-stage
 * run: the same molecules are also fragmented with a SugarRemovalUtility-only single stage, and the two-stage pipeline's
 * distinct-fragment unique-SMILES set is asserted to differ from (i.e. not be identical to) the SugarRemovalUtility-only
 * set. Because the downstream ErtlFunctionalGroupsFinder re-fragments the deglycosylated aglycone produced by the first
 * stage, the chained result cannot be the same fragment set the first stage alone produces. This keeps the test robust
 * against CDK version drift (it compares two live results to each other, never to a golden literal) while genuinely
 * demonstrating accumulation across both stages rather than a normalization identity that any non-empty map satisfies.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class PipelineFragmentationTest {
    //<editor-fold desc="Private static final class constants" defaultstate="collapsed">
    /**
     * The sugar-bearing input molecules of this test: a disaccharide and an aromatic glycoside carrying a carboxylic
     * acid. Defined once here because both the pipeline drive and the single-stage baseline it is compared against must
     * fragment exactly the same molecules for that comparison to mean anything.
     */
    private static final String[] INPUT_SMILES = {
            "OCC1OC(O)C(O)C(O)C1OC2OC(CO)C(O)C(O)C2O",
            "O=C(O)CCCCCCc1ccc(OC2OC(CO)C(O)C(O)C2O)cc1"
    };
    //</editor-fold>
    //
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor that sets the default locale to en-GB (so the fragmenter settings tooltips and display names, which
     * are resolved from the message.properties file during fragmenter instantiation, are deterministic) and bootstraps
     * the Configuration singleton from the classpath. The configuration singleton is required here because the
     * fragmentation service reads config during the pipeline drive (unlike the direct-fragmenter INT-01 test).
     *
     * @throws Exception if the Configuration singleton cannot be initialized
     */
    public PipelineFragmentationTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
        Configuration.getInstance();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Tests" defaultstate="collapsed">
    /**
     * Drives a SugarRemovalUtility -&gt; ErtlFunctionalGroupsFinder pipeline (per decision D-08) through the
     * fragmentation service over two sugar-bearing molecules: a disaccharide and an aromatic glycoside carrying a
     * carboxylic acid. The first stage splits off the sugar moieties and the second stage re-fragments the resulting
     * aglycone, so the fragment map reflects both stages. The {@code startPipelineFragmentation} call blocks until the
     * executor tasks complete, so the assertions on the following lines observe a fully populated map without any wait
     * construct. All assertions are invariant-based per decision D-04.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void sugarRemovalToErtlPipelineAccumulationTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        //SugarRemovalUtility -> Ertl pipeline (D-08): stage 1 deglycosylates, stage 2 extracts functional groups.
        //The fragmenters are looked up by algorithm name rather than by their position in getFragmenters(), because
        //that order is an implementation detail of the FragmentationService constructor: registering a further
        //algorithm would silently repoint a positional access at a different fragmenter.
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                PipelineFragmentationTest.fragmenterCopy(tmpService, SugarRemovalUtilityFragmenter.ALGORITHM_NAME),
                PipelineFragmentationTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME)
        });
        tmpService.setPipeliningFragmentationName("INT02Pipeline");
        List<MoleculeDataModel> tmpMolecules = PipelineFragmentationTest.buildInputMolecules();
        //the call blocks (invokeAll + Future.get) and returns with the fragment map fully populated; assert immediately
        tmpService.startPipelineFragmentation(tmpMolecules, 1, false, false);
        Map<String, FragmentDataModel> tmpFragments = tmpService.getFragments();
        Assertions.assertNotNull(tmpFragments);
        Assertions.assertFalse(tmpFragments.isEmpty());
        double tmpAbsolutePercentageSum = 0.0;
        for (FragmentDataModel tmpFragment : tmpFragments.values()) {
            Assertions.assertTrue(tmpFragment.getAbsoluteFrequency() >= 1);
            Assertions.assertTrue(tmpFragment.getAbsolutePercentage() > 0.0);
            Assertions.assertTrue(tmpFragment.getAbsolutePercentage() <= 1.0);
            tmpAbsolutePercentageSum += tmpFragment.getAbsolutePercentage();
        }
        Assertions.assertEquals(1.0, tmpAbsolutePercentageSum, 1e-9);
        Assertions.assertEquals("INT02Pipeline", tmpService.getCurrentFragmentationName());
        //discriminating cross-stage invariant: the percentage-sum check above is a normalization identity that any
        //non-empty fragment map satisfies (a single-stage run would pass it identically), so it does not by itself
        //prove cross-stage accumulation. To prove the downstream Ertl stage actually re-fragmented the first stage's
        //SugarRemovalUtility output, run a SugarRemovalUtility-ONLY single stage over the very same molecules and assert
        //the two-stage pipeline's distinct-fragment set is not identical to the single-stage set. The fragment map is
        //keyed by unique SMILES, so its key set is the distinct-fragment unique-SMILES set; comparing two live results
        //to each other (never to a golden literal) keeps the check CDK-drift robust while genuinely discriminating a
        //two-stage pipeline from a single SugarRemovalUtility stage.
        Set<String> tmpPipelineFragmentSmilesSet = tmpFragments.keySet();
        Set<String> tmpSugarRemovalOnlyFragmentSmilesSet = PipelineFragmentationTest.fragmentSmilesSetForSingleStage(
                PipelineFragmentationTest.fragmenterCopy(tmpService, SugarRemovalUtilityFragmenter.ALGORITHM_NAME));
        Assertions.assertFalse(tmpSugarRemovalOnlyFragmentSmilesSet.isEmpty());
        Assertions.assertNotEquals(tmpSugarRemovalOnlyFragmentSmilesSet, tmpPipelineFragmentSmilesSet);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private static methods" defaultstate="collapsed">
    /**
     * Builds a fresh molecule data model for every entry of {@link #INPUT_SMILES}. Both the pipeline drive and the
     * single-stage baseline call this, so the two runs are guaranteed to see the same input molecules while each gets
     * its own models.
     *
     * @return the input molecules of this test, as fresh models
     * @throws Exception if a SMILES cannot be parsed
     */
    private static List<MoleculeDataModel> buildInputMolecules() throws Exception {
        List<MoleculeDataModel> tmpMolecules = new ArrayList<>(PipelineFragmentationTest.INPUT_SMILES.length);
        for (String tmpSmiles : PipelineFragmentationTest.INPUT_SMILES) {
            tmpMolecules.add(PipelineFragmentationTest.buildMDM(tmpSmiles));
        }
        return tmpMolecules;
    }
    //
    /**
     * Returns a fresh copy of the registered fragmenter carrying the given algorithm name.
     *
     * @param aService the service whose registered fragmenters are searched
     * @param anAlgorithmName the algorithm name to look up
     * @return an independent copy of that fragmenter
     */
    private static IMoleculeFragmenter fragmenterCopy(FragmentationService aService, String anAlgorithmName) {
        for (IMoleculeFragmenter tmpFragmenter : aService.getFragmenters()) {
            if (tmpFragmenter.getFragmentationAlgorithmName().equals(anAlgorithmName)) {
                return tmpFragmenter.copy();
            }
        }
        throw new IllegalStateException("no fragmenter is registered under the algorithm name " + anAlgorithmName);
    }
    //
    /**
     * Builds a molecule data model from a SMILES string by parsing it into an atom container via the project-standard
     * parse entry point (kekulize=false, perceive=false, matching the production import path) and wrapping it in a
     * non-saturated molecule data model.
     *
     * @param aSmiles the SMILES string to parse
     * @return a molecule data model wrapping the parsed atom container
     * @throws Exception if the SMILES cannot be parsed
     */
    private static MoleculeDataModel buildMDM(String aSmiles) throws Exception {
        IAtomContainer tmpAtomContainer = ChemUtil.parseSmilesToAtomContainer(aSmiles, false, false);
        return new MoleculeDataModel(tmpAtomContainer, false);
    }
    //
    /**
     * Runs a single-stage (single-algorithm) fragmentation of the two pipeline input molecules through a fresh
     * fragmentation service using the supplied fragmenter, and returns the resulting distinct-fragment unique-SMILES
     * set. The fragment map is keyed by unique SMILES, so its key set is exactly the distinct-fragment unique-SMILES
     * set. This is used by the discriminating cross-stage invariant to compare a single-stage result against the
     * two-stage pipeline result without ever comparing to a golden literal (so it remains robust against CDK drift). The
     * drive is synchronous-blocking (the service joins via {@code invokeAll} + {@code Future.get}), so the fragment map
     * is fully populated when the call returns and a defensive copy of the key set is taken before it could be reused.
     *
     * @param aFragmenter the single fragmenter to drive (a fresh copy independent of the pipeline fragmenters)
     * @return the distinct-fragment unique-SMILES set produced by the single-stage fragmentation over the two pipeline
     *         input molecules
     * @throws Exception if anything goes wrong while building the molecules or driving the fragmentation
     */
    private static Set<String> fragmentSmilesSetForSingleStage(IMoleculeFragmenter aFragmenter) throws Exception {
        FragmentationService tmpSingleStageService = new FragmentationService();
        tmpSingleStageService.setSelectedFragmenter(aFragmenter.getFragmentationAlgorithmDisplayName());
        List<MoleculeDataModel> tmpMolecules = PipelineFragmentationTest.buildInputMolecules();
        //synchronous-blocking: the map is fully populated on return
        tmpSingleStageService.startSingleFragmentation(tmpMolecules, 1, false);
        return new HashSet<>(tmpSingleStageService.getFragments().keySet());
    }
    //</editor-fold>
}
