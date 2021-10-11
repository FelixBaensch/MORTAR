/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.model.settings;

/**
 * TODO:
 * - Important note for developers: When adding a new setting represented by a string, also consider the
 * SingleTermPreference class input restrictions when testing whether an input is valid!
 */

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.preference.BooleanPreference;
import de.unijena.cheminf.mortar.preference.PreferenceContainer;
import de.unijena.cheminf.mortar.preference.SingleIntegerPreference;
import de.unijena.cheminf.mortar.preference.SingleTermPreference;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Container for general settings in MORTAR, capable of managing, preserving, and reloading application settings.
 * Externally, the settings can be accessed via JavaFX properties and internally, they are managed via
 * de.unijena.cheminf.mortar.preference.IPreference objects for persistence.
 */
public class SettingsContainer {
    //<editor-fold desc="private static final constants" defaultstate="collapsed">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(SettingsContainer.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="public static final constants" defaultstate="collapsed">
    /**
     * Name of preference wrapping the number of rows per page setting.
     */
    public static final String ROWS_PER_PAGE_PREFERENCE_NAME = "Rows per page preference";

    /**
     * Default value of the number of rows per page.
     */
    public static final int ROWS_PER_PAGE_SETTING_DEFAULT = 5;

    /**
     * Name of preference wrapping the number of parallel tasks to use for fragmentation.
     */
    public static final String NR_OF_TASKS_FOR_FRAGMENTATION_PREFERENCE_NAME = "Nr of tasks for fragmentation preference";

    /**
     * Default value of the number of parallel tasks to use for fragmentation.
     */
    public static final int NR_OF_TASKS_FOR_FRAGMENTATION_SETTING_DEFAULT = Runtime.getRuntime().availableProcessors();

    /**
     * Name of preference wrapping the last directory used by the user.
     */
    public static final String RECENT_DIRECTORY_PATH_PREFERENCE_NAME = "Recent directory path preference";

    /**
     * Default value of the recent directory to use when there is no last directory used by the user.
     */
    public static final String RECENT_DIRECTORY_PATH_SETTING_DEFAULT = System.getProperty("user.home");

    /**
     * Name of preference wrapping the setting whether to add implicit hydrogen atoms to open valences in the imported
     * molecules.
     */
    public static final String ADD_IMPLICIT_HYDROGENS_AT_IMPORT_PREFERENCE_NAME = "Add implicit hydrogens at import preference";

    /**
     * Default value of whether to add implicit hydrogen atoms to open valences in the imported molecules.
     */
    public static final boolean ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT = true;

    /**
     * Name of preference wrapping the setting whether to keep the atom container in the molecule/fragment data model.
     */
    public static final String KEEP_ATOM_CONTAINER_IN_DATA_MODEL_PREFERENCE_NAME = "Keep atom container in data model preference";

    /**
     * Default value of whether to keep the atom container in the molecule/fragment data model.
     */
    public static final boolean KEEP_ATOM_CONTAINER_IN_DATA_MODEL_SETTING_DEFAULT = false;

    /**
     * Name of preference wrapping the setting whether to always use the MDL V3000 format for file export.
     */
    public static final String ALWAYS_MDLV3000_FORMAT_AT_EXPORT_PREFERENCE_NAME = "Always use MDL V3000 format at export preference";

    /**
     * Default value of whether to always use the MDL V3000 format for file export.
     */
    public static final boolean ALWAYS_MDLV3000_FORMAT_AT_EXPORT_SETTING_DEFAULT = false;
    //</editor-fold>
    //
    //<editor-fold desc="private variables">
    /**
     * Property of rows per page setting.
     */
    private SimpleIntegerProperty rowsPerPageSetting;

    /**
     * Preference of rows per page setting.
     */
    private SingleIntegerPreference rowsPerPagePreference;

    /**
     * Property of number of fragmentation tasks setting.
     */
    private SimpleIntegerProperty numberOfTasksForFragmentationSetting;

    /**
     * Preference of number of fragmentation tasks setting.
     */
    private SingleIntegerPreference numberOfTasksForFragmentationPreference;

    /**
     * Property of recent directory.
     */
    private SimpleStringProperty recentDirectoryPathSetting;

    /**
     * Preference of recent directory.
     */
    private SingleTermPreference recentDirectoryPathPreference;

    /**
     * Property of implicit hydrogens setting.
     */
    private SimpleBooleanProperty addImplicitHydrogensAtImportSetting;

    /**
     * Preference of implicit hydrogens setting.
     */
    private BooleanPreference addImplicitHydrogensAtImportPreference;

    /**
     * Property of keep atom container setting.
     */
    private SimpleBooleanProperty keepAtomContainerInDataModelSetting;

    /**
     * Preference of keep atom container setting.
     */
    private BooleanPreference keepAtomContainerInDataModelPreference;

    /**
     * Property of always MDL V3000 format setting.
     */
    private SimpleBooleanProperty alwaysMDLV3000FormatAtExportSetting;

    /**
     * Preference of always MDL V3000 format setting.
     */
    private BooleanPreference alwaysMDLV3000FormatAtExportPreference;

    /**
     * List of setting to display in the general settings dialogue; excludes recent directory path because this is only
     * for internal use, not intended to be changed by the user via this dialogue.
     */
    private List<Property> settings;

    /**
     * Internal preference container for persisting the settings via their analogous preference objects stored in this
     * container.
     */
    private PreferenceContainer preferenceContainer;
    //</editor-fold>
    //
    //<editor-fold desc="constructors">
    /**
     * Constructor that first tries to reload a previously persisted preference container from the respective directory
     * specified in the basic definitions to reload the settings from the previous session. If this fails, the settings
     * (and preferences) are set to their respective default values.
     */
    public SettingsContainer() {
        String tmpPreferenceContainerDirectoryPathName = FileUtil.getAppDirPath() + File.separator
                + BasicDefinitions.PREFERENCE_CONTAINER_FILE_DIRECTORY + File.separator;
        File tmpPreferencesDirectoryFile = new File(tmpPreferenceContainerDirectoryPathName);
        String tmpPreferenceContainerFilePathName = tmpPreferenceContainerDirectoryPathName
                + BasicDefinitions.PREFERENCE_CONTAINER_FILE_NAME
                + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION;
        File tmpPreferenceContainerFile = new File(tmpPreferenceContainerFilePathName);
        boolean tmpDoNewInitialisation = false;
        if (!tmpPreferencesDirectoryFile.exists()) {
            FileUtil.createDirectory(tmpPreferencesDirectoryFile.getAbsolutePath());
            tmpDoNewInitialisation = true;
        } else {
            boolean tmpExists = tmpPreferenceContainerFile.exists();
            boolean tmpIsFile = tmpPreferenceContainerFile.isFile();
            boolean tmpCanRead = tmpPreferenceContainerFile.canRead();
            if (!tmpExists || !tmpIsFile || !tmpCanRead) {
                SettingsContainer.LOGGER.info("Preference container file does not exist or cannot be read. " +
                        "A new one is initialised.");
                tmpDoNewInitialisation = true;
            } else {
                try {
                    this.extractGeneralSettingsPreferencesFromContainer(tmpPreferenceContainerFile);
                } catch (IOException | SecurityException anException) {
                    SettingsContainer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                    tmpDoNewInitialisation = true;
                }
            }
        }
        if (tmpDoNewInitialisation) {
            this.initialisePreferences(tmpPreferenceContainerFilePathName);
        }
        this.initialiseSettings();
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
    public List<Property> settingsProperties() {
        return this.settings;
    }

    /**
     * Returns the current value of the rows or molecules per page setting.
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
    public Property rowsPerPageSettingProperty() {
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
    public Property numberOfTasksForFragmentationSettingProperty() {
        return this.numberOfTasksForFragmentationSetting;
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
    public Property recentDirectoryPathSettingProperty() {
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
    public Property addImplicitHydrogensAtImportSettingProperty() {
        return this.addImplicitHydrogensAtImportSetting;
    }

    /**
     * Returns the current value of the keep atom container in data model setting.
     *
     * @return keep atom container in data model setting value
     */
    public boolean getKeepAtomContainerInDataModelSetting() {
        return this.keepAtomContainerInDataModelSetting.get();
    }

    /**
     * Returns the property wrapping the keep atom container in data model setting.
     *
     * @return keep atom container in data model setting property
     */
    public Property keepAtomContainerInDataModelSettingProperty() {
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
    public Property alwaysMDLV3000FormatAtExportSettingProperty() {
        return this.alwaysMDLV3000FormatAtExportSetting;
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
     */
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
     * Restores all setting to their default setting according to the respective public constants in this class.
     */
    public void restoreDefaultSettings() {
        this.rowsPerPageSetting.set(SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT);
        this.numberOfTasksForFragmentationSetting.set(SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_SETTING_DEFAULT);
        this.recentDirectoryPathSetting.set(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
        this.addImplicitHydrogensAtImportSetting.set(SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT);
        this.keepAtomContainerInDataModelSetting.set(SettingsContainer.KEEP_ATOM_CONTAINER_IN_DATA_MODEL_SETTING_DEFAULT);
        this.alwaysMDLV3000FormatAtExportSetting.set(SettingsContainer.ALWAYS_MDLV3000_FORMAT_AT_EXPORT_SETTING_DEFAULT);
    }
    //</editor-fold>
    //
    //<editor-fold desc="public methods">
    /**
     * Triggers the preservation of the current settings values in a file for re-import at the next application session.
     *
     * @throws IOException if anything goes wrong
     * @throws SecurityException if the MORTAR settings directory cannot be accessed
     */
    public void preserveSettings() throws IOException, SecurityException {
        this.preferenceContainer.writeRepresentation();
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods">
    /**
     * Reloads the setting values from a previous MORTAR session via the persisted preference container.
     *
     * @param aPreferenceContainerFile the persisted preference container representation from the previous run
     * @throws IOException if the file loading fails or if a setting cannot be re-imported
     * @throws SecurityException if the given file cannot be accessed
     */
    private void extractGeneralSettingsPreferencesFromContainer(File aPreferenceContainerFile) throws IOException, SecurityException {
        this.preferenceContainer = new PreferenceContainer(aPreferenceContainerFile);
        if (this.preferenceContainer.containsPreferenceName(SettingsContainer.ROWS_PER_PAGE_PREFERENCE_NAME)) {
            this.rowsPerPagePreference =
                    (SingleIntegerPreference) this.preferenceContainer.getPreferences(
                            SettingsContainer.ROWS_PER_PAGE_PREFERENCE_NAME)[0];
        } else {
            throw new IOException("One or multiple settings could not be restored from the previous run.");
        }
        if (this.preferenceContainer.containsPreferenceName(SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_PREFERENCE_NAME)) {
            this.numberOfTasksForFragmentationPreference =
                    (SingleIntegerPreference) this.preferenceContainer.getPreferences(
                            SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_PREFERENCE_NAME)[0];
        } else {
            throw new IOException("One or multiple settings could not be restored from the previous run.");
        }
        if (this.preferenceContainer.containsPreferenceName(SettingsContainer.RECENT_DIRECTORY_PATH_PREFERENCE_NAME)) {
            this.recentDirectoryPathPreference =
                    (SingleTermPreference) this.preferenceContainer.getPreferences(
                            SettingsContainer.RECENT_DIRECTORY_PATH_PREFERENCE_NAME)[0];
        } else {
            throw new IOException("One or multiple settings could not be restored from the previous run.");
        }
        if (this.preferenceContainer.containsPreferenceName(SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_PREFERENCE_NAME)) {
            this.addImplicitHydrogensAtImportPreference =
                    (BooleanPreference) this.preferenceContainer.getPreferences(
                            SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_PREFERENCE_NAME)[0];
        } else {
            throw new IOException("One or multiple settings could not be restored from the previous run.");
        }
        if (this.preferenceContainer.containsPreferenceName(SettingsContainer.KEEP_ATOM_CONTAINER_IN_DATA_MODEL_PREFERENCE_NAME)) {
            this.keepAtomContainerInDataModelPreference =
                    (BooleanPreference) this.preferenceContainer.getPreferences(
                            SettingsContainer.KEEP_ATOM_CONTAINER_IN_DATA_MODEL_PREFERENCE_NAME)[0];
        } else {
            throw new IOException("One or multiple settings could not be restored from the previous run.");
        }
        if (this.preferenceContainer.containsPreferenceName(SettingsContainer.ALWAYS_MDLV3000_FORMAT_AT_EXPORT_PREFERENCE_NAME)) {
            this.alwaysMDLV3000FormatAtExportPreference =
                    (BooleanPreference) this.preferenceContainer.getPreferences(
                            SettingsContainer.ALWAYS_MDLV3000_FORMAT_AT_EXPORT_PREFERENCE_NAME)[0];
        } else {
            throw new IOException("One or multiple settings could not be restored from the previous run.");
        }
    }

    /**
     * Instantiates a new preference container and new preference objects for all setting based on the default values.
     *
     * @param aPreferenceContainerFilePathName path of a file where the new setting container should persist itself
     */
    private void initialisePreferences(String aPreferenceContainerFilePathName) {
        this.preferenceContainer = new PreferenceContainer(aPreferenceContainerFilePathName);
        this.rowsPerPagePreference = new SingleIntegerPreference(SettingsContainer.ROWS_PER_PAGE_PREFERENCE_NAME,
                SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT);
        this.preferenceContainer.add(this.rowsPerPagePreference);
        this.numberOfTasksForFragmentationPreference = new SingleIntegerPreference(
                SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_PREFERENCE_NAME,
                SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_SETTING_DEFAULT);
        this.preferenceContainer.add(this.numberOfTasksForFragmentationPreference);
        this.recentDirectoryPathPreference = new SingleTermPreference(
                SettingsContainer.RECENT_DIRECTORY_PATH_PREFERENCE_NAME,
                SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
        this.preferenceContainer.add(this.recentDirectoryPathPreference);
        this.addImplicitHydrogensAtImportPreference = new BooleanPreference(
                SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_PREFERENCE_NAME,
                SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT);
        this.preferenceContainer.add(this.addImplicitHydrogensAtImportPreference);
        this.keepAtomContainerInDataModelPreference = new BooleanPreference(
                SettingsContainer.KEEP_ATOM_CONTAINER_IN_DATA_MODEL_PREFERENCE_NAME,
                SettingsContainer.KEEP_ATOM_CONTAINER_IN_DATA_MODEL_SETTING_DEFAULT);
        this.preferenceContainer.add(this.keepAtomContainerInDataModelPreference);
        this.alwaysMDLV3000FormatAtExportPreference = new BooleanPreference(
                SettingsContainer.ALWAYS_MDLV3000_FORMAT_AT_EXPORT_PREFERENCE_NAME,
                SettingsContainer.ALWAYS_MDLV3000_FORMAT_AT_EXPORT_SETTING_DEFAULT);
        this.preferenceContainer.add(this.alwaysMDLV3000FormatAtExportPreference);
    }

    /**
     * Initialises the properties representing the settings based on the respective preference objects. Related
     * preferences and properties are synced via overriding the properties set() methods and the properties are added
     * to the list of settings for display to the user.
     */
    private void initialiseSettings() {
        this.rowsPerPageSetting = new SimpleIntegerProperty(this,
                "Rows per page setting",
                this.rowsPerPagePreference.getContent()) {
            @Override
            public void set(int newValue) throws IllegalArgumentException {
                if (SettingsContainer.this.isLegalRowsPerPageSetting(newValue)) {
                    SettingsContainer.this.rowsPerPagePreference.setContent(newValue);
                    super.set(newValue);
                } else {
                    IllegalArgumentException tmpException = new IllegalArgumentException("An illegal rows per page number was given: " + newValue);
                    SettingsContainer.this.LOGGER.log(Level.WARNING, tmpException.toString(), tmpException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", tmpException.toString(), tmpException);
                    //re-throws the exception to properly reset the binding
                    throw tmpException;
                }
            }
        };
        this.numberOfTasksForFragmentationSetting = new SimpleIntegerProperty(this,
                "Nr of tasks for fragmentation setting",
                this.numberOfTasksForFragmentationPreference.getContent()) {
            @Override
            public void set(int newValue) throws IllegalArgumentException {
                if (SettingsContainer.this.isLegalNumberOfTasksForFragmentationSetting(newValue)) {
                    SettingsContainer.this.numberOfTasksForFragmentationPreference.setContent(newValue);
                    super.set(newValue);
                } else {
                    IllegalArgumentException tmpException = new IllegalArgumentException("An illegal number of tasks for fragmentation was given: " + newValue);
                    SettingsContainer.this.LOGGER.log(Level.WARNING, tmpException.toString(), tmpException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", tmpException.toString(), tmpException);
                    //re-throws the exception to properly reset the binding
                    throw tmpException;
                }
            }
        };
        this.recentDirectoryPathSetting = new SimpleStringProperty(this,
                "Recent directory path setting",
                this.recentDirectoryPathPreference.getContent()) {
            @Override
            public void set(String newValue) throws IllegalArgumentException {
                if (SettingsContainer.this.isLegalRecentDirectoryPath(newValue)) {
                    SettingsContainer.this.recentDirectoryPathPreference.setContent(newValue);
                    super.set(newValue);
                } else {
                    IllegalArgumentException tmpException = new IllegalArgumentException("An illegal number of tasks for fragmentation was given: " + newValue);
                    SettingsContainer.this.LOGGER.log(Level.WARNING, tmpException.toString(), tmpException);
                    //no GUI alert here because this is an internal setting
                    //re-throws the exception to properly reset the binding
                    throw tmpException;
                }
            }
        };
        this.addImplicitHydrogensAtImportSetting = new SimpleBooleanProperty(this,
                "Add implicit hydrogens at import setting",
                this.addImplicitHydrogensAtImportPreference.getContent()) {
            @Override
            public void set(boolean newValue) {
                SettingsContainer.this.addImplicitHydrogensAtImportPreference.setContent(newValue);
                super.set(newValue);
            }
        };
        this.keepAtomContainerInDataModelSetting = new SimpleBooleanProperty(this,
                "Keep AtomContainers in the DataModels setting",
                this.keepAtomContainerInDataModelPreference.getContent()) {
            @Override
            public void set(boolean newValue) {
                SettingsContainer.this.keepAtomContainerInDataModelPreference.setContent(newValue);
                super.set(newValue);
            }
        };
        this.alwaysMDLV3000FormatAtExportSetting = new SimpleBooleanProperty(this,
                "Always MDL V3000 format at export setting",     //TODO: better phrasing??   @Samuel
                this.alwaysMDLV3000FormatAtExportPreference.getContent()) {
            @Override
            public void set(boolean newValue) {
                SettingsContainer.this.alwaysMDLV3000FormatAtExportPreference.setContent(newValue);
                super.set(newValue);
            }
        };
        this.settings = new ArrayList<Property>(3);
        this.settings.add(this.rowsPerPageSetting);
        this.settings.add(this.numberOfTasksForFragmentationSetting);
        this.settings.add(this.addImplicitHydrogensAtImportSetting);
        this.settings.add(this.keepAtomContainerInDataModelSetting);
        this.settings.add(this.alwaysMDLV3000FormatAtExportSetting);
        //note: recent directory path is only internal, all settings in the list are for the user
    }

    /**
     * Tests whether an integer value would be an allowed argument for the rows per page setting. For this, it must be
     * positive and non-zero.
     *
     * @param anInteger the integer to test
     * @return true if the given parameter is a legal value for the setting
     */
    private boolean isLegalRowsPerPageSetting(int anInteger) {
        return !(anInteger <= 0);
    }

    /**
     * Tests whether an integer value would be an allowed argument for the number of tasks for fragmentation setting. For
     * this, it must be positive, non-zero, and not higher than the number of available processors.
     *
     * @param anInteger the integer to test
     * @return true if the given parameter is a legal value for the setting
     */
    private boolean isLegalNumberOfTasksForFragmentationSetting(int anInteger) {
        return !(anInteger <= 0 || anInteger > SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_SETTING_DEFAULT);
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
        if (tmpIsEmpty || !tmpExists || !tmpIsDirectory || !tmpCanRead) {
            return false;
        } else {
            return true;
        }
    }
    //</editor-fold>
}
