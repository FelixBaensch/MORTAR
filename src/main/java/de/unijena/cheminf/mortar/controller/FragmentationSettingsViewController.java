/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.controller;

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

import java.io.InputStream;
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
     */
    public FragmentationSettingsViewController(Stage aStage, IMoleculeFragmenter[] anArrayOfFragmenters, String aSelectedFragmenterAlgorithmName){
        this.mainStage = aStage;
        this.recentProperties = new HashMap<>(CollectionUtil.calculateInitialHashMapCapacity(anArrayOfFragmenters.length));
        this.fragmenters = anArrayOfFragmenters;
        this.selectedFragmenterName = aSelectedFragmenterAlgorithmName;
        this.openFragmentationSettingsView();
    }
    //
    /**
     * Initialises and opens a settings view for fragmentationSettings
     */
    private void openFragmentationSettingsView(){
        if(this.settingsView == null)
            this.settingsView = new SettingsView();
        this.fragmentationSettingsViewStage = new Stage();
        Scene tmpScene = new Scene(this.settingsView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.fragmentationSettingsViewStage.setScene(tmpScene);
        this.fragmentationSettingsViewStage.initModality(Modality.WINDOW_MODAL);
        this.fragmentationSettingsViewStage.initOwner(this.mainStage);
        this.fragmentationSettingsViewStage.show();
        this.fragmentationSettingsViewStage.setTitle(Message.get("FragmentationSettingsView.title.text"));
        this.fragmentationSettingsViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.fragmentationSettingsViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        InputStream tmpImageInputStream = FragmentationSettingsViewController.class.getResourceAsStream("/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png");
        this.fragmentationSettingsViewStage.getIcons().add(new Image(tmpImageInputStream));
        //
        this.addListener();
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            HashMap<String, Object> tmpRecentProperties = new HashMap<>(CollectionUtil.calculateInitialHashMapCapacity(tmpFragmenter.settingsProperties().size()));
            this.recentProperties.put(tmpFragmenter.getFragmentationAlgorithmName(), tmpRecentProperties);
            Tab tmpTab = this.settingsView.addTab(this.fragmentationSettingsViewStage,
                    tmpFragmenter.getFragmentationAlgorithmName(), tmpFragmenter.settingsProperties(),
                    tmpFragmenter.getSettingNameToTooltipTextMap(), tmpRecentProperties);
            if(tmpFragmenter.getFragmentationAlgorithmName().equals(this.selectedFragmenterName)){
                this.settingsView.getSelectionModel().select(tmpTab);
            }
        }
    }
    //
    /**
     * Adds listeners
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
    private void setRecentProperties(IMoleculeFragmenter aFragmenter, Map aRecentPropertiesMap){
        for (Property tmpProperty : aFragmenter.settingsProperties()) {
            if(aRecentPropertiesMap.containsKey(tmpProperty.getName())){
                tmpProperty.setValue(aRecentPropertiesMap.get(tmpProperty.getName()));
            }
        }
    }
}
