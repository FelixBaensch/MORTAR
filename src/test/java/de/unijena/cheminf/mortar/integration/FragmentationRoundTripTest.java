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

import de.unijena.cheminf.mortar.model.fragmentation.algorithm.CDKCircularFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.MolWURCSFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ScaffoldGeneratorFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.SugarRemovalUtilityFragmenter;
import de.unijena.cheminf.mortar.model.util.ChemUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;
import java.util.Locale;

/**
 * Integration test for requirement INT-01: an end-to-end parse -&gt; fragment -&gt; unique-SMILES round-trip for each of the
 * five fragmentation algorithms (ErtlFunctionalGroupsFinder, SugarRemovalUtility, ScaffoldGenerator, MolWURCS,
 * CDKCircularFragmenter) driven directly through the
 * {@link IMoleculeFragmenter#fragmentMolecule(IAtomContainer)} contract against at least two representative molecules
 * each.
 * <p>
 * The assertions are strictly invariant-based (no golden SMILES, no exact fragment counts): every produced fragment must
 * carry the fragment-category property, must regenerate a non-null canonical unique SMILES via
 * {@link ChemUtil#createUniqueSmiles(IAtomContainer, boolean)}, must re-parse to a valid atom container, and that
 * re-parsed container must regenerate the very same unique SMILES (round-trip identity). This keeps the test robust
 * against CDK version drift while still proving the parse + fragment + identity wiring works end-to-end.
 *
 * @author Felix Baensch, Jonas Schaub
 * @version 1.0.0.0
 */
public class FragmentationRoundTripTest {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor that sets the default locale to British English, which is important for the correct functioning of
     * the fragmenters because their settings tooltips and display names are imported from the message.properties file.
     * No configuration singleton is touched here because INT-01 drives the fragmenters directly, bypassing the
     * fragmentation service and configuration layer.
     *
     * @throws Exception if anything goes wrong
     */
    public FragmentationRoundTripTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //</editor-fold>
    //
    //<editor-fold desc="Tests" defaultstate="collapsed">
    /**
     * Drives the {@link ErtlFunctionalGroupsFinderFragmenter} over two functional-group-bearing molecules and asserts
     * the per-fragment round-trip invariant chain for each. Both molecules carry a carboxylic-acid functional group so
     * the Ertl algorithm extracts a non-empty fragment set under default settings.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void ertlRoundTripTest() throws Exception {
        ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
        String[] tmpSmilesArray = {"O=C(O)CCC", "O=C(O)CCCCCC"};
        for (String tmpSmiles : tmpSmilesArray) {
            IAtomContainer tmpMolecule = ChemUtil.parseSmilesToAtomContainer(tmpSmiles, false, false);
            Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpMolecule));
            Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpMolecule));
            Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpMolecule));
            List<IAtomContainer> tmpFragments = tmpFragmenter.fragmentMolecule(tmpMolecule);
            Assertions.assertFalse(tmpFragments.isEmpty());
            this.assertFragmentsRoundTrip(tmpFragments, true);
        }
    }

    /**
     * Drives the {@link SugarRemovalUtilityFragmenter} over a disaccharide and an aromatic glycoside and asserts the
     * per-fragment round-trip invariant chain for each. The fragmenter runs on its default settings: those already
     * return all fragments, and neither test molecule carries a non-terminal sugar moiety, so nothing has to be
     * reconfigured for the sugar-bearing molecules to split into a non-empty fragment set.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void sugarRemovalUtilityRoundTripTest() throws Exception {
        SugarRemovalUtilityFragmenter tmpFragmenter = new SugarRemovalUtilityFragmenter();
        String[] tmpSmilesArray = {
                "OCC1OC(O)C(O)C(O)C1OC2OC(CO)C(O)C(O)C2O",
                "O=C(O)CCCCCCc1ccc(OC2OC(CO)C(O)C(O)C2O)cc1"};
        for (String tmpSmiles : tmpSmilesArray) {
            IAtomContainer tmpMolecule = ChemUtil.parseSmilesToAtomContainer(tmpSmiles, false, false);
            Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpMolecule));
            Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpMolecule));
            Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpMolecule));
            List<IAtomContainer> tmpFragments = tmpFragmenter.fragmentMolecule(tmpMolecule);
            Assertions.assertFalse(tmpFragments.isEmpty());
            this.assertFragmentsRoundTrip(tmpFragments, true);
        }
    }

    /**
     * Drives the {@link ScaffoldGeneratorFragmenter} over two fused-ring scaffolds and asserts the per-fragment
     * round-trip invariant chain for each. The molecules are parsed with kekulization enabled because the scaffold
     * generator requires explicit (kekulized) bond orders on aromatic ring systems and rejects un-kekulized aromatic
     * input with an {@link IllegalArgumentException}. The first molecule is the verified substituted-tetralin fused
     * bicyclic carrying side chains; the second is the verified COCONUT triterpenoid CNP0219624.2, a strongly fused
     * tetracyclic scaffold chosen deliberately over the phenanthrenol that {@code FragmentationServiceTest}'s
     * zero-frequency test uses to demonstrate an empty pipeline result, so this test does not rest on a molecule whose
     * non-emptiness is a known drift risk. Non-emptiness is additionally asserted explicitly per molecule with a clear
     * message, so a future cdk-scaffold drift that flips either molecule to zero fragments fails loudly and diagnosably
     * rather than silently weakening the round-trip coverage.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void scaffoldGeneratorRoundTripTest() throws Exception {
        ScaffoldGeneratorFragmenter tmpFragmenter = new ScaffoldGeneratorFragmenter();
        String[] tmpSmilesArray = {
                "CCc1ccc2c(c1)CCCC2C(=O)O",
                "C/C(=C\\C[C@@H]([C@@H](C)[C@H]1CC[C@@]2(C)C3=CC[C@H]4C(C)(C)[C@@H](CC[C@]4(C)C3=CC[C@]12C)O)OC(=O)C)"
                        + "/C(=O)O"};
        for (String tmpSmiles : tmpSmilesArray) {
            //kekulize=true: the scaffold generator rejects un-kekulized aromatic systems with IllegalArgumentException
            IAtomContainer tmpMolecule = ChemUtil.parseSmilesToAtomContainer(tmpSmiles, true, false);
            Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpMolecule));
            Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpMolecule));
            Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpMolecule));
            List<IAtomContainer> tmpFragments = tmpFragmenter.fragmentMolecule(tmpMolecule);
            //explicit, clearly messaged non-emptiness guard: ScaffoldGenerator can legitimately return zero fragments
            //for unsuitable input, so a future cdk-scaffold drift must fail here loudly rather than silently
            Assertions.assertFalse(tmpFragments.isEmpty(),
                    "ScaffoldGenerator yielded zero fragments for a molecule expected to fragment non-trivially: "
                            + tmpSmiles);
            this.assertFragmentsRoundTrip(tmpFragments, true);
        }
    }

    /**
     * Drives the {@link MolWURCSFragmenter} over two verified-fragmentable glycans and asserts the per-fragment
     * round-trip invariant chain for each. Every drive is guarded by {@code canBeFragmented} first, because many
     * carbohydrate inputs are silently not detected as fragmentable or throw on WURCS retranslation; the two glycans
     * used here are known-good carbohydrate structures that fragment non-trivially.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void molWurcsRoundTripTest() throws Exception {
        MolWURCSFragmenter tmpFragmenter = new MolWURCSFragmenter();
        String[] tmpSmilesArray = {
                "CCCCCC(CCCCCCCCCC(=O)O)O[C@H]1[C@@H]([C@H]([C@@H]([C@@H](C)O1)O)O)O[C@@H]2C[C@H](CO)[C@@H](C)"
                        + "[C@@H]([C@H]2O[C@H]3[C@@H]([C@@H]([C@H]([C@H](C)O3)O[C@H]4[C@@H]([C@H]([C@@H]([C@@H](C)O4)"
                        + "O)O)O)O)O)O",
                "CCCCCCCCCCCC(=O)O[C@@H]1[C@@H]([C@H]([C@H](C)O[C@H]1O[C@H]2[C@H](C)O[C@H]([C@@H]([C@@H]2O)O)"
                        + "O[C@@H]3[C@H]([C@H]([C@@H](C)O[C@H]3O[C@@H](CCCCC)CCCCCCCCCC(=O)OC)O)O)O[C@H]4[C@@H]"
                        + "([C@@H]([C@H]([C@H](C)O4)O)O)O)O[C@H]5[C@@H]([C@@H]([C@H]([C@H](C)O5)O)O)O"};
        for (String tmpSmiles : tmpSmilesArray) {
            IAtomContainer tmpMolecule = ChemUtil.parseSmilesToAtomContainer(tmpSmiles, false, false);
            Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpMolecule));
            Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpMolecule));
            //MolWURCS requires a detectable carbohydrate; guard every drive with canBeFragmented per the algorithm's contract
            Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpMolecule));
            List<IAtomContainer> tmpFragments = tmpFragmenter.fragmentMolecule(tmpMolecule);
            Assertions.assertFalse(tmpFragments.isEmpty());
            //MolWURCS does not tag its fragments with the fragment-category property, so it is not asserted here
            this.assertFragmentsRoundTrip(tmpFragments, false);
        }
    }
    /**
     * Drives the {@link CDKCircularFragmenter} over an aliphatic fatty acid and a monosaccharide and asserts the
     * per-fragment round-trip invariant chain for each. Both molecules are free of aromatic ring systems on purpose:
     * a circular fragment cut out of an aromatic ring is a ring fragment that is no longer closed, which the unique
     * SMILES generator can only express by falling back to Kekulé output, so aromatic input would test the fallback
     * rather than the round-trip. The fragmenter runs on its default settings (radius 3, smaller radii not included,
     * no aromaticity detection).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void cdkCircularRoundTripTest() throws Exception {
        CDKCircularFragmenter tmpFragmenter = new CDKCircularFragmenter();
        String[] tmpSmilesArray = {"O=C(O)CCCCCC", "OCC1OC(O)C(O)C(O)C1O"};
        for (String tmpSmiles : tmpSmilesArray) {
            IAtomContainer tmpMolecule = ChemUtil.parseSmilesToAtomContainer(tmpSmiles, false, false);
            Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpMolecule));
            Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpMolecule));
            Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpMolecule));
            List<IAtomContainer> tmpFragments = tmpFragmenter.fragmentMolecule(tmpMolecule);
            Assertions.assertFalse(tmpFragments.isEmpty());
            //the CDK circular fragmenter does not tag its fragments with the fragment-category property
            this.assertFragmentsRoundTrip(tmpFragments, false);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods" defaultstate="collapsed">
    /**
     * Asserts the per-fragment invariant chain for a non-empty fragment list: every fragment carries the fragment
     * category property; its canonical unique SMILES is non-null; that unique SMILES re-parses to a non-null atom
     * container; and the re-parsed container regenerates exactly the same unique SMILES (round-trip identity). No
     * fragment SMILES is ever compared to a hard-coded literal, so the check is invariant-based and CDK-drift robust.
     *
     * @param aFragmentList the list of fragments produced by a fragmenter (must be non-empty)
     * @param anExpectFragmentCategory whether every fragment is expected to carry the fragment-category property; the
     *                                 Ertl, SugarRemovalUtility and ScaffoldGenerator fragmenters tag their fragments
     *                                 with it, whereas MolWURCS and the CDK circular fragmenter do not set it, so this
     *                                 is gated per algorithm
     * @throws Exception if anything goes wrong while regenerating or re-parsing SMILES
     */
    private void assertFragmentsRoundTrip(List<IAtomContainer> aFragmentList, boolean anExpectFragmentCategory)
            throws Exception {
        for (IAtomContainer tmpFragment : aFragmentList) {
            if (anExpectFragmentCategory) {
                Assertions.assertNotNull(tmpFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
            }
            String tmpUniqueSmiles = ChemUtil.createUniqueSmiles(tmpFragment, false);
            Assertions.assertNotNull(tmpUniqueSmiles);
            IAtomContainer tmpReparsed = ChemUtil.parseSmilesToAtomContainer(tmpUniqueSmiles, false, false);
            Assertions.assertNotNull(tmpReparsed);
            Assertions.assertEquals(tmpUniqueSmiles, ChemUtil.createUniqueSmiles(tmpReparsed, false));
        }
    }
    //</editor-fold>
}
