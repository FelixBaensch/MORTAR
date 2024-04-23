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
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter;
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
    private final StringProperty pipelineNameProperty;
    /**
     * Stage of the MainView.
     */
    private final Stage mainStage;
    /**
     * Configuration class to read resource file paths from.
     */
    private final IConfiguration configuration;
    /**
     * Service for fragmentation, controls the process of a fragmentation.
     */
    private final FragmentationService fragmentationService;
    /**
     * Boolean value to enable fragment button if molecules are loaded.
     */
    private final boolean isMoleculeDataLoaded;
    /**
     * Boolean value whether a fragmentation is running to disable fragment button if true.
     */
    private final boolean isFragmentationRunning;
    /**
     * Lists of fragmentation algorithms for the pipeline.
     */
    private final List<IMoleculeFragmenter> selectedPipelineFragmentersList;
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
     * Counts the fragmentation algorithms in the pipeline. Used for the row numbers in the GUI (starting at 1).
     * Cannot easily be replaced by a selectedPipelineFragmentersList.size() call because of the initial build up of
     * the dialog at initial display.
     */
    private int algorithmCounter;
    /**
     * Boolean to mark if pipeline fragmentation was started from the dialog.
     */
    private boolean isFragmentationStarted;
    //</editor-fold>
    //
    //<editor-fold desc="private static final class variables" defaultstate="collapsed">
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(PipelineSettingsViewController.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor. Opens the view after initialisations.
     *
     * @param aMainStage Stage of the MainView
     * @param aFragmentationService FragmentationService to get existing pipeline from and forward new pipeline to and also
     *                              to get the available fragmenters from
     * @param isMoleculeDataLoaded boolean whether molecule data is loaded and hence pipeline could be executed
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
        this.pipelineNameProperty = new SimpleStringProperty(this.fragmentationService.getPipeliningFragmentationName());
        this.selectedPipelineFragmentersList = new LinkedList<>();
        //copies selected pipeline fragmenters from fragmentation service to the internal fragmenter list of this class
        this.cancelChangesInFragmenterList();
        this.isFragmentationStarted = false;
        this.isMoleculeDataLoaded = isMoleculeDataLoaded;
        this.isFragmentationRunning = isFragmentationRunning;
        this.configuration = aConfiguration;
        //other variables are initialised here:
        this.showPipelineSettingsView();
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Initialises stage and view and opens view in the initialised stage. Ends with "show and wait".
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
        Platform.runLater(() -> {
            this.pipelineSettingsView.addGrid(this.pipelineSettingsViewStage);
            this.addListenerAndBindings();
            for (IMoleculeFragmenter tmpFragmenter : this.selectedPipelineFragmentersList) {
                //update only the GUI, not the internal pipeline fragmenter list, hence false as 2nd param
                this.addNewChoiceRow(tmpFragmenter.getFragmentationAlgorithmDisplayName(), false);
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
        this.pipelineSettingsView.getTextField().textProperty().bindBidirectional(this.pipelineNameProperty);
        //stage close request
        this.pipelineSettingsViewStage.setOnCloseRequest(event -> {
            //note: view and controller are right now discarded after close; so this is not really necessary...
            this.cancelChangesInFragmenterList();
            this.pipelineSettingsViewStage.close();
        });
        //fragment/run button
        this.pipelineSettingsView.getFragmentButton().setOnAction(event -> {
            //send all changes to fragmentation service but the calling class needs to actually start the fragmentation
            this.isFragmentationStarted = true;
            this.fragmentationService.setPipeliningFragmentationName(this.pipelineNameProperty.get());
            this.fragmentationService.setPipelineFragmenter(this.selectedPipelineFragmentersList.toArray(new IMoleculeFragmenter[0]));
            this.pipelineSettingsViewStage.close();
        });
        //cancel button
        this.pipelineSettingsView.getCancelButton().setOnAction(event -> {
            //note: view and controller are right now discarded after close; so this is not really necessary...
            this.cancelChangesInFragmenterList();
            this.pipelineSettingsViewStage.close();
        });
        //apply button
        this.pipelineSettingsView.getApplyButton().setOnAction(event -> {
            this.fragmentationService.setPipeliningFragmentationName(this.pipelineNameProperty.get());
            this.fragmentationService.setPipelineFragmenter(this.selectedPipelineFragmentersList.toArray(new IMoleculeFragmenter[0]));
            this.pipelineSettingsViewStage.close();
        });
        //default button
        this.pipelineSettingsView.getDefaultButton().setOnAction(event -> this.reset());
    }
    //
    /**
     * Adds a new row to the pipeline settings view, which allows to add a new fragmentation algorithm. ComboBox is
     * initially set to fragmentation algorithm corresponding to the given display name.
     *
     * @param aFragmenterDisplayName display name of Fragmenter to initially set ComboBox; if null, the combo box will
     *                               just display a prompt text
     * @param anUpdateFragmentersList whether the internal fragmenters list should be updated accordingly, e.g. false when
     *                                pipeline settings view is constructed for initial display and true when the user adds
     *                                a new row using the "+" button; only used when first param is not null
     */
    private void addNewChoiceRow(String aFragmenterDisplayName, boolean anUpdateFragmentersList) {
        ComboBox<String> tmpComboBox = this.newFragmenterComboBox();
        if (aFragmenterDisplayName == null) {
            tmpComboBox.setPromptText(Message.get("PipelineSettingsView.comboBox.promptText"));
        } else {
            //does not trigger the action defined above
            tmpComboBox.getSelectionModel().select(aFragmenterDisplayName);
            boolean tmpIsFragmenterFound = false;
            if (anUpdateFragmentersList) {
                for (IMoleculeFragmenter tmpFragmenter : this.fragmentationService.getFragmenters()) {
                    if (aFragmenterDisplayName.equals(tmpFragmenter.getFragmentationAlgorithmDisplayName())) {
                        this.selectedPipelineFragmentersList.add(tmpFragmenter.copy());
                        tmpIsFragmenterFound = true;
                        break;
                    }
                }
                if (!tmpIsFragmenterFound) {
                    PipelineSettingsViewController.LOGGER.log(Level.SEVERE, () -> String.format("Error in pipeline fragmenter list, " +
                            "cannot find fragmenter with display name %s.", aFragmenterDisplayName));
                    throw new IllegalArgumentException("fragmenter list for pipeline must not contain null");
                }
            }
        }
        Button tmpFragmenterSettingsButton = this.newFragmenterSettingsButton(tmpComboBox);
        Label tmpLabel = new Label(String.valueOf(++this.algorithmCounter));
        //remove removeButton from upper Row
        if (this.algorithmCounter > 1) {
            this.pipelineSettingsView.getGridPane().getChildren().removeIf(node ->
                    node instanceof Button button
                            && (GridPane.getRowIndex(node) == this.algorithmCounter - 1)
                            && (button).getText().equals(Message.get("PipelineSettingsView.removeRowButton.text")));
        }
        //remove addButton (it is alone in the row until this point)
        this.pipelineSettingsView.getGridPane().getChildren().removeIf(node -> GridPane.getRowIndex(node) == this.algorithmCounter);
        //add new content to row
        this.pipelineSettingsView.addAlgorithmChoiceRow(tmpLabel, tmpComboBox, tmpFragmenterSettingsButton, this.algorithmCounter);
        //add new remove button to row
        if (this.algorithmCounter > 1) {
            this.addRemoveRowButton(this.algorithmCounter);
        }
        //add new add button to next row, +1 cause of next row
        this.addAddRowButton(this.algorithmCounter + 1);
    }
    //
    /**
     * Creates a new gear button that opens the fragmenter settings. It will be disabled if the selected item of the
     * given combo box is null.
     *
     * @param aComboBox to bind the button's disable property to
     * @return the new button
     */
    private Button newFragmenterSettingsButton(ComboBox<String> aComboBox) {
        Button tmpFragmenterSettingsButton = new Button();
        tmpFragmenterSettingsButton.setTooltip(GuiUtil.createTooltip(Message.get("PipelineSettingsView.settingButton.toolTip")));
        String tmpIconURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.imagesFolder")
                        + this.configuration.getProperty("mortar.icon.gear.name")).toExternalForm();
        tmpFragmenterSettingsButton.setGraphic(new ImageView(new Image(tmpIconURL)));
        tmpFragmenterSettingsButton.setMinHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpFragmenterSettingsButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpFragmenterSettingsButton.setMaxHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpFragmenterSettingsButton.setMinWidth(GuiDefinitions.GUI_PIPELINE_SETTINGS_VIEW_BUTTON_WIDTH_VALUE);
        tmpFragmenterSettingsButton.setPrefWidth(GuiDefinitions.GUI_PIPELINE_SETTINGS_VIEW_BUTTON_WIDTH_VALUE);
        tmpFragmenterSettingsButton.setMaxWidth(GuiDefinitions.GUI_PIPELINE_SETTINGS_VIEW_BUTTON_WIDTH_VALUE);
        BooleanBinding tmpBooleanBinding = Bindings.isNull(aComboBox.getSelectionModel().selectedItemProperty());
        tmpFragmenterSettingsButton.disableProperty().bind(tmpBooleanBinding);
        tmpFragmenterSettingsButton.setOnAction(anActionEvent -> {
            int tmpFragmenterListIndex = GridPane.getRowIndex(tmpFragmenterSettingsButton) - 1;
            IMoleculeFragmenter[] tmpArray = new IMoleculeFragmenter[1];
            tmpArray[0] = this.selectedPipelineFragmentersList.get(tmpFragmenterListIndex);
            FragmentationSettingsViewController tmpFragmentationSettingsViewController = new FragmentationSettingsViewController(
                    this.pipelineSettingsViewStage,
                    tmpArray,
                    this.selectedPipelineFragmentersList.get(tmpFragmenterListIndex).getFragmentationAlgorithmDisplayName(),
                    this.configuration);
        });
        return tmpFragmenterSettingsButton;
    }
    //
    /**
     * Creates a new combo box for choosing a pipeline fragmenter for a specific step.
     *
     * @return the new combo box
     */
    private ComboBox<String> newFragmenterComboBox() {
        ComboBox<String> tmpComboBox = new ComboBox<>();
        for (IMoleculeFragmenter tmpFragmenter : this.fragmentationService.getFragmenters()) {
            tmpComboBox.getItems().add(tmpFragmenter.getFragmentationAlgorithmDisplayName());
        }
        tmpComboBox.setOnAction(anActionEvent -> {
            String tmpSelectedFragmenterDisplayName = tmpComboBox.getSelectionModel().getSelectedItem();
            //grid pane row index starts at 1, index of fragmenters list starts at 0
            int tmpIndexInList = GridPane.getRowIndex(tmpComboBox) - 1;
            boolean tmpIsFragmenterFound = false;
            for (IMoleculeFragmenter tmpFragmenter : this.fragmentationService.getFragmenters()) {
                if (tmpSelectedFragmenterDisplayName.equals(tmpFragmenter.getFragmentationAlgorithmDisplayName())) {
                    if (this.selectedPipelineFragmentersList.size() > tmpIndexInList) {
                        this.selectedPipelineFragmentersList.set(tmpIndexInList, tmpFragmenter.copy());
                    } else {
                        this.selectedPipelineFragmentersList.add(tmpFragmenter.copy());
                    }
                    tmpIsFragmenterFound = true;
                    break;
                }
            }
            if (!tmpIsFragmenterFound) {
                PipelineSettingsViewController.LOGGER.log(Level.SEVERE, String.format("Error in pipeline fragmenter list, " +
                        "cannot find fragmenter with display name %s.", tmpSelectedFragmenterDisplayName));
                throw new IllegalArgumentException("fragmenter list for pipeline must not contain null");
            }
        });
        return tmpComboBox;
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
        tmpAddButton.setText(Message.get("PipelineSettingsView.addNewRowButton.text"));
        tmpAddButton.setStyle("-fx-font-weight: bold");
        tmpAddButton.setMinHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpAddButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpAddButton.setMaxHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpAddButton.setMinWidth(GuiDefinitions.GUI_PIPELINE_SETTINGS_VIEW_BUTTON_WIDTH_VALUE);
        tmpAddButton.setPrefWidth(GuiDefinitions.GUI_PIPELINE_SETTINGS_VIEW_BUTTON_WIDTH_VALUE);
        tmpAddButton.setMaxWidth(GuiDefinitions.GUI_PIPELINE_SETTINGS_VIEW_BUTTON_WIDTH_VALUE);
        tmpAddButton.setOnAction(anActionEvent ->
                this.addNewChoiceRow(this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmDisplayName(), true));
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
        tmpRemoveButton.setText(Message.get("PipelineSettingsView.removeRowButton.text"));
        tmpRemoveButton.setStyle("-fx-font-weight: bold");
        tmpRemoveButton.setMinHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpRemoveButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpRemoveButton.setMaxHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpRemoveButton.setMinWidth(GuiDefinitions.GUI_PIPELINE_SETTINGS_VIEW_BUTTON_WIDTH_VALUE);
        tmpRemoveButton.setPrefWidth(GuiDefinitions.GUI_PIPELINE_SETTINGS_VIEW_BUTTON_WIDTH_VALUE);
        tmpRemoveButton.setMaxWidth(GuiDefinitions.GUI_PIPELINE_SETTINGS_VIEW_BUTTON_WIDTH_VALUE);
        tmpRemoveButton.setOnAction(anActionEvent -> {
            int tmpRowIndex = GridPane.getRowIndex(tmpRemoveButton);
            //remove addButton
            this.pipelineSettingsView.getGridPane().getChildren().removeIf(node -> GridPane.getRowIndex(node) == tmpRowIndex + 1);
            //remove complete row content
            this.pipelineSettingsView.getGridPane().getChildren().removeIf(node -> GridPane.getRowIndex(node) == tmpRowIndex);
            //add addButton
            this.addAddRowButton(tmpRowIndex);
            this.algorithmCounter--;
            this.selectedPipelineFragmentersList.removeLast();
            if (this.algorithmCounter > 1) {
                this.addRemoveRowButton(this.algorithmCounter);
            }
        });
        this.pipelineSettingsView.addRemoveRowButton(tmpRemoveButton, aRowNumber);
    }
    //
    /**
     * Cancels changes that are made in the fragmenter list. If there is no set pipeline available in the fragmentation
     * service, a copy of the selected fragmenter is used to build the new pipeline. Otherwise, the pipeline fragmenters
     * from the fragmentation service are copied to the displayed pipeline. The GUI is not(!) updated, neither is
     * the algorithm counter. Method to be used for initial synchronisation with the fragmentation service at
     * opening of the dialog and if the changes made in the dialog should be cancelled (and the dialog closed).
     */
    private void cancelChangesInFragmenterList() {
        this.selectedPipelineFragmentersList.clear();
        if (this.fragmentationService.getPipelineFragmenter() != null || this.fragmentationService.getPipelineFragmenter().length >= 1) {
            for (IMoleculeFragmenter tmpFragmenter : this.fragmentationService.getPipelineFragmenter()) {
                try {
                    this.selectedPipelineFragmentersList.add(tmpFragmenter.copy());
                } catch (Exception anException) {
                    PipelineSettingsViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                }
            }
        } else {
            try {
                this.selectedPipelineFragmentersList.add(this.fragmentationService.getSelectedFragmenter().copy());
            } catch (Exception anException) {
                PipelineSettingsViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            }
        }
        if (this.selectedPipelineFragmentersList.isEmpty()) {
            this.selectedPipelineFragmentersList.add(new ErtlFunctionalGroupsFinderFragmenter());
        }
    }
    //
    /**
     * Resets the complete pipeline and the view. Selected fragmenter for single fragmentation will be set as default value.
     */
    private void reset() {
        this.setPipelineName("");
        //remove complete row content
        this.pipelineSettingsView.getGridPane().getChildren().removeIf(node -> GridPane.getRowIndex(node) > 0);
        this.selectedPipelineFragmentersList.clear();
        this.algorithmCounter = 0;
        this.addNewChoiceRow(this.fragmentationService.getSelectedFragmenterDisplayName(), true);
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
        return this.pipelineNameProperty;
    }
    //
    /**
     * Returns pipeline name.
     *
     * @return String pipeline name
     */
    public String getPipelineName() {
        return this.pipelineNameProperty.get();
    }
    //
    /**
     * Sets pipeline name.
     *
     * @param aName String
     */
    public void setPipelineName(String aName) {
        this.pipelineNameProperty.set(aName);
    }
    //
    /**
     * Return boolean value whether fragmentation is started inside PipelineSettingsView via FragmentButton.
     *
     * @return true if fragmentation is started
     */
    public boolean isFragmentationStarted() {
        return this.isFragmentationStarted;
    }
    //</editor-fold>
}
