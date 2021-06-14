/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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
import javafx.beans.property.Property;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SettingsViewController {

    //<editor-fold desc="private and private final class variables">
    /**
     * SettingsContainer
     */
    private final SettingsContainer settingsContainer;
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
    //</editor-fold>

    public SettingsViewController(Stage aStage, SettingsContainer aSettingsContainer){
        this.mainStage = aStage;
        this.settingsContainer = aSettingsContainer;
        this.recentProperties = new HashMap<>(this.settingsContainer.settingsProperties().size());
        this.openSettingsView();
    }

    /**
     * Initialises and opens settingsView
     */
    private void openSettingsView(){
        if(this.settingsView == null)
            this.settingsView = new SettingsView();
        this.settingsViewStage = new Stage();
        Scene tmpScene = new Scene(this.settingsView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.settingsViewStage.setScene(tmpScene);
        this.settingsViewStage.initModality(Modality.WINDOW_MODAL);
        this.settingsViewStage.initOwner(this.mainStage);
        this.settingsViewStage.show();
        this.settingsViewStage.setTitle(Message.get("FragmentationSettingsView.title"));
        this.settingsViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.settingsViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        this.addListeners();
        this.settingsView.addTab(this.settingsViewStage, Message.get("GlobalSettingsView.title"), this.settingsContainer.settingsProperties(), this.recentProperties);
    }

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
        this.settingsContainer.rowsPerPageSettingProperty().addListener((observableValue, oldValue, newValue) -> {
            this.hasRowsPerPageChanged = Objects.equals(oldValue, newValue);
        });
    }

    /**
     * Sets the properties to the values of the 'recentPropertiesMap'
     */
    private void setRecentProperties(){
        for (Property tmpProperty : this.settingsContainer.settingsProperties()) {
            if (this.recentProperties.containsKey(tmpProperty.getName())){
                tmpProperty.setValue(this.recentProperties.get(tmpProperty.getName()));
            }
        }
    }

    /**
     * Returns boolean value whether if rowsPerPage property has changed or not
     * @return hasRowsPerPageChanged
     */
    public boolean hasRowsPerPageChanged() {
        return this.hasRowsPerPageChanged;
    }
}
