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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.configuration.IConfiguration;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
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
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class that manages the GUI view tools like the histogram view and the overview view.
 *
 * @version 1.0.0.0
 * @author Jonas Schaub
 */
public class ViewToolsManager {
    //<editor-fold desc="public static final class constants" defaultstate="collapsed">
    /**
     * Folder name for storing the view tool settings within the usual MORTAR settings folder.
     */
    public static final String VIEW_TOOLS_SETTINGS_SUBFOLDER_NAME = "View_Tools_Settings";
    //</editor-fold>
    //
    //<editor-fold desc="private static final class constants" defaultstate="collapsed">
    /**
     * Logger
     */
    private static final Logger LOGGER = Logger.getLogger(ViewToolsManager.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="private final class constants" defaultstate="collapsed">
    /**
     * Array for the different view tools available.
     */
    private final IViewToolController[] viewToolsArray;
    /**
     * HistogramViewController instance.
     */
    private final HistogramViewController histogramViewController;
    /**
     * OverviewViewController instance.
     */
    private final OverviewViewController overviewViewController;
    /**
     * Configuration class to read resource file paths from.
     */
    private final IConfiguration configuration;
    //</editor-fold>
    //
    //<editor-fold desc="constructor" defaultstate="collapsed">
    /**
     * Constructor that initialises the view tool instances and checks whether they are all valid (see {@link #checkViewTools()}).
     * Opens a GUI exception alert if they are not.
     *
     * @param aConfiguration configuration instance to read resource file paths from
     */
    public ViewToolsManager(IConfiguration aConfiguration) {
        this.configuration = aConfiguration;
        this.viewToolsArray = new IViewToolController[2];
        this.histogramViewController = new HistogramViewController(this.configuration);
        this.viewToolsArray[0] = this.histogramViewController;
        this.overviewViewController = new OverviewViewController(this.configuration);
        this.viewToolsArray[1] = this.overviewViewController;
        try {
            this.checkViewTools();
        } catch (IOException anException) {
            ViewToolsManager.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("ViewToolsManager.Error.invalidSettingFormat"),
                    anException);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     * Returns the array containing the instances of available view tools.
     *
     * @return array with one instance per available view tool
     */
    public IViewToolController[] getViewToolControllers() {
        return this.viewToolsArray;
    }
    /**
     * See {@link HistogramViewController#openHistogramView(Stage, List)}.
     *
     * @param aMainStage Stage of the MainView
     * @param aFragmentDataModelList ObservableList that holds FragmentDataModel objects for visualisation in histogram
     * @throws NullPointerException if any param is null
     */
    public void openHistogramView(Stage aMainStage, List< FragmentDataModel > aFragmentDataModelList) throws NullPointerException {
        this.histogramViewController.openHistogramView(aMainStage, aFragmentDataModelList);
    }
    /**
     * See {@link OverviewViewController#initializeAndShowOverviewView(Stage, OverviewViewController.DataSources, String, List)}.
     *
     * @param aMainStage Stage that is to be the owner of the overview view's stage
     * @param aDataSource Source of the data to be shown in the overview view
     * @param aTabName String containing the name of the tab that's content is to be shown in the overview view
     * @param aMoleculeDataModelList List of MoleculeDataModel instances
     * @throws NullPointerException if one of the parameters is null; aTabName is allowed to be null if the value of aDataSource is
     *         PARENT_MOLECULES_SAMPLE or ITEM_WITH_FRAGMENTS_SAMPLE
     */
    public void openOverviewView(
            Stage aMainStage,
            OverviewViewController.DataSources aDataSource,
            String aTabName,
            List<MoleculeDataModel> aMoleculeDataModelList)
            throws NullPointerException {
        this.overviewViewController.initializeAndShowOverviewView(aMainStage, aDataSource, aTabName, aMoleculeDataModelList);
    }
    /**
     * See {@link OverviewViewController#getCachedIndexOfStructureInMoleculeDataModelList()}.
     *
     * @return Integer value of the cached index of structure or -1
     */
    public int getCachedIndexOfStructureInMoleculeDataModelList() {
        return this.overviewViewController.getCachedIndexOfStructureInMoleculeDataModelList();
    }
    /**
     * See {@link OverviewViewController#resetCachedIndexOfStructureInMoleculeDataModelList()}.
     */
    public void resetCachedIndexOfStructureInMoleculeDataModelList() {
        this.overviewViewController.resetCachedIndexOfStructureInMoleculeDataModelList();
    }
    /**
     * Persists settings of the view tools in preference container files in a subfolder of the settings directory. The settings of the
     * view tools are translated to matching preference objects. If a single setting or several cannot be persisted, it
     * is only logged in the log file. But if persisting a whole view tool fails, a warning is given to the user. The
     * settings are saved to files denoted with the simple class name of the respective IViewToolController-implementing class.
     */
    public void persistViewToolsSettings() {
        String tmpDirectoryPath = FileUtil.getSettingsDirPath()
                + ViewToolsManager.VIEW_TOOLS_SETTINGS_SUBFOLDER_NAME + File.separator;
        File tmpDirectory = new File(tmpDirectoryPath);
        if (!tmpDirectory.exists()) {
            tmpDirectory.mkdirs();
        } else {
            FileUtil.deleteAllFilesInDirectory(tmpDirectoryPath);
        }
        if (!tmpDirectory.canWrite()) {
            GuiUtil.guiMessageAlert(Alert.AlertType.ERROR, Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("ViewToolsManager.Error.settingsPersistence"));
            return;
        }
        for (IViewToolController tmpViewTool : this.viewToolsArray) {
            if (Objects.isNull(tmpViewTool)) {
                continue;
            }
            List<Property<?>> tmpSettings = tmpViewTool.settingsProperties();
            if (Objects.isNull(tmpSettings)) {
                continue;
            }
            String tmpFilePath = tmpDirectoryPath
                    + tmpViewTool.getClass().getSimpleName()
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION;
            try {
                PreferenceContainer tmpPrefContainer = PreferenceUtil.translateJavaFxPropertiesToPreferences(tmpSettings, tmpFilePath);
                tmpPrefContainer.writeRepresentation();
            } catch (NullPointerException | IllegalArgumentException | IOException | SecurityException anException) {
                ViewToolsManager.LOGGER.log(Level.WARNING,
                        String.format("View tools settings persistence went wrong, exception: %s", anException.toString()),
                        anException);
                GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                        Message.get("Error.ExceptionAlert.Header"),
                        Message.get("ViewToolsManager.Error.settingsPersistence"),
                        anException);
                //continue;
            }
        }
    }
    /**
     * Reloads settings of the available view tools. If something goes wrong, it is logged.
     */
    public void reloadViewToolsSettings() {
        String tmpDirectoryPath = FileUtil.getSettingsDirPath()
                + ViewToolsManager.VIEW_TOOLS_SETTINGS_SUBFOLDER_NAME + File.separator;
        for (IViewToolController tmpViewTool : this.viewToolsArray) {
            String tmpClassName = tmpViewTool.getClass().getSimpleName();
            File tmpViewToolsSettingsFile = new File(tmpDirectoryPath
                    + tmpClassName
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            if (tmpViewToolsSettingsFile.exists() && tmpViewToolsSettingsFile.isFile() && tmpViewToolsSettingsFile.canRead()) {
                PreferenceContainer tmpContainer;
                try {
                    tmpContainer = new PreferenceContainer(tmpViewToolsSettingsFile);
                } catch (IllegalArgumentException | IOException anException) {
                    ViewToolsManager.LOGGER.log(Level.WARNING,
                            String.format("Unable to reload settings of view tool %s: %s", tmpClassName, anException.toString()),
                            anException);
                    continue;
                }
                this.updatePropertiesFromPreferences(tmpViewTool.settingsProperties(), tmpContainer);
            } else {
                //settings will remain in default
                ViewToolsManager.LOGGER.log(Level.WARNING, "No persisted settings for {0} available.", tmpClassName);
            }
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Sets the values of the given properties according to the preferences in the given container with the same name.
     * If no matching preference for a given property is found, the value will remain in its default setting.
     */
    private void updatePropertiesFromPreferences(List<Property<?>> aPropertiesList, PreferenceContainer aPreferenceContainer) {
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
                        ViewToolsManager.LOGGER.log(Level.WARNING, "Setting {0} is of unknown type.", tmpPropertyName);
                    }
                } catch (ClassCastException | IllegalArgumentException anException) {
                    //setting will remain in default
                    ViewToolsManager.LOGGER.log(Level.WARNING, anException.toString(), anException);
                }
            } else {
                //setting will remain in default
                ViewToolsManager.LOGGER.log(Level.WARNING, "No persisted settings for {0} available.", tmpPropertyName);
            }
        }
    }
    /**
     * Checks the available view tools and their settings for restrictions imposed by persistence. Throws an IOException if
     * anything does not meet the requirements.
     */
    private void checkViewTools() throws IOException {
        HashSet<String> tmpViewToolNames = new HashSet<>((int)(this.viewToolsArray.length * 1.5), 0.75f);
        for (IViewToolController tmpViewTool : this.viewToolsArray) {
            //view tool name should be singleton and must be persistable
            String tmpViewToolName = tmpViewTool.getViewToolNameForDisplay();
            if (!PreferenceUtil.isValidName(tmpViewToolName) || !SingleTermPreference.isValidContent(tmpViewToolName)) {
                throw new IOException(String.format("View tool name %s is invalid.", tmpViewToolName));
            }
            if (tmpViewToolNames.contains(tmpViewToolName)) {
                throw new IOException(String.format("View tool name %s is used multiple times.", tmpViewToolName));
            } else {
                tmpViewToolNames.add(tmpViewToolName);
            }
            //setting names must be singletons within the respective class
            //setting names and values must adhere to the preference input restrictions
            //setting values are only tested for their current state, not the entire possible input space! It is tested again at persistence
            List<Property<?>> tmpSettingsList = tmpViewTool.settingsProperties();
            HashSet<String> tmpSettingNames = new HashSet<>((int) (tmpSettingsList.size() * 1.5), 0.75f);
            for (Property tmpSetting : tmpSettingsList) {
                if (!PreferenceUtil.isValidName(tmpSetting.getName())) {
                    throw new IOException(String.format("Setting %s has an invalid name.", tmpSetting.getName()));
                }
                if (tmpSettingNames.contains(tmpSetting.getName())) {
                    throw new IOException(String.format("Setting name %s is used multiple times.", tmpSetting.getName()));
                } else {
                    tmpSettingNames.add(tmpSetting.getName());
                }
                if (tmpSetting instanceof SimpleBooleanProperty) {
                    //nothing to do here, booleans cannot have invalid values
                } else if (tmpSetting instanceof SimpleIntegerProperty) {
                    if (!SingleIntegerPreference.isValidContent(Integer.toString(((SimpleIntegerProperty) tmpSetting).get()))) {
                        throw new IOException(String.format("Setting value %d of setting name %s is invalid.", ((SimpleIntegerProperty) tmpSetting).get(), tmpSetting.getName()));
                    }
                } else if (tmpSetting instanceof SimpleDoubleProperty) {
                    if (!SingleNumberPreference.isValidContent(((SimpleDoubleProperty) tmpSetting).get())) {
                        throw new IOException(String.format("Setting value %d of setting name %s is invalid.", ((SimpleDoubleProperty) tmpSetting).get(), tmpSetting.getName()));
                    }
                } else if (tmpSetting instanceof SimpleEnumConstantNameProperty || tmpSetting instanceof SimpleStringProperty) {
                    if (!SingleTermPreference.isValidContent(((SimpleStringProperty) tmpSetting).get())) {
                        throw new IOException(String.format("Setting value %s of setting name %s is invalid.", ((SimpleStringProperty) tmpSetting).get(), tmpSetting.getName()));
                    }
                } else {
                    throw new IOException(String.format("Setting %s is of an invalid type.", tmpSetting.getName()));
                }
            }
        }
    }
    //</editor-fold>
}
