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
import java.util.logging.Logger;

/**
 * SettingsViewController
 * controls {@link SettingsView} for fingerprinter settings
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public class FingerprinterSettingsViewController {
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
    private Stage fingerprintsSettingsViewStage;
    /**
     * Maps of maps to hold initial settings properties for each fingerprinter
     */
    private Map<String, Map<String, Object>> recentProperties;
    /**
     * Array of {@link IMortarFingerprinter} objects
     */
    private IMortarFingerprinter[] fingerprinter;
    /**
     * Name of the selected fingerprinter
     */
    private String selectedFingerprinterName;
    /**
     * Value of the generated fingerprint value
     */
    private int fingerprintDimensionalityValue;
    //</editor-fold>
    //
    //<editor-fold desc="private static final class variables">
    private static final Logger LOGGER = Logger.getLogger(FingerprinterSettingsViewController.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="constructor">
    /**
     * Constructor
     *
     * @param aStage Stage
     * @param anArrayOfFingerprinter IMortarFingerprinter[]
     * @param aSelectedFingerprinterName name of the fingerprinter e.g. Fragment Fingerprinter
     * @param aNumber Fingerprint dimensionality value
     */
    public FingerprinterSettingsViewController(Stage aStage, IMortarFingerprinter[] anArrayOfFingerprinter, String aSelectedFingerprinterName, int aNumber) {
        this.mainStage = aStage;
        this.fingerprintDimensionalityValue = aNumber;
        this.recentProperties = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(anArrayOfFingerprinter.length));
        this.fingerprinter = anArrayOfFingerprinter;
        this.selectedFingerprinterName = aSelectedFingerprinterName;
        if(aNumber != 0) {
            this.openFingerprinterSettingsView();
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods">
    //
    /**
     * Initialises and opens a settings view for fingerprinter settings
     */
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
        this.fingerprintsSettingsViewStage.setTitle(Message.get("FingerprinterSettingsView.title.text"));
        this.fingerprintsSettingsViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.fingerprintsSettingsViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        InputStream tmpImageInputStream = FingerprinterSettingsViewController.class.getResourceAsStream("/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png");
        this.fingerprintsSettingsViewStage.getIcons().add(new Image(tmpImageInputStream));
        this.addListener();
        for(IMortarFingerprinter tmpFingerprinter : this.fingerprinter) {
            HashMap<String, Object> tmpRecentProperties = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(tmpFingerprinter.settingsProperties().size()));
            this.recentProperties.put(tmpFingerprinter.getFingerprinterName(), tmpRecentProperties);
            Tab tmpTab = this.settingsView.addTab(this.fingerprintsSettingsViewStage, tmpFingerprinter.getFingerprinterName(), tmpFingerprinter.settingsProperties(),
                    tmpFingerprinter.getSettingNameToTooltipTextMap(), tmpRecentProperties);
            if(tmpFingerprinter.getFingerprinterName().equals(this.selectedFingerprinterName)) {
                this.settingsView.getSelectionModel().select(tmpTab);
            }
        }
    }
    //
    /**
     * Adds listeners
     */
    private void addListener() {
        // fingerprinterSettingsViewStage close request
        this.fingerprintsSettingsViewStage.setOnCloseRequest(event -> {
            for(int i = 0; i < this.fingerprinter.length; i++) {
                if(this.fingerprinter[i].getFingerprinterName().equals(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId())) {
                    this.setRecentProperties(this.fingerprinter[i], this.recentProperties.get(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId()));
                }
            }
            this.fingerprintsSettingsViewStage.close();
        });
        // applyButton
        this.settingsView.getApplyButton().setOnAction(event -> {
            this.fingerprintsSettingsViewStage.close();
        });
        // cancelButton
        this.settingsView.getCancelButton().setOnAction(event -> {
            for(int i = 0; i < this.fingerprinter.length; i++) {
                this.setRecentProperties(this.fingerprinter[i], this.recentProperties.get(this.fingerprinter[i].getFingerprinterName()));
            }
            this.fingerprintsSettingsViewStage.close();
        });
        // defaultButton
        this.settingsView.getDefaultButton().setOnAction(event -> {
            for(int i = 0; i < this.fingerprinter.length; i++) {
                if(this.fingerprinter[i].getFingerprinterName().equals(this.settingsView.getTabPane().getSelectionModel().getSelectedItem().getId()));
                this.fingerprinter[i].restoreDefaultSettings(this.fingerprintDimensionalityValue);
            }
        });
    }
    //
    /**
     * Sets the properties of the given fingerprinter to the values of the 'recentPropertiesMap'
     *
     * @param aFingerprinter IMortarFingerprinter
     * @param aRecentPropertiesMap properties map
     */
    private void setRecentProperties(IMortarFingerprinter aFingerprinter, Map aRecentPropertiesMap) {
        for(Property tmpProperty : aFingerprinter.settingsProperties()) {
            if(aRecentPropertiesMap.containsKey(tmpProperty.getName())) {
                tmpProperty.setValue(aRecentPropertiesMap.get(tmpProperty.getName()));
            }
        }
    }
    //</editor-fold>
    //
}
