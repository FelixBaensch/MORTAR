package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.views.SettingsView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.clustering.IFingerprintClustering;
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

public class ClusteringSettingsViewController {
    private final Stage mainStage;
    private SettingsView settingsView;
    private Stage clusteringSettingsViewStage;
    private Map<String, Map<String, Object>> recentProperties;
    private IFingerprintClustering[] clusteringTyp;
    private String selectedClusteringTypName;
    public ClusteringSettingsViewController(Stage aStage, IFingerprintClustering[] anArrayOfClusteringAlgorithms, String aSelectedClusteringTypName) {
        this.mainStage = aStage;
        this.recentProperties = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(anArrayOfClusteringAlgorithms.length));
        this.clusteringTyp = anArrayOfClusteringAlgorithms;
        this.selectedClusteringTypName = aSelectedClusteringTypName;
        this.openClusteringSettingsView();
    }
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
        for(IFingerprintClustering tmpClusteringAlgorithms : this.clusteringTyp) {
            HashMap<String, Object> tmpRecentProperties = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(tmpClusteringAlgorithms.settingsProperties().size()));
            this.recentProperties.put(tmpClusteringAlgorithms.getClusteringName(), tmpRecentProperties);
            Tab tmpTab = this.settingsView.addTab(this.clusteringSettingsViewStage, tmpClusteringAlgorithms.getClusteringName(), tmpClusteringAlgorithms.settingsProperties(),
                    tmpClusteringAlgorithms.getSettingNameToTooltipTextMap(), tmpRecentProperties);
            if(tmpClusteringAlgorithms.getClusteringName().equals(this.selectedClusteringTypName)) { // TODO
                this.settingsView.getSelectionModel().select(tmpTab);
            }
        }
    }
    private void addListener() {
        this.clusteringSettingsViewStage.setOnCloseRequest(event -> {
            for(int i = 0; i < this.clusteringTyp.length; i++) {
                if(this.clusteringTyp[i].getClusteringName().equals(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId())) {
                    this.setRecentProperties(this.clusteringTyp[i],this.recentProperties.get(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId()));
                }
            }
            this.clusteringSettingsViewStage.close();
        });
        this.settingsView.getApplyButton().setOnAction(event -> {
            this.clusteringSettingsViewStage.close();
        });
        this.settingsView.getCancelButton().setOnAction(event -> {
            System.out.println("cancel button");
            for(int i = 0; i < this.clusteringTyp.length; i++) {
                this.setRecentProperties(this.clusteringTyp[i], this.recentProperties.get(this.clusteringTyp[i].getClusteringName()));
            }
        });
        this.settingsView.getDefaultButton().setOnAction(event -> {
            for(int i = 0; i < this.clusteringTyp.length; i++) {
                if(this.clusteringTyp[i].getClusteringName().equals(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId()));
            }
        });
    }
    private void setRecentProperties(IFingerprintClustering aClusteringAlgorithm, Map aRecentPropertiesMap) {
        for(Property tmpProperty : aClusteringAlgorithm.settingsProperties()) {
            if(aRecentPropertiesMap.containsKey(tmpProperty.getName())) {
                tmpProperty.setValue(aRecentPropertiesMap.get(tmpProperty.getName()));
            }
        }
    }

}
