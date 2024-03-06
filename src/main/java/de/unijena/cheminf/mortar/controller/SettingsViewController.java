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
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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
     * Configuration class to read resource file paths from.
     */
    private final IConfiguration configuration;
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
     * @param aConfiguration configuration instance to read resource file paths from
     */
    public SettingsViewController(Stage aStage, SettingsContainer aSettingsContainer, IConfiguration aConfiguration) {
        this.mainStage = aStage;
        this.settingsContainer = aSettingsContainer;
        this.recentSettingsContainer = aSettingsContainer;
        this.configuration = aConfiguration;
        this.recentProperties = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(this.settingsContainer.settingsProperties().size()));
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
        String tmpIconURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.imagesFolder")
                        + this.configuration.getProperty("mortar.logo.icon.name")).toExternalForm();
        this.settingsViewStage.getIcons().add(new Image(tmpIconURL));
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
