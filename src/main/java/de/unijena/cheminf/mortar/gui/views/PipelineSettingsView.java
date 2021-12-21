/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.gui.views;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.message.Message;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class PipelineSettingsView extends AnchorPane {

    private final BorderPane borderPane;
    private GridPane gridPane;
    private ChoiceBox firstAlgorithmChoiceBox;
    private Button cancelButton;
    private Button applyButton;
    private Button fragmentButton;
    private Button defaultButton;
    private TextField textField;

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
        this.defaultButton = new Button(Message.get("PipelineSettingsView.defaultButton.text"));
        HBox tmpHBoxLeftSideButtons = new HBox();
        tmpHBoxLeftSideButtons.getChildren().add(this.defaultButton);
        tmpHBoxLeftSideButtons.setAlignment(Pos.CENTER_LEFT);
        tmpHBoxLeftSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxLeftSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxLeftSideButtons, Priority.ALWAYS);
//        tmpHBoxButtonsHBox.getChildren().add(tmpHBoxLeftSideButtons); //Do not delete
        //-right side
        HBox tmpHBoxRightSideButtons = new HBox();
        this.cancelButton = new Button(Message.get("PipelineSettingsView.cancelButton.text"));
        this.cancelButton.setTooltip(new Tooltip(Message.get("PipelineSettingsView.cancelButton.toolTip")));
        this.fragmentButton = new Button(Message.get("PipelineSettingsView.fragmentButton.text"));
        this.fragmentButton.setTooltip(new Tooltip(Message.get("PipelineSettingsView.fragmentButton.toolTip")));
        this.applyButton = new Button(Message.get("PipelineSettingsView.applyButton.text"));
        this.applyButton.setTooltip(new Tooltip(Message.get("PipelineSettingsView.applyButton.toolTip")));
        tmpHBoxRightSideButtons.getChildren().addAll(this.fragmentButton, this.applyButton, this.cancelButton);
        tmpHBoxRightSideButtons.setAlignment(Pos.CENTER_RIGHT);
        tmpHBoxRightSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxRightSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxRightSideButtons, Priority.ALWAYS);
        tmpHBoxButtonsHBox.getChildren().add(tmpHBoxRightSideButtons);
        //
        this.getChildren().add(this.borderPane);
    }

    public void addGrid(Stage aStage){
        ScrollPane tmpScrollPane = new ScrollPane();
        HBox.setHgrow(tmpScrollPane,Priority.ALWAYS);
        VBox.setVgrow(tmpScrollPane,Priority.ALWAYS);
        this.borderPane.setCenter(tmpScrollPane);
        this.createGridPane(aStage);
        tmpScrollPane.setContent(this.gridPane);
    }

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
                aStage.widthProperty().multiply(0.8)
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
        this.textField.setPromptText(Message.get("PipelineSettingsView.textField.promptText"));
        this.gridPane.add(this.textField, 1, 0 );
    }

    public void addAlgorithmChoiceRow(Label aNumberingLabel, ComboBox aComboBox, Button aSettingsButton, int aRowNumber){
        this.gridPane.add(aNumberingLabel, 0, aRowNumber);
        this.gridPane.add(aComboBox, 1, aRowNumber);
        this.gridPane.add(aSettingsButton, 2, aRowNumber);
    }

    public void addNumberingLabel(Label aNumberingLabel, int aRowNumber){
        this.gridPane.add(aNumberingLabel, 0, aRowNumber);
    }
    public void addChoiceBox(ComboBox aComboBox, int aRowNumber){
        this.gridPane.add(aComboBox, 1, aRowNumber);
    }
    public void addSettingsButton(Button aSettingsButton, int aRowNumber){
        this.gridPane.add(aSettingsButton, 2, aRowNumber);
    }
    public void addRemoveRowButton(Button aRemoveButton, int aRowNumber){
        this.gridPane.add(aRemoveButton, 3, aRowNumber);
    }

    /**
     * Adds given button to given row number
     * @param anAddButton Button
     * @param aRowNumber int
     */
    public void addAddRowButton(Button anAddButton, int aRowNumber){
        this.gridPane.add(anAddButton, 2, aRowNumber);
    }

    public GridPane getGridPane(){
        return this.gridPane;
    }
    public Button getCancelButton(){
        return this.cancelButton;
    }
    public Button getFragmentButton(){
        return this.fragmentButton;
    }
    public Button getDefaultButton(){
        return this.defaultButton;
    }
    public Button getApplyButton()
    {
        return this.applyButton;
    }
    public TextField getTextField()
    {
        return this.textField;
    }
}
