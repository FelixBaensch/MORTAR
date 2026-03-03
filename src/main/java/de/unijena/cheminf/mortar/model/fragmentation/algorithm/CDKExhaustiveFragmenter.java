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
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;

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
                Message.get("CDKExhaustiveFragmenter.Saturation.Hydrogen.displayName"),
                Message.get("CDKExhaustiveFragmenter.Saturation.Hydrogen.tooltip")
        ),
        /**
         * Fragments will be saturated with R atoms.
         */
        R_SATURATED_FRAGMENTS(
                Message.get("CDKExhaustiveFragmenter.Saturation.Rest.displayName"),
                Message.get("CDKExhaustiveFragmenter.Saturation.Rest.tooltip")
        ),
        /**
         * Fragments will be returned in their unsaturated form
         * (no additional hydrogen atoms). The unsaturated atoms are the atoms
         * of the split bonds.
         */
        UNSATURATED_FRAGMENTS (
                Message.get("CDKExhaustiveFragmenter.Saturation.Unsaturated.displayName"),
                Message.get("CDKExhaustiveFragmenter.Saturation.Unsaturated.tooltip")
        );
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
        private SaturationDisplay(String aDisplayName, String aTooltip) {
            this.displayName = aDisplayName;
            this.tooltip = aTooltip;
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
    /**
     * Specifies the type of SMILES generation options to use.
     */
    public enum SmilesSettingDisplay implements IDisplayEnum {
        /**
         * Absolute SMILES: canonical SMILES with stereochemistry, atomic masses.
         */
        ABSOLUTE(
                SmiFlavor.Absolute,
                Message.get("CDKExhaustiveFragmenter.Smiles.Absolute.displayName"),
                Message.get("CDKExhaustiveFragmenter.Smiles.Absolute.tooltip")
        ),
        /**
         * Canonical SMILES: Output SMILES in a canonical order.
         */
        CANONICAL(
                SmiFlavor.Canonical,
                Message.get("CDKExhaustiveFragmenter.Smiles.Canonical.displayName"),
                Message.get("CDKExhaustiveFragmenter.Smiles.Canonical.tooltip")
        ),
        /**
         * Default SMILES: Default SMILES output write Stereochemistry, Atomic Mass, and CXSMILES layers.
         */
        DEFAULT(
                SmiFlavor.Default,
                Message.get("CDKExhaustiveFragmenter.Smiles.Default.displayName"),
                Message.get("CDKExhaustiveFragmenter.Smiles.Default.tooltip")
        ),
        /**
         * Generic SMILES: Output non-canonical SMILES without stereochemistry, atomic masses.
         */
        GENERIC(
                SmiFlavor.Generic,
                Message.get("CDKExhaustiveFragmenter.Smiles.Generic.displayName"),
                Message.get("CDKExhaustiveFragmenter.Smiles.Generic.tooltip")
        ),
        /**
         * Stereo SMILES: Output supported stereochemistry types.
         */
        STEREO(
                SmiFlavor.Stereo,
                Message.get("CDKExhaustiveFragmenter.Smiles.Stereo.displayName"),
                Message.get("CDKExhaustiveFragmenter.Smiles.Stereo.tooltip")
        ),
        /**
         * Universal SMILES: Output canonical SMILES with stereochemistry and atomic masses, This output uses the InChI
         * labeling algorithm to generate a 'Universal SMILES' [O'Boyle, Noel. Journal of Cheminformatics. 2012. 4].
         */
        UNIVERSAL_SMILES(
                SmiFlavor.UniversalSmiles,
                Message.get("CDKExhaustiveFragmenter.Smiles.UniversalSmiles.displayName"),
                Message.get("CDKExhaustiveFragmenter.Smiles.UniversalSmiles.tooltip")
        ),
        /**
         * Writes aromatic atoms as lower case letters.
         */
        USE_AROMATIC_SYMBOLS(
                SmiFlavor.UseAromaticSymbols,
                Message.get("CDKExhaustiveFragmenter.Smiles.UseAromaticSymbols.displayName"),
                Message.get("CDKExhaustiveFragmenter.Smiles.UseAromaticSymbols.tooltip")
        );
        /**
         * Underlying CDK SmiFlavor value.
         */
        private final int smilesFlavor;
        /**
         * Language-specific name for display in GUI.
         */
        private final String displayName;
        /**
         * Language-specific tooltip text for display in GUI.
         */
        private final String tooltip;
        /**
         * Construct a SmilesSettingDisplay.
         *
         * @param aSmilesFlavor integer code representing the CDK SmiFlavor
         * @param aDisplayName localized display name (from resource bundle)
         * @param aTooltip localized tooltip text (from resource bundle)
         */
        private SmilesSettingDisplay(int aSmilesFlavor, String aDisplayName, String aTooltip) {
            this.smilesFlavor = aSmilesFlavor;
            this.displayName = aDisplayName;
            this.tooltip = aTooltip;
        }
        /**
         * Get the CDK SMILES flavor value associated with this enum constant.
         *
         * @return integer SmiFlavor value
         */
        public int getSmilesFlavor() {
            return this.smilesFlavor;
        }
        @Override
        public String getDisplayName() {
            return this.displayName;
        }
        @Override
        public String getTooltipText() {
            return this.tooltip;
        }
    }
    //
    //<editor-fold desc="Public static final variables">
    /**
     * The default value for the minimum fragment size used for the fragmentation.
     */
    public static final int DEFAULT_MINIMUM_FRAGMENT_SIZE = 6;
    //
    /**
     * The name of the algorithm used for fragmentation.
     */
    public static final int DEFAULT_MIN_FRAG_SIZE = 6;
    /**
     * The default setting for saturation is {{@code UNSATURATED_Fragments}}.
     */
    public static final SaturationDisplay DEFAULT_SATURATION =  SaturationDisplay.UNSATURATED_FRAGMENTS;
    /**
     * Default smiles flavor to be used for the {{@link SmilesGenerator}}.
     */
    public static final SmilesSettingDisplay DEFAULT_SMILES_SETTING =
                    SmilesSettingDisplay.ABSOLUTE;
    // assuming each fragment is unique (as if there was no deduplication)
    // 27 would be the maximum tree depth to hold all fragments in the
    // hashmap.
    private static final int DEFAULT_INCLUSIVE_MAX_TREE_DEPTH = 27;
    private static final boolean DEFAULT_COPY_STEREO_INFO = false;
    /**
     * The name of this fragmenter.
     */
    public static final String ALGORITHM_NAME = "Exhaustive Fragmenter";
    //</editor-fold>
    //
    //<editor-fold desc="Private final variables">

    /**
     * The smiles flavor to use for the generator.
     */
    // TODO: Find a good way to present the options of setting AND COMBINING SmiFlavors
    //       for the user:
    //          1. option: make a preset of flavor combination and expose these via enum
    //
    // TODO:
    // additionally it would be nice to get some kind of relationship
    // between for example preserve stereo and the type of smilesGenerator
    // because the smilesGnerator needs to also be capable of producing
    // stereo information if one wants to try to preserve it.
    // Idea:
    //      1. if one turns preserve stereo on/off it automatically makes
    //          a new smilesGenerator with or without the most appropriate
    //          stereo information setting
    //
    //
    // private final SimpleIntegerProperty smilesFlavorSetting;
    //
    /**
     * The maximum tree depth which represents the maximum number of bonds that will be split in a fragmentation.
     */
    private final SimpleIntegerProperty inclusiveMaxTreeDepthSetting;
    //
    /**
     * The saturation setting specifying the {{@link org.openscience.cdk.fragment.ExhaustiveFragmenter.Saturation}}
     */
    private final SimpleIDisplayEnumConstantProperty saturationSetting;
    //
    /**
     * The smiles setting for the {{@link SmilesGenerator}} specifying the configuration
     * with the {{@link SmiFlavor}}.
     */
    private final SimpleIDisplayEnumConstantProperty smilesSetting;
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

        this.inclusiveMaxTreeDepthSetting = new SimpleIntegerProperty(this,
                "Inclusive Maximum Tree Depth",
                DEFAULT_INCLUSIVE_MAX_TREE_DEPTH) {
            @Override
            public void set(int newValue) {
                if (newValue > 0 && newValue < 31) {
                    super.set(newValue);
                }
                else {
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
                    super.set(newValue);
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

        this.smilesSetting = new SimpleIDisplayEnumConstantProperty(this,
                "Smiles Display Setting",
                DEFAULT_SMILES_SETTING,
                CDKExhaustiveFragmenter.SmilesSettingDisplay.class) {

            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    if (newValue instanceof SmilesSettingDisplay tmpSmilesFlavor) {
                        // this.smilesGenerator = new SmilesGenerator(tmpSmilesFlavor.smilesFlavor);
                        super.set(tmpSmilesFlavor);
                    } else {
                        throw new IllegalArgumentException("");
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
                DEFAULT_COPY_STEREO_INFO) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
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

        this.settingNameTooltipTextMap.put(saturationSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.saturationSetting.tooltip"));
        this.settingNameDisplayNameMap.put(saturationSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.saturationSetting.displayName"));

        this.settingNameTooltipTextMap.put(preserveStereoSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.preserveStereo.tooltip"));
        this.settingNameDisplayNameMap.put(preserveStereoSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.preserveStereo.displayName"));

        this.settingNameTooltipTextMap.put(smilesSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.smilesGenerator.tooltip"));
        this.settingNameDisplayNameMap.put(smilesSetting.getName(),
                Message.get("CDKExhaustiveFragmenter.smilesGenerator.displayName"));

        this.settings = new ArrayList<>(tmpNumberOfSettingsForTooltipMapSize);
        this.settings.add(minimumFragmentSizeSetting);
        this.settings.add(inclusiveMaxTreeDepthSetting);
        this.settings.add(saturationSetting);
        this.settings.add(preserveStereoSetting);
        this.settings.add(smilesSetting);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     * Returns the setting for the minimum fragment size.
     *
     * @return the setting for the minimum fragment size.
     */
    public SimpleIntegerProperty getMinimumFragmentSizeSettingProperty() {
        return minimumFragmentSizeSetting;
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
        return settings;
    }

    /**
     * Indicates whether stereochemistry should be preserved when generating SMILES/fragments.
     *
     * @return true if stereochemistry preservation is enabled; false otherwise
     */
    public boolean isPreserveStereoSetting() {
        return preserveStereoSetting.get();
    }

    public SimpleBooleanProperty preserveStereoSettingProperty() {
        return preserveStereoSetting;
    }

    /**
     * Gets the currently selected saturation display option.
     *
     * @return the selected {@link IDisplayEnum} value (e.g., HYDROGEN_SATURATED_FRAGMENTS,
     *         R_SATURATED_FRAGMENTS, UNSATURATED_FRAGMENTS)
     */
    public ExhaustiveFragmenter.Saturation getSaturationSetting() {
        return ExhaustiveFragmenter.Saturation.valueOf(this.saturationSetting.getName());
    }

    public SimpleIDisplayEnumConstantProperty saturationSettingProperty() {
        return saturationSetting;
    }

    /**
     * Gets the inclusive maximum tree depth used by the exhaustive fragmenter. This value is inclusive:
     * a value of N means the fragmenter will include nodes at depth N.
     *
     * @return the inclusive maximum tree depth as an int
     */
    public int getInclusiveMaxTreeDepthSetting() {
        return inclusiveMaxTreeDepthSetting.get();
    }

    public SimpleIntegerProperty inclusiveMaxTreeDepthSettingProperty() {
        return inclusiveMaxTreeDepthSetting;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     * Sets the minimum fragment size.
     *
     * @param minimumFragmentSize the new minimum fragment size.
     */
    public void setMinimumFragmentSize(int minimumFragmentSize) {
        this.minimumFragmentSizeSetting.set(minimumFragmentSize);
    }

    /**
     * Enable or disable stereochemistry preservation when generating SMILES/fragments.
     *
     * @param preserve true to preserve stereochemistry; false to ignore it
     */
    public void setPreserveStereoSetting(boolean preserve) {
        this.preserveStereoSetting.set(preserve);
    }

    /**
     * Set the saturation display option.
     *
     * @param saturation the saturation display enum constant to use; must not be null
     * @throws NullPointerException if saturation is null
     */
    public void setSaturationSetting(ExhaustiveFragmenter.Saturation saturation) {
        Objects.requireNonNull(saturation, "saturation must not be null");
        this.saturationSetting.set(SaturationDisplay.valueOf(saturation.name()));
    }

    /**
     * Set the inclusive maximum tree depth for the exhaustive fragmenter.
     * The value is inclusive: a value of N means nodes at depth N are included.
     *
     * @param depth the new inclusive maximum tree depth; must be >= 0
     * @throws IllegalArgumentException if depth is negative
     */
    public void setInclusiveMaxTreeDepthSetting(int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("inclusiveMaxTreeDepth must be >= 0");
        }
        this.inclusiveMaxTreeDepthSetting.set(depth);
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
    //
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
