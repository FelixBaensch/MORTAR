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
import de.unijena.cheminf.mortar.message.Message;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class FragmentationSettingsView extends AnchorPane {

    private TabPane tabPane;
    private BorderPane borderPane;
    private Button cancelButton;
    private Button applyButton;
    private Button defaultButton;
    private HBox hBoxRightSideButtons;
    private HBox hBoxLeftSideButtons;
    private HBox hBoxButtonsHBox;
    private SelectionModel<Tab> selectionModel;


    public FragmentationSettingsView(){
        super();
        //borderPane
        this.borderPane = new BorderPane();
        FragmentationSettingsView.setTopAnchor(this.borderPane, 0.0);
        FragmentationSettingsView.setRightAnchor(this.borderPane, 0.0);
        FragmentationSettingsView.setLeftAnchor(this.borderPane, 0.0);
        FragmentationSettingsView.setBottomAnchor(this.borderPane, 0.0);

        //tabPane
        this.tabPane =  new TabPane();
        this.selectionModel = this.tabPane.getSelectionModel();
        this.tabPane.setSide(Side.LEFT);
//        FragmentationSettingsView.setTopAnchor(this.tabPane, 0.0);
//        FragmentationSettingsView.setRightAnchor(this.tabPane, 0.0);
//        FragmentationSettingsView.setLeftAnchor(this.tabPane, 0.0);
//        FragmentationSettingsView.setBottomAnchor(this.tabPane, 0.0);
        this.borderPane.setCenter(this.tabPane);
        //buttons
        this.hBoxButtonsHBox = new HBox();
        this.hBoxButtonsHBox.setStyle("-fx-background-color: LightGrey");
        this.borderPane.setBottom(hBoxButtonsHBox);
        //-left side
        this.defaultButton = new Button(Message.get("FragmentationSettingsView.defaultButton.text"));
        this.hBoxLeftSideButtons = new HBox();
        this.hBoxLeftSideButtons.getChildren().add(this.defaultButton);
        this.hBoxLeftSideButtons.setAlignment(Pos.CENTER_LEFT);
        this.hBoxLeftSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        this.hBoxLeftSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(this.hBoxLeftSideButtons, Priority.ALWAYS);
        this.hBoxButtonsHBox.getChildren().add(this.hBoxLeftSideButtons);
        //-right side
        this.hBoxRightSideButtons = new HBox();
        this.cancelButton = new Button(Message.get("FragmentationSettingsView.cancelButton.text"));
        this.applyButton = new Button(Message.get("FragmentationSettingsView.applyButton.text"));
        this.hBoxRightSideButtons.getChildren().addAll(this.applyButton, this.cancelButton);
        this.hBoxRightSideButtons.setAlignment(Pos.CENTER_RIGHT);
        this.hBoxRightSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        this.hBoxRightSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(this.hBoxRightSideButtons, Priority.ALWAYS);
        this.hBoxButtonsHBox.getChildren().add(this.hBoxRightSideButtons);
        //
        this.getChildren().add(this.borderPane);
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
