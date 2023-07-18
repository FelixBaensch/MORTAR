package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.views.SettingsView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.Fingerprints.IMortarFingerprinter;
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

public class FingerprinterSettingsViewController {
    private final Stage mainStage;
    private SettingsView settingsView;
    private Stage fingerprintsSettingsViewStage;
    private Map<String, Map<String, Object>> recentProperties;
    private IMortarFingerprinter[] fingerprinterTyp;
    private String selectedFingerprinterTypName;
    private int dime;

    public FingerprinterSettingsViewController(Stage aStage, IMortarFingerprinter[] anArrayOfClusteringAlgorithms, String aSelectedClusteringTypName, int aNumber) {
        this.mainStage = aStage;
        this.dime = aNumber;
        this.recentProperties = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(anArrayOfClusteringAlgorithms.length));
        this.fingerprinterTyp = anArrayOfClusteringAlgorithms;
        this.selectedFingerprinterTypName = aSelectedClusteringTypName;
        this.openFingerprinterSettingsView();
    }
    private void openFingerprinterSettingsView() {
        if(this.settingsView == null) {
            this.settingsView = new SettingsView();
        }
        this.fingerprintsSettingsViewStage = new Stage();
        Scene tmpScene = new Scene(this.settingsView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.fingerprintsSettingsViewStage.setScene(tmpScene);
        this.fingerprintsSettingsViewStage.initModality(Modality.WINDOW_MODAL);
        this.fingerprintsSettingsViewStage.initOwner(this.mainStage);
        this.fingerprintsSettingsViewStage.show();
        this.fingerprintsSettingsViewStage.setTitle(Message.get("ClusteringSettingsView.title.text"));
        this.fingerprintsSettingsViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.fingerprintsSettingsViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        InputStream tmpImageInputStream = FingerprinterSettingsViewController.class.getResourceAsStream("/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png");
        this.fingerprintsSettingsViewStage.getIcons().add(new Image(tmpImageInputStream));
        this.addListener();
        for(IMortarFingerprinter tmpClusteringAlgorithms : this.fingerprinterTyp) {
            HashMap<String, Object> tmpRecentProperties = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(tmpClusteringAlgorithms.settingsProperties().size()));
            this.recentProperties.put(tmpClusteringAlgorithms.getFingerprinterName(), tmpRecentProperties);
            Tab tmpTab = this.settingsView.addTab(this.fingerprintsSettingsViewStage, tmpClusteringAlgorithms.getFingerprinterName(), tmpClusteringAlgorithms.settingsProperties(),
                    tmpClusteringAlgorithms.getSettingNameToTooltipTextMap(), tmpRecentProperties);
            if(tmpClusteringAlgorithms.getFingerprinterName().equals(this.selectedFingerprinterTypName)) { // TODO
                this.settingsView.getSelectionModel().select(tmpTab);
            }
        }
    }
    private void addListener() {
        this.fingerprintsSettingsViewStage.setOnCloseRequest(event -> {
            for(int i = 0; i < this.fingerprinterTyp.length; i++) {
                if(this.fingerprinterTyp[i].getFingerprinterName().equals(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId())) {
                    this.setRecentProperties(this.fingerprinterTyp[i],this.recentProperties.get(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId()));
                }
            }
            this.fingerprintsSettingsViewStage.close();
        });
        this.settingsView.getApplyButton().setOnAction(event -> {
            this.fingerprintsSettingsViewStage.close();
        });
        this.settingsView.getCancelButton().setOnAction(event -> {
            for(int i = 0; i < this.fingerprinterTyp.length; i++) {
                this.setRecentProperties(this.fingerprinterTyp[i], this.recentProperties.get(this.fingerprinterTyp[i].getFingerprinterName()));
            }
            this.fingerprintsSettingsViewStage.close();
        });
        this.settingsView.getDefaultButton().setOnAction(event -> {
            for(int i = 0; i < this.fingerprinterTyp.length; i++) {
                if(this.fingerprinterTyp[i].getFingerprinterName().equals(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId()));
                this.fingerprinterTyp[i].restoreDefaultSettings(this.dime);
            }
            System.out.println(this.dime+ "----------default dimensionality fingerprints");
        });
    }
    private void setRecentProperties(IMortarFingerprinter aFingerprinter, Map aRecentPropertiesMap) {
        for(Property tmpProperty : aFingerprinter.settingsProperties()) {
            if(aRecentPropertiesMap.containsKey(tmpProperty.getName())) {
                tmpProperty.setValue(aRecentPropertiesMap.get(tmpProperty.getName()));
            }
        }
    }
}
