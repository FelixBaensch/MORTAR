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

package de.unijena.cheminf.mortar.gui;

import de.unijena.cheminf.mortar.message.Message;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class MainToolBar extends ToolBar {

    private Button startFragmentationButton;
    private Label fragmentationProgressLabel;

    public MainToolBar(){
        super();
        setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
//        setStyle("-fx-background-color: LIGHTGREY");

        this.startFragmentationButton = new Button(Message.get("MainView.toolBar.startFragmentationButton.text"));
        this.startFragmentationButton.setMinSize(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE, GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.startFragmentationButton.setMaxHeight(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);

        Pane tmpSpacerPane = new Pane();
        HBox.setHgrow(tmpSpacerPane, Priority.ALWAYS);

        this.fragmentationProgressLabel = new Label("Hier koennte Ihre Werbung stehen.");
        this.fragmentationProgressLabel.setMinHeight(GuiDefinitions.GUI_PROGRESSBAR_HEIGHT_VALUE);
        this.fragmentationProgressLabel.setMaxHeight(GuiDefinitions.GUI_PROGRESSBAR_HEIGHT_VALUE);

        getItems().addAll(this.startFragmentationButton, tmpSpacerPane, this.fragmentationProgressLabel);
    }
}
