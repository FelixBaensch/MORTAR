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

import de.unijena.cheminf.mortar.gui.controls.ToggleSwitch;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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

import java.util.Collections;
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
    private final TabPane tabPane;
    private final BorderPane borderPane;
    private final Button cancelButton;
    private final Button applyButton;
    private final Button defaultButton;
    private final HBox hBoxRightSideButtons;
    private final HBox hBoxLeftSideButtons;
    private final HBox hBoxButtonsHBox;
    private final SelectionModel<Tab> selectionModel;
    private static final String COLOR_ODD_ROW = "-fx-control-inner-background-alt";
    private static final String COLOR_EVEN_ROW = "-fx-control-inner-background";
    //</editor-fold>
    //
    /**
     * Constructor.
     */
    public SettingsView(){
        super();
        //borderPane
        this.borderPane = new BorderPane();
        AnchorPane.setTopAnchor(this.borderPane, 0.0);
        AnchorPane.setRightAnchor(this.borderPane, 0.0);
        AnchorPane.setLeftAnchor(this.borderPane, 0.0);
        AnchorPane.setBottomAnchor(this.borderPane, 0.0);
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
        this.defaultButton.setTooltip(GuiUtil.createTooltip(Message.get("SettingsView.defaultButton.toolTip")));
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
        this.cancelButton.setTooltip(GuiUtil.createTooltip(Message.get("SettingsView.cancelButton.toolTip")));
        this.applyButton = GuiUtil.getButtonOfStandardSize(Message.get("SettingsView.applyButton.text"));
        this.applyButton.setTooltip(GuiUtil.createTooltip(Message.get("SettingsView.applyButton.toolTip")));
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
     * Adds a tab which contains the properties of the given properties list.
     *
     * @param aLabel Label for the tab title and the tab Id
     * @param aPropertiesList List of properties to show in created tab
     * @param aDisplayNamesMap Map containing setting names as keys and language-specific names for the settings to display in the GUI
     * @param aTooltipTextsMap Map containing setting names as keys and tooltip text as values
     * @param aRecentPropertiesMap Map to hold recent properties to restore them if necessary
     * @return Tab
     */
    public Tab addTab(String aLabel, List<Property<?>> aPropertiesList, Map<String, String> aDisplayNamesMap, Map<String, String> aTooltipTextsMap, Map<String, Object> aRecentPropertiesMap) {
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
        this.addPropertyItems(tmpGridPane, aPropertiesList, aDisplayNamesMap, aTooltipTextsMap, aRecentPropertiesMap);
        tmpScrollPane.setContent(tmpGridPane);
        tmpTab.setContent(tmpScrollPane);
        this.tabPane.getTabs().add(tmpTab);
        return tmpTab;
    }
    //
    /**
     * Adds a row for each {@link Property} of given List which contains the property name and a control to change the property value.
     *
     * @param aGridPane GridPane to add row
     * @param aPropertiesList List of properties to show in created tab
     * @param aDisplayNamesMap Map containing setting names as keys and language-specific names for the settings to display in the GUI
     * @param aTooltipTextsMap Map containing setting names as keys and tooltip text as values
     * @param aRecentPropertiesMap Map to hold recent properties to restore them if necessary
     */
    private void addPropertyItems(GridPane aGridPane, List<Property<?>> aPropertiesList, Map<String, String> aDisplayNamesMap, Map<String, String> aTooltipTextsMap, Map<String, Object> aRecentPropertiesMap) {
        int tmpRowIndex = 0;
        for (Property tmpProperty : aPropertiesList) {
            String backgroundColor = (tmpRowIndex % 2 == 0) ? COLOR_ODD_ROW : COLOR_EVEN_ROW;

            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setVgrow(Priority.ALWAYS);
            rowConstraints.setPrefHeight(100);
            rowConstraints.setMinHeight(100);
            aGridPane.getRowConstraints().add(rowConstraints);

            Label tmpNameLabel = new Label(aDisplayNamesMap.get(tmpProperty.getName()));
            Tooltip tmpTooltip = GuiUtil.createTooltip(aTooltipTextsMap.get(tmpProperty.getName()));
            tmpNameLabel.setTooltip(tmpTooltip);

            // Create a StackPane to wrap the label and apply background color
            StackPane labelWrapper = new StackPane(tmpNameLabel);
            labelWrapper.setStyle("-fx-background-color: " + backgroundColor + ";");
            labelWrapper.setPadding(new Insets(GuiDefinitions.GUI_SPACING_VALUE, GuiDefinitions.GUI_SPACING_VALUE, GuiDefinitions.GUI_SPACING_VALUE, GuiDefinitions.GUI_SPACING_VALUE));
            labelWrapper.setAlignment(Pos.CENTER_LEFT);
            GridPane.setHgrow(labelWrapper, Priority.ALWAYS);
            GridPane.setVgrow(labelWrapper, Priority.ALWAYS);
            aGridPane.add(labelWrapper, 0, tmpRowIndex);

            StackPane controlWrapper = new StackPane();
            controlWrapper.setStyle("-fx-background-color: " + backgroundColor + ";");
            controlWrapper.setPadding(new Insets(GuiDefinitions.GUI_SPACING_VALUE, GuiDefinitions.GUI_SPACING_VALUE, GuiDefinitions.GUI_SPACING_VALUE, GuiDefinitions.GUI_SPACING_VALUE));
            controlWrapper.setAlignment(Pos.CENTER_RIGHT);
            GridPane.setHgrow(controlWrapper, Priority.ALWAYS);
            GridPane.setVgrow(controlWrapper, Priority.ALWAYS);

            // Add the control to its wrapper
            switch (tmpProperty) {
                case SimpleBooleanProperty tmpSimpleBooleanProperty -> {
                    ToggleSwitch tmpToggle = new ToggleSwitch();
                    tmpToggle.setTooltip(tmpTooltip);
                    tmpToggle.getSwitchStateProperty().bindBidirectional(tmpSimpleBooleanProperty);
                    controlWrapper.getChildren().add(tmpToggle);
                }
                case SimpleIntegerProperty simpleIntegerProperty -> {
                    TextField tmpIntegerTextField = new TextField();
                    tmpIntegerTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
                    tmpIntegerTextField.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE);
                    int tmpDefaultValue = 0;
                    //note: setting the filter to only accept positive integers including zero is an assumption that is true
                    // for all settings so far but might have to be changed in the future
                    TextFormatter<Integer> tmpFormatter = new TextFormatter<>(GuiUtil.getStringToIntegerConverter(),
                            tmpDefaultValue,
                            GuiUtil.getPositiveIntegerFilter(true));
                    tmpIntegerTextField.setTextFormatter(tmpFormatter);
                    tmpFormatter.valueProperty().bindBidirectional(tmpProperty);
                    tmpIntegerTextField.setTooltip(tmpTooltip);
                    controlWrapper.getChildren().add(tmpIntegerTextField);
                }
                case SimpleDoubleProperty simpleDoubleProperty -> {
                    TextField tmpDoubleTextField = new TextField();
                    tmpDoubleTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
                    tmpDoubleTextField.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE);
                    double tmpDefaultValue = 0.0;
                    //note: setting the filter to only accept positive double values including zero is an assumption that is true
                    // for all settings so far but might have to be changed in the future
                    TextFormatter<Double> tmpFormatter = new TextFormatter<>(GuiUtil.getStringToDoubleConverter(),
                            tmpDefaultValue,
                            GuiUtil.getPositiveDoubleFilter());
                    tmpDoubleTextField.setTextFormatter(tmpFormatter);
                    tmpFormatter.valueProperty().bindBidirectional(tmpProperty);
                    tmpDoubleTextField.setTooltip(tmpTooltip);
                    controlWrapper.getChildren().add(tmpDoubleTextField);
                }
                case SimpleIDisplayEnumConstantProperty tmpSimpleIDisplayEnumConstantProperty -> {
                    ComboBox<IDisplayEnum> tmpEnumComboBox = new ComboBox<>();
                    tmpEnumComboBox.setPrefWidth(GuiDefinitions.GUI_SETTING_COMBO_BOX_PREF_WIDTH_VALUE);
                    tmpEnumComboBox.setMaxWidth(GuiDefinitions.GUI_SETTING_COMBO_BOX_MAX_WIDTH_VALUE);
                    final ObservableList<IDisplayEnum> tmpItems = FXCollections.observableArrayList();
                    Collections.addAll(tmpItems, (IDisplayEnum[]) tmpSimpleIDisplayEnumConstantProperty.getAssociatedEnumConstants());
                    tmpEnumComboBox.setItems(tmpItems);
                    tmpEnumComboBox.setCellFactory(param -> new ListCell<>() {
                        @Override
                        protected void updateItem(IDisplayEnum iDisplayEnum, boolean empty) {
                            super.updateItem(iDisplayEnum, empty);
                            if (!empty) {
                                this.setText(iDisplayEnum.getDisplayName());
                                this.setTooltip(GuiUtil.createTooltip(iDisplayEnum.getTooltipText()));
                            }
                        }
                    });
                    //note this is called to set the initial value, so yes, we need to overwrite both methods with the same functionality here
                    tmpEnumComboBox.setButtonCell(new ListCell<>() {
                        @Override
                        protected void updateItem(IDisplayEnum item, boolean empty) {
                            super.updateItem(item, empty);
                            if (!empty) {
                                this.setText(item.getDisplayName());
                                this.setTooltip(GuiUtil.createTooltip(item.getTooltipText()));
                            }
                        }
                    });
                    tmpEnumComboBox.valueProperty().bindBidirectional(tmpSimpleIDisplayEnumConstantProperty);
                    tmpEnumComboBox.setTooltip(tmpTooltip);
                    controlWrapper.getChildren().add(tmpEnumComboBox);
                }
                case SimpleEnumConstantNameProperty tmpSimpleEnumConstantNameProperty -> {
                    ComboBox<String> tmpEnumComboBox = new ComboBox<>();
                    tmpEnumComboBox.setPrefWidth(GuiDefinitions.GUI_SETTING_COMBO_BOX_PREF_WIDTH_VALUE);
                    tmpEnumComboBox.setMaxWidth(GuiDefinitions.GUI_SETTING_COMBO_BOX_MAX_WIDTH_VALUE);
                    tmpEnumComboBox.getItems().addAll(((SimpleEnumConstantNameProperty) tmpProperty).getAssociatedEnumConstantNames());
                    tmpEnumComboBox.valueProperty().bindBidirectional(tmpSimpleEnumConstantNameProperty);
                    tmpEnumComboBox.setTooltip(tmpTooltip);
                    controlWrapper.getChildren().add(tmpEnumComboBox);
                }
                case SimpleStringProperty simpleStringProperty -> {
                    TextField tmpStringTextField = new TextField();
                    tmpStringTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
                    tmpStringTextField.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE);
                    tmpStringTextField.textProperty().bindBidirectional(tmpProperty);
                    tmpStringTextField.setTooltip(tmpTooltip);
                    controlWrapper.getChildren().add(tmpStringTextField);
                }
                default ->
                        throw new UnsupportedOperationException("Unknown property type " + tmpProperty.getName());
            }
            controlWrapper.setAlignment(Pos.CENTER_RIGHT);
            // Add the control wrapper to column 1
            aGridPane.add(controlWrapper, 1, tmpRowIndex);
            tmpRowIndex++;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns the tab pane, holding tabs for the settings of the different fragmenters.
     *
     * @return TabPane
     */
    public TabPane getTabPane() {
        return this.tabPane;
    }
    //
    /**
     * Returns selection model, holding the active tab.
     * Used to set tab of the selected fragmenter as active tab.
     *
     * @return SelectionModel {@literal <} Tab {@literal >}
     */
    public SelectionModel<Tab> getSelectionModel() {
        return this.selectionModel;
    }
    //
    /**
     * Returns cancel button, which closes the view without saving changes.
     *
     * @return CancelButton
     */
    public Button getCancelButton() {
        return this.cancelButton;
    }
    //
    /**
     * Returns apply button, which applies changes and closes the view.
     *
     * @return ApplyButton
     */
    public Button getApplyButton() {
        return this.applyButton;
    }
    //
    /**
     * Returns default button, which sets all options of active tab to default values.
     *
     * @return DefaultButton
     */
    public Button getDefaultButton() {
        return this.defaultButton;
    }
    //</editor-fold>
}
