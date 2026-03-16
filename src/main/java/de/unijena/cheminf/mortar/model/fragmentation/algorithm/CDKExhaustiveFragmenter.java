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

import org.openscience.cdk.fragment.ExhaustiveFragmenter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesGenerator;

import java.util.ArrayList;
import java.util.Collections;
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
 * from the CDK available for MORTAR. It has a performance of O(n!) where n is the number of splittable bonds. Splittable
 * bonds are defined as non-ring, single bonds that are not connected to non-terminal atoms. Here, Non-terminal refers
 * to atoms that are connected to at least one other atom, which itself is connected to other atoms.
 *
 * @author Tom Weiß
 * @version 1.0.0.0
 */
public class CDKExhaustiveFragmenter implements IMoleculeFragmenter {
    //<editor-fold desc="Enum FragmentSaturationOption">
    /**
     * Specifies whether generated fragments should be saturated (hydrogens added, or R-groups added)
     * or unsaturated.
     */
    public enum SaturationDisplay implements IDisplayEnum {
        /**
         * Fragments will be returned in their saturated form
         * (implicit hydrogen atoms added).
         */
        HYDROGEN_SATURATED_FRAGMENTS(
                ExhaustiveFragmenter.Saturation.HYDROGEN_SATURATED_FRAGMENTS,
                Message.get("CDKExhaustiveFragmenter.Saturation.Hydrogen.displayName"),
                Message.get("CDKExhaustiveFragmenter.Saturation.Hydrogen.tooltip")
        ),
        /**
         * Fragments will be saturated with R atoms.
         */
        R_SATURATED_FRAGMENTS(
                ExhaustiveFragmenter.Saturation.R_SATURATED_FRAGMENTS,
                Message.get("CDKExhaustiveFragmenter.Saturation.Rest.displayName"),
                Message.get("CDKExhaustiveFragmenter.Saturation.Rest.tooltip")
        ),
        /**
         * Fragments will be returned in their unsaturated form
         * (no additional hydrogen atoms). The unsaturated atoms are the atoms
         * of the split bonds.
         */
        UNSATURATED_FRAGMENTS (
                ExhaustiveFragmenter.Saturation.UNSATURATED_FRAGMENTS,
                Message.get("CDKExhaustiveFragmenter.Saturation.Unsaturated.displayName"),
                Message.get("CDKExhaustiveFragmenter.Saturation.Unsaturated.tooltip")
        );
        /**
         * The actual value of the {@link org.openscience.cdk.fragment.ExhaustiveFragmenter.Saturation}.
         */
        private final ExhaustiveFragmenter.Saturation saturationValue;
        /**
         * Language-specific name for display in GUI.
         */
        private final String displayName;
        /**
         * Language-specific tooltip text for display in GUI.
         */
        private final String tooltip;
        /**
         * Constructor.
         *
         * @param aDisplayName display name
         * @param aTooltip tooltip text
         */
        private SaturationDisplay(ExhaustiveFragmenter.Saturation aSaturationValue, String aDisplayName, String aTooltip) {
            this.saturationValue = aSaturationValue;
            this.displayName = aDisplayName;
            this.tooltip = aTooltip;
        }
        //
        /**
         * Gets the wrapped {@link org.openscience.cdk.fragment.ExhaustiveFragmenter.Saturation} value.
         *
         * @return the wrapped {@link org.openscience.cdk.fragment.ExhaustiveFragmenter.Saturation} value.
         */
        public ExhaustiveFragmenter.Saturation getSaturationValue() {
            return this.saturationValue;
        }
        //
        @Override
        public String getDisplayName() {
            return this.displayName;
        }
        //
        @Override
        public String getTooltipText() {
            return this.tooltip;
        }
    }
    //
    //<editor-fold desc="Public static final variables">
    /**
     * The default threshold at which molecules will be filtered out for the fragmentation.
     */
    public static final int DEFAULT_THRESHOLD_FOR_SPLITTABLE_BONDS = 31;
    /**
     * The default value for the minimum fragment size used for the fragmentation.
     */
    public static final int DEFAULT_MINIMUM_FRAGMENT_SIZE = 6;
    //
    /**
     * The default setting for saturation is {@code UNSATURATED_Fragments}.
     */
    public static final SaturationDisplay DEFAULT_SATURATION =  SaturationDisplay.UNSATURATED_FRAGMENTS;
    //
    /** Default number of bonds that get split in one fragmentation.
     * This value is based on the assumption each fragment is unique (as if there was no deduplication)
     * this 27 would be the maximum tree depth to hold all fragments in the
     * hashmap.
     */
    public static final int DEFAULT_INCLUSIVE_MAX_TREE_DEPTH = 27;
    /**
     * Do not copy stereochemistry information by default
     */
    public static final boolean DEFAULT_PRESERVE_STEREO_INFO = false;
    //
    /**
     * The maximum number of bonds split in one fragmentation.
     */
    public static final int MAX_TREE_DEPTH_LIMIT = 31;
    //
    /**
     * The name of this fragmenter.
     */
    public static final String ALGORITHM_NAME = "Exhaustive Fragmenter";
    //</editor-fold>
    //
    //<editor-fold desc="Private final variables">
    /**
     * The threshold of the (inclusive) number of splittable bonds one molecule can contain before being filtered.
     */
    private final SimpleIntegerProperty inclusiveSplittableBondsThreshold;
    /**
     * The maximum tree depth which represents the maximum number of bonds that will be split in a fragmentation.
     */
    private final SimpleIntegerProperty inclusiveMaxTreeDepthSetting;
    //
    /**
     * The saturation setting specifying the {@link org.openscience.cdk.fragment.ExhaustiveFragmenter.Saturation}
     */
    private final SimpleIDisplayEnumConstantProperty saturationSetting;
    //
    /**
     * Whether to try to conserve the stereochemistry information of the molecules to split.
     */
    private final SimpleBooleanProperty preserveStereoSetting;
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

    private SmilesGenerator smilesGenerator;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialized with their default values as declared in the respective public constants.
     */
    public CDKExhaustiveFragmenter() {
        int tmpNumberOfSettingsForTooltipMapSize = 5;
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
                } else {
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

        this.inclusiveSplittableBondsThreshold = new SimpleIntegerProperty(this,
                "Inclusive number of the filter threshold of splittable bonds",
                CDKExhaustiveFragmenter.DEFAULT_THRESHOLD_FOR_SPLITTABLE_BONDS) {
            @Override
            public void set(int newValue) {
                if (newValue > 0) {
                    super.set(newValue);
                } else {
                    IllegalArgumentException anException = new IllegalArgumentException("The threshold of splittable bonds can not be zero");
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

        this.inclusiveMaxTreeDepthSetting = new SimpleIntegerProperty(this,
                "Inclusive Maximum Tree Depth",
                DEFAULT_INCLUSIVE_MAX_TREE_DEPTH) {
            @Override
            public void set(int newValue) {
                if (newValue > 0 && newValue < CDKExhaustiveFragmenter.MAX_TREE_DEPTH_LIMIT) {
                    CDKExhaustiveFragmenter.this.cdkEFInstance.setInclusiveMaxTreeDepth(newValue);
                    super.set(newValue);
                } else {
                    IllegalArgumentException anException = new IllegalArgumentException(
                            "The inclusive max tree depth must be positive and smaller than 31" +
                            "to mitigate the runtime of O(n!)");
                    CDKExhaustiveFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    throw anException;
                }
            }
        };

        this.saturationSetting = new SimpleIDisplayEnumConstantProperty(this,
                "Saturation Display Setting",
                DEFAULT_SATURATION,
                CDKExhaustiveFragmenter.SaturationDisplay.class) {

            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    if (newValue instanceof SaturationDisplay newSaturation) {
                        CDKExhaustiveFragmenter.this.cdkEFInstance.setSaturationSetting(newSaturation.getSaturationValue());
                        super.set(newValue);
                    }
                } catch (NullPointerException | IllegalArgumentException anException) {
                    CDKExhaustiveFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    throw anException;
                }
            }
        };

        this.preserveStereoSetting = new SimpleBooleanProperty(this,
                "Preserve Stereo Information",
                DEFAULT_PRESERVE_STEREO_INFO) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                CDKExhaustiveFragmenter.this.cdkEFInstance.setPreserveStereo(newValue);
            }
        };

        this.settingNameTooltipTextMap.put(minimumFragmentSizeSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.minFragmentSize.tooltip"));
        this.settingNameDisplayNameMap.put(minimumFragmentSizeSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.minFragmentSize.displayName"));

        this.settingNameTooltipTextMap.put(inclusiveMaxTreeDepthSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.inclusiveMaxTreeDepth.tooltip"));
        this.settingNameDisplayNameMap.put(inclusiveMaxTreeDepthSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.inclusiveMaxTreeDepth.displayName"));

        this.settingNameTooltipTextMap.put(inclusiveSplittableBondsThreshold.getName(),
                Message.get("CDKExhaustiveFragmenter.splittableBondsThreshold.tooltip"));
        this.settingNameDisplayNameMap.put(inclusiveSplittableBondsThreshold.getName(),
                Message.get("CDKExhaustiveFragmenter.splittableBondsThreshold.displayName"));

        this.settingNameTooltipTextMap.put(saturationSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.saturationSetting.tooltip"));
        this.settingNameDisplayNameMap.put(saturationSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.saturationSetting.displayName"));

        this.settingNameTooltipTextMap.put(preserveStereoSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.preserveStereo.tooltip"));
        this.settingNameDisplayNameMap.put(preserveStereoSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.preserveStereo.displayName"));


        this.settings = new ArrayList<>(tmpNumberOfSettingsForTooltipMapSize);
        this.settings.add(minimumFragmentSizeSetting);
        this.settings.add(inclusiveSplittableBondsThreshold);
        this.settings.add(inclusiveMaxTreeDepthSetting);
        this.settings.add(saturationSetting);
        this.settings.add(preserveStereoSetting);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     * Returns the setting for the threshold of splittable bonds.
     * If fragments have more splittable bonds then the value of this setting
     * they will not be fragmented.
     *
     * @return the setting for the minimum fragment size.
     */
    public SimpleIntegerProperty inclusiveSplittableBondsThresholdSettingProperty() {
        return this.inclusiveSplittableBondsThreshold;
    }

    /**
     * Returns the threshold of splittable bonds for the filtering.
     *
     * @return the currently set minimum fragment size.
     */
    public int getInclusiveThresholdForSplittableBonds() {
        return this.inclusiveSplittableBondsThreshold.get();
    }
    /**
     * Returns the setting for the minimum fragment size.
     *
     * @return the setting for the minimum fragment size.
     */
    public SimpleIntegerProperty getMinimumFragmentSizeSettingProperty() {
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
    /**
     * Returns the list of all setting properties exposed by this component.
     *
     * @return an unmodifiable list of property objects representing the configurable settings;
     */
    public List<Property<?>> getSettings() {
        return this.settings;
    }

    /**
     * Indicates whether stereochemistry should be preserved when generating SMILES/fragments.
     *
     * @return true if stereochemistry preservation is enabled; false otherwise.
     */
    public boolean getPreserveStereoSetting() {
        return this.preserveStereoSetting.get();
    }

    public SimpleBooleanProperty preserveStereoSettingProperty() {
        return this.preserveStereoSetting;
    }

    /**
     * Gets the currently selected saturation display option.
     *
     * @return the selected {@link SaturationDisplay}.
     */
    public SaturationDisplay getSaturationSetting() {
        return (SaturationDisplay) this.saturationSetting.get();
    }

    public SimpleIDisplayEnumConstantProperty saturationSettingProperty() {
        return this.saturationSetting;
    }

    /**
     * Gets the inclusive maximum tree depth used by the exhaustive fragmenter. This value is inclusive:
     * a value of N means the fragmenter will include nodes at depth N.
     *
     * @return the inclusive maximum tree depth.
     */
    public int getInclusiveMaxTreeDepthSetting() {
        return this.inclusiveMaxTreeDepthSetting.get();
    }

    public SimpleIntegerProperty inclusiveMaxTreeDepthSettingProperty() {
        return this.inclusiveMaxTreeDepthSetting;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     * Sets the minimum fragment size.
     *
     * @param minimumFragmentSize the new minimum fragment size.
     * @throws IllegalArgumentException if the fragment size is negative.
     */
    public void setMinimumFragmentSize(int minimumFragmentSize) {
        if (minimumFragmentSize <= 0) {
            throw new IllegalArgumentException(
                    "Minimum fragment size must be positive");
        }
        this.minimumFragmentSizeSetting.set(minimumFragmentSize);
    }

    /**
     * Sets the threshold for filtering. Molecules with more than the specified number here
     * will not be fragmented.
     *
     * @param aThresholdForSplittableBonds the maximum number of splittable bonds to still be fragmented.
     * @throws IllegalArgumentException if the value is less than zero.
     */
    public void setThresholdForSplittableBonds(int aThresholdForSplittableBonds) {
        if (aThresholdForSplittableBonds <= 0) {
            throw new IllegalArgumentException(
                    "Threshold for splittable bonds must be positive");
        }
        this.inclusiveSplittableBondsThreshold.set(aThresholdForSplittableBonds);
    }

    /**
     * Enable or disable stereochemistry preservation when generating SMILES/fragments.
     *
     * @param preserve true to preserve stereochemistry; false to ignore it.
     */
    public void setPreserveStereoSetting(boolean preserve) {
        this.preserveStereoSetting.set(preserve);
    }

    /**
     * Set the saturation display option using a CDK saturation enum value.
     *
     * @param aSaturation the {@link org.openscience.cdk.fragment.ExhaustiveFragmenter.Saturation}
     *                   enum constant to convert; must not be null
     * @throws NullPointerException if saturation is null.
     * @throws IllegalArgumentException if the saturation enum cannot be converted to SaturationDisplay.
     */
    public void setSaturationSetting(ExhaustiveFragmenter.Saturation aSaturation) {
        Objects.requireNonNull(aSaturation, "saturation must not be null");
        try {
            this.saturationSetting.set(SaturationDisplay.valueOf(aSaturation.name()));
        } catch (IllegalArgumentException anException) {
            LOGGER.log(Level.WARNING, "Failed to convert saturation: " + aSaturation.name(), anException);
            throw new IllegalArgumentException("Unsupported saturation type: " + aSaturation.name(), anException);
        }
    }

    /**
     * Set the inclusive maximum tree depth for the exhaustive fragmenter.
     * The value is inclusive: a value of N means nodes at depth N are included.
     *
     * @param depth the new inclusive maximum tree depth; must be >= 0.
     * @throws IllegalArgumentException if depth is negative.
     */
    public void setInclusiveMaxTreeDepthSetting(int depth) {
        if (depth < 0 || depth >= MAX_TREE_DEPTH_LIMIT) {
            throw new IllegalArgumentException(
                    "inclusiveMaxTreeDepth must be >= 0 and < " + MAX_TREE_DEPTH_LIMIT);
        }
        this.inclusiveMaxTreeDepthSetting.set(depth);
    }
    //</editor-fold>
    //
    //<editor-fold desc="IMoleculeFragmenter methods">
    @Override
    public List<Property<?>> settingsProperties() {
        return Collections.unmodifiableList(this.settings);
    }

    @Override
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return Collections.unmodifiableMap(this.settingNameTooltipTextMap);
    }

    @Override
    public Map<String, String> getSettingNameToDisplayNameMap() {
        return Collections.unmodifiableMap(this.settingNameDisplayNameMap);
    }

    @Override
    public String getFragmentationAlgorithmName() {
        return CDKExhaustiveFragmenter.ALGORITHM_NAME;
    }

    @Override
    public String getFragmentationAlgorithmDisplayName() {
        return Message.get("CDKExhaustiveFragmenter.displayName");
    }
    //
    @Override
    public IMoleculeFragmenter copy() {
        CDKExhaustiveFragmenter tmpCopy = new CDKExhaustiveFragmenter();
        tmpCopy.minimumFragmentSizeSetting.set(this.minimumFragmentSizeSetting.get());
        tmpCopy.inclusiveSplittableBondsThreshold.set(this.inclusiveSplittableBondsThreshold.get());
        tmpCopy.inclusiveMaxTreeDepthSetting.set(this.inclusiveMaxTreeDepthSetting.get());
        tmpCopy.saturationSetting.set(this.saturationSetting.get());
        tmpCopy.preserveStereoSetting.set(this.preserveStereoSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.minimumFragmentSizeSetting.set(CDKExhaustiveFragmenter.DEFAULT_MINIMUM_FRAGMENT_SIZE);
        this.inclusiveSplittableBondsThreshold.set(CDKExhaustiveFragmenter.DEFAULT_THRESHOLD_FOR_SPLITTABLE_BONDS);
        this.inclusiveMaxTreeDepthSetting.set(CDKExhaustiveFragmenter.DEFAULT_INCLUSIVE_MAX_TREE_DEPTH);
        this.saturationSetting.set(CDKExhaustiveFragmenter.DEFAULT_SATURATION);
        this.preserveStereoSetting.set(CDKExhaustiveFragmenter.DEFAULT_PRESERVE_STEREO_INFO);
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
        // a rough estimation of the number of unique fragments produced by this fragmenter.
        int fragmentListSizeEstimation = tmpMoleculeClone.getAtomCount() / 2;
        List<IAtomContainer> tmpFragments = new ArrayList<>(fragmentListSizeEstimation);
        try {
            this.cdkEFInstance.generateFragments(tmpMoleculeClone);
            tmpFragments.addAll(List.of(this.cdkEFInstance.getFragmentsAsContainers()));
        } catch (Exception anException) {
            throw new IllegalArgumentException("An error occurred during fragmentation: " + anException.toString() + " Molecule Name: " + aMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        }
        return tmpFragments;
    }

    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        if (Objects.isNull(aMolecule) || aMolecule.isEmpty()) {
            return true;
        }
        if (ExhaustiveFragmenter.getSplittableBonds(aMolecule).length > this.inclusiveSplittableBondsThreshold.get()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (!this.getPreserveStereoSetting() && aMolecule.stereoElements().iterator().hasNext()) {
            return true;
        }
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
        IAtomContainer tmpMoleculeClone = aMolecule.clone();
        if (this.shouldBePreprocessed(aMolecule)) {
            tmpMoleculeClone.setStereoElements(new ArrayList<>(0));
        }
        return tmpMoleculeClone;
    }
    //</editor-fold>
}
