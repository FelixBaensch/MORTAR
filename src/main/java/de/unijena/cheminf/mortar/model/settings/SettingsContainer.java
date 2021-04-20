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
import java.util.logging.Level;
import java.util.logging.Logger;

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
    //<editor-fold desc="private final variables">
    /**
     *
     */
    private final SimpleIntegerProperty rowsPerPageSetting;

    /**
     *
     */
    private SingleIntegerPreference rowsPerPagePreference;

    /**
     *
     */
    private final SimpleIntegerProperty numberOfTasksForFragmentationSetting;

    /**
     *
     */
    private SingleIntegerPreference numberOfTasksForFragmentationPreference;

    /**
     *
     */
    private final SimpleStringProperty recentDirectoryPathSetting;

    /**
     *
     */
    private SingleTermPreference recentDirectoryPathPreference;

    /**
     *
     */
    private final SimpleBooleanProperty addImplicitHydrogensAtImportSetting;

    /**
     *
     */
    private BooleanPreference addImplicitHydrogensAtImportPreference;

    /**
     *
     */
    private final List<Property> settings;

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
                    this.preferenceContainer = new PreferenceContainer(tmpPreferenceContainerFile);
                    this.rowsPerPagePreference = (SingleIntegerPreference) this.preferenceContainer.getPreferences(SettingsContainer.ROWS_PER_PAGE_PREFERENCE_NAME)[0];
                    this.numberOfTasksForFragmentationPreference = (SingleIntegerPreference) this.preferenceContainer.getPreferences(SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_PREFERENCE_NAME)[0];
                    this.recentDirectoryPathPreference = (SingleTermPreference) this.preferenceContainer.getPreferences(SettingsContainer.RECENT_DIRECTORY_PATH_PREFERENCE_NAME)[0];
                    this.addImplicitHydrogensAtImportPreference = (BooleanPreference) this.preferenceContainer.getPreferences(SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_PREFERENCE_NAME)[0];

                } catch (IOException | SecurityException anException) {
                    SettingsContainer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                    tmpDoNewInitialisation = true;
                }
            }
        }
        if (tmpDoNewInitialisation) {
            this.preferenceContainer = new PreferenceContainer(tmpPreferenceContainerFilePathName);
            this.rowsPerPagePreference = new SingleIntegerPreference(SettingsContainer.ROWS_PER_PAGE_PREFERENCE_NAME, SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT);
            this.preferenceContainer.add(this.rowsPerPagePreference);
            this.numberOfTasksForFragmentationPreference = new SingleIntegerPreference(SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_PREFERENCE_NAME, SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_SETTING_DEFAULT);
            this.preferenceContainer.add(this.numberOfTasksForFragmentationPreference);
            this.recentDirectoryPathPreference = new SingleTermPreference(SettingsContainer.RECENT_DIRECTORY_PATH_PREFERENCE_NAME, SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
            this.preferenceContainer.add(this.recentDirectoryPathPreference);
            this.addImplicitHydrogensAtImportPreference = new BooleanPreference(SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_PREFERENCE_NAME, SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT);
            this.preferenceContainer.add(this.addImplicitHydrogensAtImportPreference);
        }
        this.rowsPerPageSetting = new SimpleIntegerProperty(this, "Rows per page setting", this.rowsPerPagePreference.getContent()) {
            @Override
            public void set(int newValue) {
                //TODO: synchronise preference
                super.set(newValue);
            }
        };
        this.numberOfTasksForFragmentationSetting = new SimpleIntegerProperty(this, "Nr of tasks for fragmentation setting", this.numberOfTasksForFragmentationPreference.getContent()) {
            @Override
            public void set(int newValue) {
                //TODO: synchronise preference
                super.set(newValue);
            }
        };
        this.recentDirectoryPathSetting = new SimpleStringProperty(this, "Recent directory path setting", this.recentDirectoryPathPreference.getContent()) {
            @Override
            public void set(String newValue) {
                //TODO: synchronise preference
                super.set(newValue);
            }
        };
        this.addImplicitHydrogensAtImportSetting = new SimpleBooleanProperty(this, "Add implicit hydrogens at import setting", this.addImplicitHydrogensAtImportPreference.getContent()) {
            @Override
            public void set(boolean newValue) {
                //TODO: synchronise preference
                super.set(newValue);
            }
        };
        this.settings = new ArrayList<Property>(3);
        this.settings.add(this.rowsPerPageSetting);
        this.settings.add(this.numberOfTasksForFragmentationSetting);
        this.settings.add(this.addImplicitHydrogensAtImportSetting);
        //note: recent directory path is only internal, all settings in the list are for the user
    }
    //</editor-fold>
}
