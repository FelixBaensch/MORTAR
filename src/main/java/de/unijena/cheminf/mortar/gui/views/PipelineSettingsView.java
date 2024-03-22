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

package de.unijena.cheminf.mortar.gui.views;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * View for the pipeline settings
 *
 * @author Felix Baensch, Jonas Schaub
 * @version 1.0.0.0
 */
public class PipelineSettingsView extends AnchorPane {
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * BorderPane to hold and adjust child panes
     */
    private BorderPane borderPane;
    /**
     * GridPane to structure "algorithm choice rows"
     */
    private GridPane gridPane;
    /**
     * Button to cancel changes and close view
     */
    private Button cancelButton;
    /**
     * Button to apply changes and close view
     */
    private Button applyButton;
    /**
     * Button to start pipeline fragmentation and close view
     */
    private Button fragmentButton;
    /**
     * Button to set pipeline and view o default state
     */
    private Button defaultButton;
    /**
     * TextField for the pipeline name
     */
    private TextField textField;
    //</editor-fold>
    //
    /**
     * Constructor
     */
    public PipelineSettingsView(){
        super();
        //borderPane
        this.borderPane = new BorderPane();
        PipelineSettingsView.setTopAnchor(this.borderPane, 0.0);
        PipelineSettingsView.setRightAnchor(this.borderPane, 0.0);
        PipelineSettingsView.setLeftAnchor(this.borderPane, 0.0);
        PipelineSettingsView.setBottomAnchor(this.borderPane, 0.0);
        //buttons
        HBox tmpHBoxButtonsHBox = new HBox();
        tmpHBoxButtonsHBox.setStyle("-fx-background-color: LightGrey");
        this.borderPane.setBottom(tmpHBoxButtonsHBox);
        //-left side
        this.defaultButton = GuiUtil.getButtonOfStandardSize(Message.get("PipelineSettingsView.defaultButton.text"));
        this.defaultButton.setTooltip(GuiUtil.createTooltip(Message.get("PipelineSettingsView.defaultButton.tooltip")));
        HBox tmpHBoxLeftSideButtons = new HBox();
        tmpHBoxLeftSideButtons.getChildren().add(this.defaultButton);
        tmpHBoxLeftSideButtons.setAlignment(Pos.CENTER_LEFT);
        tmpHBoxLeftSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxLeftSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxLeftSideButtons, Priority.ALWAYS);
        tmpHBoxButtonsHBox.getChildren().add(tmpHBoxLeftSideButtons); //Do not delete
        //-right side
        HBox tmpHBoxRightSideButtons = new HBox();
        this.cancelButton = GuiUtil.getButtonOfStandardSize(Message.get("PipelineSettingsView.cancelButton.text"));
        this.cancelButton.setTooltip(GuiUtil.createTooltip(Message.get("PipelineSettingsView.cancelButton.toolTip")));
        this.fragmentButton = GuiUtil.getButtonOfStandardSize(Message.get("PipelineSettingsView.fragmentButton.text"));
        this.fragmentButton.setTooltip(GuiUtil.createTooltip(Message.get("PipelineSettingsView.fragmentButton.toolTip")));
        this.applyButton = GuiUtil.getButtonOfStandardSize(Message.get("PipelineSettingsView.applyButton.text"));
        this.applyButton.setTooltip(GuiUtil.createTooltip(Message.get("PipelineSettingsView.applyButton.toolTip")));
        tmpHBoxRightSideButtons.getChildren().addAll(this.fragmentButton, this.applyButton, this.cancelButton);
        tmpHBoxRightSideButtons.setAlignment(Pos.CENTER_RIGHT);
        tmpHBoxRightSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxRightSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxRightSideButtons, Priority.ALWAYS);
        tmpHBoxButtonsHBox.getChildren().add(tmpHBoxRightSideButtons);
        //
        this.getChildren().add(this.borderPane);
    }
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     * Adds the GridPane for the "algorithm choice rows" inside of a ScrollPane, Stage is necessary to bind width
     *
     * @param aStage Stage to bind width
     */
    public void addGrid(Stage aStage){
        ScrollPane tmpScrollPane = new ScrollPane();
        HBox.setHgrow(tmpScrollPane,Priority.ALWAYS);
        VBox.setVgrow(tmpScrollPane,Priority.ALWAYS);
        this.borderPane.setCenter(tmpScrollPane);
        this.createGridPane(aStage);
        tmpScrollPane.setContent(this.gridPane);
    }
    //
    /**
     * Adds a new "algorithm choice row"
     *
     * @param aNumberingLabel Label to show row number
     * @param aComboBox CombBox to select fragmentation algorithm
     * @param aSettingsButton Button to open SettingsView for the corresponding algorithm
     * @param aRowNumber int row number, shown in Label
     */
    public void addAlgorithmChoiceRow(Label aNumberingLabel, ComboBox aComboBox, Button aSettingsButton, int aRowNumber){
        this.gridPane.add(aNumberingLabel, 0, aRowNumber);
        this.gridPane.add(aComboBox, 1, aRowNumber);
        this.gridPane.add(aSettingsButton, 2, aRowNumber);
    }
    //
    /**
     * Adds a new Button which removes last "algorithm choice row" of GridPane
     *
     * @param aRemoveButton Button to remove a row
     * @param aRowNumber int specifies which row should be removed
     */
    public void addRemoveRowButton(Button aRemoveButton, int aRowNumber){
        this.gridPane.add(aRemoveButton, 3, aRowNumber);
    }
    //
    /**
     * Adds given button to given row number
     * @param anAddButton Button ad a row
     * @param aRowNumber int specifies to which row the button should be added
     */
    public void addAddRowButton(Button anAddButton, int aRowNumber){
        this.gridPane.add(anAddButton, 2, aRowNumber);
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Creates the GridPane to align "algorithm rows", Stage is necessary to bind width
     *
     * @param aStage Stage to bind width
     */
    private void createGridPane(Stage aStage){
        if(this.gridPane != null)
            this.gridPane = null;
        this.gridPane = new GridPane();
        this.gridPane.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        this.gridPane.setVgap(GuiDefinitions.GUI_INSETS_VALUE);
        this.gridPane.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        //0th column (numbering)
        ColumnConstraints tmpColCon0 = new ColumnConstraints();
        tmpColCon0.setHalignment(HPos.CENTER);
        tmpColCon0.setHgrow(Priority.ALWAYS);
//        tmpColCon0.prefWidthProperty().bind(
//                aStage.widthProperty().multiply(0.05)
//        );
        tmpColCon0.setPrefWidth(40);
        tmpColCon0.setMinWidth(40);
        tmpColCon0.setMaxWidth(40);
        this.gridPane.getColumnConstraints().add(tmpColCon0);
        //1st column (choicebox for algorithm)
        ColumnConstraints tmpColCon1 = new ColumnConstraints();
        tmpColCon1.setHalignment(HPos.LEFT);
        tmpColCon1.setHgrow(Priority.ALWAYS);
        tmpColCon1.prefWidthProperty().bind(
//                aStage.widthProperty().multiply(0.8)
                aStage.widthProperty().subtract(200) //magic number
        );
        this.gridPane.getColumnConstraints().add(tmpColCon1);
        //2nd column (settings button or add new algorithm button)
        ColumnConstraints tmpColCon2 = new ColumnConstraints();
        tmpColCon2.setHalignment(HPos.CENTER);
        tmpColCon2.setHgrow(Priority.ALWAYS);
        tmpColCon2.setPrefWidth(40);
        tmpColCon2.setMinWidth(40);
        tmpColCon2.setMaxWidth(40);
//        tmpColCon2.prefWidthProperty().bind(
//                aStage.widthProperty().multiply(0.175)
//        );
        this.gridPane.getColumnConstraints().add(tmpColCon2);
        //3d column (remove algorithm button)
        ColumnConstraints tmpColCon3 = new ColumnConstraints();
        tmpColCon3.setHalignment(HPos.CENTER);
        tmpColCon3.setHgrow(Priority.ALWAYS);
        tmpColCon3.setPrefWidth(40);
        tmpColCon3.setMinWidth(40);
        tmpColCon3.setMaxWidth(40);
//        tmpColCon3.prefWidthProperty().bind(
//                aStage.widthProperty().multiply(0.175)
//        );
        this.gridPane.getColumnConstraints().add(tmpColCon3);
        //row constraints
        RowConstraints tmpRowCon1 = new RowConstraints();
        tmpRowCon1.setValignment(VPos.CENTER);
        tmpRowCon1.setVgrow(Priority.ALWAYS);
        this.gridPane.getRowConstraints().add(tmpRowCon1);
        tmpRowCon1.setPrefHeight(50);
        tmpRowCon1.setMaxHeight(50);
        tmpRowCon1.setMinHeight(50);
        //name textfield
        this.textField = new TextField();
        this.textField.setMaxWidth(250);
        this.textField.setPromptText(Message.get("PipelineSettingsView.textField.promptText"));
        this.gridPane.add(this.textField, 1, 0 );
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns GridPane which holds the "algorithm choice rows"
     *
     * @return GridPane
     */
    public GridPane getGridPane(){
        return this.gridPane;
    }
    //
    /**
     * Returns cancelButton to cancel changes and close view
     *
     * @return Button
     */
    public Button getCancelButton(){
        return this.cancelButton;
    }
    //
    /**
     * Returns fragmentButton to start pipeline fragmentation and close view
     *
     * @return Button
     */
    public Button getFragmentButton(){
        return this.fragmentButton;
    }
    //
    /**
     * Returns defaultButton to set settings and view to default
     *
     * @return Button
     */
    public Button getDefaultButton(){
        return this.defaultButton;
    }
    //
    /**
     * Returns applyButton to apply changes and close view
     *
     * @return Button
     */
    public Button getApplyButton()
    {
        return this.applyButton;
    }
    //
    /**
     * Returns textField which shows pipeline name
     *
     * @return TextField
     */
    public TextField getTextField()
    {
        return this.textField;
    }
    //</editor-fold>
}
