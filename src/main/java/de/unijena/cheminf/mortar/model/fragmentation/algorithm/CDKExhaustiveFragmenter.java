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
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;

import org.openscience.cdk.fragment.ExhaustiveFragmenter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class that makes the exhaustive fragmentation from the CDK, available for MORTAR.
 */
public class CDKExhaustiveFragmenter implements IMoleculeFragmenter {

    /**
     * The default value for the minimum fragment size used for the fragmentation.
     */
    public static final int DEFAULT_MINIMUM_FRAGMENT_SIZE = 6;
    //
    /**
     * The name of the algorithm used for fragmentation.
     */
    public static final String ALGORITHM_NAME = "Exhaustive Fragmenter";
    //
    /**
     * The minimum size of the returned fragments.
     */
    private final SimpleIntegerProperty minimumFragmentSize;
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
    private final ExhaustiveFragmenter cdkExhaustiveFragmenter;
    //
    /**
     * A property that has a constant from the IMoleculeFragmenter.FragmentSaturationOption enum as value.
     */
    private final SimpleIDisplayEnumConstantProperty fragmentSaturationSetting;
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
        int tmpNumberOfSettings = 2;
        this.settingNameTooltipTextMap = new HashMap<>(tmpNumberOfSettings,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameDisplayNameMap = new HashMap<>(tmpNumberOfSettings,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.cdkExhaustiveFragmenter = new ExhaustiveFragmenter();
        this.minimumFragmentSize = new SimpleIntegerProperty(this,
                "Minimum Size for the returned fragments",
                DEFAULT_MINIMUM_FRAGMENT_SIZE) {
            @Override
            public void set(int newValue) {
                if (newValue != 0) {
                    try {
                        //throws IllegalArgumentException
                        CDKExhaustiveFragmenter.this.cdkExhaustiveFragmenter.setMinimumFragmentSize(newValue);
                    } catch (IllegalArgumentException anException) {
                        CDKExhaustiveFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                        GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                                Message.get("Fragmenter.IllegalSettingValue.Header"),
                                anException.toString(),
                                anException);
                        //re-throws the exception to properly reset the binding
                        throw anException;
                    }
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
        this.settingNameTooltipTextMap.put(this.minimumFragmentSize.getName(),
                Message.get("CDKExhaustiveFragmenter.minFragmentSize.tooltip"));
        this.settingNameDisplayNameMap.put(this.minimumFragmentSize.getName(),
                Message.get("CDKExhaustiveFragmenter.minFragmentSize.displayName"));
        this.fragmentSaturationSetting = new SimpleIDisplayEnumConstantProperty(this, "Fragment saturation setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT, IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
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
        this.settingNameTooltipTextMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.fragmentSaturationSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.fragmentSaturationSetting.displayName"));
        this.settings = new ArrayList<>(tmpNumberOfSettings);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.minimumFragmentSize);
    }

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
    public IMoleculeFragmenter.FragmentSaturationOption getFragmentSaturationSetting() {
        return (IMoleculeFragmenter.FragmentSaturationOption) this.fragmentSaturationSetting.get();
    }

    @Override
    public SimpleIDisplayEnumConstantProperty fragmentSaturationSettingProperty() {
        return this.fragmentSaturationSetting;
    }

    @Override
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given saturation option is null.");
        this.fragmentSaturationSetting.set(anOption);
    }

    @Override
    public IMoleculeFragmenter copy() {
        CDKExhaustiveFragmenter tmpCopy = new CDKExhaustiveFragmenter();
        tmpCopy.minimumFragmentSize.set(this.minimumFragmentSize.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.minimumFragmentSize.set(CDKExhaustiveFragmenter.DEFAULT_MINIMUM_FRAGMENT_SIZE);
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
        List<IAtomContainer> tmpFragments = new ArrayList<>();
        try {
            this.cdkExhaustiveFragmenter.generateFragments(tmpMoleculeClone);
            tmpFragments.addAll(List.of(this.cdkExhaustiveFragmenter.getFragmentsAsContainers()));
            if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION)) {
                for (IAtomContainer fragment : tmpFragments) {
                    CDKHydrogenAdder.getInstance(fragment.getBuilder()).addImplicitHydrogens(fragment);
                    AtomContainerManipulator.convertImplicitToExplicitHydrogens(fragment);
                }
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
        if (!this.shouldBePreprocessed(aMolecule)) {
            return aMolecule.clone();
        }
        return aMolecule.clone();
    }
}
