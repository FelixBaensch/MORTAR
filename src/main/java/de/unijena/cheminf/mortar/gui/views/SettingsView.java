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
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.Tooltip;
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

/**
 * View class for the settings windows, both global and fragmenter settings.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
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
    //
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
        this.defaultButton = GuiUtil.getButtonOfStandardSize(Message.get("SettingsView.defaultButton.text"));
        this.defaultButton.setTooltip(new Tooltip(Message.get("SettingsView.defaultButton.toolTip")));
        this.hBoxLeftSideButtons = new HBox();
        this.hBoxLeftSideButtons.getChildren().add(this.defaultButton);
        this.hBoxLeftSideButtons.setAlignment(Pos.CENTER_LEFT);
        this.hBoxLeftSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        this.hBoxLeftSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(this.hBoxLeftSideButtons, Priority.ALWAYS);
        this.hBoxButtonsHBox.getChildren().add(this.hBoxLeftSideButtons);
        //-right side
        this.hBoxRightSideButtons = new HBox();
        this.cancelButton = GuiUtil.getButtonOfStandardSize(Message.get("SettingsView.cancelButton.text"));
        this.cancelButton.setTooltip(new Tooltip(Message.get("SettingsView.cancelButton.toolTip")));
        this.applyButton = GuiUtil.getButtonOfStandardSize(Message.get("SettingsView.applyButton.text"));
        this.applyButton.setTooltip(new Tooltip(Message.get("SettingsView.applyButton.toolTip")));
        this.hBoxRightSideButtons.getChildren().addAll(this.applyButton, this.cancelButton);
        this.hBoxRightSideButtons.setAlignment(Pos.CENTER_RIGHT);
        this.hBoxRightSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        this.hBoxRightSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(this.hBoxRightSideButtons, Priority.ALWAYS);
        this.hBoxButtonsHBox.getChildren().add(this.hBoxRightSideButtons);
        //
        this.getChildren().add(this.borderPane);
    }
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     * Adds a tab which contains the properties of the given properties list
     * @param aStage Stage to bind width and height
     * @param aLabel Label for the tab title and the tab Id
     * @param aPropertiesList List of properties to show in created tab
     * @param aTooltipTextsMap Map containing setting names as keys and tooltip text as values
     * @param aRecentPropertiesMap Map to hold recent properties to restore them if necessary
     * @return Tab
     */
    public Tab addTab(Stage aStage, String aLabel, List<Property<?>> aPropertiesList, Map<String, String> aTooltipTextsMap, Map<String, Object> aRecentPropertiesMap){
        Tab tmpTab = new Tab();
        tmpTab.setClosable(false);
        tmpTab.setId(aLabel);
        Label tmpTabTitle = new Label(aLabel);
        StackPane tmpStackPane = new StackPane(new Group(tmpTabTitle));
        tmpTab.setGraphic(tmpStackPane);
        tmpTab.setStyle("-fx-pref-height: 150");
        ScrollPane tmpScrollPane = new ScrollPane();
        tmpScrollPane.setFitToWidth(true);
        tmpScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        GridPane tmpGridPane = new GridPane();
        tmpGridPane.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        ColumnConstraints tmpColCon1 = new ColumnConstraints();
        tmpColCon1.setHalignment(HPos.LEFT);
        tmpColCon1.setHgrow(Priority.ALWAYS);
        tmpColCon1.prefWidthProperty().bind(
                tmpScrollPane.widthProperty().multiply(0.5)
        );
        tmpGridPane.getColumnConstraints().add(tmpColCon1);
        ColumnConstraints tmpColCon2 = new ColumnConstraints();
        tmpColCon2.setHalignment(HPos.RIGHT);
        tmpColCon2.setHgrow(Priority.ALWAYS);
        tmpColCon2.prefWidthProperty().bind(
                tmpScrollPane.widthProperty().multiply(0.5)
        );
        tmpGridPane.getColumnConstraints().add(tmpColCon2);
        this.addPropertyItems(tmpGridPane, aPropertiesList, aTooltipTextsMap, aRecentPropertiesMap);
        tmpScrollPane.setContent(tmpGridPane);
        tmpTab.setContent(tmpScrollPane);
        this.tabPane.getTabs().add(tmpTab);
        return tmpTab;
    }
    //
    /**
     * Adds a row for each {@link Property} of given List which contains of properties name and a control to change properties value
     * @param aGridPane GridPane to add row
     * @param aPropertiesList List of properties to show in created tab
     * @param aTooltipTextsMap Map containing setting names as keys and tooltip text as values
     * @param aRecentPropertiesMap Map to hold recent properties to restore them if necessary
     */
    private void addPropertyItems(GridPane aGridPane, List<Property<?>> aPropertiesList, Map<String, String> aTooltipTextsMap, Map<String, Object> aRecentPropertiesMap){
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
            Tooltip tmpTooltip = new Tooltip(aTooltipTextsMap.get(tmpProperty.getName()));
            tmpTooltip.setMaxWidth(GuiDefinitions.GUI_TOOLTIP_MAX_WIDTH);
            tmpTooltip.setWrapText(true);
            tmpNameLabel.setTooltip(tmpTooltip);
            aGridPane.add(tmpNameLabel, 0, tmpRowIndex);
            GridPane.setMargin(tmpNameLabel, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            Object tmpRecentValue = tmpProperty.getValue();
            aRecentPropertiesMap.put(tmpPropName, tmpRecentValue);
            if(tmpProperty instanceof SimpleBooleanProperty){
                ComboBox<Boolean> tmpBooleanComboBox = new ComboBox<>();
                tmpBooleanComboBox.getItems().addAll(Boolean.FALSE, Boolean.TRUE);
                tmpBooleanComboBox.valueProperty().bindBidirectional(tmpProperty);
                tmpBooleanComboBox.setTooltip(tmpTooltip);
                //add to gridpane
                aGridPane.add(tmpBooleanComboBox, 1, tmpRowIndex++);
                GridPane.setMargin(tmpBooleanComboBox, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            }
            else if(tmpProperty instanceof SimpleIntegerProperty){
                TextField tmpIntegerTextField = new TextField();
                tmpIntegerTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
                tmpIntegerTextField.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE);
                tmpIntegerTextField.setAlignment(Pos.CENTER_RIGHT);
                TextFormatter<Integer> tmpFormatter = new TextFormatter<>(GuiUtil.getStringToIntegerConverter(), 0, GuiUtil.getIntegerFilter());
                tmpIntegerTextField.setTextFormatter(tmpFormatter);
                tmpFormatter.valueProperty().bindBidirectional(tmpProperty);
                tmpIntegerTextField.setTooltip(tmpTooltip);
                //add to gridpane
                aGridPane.add(tmpIntegerTextField, 1, tmpRowIndex++);
                GridPane.setMargin(tmpIntegerTextField, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            }
            else if(tmpProperty instanceof SimpleDoubleProperty){
                TextField tmpDoubleTextField = new TextField();
                tmpDoubleTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
                tmpDoubleTextField.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE);
                tmpDoubleTextField.setAlignment(Pos.CENTER_RIGHT);
                TextFormatter<Double> tmpFormatter = new TextFormatter<>(GuiUtil.getStringToDoubleConverter(), 0.0, GuiUtil.getDoubleFilter());
                tmpDoubleTextField.setTextFormatter(tmpFormatter);
                tmpFormatter.valueProperty().bindBidirectional(tmpProperty);
                tmpDoubleTextField.setTooltip(tmpTooltip);
                //add to gridpane
                aGridPane.add(tmpDoubleTextField, 1, tmpRowIndex++);
                GridPane.setMargin(tmpDoubleTextField, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            }
            else if(tmpProperty instanceof SimpleEnumConstantNameProperty){
                ComboBox<String> tmpEnumComboBox = new ComboBox();
                tmpEnumComboBox.getItems().addAll(((SimpleEnumConstantNameProperty) tmpProperty).getAssociatedEnumConstantNames());
                tmpEnumComboBox.valueProperty().bindBidirectional(tmpProperty);
                tmpEnumComboBox.setTooltip(tmpTooltip);
                //add to gridpane
                aGridPane.add(tmpEnumComboBox, 1, tmpRowIndex++);
                GridPane.setMargin(tmpEnumComboBox, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            }
            else if(tmpProperty instanceof SimpleStringProperty){
                TextField tmpStringTextField = new TextField();
                tmpStringTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
                tmpStringTextField.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE);
                tmpStringTextField.setAlignment(Pos.CENTER_RIGHT);
                tmpStringTextField.textProperty().bindBidirectional(tmpProperty);
                tmpStringTextField.setTooltip(tmpTooltip);
                //add to gridpane
                aGridPane.add(tmpStringTextField, 1, tmpRowIndex++);
                GridPane.setMargin(tmpStringTextField, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            }
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns the tab pane, holding tabs for the settings of the different fragmenters
     *
     * @return TabPane
     */
    public TabPane getTabPane(){
        return this.tabPane;
    }
    //
    /**
     * Returns selection model, holding the active tab.
     * Used to set tab of the selected fragmenter as active tab
     *
     * @return SelectionModel {@literal <} Tab {@literal >}
     */
    public SelectionModel<Tab> getSelectionModel(){
        return this.selectionModel;
    }
    //
    /**
     * Returns cancel button, which closes the view without saving changes
     *
     * @return CancelButton
     */
    public Button getCancelButton(){
        return this.cancelButton;
    }
    //
    /**
     * Returns apply button, which applies changes and closes the view
     *
     * @return ApplyButton
     */
    public Button getApplyButton(){
        return this.applyButton;
    }
    //
    /**
     * Returns default button, which sets all options of active tab to default values
     *
     * @return DefaultButton
     */
    public Button getDefaultButton(){
        return this.defaultButton;
    }
    //</editor-fold>
}
