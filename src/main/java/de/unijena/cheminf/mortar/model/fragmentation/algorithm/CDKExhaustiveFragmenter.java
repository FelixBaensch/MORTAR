/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2024  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.io.Importer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;

import org.openscience.cdk.fragment.ExhaustiveFragmenter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class that makes the
 * <a href="https://cdk.github.io/cdk/latest/docs/api/org/openscience/cdk/fragment/ExhaustiveFragmenter.html">
 *     exhaustive fragmentation
 * </a>
 * from the CDK available for MORTAR.
 *
 * @author Tom Weiß
 * @version 1.0.0.0
 */
public class CDKExhaustiveFragmenter implements IMoleculeFragmenter {
    //<editor-fold desc="Public static final variables">
    /**
     * The default value for the minimum fragment size used for the fragmentation.
     */
    public static final int DEFAULT_MINIMUM_FRAGMENT_SIZE = 6;
    //
    /**
     * The name of the algorithm used for fragmentation.
     */
    public static final String ALGORITHM_NAME = "Exhaustive Fragmenter";
    //</editor-fold>
    //
    //<editor-fold desc="Private final variables">
    /**
     * The minimum size of the returned fragments. This size consists of all atoms, that are connected by more than
     * a single bond or have more than one single bond.
     */
    private final SimpleIntegerProperty minimumFragmentSizeSetting;
    //
    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding in GUI.
     */
    private final List<Property<?>> settings;
    //
    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private final HashMap<String, String> settingNameTooltipTextMap;
    //
    /**
     * Map to store pairs of {@literal <setting name, display name>}.
     */
    private final HashMap<String, String> settingNameDisplayNameMap;
    //
    /**
     * Instance of ExhaustiveFragmenter class to fragment a molecule.
     */
    private final ExhaustiveFragmenter cdkEFInstance;
    //
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(CDKExhaustiveFragmenter.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their default values as declared in the respective public constants.
     */
    public CDKExhaustiveFragmenter() {
        int tmpNumberOfSettingsForTooltipMapSize = 1;
        int tmpInitialCapacityForSettingNameTooltipTextMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpNumberOfSettingsForTooltipMapSize,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap<>(tmpInitialCapacityForSettingNameTooltipTextMap,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameDisplayNameMap = new HashMap<>(tmpInitialCapacityForSettingNameTooltipTextMap,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.cdkEFInstance = new ExhaustiveFragmenter();
        this.minimumFragmentSizeSetting = new SimpleIntegerProperty(this,
                "Minimum Size for the returned fragments",
                CDKExhaustiveFragmenter.DEFAULT_MINIMUM_FRAGMENT_SIZE) {
            @Override
            public void set(int newValue) {
                if (newValue > 0) {
                    CDKExhaustiveFragmenter.this.cdkEFInstance.setMinimumFragmentSize(newValue);
                    super.set(newValue);
                }
                else {
                    IllegalArgumentException anException = new IllegalArgumentException("The minimum fragment size can not be zero");
                    CDKExhaustiveFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.minimumFragmentSizeSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.minFragmentSize.tooltip"));
        this.settingNameDisplayNameMap.put(this.minimumFragmentSizeSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.minFragmentSize.displayName"));
        this.settings = new ArrayList<>(tmpNumberOfSettingsForTooltipMapSize);
        this.settings.add(this.minimumFragmentSizeSetting);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     * Returns the setting for the minimum fragment size.
     *
     * @return the setting for the minimum fragment size.
     */
    public SimpleIntegerProperty getMinimumFragmentSizeSetting() {
        return this.minimumFragmentSizeSetting;
    }
    /**
     * Returns the minimum fragment size currently set.
     *
     * @return the currently set minimum fragment size.
     */
    public int getMinimumFragmentSize() {
        return this.minimumFragmentSizeSetting.get();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">

    /**
     * Returns the minimum fragment size currently set.
     *
     * @param minimumFragmentSize the new minimum fragment size.
     */
    public void setMinimumFragmentSize(int minimumFragmentSize) {
        this.minimumFragmentSizeSetting.set(minimumFragmentSize);
    }
    //</editor-fold>
    //
    //<editor-fold desc="IMoleculeFragmenter methods">

    @Override
    public List<Property<?>> settingsProperties() {
        return this.settings;
    }

    @Override
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return this.settingNameTooltipTextMap;
    }

    @Override
    public Map<String, String> getSettingNameToDisplayNameMap() {
        return this.settingNameDisplayNameMap;
    }

    @Override
    public String getFragmentationAlgorithmName() {
        return CDKExhaustiveFragmenter.ALGORITHM_NAME;
    }

    @Override
    public String getFragmentationAlgorithmDisplayName() {
        return Message.get("CDKExhaustiveFragmenter.displayName");
    }

    @Override
    public FragmentSaturationOption getFragmentSaturationSetting() throws UnsupportedOperationException {
        //TODO: there is currently no possibility to implement saturation settings for the exhaustive fragmenter.
        // Because the exhaustive fragmenter in the CDK saturates the fragments by default and is not modifiable.
        throw new UnsupportedOperationException("The saturation is currently not configurable for the " + CDKExhaustiveFragmenter.ALGORITHM_NAME);
    }

    @Override
    public SimpleIDisplayEnumConstantProperty fragmentSaturationSettingProperty() throws UnsupportedOperationException {
        //TODO: there is currently no possibility to implement saturation settings for the exhaustive fragmenter.
        // Because the exhaustive fragmenter in the CDK saturates the fragments by default and is not modifiable.
        throw new UnsupportedOperationException("The saturation is currently not configurable for the " + CDKExhaustiveFragmenter.ALGORITHM_NAME);
    }

    @Override
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws UnsupportedOperationException {
        //TODO: there is currently no possibility to implement saturation settings for the exhaustive fragmenter.
        // Because the exhaustive fragmenter in the CDK saturates the fragments by default and is not modifiable.
        throw new UnsupportedOperationException("The saturation is currently not configurable for the " + CDKExhaustiveFragmenter.ALGORITHM_NAME);
    }

    @Override
    public IMoleculeFragmenter copy() {
        CDKExhaustiveFragmenter tmpCopy = new CDKExhaustiveFragmenter();
        tmpCopy.minimumFragmentSizeSetting.set(this.minimumFragmentSizeSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.minimumFragmentSizeSetting.set(CDKExhaustiveFragmenter.DEFAULT_MINIMUM_FRAGMENT_SIZE);
    }

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        //<editor-fold desc="Parameter tests">
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpCanBeFragmented = this.canBeFragmented(aMolecule);
        if (!tmpCanBeFragmented) {
            throw new IllegalArgumentException("Given molecule cannot be fragmented but should be filtered or preprocessed first.");
        }
        //</editor-fold>
        IAtomContainer tmpMoleculeClone = aMolecule.clone();
        List<IAtomContainer> tmpFragments = new ArrayList<>(tmpMoleculeClone.getAtomCount() / 2);
        try {
            SmilesParser tmpSmilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            this.cdkEFInstance.generateFragments(tmpMoleculeClone);
            // TODO: there is also an option to extract atom containers directly with getFragmentsAsContainers but this
            //  oversaturates fragments described in this issue https://github.com/cdk/cdk/issues/1119.
            List<String> tmpSmiles = new ArrayList<>(List.of(this.cdkEFInstance.getFragments()));
            for (String smile : tmpSmiles) {
                tmpFragments.add(tmpSmilesParser.parseSmiles(smile));
            }

        } catch (Exception anException) {
            throw new IllegalArgumentException("An error occurred during fragmentation: " + anException.toString() + " Molecule Name: " + aMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        }
        return tmpFragments;
    }

    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        return (Objects.isNull(aMolecule) || aMolecule.isEmpty());
    }

    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        return false;
    }

    @Override
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        boolean tmpShouldBePreprocessed = this.shouldBePreprocessed(aMolecule);
        return !(tmpShouldBeFiltered || tmpShouldBePreprocessed);
    }

    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        if (tmpShouldBeFiltered) {
            throw new IllegalArgumentException("The given molecule cannot be preprocessed but should be filtered.");
        }
        return aMolecule.clone();
    }
    //</editor-fold>
}
