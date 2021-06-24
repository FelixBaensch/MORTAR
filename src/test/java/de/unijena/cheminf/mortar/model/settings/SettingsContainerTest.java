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

import javafx.beans.property.Property;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test class for testing and demonstrating the usage of SettingsContainer class.
 */
public class SettingsContainerTest {
    /**
     * Tests the basic functionalities of SettingsContainer. These are instantiation, restoring default settings,
     * getting the settings, changing the settings, persisting the settings, and reloading them.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSettingsContainerBasics() throws Exception {
        //if there is a persisted settings container file already on the machine, it is loaded into the new SettingsContainer object
        SettingsContainer tmpSettingsContainer = new SettingsContainer();
        //restoring to default because a previous settings file with altered settings may have been imported (see below)
        tmpSettingsContainer.restoreDefaultSettings();
        List<Property> tmpPropertiesList = tmpSettingsContainer.settingsProperties();
        System.out.println();
        for (Property tmpProp : tmpPropertiesList) {
            //recent directory path setting is not included because it is an internal setting
            System.out.println(tmpProp.getName() + ": " + tmpProp.getValue());
        }
        System.out.println(tmpSettingsContainer.recentDirectoryPathSettingProperty().getName() + ": "
                + tmpSettingsContainer.recentDirectoryPathSettingProperty().getValue());
        System.out.println();
        Assert.assertEquals(SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT, tmpSettingsContainer.getRowsPerPageSetting());
        Assert.assertEquals(SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT, tmpSettingsContainer.getAddImplicitHydrogensAtImportSetting());
        Assert.assertEquals(SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_SETTING_DEFAULT, tmpSettingsContainer.getNumberOfTasksForFragmentationSetting());
        Assert.assertEquals(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT, tmpSettingsContainer.getRecentDirectoryPathSetting());
        //TODO @Samuel
        Assert.assertEquals(SettingsContainer.KEEP_ATOM_CONTAINER_IN_DATA_MODEL_SETTING_DEFAULT, tmpSettingsContainer.getKeepAtomContainerInDataModelSetting());
        tmpSettingsContainer.setRowsPerPageSetting(SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT + 5);
        tmpSettingsContainer.setAddImplicitHydrogensAtImportSetting(!SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT);
        tmpSettingsContainer.setNumberOfTasksForFragmentationSetting(SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_SETTING_DEFAULT - 1);
        //TODO @Samuel
        tmpSettingsContainer.setKeepAtomContainerInDataModelSetting(!SettingsContainer.KEEP_ATOM_CONTAINER_IN_DATA_MODEL_SETTING_DEFAULT);
        //persisting the settings container
        tmpSettingsContainer.preserveSettings();
        //reload persisted container
        SettingsContainer tmpSecondContainer = new SettingsContainer();
        Assert.assertEquals(SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT + 5, tmpSecondContainer.getRowsPerPageSetting());
        Assert.assertEquals(!SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT, tmpSecondContainer.getAddImplicitHydrogensAtImportSetting());
        Assert.assertEquals(SettingsContainer.NR_OF_TASKS_FOR_FRAGMENTATION_SETTING_DEFAULT - 1, tmpSecondContainer.getNumberOfTasksForFragmentationSetting());
        //TODO @Samuel
        Assert.assertEquals(!SettingsContainer.KEEP_ATOM_CONTAINER_IN_DATA_MODEL_SETTING_DEFAULT, tmpSettingsContainer.getKeepAtomContainerInDataModelSetting());
        tmpSecondContainer.restoreDefaultSettings();
        tmpSecondContainer.preserveSettings();
    }
}