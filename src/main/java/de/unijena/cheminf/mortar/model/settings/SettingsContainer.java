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
 * - Test persistence
 * - clean-up
 * - include it in main view controller, especially at start-up and exiting
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
 *
 */
public class SettingsContainer {

    //<editor-fold desc="private static final constants" defaultstate="collapsed">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(SettingsContainer.class.getName());

    //TODO: Move the following to definitions?
    /**
     *
     */
    private static final String ROWS_PER_PAGE_PREFERENCE_NAME = "Rows per page preference";

    /**
     *
     */
    private static final int ROWS_PER_PAGE_SETTING_DEFAULT = 5;

    /**
     *
     */
    private static final String NR_OF_TASKS_FOR_FRAGMENTATION_PREFERENCE_NAME = "Nr of tasks for fragmentation preference";

    /**
     *
     */
    private static final int NR_OF_TASKS_FOR_FRAGMENTATION_SETTING_DEFAULT = Runtime.getRuntime().availableProcessors();

    /**
     *
     */
    private static final String RECENT_DIRECTORY_PATH_PREFERENCE_NAME = "Recent directory path preference";

    /**
     *
     */
    private static final String RECENT_DIRECTORY_PATH_SETTING_DEFAULT = System.getProperty("user.home");

    /**
     *
     */
    private static final String ADD_IMPLICIT_HYDROGENS_AT_IMPORT_PREFERENCE_NAME = "Add implicit hydrogens at import preference name";

    /**
     *
     */
    private static final boolean ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT = true;
    //</editor-fold>
    //
    //<editor-fold desc="private variables">
    /**
     *
     */
    private SimpleIntegerProperty rowsPerPageSetting;

    /**
     *
     */
    private SingleIntegerPreference rowsPerPagePreference;

    /**
     *
     */
    private SimpleIntegerProperty numberOfTasksForFragmentationSetting;

    /**
     *
     */
    private SingleIntegerPreference numberOfTasksForFragmentationPreference;

    /**
     *
     */
    private SimpleStringProperty recentDirectoryPathSetting;

    /**
     *
     */
    private SingleTermPreference recentDirectoryPathPreference;

    /**
     *
     */
    private SimpleBooleanProperty addImplicitHydrogensAtImportSetting;

    /**
     *
     */
    private BooleanPreference addImplicitHydrogensAtImportPreference;

    /**
     *
     */
    private List<Property> settings;

    /**
     *
     */
    private PreferenceContainer preferenceContainer;
    //</editor-fold>
    //
    //<editor-fold desc="constructors">
    /**
     *
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
    /**
     *
     */
    public List<Property> settingsProperties() {
        return this.settings;
    }

    public int getRowsPerPageSetting() {
        return this.rowsPerPageSetting.get();
    }

    public Property rowsPerPageSettingProperty() {
        return this.rowsPerPageSetting;
    }

    public int getNumberOfTasksForFragmentationSetting() {
        return this.numberOfTasksForFragmentationSetting.get();
    }

    public Property numberOfTasksForFragmentationSettingProperty() {
        return this.numberOfTasksForFragmentationSetting;
    }

    public String getRecentDirectoryPathSetting() {
        return this.recentDirectoryPathSetting.get();
    }

    public Property recentDirectoryPathSettingProperty() {
        return this.recentDirectoryPathSetting;
    }

    public boolean getAddImplicitHydrogensAtImportSetting() {
        return this.addImplicitHydrogensAtImportSetting.get();
    }

    public Property addImplicitHydrogensAtImportSettingProperty() {
        return this.addImplicitHydrogensAtImportSetting;
    }

    public void setRowsPerPageSetting(int anInteger) throws IllegalArgumentException {
        if (this.isLegalRowsPerPageSetting(anInteger)) {
            //synchronises the preference also
            this.rowsPerPageSetting.set(anInteger);
        } else {
            throw new IllegalArgumentException("The given rows per page number is 0 or negative.");
        }
    }

    public void setNumberOfTasksForFragmentationSetting(int anInteger) throws IllegalArgumentException {
        if (this.isLegalNumberOfTasksForFragmentationSetting(anInteger)) {
            //synchronises the preference also
            this.numberOfTasksForFragmentationSetting.set(anInteger);
        } else {
            throw new IllegalArgumentException("The given number of tasks for fragmentation is 0 or negative or higher "
                   + "than the number of available processors.");
        }
    }

    public void setRecentDirectoryPathSetting(String aPath) throws IllegalArgumentException {
        if (this.isLegalRecentDirectoryPath(aPath)) {
            //synchronises the preference also
            this.recentDirectoryPathSetting.set(aPath);
        } else {
            throw new IllegalArgumentException("Given recent directory path is null, empty, does not exist, " +
                    "is no directory, or cannot be read.");
        }
    }

    public void setAddImplicitHydrogensAtImportSetting(boolean aBoolean) {
        //synchronises the preference also
        this.addImplicitHydrogensAtImportSetting.set(aBoolean);
    }

    /**
     *
     */
    public void restoreDefaultSettings() {
        this.rowsPerPageSetting.set(SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT);
        this.numberOfTasksForFragmentationSetting.set(SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_SETTING_DEFAULT);
        this.recentDirectoryPathSetting.set(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
        this.addImplicitHydrogensAtImportSetting.set(SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT);
    }

    /**
     *
     */
    public void preserveSettings() throws IOException, SecurityException {
        this.preferenceContainer.writeRepresentation();
    }

    /**
     *
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
    }

    /**
     *
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
    }

    /**
     * Test the reloaded settings also?
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
        this.settings = new ArrayList<Property>(3);
        this.settings.add(this.rowsPerPageSetting);
        this.settings.add(this.numberOfTasksForFragmentationSetting);
        this.settings.add(this.addImplicitHydrogensAtImportSetting);
        //note: recent directory path is only internal, all settings in the list are for the user
    }

    private boolean isLegalRowsPerPageSetting(int anInteger) {
        return !(anInteger <= 0);
    }

    private boolean isLegalNumberOfTasksForFragmentationSetting(int anInteger) {
        return !(anInteger <= 0 || anInteger > SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_SETTING_DEFAULT);
    }

    private boolean isLegalRecentDirectoryPath(String aPath) {
        boolean tmpIsNull = Objects.isNull(aPath);
        if (tmpIsNull) {
            return false;
        }
        boolean tmpIsEmpty = aPath.trim().isEmpty();
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
}
