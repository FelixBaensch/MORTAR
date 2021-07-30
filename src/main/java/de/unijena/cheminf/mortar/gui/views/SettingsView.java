/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class SettingsView extends AnchorPane {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private TabPane tabPane;
    private BorderPane borderPane;
    private Button cancelButton;
    private Button applyButton;
    private Button defaultButton;
    private HBox hBoxRightSideButtons;
    private HBox hBoxLeftSideButtons;
    private HBox hBoxButtonsHBox;
    private SelectionModel<Tab> selectionModel;
    //</editor-fold>

    /**
     * Constructor
     */
    public SettingsView(){
        super();
        //borderPane
        this.borderPane = new BorderPane();
        SettingsView.setTopAnchor(this.borderPane, 0.0);
        SettingsView.setRightAnchor(this.borderPane, 0.0);
        SettingsView.setLeftAnchor(this.borderPane, 0.0);
        SettingsView.setBottomAnchor(this.borderPane, 0.0);

        //tabPane
        this.tabPane =  new TabPane();
        this.selectionModel = this.tabPane.getSelectionModel();
        this.tabPane.setSide(Side.LEFT);
        this.borderPane.setCenter(this.tabPane);
        //buttons
        this.hBoxButtonsHBox = new HBox();
        this.hBoxButtonsHBox.setStyle("-fx-background-color: LightGrey");
        this.borderPane.setBottom(hBoxButtonsHBox);
        //-left side
        this.defaultButton = new Button(Message.get("SettingsView.defaultButton.text"));
        this.hBoxLeftSideButtons = new HBox();
        this.hBoxLeftSideButtons.getChildren().add(this.defaultButton);
        this.hBoxLeftSideButtons.setAlignment(Pos.CENTER_LEFT);
        this.hBoxLeftSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        this.hBoxLeftSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(this.hBoxLeftSideButtons, Priority.ALWAYS);
        this.hBoxButtonsHBox.getChildren().add(this.hBoxLeftSideButtons);
        //-right side
        this.hBoxRightSideButtons = new HBox();
        this.cancelButton = new Button(Message.get("SettingsView.cancelButton.text"));
        this.applyButton = new Button(Message.get("SettingsView.applyButton.text"));
        this.hBoxRightSideButtons.getChildren().addAll(this.applyButton, this.cancelButton);
        this.hBoxRightSideButtons.setAlignment(Pos.CENTER_RIGHT);
        this.hBoxRightSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        this.hBoxRightSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(this.hBoxRightSideButtons, Priority.ALWAYS);
        this.hBoxButtonsHBox.getChildren().add(this.hBoxRightSideButtons);
        //
        this.getChildren().add(this.borderPane);
    }

    /**
     * Adds a tab which contains the properties of the given properties list
     * @param aStage Stage to bind width and height
     * @param aLabel Label for the tab title and the tab Id
     * @param aPropertiesList List of properties to show in created tab
     * @param aRecentPropertiesMap Map to hold recent properties to restore them if necessary
     * @return Tab
     */
    public Tab addTab(Stage aStage, String aLabel, List<Property> aPropertiesList, Map<String, Object> aRecentPropertiesMap){
        Tab tmpTab = new Tab();
        tmpTab.setClosable(false);
        tmpTab.setId(aLabel);
        Label tmpTabTitle = new Label(aLabel);
        StackPane tmpStackPane = new StackPane(new Group(tmpTabTitle));
        tmpTab.setGraphic(tmpStackPane);
        tmpTab.setStyle("-fx-pref-height: 150");
        ScrollPane tmpScrollPane = new ScrollPane();
        GridPane tmpGridPane = new GridPane();
        tmpGridPane.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        ColumnConstraints tmpColCon1 = new ColumnConstraints();
        tmpColCon1.setHalignment(HPos.LEFT);
        tmpColCon1.setHgrow(Priority.ALWAYS);
        tmpColCon1.prefWidthProperty().bind(
                aStage.widthProperty().multiply(0.5)
        );
        tmpGridPane.getColumnConstraints().add(tmpColCon1);
        ColumnConstraints tmpColCon2 = new ColumnConstraints();
        tmpColCon2.setHalignment(HPos.RIGHT);
        tmpColCon2.setHgrow(Priority.ALWAYS);
        tmpColCon1.prefWidthProperty().bind(
                aStage.widthProperty().multiply(0.5)
        );
        tmpGridPane.getColumnConstraints().add(tmpColCon2);
        this.addPropertyItems(tmpGridPane, aPropertiesList, aRecentPropertiesMap);
        tmpScrollPane.setContent(tmpGridPane);
        tmpTab.setContent(tmpScrollPane);
        this.tabPane.getTabs().add(tmpTab);
        return tmpTab;
    }

    /**
     * Adds a row for each {@link Property} of given List which contains of properties name and a control to change properties value
     * @param aGridPane GridPane to add row
     * @param aPropertiesList List of properties to show in created tab
     * @param aRecentPropertiesMap Map to hold recent properties to restore them if necessary
     */
    private void addPropertyItems(GridPane aGridPane, List<Property> aPropertiesList, Map<String, Object> aRecentPropertiesMap){
        int tmpRowIndex = 0;
        for(Property tmpProperty : aPropertiesList){
            RowConstraints tmpRow = new RowConstraints();
            tmpRow.setVgrow(Priority.ALWAYS);
            tmpRow.setPrefHeight(50);
            tmpRow.setMaxHeight(50);
            tmpRow.setMinHeight(50);
            aGridPane.getRowConstraints().add(tmpRow);
            String tmpPropName = tmpProperty.getName();
            Label tmpNameLabel = new Label(tmpPropName);
            aGridPane.add(tmpNameLabel, 0, tmpRowIndex);
            GridPane.setMargin(tmpNameLabel, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            Object tmpRecentValue = tmpProperty.getValue(); //TODO: Maybe change this line to getDefault() or something else
            aRecentPropertiesMap.put(tmpPropName, tmpRecentValue);
            if(tmpProperty instanceof SimpleBooleanProperty){
                ComboBox<Boolean> tmpBooleanComboBox = new ComboBox<>();
                tmpBooleanComboBox.getItems().addAll(Boolean.FALSE, Boolean.TRUE);
                tmpBooleanComboBox.valueProperty().bindBidirectional(tmpProperty);
                //add to gridpane
                aGridPane.add(tmpBooleanComboBox, 1, tmpRowIndex++);
                GridPane.setMargin(tmpBooleanComboBox, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            }
            else if(tmpProperty instanceof SimpleIntegerProperty){
                TextField tmpIntegerTextField = new TextField();
                tmpIntegerTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
                tmpIntegerTextField.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE);
                tmpIntegerTextField.setAlignment(Pos.CENTER_RIGHT);
                TextFormatter<Integer> tmpFormatter = new TextFormatter<>(GuiUtil.GetStringToIntegerConverter(), 0, GuiUtil.GetIntegerFilter());
                tmpIntegerTextField.setTextFormatter(tmpFormatter);
                tmpFormatter.valueProperty().bindBidirectional(tmpProperty);
                //add to gridpane
                aGridPane.add(tmpIntegerTextField, 1, tmpRowIndex++);
                GridPane.setMargin(tmpIntegerTextField, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            }
            else if(tmpProperty instanceof SimpleDoubleProperty){
                TextField tmpDoubleTextField = new TextField();
                tmpDoubleTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
                tmpDoubleTextField.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE);
                tmpDoubleTextField.setAlignment(Pos.CENTER_RIGHT);
                TextFormatter<Double> tmpFormatter = new TextFormatter<>(GuiUtil.GetStringToDoubleConverter(), 0.0, GuiUtil.GetDoubleFilter());
                tmpDoubleTextField.setTextFormatter(tmpFormatter);
                tmpFormatter.valueProperty().bindBidirectional(tmpProperty);
                //add to gridpane
                aGridPane.add(tmpDoubleTextField, 1, tmpRowIndex++);
                GridPane.setMargin(tmpDoubleTextField, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            }
            else if(tmpProperty instanceof SimpleEnumConstantNameProperty){
                ComboBox<String> tmpEnumComboBox = new ComboBox();
                tmpEnumComboBox.getItems().addAll(((SimpleEnumConstantNameProperty) tmpProperty).getAssociatedEnumConstantNames());
                tmpEnumComboBox.valueProperty().bindBidirectional(tmpProperty);
                //add to gridpane
                aGridPane.add(tmpEnumComboBox, 1, tmpRowIndex++);
                GridPane.setMargin(tmpEnumComboBox, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            }
        }
    }

    public TabPane getTabPane(){
        return this.tabPane;
    }
    public SelectionModel<Tab> getSelectionModel(){
        return this.selectionModel;
    }
    public Button getCancelButton(){
        return this.cancelButton;
    }
    public Button getApplyButton(){
        return this.applyButton;
    }
    public Button getDefaultButton(){
        return this.defaultButton;
    }
}
