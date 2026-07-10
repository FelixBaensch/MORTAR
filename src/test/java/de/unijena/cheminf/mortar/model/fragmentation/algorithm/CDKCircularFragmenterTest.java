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

package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

import javafx.beans.property.Property;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.fragment.CircularFragmenter;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.List;
import java.util.Locale;

/**
 * Class to test the correct working of
 * {@link de.unijena.cheminf.mortar.model.fragmentation.algorithm.CDKCircularFragmenter}.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
class CDKCircularFragmenterTest {
    /**
     * Constructor that sets the default locale to British English, which is important for the correct functioning of the
     * fragmenter because the settings tooltips are imported from the message.properties file.
     */
    public CDKCircularFragmenterTest() {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //
    /**
     * Tests instantiation and basic settings retrieval, including verification that all default values match
     * their declared constants.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void basicTest() throws Exception {
        CDKCircularFragmenter tmpFragmenter = new CDKCircularFragmenter();
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmName);
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmDisplayName);
        Assertions.assertDoesNotThrow(tmpFragmenter::getRadiusSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::getIncludeSmallerRadiiSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::getPreserveStereoSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::getMarkAttachmentsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::getApplyAromaticityDetectionSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::getElectronDonationModelSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::getCycleFinderSetting);
        for (Property<?> tmpSetting : tmpFragmenter.settingsProperties()) {
            Assertions.assertDoesNotThrow(tmpSetting::getName);
        }
        // Verify default values match the declared constants
        Assertions.assertEquals(CDKCircularFragmenter.RADIUS_SETTING_DEFAULT, tmpFragmenter.getRadiusSetting());
        Assertions.assertEquals(CDKCircularFragmenter.INCLUDE_SMALLER_RADII_SETTING_DEFAULT, tmpFragmenter.getIncludeSmallerRadiiSetting());
        Assertions.assertEquals(CDKCircularFragmenter.PRESERVE_STEREO_SETTING_DEFAULT, tmpFragmenter.getPreserveStereoSetting());
        Assertions.assertEquals(CDKCircularFragmenter.MARK_ATTACHMENTS_SETTING_DEFAULT, tmpFragmenter.getMarkAttachmentsSetting());
        Assertions.assertEquals(CDKCircularFragmenter.APPLY_AROMATICITY_DETECTION_SETTING_DEFAULT, tmpFragmenter.getApplyAromaticityDetectionSetting());
        Assertions.assertEquals(CDKCircularFragmenter.ELECTRON_DONATION_MODEL_OPTION_DEFAULT, tmpFragmenter.getElectronDonationModelSetting());
        Assertions.assertEquals(CDKCircularFragmenter.CYCLE_FINDER_OPTION_DEFAULT, tmpFragmenter.getCycleFinderSetting());
    }
    //
    /**
     * Does a test fragmentation on the COCONUT natural product CNP0151033. Verifies that the number of returned
     * fragments equals the number of atoms in the molecule (one circular fragment per atom, no deduplication),
     * that a valid canonical SMILES string can be generated for every fragment, and that each atom in every
     * fragment carries the {@link CircularFragmenter#FRAGMENT_ATOM_DEPTH_PROPERTY_KEY} property.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void fragmentationTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        SmilesGenerator tmpSmiGen = new SmilesGenerator(SmiFlavor.Canonical);
        CDKCircularFragmenter tmpFragmenter = new CDKCircularFragmenter();
        IAtomContainer tmpMolecule = tmpSmiPar.parseSmiles(
                //CNP0151033
                "O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4");
        Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpMolecule));
        Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpMolecule));
        Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpMolecule));
        List<IAtomContainer> tmpFragments = tmpFragmenter.fragmentMolecule(tmpMolecule);
        // The fragmenter produces exactly one fragment per atom (not deduplicated)
        Assertions.assertEquals(tmpMolecule.getAtomCount(), tmpFragments.size());
        for (IAtomContainer tmpFragment : tmpFragments) {
            // Every fragment can be converted to a canonical SMILES string
            Assertions.assertDoesNotThrow(() -> tmpSmiGen.create(tmpFragment));
            // Every atom in every fragment has the depth property set (0 = center atom)
            for (IAtom tmpAtom : tmpFragment.atoms()) {
                Assertions.assertNotNull(tmpAtom.getProperty(CircularFragmenter.FRAGMENT_ATOM_DEPTH_PROPERTY_KEY));
            }
        }
    }
    //
    /**
     * Tests that the {@code shouldBeFiltered} method correctly identifies molecules that should not be
     * fragmented (null and empty molecules), and that {@code shouldBePreprocessed} always returns false
     * (no preprocessing needed).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void filterAndPreprocessTest() throws Exception {
        CDKCircularFragmenter tmpFragmenter = new CDKCircularFragmenter();
        // Null molecules must be filtered
        Assertions.assertTrue(tmpFragmenter.shouldBeFiltered(null));
        // Empty molecules must be filtered
        IAtomContainer tmpEmpty = SilentChemObjectBuilder.getInstance().newAtomContainer();
        Assertions.assertTrue(tmpFragmenter.shouldBeFiltered(tmpEmpty));
        // Non-empty molecules must not be filtered
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpMolecule = tmpSmiPar.parseSmiles("c1ccccc1");
        Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpMolecule));
        // The fragmenter never requires preprocessing
        Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpMolecule));
        Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpMolecule));
    }
    //
    /**
     * Tests the effect of the radius setting on the size of the extracted circular fragments using benzene
     * (6 atoms in a ring with maximum distance of 3 bonds between any two atoms).
     * <ul>
     *   <li>Radius 0: each fragment contains exactly 1 atom (the center atom only) and 0 bonds.</li>
     *   <li>Radius 3: each fragment contains all 6 atoms of benzene (the whole molecule fits within
     *       3 bonds of any center atom).</li>
     * </ul>
     * In both cases the number of fragments equals the atom count.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void radiusSettingTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        CDKCircularFragmenter tmpFragmenter = new CDKCircularFragmenter();
        // Benzene: 6 atoms in a ring; maximum shortest path between any two atoms = 3 bonds
        IAtomContainer tmpBenzene = tmpSmiPar.parseSmiles("c1ccccc1");
        int tmpAtomCount = tmpBenzene.getAtomCount();
        // Radius 0: each fragment is just the center atom itself
        tmpFragmenter.setRadiusSetting(0);
        List<IAtomContainer> tmpFragsRadius0 = tmpFragmenter.fragmentMolecule(tmpBenzene);
        Assertions.assertEquals(tmpAtomCount, tmpFragsRadius0.size());
        for (IAtomContainer tmpFragment : tmpFragsRadius0) {
            Assertions.assertEquals(1, tmpFragment.getAtomCount());
            Assertions.assertEquals(0, tmpFragment.getBondCount());
            // The single atom must be the center atom (depth 0)
            Assertions.assertEquals(0,
                    (int) tmpFragment.getAtom(0).getProperty(CircularFragmenter.FRAGMENT_ATOM_DEPTH_PROPERTY_KEY));
        }
        // Radius 3: the entire benzene ring fits within 3 bonds of any atom
        tmpFragmenter.setRadiusSetting(3);
        List<IAtomContainer> tmpFragsRadius3 = tmpFragmenter.fragmentMolecule(tmpBenzene);
        Assertions.assertEquals(tmpAtomCount, tmpFragsRadius3.size());
        for (IAtomContainer tmpFragment : tmpFragsRadius3) {
            Assertions.assertEquals(tmpAtomCount, tmpFragment.getAtomCount());
        }
    }
    //
    /**
     * Tests the include-smaller-radii setting using benzene.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void includeSmallerRadiiSettingTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        CDKCircularFragmenter tmpFragmenter = new CDKCircularFragmenter();
        IAtomContainer tmpBenzene = tmpSmiPar.parseSmiles("c1ccccc1");
        int tmpAtomCount = tmpBenzene.getAtomCount();
        tmpFragmenter.setRadiusSetting(3);
        tmpFragmenter.setIncludeSmallerRadiiSetting(true);
        List<IAtomContainer> tmpFrags = tmpFragmenter.fragmentMolecule(tmpBenzene);
        // 4 radii: 0, 1, 2, 3; one fragment created per atom in each round
        Assertions.assertEquals(tmpAtomCount * 4, tmpFrags.size());
    }
    //
    /**
     * Tests the mark-attachments setting using propane (CCC, 3 atoms) at radius 1.
     * <ul>
     *   <li>Fragment 0 (center = C0, terminal carbon): contains C0 and C1; the bond C1–C2 was cut.
     *       Without marking: 2 atoms (saturation via implicit H). With marking: 3 atoms (C0 + C1 + one pseudo atom).</li>
     *   <li>Fragment 1 (center = C1, middle carbon): contains all 3 atoms; no bonds are cut.
     *       Atom count is 3 regardless of the setting.</li>
     *   <li>Fragment 2 (center = C2, terminal carbon): mirrors fragment 0 by symmetry.</li>
     * </ul>
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void markAttachmentsSettingTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        CDKCircularFragmenter tmpFragmenter = new CDKCircularFragmenter();
        tmpFragmenter.setRadiusSetting(1);
        IAtomContainer tmpPropane = tmpSmiPar.parseSmiles("CCC");
        Assertions.assertEquals(3, tmpPropane.getAtomCount());
        // Without mark attachments: cut bonds are saturated with implicit hydrogen atoms
        tmpFragmenter.setMarkAttachmentsSetting(false);
        List<IAtomContainer> tmpFragsNoMark = tmpFragmenter.fragmentMolecule(tmpPropane);
        Assertions.assertEquals(3, tmpFragsNoMark.size());
        Assertions.assertEquals(2, tmpFragsNoMark.get(0).getAtomCount()); // C0 + C1
        Assertions.assertEquals(3, tmpFragsNoMark.get(1).getAtomCount()); // C0 + C1 + C2 (no bond cut)
        Assertions.assertEquals(2, tmpFragsNoMark.get(2).getAtomCount()); // C1 + C2
        // With mark attachments: each cut-bond endpoint gets an additional pseudo atom
        tmpFragmenter.setMarkAttachmentsSetting(true);
        List<IAtomContainer> tmpFragsMark = tmpFragmenter.fragmentMolecule(tmpPropane);
        Assertions.assertEquals(3, tmpFragsMark.size());
        Assertions.assertEquals(3, tmpFragsMark.get(0).getAtomCount()); // C0 + C1 + 1 pseudo atom
        Assertions.assertEquals(3, tmpFragsMark.get(1).getAtomCount()); // C0 + C1 + C2 (no bond cut, unchanged)
        Assertions.assertEquals(3, tmpFragsMark.get(2).getAtomCount()); // C1 + C2 + 1 pseudo atom
    }
    //
    /**
     * Tests the aromaticity-detection setting.
     * Benzene is parsed in Kekulé notation ("C1=CC=CC=C1") so that atoms carry no aromatic flags
     * initially. Without aromaticity detection, the center atoms of the fragments must not be
     * aromatic. After enabling aromaticity detection with the Daylight electron-donation model
     * (which operates on Kekulé bond orders) the center atoms must be detected as aromatic.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void aromaticityDetectionTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        CDKCircularFragmenter tmpFragmenter = new CDKCircularFragmenter();
        // Parse benzene in Kekulé notation; atoms are NOT marked as aromatic
        IAtomContainer tmpKekuleBenzene = tmpSmiPar.parseSmiles("C1=CC=CC=C1");
        // Without detection: the input molecule's (non-aromatic) flags are preserved in the fragments
        tmpFragmenter.setApplyAromaticityDetectionSetting(false);
        List<IAtomContainer> tmpFragsNoDetection = tmpFragmenter.fragmentMolecule(tmpKekuleBenzene);
        Assertions.assertEquals(tmpKekuleBenzene.getAtomCount(), tmpFragsNoDetection.size());
        for (IAtomContainer tmpFragment : tmpFragsNoDetection) {
            IAtom tmpCenterAtom = CDKCircularFragmenterTest.getCenterAtom(tmpFragment);
            Assertions.assertFalse(tmpCenterAtom.isAromatic(),
                    "Center atom must not be aromatic when detection is disabled and input has no aromaticity flags.");
        }
        // With detection (Daylight model, which works on Kekulé bond orders):
        // the benzene ring atoms should now be detected as aromatic
        tmpFragmenter.setApplyAromaticityDetectionSetting(true);
        tmpFragmenter.setElectronDonationModelSetting(IMoleculeFragmenter.ElectronDonationModelOption.DAYLIGHT);
        List<IAtomContainer> tmpFragsWithDetection = tmpFragmenter.fragmentMolecule(tmpKekuleBenzene);
        Assertions.assertEquals(tmpKekuleBenzene.getAtomCount(), tmpFragsWithDetection.size());
        for (IAtomContainer tmpFragment : tmpFragsWithDetection) {
            IAtom tmpCenterAtom = CDKCircularFragmenterTest.getCenterAtom(tmpFragment);
            Assertions.assertTrue(tmpCenterAtom.isAromatic(),
                    "Center atom must be aromatic after Daylight aromaticity detection on Kekulé benzene.");
        }
    }
    //
    /**
     * Tests that all settings can be changed to non-default values and then restored to their defaults via
     * {@link CDKCircularFragmenter#restoreDefaultSettings()}.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void restoreDefaultSettingsTest() throws Exception {
        CDKCircularFragmenter tmpFragmenter = new CDKCircularFragmenter();
        // Change every setting away from its default
        tmpFragmenter.setRadiusSetting(5);
        tmpFragmenter.setPreserveStereoSetting(!CDKCircularFragmenter.PRESERVE_STEREO_SETTING_DEFAULT);
        tmpFragmenter.setIncludeSmallerRadiiSetting(!CDKCircularFragmenter.INCLUDE_SMALLER_RADII_SETTING_DEFAULT);
        tmpFragmenter.setMarkAttachmentsSetting(!CDKCircularFragmenter.MARK_ATTACHMENTS_SETTING_DEFAULT);
        tmpFragmenter.setApplyAromaticityDetectionSetting(!CDKCircularFragmenter.APPLY_AROMATICITY_DETECTION_SETTING_DEFAULT);
        tmpFragmenter.setElectronDonationModelSetting(IMoleculeFragmenter.ElectronDonationModelOption.CDK);
        tmpFragmenter.setCycleFinderSetting(IMoleculeFragmenter.CycleFinderOption.MCB);
        // Restore and verify every setting is back to its default
        tmpFragmenter.restoreDefaultSettings();
        Assertions.assertEquals(CDKCircularFragmenter.RADIUS_SETTING_DEFAULT, tmpFragmenter.getRadiusSetting());
        Assertions.assertEquals(CDKCircularFragmenter.INCLUDE_SMALLER_RADII_SETTING_DEFAULT, tmpFragmenter.getIncludeSmallerRadiiSetting());
        Assertions.assertEquals(CDKCircularFragmenter.PRESERVE_STEREO_SETTING_DEFAULT, tmpFragmenter.getPreserveStereoSetting());
        Assertions.assertEquals(CDKCircularFragmenter.MARK_ATTACHMENTS_SETTING_DEFAULT, tmpFragmenter.getMarkAttachmentsSetting());
        Assertions.assertEquals(CDKCircularFragmenter.APPLY_AROMATICITY_DETECTION_SETTING_DEFAULT, tmpFragmenter.getApplyAromaticityDetectionSetting());
        Assertions.assertEquals(CDKCircularFragmenter.ELECTRON_DONATION_MODEL_OPTION_DEFAULT, tmpFragmenter.getElectronDonationModelSetting());
        Assertions.assertEquals(CDKCircularFragmenter.CYCLE_FINDER_OPTION_DEFAULT, tmpFragmenter.getCycleFinderSetting());
        // The fragmenter must still produce correct results after a settings restore
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpMolecule = tmpSmiPar.parseSmiles("c1ccccc1");
        List<IAtomContainer> tmpFragments = tmpFragmenter.fragmentMolecule(tmpMolecule);
        Assertions.assertEquals(tmpMolecule.getAtomCount(), tmpFragments.size());
    }
    //
    /**
     * Returns the center atom (depth 0) from the given circular fragment.
     *
     * @param aFragment a circular fragment produced by {@link CDKCircularFragmenter}
     * @return the center atom of the fragment
     * @throws IllegalStateException if no atom with depth 0 can be found in the fragment
     */
    private static IAtom getCenterAtom(IAtomContainer aFragment) {
        for (IAtom tmpAtom : aFragment.atoms()) {
            Integer tmpDepth = tmpAtom.getProperty(CircularFragmenter.FRAGMENT_ATOM_DEPTH_PROPERTY_KEY);
            if (tmpDepth != null && tmpDepth.equals(0)) {
                return tmpAtom;
            }
        }
        throw new IllegalStateException("No center atom (depth 0) found in the fragment.");
    }
}
