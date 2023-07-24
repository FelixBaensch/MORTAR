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

package de.unijena.cheminf.mortar.model.clustering;

import de.unijena.cheminf.clustering.art2a.interfaces.IArt2aClusteringResult;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
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
 * Service class for clustering.
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public class ClusteringService {
    //<editor-fold desc="public static final constants">
    /**
     * Default selected clustering algorithm
     */
    public static final String DEFAULT_SELECTED_CLUSTERING_ALGORITHM_NAME = Art2aClusteringAlgorithm.CLUSTERING_ALGORITHM_NAME;
    /**
     * Subfolder name in the settings directory where the clustering settings are persisted.
     */
    public static final String CLUSTERING_SETTINGS_SUBFOLDER_NAME = "Clustering_Settings";
    /**
     * Logger of this class
     */
    private static final Logger LOGGER = Logger.getLogger(ClusteringService.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="private final constants" defaultstate="collapsed">
    /**
     * Name of the ART 2-A clustering algorithm
     */
    private final String ART_2_A_CLUSTERING_ALGORITHM = "ART 2-A Clustering";
    //</editor-fold>
    //
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Array for the different available clustering algorithm
     */
    private IMortarClustering[] clusterer;
    /**
     * ART 2-A clustering algorithm
     */
    private Art2aClusteringAlgorithm art2aClustering;
    /**
     * Selected clustering algorithm
     */
    private IMortarClustering selectedClusteringAlgorithm;
    /**
     * SettingContainer to hold settings
     */
    private SettingsContainer settingsContainer;
    /**
     * Property of name of selected algorithm
     */
    private SimpleStringProperty selectedClusteringAlgorithmNameProperty;
    //</editor-fold>
    //
    //<editor-fold desc="Constructors">
    /**
     * Constructor.
     *
     * @param aSettingsContainer SettingsContainer which holds the settings
     */
    public ClusteringService(SettingsContainer aSettingsContainer) {
        this.clusterer = new IMortarClustering[1];
        this.art2aClustering = new Art2aClusteringAlgorithm();
        this.clusterer[0] = this.art2aClustering;
        this.settingsContainer = aSettingsContainer;
        this.selectedClusteringAlgorithmNameProperty = new SimpleStringProperty();
        try {
            this.checkClusteringAlgorithms();
        } catch (Exception anException) {
            ClusteringService.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("ClusteringService.Error.invalidSettingFormat"),
                    anException);
        }
        for(IMortarClustering tmpClustering : this.clusterer) {
            if(tmpClustering.getClusteringName().equals(ClusteringService.DEFAULT_SELECTED_CLUSTERING_ALGORITHM_NAME)) {
                this.selectedClusteringAlgorithm = tmpClustering;
            }
        }
        if(Objects.isNull(this.selectedClusteringAlgorithm)) {
            this.selectedClusteringAlgorithm  = this.art2aClustering;
        }
        this.setSelectedClusteringAlgorithmNameProperty(this.selectedClusteringAlgorithm.getClusteringName());
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public methods" defaultstate="collapsed">
    /**
     * Start clustering, depending on which clustering algorithm is selected.
     *
     * @param aDataMatrix matrix  containing all fingerprints to be clustered.
     * @param aNumberOfTasks int number of tasks
     * @return clustering results
     * @throws Exception is thrown if the clustering failed
     */
    public IArt2aClusteringResult[] startClustering(int[][] aDataMatrix, int aNumberOfTasks) throws Exception {
        if(this.selectedClusteringAlgorithm.getClusteringName().equals(this.ART_2_A_CLUSTERING_ALGORITHM)) {
            IArt2aClusteringResult[] tmpClusterResults = this.art2aClustering.startArt2aClustering(aDataMatrix, aNumberOfTasks, this.selectedClusteringAlgorithm.getClusteringName());
            return tmpClusterResults;
        } else {
            return null;
        }
    }
    //
    /**
     * Returns array of {@link IMortarClustering}
     *
     * return clustering algorithm
     */
    public IMortarClustering[] getClusterer() {
        return this.clusterer;
    }
    //
    /**
     * Returns clustering instance
     *
     * @return Art2aClusteringAlgorithm instance
     */
    public Art2aClusteringAlgorithm getArt2aClusteringInstance() {
        return this.art2aClustering;
    }
    //
    /**
     * Returns selected {@link IMortarClustering}
     *
     * @return selected clustering algorithm
     */
    public IMortarClustering getSelectedClusteringAlgorithm() {
        return this.selectedClusteringAlgorithm;
    }
    //
    /**
     * Sets the name of the selected clustering algorithm
     *
     * @param aClusteringAlgorithmName String for the name of the algorithm
     */
    public void setSelectedClusteringAlgorithmNameProperty(String aClusteringAlgorithmName) {
        this.selectedClusteringAlgorithmNameProperty.set(aClusteringAlgorithmName);
    }
    //
    /**
     * Sets the selected algorithm
     *
     * @param anAlgorithmName must be retrieved using the respective method of the clusterer object
     */
    public void setSelectedClusteringAlgorithm(String anAlgorithmName){
        for(IMortarClustering tmpClusteringAlgorithm : this.clusterer) {
            if(anAlgorithmName.equals(tmpClusteringAlgorithm.getClusteringName())) {
                this.selectedClusteringAlgorithm = tmpClusteringAlgorithm;
            }
        }
    }
    //
    /**
     * Persists settings of the clustering algorithms in preference container files in a subfolder of the settings directory. The settings of the
     * algorithms are translated to matching preference objects. If a single setting or several cannot be persisted, it
     * is only logged in the log file. But if persisting a whole algorithms fails, a warning is given to the user. The
     * settings are saved to files denoted with the simple class name of the respective algorithm.
     */
    public void persistClusteringSettings() {
        String tmpDirectoryPath = FileUtil.getSettingsDirPath()
                + ClusteringService.CLUSTERING_SETTINGS_SUBFOLDER_NAME + File.separator;
        File tmpDirectory = new File(tmpDirectoryPath);
        if (!tmpDirectory.exists()) {
            tmpDirectory.mkdirs();
        } else {
            FileUtil.deleteAllFilesInDirectory(tmpDirectoryPath);
        }
        if (!tmpDirectory.canWrite()) {
            GuiUtil.guiMessageAlert(Alert.AlertType.ERROR, Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("ClusteringService.Error.settingsPersistence"));
            return;
        }
        for (IMortarClustering tmpFingerprinter : this.clusterer) {
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
                ClusteringService.LOGGER.log(Level.WARNING, "Clustering settings persistence went wrong, exception: " + anException.toString(), anException);
                GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                        Message.get("Error.ExceptionAlert.Header"),
                        Message.get("ClusteringService.Error.settingsPersistence"),
                        anException);
                continue;
            }
        }
    }
    //
    /**
     * Reloads settings of the available fingerprinter. If something goes wrong, it is logged.
     */
    public void reloadClusteringSettings() {
        String tmpDirectoryPath = FileUtil.getSettingsDirPath()
                + ClusteringService.CLUSTERING_SETTINGS_SUBFOLDER_NAME + File.separator;
        for (IMortarClustering tmpClusteringAlgorithm : this.clusterer) {
            String tmpClassName = tmpClusteringAlgorithm.getClass().getSimpleName();
            File tmpFAlgorithmSettingsFile = new File(tmpDirectoryPath
                    + tmpClassName
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            if (tmpFAlgorithmSettingsFile.exists() && tmpFAlgorithmSettingsFile.isFile() && tmpFAlgorithmSettingsFile.canRead()) {
                PreferenceContainer tmpContainer;
                try {
                    tmpContainer = new PreferenceContainer(tmpFAlgorithmSettingsFile);
                } catch (IllegalArgumentException | IOException anException) {
                    ClusteringService.LOGGER.log(Level.WARNING, "Unable to reload settings of clustering algorithm " + tmpClassName + " : " + anException.toString(), anException);
                    continue;
                }
                this.updatePropertiesFromPreferences(tmpClusteringAlgorithm.settingsProperties(), tmpContainer);
            } else {
                //settings will remain in default
                ClusteringService.LOGGER.log(Level.WARNING, "No persisted settings for " + tmpClassName + " available.");
            }
        }
    }
    //
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
                        ClusteringService.LOGGER.log(Level.WARNING, "Setting " + tmpPropertyName + " is of unknown type.");
                    }
                } catch (ClassCastException | IllegalArgumentException anException) {
                    //setting will remain in default
                    ClusteringService.LOGGER.log(Level.WARNING, anException.toString(), anException);
                }
            } else {
                //setting will remain in default
                ClusteringService.LOGGER.log(Level.WARNING, "No persisted settings for " + tmpPropertyName + " available.");
            }
        }
    }
    //
    /**
     * Checks the available clustering algorithms and their settings for restrictions imposed by persistence. Throws an exception if
     * anything does not meet the requirements.
     */
    private void checkClusteringAlgorithms() throws Exception {
        int tmpAlgorithmNamesSetInitCapacity = CollectionUtil.calculateInitialHashCollectionCapacity(this.clusterer.length,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        HashSet<String> tmpAlgorithmNamesSet = new HashSet<>(tmpAlgorithmNamesSetInitCapacity, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        for (IMortarClustering tmpClusterer : this.clusterer) {
            //algorithm name should be singleton and must be persistable
            String tmpAlgName = tmpClusterer.getClusteringName();
            if (!PreferenceUtil.isValidName(tmpAlgName) || !SingleTermPreference.isValidContent(tmpAlgName)) {
                throw new Exception("Algorithm name " + tmpAlgName + " is invalid.");
            }
            if (tmpAlgorithmNamesSet.contains(tmpAlgName)) {
                throw new Exception("Algorithm name " + tmpAlgName + " is used multiple times.");
            } else {
                tmpAlgorithmNamesSet.add(tmpAlgName);
            }
            //setting names must be singletons within the respective class
            //setting names and values must adhere to the preference input restrictions
            //setting values are only tested for their current state, not the entire possible input space! It is tested again at persistence
            List<Property> tmpSettingsList = tmpClusterer.settingsProperties();
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
    //</editor-fold>
}
