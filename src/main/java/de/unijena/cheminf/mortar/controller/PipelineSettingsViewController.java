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
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.PipelineSettingsView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.fragmentation.FragmentationService;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class for the PipelineSettingsView.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class PipelineSettingsViewController {
    //<editor-fold desc="private final class variables" defaultstate="collapsed">
    private final StringProperty pipelineName;
    /**
     * Stage of the MainView.
     */
    private final Stage mainStage;
    /**
     * Configuration class to read resource file paths from.
     */
    private final IConfiguration configuration;
    //</editor-fold>
    //
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * PipelineSettingsView.
     */
    private PipelineSettingsView pipelineSettingsView;
    /**
     * Stage of the PipelineSettingsView.
     */
    private Stage pipelineSettingsViewStage;
    /**
     * Counts the amount of fragmentation algorithms in the pipeline.
     */
    private int algorithmCounter;
    /**
     * Service for fragmentation, controls the process of a fragmentation.
     */
    private final FragmentationService fragmentationService;
    /**
     * Array of the available fragmentation algorithms.
     */
    private final IMoleculeFragmenter[] fragmenters;
    /**
     * Lists of fragmentation algorithms for the pipeline.
     */
    private List<IMoleculeFragmenter> fragmenterList;
    /**
     * Boolean to mark if pipeline fragmentation was started from the dialog.
     */
    private boolean isFragmentationStarted;
    /**
     * Boolean value to enable fragment button if molecules are loaded.
     */
    private final boolean isMoleculeDataLoaded;
    /**
     * Boolean value whether a fragmentation is running.
     */
    private final boolean isFragmentationRunning;
    //</editor-fold>
    //
    //<editor-fold desc="private static final class variables" defaultstate="collapsed">
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(PipelineSettingsViewController.class.getName());
    //</editor-fold>
    //
    /**
     * Constructor.
     *
     * @param aMainStage Stage of the MainView
     * @param aFragmentationService FragmentationService
     * @param isMoleculeDataLoaded boolean whether molecule data is loaded
     * @param isFragmentationRunning boolean whether fragmentation is running
     * @param aConfiguration configuration instance to read resource file paths from
     */
    public PipelineSettingsViewController(Stage aMainStage,
                                          FragmentationService aFragmentationService,
                                          boolean isMoleculeDataLoaded,
                                          boolean isFragmentationRunning,
                                          IConfiguration aConfiguration) {
        this.mainStage = aMainStage;
        this.algorithmCounter = 0;
        this.fragmentationService = aFragmentationService;
        this.fragmenters = this.fragmentationService.getFragmenters();
        this.pipelineName = new SimpleStringProperty();
        this.fragmenterList = new LinkedList<>();
        this.cancelChangesInFragmenterList();
        this.isFragmentationStarted = false;
        this.isMoleculeDataLoaded = isMoleculeDataLoaded;
        this.isFragmentationRunning = isFragmentationRunning;
        this.configuration = aConfiguration;
        this.showPipelineSettingsView();
    }
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Initialises stage and view and opens view in the initialised stage.
     */
    private void showPipelineSettingsView() {
        if (this.pipelineSettingsView == null) {
            this.pipelineSettingsView = new PipelineSettingsView();
        }
        this.pipelineSettingsViewStage = new Stage();
        Scene tmpScene = new Scene(this.pipelineSettingsView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.pipelineSettingsViewStage.setScene(tmpScene);
        this.pipelineSettingsViewStage.initModality(Modality.WINDOW_MODAL);
        this.pipelineSettingsViewStage.initOwner(this.mainStage);
        this.pipelineSettingsViewStage.setTitle(Message.get("PipelineSettingsView.title.text"));
        this.pipelineSettingsViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.pipelineSettingsViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        String tmpIconURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.imagesFolder")
                        + this.configuration.getProperty("mortar.logo.icon.name")).toExternalForm();
        this.pipelineSettingsViewStage.getIcons().add(new Image(tmpIconURL));
        Platform.runLater(()->{
            this.pipelineSettingsView.addGrid(this.pipelineSettingsViewStage);
            this.addListenerAndBindings();
            for(IMoleculeFragmenter tmpFragmenter : this.fragmenterList){
                this.addNewChoiceRow(tmpFragmenter.getFragmentationAlgorithmName());
            }
            this.setPipelineName(this.fragmentationService.getPipeliningFragmentationName());
            this.pipelineSettingsView.getFragmentButton().setDisable(!this.isMoleculeDataLoaded || this.isFragmentationRunning);
        });
        this.pipelineSettingsViewStage.showAndWait();
    }
    //
    /**
     * Add listeners and bindings.
     */
    private void addListenerAndBindings() {
        //text field binding
        this.pipelineSettingsView.getTextField().textProperty().bindBidirectional(this.pipelineName);
        //stage close request
        this.pipelineSettingsViewStage.setOnCloseRequest(event -> {
            this.cancelChangesInFragmenterList();
            this.pipelineSettingsViewStage.close();
        });
        //fragment button
        this.pipelineSettingsView.getFragmentButton().setOnAction(event -> {
            this.isFragmentationStarted = true;
            this.fragmentationService.setPipeliningFragmentationName(this.pipelineName.get());
            this.fragmentationService.setPipelineFragmenter(this.fragmenterList.toArray(new IMoleculeFragmenter[0]));
            this.pipelineSettingsViewStage.close();
        });
        //cancel button
        this.pipelineSettingsView.getCancelButton().setOnAction(event -> {
            this.cancelChangesInFragmenterList();
            this.pipelineSettingsViewStage.close();
        });
        //apply button
        this.pipelineSettingsView.getApplyButton().setOnAction(event -> {
            this.fragmentationService.setPipeliningFragmentationName(this.pipelineName.get());
            this.fragmentationService.setPipelineFragmenter(this.fragmenterList.toArray(new IMoleculeFragmenter[0]));
            this.pipelineSettingsViewStage.close();
        });
        //default button
        this.pipelineSettingsView.getDefaultButton().setOnAction(event -> this.reset());
    }
    //
    /**
     * Adds a new row to the pipeline settings view, which allows to add a new fragmentation algorithms. ComboBox is
     * initially set to fragmentation algorithm corresponding to the given name.
     *
     * @param aFragmenterName name of Fragmenter to initially set ComboBox
     */
    private void addNewChoiceRow(String aFragmenterName) {
        ComboBox<String> tmpComboBox = new ComboBox<>();
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            tmpComboBox.getItems().add(tmpFragmenter.getFragmentationAlgorithmName());
        }
        tmpComboBox.setPromptText(Message.get("PipelineSettingsView.comboBox.promptText"));
        if (aFragmenterName != null) {
            tmpComboBox.getSelectionModel().select(aFragmenterName);
        }
        tmpComboBox.setOnAction(anActionEvent -> {
            Object tmpSelectedFragmenterString = tmpComboBox.getSelectionModel().getSelectedItem();
            int tmpIndex = GridPane.getRowIndex(tmpComboBox) - 1;
            for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
                if (tmpSelectedFragmenterString.equals(tmpFragmenter.getFragmentationAlgorithmName())) {
                    // will not work cause size of list is set - why not, it is in case a fragmenter that has already been set is changed
                    if (this.fragmenterList.size() > tmpIndex) {
                        this.fragmenterList.set(tmpIndex, Arrays.stream(this.fragmenters).filter(x -> x.getFragmentationAlgorithmName().equals(tmpSelectedFragmenterString)).findFirst().orElse(null));
                    } else {
                        this.fragmenterList.add(Arrays.stream(this.fragmenters).filter(x -> x.getFragmentationAlgorithmName().equals(tmpSelectedFragmenterString)).findFirst().orElse(null));
                    }
                    break;
                }
            }
            if (this.fragmenterList.contains(null)) {
                PipelineSettingsViewController.LOGGER.log(Level.SEVERE, "Error in pipeline fragmenter list, it contains null");
                throw new IllegalArgumentException("fragmenter list for pipeline must not contain null");
            }
        });
        Button tmpFragmenterSettingsButton = new Button();
        tmpFragmenterSettingsButton.setTooltip(GuiUtil.createTooltip(Message.get("PipelineSettingsView.settingButton.toolTip")));
        String tmpIconURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.imagesFolder")
                        + this.configuration.getProperty("mortar.icon.gear.name")).toExternalForm();
        tmpFragmenterSettingsButton.setGraphic(new ImageView(new Image(tmpIconURL)));
        tmpFragmenterSettingsButton.setMinHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpFragmenterSettingsButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpFragmenterSettingsButton.setMaxHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        BooleanBinding tmpBooleanBinding = Bindings.isNull(tmpComboBox.getSelectionModel().selectedItemProperty());
        tmpFragmenterSettingsButton.disableProperty().bind(tmpBooleanBinding);
        tmpFragmenterSettingsButton.setOnAction(anActionEvent -> {
            int tmpFragmenterListIndex = GridPane.getRowIndex(tmpFragmenterSettingsButton) - 1;
            IMoleculeFragmenter[] tmpArray = new IMoleculeFragmenter[1];
            tmpArray[0] = this.fragmenterList.get(tmpFragmenterListIndex);
            FragmentationSettingsViewController tmpFragmentationSettingsViewController
                    = new FragmentationSettingsViewController(this.pipelineSettingsViewStage, tmpArray, this.fragmenterList.get(tmpFragmenterListIndex).getFragmentationAlgorithmName(), this.configuration);
        });
        Label tmpLabel = new Label(String.valueOf(++this.algorithmCounter));
        //remove removeButton from upper Row
        if (this.algorithmCounter > 1) {
            this.pipelineSettingsView.getGridPane().getChildren().removeIf(node -> node instanceof Button button && (GridPane.getRowIndex(node) == this.algorithmCounter - 1) && (button).getText().equals("-") );
        }
        //remove addButton
        this.pipelineSettingsView.getGridPane().getChildren().removeIf(node -> GridPane.getRowIndex(node) == this.algorithmCounter);
        //add new content to row
        this.pipelineSettingsView.addAlgorithmChoiceRow(tmpLabel, tmpComboBox, tmpFragmenterSettingsButton, this.algorithmCounter);
        //add new remove button to row
        if (this.algorithmCounter > 1) {
            this.addRemoveRowButton(this.algorithmCounter);
        }
        //add new add button to next row
        this.addAddRowButton(this.algorithmCounter+1); //+1 cause of next row
    }
    //
    /**
     * Adds button to row of given index which adds a new row.
     *
     * @param aRowNumber int to specify row position
     */
    private void addAddRowButton(int aRowNumber) {
        Button tmpAddButton = new Button();
        tmpAddButton.setTooltip(GuiUtil.createTooltip(Message.get("PipelineSettingsView.addNewRowButton.toolTip")));
        tmpAddButton.setText("+");
        tmpAddButton.setMinHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpAddButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpAddButton.setMaxHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpAddButton.setMinWidth(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpAddButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpAddButton.setMaxWidth(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpAddButton.setOnAction(anActionEvent -> this.addNewChoiceRow(null));
        this.pipelineSettingsView.addAddRowButton(tmpAddButton, aRowNumber);
    }
    //
    /**
     * Adds button to row of given index which removes its own row.
     *
     * @param aRowNumber specifies which row should be removed
     */
    private void addRemoveRowButton(int aRowNumber) {
        Button tmpRemoveButton = new Button();
        tmpRemoveButton.setTooltip(GuiUtil.createTooltip(Message.get("PipelineSettingsView.removeRowButton.toolTip")));
        tmpRemoveButton.setText("-");
        tmpRemoveButton.setMinHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpRemoveButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpRemoveButton.setMaxHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpRemoveButton.setMinWidth(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpRemoveButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpRemoveButton.setMaxWidth(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpRemoveButton.setOnAction(anActionEvent -> {
            int tmpRowIndex = GridPane.getRowIndex(tmpRemoveButton);
            //remove addButton
            this.pipelineSettingsView.getGridPane().getChildren().removeIf(node -> GridPane.getRowIndex(node) == tmpRowIndex + 1);
            //remove complete row content
            this.pipelineSettingsView.getGridPane().getChildren().removeIf(node -> GridPane.getRowIndex(node) == tmpRowIndex);
            //add addButton
            this.addAddRowButton(tmpRowIndex);
            //
            this.algorithmCounter--;
            this.fragmenterList.removeLast();
            if(this.algorithmCounter > 1)
                this.addRemoveRowButton(this.algorithmCounter);
        });
        this.pipelineSettingsView.addRemoveRowButton(tmpRemoveButton, aRowNumber);
    }
    //
    /**
     * Cancels changes that are made in the fragmenter list.
     */
    private void cancelChangesInFragmenterList() {
        if (this.fragmentationService.getPipelineFragmenter() == null || this.fragmentationService.getPipelineFragmenter().length < 1) {
            try {
                this.fragmenterList.add(this.fragmentationService.getSelectedFragmenter().copy());
            } catch (Exception anException) {
                PipelineSettingsViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            }
        } else {
            for (IMoleculeFragmenter tmpFragmenter : this.fragmentationService.getPipelineFragmenter()) {
                this.fragmenterList.add(tmpFragmenter.copy());
            }
        }
    }
    //
    /**
     * Resets the complete pipeline and the view. Selected fragmenter for single fragmentation will be set as default value.
     */
    private void reset() {
        this.setPipelineName("");
        for (int i = 0; i < this.pipelineSettingsView.getGridPane().getChildren().size(); i++) {
            //remove addButton
            this.pipelineSettingsView.getGridPane().getChildren().removeIf(node -> GridPane.getRowIndex(node) == 2);
            //remove complete row content
            this.pipelineSettingsView.getGridPane().getChildren().removeIf(node -> GridPane.getRowIndex(node) > 1);
            //add addButton
            this.addAddRowButton(2);
        }
        this.fragmenterList = new LinkedList<>();
        this.algorithmCounter = 1;
        ComboBox<String> tmpBox = (ComboBox<String>) this.pipelineSettingsView.getGridPane().getChildren().stream().filter(node -> GridPane.getRowIndex(node) == 1 && (node instanceof ComboBox)).findFirst().orElse(null);
        if (tmpBox != null) {
            tmpBox.getSelectionModel().select(this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmName());
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns StringProperty for the pipeline name as configured in the dialog.
     *
     * @return StringProperty
     */
    public StringProperty pipelineNameProperty() {
        return this.pipelineName;
    }
    //
    /**
     * Returns pipeline name.
     *
     * @return String pipeline name
     */
    public String getPipelineName() {
        return this.pipelineName.get();
    }
    //
    /**
     * Sets pipeline name.
     *
     * @param aName String
     */
    public void setPipelineName(String aName) {
        this.pipelineName.set(aName);
    }
    //
    /**
     * Return boolean value whether fragmentation is started inside PipelineSettingsView via FragmentButton.
     *
     * @return true if fragmentation is started
     */
    public boolean isFragmentationStarted() {
        return isFragmentationStarted;
    }
    //</editor-fold>
}
