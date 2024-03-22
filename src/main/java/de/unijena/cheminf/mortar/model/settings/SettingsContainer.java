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

package de.unijena.cheminf.mortar.model.settings;

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.io.Exporter;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Container for general settings in MORTAR, capable of managing, preserving, and reloading application settings.
 * Externally, the settings can be accessed via JavaFX properties and internally, they are managed via
 * {@link de.unijena.cheminf.mortar.preference.IPreference} objects for persistence.
 * IMPORTANT NOTE for developers: When adding a new setting represented by a string, also consider the
 * SingleTermPreference class input restrictions when testing whether an input is valid!
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SettingsContainer {
    //<editor-fold desc="public static final constants" defaultstate="collapsed">
    /**
     * Name of the settings container file that persists the global settings.
     */
    public static final String SETTINGS_CONTAINER_FILE_NAME = "MORTAR_Settings";
    /**
     * Maximum available threads on the given machine.
     */
    public static final int MAX_AVAILABLE_THREADS = Runtime.getRuntime().availableProcessors();

    /**
     * Default value of the number of rows per page.
     */
    public static final int ROWS_PER_PAGE_SETTING_DEFAULT = 5;

    /**
     * Default value of the recent directory to use when there is no last directory used by the user.
     */
    public static final String RECENT_DIRECTORY_PATH_SETTING_DEFAULT = System.getProperty("user.home");

    /**
     * Default value of whether to add implicit hydrogen atoms to open valences in the imported molecules.
     */
    public static final boolean ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT = true;

    /**
     * Default value of whether to keep the atom container in the molecule/fragment data model.
     */
    public static final boolean KEEP_ATOM_CONTAINER_IN_DATA_MODEL_SETTING_DEFAULT = false;

    /**
     * Default value of whether to always use the MDL V3000 format for file export.
     */
    public static final boolean ALWAYS_MDLV3000_FORMAT_AT_EXPORT_SETTING_DEFAULT = false;

    /**
     * Default separator for the csv export.
     */
    public static final Exporter.CSVSeparator CSV_EXPORT_SEPARATOR_SETTING_DEFAULT = Exporter.CSVSeparator.COMMA;

    /**
     * Default value of whether to keep last fragment.
     */
    public static final boolean KEEP_LAST_FRAGMENT_SETTING_DEFAULT = false;
    //</editor-fold>
    //
    //<editor-fold desc="private static final constants" defaultstate="collapsed">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(SettingsContainer.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="private final variables">
    /**
     * Default value of the number of parallel tasks to use for fragmentation, determined based on the maximum available
     * threads on this machine in the constructor.
     */
    private final int nrOfTasksForFragmentationSettingDefault;
    //</editor-fold>
    //
    //<editor-fold desc="private variables">

    private SimpleIntegerProperty rowsPerPageSetting;

    private SimpleIntegerProperty numberOfTasksForFragmentationSetting;

    private SimpleStringProperty recentDirectoryPathSetting;

    private SimpleBooleanProperty addImplicitHydrogensAtImportSetting;

    private SimpleBooleanProperty keepAtomContainerInDataModelSetting;

    private SimpleBooleanProperty alwaysMDLV3000FormatAtExportSetting;

    private SimpleIDisplayEnumConstantProperty csvExportSeparatorSetting;

    private SimpleBooleanProperty keepLastFragmentSetting;

    /**
     * List of setting to display in the general settings dialogue; excludes recent directory path because this is only
     * for internal use, not intended to be changed by the user via this dialogue.
     */
    private List<Property<?>> settings;

    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private HashMap<String, String> settingNameTooltipTextMap;
    //
    /**
     * Map to store pairs of {@literal <setting name, display name>}.
     */
    private HashMap<String, String> settingNameDisplayNameMap;
    //</editor-fold>
    //
    //<editor-fold desc="constructors">
    /**
     * Constructor that sets all settings to their default values.
     */
    public SettingsContainer() {
        if (SettingsContainer.MAX_AVAILABLE_THREADS == 1) {
            this.nrOfTasksForFragmentationSettingDefault = 1;
        } else if (SettingsContainer.MAX_AVAILABLE_THREADS < 4) {
            this.nrOfTasksForFragmentationSettingDefault = 2;
        } else {
            //max available threads equal or higher than 4
            this.nrOfTasksForFragmentationSettingDefault = 4;
        }
        this.initialiseSettings();
        try {
            this.checkSettings();
        } catch (Exception anException) {
            SettingsContainer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("SettingsContainer.Error.invalidSettingFormat"),
                    anException);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties">
    /**
     * Returns a list of all settings intended to be set by the user as JavaFX properties. For example, the recent
     * directory path is excluded from this list.
     *
     * @return list of settings as properties
     */
    public List<Property<?>> settingsProperties() {
        return this.settings;
    }

    /**
     * Returns a map containing descriptive texts (values) for the settings with the given names (keys) to be used as
     * tooltips in the GUI.
     *
     * @return map with tooltip texts
     */
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return this.settingNameTooltipTextMap;
    }

    /**
     * Returns a map containing language-specific names for display (values) for the settings with the given names (keys)
     * to be used in the GUI.
     *
     * @return map with display names
     */
    public Map<String, String> getSettingNameToDisplayNameMap() {
        return this.settingNameDisplayNameMap;
    }

    /**
     * Returns the current value of the rows, or rather molecules, per page setting.
     *
     * @return rows per page setting value
     */
    public int getRowsPerPageSetting() {
        return this.rowsPerPageSetting.get();
    }

    /**
     * Returns the property wrapping the rows per page setting.
     *
     * @return rows per page setting property
     */
    public SimpleIntegerProperty rowsPerPageSettingProperty() {
        return this.rowsPerPageSetting;
    }

    /**
     * Returns the current value of the number of tasks for fragmentation setting.
     *
     * @return number of tasks for fragmentation setting value
     */
    public int getNumberOfTasksForFragmentationSetting() {
        return this.numberOfTasksForFragmentationSetting.get();
    }

    /**
     * Returns the property wrapping the number of tasks for fragmentation setting.
     *
     * @return number of tasks for fragmentation setting property
     */
    public SimpleIntegerProperty numberOfTasksForFragmentationSettingProperty() {
        return this.numberOfTasksForFragmentationSetting;
    }

    /**
     * Returns the default value for the number of tasks for fragmentation setting that is determined in the class
     * constructor based on the number of maximum available threads on the specific machine.
     *
     * @return default value of number of tasks for fragmentation setting
     */
    public int getNumberOfTasksForFragmentationSettingDefault() {
        return this.nrOfTasksForFragmentationSettingDefault;
    }

    /**
     * Returns the current value of the recent directory path setting.
     *
     * @return recent directory path setting value
     */
    public String getRecentDirectoryPathSetting() {
        return this.recentDirectoryPathSetting.get();
    }

    /**
     * Returns the property wrapping the recent directory path setting.
     *
     * @return recent directory path setting property
     */
    public SimpleStringProperty recentDirectoryPathSettingProperty() {
        return this.recentDirectoryPathSetting;
    }

    /**
     * Returns the current value of the add implicit hydrogens at import setting.
     *
     * @return add implicit hydrogens at import setting value
     */
    public boolean getAddImplicitHydrogensAtImportSetting() {
        return this.addImplicitHydrogensAtImportSetting.get();
    }

    /**
     * Returns the property wrapping the add implicit hydrogens at import setting.
     *
     * @return add implicit hydrogens at import setting property
     */
    public SimpleBooleanProperty addImplicitHydrogensAtImportSettingProperty() {
        return this.addImplicitHydrogensAtImportSetting;
    }

    /**
     * Returns the current value of the keep atom container in data model setting.
     *
     * @return keep atom container in data model setting value
     * @deprecated currently not in use, returns always false
     */
    @Deprecated
    public boolean getKeepAtomContainerInDataModelSetting() {
        //return this.keepAtomContainerInDataModelSetting.get();
        return false;
    }

    /**
     * Returns the property wrapping the keep atom container in data model setting.
     *
     * @return keep atom container in data model setting property
     * @deprecated currently not in use, returns always false
     */
    @Deprecated
    public SimpleBooleanProperty keepAtomContainerInDataModelSettingProperty() {
        return this.keepAtomContainerInDataModelSetting;
    }

    /**
     * Returns the current value of the always MDL V3000 format at export setting.
     *
     * @return always MDLV3000 format at export setting value
     */
    public boolean getAlwaysMDLV3000FormatAtExportSetting() {
        return this.alwaysMDLV3000FormatAtExportSetting.get();
    }

    /**
     * Returns the property wrapping the always MDL V3000 format at export setting.
     *
     * @return always MDLV3000 format at export setting property
     */
    public SimpleBooleanProperty alwaysMDLV3000FormatAtExportSettingProperty() {
        return this.alwaysMDLV3000FormatAtExportSetting;
    }

    /**
     * Returns the current value of the separator setting for csv export.
     *
     * @return csv export separator value
     */
    public Exporter.CSVSeparator getCsvExportSeparatorSetting() {
        return (Exporter.CSVSeparator) this.csvExportSeparatorSetting.get();
    }

    /**
     * Returns the property wrapping the separator setting for csv export.
     *
     * @return csv export separator setting property
     */
    public SimpleIDisplayEnumConstantProperty csvExportSeparatorSettingProperty() {
        return this.csvExportSeparatorSetting;
    }

    /**
     * Returns the currently set CSV export separator character.
     *
     * @return CSV separator char
     */
    public char getCsvExportSeparatorSettingCharacter() {
        return ((Exporter.CSVSeparator) this.csvExportSeparatorSetting.get()).getSeparatorChar();
    }

    /**
     * Returns the current value of the keep last fragment setting.
     *
     * @return keep last fragment setting value
     */
    public boolean isKeepLastFragmentSetting(){
        return this.keepLastFragmentSetting.get();
    }

    /**
     * Return the property wrapping the keep last fragment setting.
     *
     * @return keep last fragment setting property
     */
    public SimpleBooleanProperty keepLastFragmentSettingProperty(){
        return this.keepLastFragmentSetting;
    }

    /**
     * Sets the setting for how many rows/molecules should be displayed per page in the tabs.
     *
     * @param anInteger the number of molecules displayed per page in the tabs
     * @throws IllegalArgumentException if the parameter is 0 or negative
     */
    public void setRowsPerPageSetting(int anInteger) throws IllegalArgumentException {
        if (this.isLegalRowsPerPageSetting(anInteger)) {
            //synchronises the preference also
            this.rowsPerPageSetting.set(anInteger);
        } else {
            throw new IllegalArgumentException("The given rows per page number is 0 or negative.");
        }
    }

    /**
     * Sets the setting for how many parallel threads should be used for a fragmentation.
     *
     * @param anInteger the number of threads to use
     * @throws IllegalArgumentException if the given parameter is 0 or negative or is higher than the number of
     * available processors
     */
    public void setNumberOfTasksForFragmentationSetting(int anInteger) throws IllegalArgumentException {
        if (this.isLegalNumberOfTasksForFragmentationSetting(anInteger)) {
            //synchronises the preference also
            this.numberOfTasksForFragmentationSetting.set(anInteger);
        } else {
            throw new IllegalArgumentException("The given number of tasks for fragmentation is 0 or negative or higher "
                   + "than the number of available processors.");
        }
    }

    /**
     * Sets the recent directory path, i.e. the path that was opened last to import or export a file.
     *
     * @param aPath the last used path
     * @throws IllegalArgumentException if the parameter is null, empty, does not exist, is no directory, or cannot be
     * read
     */
    public void setRecentDirectoryPathSetting(String aPath) throws IllegalArgumentException {
        if (this.isLegalRecentDirectoryPath(aPath)) {
            //synchronises the preference also
            this.recentDirectoryPathSetting.set(aPath);
        } else {
            throw new IllegalArgumentException("Given recent directory path is null, empty, does not exist, " +
                    "is no directory, or cannot be read.");
        }
    }

    /**
     * Sets the setting for whether to add implicit hydrogen atoms to incomplete valences in the imported molecules.
     *
     * @param aBoolean whether to add implicit hydrogens at molecule import
     */
    public void setAddImplicitHydrogensAtImportSetting(boolean aBoolean) {
        //synchronises the preference also
        this.addImplicitHydrogensAtImportSetting.set(aBoolean);
    }

    /**
     * Sets the setting for whether to keep the atom container in the molecule/fragment data model.
     *
     * @param aBoolean whether to keep the atom container in the molecule/fragment data model
     * @deprecated setting is currently unused
     */
    @Deprecated
    public void setKeepAtomContainerInDataModelSetting(boolean aBoolean) {
        this.keepAtomContainerInDataModelSetting.set(aBoolean);
    }

    /**
     * Sets the setting for whether to always use MDL V3000 format for file export. Per default, this is set to false and
     * the MOL file and SD file export will in most cases create MDL V2000 representations of the exported molecules,
     * except for molecules that have more than 999 atoms because the V2000 cannot handle it. In the opposite case, the
     * export routines will always use the MDL V3000 format.
     *
     * @param aBoolean whether to always use the MDL V3000 format for file export
     */
    public void setAlwaysMDLV3000FormatAtExportSetting(boolean aBoolean) {
        this.alwaysMDLV3000FormatAtExportSetting.set(aBoolean);
    }

    /**
     * Sets the setting for the separator for the csv export. Param must be an enum constant of
     * the Exporter CSV separator enum.
     *
     * @param aSeparator enum constant
     * @throws IllegalArgumentException if the param is null or not valid
     */
    public void setCsvExportSeparatorSetting(Exporter.CSVSeparator aSeparator) throws IllegalArgumentException {
        if (this.isLegalCsvExportSeparator(aSeparator.getSeparatorChar())) {
            this.csvExportSeparatorSetting.set(aSeparator);
        } else {
            throw new IllegalArgumentException("Given separator for csv export is null, empty, blank or not valid");
        }
    }

    /**
     * Sets the setting for whether the last fragment is to be kept if no new fragment is created in a
     * pipeline fragmentation step or whether it is discarded.
     *
     * @param aBoolean whether to keep last fragment or to discard
     */
    public void setKeepLastFragmentSetting(boolean aBoolean){
        this.keepLastFragmentSetting.set(aBoolean);
    }

    /**
     * Restores all setting to their default setting according to the respective public constants in this class.
     */
    public void restoreDefaultSettings() {
        this.rowsPerPageSetting.set(SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT);
        this.numberOfTasksForFragmentationSetting.set(this.nrOfTasksForFragmentationSettingDefault);
        this.recentDirectoryPathSetting.set(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
        this.addImplicitHydrogensAtImportSetting.set(SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT);
        //DEPRECATED
        //this.keepAtomContainerInDataModelSetting.set(SettingsContainer.KEEP_ATOM_CONTAINER_IN_DATA_MODEL_SETTING_DEFAULT);
        this.alwaysMDLV3000FormatAtExportSetting.set(SettingsContainer.ALWAYS_MDLV3000_FORMAT_AT_EXPORT_SETTING_DEFAULT);
        this.csvExportSeparatorSetting.set(SettingsContainer.CSV_EXPORT_SEPARATOR_SETTING_DEFAULT);
        this.keepLastFragmentSetting.set(SettingsContainer.KEEP_LAST_FRAGMENT_SETTING_DEFAULT);
    }
    //</editor-fold>
    //
    //<editor-fold desc="public methods">
    /**
     * Triggers the preservation of the current settings values in a file for re-import at the next application session.
     */
    public void preserveSettings() {
        String tmpSettingsDirectoryPathName = FileUtil.getSettingsDirPath();
        File tmpSettingsDirectoryFile = new File(tmpSettingsDirectoryPathName);
        boolean tmpMKDirsSuccessful = true;
        if (!tmpSettingsDirectoryFile.exists()) {
            tmpMKDirsSuccessful = tmpSettingsDirectoryFile.mkdirs();
        }
        if (!tmpSettingsDirectoryFile.canWrite() || !tmpMKDirsSuccessful) {
            SettingsContainer.LOGGER.log(Level.WARNING, "Global settings persistence went wrong, cannot write to settings directory.");
            GuiUtil.guiMessageAlert(Alert.AlertType.ERROR, Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("SettingsContainer.Error.settingsPersistence"));
            return;
        }
        String tmpPreferenceContainerFilePathName = tmpSettingsDirectoryPathName
                + SettingsContainer.SETTINGS_CONTAINER_FILE_NAME
                + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION;
        List<Property<?>> tmpSettings = new ArrayList<>(6);
        tmpSettings.addAll(this.settings);
        tmpSettings.add(this.recentDirectoryPathSetting);
        try {
            PreferenceContainer tmpPrefContainer = PreferenceUtil.translateJavaFxPropertiesToPreferences(tmpSettings, tmpPreferenceContainerFilePathName);
            tmpPrefContainer.writeRepresentation();
        } catch (NullPointerException | IllegalArgumentException | IOException | SecurityException anException) {
            SettingsContainer.LOGGER.log(Level.WARNING, String.format("Global settings persistence went wrong, exception: %s", anException.toString()), anException);
            GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("SettingsContainer.Error.settingsPersistence"),
                    anException);
            //return;
        }
    }

    /**
     * Reloads the setting values from a previous MORTAR session via the persisted preference container.
     */
    public void reloadGlobalSettings() {
        String tmpSettingsDirectoryPathName = FileUtil.getSettingsDirPath();
        File tmpSettingsDirectoryFile = new File(tmpSettingsDirectoryPathName);
        String tmpPreferenceContainerFilePathName = tmpSettingsDirectoryPathName
                + SettingsContainer.SETTINGS_CONTAINER_FILE_NAME
                + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION;
        File tmpPreferenceContainerFile = new File(tmpPreferenceContainerFilePathName);
        if (!tmpSettingsDirectoryFile.exists()) {
            FileUtil.createDirectory(tmpSettingsDirectoryFile.getAbsolutePath());
            SettingsContainer.LOGGER.info("No persisted global settings could be found, all set to default.");
            //return;
        } else {
            boolean tmpExists = tmpPreferenceContainerFile.exists();
            boolean tmpIsFile = tmpPreferenceContainerFile.isFile();
            boolean tmpCanRead = tmpPreferenceContainerFile.canRead();
            if (!tmpExists || !tmpIsFile || !tmpCanRead) {
                SettingsContainer.LOGGER.warning("Preference container file does not exist or cannot be read. " +
                        "A new one is initialised.");
                //return;
            } else {
                PreferenceContainer tmpDePersistedContainer;
                try {
                    tmpDePersistedContainer = new PreferenceContainer(tmpPreferenceContainerFile);
                } catch (IOException | SecurityException anException) {
                    SettingsContainer.LOGGER.log(Level.SEVERE, String.format("Unable to reload global settings: %s", anException.toString()), anException);
                    return;
                }
                List<Property<?>> tmpSettings = new ArrayList<>(8);
                tmpSettings.addAll(this.settings);
                tmpSettings.add(this.recentDirectoryPathSetting);
                for (Property<?> tmpSettingProperty : tmpSettings) {
                    String tmpPropertyName = tmpSettingProperty.getName();
                    if (tmpDePersistedContainer.containsPreferenceName(tmpPropertyName)) {
                        IPreference[] tmpPreferences = tmpDePersistedContainer.getPreferences(tmpPropertyName);
                        try {
                            switch (tmpSettingProperty) {
                                case SimpleBooleanProperty tmpSimpleBooleanProperty -> {
                                    BooleanPreference tmpBooleanPreference = (BooleanPreference) tmpPreferences[0];
                                    tmpSimpleBooleanProperty.setValue(tmpBooleanPreference.getContent());
                                }
                                case SimpleIntegerProperty tmpSimpleIntegerProperty -> {
                                    SingleIntegerPreference tmpIntPreference = (SingleIntegerPreference) tmpPreferences[0];
                                    tmpSimpleIntegerProperty.setValue(tmpIntPreference.getContent());
                                }
                                case SimpleDoubleProperty tmpSimpleDoubleProperty -> {
                                    SingleNumberPreference tmpDoublePreference = (SingleNumberPreference) tmpPreferences[0];
                                    tmpSimpleDoubleProperty.setValue(tmpDoublePreference.getContent());
                                }
                                case SimpleIDisplayEnumConstantProperty tmpSimpleIDisplayEnumConstantProperty -> {
                                    SingleTermPreference tmpStringPreference = (SingleTermPreference) tmpPreferences[0];
                                    tmpSimpleIDisplayEnumConstantProperty.setValue((IDisplayEnum) Enum.valueOf(tmpSimpleIDisplayEnumConstantProperty.getAssociatedEnum(), tmpStringPreference.getContent()));
                                }
                                case SimpleStringProperty tmpSimpleStringProperty -> {
                                    //also true for case of SimpleEnumConstantNameProperty
                                    SingleTermPreference tmpStringPreference = (SingleTermPreference) tmpPreferences[0];
                                    tmpSimpleStringProperty.setValue(tmpStringPreference.getContent());
                                }
                                default -> {
                                    //setting will remain in default
                                    SettingsContainer.LOGGER.log(Level.WARNING, "Setting {0} is of unknown type.", tmpPropertyName);
                                }
                            }
                        } catch (ClassCastException | IllegalArgumentException anException) {
                            //setting will remain in default
                            SettingsContainer.LOGGER.log(Level.WARNING, anException.toString(), anException);
                        }
                    } else {
                        //setting will remain in default
                        SettingsContainer.LOGGER.log(Level.WARNING, "No persisted settings for {0} available.", tmpPropertyName);
                    }
                }
            }
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods">
    /**
     * Initialises the properties representing the settings in default values. Properties are added
     * to the list of settings for display to the user.
     */
    private void initialiseSettings() {
        int tmpNumberOfSettings = 6;
        int tmpInitialCapacityForSettingNameMaps = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpNumberOfSettings,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap<>(tmpInitialCapacityForSettingNameMaps, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameDisplayNameMap = new HashMap<>(tmpInitialCapacityForSettingNameMaps, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.rowsPerPageSetting = new SimpleIntegerProperty(this,
                // note: these names are for persistence and de-persistence! The map values are for display
                "Rows per page setting",
                SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT) {
            @Override
            public void set(int newValue) throws IllegalArgumentException {
                if (SettingsContainer.this.isLegalRowsPerPageSetting(newValue)) {
                    super.set(newValue);
                } else {
                    IllegalArgumentException tmpException = new IllegalArgumentException("An illegal rows per page number was given: " + newValue);
                    SettingsContainer.LOGGER.log(Level.WARNING, tmpException.toString(), tmpException);
                    GuiUtil.guiExceptionAlert(Message.get("SettingsContainer.Error.invalidSettingArgument.Title"),
                            Message.get("SettingsContainer.Error.invalidSettingArgument.Header"),
                            tmpException.toString(),
                            tmpException);
                    //re-throws the exception to properly reset the binding
                    throw tmpException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.rowsPerPageSetting.getName(), Message.get("SettingsContainer.rowsPerPageSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.rowsPerPageSetting.getName(), Message.get("SettingsContainer.rowsPerPageSetting.displayName"));
        this.numberOfTasksForFragmentationSetting = new SimpleIntegerProperty(this,
                "Nr of tasks for fragmentation setting",
                this.nrOfTasksForFragmentationSettingDefault) {
            @Override
            public void set(int newValue) throws IllegalArgumentException {
                if (SettingsContainer.this.isLegalNumberOfTasksForFragmentationSetting(newValue)) {
                    super.set(newValue);
                } else {
                    IllegalArgumentException tmpException = new IllegalArgumentException("An illegal number of tasks for fragmentation was given: " + newValue);
                    SettingsContainer.LOGGER.log(Level.WARNING, tmpException.toString(), tmpException);
                    GuiUtil.guiExceptionAlert(Message.get("SettingsContainer.Error.invalidSettingArgument.Title"),
                            Message.get("SettingsContainer.Error.invalidSettingArgument.Header"),
                            tmpException.toString(),
                            tmpException);
                    //re-throws the exception to properly reset the binding
                    throw tmpException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.numberOfTasksForFragmentationSetting.getName(),
                String.format(Message.get("SettingsContainer.numberOfTasksForFragmentationSetting.tooltip"), SettingsContainer.MAX_AVAILABLE_THREADS));
        this.settingNameDisplayNameMap.put(this.numberOfTasksForFragmentationSetting.getName(),
                Message.get("SettingsContainer.numberOfTasksForFragmentationSetting.displayName"));
        this.recentDirectoryPathSetting = new SimpleStringProperty(this,
                "Recent directory path setting",
                SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT) {
            @Override
            public void set(String newValue) throws IllegalArgumentException {
                if (SettingsContainer.this.isLegalRecentDirectoryPath(newValue)) {
                    super.set(newValue);
                } else {
                    IllegalArgumentException tmpException = new IllegalArgumentException("An illegal number of tasks for fragmentation was given: " + newValue);
                    SettingsContainer.LOGGER.log(Level.WARNING, tmpException.toString(), tmpException);
                    //no GUI alert here because this is an internal setting
                    //re-throws the exception to properly reset the binding
                    throw tmpException;
                }
            }
        };
        this.addImplicitHydrogensAtImportSetting = new SimpleBooleanProperty(this,
                "Add implicit hydrogens at import setting",
                SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.addImplicitHydrogensAtImportSetting.getName(), Message.get("SettingsContainer.addImplicitHydrogensAtImportSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.addImplicitHydrogensAtImportSetting.getName(), Message.get("SettingsContainer.addImplicitHydrogensAtImportSetting.displayName"));
        //DEPRECATED
        /*this.keepAtomContainerInDataModelSetting = new SimpleBooleanProperty(this,
                "Keep AtomContainers in the DataModels setting",
                this.keepAtomContainerInDataModelPreference.getContent()) {
            @Override
            public void set(boolean newValue) {
                SettingsContainer.this.keepAtomContainerInDataModelPreference.setContent(newValue);
                super.set(newValue);
            }
        };*/
        //Dummy:
        this.keepAtomContainerInDataModelSetting = new SimpleBooleanProperty(this,
                "Keep AtomContainers in the DataModels setting", false) {
            @Override
            public void set(boolean newValue) {
                //do nothing, the value should remain false!
            }
        };
        this.alwaysMDLV3000FormatAtExportSetting = new SimpleBooleanProperty(this,
                "Always MDL V3000 format at export setting",
                SettingsContainer.ALWAYS_MDLV3000_FORMAT_AT_EXPORT_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.alwaysMDLV3000FormatAtExportSetting.getName(), Message.get("SettingsContainer.alwaysMDLV3000FormatAtExportSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.alwaysMDLV3000FormatAtExportSetting.getName(), Message.get("SettingsContainer.alwaysMDLV3000FormatAtExportSetting.displayName"));
        this.csvExportSeparatorSetting = new SimpleIDisplayEnumConstantProperty(this,
                "Csv export separator setting", SettingsContainer.CSV_EXPORT_SEPARATOR_SETTING_DEFAULT,
                Exporter.CSVSeparator.class) {
            @Override
            public void set(IDisplayEnum newValue) throws IllegalArgumentException {
                if (SettingsContainer.this.isLegalCsvExportSeparator(((Exporter.CSVSeparator) newValue).getSeparatorChar())) {
                    super.set(newValue);
                } else {
                    IllegalArgumentException tmpException = new IllegalArgumentException("An illegal value for the separator for the csv export was given: " + newValue);
                    SettingsContainer.LOGGER.log(Level.WARNING, tmpException.toString(), tmpException);
                    GuiUtil.guiExceptionAlert(Message.get("SettingsContainer.Error.invalidSettingArgument.Title"),
                            Message.get("SettingsContainer.Error.invalidSettingArgument.Header"),
                            tmpException.toString(),
                            tmpException);
                    //re-throws the exception to properly reset the binding
                    throw tmpException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.csvExportSeparatorSetting.getName(), Message.get("SettingsContainer.csvExportSeparatorSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.csvExportSeparatorSetting.getName(), Message.get("SettingsContainer.csvExportSeparatorSetting.displayName"));
        this.keepLastFragmentSetting = new SimpleBooleanProperty(this,
                "Keep last fragment in pipelining",
                SettingsContainer.KEEP_LAST_FRAGMENT_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.keepLastFragmentSetting.getName(), Message.get("SettingsContainer.keepLastFragmentSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.keepLastFragmentSetting.getName(), Message.get("SettingsContainer.keepLastFragmentSetting.displayName"));
        this.settings = new ArrayList<>(tmpNumberOfSettings);
        this.settings.add(this.rowsPerPageSetting);
        this.settings.add(this.numberOfTasksForFragmentationSetting);
        this.settings.add(this.addImplicitHydrogensAtImportSetting);
        //DEPRECATED
        //this.settings.add(this.keepAtomContainerInDataModelSetting);
        this.settings.add(this.alwaysMDLV3000FormatAtExportSetting);
        this.settings.add(this.keepLastFragmentSetting);
        this.settings.add(this.csvExportSeparatorSetting);
        //note: recent directory path is only internal, all settings in the list are for the user
    }

    /**
     * Checks the settings for restrictions imposed by persistence. Throws an exception if
     * anything does not meet the requirements.
     * - setting names must be singletons
     * - setting names and values must adhere to the preference input restrictions
     * - setting values are only tested for their current state, not the entire possible input space! It is tested again at persistence
     *
     * @throws UnsupportedOperationException if a setting does not fulfil the requirements
     */
    private void checkSettings() throws UnsupportedOperationException {
        List<Property<?>> tmpSettingsList = this.settings;
        int tmpSettingNamesSetInitCapacity = CollectionUtil.calculateInitialHashCollectionCapacity(tmpSettingsList.size(), BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        HashSet<String> tmpSettingNamesSet = new HashSet<>(tmpSettingNamesSetInitCapacity, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        for (Property<?> tmpSetting : tmpSettingsList) {
            if (!PreferenceUtil.isValidName(tmpSetting.getName())) {
                throw new UnsupportedOperationException(String.format("Setting %s has an invalid name.", tmpSetting.getName()));
            }
            if (tmpSettingNamesSet.contains(tmpSetting.getName())) {
                throw new UnsupportedOperationException(String.format("Setting name %s is used multiple times.", tmpSetting.getName()));
            } else {
                tmpSettingNamesSet.add(tmpSetting.getName());
            }
            switch (tmpSetting) {
                case SimpleBooleanProperty tmpSimpleBooleanProperty -> {
                    //nothing to do here, booleans cannot have invalid values
                }
                case SimpleIntegerProperty tmpSimpleIntegerProperty -> {
                    if (!SingleIntegerPreference.isValidContent(Integer.toString(tmpSimpleIntegerProperty.get()))) {
                        throw new UnsupportedOperationException(String.format("Setting value %d of setting name %s is invalid.", tmpSimpleIntegerProperty.get(), tmpSimpleIntegerProperty.getName()));
                    }
                }
                case SimpleDoubleProperty tmpSimpleDoubleProperty -> {
                    if (!SingleNumberPreference.isValidContent(tmpSimpleDoubleProperty.get())) {
                        throw new UnsupportedOperationException(String.format("Setting value %f of setting name %s is invalid.", tmpSimpleDoubleProperty.get(), tmpSetting.getName()));
                    }
                }
                case SimpleIDisplayEnumConstantProperty simpleIDisplayEnumConstantProperty -> {
                    if (!SingleTermPreference.isValidContent(((Enum)simpleIDisplayEnumConstantProperty.get()).name())) {
                        throw new UnsupportedOperationException("Setting value " + simpleIDisplayEnumConstantProperty.get() + " of setting name " + tmpSetting.getName() + " is invalid.");
                    }
                }
                case SimpleStringProperty tmpSimpleStringProperty -> {
                    //also true for SimpleEnumConstantNameProperty
                    if (!SingleTermPreference.isValidContent(tmpSimpleStringProperty.get())) {
                        throw new UnsupportedOperationException(String.format("Setting value %s of setting name %s is invalid.", tmpSimpleStringProperty.get(), tmpSetting.getName()));
                    }
                }
                default -> {
                    throw new UnsupportedOperationException(String.format("Setting %s is of an invalid type.", tmpSetting.getName()));
                }
            }
        }
    }

    /**
     * Tests whether an integer value would be an allowed argument for the rows per page setting. For this, it must be
     * positive and non-zero.
     *
     * @param anInteger the integer to test
     * @return true if the given parameter is a legal value for the setting
     */
    private boolean isLegalRowsPerPageSetting(int anInteger) {
        return anInteger > 0;
    }

    /**
     * Tests whether an integer value would be an allowed argument for the number of tasks for fragmentation setting. For
     * this, it must be positive, non-zero, and not higher than the number of available processors.
     *
     * @param anInteger the integer to test
     * @return true if the given parameter is a legal value for the setting
     */
    private boolean isLegalNumberOfTasksForFragmentationSetting(int anInteger) {
        return !(anInteger <= 0 || anInteger > SettingsContainer.MAX_AVAILABLE_THREADS);
    }

    /**
     * Tests whether a path would be an allowed argument for the recent directory path setting. For this, it must be
     * not null, not empty, existing, a directory, and readable.
     *
     * @param aPath the path to test
     * @return true if the given parameter is a legal value for the setting
     */
    private boolean isLegalRecentDirectoryPath(String aPath) {
        boolean tmpIsNull = Objects.isNull(aPath);
        if (tmpIsNull) {
            return false;
        }
        boolean tmpIsEmpty = aPath.trim().isEmpty();
        if (!SingleTermPreference.isValidContent(aPath.trim())) {
            return false;
        }
        File tmpFile = new File(aPath);
        boolean tmpExists = tmpFile.exists();
        boolean tmpIsDirectory = tmpFile.isDirectory();
        boolean tmpCanRead = tmpFile.canRead();
        return !tmpIsEmpty && tmpExists && tmpIsDirectory && tmpCanRead;
    }

    /**
     * Tests whether a character is an allowed separator for csv export. For this, it must be not null, not empty, not blank
     * and must be findable in the exporter csv separator enum.
     *
     * @param aSeparator the separator to test
     * @return true if the given parameter is a legal value for the setting
     */
    private boolean isLegalCsvExportSeparator(char aSeparator){
        for (Enum<Exporter.CSVSeparator> tmpEnumConstant : Exporter.CSVSeparator.values()) {
            if (aSeparator == ((Exporter.CSVSeparator) tmpEnumConstant).getSeparatorChar()) {
                return true;
            }
        }
        return false;
    }
    //</editor-fold>
}
