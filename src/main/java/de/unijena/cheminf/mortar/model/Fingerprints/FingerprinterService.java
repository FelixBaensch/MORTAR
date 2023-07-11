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

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import de.unijena.cheminf.mortar.preference.BooleanPreference;
import de.unijena.cheminf.mortar.preference.IPreference;
import de.unijena.cheminf.mortar.preference.PreferenceContainer;
import de.unijena.cheminf.mortar.preference.PreferenceUtil;
import de.unijena.cheminf.mortar.preference.SingleIntegerPreference;
import de.unijena.cheminf.mortar.preference.SingleNumberPreference;
import de.unijena.cheminf.mortar.preference.SingleTermPreference;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Alert;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for fingerprinting.
 *
 * @author Betuel Sevindik
 * @version 1.0.0.1
 *
 */
public class FingerprinterService {
    //<editor-fold desc="public static final constants">
    /**
     * Default selected fingerprinter
     */
    private static final String DEFAULT_SELECTED_FINGERPRINTER_NAME = FragmentFingerprinterWrapper.FINGERPRINTER_NAME;
    /**
     * Subfolder name in the settings directory where the fingerprinter settings are persisted.
     */
    public static final String FINGERPRINTER_SETTINGS_SUBFOLDER_NAME = "Fingerprinter_Settings";
    /**
     * Array for the different fingerprinter available
     */
    IMortarFingerprinter[] fingerprinter;
    /**
     * Fragment fingerprinter
     */
    FragmentFingerprinterWrapper fragmentFingerprinterWrapper; // TODO
    /**
     * Selected fingerprinter
     */
    IMortarFingerprinter selectedFingerprinter;
    /**
     * SettingContainer to hold settings
     */
    private SettingsContainer settingsContainer;
    /**
     * Property of name of selected fingerprinter
     */
    private SimpleStringProperty selectedFingerprinterNameProperty;
    /**
     * Name of fragment fingerprinter
     */
    private final String FRAGMENT_FINGERPRINTER_NAME = "Fragment Fingerprinter";
    /**
     * Logger of this class
     */
    private static final Logger LOGGER = Logger.getLogger(FingerprinterService.class.getName());

    /**
     * Constructor.
     *
     * @param aSettingsContainer SettingsContainer which holds the settings
     */
    public FingerprinterService(SettingsContainer aSettingsContainer) {
        this.fingerprinter = new IMortarFingerprinter[1];
        this.fragmentFingerprinterWrapper = new FragmentFingerprinterWrapper();
        this.fingerprinter[0] = this.fragmentFingerprinterWrapper;
        Objects.requireNonNull(aSettingsContainer, "aSettingsContainer must not be null");
        this.settingsContainer = aSettingsContainer;
        this.selectedFingerprinterNameProperty = new SimpleStringProperty();
        try{
            this.checkFingerprinters();
        }
        catch (Exception anException) {
            FingerprinterService.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("FingerprinterService.Error.invalidSettingFormat"),
                    anException);
        }
        for(IMortarFingerprinter tmpFingerprinter : this.fingerprinter) {
            if(tmpFingerprinter.getFingerprinterName().equals(FingerprinterService.DEFAULT_SELECTED_FINGERPRINTER_NAME)) {
                this.selectedFingerprinter = tmpFingerprinter;
            }
        }
        if(Objects.isNull(this.selectedFingerprinter)) {
            this.selectedFingerprinter = this.fragmentFingerprinterWrapper;
        }
        this.setSelectedFingerprinterNameProperty(this.selectedFingerprinter.getFingerprinterName()); // TODO why?
    }

    /**
     * Generates fingerprints, depending on which fingerprint is selected.
     *
     * @param aMoleculeDataModelList List {@literal <}MoleculeDataModel {@literal >}
     * @param aFragmentDataModelList List {@literal <}FragmentDataModel {@literal >}
     * @param aFragmentationName current fingerprinter name
     * @return data matrix with generated fingerprints
     */
    public int[][] getFingerprints(List<MoleculeDataModel> aMoleculeDataModelList, List<FragmentDataModel> aFragmentDataModelList, String aFragmentationName,
                                   String aFingerprinterTypEnumName) {
        if(this.selectedFingerprinter.getFingerprinterName().equals(this.FRAGMENT_FINGERPRINTER_NAME)) {
            return this.fragmentFingerprinterWrapper.getFragmentFingerprints(aMoleculeDataModelList, aFragmentDataModelList, aFragmentationName, aFingerprinterTypEnumName);
        } else {
            return null;
        }
    }
    /**
     * Persists settings of the fingerprinter in preference container files in a subfolder of the settings directory. The settings of the
     * fingerprinter are translated to matching preference objects. If a single setting or several cannot be persisted, it
     * is only logged in the log file. But if persisting a whole fingerprinter fails, a warning is given to the user. The
     * settings are saved to files denoted with the simple class name of the respective fingerprinter.
     */
    public void persistFingerprinterSettings() {
        String tmpDirectoryPath = FileUtil.getSettingsDirPath()
                + FingerprinterService.FINGERPRINTER_SETTINGS_SUBFOLDER_NAME + File.separator;
        File tmpDirectory = new File(tmpDirectoryPath);
        if (!tmpDirectory.exists()) {
            tmpDirectory.mkdirs();
        } else {
            FileUtil.deleteAllFilesInDirectory(tmpDirectoryPath);
        }
        if (!tmpDirectory.canWrite()) {
            GuiUtil.guiMessageAlert(Alert.AlertType.ERROR, Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("FingerprinterService.Error.settingsPersistence"));
            return;
        }
        for (IMortarFingerprinter tmpFingerprinter : this.fingerprinter) {
            if (Objects.isNull(tmpFingerprinter)) {
                continue;
            }
            List<Property> tmpSettings = tmpFingerprinter.settingsProperties();
            if (Objects.isNull(tmpSettings)) {
                continue;
            }
            String tmpFilePath = tmpDirectoryPath
                    + tmpFingerprinter.getClass().getSimpleName()
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION;
            try {
                PreferenceContainer tmpPrefContainer = PreferenceUtil.translateJavaFxPropertiesToPreferences(tmpSettings, tmpFilePath);
                tmpPrefContainer.writeRepresentation();
            } catch (NullPointerException | IllegalArgumentException | IOException | SecurityException anException) {
                FingerprinterService.LOGGER.log(Level.WARNING, "Fingerprinter settings persistence went wrong, exception: " + anException.toString(), anException);
                GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                        Message.get("Error.ExceptionAlert.Header"),
                        Message.get("FingerprinterService.Error.settingsPersistence"),
                        anException);
                continue;
            }
        }
    }

    /**
     *  Reloads settings of the available fingerprinter. If something goes wrong, it is logged.
     */
    public void reloadFingerprinterSettings() {
        String tmpDirectoryPath = FileUtil.getSettingsDirPath()
                + FingerprinterService.FINGERPRINTER_SETTINGS_SUBFOLDER_NAME + File.separator;
        for (IMortarFingerprinter tmpFragmenter : this.fingerprinter) {
            String tmpClassName = tmpFragmenter.getClass().getSimpleName();
            File tmpFragmenterSettingsFile = new File(tmpDirectoryPath
                    + tmpClassName
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            if (tmpFragmenterSettingsFile.exists() && tmpFragmenterSettingsFile.isFile() && tmpFragmenterSettingsFile.canRead()) {
                PreferenceContainer tmpContainer;
                try {
                    tmpContainer = new PreferenceContainer(tmpFragmenterSettingsFile);
                } catch (IllegalArgumentException | IOException anException) {
                    FingerprinterService.LOGGER.log(Level.WARNING, "Unable to reload settings of fingerprinter " + tmpClassName + " : " + anException.toString(), anException);
                    continue;
                }
                this.updatePropertiesFromPreferences(tmpFragmenter.settingsProperties(), tmpContainer);
            } else {
                //settings will remain in default
                FingerprinterService.LOGGER.log(Level.WARNING, "No persisted settings for " + tmpClassName + " available.");
            }
        }
    }
    /**
     * Returns array of {@link IMortarFingerprinter}
     * @return fingerprinter
     */
    public IMortarFingerprinter[] getFingerprinter() {
        return this.fingerprinter;
    }
    /**
     * Returns selected {@link IMortarFingerprinter}
     *
     * @return selected fingerprinter
     */
    public IMortarFingerprinter getSelectedFingerprinter() {
        return this.selectedFingerprinter;
    }

    /**
     * TODO delte?
     * @param aFingerprinterName
     */
    public void setSelectedFingerprinterNameProperty(String aFingerprinterName) {
        this.selectedFingerprinterNameProperty.set(aFingerprinterName);
    }
    /**
     * Sets the selected fingerprinter
     *
     * @param aFingerprinterName must be retrieved using the respective method of the fingerprinter object
     */
    public void setSelectedFingerprinter(String aFingerprinterName) {
        for(IMortarFingerprinter tmpFingerprinter : this.fingerprinter) {
            if(aFingerprinterName.equals(tmpFingerprinter.getFingerprinterName())) {
                this.selectedFingerprinter = tmpFingerprinter;
            }
        }
    }
    /**
     * After fragmentation, sets the number of fragments as
     * fingerprint dimensionality in the settings.
     *
     * @return int value for fingerprint dimensionality
     */
    public void setMaximumFingerprintDimensionality(int aNumberOfMaxFragments) {
        this.fragmentFingerprinterWrapper.setFingerprintDimensionality(aNumberOfMaxFragments);
    }
    /**
     * Returns the fingerprint dimensionality
     *
     * @return int fingerprint dimensionality
     */
    public int getMaximumFingerprintDimensionality(){
        return this.fragmentFingerprinterWrapper.getFingerprintDimensionality();
    }

    /**
     * Return the typ of fingerprints
     *
     * @return typ name of the fingerprints (count or bit)
     */
    public String getFingerprintTypEnumName() {
        return this.fragmentFingerprinterWrapper.getFingerprintTyp();
    }
    /**
     * Checks the available fragmenters and their settings for restrictions imposed by persistence. Throws an exception if
     * anything does not meet the requirements.
     */
    private void checkFingerprinters() throws Exception {
        int tmpAlgorithmNamesSetInitCapacity = CollectionUtil.calculateInitialHashCollectionCapacity(this.fingerprinter.length,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        HashSet<String> tmpAlgorithmNamesSet = new HashSet<>(tmpAlgorithmNamesSetInitCapacity, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        for (IMortarFingerprinter tmpFingerprinter : this.fingerprinter) {
            //algorithm name should be singleton and must be persistable
            String tmpAlgName = tmpFingerprinter.getFingerprinterName();
            if (!PreferenceUtil.isValidName(tmpAlgName) || !SingleTermPreference.isValidContent(tmpAlgName)) {
                throw new Exception("Fingerprinter name " + tmpAlgName + " is invalid.");
            }
            if (tmpAlgorithmNamesSet.contains(tmpAlgName)) {
                throw new Exception("Fingerprinter name " + tmpAlgName + " is used multiple times.");
            } else {
                tmpAlgorithmNamesSet.add(tmpAlgName);
            }
            //setting names must be singletons within the respective class
            //setting names and values must adhere to the preference input restrictions
            //setting values are only tested for their current state, not the entire possible input space! It is tested again at persistence
            List<Property> tmpSettingsList = tmpFingerprinter.settingsProperties();
            int tmpSettingNamesSetInitCapacity = CollectionUtil.calculateInitialHashCollectionCapacity(tmpSettingsList.size(), BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
            HashSet<String> tmpSettingNames = new HashSet<>(tmpSettingNamesSetInitCapacity, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
            for (Property tmpSetting : tmpSettingsList) {
                if (!PreferenceUtil.isValidName(tmpSetting.getName())) {
                    throw new Exception("Setting " + tmpSetting.getName() + " has an invalid name.");
                }
                if (tmpSettingNames.contains(tmpSetting.getName())) {
                    throw new Exception("Setting name " + tmpSetting.getName() + " is used multiple times.");
                } else {
                    tmpSettingNames.add(tmpSetting.getName());
                }
                if (tmpSetting instanceof SimpleBooleanProperty) {
                    //nothing to do here, booleans cannot have invalid values
                } else if (tmpSetting instanceof SimpleIntegerProperty) {
                    if (!SingleIntegerPreference.isValidContent(Integer.toString(((SimpleIntegerProperty) tmpSetting).get()))) {
                        throw new Exception("Setting value " + ((SimpleIntegerProperty) tmpSetting).get() + " of setting name " + tmpSetting.getName() + " is invalid.");
                    }
                } else if (tmpSetting instanceof SimpleDoubleProperty) {
                    if (!SingleNumberPreference.isValidContent(((SimpleDoubleProperty) tmpSetting).get())) {
                        throw new Exception("Setting value " + ((SimpleDoubleProperty) tmpSetting).get() + " of setting name " + tmpSetting.getName() + " is invalid.");
                    }
                } else if (tmpSetting instanceof SimpleEnumConstantNameProperty || tmpSetting instanceof SimpleStringProperty) {
                    if (!SingleTermPreference.isValidContent(((SimpleStringProperty) tmpSetting).get())) {
                        throw new Exception("Setting value " + ((SimpleStringProperty) tmpSetting).get() + " of setting name " + tmpSetting.getName() + " is invalid.");
                    }
                } else {
                    throw new Exception("Setting " + tmpSetting.getName() + " is of an invalid type.");
                }
            }
        }
    }
    /**
     * Sets the values of the given properties according to the preferences in the given container with the same name.
     * If no matching preference for a given property is found, the value will remain in its default setting.
     */
    private void updatePropertiesFromPreferences(List<Property> aPropertiesList, PreferenceContainer aPreferenceContainer) {
        for (Property tmpSettingProperty : aPropertiesList) {
            String tmpPropertyName = tmpSettingProperty.getName();
            if (aPreferenceContainer.containsPreferenceName(tmpPropertyName)) {
                IPreference[] tmpPreferences = aPreferenceContainer.getPreferences(tmpPropertyName);
                try {
                    if (tmpSettingProperty instanceof SimpleBooleanProperty) {
                        BooleanPreference tmpBooleanPreference = (BooleanPreference) tmpPreferences[0];
                        tmpSettingProperty.setValue(tmpBooleanPreference.getContent());
                    } else if (tmpSettingProperty instanceof SimpleIntegerProperty) {
                        SingleIntegerPreference tmpIntPreference = (SingleIntegerPreference) tmpPreferences[0];
                        tmpSettingProperty.setValue(tmpIntPreference.getContent());
                    } else if (tmpSettingProperty instanceof SimpleDoubleProperty) {
                        SingleNumberPreference tmpDoublePreference = (SingleNumberPreference) tmpPreferences[0];
                        tmpSettingProperty.setValue(tmpDoublePreference.getContent());
                    } else if (tmpSettingProperty instanceof SimpleEnumConstantNameProperty || tmpSettingProperty instanceof SimpleStringProperty) {
                        SingleTermPreference tmpStringPreference = (SingleTermPreference) tmpPreferences[0];
                        tmpSettingProperty.setValue(tmpStringPreference.getContent());
                    } else {
                        //setting will remain in default
                        FingerprinterService.LOGGER.log(Level.WARNING, "Setting " + tmpPropertyName + " is of unknown type.");
                    }
                } catch (ClassCastException | IllegalArgumentException anException) {
                    //setting will remain in default
                    FingerprinterService.LOGGER.log(Level.WARNING, anException.toString(), anException);
                }
            } else {
                //setting will remain in default
                FingerprinterService.LOGGER.log(Level.WARNING, "No persisted settings for " + tmpPropertyName + " available.");
            }
        }
    }
}
