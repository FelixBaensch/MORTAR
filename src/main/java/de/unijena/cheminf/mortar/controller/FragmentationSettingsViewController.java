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
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.views.SettingsView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;

import javafx.beans.property.Property;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * SettingsViewController
 * controls {@link SettingsView} for fragmentation settings
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class FragmentationSettingsViewController {
    //<editor-fold desc="private and private final class variables">
    /**
     * Main stage object of the application
     */
    private final Stage mainStage;
    /**
     * Stage for the SettingsView
     */
    private Stage fragmentationSettingsViewStage;
    /**
     * SettingsView
     */
    private SettingsView settingsView;
    /**
     * Map of maps to hold initial settings properties for each algorithm
     */
    private Map<String, Map<String, Object>> recentProperties;
    /**
     * Array of {@link IMoleculeFragmenter} objects
     */
    private IMoleculeFragmenter[] fragmenters;
    /**
     * Name of the selected fragmentation algorithm
     */
    private String selectedFragmenterName;
    /**
     * Configuration class to read resource file paths from.
     */
    private final IConfiguration configuration;
    /**
     * Logger
     */
    private static final Logger LOGGER = Logger.getLogger(FragmentationSettingsViewController.class.getName());
    //</editor-fold>
    /**
     * Constructor
     *
     * @param aStage Stage
     * @param anArrayOfFragmenters IMoleculeFragmenter[]
     * @param aSelectedFragmenterAlgorithmName String
     * @param aConfiguration configuration instance to read resource file paths from
     */
    public FragmentationSettingsViewController(Stage aStage, IMoleculeFragmenter[] anArrayOfFragmenters, String aSelectedFragmenterAlgorithmName, IConfiguration aConfiguration){
        this.mainStage = aStage;
        this.recentProperties = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(anArrayOfFragmenters.length));
        this.fragmenters = anArrayOfFragmenters;
        this.selectedFragmenterName = aSelectedFragmenterAlgorithmName;
        this.configuration = aConfiguration;
        this.openFragmentationSettingsView();
    }
    //
    /**
     * Initialises and opens a settings view for fragmentationSettings.
     */
    private void openFragmentationSettingsView() {
        if (this.settingsView == null) {
            this.settingsView = new SettingsView();
        }
        this.fragmentationSettingsViewStage = new Stage();
        Scene tmpScene = new Scene(this.settingsView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.fragmentationSettingsViewStage.setScene(tmpScene);
        this.fragmentationSettingsViewStage.initModality(Modality.WINDOW_MODAL);
        this.fragmentationSettingsViewStage.initOwner(this.mainStage);
        this.fragmentationSettingsViewStage.show();
        this.fragmentationSettingsViewStage.setTitle(Message.get("FragmentationSettingsView.title.text"));
        this.fragmentationSettingsViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.fragmentationSettingsViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        String tmpIconURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.imagesFolder")
                        + this.configuration.getProperty("mortar.logo.icon.name")).toExternalForm();
        this.fragmentationSettingsViewStage.getIcons().add(new Image(tmpIconURL));
        //
        this.addListener();
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            HashMap<String, Object> tmpRecentProperties = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(tmpFragmenter.settingsProperties().size()));
            this.recentProperties.put(tmpFragmenter.getFragmentationAlgorithmName(), tmpRecentProperties);
            Tab tmpTab = this.settingsView.addTab(
                    tmpFragmenter.getFragmentationAlgorithmName(), tmpFragmenter.settingsProperties(),
                    tmpFragmenter.getSettingNameToDisplayNameMap(),
                    tmpFragmenter.getSettingNameToTooltipTextMap(), tmpRecentProperties);
            if (tmpFragmenter.getFragmentationAlgorithmName().equals(this.selectedFragmenterName)) {
                this.settingsView.getSelectionModel().select(tmpTab);
            }
        }
    }
    //
    /**
     * Adds listeners.
     */
    private void addListener(){
        //fragmentationSettingsViewStage close request
        this.fragmentationSettingsViewStage.setOnCloseRequest(event -> {
            for(int i = 0; i < this.fragmenters.length; i++){
                if(this.fragmenters[i].getFragmentationAlgorithmName().equals(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId())){
                    this.setRecentProperties(this.fragmenters[i], this.recentProperties.get(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId()));
                }
            }
            this.fragmentationSettingsViewStage.close();
        });
        //applyButton
        this.settingsView.getApplyButton().setOnAction(event -> {
            this.fragmentationSettingsViewStage.close();
        });
        //cancelButton
        this.settingsView.getCancelButton().setOnAction(event -> {
            for(int i = 0; i < this.fragmenters.length; i++){
                this.setRecentProperties(this.fragmenters[i], this.recentProperties.get(this.fragmenters[i].getFragmentationAlgorithmName()));
            }
            this.fragmentationSettingsViewStage.close();
        });
        //defaultButton
        this.settingsView.getDefaultButton().setOnAction(event -> {
            for(int i = 0; i < this.fragmenters.length; i++){
                if(this.fragmenters[i].getFragmentationAlgorithmName().equals(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId())){
                    this.fragmenters[i].restoreDefaultSettings();
                }
            }
        });
    }

    /**
     * Sets the properties of the given fragmenter to the values of the 'recentPropertiesMap'
     *
     * @param aFragmenter IMoleculeFragmenter
     * @param aRecentPropertiesMap Map
     */
    private void setRecentProperties(IMoleculeFragmenter aFragmenter, Map<String, Object> aRecentPropertiesMap){
        for (Property tmpProperty : aFragmenter.settingsProperties()) {
            if(aRecentPropertiesMap.containsKey(tmpProperty.getName())){
                tmpProperty.setValue(aRecentPropertiesMap.get(tmpProperty.getName()));
            }
        }
    }
}
