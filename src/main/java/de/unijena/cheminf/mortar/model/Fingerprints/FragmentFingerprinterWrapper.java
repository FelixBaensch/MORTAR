/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.cheminf.mortar.model.Fingerprints;

import de.unijena.cheminf.fragmentFingerprinter.FragmentFingerprinter;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Alert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class that makes the
 * <a href="https://github.com/JonasSchaub/FragmentFingerprints">FragmentFingerprints</a> available in MORTAR.
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public class FragmentFingerprinterWrapper implements IMortarFingerprinter {
    //<editor-fold desc="Enum FingerprintTyp">
    /**
     * Enum for options concerning the fingerprint type. Bit or Count fingerprints.
     */
    public static enum FingerprintTyp {
        /**
         * Bit fingerprints
         */
        BIT_FINGERPRINTS,
        /**
         * Count fingerprints
         */
        COUNT_FINGERPRINTS;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public static final constants">
    /**
     * Name of the fingerprinter used in this Wrapper class
     */
    public static final String FINGERPRINTER_NAME = "Fragment Fingerprinter";
    /**
     * Default fingerprint typ
     */
    public static final FingerprintTyp COUNT_FINGERPRINTS_DEFAULT = FingerprintTyp.COUNT_FINGERPRINTS;
    /**
     * Default fingerprint frequency threshold value
     */
    private static final int DEFAULT_FINGERPRINT_FREQUENCY_THRESHOLD = 1;
    //</editor-fold>
    //
    //<editor-fold desc="Private final constants">
    /**
     * Export bit fingerprints
     */
    private final String EXPORT_BIT_FINGERPRINTS = "Export bit fingerprints";
    /**
     * Export count fingerprints
     */
    private final String EXPORT_COUNT_FINGERPRINTS = "Export count fingerprints";
    //</editor-fold>
    //
    //<editor-fold desc="Private final class variables">
    /**
     * A property that has a constant name from FingerprinterTyp enum as value
     */
    private final SimpleEnumConstantNameProperty fingerprintTyp;
    /**
     * Property wrapping the 'Fragment fingerprinter dimensionality' setting of the fragment fingerprinter
     */
    private final SimpleIntegerProperty fingerprintDimensionality;
    /**
     * Property warpping the 'Fragment Fingerprinter Frequency Threshold' setting
     */
    private final SimpleIntegerProperty fingerprintFrequencyThreshold;
    /**
     * All settings of this fingerprinter, encapsulated in JavaFX properties for binding in GUI.
     */
    private final List<Property> settings;
    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private final HashMap<String, String> settingNameTooltipTextMap;
    /**
     * Logger of this class.
     */
    private final Logger logger = Logger.getLogger(FragmentFingerprinterWrapper.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their default values as declared in the respective public constants.
     */
    public FragmentFingerprinterWrapper() {
        int tmpNumberOfSettings = 3;
        this.settings = new ArrayList<>(tmpNumberOfSettings);
        int tmpInitialCapacityForSettingNameToolTipTextMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpNumberOfSettings,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap<>(tmpInitialCapacityForSettingNameToolTipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.fingerprintTyp = new SimpleEnumConstantNameProperty(this, "Fragment Fingerprint Typ",
                FragmentFingerprinterWrapper.COUNT_FINGERPRINTS_DEFAULT.name(), FragmentFingerprinterWrapper.FingerprintTyp.class) {
            @Override
            public void set(String newValue) {
                super.set(newValue);
            }
        };
        this.settings.add(this.fingerprintTyp);
        this.settingNameTooltipTextMap.put(this.fingerprintTyp.getName(), Message.get("FragmentFingerprinterWrapper.fingerprintTyp.tooltip"));
        this.fingerprintDimensionality = new SimpleIntegerProperty(this, "Fragment Fingerprint Dimensionality") {
            @Override
            public void set(int newValue) throws IllegalArgumentException {
                if(FragmentFingerprinterWrapper.this.isLegalFingerprintDimensionality(newValue)) {
                    super.set(newValue);
                } else {
                    IllegalArgumentException tmpException = new IllegalArgumentException("An illegal fingerprint dimensionality number was given: " + newValue);
                    FragmentFingerprinterWrapper.this.logger.log(Level.WARNING, tmpException.toString(), tmpException);
                    GuiUtil.guiExceptionAlert(Message.get("FragmentFingerprinterWrapper.Error.invalidFingerprintDimensionalityArgument.Title"),
                            Message.get("FragmentFingerprinterWrapper.Error.invalidFingerprintDimensionalityArgument.Header"),
                            tmpException.toString(),
                            tmpException);
                    //re-throws the exception to properly reset the binding
                    throw tmpException;
                }
            }
        };
        this.settings.add(this.fingerprintDimensionality);
        this.settingNameTooltipTextMap.put(this.fingerprintDimensionality.getName(), Message.get("FragmentFingerprinterWrapper.fingerprintDimensionality.tooltip"));
        this.fingerprintFrequencyThreshold = new SimpleIntegerProperty(this, "Fragment Fingerprint Frequency Threshold", this.DEFAULT_FINGERPRINT_FREQUENCY_THRESHOLD){
            @Override
            public void set(int newValue) throws IllegalArgumentException {
                if(FragmentFingerprinterWrapper.this.isLegalFingerprintFrequencyThreshold(newValue)) {
                    super.set(newValue);
                } else {
                    IllegalArgumentException tmpException = new IllegalArgumentException("An illegal fingerprint frequency threshold number was given: " + newValue);
                    FragmentFingerprinterWrapper.this.logger.log(Level.WARNING, tmpException.toString(), tmpException);
                    GuiUtil.guiExceptionAlert(Message.get("FragmentFingerprinterWrapper.Error.invalidFingerprintDimensionalityArgument.Title"),
                            Message.get("FragmentFingerprinterWrapper.Error.invalidFingerprintDimensionalityArgument.Header"),
                            tmpException.toString(),
                            tmpException);
                    //re-throws the exception to properly reset the binding
                    throw tmpException;
                }
            }
        };
        this.settings.add(this.fingerprintFrequencyThreshold);
        this.settingNameTooltipTextMap.put(this.fingerprintFrequencyThreshold.getName(), Message.get("FragmentFingerprinterWrapper.fingerprintFrequencyThreshold.tooltip"));
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     * Returns the string representation of the currently set option for the fingerprint typ
     *
     * @return enum constant name of the set option
     */
    public String getFingerprintTyp() {
        return this.fingerprintTyp.get();
    }
    //
    /**
     * Returns the integer value of the fingerprint dimensionality
     *
     * @return fingerprint dimensionality value
     */
    public int getFingerprintDimensionality() {
        return this.fingerprintDimensionality.get();
    }
    //
    /**
     * Returns the integer value of the fingerprint threshold
     *
     * @return fingerprint threshold value
     */
    public int getFingerprintFrequencyThreshold() {
        return this.fingerprintFrequencyThreshold.get();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     * Sets the fingerprint dimensionality value
     *
     * @param aNumberOfFingerprintDimensionality fingerprint dimensionality number
     */
    public void setFingerprintDimensionality(int aNumberOfFingerprintDimensionality) {
        this.fingerprintDimensionality.set(aNumberOfFingerprintDimensionality);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Overriden public methods">
    /**
     * {@inheritDoc}
     */
    @Override
    public List<Property> settingsProperties() {
        return this.settings;
    }
    //
    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return this.settingNameTooltipTextMap;
    }
    //
    /**
     * {@inheritDoc}
     */
    @Override
    public String getFingerprinterName() {
        return FragmentFingerprinterWrapper.FINGERPRINTER_NAME;
    }
    //
    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreDefaultSettings(int aDefaultFingerprintDimensionalityValue) {
        this.fingerprintTyp.set(FingerprintTyp.COUNT_FINGERPRINTS.name());
        this.fingerprintDimensionality.set(aDefaultFingerprintDimensionalityValue);
        this.fingerprintFrequencyThreshold.set(this.DEFAULT_FINGERPRINT_FREQUENCY_THRESHOLD);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public method">
    /**
     * Generate fragment fingerprints to cluster
     *
     * @param aMoleculeDataModelList List {@literal <}MoleculeDataModel {@literal >}
     * @param aFragmentDataModelList List {@literal <}FragmentDataModel {@literal >}
     * @param aFragmentationName name of the fragmentation algorithm
     * @return int matrix containing all generated fragment fingerprints
     */
    public int[][] getFragmentFingerprints(List<MoleculeDataModel> aMoleculeDataModelList, List<FragmentDataModel> aFragmentDataModelList, String aFragmentationName) {
        int tmpMaximumFingerprintDimensionalityValue = this.getFingerprintDimensionality();
        String tmpSortProperty = "absoluteFrequency";
        if(tmpMaximumFingerprintDimensionalityValue > aFragmentDataModelList.size()) {
            IllegalArgumentException tmpException = new IllegalArgumentException("An illegal fingerprint dimensionality number was given: " + tmpMaximumFingerprintDimensionalityValue +
                    " as dimensionality values is too high.");
            FragmentFingerprinterWrapper.this.logger.log(Level.WARNING, tmpException.toString(), tmpException);
            throw tmpException;
        } else {
            CollectionUtil.sortGivenFragmentListByPropertyAndSortType(aFragmentDataModelList, tmpSortProperty, "DESCENDING");
            List<FragmentDataModel> tmpSubList = aFragmentDataModelList.subList(0, tmpMaximumFingerprintDimensionalityValue);
            int[][] tmpDataMatrix = new int[aMoleculeDataModelList.size()][tmpSubList.size()];
            ArrayList<String> tmpKeyFragmentsToGenerateBitFingerprints = new ArrayList<>(tmpSubList.size());
            for (FragmentDataModel tmpFragmentDataModel : tmpSubList) {
                if (tmpFragmentDataModel.getAbsoluteFrequency() >= this.fingerprintFrequencyThreshold.get()) {
                    tmpKeyFragmentsToGenerateBitFingerprints.add(tmpFragmentDataModel.getUniqueSmiles());
                }
            }
            if (tmpKeyFragmentsToGenerateBitFingerprints.isEmpty()) {
                GuiUtil.guiMessageAlert(Alert.AlertType.INFORMATION, Message.get("FragmentFingerprinterWrapper.fingerprintThresholdInformation.Title"),
                        Message.get("FragmentFingerprinterWrapper.fingerprintThresholdInformation.Header"),
                        Message.get("FragmentFingerprinterWrapper.fingerprintThresholdInformation.Content"));
                for (FragmentDataModel tmpFragmentDataModel : tmpSubList) {
                    tmpKeyFragmentsToGenerateBitFingerprints.add(tmpFragmentDataModel.getUniqueSmiles());
                }
            }
            FragmentFingerprinter tmpFragmentFingerprinter = new FragmentFingerprinter(tmpKeyFragmentsToGenerateBitFingerprints);
            int tmpIterator = 0;
            for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
                ArrayList<String> tmpMoleculeFragmentsToGenerateBitFingerprints = new ArrayList<>();
                HashMap<String, Integer> tmpFragmentSmilesToFrequencyMapToGenerateCountFingerprints = new HashMap<>();
                if (!tmpMoleculeDataModel.hasMoleculeUndergoneSpecificFragmentation(aFragmentationName)) {
                    continue;
                }
                List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificAlgorithm(aFragmentationName);
                for (FragmentDataModel tmpFragmentDataModel : tmpFragmentList) {
                    if (this.getFingerprintTyp().equals(FragmentFingerprinterWrapper.FingerprintTyp.BIT_FINGERPRINTS.name())) {
                        tmpMoleculeFragmentsToGenerateBitFingerprints.add(tmpFragmentDataModel.getUniqueSmiles());
                    } else {
                        tmpFragmentSmilesToFrequencyMapToGenerateCountFingerprints.put(tmpFragmentDataModel.getUniqueSmiles(),
                                tmpMoleculeDataModel.getFragmentFrequencyOfSpecificAlgorithm(aFragmentationName).get(tmpFragmentDataModel.getUniqueSmiles()));
                    }
                }
                if (this.getFingerprintTyp().equals(FragmentFingerprinterWrapper.FingerprintTyp.BIT_FINGERPRINTS.name())) {
                    tmpFragmentFingerprinter.getBitFingerprint(tmpMoleculeFragmentsToGenerateBitFingerprints);
                    tmpDataMatrix[tmpIterator] = tmpFragmentFingerprinter.getBitArray(tmpMoleculeFragmentsToGenerateBitFingerprints);
                } else {
                    tmpFragmentFingerprinter.getCountFingerprint(tmpFragmentSmilesToFrequencyMapToGenerateCountFingerprints);
                    tmpDataMatrix[tmpIterator] = tmpFragmentFingerprinter.getCountArray(tmpFragmentSmilesToFrequencyMapToGenerateCountFingerprints);
                }
                tmpIterator++;
            }
            return tmpDataMatrix;
        }
    }
    /**
     * Generate fragment fingerprints to export
     *
     * @param aMoleculeDataModelList List {@literal <}MoleculeDataModel {@literal >}
     * @param aFragmentDataModelList List {@literal <}FragmentDataModel {@literal >}
     * @param aFragmentationName name of the fragmentation algorithm
     * @param aFingerprintTypString fingerprint typ in String
     * @return int matrix containing all generated fragment fingerprints
     */
    public int[][] getFragmentFingerprintsToExport(List<MoleculeDataModel> aMoleculeDataModelList, List<FragmentDataModel> aFragmentDataModelList, String aFragmentationName,
                                           String aFingerprintTypString) {
        int tmpMaximumFingerprintDimensionalityValue = this.getFingerprintDimensionality();
        String tmpSortProperty = "absoluteFrequency";
        if(tmpMaximumFingerprintDimensionalityValue > aFragmentDataModelList.size()) {
            IllegalArgumentException tmpException = new IllegalArgumentException("An illegal fingerprint dimensionality number was given: " + tmpMaximumFingerprintDimensionalityValue
            + " as dimensionality values is too high.");
            FragmentFingerprinterWrapper.this.logger.log(Level.WARNING, tmpException.toString(), tmpException);
            throw tmpException;
        } else {
            CollectionUtil.sortGivenFragmentListByPropertyAndSortType(aFragmentDataModelList, tmpSortProperty, "DESCENDING");
            List<FragmentDataModel> tmpSubList = aFragmentDataModelList.subList(0, tmpMaximumFingerprintDimensionalityValue);
            int[][] tmpDataMatrix = new int[aMoleculeDataModelList.size()][tmpSubList.size()];
            ArrayList<String> tmpKeyFragmentsToGenerateBitFingerprints = new ArrayList<>(tmpSubList.size());
            for (FragmentDataModel tmpFragmentDataModel : tmpSubList) {
                if (tmpFragmentDataModel.getAbsoluteFrequency() >= this.fingerprintFrequencyThreshold.get()) {
                    tmpKeyFragmentsToGenerateBitFingerprints.add(tmpFragmentDataModel.getUniqueSmiles());
                }
            }
            if (tmpKeyFragmentsToGenerateBitFingerprints.isEmpty()) {
                GuiUtil.guiMessageAlert(Alert.AlertType.INFORMATION, Message.get("FragmentFingerprinterWrapper.fingerprintThresholdInformation.Title"),
                        Message.get("FragmentFingerprinterWrapper.fingerprintThresholdInformation.Header"),
                        Message.get("FragmentFingerprinterWrapper.fingerprintThresholdInformation.Content"));
                for (FragmentDataModel tmpFragmentDataModel : tmpSubList) {
                    tmpKeyFragmentsToGenerateBitFingerprints.add(tmpFragmentDataModel.getUniqueSmiles());
                }
            }
            FragmentFingerprinter tmpFragmentFingerprinter = new FragmentFingerprinter(tmpKeyFragmentsToGenerateBitFingerprints);
            int tmpIterator = 0;
            for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
                ArrayList<String> tmpMoleculeFragmentsToGenerateBitFingerprints = new ArrayList<>();
                HashMap<String, Integer> tmpFragmentSmilesToFrequencyMapToGenerateCountFingerprints = new HashMap<>();
                if (!tmpMoleculeDataModel.hasMoleculeUndergoneSpecificFragmentation(aFragmentationName)) {
                    continue;
                }
                List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificAlgorithm(aFragmentationName);
                for (FragmentDataModel tmpFragmentDataModel : tmpFragmentList) {
                    if (aFingerprintTypString.equals(this.EXPORT_BIT_FINGERPRINTS)) {
                        System.out.println("generate bit fingerprints");
                        tmpMoleculeFragmentsToGenerateBitFingerprints.add(tmpFragmentDataModel.getUniqueSmiles());
                    } else {
                        System.out.println("generate count fingerprints");
                        tmpFragmentSmilesToFrequencyMapToGenerateCountFingerprints.put(tmpFragmentDataModel.getUniqueSmiles(),
                                tmpMoleculeDataModel.getFragmentFrequencyOfSpecificAlgorithm(aFragmentationName).get(tmpFragmentDataModel.getUniqueSmiles()));
                    }
                }
                if (aFingerprintTypString.equals(this.EXPORT_BIT_FINGERPRINTS)) {
                    System.out.println("generate bit fingerprint array");
                    tmpFragmentFingerprinter.getBitFingerprint(tmpMoleculeFragmentsToGenerateBitFingerprints);
                    tmpDataMatrix[tmpIterator] = tmpFragmentFingerprinter.getBitArray(tmpMoleculeFragmentsToGenerateBitFingerprints);
                } else {
                    System.out.println("generate count fingerprints array");
                    tmpFragmentFingerprinter.getCountFingerprint(tmpFragmentSmilesToFrequencyMapToGenerateCountFingerprints);
                    tmpDataMatrix[tmpIterator] = tmpFragmentFingerprinter.getCountArray(tmpFragmentSmilesToFrequencyMapToGenerateCountFingerprints);
                }
                tmpIterator++;
            }
            return tmpDataMatrix;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods">
    /**
     * Tests whether an integer value would be an allowed argument for the fingerprint dimensionality setting.
     * For this, it must be positive and non-zero.
     *
     * @param anInteger the integer to test
     * @return true if the given parameter is a legal value for the setting
     */
    private boolean isLegalFingerprintDimensionality(int anInteger) {
        return !(anInteger <= 0);
    }
    //
    /**
     * Tests whether an integer value would be an allowed argument for the fingerprint threshold setting.
     * For this, it must be positive and non-zero.
     *
     * @param anInteger the integer to test
     * @return true if the given parameter is a legal value for the setting
     */
    private boolean isLegalFingerprintFrequencyThreshold(int anInteger) {
        return !(anInteger <= 0);
    }
    //</editor-fold>
}
