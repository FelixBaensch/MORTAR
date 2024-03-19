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

import de.unijena.cheminf.mortar.configuration.Configuration;
import de.unijena.cheminf.mortar.model.io.Exporter;

import javafx.beans.property.Property;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

/**
 * Test class for testing and demonstrating the usage of SettingsContainer class.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SettingsContainerTest {
    /**
     * Constructor to initialize locale and configuration.
     */
    public SettingsContainerTest() throws Exception {
        Locale.setDefault(new Locale("en", "GB"));
        Configuration.getInstance();
    }
    /**
     * Tests the basic functionalities of SettingsContainer. These are instantiation, restoring default settings,
     * getting the settings, changing the settings, persisting the settings, and reloading them.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSettingsContainerBasics() throws Exception {
        Exporter.CSVSeparator tmpCsvExportSeparatorTest = Exporter.CSVSeparator.COMMA;
        //if there is a persisted settings container file already on the machine, it is loaded into the new SettingsContainer object
        SettingsContainer tmpSettingsContainer = new SettingsContainer();
        //restoring to default because a previous settings file with altered settings may have been imported (see below)
        tmpSettingsContainer.restoreDefaultSettings();
        List<Property<?>> tmpPropertiesList = tmpSettingsContainer.settingsProperties();
        System.out.println();
        for (Property<?> tmpProp : tmpPropertiesList) {
            //recent directory path setting is not included because it is an internal setting
            System.out.println(tmpProp.getName() + ": " + tmpProp.getValue());
        }
        System.out.println(tmpSettingsContainer.recentDirectoryPathSettingProperty().getName() + ": "
                + tmpSettingsContainer.recentDirectoryPathSettingProperty().getValue());
        System.out.println();
        Assertions.assertEquals(SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT, tmpSettingsContainer.getRowsPerPageSetting());
        Assertions.assertEquals(SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT, tmpSettingsContainer.getAddImplicitHydrogensAtImportSetting());
        Assertions.assertEquals(tmpSettingsContainer.getNumberOfTasksForFragmentationSettingDefault(), tmpSettingsContainer.getNumberOfTasksForFragmentationSetting());
        Assertions.assertEquals(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT, tmpSettingsContainer.getRecentDirectoryPathSetting());
        Assertions.assertEquals(SettingsContainer.KEEP_ATOM_CONTAINER_IN_DATA_MODEL_SETTING_DEFAULT, tmpSettingsContainer.getKeepAtomContainerInDataModelSetting());
        Assertions.assertEquals(SettingsContainer.ALWAYS_MDLV3000_FORMAT_AT_EXPORT_SETTING_DEFAULT, tmpSettingsContainer.getAlwaysMDLV3000FormatAtExportSetting());
        Assertions.assertEquals(SettingsContainer.CSV_EXPORT_SEPARATOR_SETTING_DEFAULT.name(), tmpSettingsContainer.getCsvExportSeparatorSetting());
        tmpSettingsContainer.setRowsPerPageSetting(SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT + 5);
        tmpSettingsContainer.setAddImplicitHydrogensAtImportSetting(!SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT);
        //tmpSettingsContainer.setNumberOfTasksForFragmentationSetting(tmpSettingsContainer.getNumberOfTasksForFragmentationSettingDefault() - 1);
        //tmpSettingsContainer.setKeepAtomContainerInDataModelSetting(!SettingsContainer.KEEP_ATOM_CONTAINER_IN_DATA_MODEL_SETTING_DEFAULT);
        tmpSettingsContainer.setAlwaysMDLV3000FormatAtExportSetting(!SettingsContainer.ALWAYS_MDLV3000_FORMAT_AT_EXPORT_SETTING_DEFAULT);
        tmpSettingsContainer.setCsvExportSeparatorSetting(tmpCsvExportSeparatorTest.name());
        //persisting the settings container
        tmpSettingsContainer.preserveSettings();
        //reload persisted container
        SettingsContainer tmpSecondContainer = new SettingsContainer();
        tmpSecondContainer.reloadGlobalSettings();
        Assertions.assertEquals(SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT + 5, tmpSecondContainer.getRowsPerPageSetting());
        Assertions.assertEquals(!SettingsContainer.ADD_IMPLICIT_HYDROGENS_AT_IMPORT_SETTING_DEFAULT, tmpSecondContainer.getAddImplicitHydrogensAtImportSetting());
        //Assertions.assertEquals(tmpSettingsContainer.getNumberOfTasksForFragmentationSettingDefault() - 1, tmpSecondContainer.getNumberOfTasksForFragmentationSetting());
        //Assertions.assertEquals(!SettingsContainer.KEEP_ATOM_CONTAINER_IN_DATA_MODEL_SETTING_DEFAULT, tmpSettingsContainer.getKeepAtomContainerInDataModelSetting());
        Assertions.assertEquals(!SettingsContainer.ALWAYS_MDLV3000_FORMAT_AT_EXPORT_SETTING_DEFAULT, tmpSettingsContainer.getAlwaysMDLV3000FormatAtExportSetting());
        Assertions.assertEquals(tmpCsvExportSeparatorTest.name(), tmpSecondContainer.getCsvExportSeparatorSetting());
        tmpSecondContainer.restoreDefaultSettings();
        tmpSecondContainer.preserveSettings();
    }
}
