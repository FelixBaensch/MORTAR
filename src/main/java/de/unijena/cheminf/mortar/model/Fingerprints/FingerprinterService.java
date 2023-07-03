package de.unijena.cheminf.mortar.model.Fingerprints;

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
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
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FingerprinterService {
    private static final String DEFAULT_SELECTED_FINGERPRINTER_NAME = FragmentFingerprintGenerator.FINGERPRINTER_NAME;
    public static final String FINGERPRINTER_SETTINGS_SUBFOLDER_NAME = "Fingerprinter_Settings";

    IMoleculeFingerprinter[] fingerprinter;
    FragmentFingerprintGenerator fragmentFingerprinterGenerator;
    IMoleculeFingerprinter selectedFingerprinter;
    private SettingsContainer settingsContainer;
    private SimpleStringProperty selectedFingerprinterNameProperty;
    private List<FragmentDataModel> model;
    private static final Logger LOGGER = Logger.getLogger(FingerprinterService.class.getName());

    public FingerprinterService(SettingsContainer settingsContainer) {
        this.fingerprinter = new IMoleculeFingerprinter[1];
        this.fragmentFingerprinterGenerator= new FragmentFingerprintGenerator();
        this.fingerprinter[0] = this.fragmentFingerprinterGenerator;
        this.settingsContainer = settingsContainer;
        this.selectedFingerprinterNameProperty = new SimpleStringProperty();
        for(IMoleculeFingerprinter tmpFingerprinter : this.fingerprinter) {
            if(tmpFingerprinter.getFingerprinterName().equals(FingerprinterService.DEFAULT_SELECTED_FINGERPRINTER_NAME)) {
                this.selectedFingerprinter = tmpFingerprinter;
            }
        }
        if(Objects.isNull(this.selectedFingerprinter)) {
            this.selectedFingerprinter = this.fragmentFingerprinterGenerator;
        }

    }
    public IMoleculeFingerprinter[] getFingerprinter() {
        return this.fingerprinter;
    }
    public IMoleculeFingerprinter getSelectedFingerprinter() {
        return this.selectedFingerprinter;
    }
    public void setSelectedFingerprinterNameProperty(String aFingerprinterName) {
        this.selectedFingerprinterNameProperty.set(aFingerprinterName);
    }
    public void setSelectedFingerprinter(String aFingerprinterName) {
        for(IMoleculeFingerprinter tmpFingerprinter : this.fingerprinter) {
            if(aFingerprinterName.equals(tmpFingerprinter.getFingerprinterName())) {
                this.selectedFingerprinter = tmpFingerprinter;
            }
        }
    }
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
                    Message.get("FragmentationService.Error.settingsPersistence"));
            return;
        }
        for (IMoleculeFingerprinter tmpFingerprinter : this.fingerprinter) {
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
                FingerprinterService.LOGGER.log(Level.WARNING, "Fragmenter settings persistence went wrong, exception: " + anException.toString(), anException);
                GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                        Message.get("Error.ExceptionAlert.Header"),
                        Message.get("FragmentationService.Error.settingsPersistence"),
                        anException);
                continue;
            }
        }
    }
    public void reloadFingerprinterSettings() {
        String tmpDirectoryPath = FileUtil.getSettingsDirPath()
                + FingerprinterService.FINGERPRINTER_SETTINGS_SUBFOLDER_NAME + File.separator;
        for (IMoleculeFingerprinter tmpFragmenter : this.fingerprinter) {
            String tmpClassName = tmpFragmenter.getClass().getSimpleName();
            File tmpFragmenterSettingsFile = new File(tmpDirectoryPath
                    + tmpClassName
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            if (tmpFragmenterSettingsFile.exists() && tmpFragmenterSettingsFile.isFile() && tmpFragmenterSettingsFile.canRead()) {
                PreferenceContainer tmpContainer;
                try {
                    tmpContainer = new PreferenceContainer(tmpFragmenterSettingsFile);
                } catch (IllegalArgumentException | IOException anException) {
                    FingerprinterService.LOGGER.log(Level.WARNING, "Unable to reload settings of fragmenter " + tmpClassName + " : " + anException.toString(), anException);
                    continue;
                }
                this.updatePropertiesFromPreferences(tmpFragmenter.settingsProperties(), tmpContainer);
            } else {
                //settings will remain in default
                FingerprinterService.LOGGER.log(Level.WARNING, "No persisted settings for " + tmpClassName + " available.");
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
    public int[][] getFingerprints(List<MoleculeDataModel> aMoleculeDataModelList, List<FragmentDataModel> aFragmentDataModelList, String aFragmentationName) {
        if(selectedFingerprinter.getFingerprinterName().equals("Fragment Fingerprinter")) {// TODO
           return this.fragmentFingerprinterGenerator.getFragmentFingerprints(aMoleculeDataModelList, aFragmentDataModelList, aFragmentationName);
        } else {
            return null;
        }
    }
}
