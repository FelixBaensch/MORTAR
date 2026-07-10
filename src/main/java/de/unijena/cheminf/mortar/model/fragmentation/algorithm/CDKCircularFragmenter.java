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

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.io.Importer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.fragment.CircularFragmenter;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class that makes the CDK {@link CircularFragmenter} available in MORTAR.
 * The fragmenter extracts atom-centered circular / spherical fragments from a molecule,
 * analogous to HOSE codes, circular Morgan-type fingerprints, and Molecular Signatures.
 * For every atom in the input molecule, the neighborhood up to a user-defined radius
 * (number of bonds) is collected by a breadth-first expansion and returned as an
 * independent {@link IAtomContainer}. Note that the resulting fragments are not
 * deduplicated.
 * <br>
 * Optionally, an aromaticity detection step can be applied as preprocessing before fragmentation.
 * The aromaticity model is composed of a configurable electron donation model and cycle finder algorithm.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class CDKCircularFragmenter implements IMoleculeFragmenter {
    //<editor-fold desc="Public static final constants">
    /**
     * Name of the algorithm used in this fragmenter.
     */
    public static final String ALGORITHM_NAME = "CDK Circular Fragmenter";

    /**
     * Default radius setting value (number of bonds), taken from {@link CircularFragmenter}.
     */
    public static final int RADIUS_SETTING_DEFAULT = 3;

    /**
     * Default include smaller radii setting value.
     */
    public static final boolean INCLUDE_SMALLER_RADII_SETTING_DEFAULT = false;

    /**
     * Default preserve stereo setting value, taken from {@link CircularFragmenter}.
     */
    public static final boolean PRESERVE_STEREO_SETTING_DEFAULT = false;

    /**
     * Default mark attachments setting value, taken from {@link CircularFragmenter}.
     */
    public static final boolean MARK_ATTACHMENTS_SETTING_DEFAULT = false;

    /**
     * Default value for the apply aromaticity detection setting; aromaticity detection is *not* applied by default.
     */
    public static final boolean APPLY_AROMATICITY_DETECTION_SETTING_DEFAULT = false;

    /**
     * Default electron donation model for aromaticity detection.
     */
    public static final IMoleculeFragmenter.ElectronDonationModelOption ELECTRON_DONATION_MODEL_OPTION_DEFAULT =
            IMoleculeFragmenter.ElectronDonationModelOption.DAYLIGHT;

    /**
     * Default option for the cycle finder algorithm employed for aromaticity detection.
     */
    public static final IMoleculeFragmenter.CycleFinderOption CYCLE_FINDER_OPTION_DEFAULT =
            IMoleculeFragmenter.CycleFinderOption.CDK_AROMATIC_SET;

    /**
     * Cycle finder algorithm that is used should the set option cause an IntractableException.
     */
    public static final CycleFinder AUXILIARY_CYCLE_FINDER = Cycles.cdkAromaticSet();
    //</editor-fold>
    //
    //<editor-fold desc="Private variables">
    /**
     * The aromaticity model used for preprocessing prior to fragmentation. Constructed from the set electron donation
     * model and cycle finder algorithm.
     */
    private Aromaticity aromaticityModelInstance;

    /**
     * A cycle finder instance for construction of the aromaticity model.
     */
    private CycleFinder cycleFinderInstance;

    /**
     * An electron donation instance for construction of the aromaticity model.
     */
    private ElectronDonation electronDonationInstance;
    //</editor-fold>
    //
    //<editor-fold desc="Private final variables">
    /**
     * Instance of the CDK CircularFragmenter class that is used for fragmentation.
     */
    private final CircularFragmenter circularFragmenterInstance;
    //note: since Java 21, the javadoc build complains about "double comments" when there is a comment
    // for the get() method of the property and the private property itself as well
    private final SimpleIntegerProperty radiusSetting;

    private final SimpleBooleanProperty includeSmallerRadiiSetting;

    private final SimpleBooleanProperty preserveStereoSetting;

    private final SimpleBooleanProperty markAttachmentsSetting;

    private final SimpleBooleanProperty applyAromaticityDetectionSetting;

    private final SimpleIDisplayEnumConstantProperty electronDonationModelSetting;

    private final SimpleIDisplayEnumConstantProperty cycleFinderSetting;

    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding in GUI.
     */
    private final List<Property<?>> settings;

    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private final HashMap<String, String> settingNameToTooltipTextMap;

    /**
     * Map to store pairs of {@literal <setting name, display name>}.
     */
    private final HashMap<String, String> settingNameToDisplayNameMap;

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(CDKCircularFragmenter.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialized with their default values as declared in the respective public constants.
     */
    public CDKCircularFragmenter() {
        this.circularFragmenterInstance = new CircularFragmenter(
                CDKCircularFragmenter.RADIUS_SETTING_DEFAULT,
                CDKCircularFragmenter.PRESERVE_STEREO_SETTING_DEFAULT,
                CDKCircularFragmenter.MARK_ATTACHMENTS_SETTING_DEFAULT
        );
        int tmpNumberOfSettings = 7;
        this.settings = new ArrayList<>(tmpNumberOfSettings);
        int tmpInitialCapacityForSettingNameTooltipTextMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpNumberOfSettings,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameToTooltipTextMap = new HashMap<>(tmpInitialCapacityForSettingNameTooltipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameToDisplayNameMap = new HashMap<>(tmpInitialCapacityForSettingNameTooltipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);

        this.radiusSetting = new SimpleIntegerProperty(this, "Radius setting",
                CDKCircularFragmenter.RADIUS_SETTING_DEFAULT) {
            @Override
            public void set(int newValue) throws IllegalArgumentException {
                if (newValue < 0) {
                    IllegalArgumentException tmpException = new IllegalArgumentException(
                            "Radius must be >= 0, got: " + newValue);
                    CDKCircularFragmenter.LOGGER.log(Level.WARNING, tmpException.toString(), tmpException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            tmpException.toString(),
                            tmpException);
                    //re-throws the exception to properly reset the binding
                    throw tmpException;
                }
                super.set(newValue);
                CDKCircularFragmenter.this.circularFragmenterInstance.setRadius(newValue);
            }
        };
        this.settings.add(this.radiusSetting);
        this.settingNameToTooltipTextMap.put(this.radiusSetting.getName(),
                Message.get("CDKCircularFragmenter.radiusSetting.tooltip"));
        this.settingNameToDisplayNameMap.put(this.radiusSetting.getName(),
                Message.get("CDKCircularFragmenter.radiusSetting.displayName"));

        this.includeSmallerRadiiSetting = new SimpleBooleanProperty(this, "Include smaller radii setting",
                CDKCircularFragmenter.INCLUDE_SMALLER_RADII_SETTING_DEFAULT);
        this.settings.add(this.includeSmallerRadiiSetting);
        this.settingNameToTooltipTextMap.put(this.includeSmallerRadiiSetting.getName(),
                Message.get("CDKCircularFragmenter.includeSmallerRadiiSetting.tooltip"));
        this.settingNameToDisplayNameMap.put(this.includeSmallerRadiiSetting.getName(),
                Message.get("CDKCircularFragmenter.includeSmallerRadiiSetting.displayName"));

        this.preserveStereoSetting = new SimpleBooleanProperty(this,
                "Preserve stereo setting",
                CDKCircularFragmenter.PRESERVE_STEREO_SETTING_DEFAULT) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                CDKCircularFragmenter.this.circularFragmenterInstance.setPreserveStereo(newValue);
            }
        };
        this.settings.add(this.preserveStereoSetting);
        this.settingNameToTooltipTextMap.put(this.preserveStereoSetting.getName(),
                Message.get("CDKCircularFragmenter.preserveStereoSetting.tooltip"));
        this.settingNameToDisplayNameMap.put(this.preserveStereoSetting.getName(),
                Message.get("CDKCircularFragmenter.preserveStereoSetting.displayName"));

        this.markAttachmentsSetting = new SimpleBooleanProperty(this,
                "Mark attachments setting",
                CDKCircularFragmenter.MARK_ATTACHMENTS_SETTING_DEFAULT) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                CDKCircularFragmenter.this.circularFragmenterInstance.setMarkAttachments(newValue);
            }
        };
        this.settings.add(this.markAttachmentsSetting);
        this.settingNameToTooltipTextMap.put(this.markAttachmentsSetting.getName(),
                Message.get("CDKCircularFragmenter.markAttachmentsSetting.tooltip"));
        this.settingNameToDisplayNameMap.put(this.markAttachmentsSetting.getName(),
                Message.get("CDKCircularFragmenter.markAttachmentsSetting.displayName"));

        this.applyAromaticityDetectionSetting = new SimpleBooleanProperty(this,
                "Apply aromaticity detection setting",
                CDKCircularFragmenter.APPLY_AROMATICITY_DETECTION_SETTING_DEFAULT);
        this.settings.add(this.applyAromaticityDetectionSetting);
        this.settingNameToTooltipTextMap.put(this.applyAromaticityDetectionSetting.getName(),
                Message.get("CDKCircularFragmenter.applyAromaticityDetectionSetting.tooltip"));
        this.settingNameToDisplayNameMap.put(this.applyAromaticityDetectionSetting.getName(),
                Message.get("CDKCircularFragmenter.applyAromaticityDetectionSetting.displayName"));

        //note: cycle finder and electron donation model have to be set prior to setting the aromaticity model!
        this.cycleFinderSetting = new SimpleIDisplayEnumConstantProperty(this, "Cycle finder algorithm setting",
                CDKCircularFragmenter.CYCLE_FINDER_OPTION_DEFAULT,
                IMoleculeFragmenter.CycleFinderOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    CDKCircularFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                CDKCircularFragmenter.this.setCycleFinderInstance((IMoleculeFragmenter.CycleFinderOption) this.get());
                CDKCircularFragmenter.this.setAromaticityInstance(
                        CDKCircularFragmenter.this.electronDonationInstance,
                        CDKCircularFragmenter.this.cycleFinderInstance);
            }
        };
        this.settings.add(this.cycleFinderSetting);
        this.settingNameToTooltipTextMap.put(this.cycleFinderSetting.getName(),
                Message.get("CDKCircularFragmenter.cycleFinderSetting.tooltip"));
        this.settingNameToDisplayNameMap.put(this.cycleFinderSetting.getName(),
                Message.get("CDKCircularFragmenter.cycleFinderSetting.displayName"));
        this.setCycleFinderInstance((IMoleculeFragmenter.CycleFinderOption) this.cycleFinderSetting.get());

        this.electronDonationModelSetting = new SimpleIDisplayEnumConstantProperty(this, "Electron donation model setting",
                CDKCircularFragmenter.ELECTRON_DONATION_MODEL_OPTION_DEFAULT,
                IMoleculeFragmenter.ElectronDonationModelOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    CDKCircularFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                CDKCircularFragmenter.this.setElectronDonationInstance((IMoleculeFragmenter.ElectronDonationModelOption) this.get());
                CDKCircularFragmenter.this.setAromaticityInstance(
                        CDKCircularFragmenter.this.electronDonationInstance,
                        CDKCircularFragmenter.this.cycleFinderInstance);
            }
        };
        this.settings.add(this.electronDonationModelSetting);
        this.settingNameToTooltipTextMap.put(this.electronDonationModelSetting.getName(),
                Message.get("CDKCircularFragmenter.electronDonationModelSetting.tooltip"));
        this.settingNameToDisplayNameMap.put(this.electronDonationModelSetting.getName(),
                Message.get("CDKCircularFragmenter.electronDonationModelSetting.displayName"));
        this.setElectronDonationInstance((IMoleculeFragmenter.ElectronDonationModelOption) this.electronDonationModelSetting.get());
        this.setAromaticityInstance(this.electronDonationInstance, this.cycleFinderInstance);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     * Returns the currently set radius for the circular neighborhood extraction (number of bonds).
     *
     * @return current radius setting value
     */
    public int getRadiusSetting() {
        return this.radiusSetting.get();
    }

    /**
     * Returns the property object of the radius setting that can be used to configure this setting.
     *
     * @return property object of the radius setting
     */
    public SimpleIntegerProperty radiusSettingProperty() {
        return this.radiusSetting;
    }

    /**
     * Returns the currently set value of the include smaller radii setting which can be used to extract fragments for
     * all the radii leading up to the one set above (starting at 0) in addition to the fragments for the set radius.
     *
     * @return current include smaller radii setting value
     */
    public boolean getIncludeSmallerRadiiSetting() {
        return this.includeSmallerRadiiSetting.get();
    }

    /**
     * Returns the property object of the include smaller radii setting that can be used to configure this setting.
     *
     * @return property object of the include smaller radii setting
     */
    public SimpleBooleanProperty includeSmallerRadiiSettingProperty() {
        return this.includeSmallerRadiiSetting;
    }


    /**
     * Returns the current state of the preserve stereo setting.
     *
     * @return true if stereochemistry annotations should be preserved in fragments
     */
    public boolean getPreserveStereoSetting() {
        return this.preserveStereoSetting.get();
    }

    /**
     * Returns the property object of the preserve stereo setting that can be used to configure this setting.
     *
     * @return property object of the preserve stereo setting
     */
    public SimpleBooleanProperty preserveStereoSettingProperty() {
        return this.preserveStereoSetting;
    }

    /**
     * Returns the current state of the mark attachments setting.
     *
     * @return true if attachment points of broken bonds should be marked with pseudo atoms
     */
    public boolean getMarkAttachmentsSetting() {
        return this.markAttachmentsSetting.get();
    }

    /**
     * Returns the property object of the mark attachments setting that can be used to configure this setting.
     *
     * @return property object of the mark attachments setting
     */
    public SimpleBooleanProperty markAttachmentsSettingProperty() {
        return this.markAttachmentsSetting;
    }

    /**
     * Returns the current state of the apply aromaticity detection setting.
     *
     * @return true if aromaticity detection is applied as preprocessing before fragmentation
     */
    public boolean getApplyAromaticityDetectionSetting() {
        return this.applyAromaticityDetectionSetting.get();
    }

    /**
     * Returns the property object of the apply aromaticity detection setting that can be used to configure this setting.
     *
     * @return property object of the apply aromaticity detection setting
     */
    public SimpleBooleanProperty applyAromaticityDetectionSettingProperty() {
        return this.applyAromaticityDetectionSetting;
    }

    /**
     * Returns the currently set option for the electron donation model setting used for
     * aromaticity detection together with the set cycle finder algorithm.
     *
     * @return enum constant of the set option
     */
    public IMoleculeFragmenter.ElectronDonationModelOption getElectronDonationModelSetting() {
        return (IMoleculeFragmenter.ElectronDonationModelOption) this.electronDonationModelSetting.get();
    }

    /**
     * Returns the property object of the electron donation model setting that can be used to configure this setting.
     *
     * @return property object of the electron donation model setting
     */
    public SimpleIDisplayEnumConstantProperty electronDonationModelSettingProperty() {
        return this.electronDonationModelSetting;
    }

    /**
     * Returns the currently set option for the cycle finder setting used for aromaticity
     * detection together with the electron donation model setting.
     *
     * @return enum constant of the set option
     */
    public IMoleculeFragmenter.CycleFinderOption getCycleFinderSetting() {
        return (IMoleculeFragmenter.CycleFinderOption) this.cycleFinderSetting.get();
    }

    /**
     * Returns the property object of the cycle finder setting that can be used to configure this setting.
     *
     * @return property object of the cycle finder setting
     */
    public SimpleIDisplayEnumConstantProperty cycleFinderSettingProperty() {
        return this.cycleFinderSetting;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     * Sets the radius for the circular neighborhood extraction (number of bonds).
     *
     * @param aRadius the new radius; must be >= 0
     * @throws IllegalArgumentException if the given radius is negative
     */
    public void setRadiusSetting(int aRadius) throws IllegalArgumentException {
        //parameter test done in overridden set() method of the property
        this.radiusSetting.set(aRadius);
    }

    /**
     * Sets the include smaller radii setting which can be used to extract fragments for
     * all the radii leading up to the one set above (starting at 0) in addition to the fragments for the set radius.
     *
     * @param aBoolean true if smaller radii should be included
     */
    public void setIncludeSmallerRadiiSetting(boolean aBoolean) {
        this.includeSmallerRadiiSetting.set(aBoolean);
    }

    /**
     * Sets the preserve stereo setting, defining whether stereochemistry annotations should be
     * preserved in the circular fragments.
     *
     * @param aBoolean true if stereo annotations should be preserved
     */
    public void setPreserveStereoSetting(boolean aBoolean) {
        this.preserveStereoSetting.set(aBoolean);
    }

    /**
     * Sets the mark attachments setting, defining whether attachment points of broken bonds
     * should be marked with pseudo ("R") atoms in the fragments.
     *
     * @param aBoolean true if attachment points should be marked with pseudo atoms
     */
    public void setMarkAttachmentsSetting(boolean aBoolean) {
        this.markAttachmentsSetting.set(aBoolean);
    }

    /**
     * Sets the apply aromaticity detection setting. If true, aromaticity detection using the configured
     * electron donation model and cycle finder algorithm is applied as preprocessing before fragmentation.
     * If false, the aromaticity information already present in the input molecule is used as-is.
     *
     * @param aBoolean true if aromaticity detection should be applied as preprocessing
     */
    public void setApplyAromaticityDetectionSetting(boolean aBoolean) {
        this.applyAromaticityDetectionSetting.set(aBoolean);
    }

    /**
     * Sets the electron donation model setting. The set electron donation model is used for aromaticity detection in
     * preprocessing together with the set cycle finder algorithm.
     *
     * @param anOption a constant from the {@link IMoleculeFragmenter.ElectronDonationModelOption} enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setElectronDonationModelSetting(IMoleculeFragmenter.ElectronDonationModelOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        //synchronisation with aromaticity model instance done in overridden set() function of the property
        this.electronDonationModelSetting.set(anOption);
    }

    /**
     * Sets the cycle finder setting. The chosen cycle finder algorithm is used for aromaticity detection in
     * preprocessing together with the set electron donation model.
     *
     * @param anOption a constant from the {@link IMoleculeFragmenter.CycleFinderOption} enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setCycleFinderSetting(IMoleculeFragmenter.CycleFinderOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        this.cycleFinderSetting.set(anOption);
    }
    //</editor-fold>
    //
    //<editor-fold desc="IMoleculeFragmenter methods">
    //without the empty line, the code folding does not work properly here...

    @Override
    public List<Property<?>> settingsProperties() {
        return this.settings;
    }

    @Override
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return this.settingNameToTooltipTextMap;
    }

    @Override
    public Map<String, String> getSettingNameToDisplayNameMap() {
        return this.settingNameToDisplayNameMap;
    }

    @Override
    public String getFragmentationAlgorithmName() {
        return CDKCircularFragmenter.ALGORITHM_NAME;
    }

    @Override
    public String getFragmentationAlgorithmDisplayName() {
        return Message.get("CDKCircularFragmenter.displayName");
    }

    @Override
    public IMoleculeFragmenter copy() {
        CDKCircularFragmenter tmpCopy = new CDKCircularFragmenter();
        tmpCopy.setRadiusSetting(this.radiusSetting.get());
        tmpCopy.setIncludeSmallerRadiiSetting(this.includeSmallerRadiiSetting.get());
        tmpCopy.setPreserveStereoSetting(this.preserveStereoSetting.get());
        tmpCopy.setMarkAttachmentsSetting(this.markAttachmentsSetting.get());
        tmpCopy.setApplyAromaticityDetectionSetting(this.applyAromaticityDetectionSetting.get());
        tmpCopy.setCycleFinderSetting((IMoleculeFragmenter.CycleFinderOption) this.cycleFinderSetting.get());
        tmpCopy.setElectronDonationModelSetting((IMoleculeFragmenter.ElectronDonationModelOption) this.electronDonationModelSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.radiusSetting.set(CDKCircularFragmenter.RADIUS_SETTING_DEFAULT);
        this.includeSmallerRadiiSetting.set(CDKCircularFragmenter.INCLUDE_SMALLER_RADII_SETTING_DEFAULT);
        this.preserveStereoSetting.set(CDKCircularFragmenter.PRESERVE_STEREO_SETTING_DEFAULT);
        this.markAttachmentsSetting.set(CDKCircularFragmenter.MARK_ATTACHMENTS_SETTING_DEFAULT);
        this.applyAromaticityDetectionSetting.set(CDKCircularFragmenter.APPLY_AROMATICITY_DETECTION_SETTING_DEFAULT);
        this.cycleFinderSetting.set(CDKCircularFragmenter.CYCLE_FINDER_OPTION_DEFAULT);
        this.electronDonationModelSetting.set(CDKCircularFragmenter.ELECTRON_DONATION_MODEL_OPTION_DEFAULT);
    }

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpCanBeFragmented = this.canBeFragmented(aMolecule);
        if (!tmpCanBeFragmented) {
            throw new IllegalArgumentException("Given molecule cannot be fragmented but should be filtered or preprocessed first.");
        }
        IAtomContainer tmpMoleculeToWorkWith;
        List<IAtomContainer> tmpFragments;
        try {
            if (this.applyAromaticityDetectionSetting.get()) {
                tmpMoleculeToWorkWith = aMolecule.clone();
                if (this.electronDonationModelSetting.get().equals(IMoleculeFragmenter.ElectronDonationModelOption.CDK)
                        || this.electronDonationModelSetting.get().equals(IMoleculeFragmenter.ElectronDonationModelOption.CDK_ALLOWING_EXOCYCLIC)) {
                    //the other aromaticity models do not need atom types to be set
                    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpMoleculeToWorkWith);
                }
                Aromaticity.clear(tmpMoleculeToWorkWith);
                this.aromaticityModelInstance.apply(tmpMoleculeToWorkWith);
            } else {
                tmpMoleculeToWorkWith = aMolecule;
            }
            if (this.includeSmallerRadiiSetting.get()) {
                //adding the atom count once more for the radius = 0 fragments
                tmpFragments = new ArrayList<>(tmpMoleculeToWorkWith.getAtomCount() * (this.radiusSetting.get() + 1));
                for (int i = 0; i <= this.radiusSetting.get(); i++) {
                    //bypass of the radius setting property of this class, cave! Works here because the last iteration restores the previous state
                    this.circularFragmenterInstance.setRadius(i);
                    tmpFragments.addAll(this.circularFragmenterInstance.getCircularFragments(tmpMoleculeToWorkWith));
                }
            } else {
                tmpFragments = this.circularFragmenterInstance.getCircularFragments(tmpMoleculeToWorkWith);
            }
        } catch (Exception anException) {
            throw new IllegalArgumentException("An error occurred during fragmentation: " + anException.toString()
                    + " Molecule Name: " + aMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
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
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        if (tmpShouldBeFiltered) {
            throw new IllegalArgumentException("The given molecule cannot be preprocessed but should be filtered.");
        }
        return aMolecule.clone();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods">
    /**
     * Sets only the aromaticity model instance, not the property! So it is safe for the property to call this method
     * when overriding set().
     *
     * @param anElectronDonation the electron donation model to use
     * @param aCycleFinder the cycle finder algorithm to use
     * @throws NullPointerException if any parameter is null
     */
    private void setAromaticityInstance(ElectronDonation anElectronDonation, CycleFinder aCycleFinder) throws NullPointerException {
        Objects.requireNonNull(anElectronDonation, "Given electron donation model is null.");
        Objects.requireNonNull(aCycleFinder, "Given cycle finder algorithm is null.");
        this.aromaticityModelInstance = new Aromaticity(anElectronDonation, aCycleFinder);
    }

    /**
     * Sets only the cycle finder instance, not the property! Calling method needs to update the aromaticity model
     * afterwards!
     *
     * @param anOption a constant from the {@link IMoleculeFragmenter.CycleFinderOption} enum
     * @throws NullPointerException if the given option is null
     */
    private void setCycleFinderInstance(IMoleculeFragmenter.CycleFinderOption anOption) throws NullPointerException {
        //Developer comment: the switch way is used instead of having the CycleFinder objects as variables of the enum constants
        // to not have static objects becoming bottlenecks in parallelization.
        Objects.requireNonNull(anOption, "Given option is null.");
        switch (anOption) {
            case IMoleculeFragmenter.CycleFinderOption.ALL:
                this.cycleFinderInstance = Cycles.or(Cycles.all(), CDKCircularFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.MCB:
                this.cycleFinderInstance = Cycles.or(Cycles.mcb(), CDKCircularFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.RELEVANT:
                this.cycleFinderInstance = Cycles.or(Cycles.relevant(), CDKCircularFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.ESSENTIAL:
                this.cycleFinderInstance = Cycles.or(Cycles.essential(), CDKCircularFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.EDGE_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.edgeShort(), CDKCircularFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.VERTEX_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.vertexShort(), CDKCircularFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.TRIPLET_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.tripletShort(), CDKCircularFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.CDK_AROMATIC_SET:
                this.cycleFinderInstance = Cycles.cdkAromaticSet();
                break;
            default:
                throw new IllegalArgumentException("Undefined cycle finder option.");
        }
    }

    /**
     * Sets only the electron donation instance, not the property! Calling method needs to update the aromaticity model
     * afterwards!
     *
     * @param anOption a constant from the {@link IMoleculeFragmenter.ElectronDonationModelOption} enum
     * @throws NullPointerException if the given option is null
     */
    private void setElectronDonationInstance(IMoleculeFragmenter.ElectronDonationModelOption anOption) throws NullPointerException {
        //Developer comment: the switch way is used instead of having the ElectronDonation objects as variables of the enum constants
        // to not have static objects becoming bottlenecks in parallelization.
        Objects.requireNonNull(anOption, "Given option is null.");
        switch (anOption) {
            case IMoleculeFragmenter.ElectronDonationModelOption.CDK:
                this.electronDonationInstance = Aromaticity.Model.CDK_AtomTypes;
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.DAYLIGHT:
                this.electronDonationInstance = Aromaticity.Model.Daylight;
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.CDK_ALLOWING_EXOCYCLIC:
                this.electronDonationInstance = ElectronDonation.cdkAllowingExocyclic();
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.CDK_1X:
                this.electronDonationInstance = Aromaticity.Model.CDK_1x;
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.CDK_2X:
                this.electronDonationInstance = Aromaticity.Model.CDK_2x;
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.MDL:
                this.electronDonationInstance = Aromaticity.Model.Mdl;
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.OPEN_SMILES:
                this.electronDonationInstance = Aromaticity.Model.OpenSmiles;
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.PI_BONDS:
                this.electronDonationInstance = Aromaticity.Model.PiBonds;
                break;
            default:
                throw new IllegalArgumentException("Undefined electron donation model option.");
        }
    }
    //</editor-fold>
}
