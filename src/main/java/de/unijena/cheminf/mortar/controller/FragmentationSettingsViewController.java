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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.FragmentationSettingsView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.fragmentation.ErtlFunctionalGroupsFinderFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.IMoleculeFragmenter;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * SettingsViewController
 * controls {@link de.unijena.cheminf.mortar.gui.views.FragmentationSettingsView}
 */
public class FragmentationSettingsViewController {

    private Stage mainStage;
    private Stage fragmentationSettingsViewStage;
    private boolean isViewDisplayed = false;
    private FragmentationSettingsView fragmentationSettingsView;
    private Properties recentProperties;
    private List<Property> recentPropertiesList;


    public FragmentationSettingsViewController(Stage aStage){
        this.mainStage = aStage;
        this.openFragmentationSettingsView();
        this.recentProperties = new Properties();
    }

    private void openFragmentationSettingsView(){
        if(this.fragmentationSettingsView == null)
            this.fragmentationSettingsView = new FragmentationSettingsView();
        this.fragmentationSettingsViewStage = new Stage();
        Scene tmpScene = new Scene(this.fragmentationSettingsView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.fragmentationSettingsViewStage.setScene(tmpScene);
        this.fragmentationSettingsViewStage.initModality(Modality.WINDOW_MODAL);
        this.fragmentationSettingsViewStage.initOwner(this.mainStage);
        this.isViewDisplayed = true;
        this.fragmentationSettingsViewStage.show();
        this.fragmentationSettingsViewStage.setTitle(Message.get("FragmentationSettingsView.title"));
        this.fragmentationSettingsViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.fragmentationSettingsViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        //
        this.addListener();
        this.addTab(new ErtlFunctionalGroupsFinderFragmenter()); //TODO: remove line after gui testing
    }

    private void addTab(IMoleculeFragmenter aFragmenter){
        Tab tmpTab = new Tab();
        tmpTab.setClosable(false);
        Label tmpTabTitle = new Label(aFragmenter.getFragmentationAlgorithmName());

        StackPane tmpStackPane = new StackPane(new Group(tmpTabTitle));
        tmpTab.setGraphic(tmpStackPane);
        tmpTab.setStyle("-fx-pref-height: 150");
        tmpTab.setId(aFragmenter.getFragmentationAlgorithmName()); //TODO: Maybe enum is the better way

        GridPane tmpGridPane = new GridPane();
        tmpGridPane.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
//        tmpGridPane.setGridLinesVisible(true);
        ColumnConstraints tmpColCon1 = new ColumnConstraints();
        tmpColCon1.setHalignment(HPos.LEFT);
        tmpColCon1.setHgrow(Priority.ALWAYS);
        tmpColCon1.prefWidthProperty().bind(
                this.fragmentationSettingsViewStage.widthProperty().multiply(0.5)
        );
        tmpGridPane.getColumnConstraints().add(tmpColCon1);
        ColumnConstraints tmpColCon2 = new ColumnConstraints();
        tmpColCon2.setHalignment(HPos.RIGHT);
        tmpColCon2.setHgrow(Priority.ALWAYS);
        tmpColCon1.prefWidthProperty().bind(
                this.fragmentationSettingsViewStage.widthProperty().multiply(0.5)
        );
        tmpGridPane.getColumnConstraints().add(tmpColCon2);

//        this.addPropertyItems(aFragmenter.settingsProperties(), tmpGridPane);
        this.addPropertyItems(aFragmenter, tmpGridPane);

        tmpTab.setContent(tmpGridPane);

        this.fragmentationSettingsView.getTabPane().getTabs().add(tmpTab);



    }

//    private void addPropertyItems(List<Property> aPropertyList, GridPane aGridPane){
    private void addPropertyItems(IMoleculeFragmenter aFragmenter, GridPane aGridPane){
        int tmpRowIndex = 0;
        for(Property tmpProperty : aFragmenter.settingsProperties()){

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
            Object tmpSetValue = tmpProperty.getValue(); //TODO: Maybe change this line to getDefault() or something else
            if(tmpProperty instanceof SimpleBooleanProperty){
//                SimpleBooleanProperty tmpBooleanProperty = (SimpleBooleanProperty) tmpProperty;
                ComboBox tmpBooleanComboBox = new ComboBox();
                tmpBooleanComboBox.getItems().addAll(Boolean.FALSE, Boolean.TRUE);
                //TODO: set to default or to set value, not necessary cause of bidirectional binding
                tmpBooleanComboBox.valueProperty().bindBidirectional(tmpProperty);

                //add to gridpane
                aGridPane.add(tmpBooleanComboBox, 1, tmpRowIndex++);
                GridPane.setMargin(tmpBooleanComboBox, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            }
            else if(tmpProperty instanceof SimpleDoubleProperty){
                TextField tmpDoubleTextField = new TextField();
                tmpDoubleTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_WIDTH_VALUE);
                TextFormatter<Double> tmpFormatter = new TextFormatter<>(GuiUtil.GetStringToDoubleConverter(), 0.0, GuiUtil.GetNumericFilter());
                tmpDoubleTextField.setTextFormatter(tmpFormatter);
                tmpFormatter.valueProperty().bindBidirectional(tmpProperty);
                //add to gridpane
                aGridPane.add(tmpDoubleTextField, 1, tmpRowIndex++);
                GridPane.setMargin(tmpDoubleTextField, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            }
            else if(tmpProperty instanceof SimpleStringProperty){ //TODO: change to Enum
                ComboBox tmpEnumComboBox = new ComboBox();
                tmpEnumComboBox.getItems().addAll("Jonas", "Kohulan", "Achim", "Lisa", "AKB", "Felix");
//                tmpEnumComboBox.valueProperty().bindBidirectional(ErtlFunctionalGroupsFinderFragmenter.);
                aGridPane.add(tmpEnumComboBox, 1, tmpRowIndex++);
                GridPane.setMargin(tmpEnumComboBox, new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            }
        }
    }

    private void addListener(){
        //fragmentationSettingsViewStage close request
        this.fragmentationSettingsViewStage.setOnCloseRequest(event -> {
            //TODO set properties back to recent values but only on active/focused/selected tab
//            this.fragmentationSettingsView.getTabPane().getSelectionModel().getSelectedItem().getId();
            this.fragmentationSettingsViewStage.close();
        });
        //applyButton
        this.fragmentationSettingsView.getApplyButton().setOnAction(event -> {
            this.fragmentationSettingsViewStage.close();
        });
        //cancelButton
        this.fragmentationSettingsView.getCancelButton().setOnAction(event -> {
            //TODO set properties back to recent values but only on active/focused/selected tab
//            for(Property tmpProp : this.recentPropertiesList){
//                tmpProp
//            }
            this.fragmentationSettingsViewStage.close();
        });
        //defaultButton
        this.fragmentationSettingsView.getDefaultButton().setOnAction(event -> {
            //TODO set properties to default values but only on active/focused/selected tab
        });
    }
}
