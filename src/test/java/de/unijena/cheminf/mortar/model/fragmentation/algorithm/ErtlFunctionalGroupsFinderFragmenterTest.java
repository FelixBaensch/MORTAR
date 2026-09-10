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

import de.unijena.cheminf.mortar.model.util.ChemUtil;

import javafx.beans.property.Property;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smarts.SmartsPattern;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Class to test the correct working of
 * {@link de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter}.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class ErtlFunctionalGroupsFinderFragmenterTest {

    /**
     * Constructor that sets the default locale to british english, which is important for the correct functioning of the
     * fragmenter because the settings tooltips are imported from the message.properties file.
     */
    public ErtlFunctionalGroupsFinderFragmenterTest() {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //
    /**
     * Tests instantiation and basic settings retrieval.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void basicTest() throws Exception {
        ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmName);
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmDisplayName);
        Assertions.assertDoesNotThrow(tmpFragmenter::getElectronDonationModelSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::getEnvironmentModeSetting);
        for (Property<?> tmpSetting : tmpFragmenter.settingsProperties()) {
            Assertions.assertDoesNotThrow(tmpSetting::getName);
        }
    }
    //
    /**
     * Does a test fragmentation on the COCONUT natural product CNP0151033.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void fragmentationTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        SmilesGenerator tmpSmiGen = new SmilesGenerator(SmiFlavor.Canonical);
        IAtomContainer tmpOriginalMolecule;
        List<IAtomContainer> tmpFragmentList;
        ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
        tmpFragmenter.setEnvironmentModeSetting(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.NO_ENVIRONMENT);
        tmpFragmenter.setEnvironmentModeSetting(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.GENERALIZATION);
        tmpFragmenter.environmentModeSettingProperty().set(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.FULL_ENVIRONMENT);
        tmpFragmenter.setFragmentSaturationSetting(IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION);
        tmpFragmenter.setElectronDonationModelSetting(ErtlFunctionalGroupsFinderFragmenter.ElectronDonationModelOption.CDK);
        tmpFragmenter.setReturnedFragmentsSetting(ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.ALL_FRAGMENTS);
        tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                //CNP0151033
                "O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4");
        Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpOriginalMolecule));
        Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
        Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpOriginalMolecule));
        tmpFragmentList = tmpFragmenter.fragmentMolecule(tmpOriginalMolecule);
        for (IAtomContainer tmpFragment : tmpFragmentList) {
            Assertions.assertDoesNotThrow(() -> tmpSmiGen.create(tmpFragment));
            Assertions.assertNotNull(tmpFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        }
    }
    //
    /**
     * Tests that the functional groups produced by EFGFFragmenter are really unaffected by whether stereochemistry is
     * encoded in the internal SMILES strings or not on a set of randomly picked COCONUT structures.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testNoStereoInFG() throws Exception {
        String[] tmpSmilesCodes = new String[] {
                "C/C=C(\\C)/C(=O)O[C@H]1C[C@@H]2C[C@@H](C[C@H]1N2C)OC(=O)/C=C(\\C)/C(=O)OCC", //CNP0315572.1
                "C/C(=C\\C[C@@H]([C@@H](C)[C@H]1CC[C@@]2(C)C3=CC[C@H]4C(C)(C)[C@@H](CC[C@]4(C)C3=CC[C@]12C)O)OC(=O)C)/C(=O)O", //CNP0219624.2
                "CC1=C2C(=O)C=C(C)C2[C@@H]3C(CC1)C(=C)C(=O)O3", //CNP0408686.4
                "C[C@@]12CC[C@@H](C[C@H]2CC[C@@H]3[C@@H]1[C@@H](C[C@]4(C)[C@@H](CC[C@]34O)C5=COC(=O)C=C5)O)OC(=O)CCCCCCC(=O)O", //CNP0222895.4
                "N#CS", //CNP0590020.0
                "CCCCCCC/C=C\\[C@@H](C#CC1=C2C(=CC3=CC(=C(C=C31)O)OC)CO[C@@H]2C=C)O", //CNP0532297.1
                "C[C@@H]1[C@@H]2[C@H](C[C@@H](O1)O[C@H]3CC[C@]4(C=O)[C@H]5CC[C@]6(C)[C@H](CC[C@@]6([C@@H]5CC[C@@]4(C3)O)O)C7=CC(=O)OC7)OC(C)(C)O2", //CNP0300894.8
                "CC1(C)CC[C@@]2(CC[C@]3(C)C(=CC[C@@H]4[C@@]5(C)CC[C@@H](C(C)(C)[C@@H]5CC[C@]43C)O[C@H]6[C@@H]([C@H]([C@@H]([C@@H](CO)O6)O)O[C@H]7[C@@H]([C@H]([C@@H]([C@@H](CO)O7)O)O)O)O[C@H]8[C@@H]([C@H]([C@@H]([C@@H](CO)O8)O)O)O)[C@@H]2C1)C(=O)O", //CNP0232655.2
                "CC1(C)[C@H]2C[C@@H]([C@]1(C)C[C@@H]2O)O[C@@H]3[C@@H]([C@@H]([C@@H]([C@H](CO[C@@H]4[C@@H]([C@](CO)(CO4)O)O)O3)O)O)O", //CNP0192219.5
                "C[C@@H](C1=CC=C2C(=C1)CC[C@H]3[C@H]2CC(=O)[C@]4(CC=CC(=O)[C@]34C)O)[C@@H]5C[C@H]6[C@@H](C(=O)O5)O6", //CNP0584929.1
                "CC1=C2C(=C(C(=C1)Cl)O)C(=O)C3=C(N(C(=C3)Cl)[C@@H]4[C@@H]([C@H]([C@@H]([C@@H](CO)O4)OC)O)O)O2", //CNP0110463.2
                "O(C1=CC(C=CC2=CC=CC=C2)=CC(OC)=C1CC=C(C)C)C", //CNP0421031.0
                "C#C[C@]1(CC[C@H]2[C@@H]3CCC4=C/C(=N/OCC(=O)N[C@H](C5=CC=CC=C5)C(=O)OC)/CC[C@]4(C)[C@H]3CC[C@@]21C)O", //CNP0232059.4
                "O=C(OC1=CC=C2C(=O)C3=CC=CC=C3C(=O)C2=C1O)C", //CNP0202328.0
                "CCCCC/C=C/C=C/C(=O)O[C@@H]1[C@@H](C)O[C@H](C[C@H]1O)C2=CC=C3C(=C2O)C(=O)C4=CC5=C(C=C(C)C=C5C(=C4C3=O)O)O", //CNP0275621.2
                "CC1=CCC[C@@]2(C)[C@@H]1O[C@@H]3C[C@H]([C@@]2(C)[C@@]43CO4)OC(=O)C", //CNP0086400.1
                "CC(C)C(=O)N[C@@H]1CCCN1C(=O)[C@@H]2[C@@H](C3=CC=CC=C3)[C@@]4(C5=CC=C(C=C5)OC)[C@H]([C@@]2(C6=C(C7=C(C=C6O4)OCO7)OC)O)OC(=O)C", //CNP0271679.1
                "CC(=CCC[C@H](C)C1=CC(=O)C(=C(C1=O)O)C)C", //CNP0223969.1
                "CC1[C@@H](C([C@@H]([C@@H](O1)OC2=CC(=CC(=C2O)O)C3=C(C(=O)C4=C(C=C(C=C4O3)O)O)O)O)O)O", //CNP0121867.3
                "CC(=O)O[C@@H]1C[C@@H]2[C@@]3(C)CCCC(C)(C)[C@@H]3CC[C@@]2(C)[C@@H]4CC=C5CO[C@H]([C@@H]5[C@]41C)O" //COCONUT CNP0276098.2 12-Epi-Deoxoscalarin
        };
        ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
        tmpFragmenter.setReturnedFragmentsSetting(ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.ONLY_FUNCTIONAL_GROUPS);
        for (String tmpSmilesCode : tmpSmilesCodes) {
            IAtomContainer tmpMolecule = ChemUtil.parseSmilesToAtomContainer(tmpSmilesCode, false, false);
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpMolecule);
            List<IAtomContainer> tmpFragments = tmpFragmenter.fragmentMolecule(tmpMolecule);
            for (IAtomContainer tmpFragment : tmpFragments) {
                Assertions.assertEquals(ChemUtil.createUniqueSmiles(tmpFragment, true),
                        ChemUtil.createUniqueSmiles(tmpFragment, false));
            }
        }
    }
    //
    /**
     * For a set of randomly picked COCONUT structures, their alkyl fragments are generated, parsed into SMILES codes
     * *with* stereochemistry, and then used as SMARTS strings to do substructure matching in the original molecule.
     * This test does not assert, structures where the substructure matching is not working are printed as SMILES codes
     * to console. This is merely an illustration of the effects the fragmentation has on stereochemistry.
     * If ligands like hydroxy groups are extracted as functional groups, the formerly connected carbon atom in the alkyl
     * remnant might not be a stereo center anymore as a result. Another possible effect is that a stereo center in the
     * alkyl remnant might change its R/S configuration because a functional group ligand is replaced by a hydrogen atom.
     * But, in this case, the overall 3D stereo configuration of the carbon atom still remains the same, just the
     * priorities and hence the R/S label might change. Also note that the depiction might change (or definitely does
     * in most cases).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testStereoInAlkylFragments() throws Exception {
        String[] tmpSmilesCodes = new String[] {
                "C/C=C(\\C)/C(=O)O[C@H]1C[C@@H]2C[C@@H](C[C@H]1N2C)OC(=O)/C=C(\\C)/C(=O)OCC", //CNP0315572.1
                //"C/C(=C\\C[C@@H]([C@@H](C)[C@H]1CC[C@@]2(C)C3=CC[C@H]4C(C)(C)[C@@H](CC[C@]4(C)C3=CC[C@]12C)O)OC(=O)C)/C(=O)O", //CNP0219624.2
                "CC1=C2C(=O)C=C(C)C2[C@@H]3C(CC1)C(=C)C(=O)O3", //CNP0408686.4
                "C[C@@]12CC[C@@H](C[C@H]2CC[C@@H]3[C@@H]1[C@@H](C[C@]4(C)[C@@H](CC[C@]34O)C5=COC(=O)C=C5)O)OC(=O)CCCCCCC(=O)O", //CNP0222895.4
                "N#CS", //CNP0590020.0
                "CCCCCCC/C=C\\[C@@H](C#CC1=C2C(=CC3=CC(=C(C=C31)O)OC)CO[C@@H]2C=C)O", //CNP0532297.1
                "C[C@@H]1[C@@H]2[C@H](C[C@@H](O1)O[C@H]3CC[C@]4(C=O)[C@H]5CC[C@]6(C)[C@H](CC[C@@]6([C@@H]5CC[C@@]4(C3)O)O)C7=CC(=O)OC7)OC(C)(C)O2", //CNP0300894.8
                "CC1(C)CC[C@@]2(CC[C@]3(C)C(=CC[C@@H]4[C@@]5(C)CC[C@@H](C(C)(C)[C@@H]5CC[C@]43C)O[C@H]6[C@@H]([C@H]([C@@H]([C@@H](CO)O6)O)O[C@H]7[C@@H]([C@H]([C@@H]([C@@H](CO)O7)O)O)O)O[C@H]8[C@@H]([C@H]([C@@H]([C@@H](CO)O8)O)O)O)[C@@H]2C1)C(=O)O", //CNP0232655.2
                "CC1(C)[C@H]2C[C@@H]([C@]1(C)C[C@@H]2O)O[C@@H]3[C@@H]([C@@H]([C@@H]([C@H](CO[C@@H]4[C@@H]([C@](CO)(CO4)O)O)O3)O)O)O", //CNP0192219.5
                "C[C@@H](C1=CC=C2C(=C1)CC[C@H]3[C@H]2CC(=O)[C@]4(CC=CC(=O)[C@]34C)O)[C@@H]5C[C@H]6[C@@H](C(=O)O5)O6", //CNP0584929.1
                "CC1=C2C(=C(C(=C1)Cl)O)C(=O)C3=C(N(C(=C3)Cl)[C@@H]4[C@@H]([C@H]([C@@H]([C@@H](CO)O4)OC)O)O)O2", //CNP0110463.2
                "O(C1=CC(C=CC2=CC=CC=C2)=CC(OC)=C1CC=C(C)C)C", //CNP0421031.0
                "C#C[C@]1(CC[C@H]2[C@@H]3CCC4=C/C(=N/OCC(=O)N[C@H](C5=CC=CC=C5)C(=O)OC)/CC[C@]4(C)[C@H]3CC[C@@]21C)O", //CNP0232059.4
                "O=C(OC1=CC=C2C(=O)C3=CC=CC=C3C(=O)C2=C1O)C", //CNP0202328.0
                "CCCCC/C=C/C=C/C(=O)O[C@@H]1[C@@H](C)O[C@H](C[C@H]1O)C2=CC=C3C(=C2O)C(=O)C4=CC5=C(C=C(C)C=C5C(=C4C3=O)O)O", //CNP0275621.2
                "CC1=CCC[C@@]2(C)[C@@H]1O[C@@H]3C[C@H]([C@@]2(C)[C@@]43CO4)OC(=O)C", //CNP0086400.1
                "CC(C)C(=O)N[C@@H]1CCCN1C(=O)[C@@H]2[C@@H](C3=CC=CC=C3)[C@@]4(C5=CC=C(C=C5)OC)[C@H]([C@@]2(C6=C(C7=C(C=C6O4)OCO7)OC)O)OC(=O)C", //CNP0271679.1
                "CC(=CCC[C@H](C)C1=CC(=O)C(=C(C1=O)O)C)C", //CNP0223969.1
                "CC1[C@@H](C([C@@H]([C@@H](O1)OC2=CC(=CC(=C2O)O)C3=C(C(=O)C4=C(C=C(C=C4O3)O)O)O)O)O)O", //CNP0121867.3
                "CC(=O)O[C@@H]1C[C@@H]2[C@@]3(C)CCCC(C)(C)[C@@H]3CC[C@@]2(C)[C@@H]4CC=C5CO[C@H]([C@@H]5[C@]41C)O" //COCONUT CNP0276098.2 12-Epi-Deoxoscalarin
        };
        ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
        tmpFragmenter.setReturnedFragmentsSetting(ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.ONLY_ALKANE_FRAGMENTS);
        for (String tmpSmilesCode : tmpSmilesCodes) {
            IAtomContainer tmpMolecule = ChemUtil.parseSmilesToAtomContainer(tmpSmilesCode, false, false);
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpMolecule);
            List<IAtomContainer> tmpFragments = tmpFragmenter.fragmentMolecule(tmpMolecule);
            for (IAtomContainer tmpFragment : tmpFragments) {
                String tmpAlkylFragmentSmiles = ChemUtil.createUniqueSmiles(tmpFragment, true, true);
                SmartsPattern tmpPattern = SmartsPattern.create(tmpAlkylFragmentSmiles);
                if(!tmpPattern.matches(tmpMolecule)) {
                    System.out.println("\nOriginal molecule: " + tmpSmilesCode);
                    System.out.println("Fragment: " + tmpAlkylFragmentSmiles + "\n");
                }
            }
        }
    }
    //
    /**
     * Exercises every settings accessor and property getter, drives the boolean and enum setters
     * (filter single atoms, apply input restrictions, cycle finder) with read-back assertions, checks the tooltip and
     * display-name maps, and verifies that {@link ErtlFunctionalGroupsFinderFragmenter#copy()} preserves settings and
     * {@link ErtlFunctionalGroupsFinderFragmenter#restoreDefaultSettings()} resets them to the documented defaults.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void settingsTest() throws Exception {
        ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
        //every get*Setting / *SettingProperty returns without throwing
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentSaturationSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::fragmentSaturationSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getEnvironmentModeSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::environmentModeSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getElectronDonationModelSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::electronDonationModelSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getReturnedFragmentsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::returnedFragmentsSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getCycleFinderSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::cycleFinderSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getFilterSingleAtomsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::filterSingleAtomsSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getApplyInputRestrictionsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::applyInputRestrictionsSettingProperty);
        //boolean setters with read-back
        tmpFragmenter.setFilterSingleAtomsSetting(false);
        Assertions.assertFalse(tmpFragmenter.getFilterSingleAtomsSetting());
        tmpFragmenter.setFilterSingleAtomsSetting(true);
        Assertions.assertTrue(tmpFragmenter.getFilterSingleAtomsSetting());
        tmpFragmenter.setApplyInputRestrictionsSetting(true);
        Assertions.assertTrue(tmpFragmenter.getApplyInputRestrictionsSetting());
        tmpFragmenter.setApplyInputRestrictionsSetting(false);
        Assertions.assertFalse(tmpFragmenter.getApplyInputRestrictionsSetting());
        //enum setters with read-back, driving each value
        for (ErtlFunctionalGroupsFinderFragmenter.FGEnvOption tmpOption
                : ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.values()) {
            tmpFragmenter.setEnvironmentModeSetting(tmpOption);
            Assertions.assertEquals(tmpOption, tmpFragmenter.getEnvironmentModeSetting());
        }
        for (ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption tmpOption
                : ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.values()) {
            tmpFragmenter.setReturnedFragmentsSetting(tmpOption);
            Assertions.assertEquals(tmpOption, tmpFragmenter.getReturnedFragmentsSetting());
        }
        for (IMoleculeFragmenter.CycleFinderOption tmpOption : IMoleculeFragmenter.CycleFinderOption.values()) {
            tmpFragmenter.setCycleFinderSetting(tmpOption);
            Assertions.assertEquals(tmpOption, tmpFragmenter.getCycleFinderSetting());
        }
        for (IMoleculeFragmenter.ElectronDonationModelOption tmpOption
                : IMoleculeFragmenter.ElectronDonationModelOption.values()) {
            tmpFragmenter.setElectronDonationModelSetting(tmpOption);
            Assertions.assertEquals(tmpOption, tmpFragmenter.getElectronDonationModelSetting());
        }
        for (IMoleculeFragmenter.FragmentSaturationOption tmpOption
                : IMoleculeFragmenter.FragmentSaturationOption.values()) {
            tmpFragmenter.setFragmentSaturationSetting(tmpOption);
            Assertions.assertEquals(tmpOption, tmpFragmenter.getFragmentSaturationSetting());
        }
        //tooltip and display-name maps non-null/non-empty with an entry per setting
        Map<String, String> tmpTooltipMap = tmpFragmenter.getSettingNameToTooltipTextMap();
        Map<String, String> tmpDisplayMap = tmpFragmenter.getSettingNameToDisplayNameMap();
        Assertions.assertNotNull(tmpTooltipMap);
        Assertions.assertNotNull(tmpDisplayMap);
        Assertions.assertFalse(tmpTooltipMap.isEmpty());
        Assertions.assertFalse(tmpDisplayMap.isEmpty());
        for (Property<?> tmpSetting : tmpFragmenter.settingsProperties()) {
            Assertions.assertTrue(tmpTooltipMap.containsKey(tmpSetting.getName()));
            Assertions.assertTrue(tmpDisplayMap.containsKey(tmpSetting.getName()));
        }
        //copy() preserves representative settings
        tmpFragmenter.setEnvironmentModeSetting(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.NO_ENVIRONMENT);
        tmpFragmenter.setCycleFinderSetting(IMoleculeFragmenter.CycleFinderOption.MCB);
        tmpFragmenter.setElectronDonationModelSetting(IMoleculeFragmenter.ElectronDonationModelOption.MDL);
        tmpFragmenter.setFilterSingleAtomsSetting(false);
        IMoleculeFragmenter tmpCopyAsInterface = tmpFragmenter.copy();
        Assertions.assertInstanceOf(ErtlFunctionalGroupsFinderFragmenter.class, tmpCopyAsInterface);
        ErtlFunctionalGroupsFinderFragmenter tmpCopy = (ErtlFunctionalGroupsFinderFragmenter) tmpCopyAsInterface;
        Assertions.assertEquals(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.NO_ENVIRONMENT, tmpCopy.getEnvironmentModeSetting());
        Assertions.assertEquals(IMoleculeFragmenter.CycleFinderOption.MCB, tmpCopy.getCycleFinderSetting());
        Assertions.assertEquals(IMoleculeFragmenter.ElectronDonationModelOption.MDL, tmpCopy.getElectronDonationModelSetting());
        Assertions.assertFalse(tmpCopy.getFilterSingleAtomsSetting());
        //restoreDefaultSettings() resets representative settings to their documented defaults
        tmpFragmenter.restoreDefaultSettings();
        Assertions.assertEquals(ErtlFunctionalGroupsFinderFragmenter.ENVIRONMENT_MODE_OPTION_DEFAULT, tmpFragmenter.getEnvironmentModeSetting());
        Assertions.assertEquals(ErtlFunctionalGroupsFinderFragmenter.CYCLE_FINDER_OPTION_DEFAULT, tmpFragmenter.getCycleFinderSetting());
        Assertions.assertEquals(ErtlFunctionalGroupsFinderFragmenter.ELECTRON_DONATION_MODEL_OPTION_DEFAULT, tmpFragmenter.getElectronDonationModelSetting());
        Assertions.assertEquals(ErtlFunctionalGroupsFinderFragmenter.RETURNED_FRAGMENTS_OPTION_DEFAULT, tmpFragmenter.getReturnedFragmentsSetting());
        Assertions.assertEquals(ErtlFunctionalGroupsFinderFragmenter.FILTER_SINGLE_ATOMS_OPTION_DEFAULT, tmpFragmenter.getFilterSingleAtomsSetting());
        Assertions.assertEquals(ErtlFunctionalGroupsFinderFragmenter.APPLY_INPUT_RESTRICTIONS_OPTION_DEFAULT, tmpFragmenter.getApplyInputRestrictionsSetting());
    }
    //
    /**
     * Drives the remaining edge branches of the fragmenter: with the filter-single-atoms setting enabled, a single-atom
     * molecule hits the corresponding {@code shouldBeFiltered} branch; {@code applyPreprocessing} is exercised on a
     * fragmentable molecule (asserting a non-null, fragmentable result); and every {@link IMoleculeFragmenter.CycleFinderOption}
     * and every {@link IMoleculeFragmenter.ElectronDonationModelOption} value is set and used for a fragmentation run
     * without throwing, covering the private instance-setter switches and the {@code IMoleculeFragmenter} nested enums (D-08).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void edgeTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        //single-atom molecule (zero bonds) should be filtered when filterSingleAtoms is enabled
        ErtlFunctionalGroupsFinderFragmenter tmpFilteringFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
        tmpFilteringFragmenter.setFilterSingleAtomsSetting(true);
        IAtomContainer tmpSingleAtomMolecule = tmpSmiPar.parseSmiles("[Ne]");
        Assertions.assertTrue(tmpFilteringFragmenter.shouldBeFiltered(tmpSingleAtomMolecule));
        Assertions.assertFalse(tmpFilteringFragmenter.canBeFragmented(tmpSingleAtomMolecule));
        //with filtering disabled, the single-atom molecule is no longer filtered
        tmpFilteringFragmenter.setFilterSingleAtomsSetting(false);
        IAtomContainer tmpSingleAtomMolecule2 = tmpSmiPar.parseSmiles("[Ne]");
        Assertions.assertFalse(tmpFilteringFragmenter.shouldBeFiltered(tmpSingleAtomMolecule2));
        //applyPreprocessing on a fragmentable molecule returns a non-null, fragmentable container
        ErtlFunctionalGroupsFinderFragmenter tmpPreprocessFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
        //CNP0151033
        String tmpSmiles = "O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4";
        IAtomContainer tmpPreprocessMolecule = tmpSmiPar.parseSmiles(tmpSmiles);
        Assertions.assertFalse(tmpPreprocessFragmenter.shouldBePreprocessed(tmpPreprocessMolecule));
        IAtomContainer tmpPreprocessed = tmpPreprocessFragmenter.applyPreprocessing(tmpPreprocessMolecule);
        Assertions.assertNotNull(tmpPreprocessed);
        Assertions.assertTrue(tmpPreprocessFragmenter.canBeFragmented(tmpPreprocessed));
        //applyPreprocessing throws if the molecule should be filtered
        ErtlFunctionalGroupsFinderFragmenter tmpFilterThenPreprocess = new ErtlFunctionalGroupsFinderFragmenter();
        tmpFilterThenPreprocess.setFilterSingleAtomsSetting(true);
        IAtomContainer tmpSingleAtomForPreprocess = tmpSmiPar.parseSmiles("[Ne]");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> tmpFilterThenPreprocess.applyPreprocessing(tmpSingleAtomForPreprocess));
        //every CycleFinderOption value: set then fragment without throwing
        for (IMoleculeFragmenter.CycleFinderOption tmpOption : IMoleculeFragmenter.CycleFinderOption.values()) {
            ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
            tmpFragmenter.setCycleFinderSetting(tmpOption);
            IAtomContainer tmpMolecule = tmpSmiPar.parseSmiles(tmpSmiles);
            Assertions.assertDoesNotThrow(() -> tmpFragmenter.fragmentMolecule(tmpMolecule));
        }
        //every ElectronDonationModelOption value: set then fragment without throwing
        for (IMoleculeFragmenter.ElectronDonationModelOption tmpOption
                : IMoleculeFragmenter.ElectronDonationModelOption.values()) {
            ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
            tmpFragmenter.setElectronDonationModelSetting(tmpOption);
            IAtomContainer tmpMolecule = tmpSmiPar.parseSmiles(tmpSmiles);
            Assertions.assertDoesNotThrow(() -> tmpFragmenter.fragmentMolecule(tmpMolecule));
        }
    }
}
