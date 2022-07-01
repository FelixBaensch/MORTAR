/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2022  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

/**
 * SettingsViewController
 * controls {@link SettingsView} for {@link SettingsContainer}
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class SettingsViewController {

    //<editor-fold desc="private and private final class variables">
    /**
     * SettingsContainer
     */
    private final SettingsContainer settingsContainer;
    private final SettingsContainer recentSettingsContainer;
    /**
     * Main stage object of the application
     */
    private final Stage mainStage;
    /**
     * Stage for the SettingsView
     */
    private Stage settingsViewStage;
    /**
     * SettingsView
     */
    private SettingsView settingsView;
    /**
     * Map to hold the initial settings properties
     */
    private Map<String, Object> recentProperties;
    /**
     * Boolean value to check if the rowsPerPage property has changed
     */
    private boolean hasRowsPerPageChanged;
    /**
     * Boolean value to check if the keepAtomContainerInDataModel property has changed
     */
    private boolean hasKeepAtomContainerInDataModelChanged;
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param aStage Parent stage
     * @param aSettingsContainer SettingsContainer
     */
    public SettingsViewController(Stage aStage, SettingsContainer aSettingsContainer){
        this.mainStage = aStage;
        this.settingsContainer = aSettingsContainer;
        this.recentSettingsContainer = aSettingsContainer;
        this.recentProperties = new HashMap<>(this.settingsContainer.settingsProperties().size());
        this.showSettingsView();
    }
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Initialises and opens settingsView
     */
    private void showSettingsView(){
        if(this.settingsView == null)
            this.settingsView = new SettingsView();
        this.settingsViewStage = new Stage();
        Scene tmpScene = new Scene(this.settingsView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.settingsViewStage.setScene(tmpScene);
        this.settingsViewStage.initModality(Modality.WINDOW_MODAL);
        this.settingsViewStage.initOwner(this.mainStage);
        this.settingsViewStage.setTitle(Message.get("SettingsView.title.default.text"));
        this.settingsViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.settingsViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        Platform.runLater(()->{
            this.addListeners();
            this.settingsView.addTab(this.settingsViewStage, Message.get("GlobalSettingsView.title.text"),
                    this.settingsContainer.settingsProperties(), this.settingsContainer.getSettingNameToTooltipTextMap(),
                    this.recentProperties);
        });
        this.settingsViewStage.showAndWait();
    }
    //
    /**
     * Adds listeners and event handlers to the buttons of the settings view
     */
    private void addListeners(){
        //stage close request
        this.settingsViewStage.setOnCloseRequest(event -> {
            this.setRecentProperties();
            this.settingsViewStage.close();
        });
        //apply button
        this.settingsView.getApplyButton().setOnAction(event -> {
            this.hasRowsPerPageChanged = (int) this.settingsContainer.rowsPerPageSettingProperty().getValue()
                    != (int) this.recentProperties.get(this.settingsContainer.rowsPerPageSettingProperty().getName());
            this.hasKeepAtomContainerInDataModelChanged = this.settingsContainer.keepAtomContainerInDataModelSettingProperty().getValue()
                    != this.recentProperties.get(this.settingsContainer.keepAtomContainerInDataModelSettingProperty().getName());
            this.settingsViewStage.close();
        });
        //cancel button
        this.settingsView.getCancelButton().setOnAction(event ->{
            this.setRecentProperties();
            this.settingsViewStage.close();
        });
        //default button
        this.settingsView.getDefaultButton().setOnAction(event -> {
            this.settingsContainer.restoreDefaultSettings();
        });
    }
    //
    /**
     * Sets the properties to the values of the 'recentPropertiesMap'
     */
    private void setRecentProperties(){
        Platform.runLater(()->{
            for (Property tmpProperty : this.settingsContainer.settingsProperties()) {
                if (this.recentProperties.containsKey(tmpProperty.getName())){
                    tmpProperty.setValue(this.recentProperties.get(tmpProperty.getName()));
                }
            }
        });
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns boolean value whether if rowsPerPage property has changed or not
     * @return hasRowsPerPageChanged
     */
    public boolean hasRowsPerPageChanged() {
        return this.hasRowsPerPageChanged;
    }
    //
    /**
     * Returns boolean value whether if keepAtomContainerInDataModel property has changed or not.
     * @return hasKeepAtomContainerInDataModelChanged
     */
    public boolean hasKeepAtomContainerInDataModelChanged() {
        return this.hasKeepAtomContainerInDataModelChanged;
    }
    //</editor-fold>
}
