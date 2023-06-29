package de.unijena.cheminf.mortar.model.clustering;

import de.unijena.cheminf.art2aClustering.interfaces.IArt2aClusteringResult;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.Fingerprints.FingerprinterService;
import de.unijena.cheminf.mortar.model.fragmentation.FragmentationService;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
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


public class ClusteringService {
    public static final String DEFAULT_SELECTED_CLUSTERING_ALGORITHM_NAME = Art2aClusteringAlgorithm.CLUSTERING_NAME;
    public static final String CLUSTERING_SETTINGS_SUBFOLDER_NAME = "Clustering_Settings";

    IFingerprintClustering[] clusterer;
    Art2aClusteringAlgorithm art2aClustering;
    IFingerprintClustering selectedClusteringAlgorithm;
    private SettingsContainer settingsContainer;
    private SimpleStringProperty selectedClusteringAlgorithmNameProperty;
    private static final Logger LOGGER = Logger.getLogger(ClusteringService.class.getName());
    public ClusteringService(SettingsContainer settingsContainer) {
        this.clusterer = new IFingerprintClustering[1];
        this.art2aClustering = new Art2aClusteringAlgorithm();
        this.clusterer[0] = this.art2aClustering;
        this.settingsContainer = settingsContainer;
        this.selectedClusteringAlgorithmNameProperty = new SimpleStringProperty();
        try {
            this.checkFragmenters();
        } catch (Exception anException) {
            ClusteringService.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("FragmentationService.Error.invalidSettingFormat"),
                    anException);
        }
        for(IFingerprintClustering tmpClustering : this.clusterer) {
            if(tmpClustering.getClusteringName().equals(ClusteringService.DEFAULT_SELECTED_CLUSTERING_ALGORITHM_NAME)) {
                this.selectedClusteringAlgorithm = tmpClustering;
            }
        }
        if(Objects.isNull(this.selectedClusteringAlgorithm)) {
            this.selectedClusteringAlgorithm  = this.art2aClustering;
        }
        this.setSelectedClusteringAlgorithmNameProperty(this.selectedClusteringAlgorithm.getClusteringName());
    }
    public IArt2aClusteringResult[] startClustering(int[][] aDataMatrix, int aNumberOfTasks) throws InterruptedException {
        if(selectedClusteringAlgorithm.getClusteringName().equals("ART 2-A Clustering")) {  // ART 2-A Clustering
            IArt2aClusteringResult[]tmpClusterResults =  this.art2aClustering.startArt2aClustering(aDataMatrix, aNumberOfTasks, this.selectedClusteringAlgorithm.getClusteringName());
            return tmpClusterResults;
        } else {
            return null;
        }
    }
    public IFingerprintClustering[] getClusterer() {
        return this.clusterer;
    }
    public IFingerprintClustering getSelectedClusteringAlgorithm() {
        return this.selectedClusteringAlgorithm;
    }
    public void setSelectedClusteringAlgorithmNameProperty(String aClusteringAlgorithmName) {
        this.selectedClusteringAlgorithmNameProperty.set(aClusteringAlgorithmName);
    }
    public void setSelectedClusteringAlgorithm(String anAlgorithmName){
        for(IFingerprintClustering tmpClusteringAlgorithm : this.clusterer) {
            if(anAlgorithmName.equals(tmpClusteringAlgorithm.getClusteringName())) {
                this.selectedClusteringAlgorithm = tmpClusteringAlgorithm;
            }
        }
    }
    public void persistClusteringSettings() {
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
                    Message.get("FragmentationService.Error.settingsPersistence"));
            return;
        }
        for (IFingerprintClustering tmpFingerprinter : this.clusterer) {
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
                ClusteringService.LOGGER.log(Level.WARNING, "Fragmenter settings persistence went wrong, exception: " + anException.toString(), anException);
                GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                        Message.get("Error.ExceptionAlert.Header"),
                        Message.get("FragmentationService.Error.settingsPersistence"),
                        anException);
                continue;
            }
        }
    }
    public void reloadClusteringSettings() {
        String tmpDirectoryPath = FileUtil.getSettingsDirPath()
                + FingerprinterService.FINGERPRINTER_SETTINGS_SUBFOLDER_NAME + File.separator;
        for (IFingerprintClustering tmpFragmenter : this.clusterer) {
            String tmpClassName = tmpFragmenter.getClass().getSimpleName();
            File tmpFragmenterSettingsFile = new File(tmpDirectoryPath
                    + tmpClassName
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            if (tmpFragmenterSettingsFile.exists() && tmpFragmenterSettingsFile.isFile() && tmpFragmenterSettingsFile.canRead()) {
                PreferenceContainer tmpContainer;
                try {
                    tmpContainer = new PreferenceContainer(tmpFragmenterSettingsFile);
                } catch (IllegalArgumentException | IOException anException) {
                    ClusteringService.LOGGER.log(Level.WARNING, "Unable to reload settings of fragmenter " + tmpClassName + " : " + anException.toString(), anException);
                    continue;
                }
                this.updatePropertiesFromPreferences(tmpFragmenter.settingsProperties(), tmpContainer);
            } else {
                //settings will remain in default
                ClusteringService.LOGGER.log(Level.WARNING, "No persisted settings for " + tmpClassName + " available.");
            }
        }
    }
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
    private void checkFragmenters() throws Exception {
        int tmpAlgorithmNamesSetInitCapacity = CollectionUtil.calculateInitialHashCollectionCapacity(this.clusterer.length,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        HashSet<String> tmpAlgorithmNamesSet = new HashSet<>(tmpAlgorithmNamesSetInitCapacity, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        for (IFingerprintClustering tmpFragmenter : this.clusterer) {
            //algorithm name should be singleton and must be persistable
            String tmpAlgName = tmpFragmenter.getClusteringName();
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
            List<Property> tmpSettingsList = tmpFragmenter.settingsProperties();
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
}
