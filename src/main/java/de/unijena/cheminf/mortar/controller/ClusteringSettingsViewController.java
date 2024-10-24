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

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.views.SettingsView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.clustering.IMortarClustering;
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
 * controls {@link SettingsView} for clustering settings
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public class ClusteringSettingsViewController {
    //<editor-fold desc="private and private final class variables">
    /**
     * Main stage object of the application
     */
    private final Stage mainStage;
    /**
     * SettingsView
     */
    private SettingsView settingsView;
    /**
     * Stage for the SettingsView
     */
    private Stage clusteringSettingsViewStage;
    /**
     * Map of maps to hold initial settings properties for each clustering algorithm
     */
    private Map<String, Map<String, Object>> recentProperties;
    /**
     * Array of {@link IMortarClustering} objects
     */
    private IMortarClustering[] clusteringAlgorithms;
    /**
     * Name of the selected clustering algorithm
     */
    private String selectedClusteringAlgorithmName;
    //</editor-fold>
    //
    //<editor-fold desc="private static final class variables">
    /**
     * Logger of this class
     */
    private static final Logger LOGGER = Logger.getLogger(ClusteringSettingsViewController.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="constructor">
    /**
     * Constructor
     *
     * @param aStage Stage
     * @param anArrayOfClusteringAlgorithms IMortarClustering
     * @param aSelectedClusteringTypName name of the selected clustering algorithm e.g. ART 2-A Clustering
     */
    public ClusteringSettingsViewController(Stage aStage, IMortarClustering[] anArrayOfClusteringAlgorithms, String aSelectedClusteringTypName) {
        this.mainStage = aStage;
        this.recentProperties = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(anArrayOfClusteringAlgorithms.length));
        this.clusteringAlgorithms = anArrayOfClusteringAlgorithms;
        this.selectedClusteringAlgorithmName = aSelectedClusteringTypName;
        this.openClusteringSettingsView();
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods">
    private void openClusteringSettingsView() {
        if(this.settingsView == null) {
            this.settingsView = new SettingsView();
        }
        this.clusteringSettingsViewStage = new Stage();
        Scene tmpScene = new Scene(this.settingsView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.clusteringSettingsViewStage.setScene(tmpScene);
        this.clusteringSettingsViewStage.initModality(Modality.WINDOW_MODAL);
        this.clusteringSettingsViewStage.initOwner(this.mainStage);
        this.clusteringSettingsViewStage.show();
        this.clusteringSettingsViewStage.setTitle(Message.get("ClusteringSettingsView.title.text"));
        this.clusteringSettingsViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.clusteringSettingsViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        InputStream tmpImageInputStream = ClusteringSettingsViewController.class.getResourceAsStream("/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png");
        this.clusteringSettingsViewStage.getIcons().add(new Image(tmpImageInputStream));
        this.addListener();
        for (IMortarClustering tmpClusteringAlgorithm : this.clusteringAlgorithms) {
            HashMap<String, Object> tmpRecentProperties = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(tmpClusteringAlgorithm.settingsProperties().size()));
            this.recentProperties.put(tmpClusteringAlgorithm.getClusteringName(), tmpRecentProperties);
            Tab tmpTab = this.settingsView.addTab(tmpClusteringAlgorithm.getClusteringName(), tmpClusteringAlgorithm.settingsProperties(),
                    tmpClusteringAlgorithm.getSettingNameToDisplayNameMap(), tmpClusteringAlgorithm.getSettingNameToTooltipTextMap(), tmpRecentProperties);
            if(tmpClusteringAlgorithm.getClusteringName().equals(this.selectedClusteringAlgorithmName)) {
                this.settingsView.getSelectionModel().select(tmpTab);
            }
        }
    }
    //
    /**
     * Adds listeners
     */
    private void addListener() {
        // clusteringSettingsViewStage lose request
        this.clusteringSettingsViewStage.setOnCloseRequest(event -> {
            for(int i = 0; i < this.clusteringAlgorithms.length; i++) {
                if(this.clusteringAlgorithms[i].getClusteringName().equals(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId())) {
                    this.setRecentProperties(this.clusteringAlgorithms[i],this.recentProperties.get(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId()));
                }
            }
            this.clusteringSettingsViewStage.close();
        });
        // applyButton
        this.settingsView.getApplyButton().setOnAction(event -> {
            this.clusteringSettingsViewStage.close();
        });
        // cancelButton
        this.settingsView.getCancelButton().setOnAction(event -> {
            for(int i = 0; i < this.clusteringAlgorithms.length; i++) {
                this.setRecentProperties(this.clusteringAlgorithms[i], this.recentProperties.get(this.clusteringAlgorithms[i].getClusteringName()));
            }
           this.clusteringSettingsViewStage.close();
        });
        //defaultButton
        this.settingsView.getDefaultButton().setOnAction(event -> {
            for(int i = 0; i < this.clusteringAlgorithms.length; i++) {
                if(this.clusteringAlgorithms[i].getClusteringName().equals(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId())) {
                    this.clusteringAlgorithms[i].restoreDefaultSettings();
                }
            }
        });
    }
    //
    /**
     * Sets the properties of the given clustering alogrithm to the values of the 'recentPropertiesMap'
     *
     * @param aClusteringAlgorithm IMortarClustering
     * @param aRecentPropertiesMap properties map
     */
    private void setRecentProperties(IMortarClustering aClusteringAlgorithm, Map aRecentPropertiesMap) {
        for(Property tmpProperty : aClusteringAlgorithm.settingsProperties()) {
            if(aRecentPropertiesMap.containsKey(tmpProperty.getName())) {
                tmpProperty.setValue(aRecentPropertiesMap.get(tmpProperty.getName()));
            }
        }
    }
    //</editor-fold>
    //
}
