/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2026  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.configuration.IConfiguration;
import de.unijena.cheminf.mortar.controller.MainViewController;
import de.unijena.cheminf.mortar.controller.TabNames;
import de.unijena.cheminf.mortar.gui.controls.GridTabForTableView;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;

public class FragmentsTabView extends GridTabForTableView {

    private final MainViewController mainViewController;

    private final Button exportCsvButton;

    private final Button exportPdfButton;

    private final Button cancelExportButton;

    private final Button overviewButton;
    private
    private final Button histogramButton;

    private final Button viewButtons;

    private

    /**
     * Creates and a fragmentation result tab, which visualizes the resulting fragments of the fragmentation with given name.
     *
     * @param aFragmentationName String, unique name for fragmentation job
     * @return Tab
     */
    FragmentsTabView(
            IConfiguration aConfiguration,
            MainViewController aMainViewController,
            FragmentsDataTableView aFragmentsDataTableView,
            String aFragmentationName,
            ObservableList<MoleculeDataModel> aMoleculeDataModelList,
            SettingsContainer aSettingsContainer
    ) {
        super(Message.get("MainTabPane.fragmentsTab.title") + " - " + aFragmentationName, TabNames.FRAGMENTS.name(), aFragmentsDataTableView);
        this.mainViewController = aMainViewController;

        // make the fragmentation tab closeable and set cleanup function
        MenuItem tmpCloseAllMenuItem = new MenuItem(Message.get("MainView.tabMenuBar.closeAll.text"));
        super.setClosable(true);

        // add close all and close Tab context menu and confirmation menu
        MenuItem tmpCloseAllItem = this.createCloseAllMenuItem();
        MenuItem tmpCloseTabItem = this.createCloseTabMenuItem(this);
        aFragmentsDataTableView.setContextMenu(new ContextMenu(tmpCloseTabItem, tmpCloseAllItem));

        Button tmpExportCsvButton = GuiUtil.getButtonOfStandardSize(Message.get("MainTabPane.fragments.buttonCSV.txt"));
        tmpExportCsvButton.setTooltip(GuiUtil.createTooltip(Message.get("MainTabPane.fragments.buttonCSV.tooltip")));
        Button tmpExportPdfButton = GuiUtil.getButtonOfStandardSize(Message.get("MainTabPane.fragments.buttonPDF.txt"));
        tmpExportPdfButton.setTooltip(GuiUtil.createTooltip(Message.get("MainTabPane.fragments.buttonPDF.tooltip")));
        Button tmpCancelExportButton = GuiUtil.getButtonOfStandardSize(Message.get("MainTabPane.fragments.buttonCancelExport.txt"));
        tmpCancelExportButton.setTooltip(GuiUtil.createTooltip(Message.get("MainTabPane.fragments.buttonCancelExport.tooltip")));
        tmpCancelExportButton.visibleProperty().bind(this.mainViewController.isExportRunningProperty);
        HBox tmpExportButtonsHBox = new HBox();
        tmpExportButtonsHBox.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE));
        tmpExportButtonsHBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpExportButtonsHBox.setAlignment(Pos.CENTER_LEFT);
        tmpExportButtonsHBox.getChildren().addAll(tmpExportCsvButton, tmpExportPdfButton, tmpCancelExportButton);
        this.addNodeToGridPane(tmpExportButtonsHBox, 0, 1, 1, 1);
        HBox tmpViewButtonsHBox = new HBox();
        tmpViewButtonsHBox.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE));
        tmpViewButtonsHBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpViewButtonsHBox.setAlignment(Pos.CENTER_RIGHT);
        tmpViewButtonsHBox.setMaxWidth(GuiDefinitions.GUI_GRIDPANE_FOR_NODE_ALIGNMENT_THIRD_COL_WIDTH);
        this.overviewButton = GuiUtil.getButtonOfStandardSize(Message.get("MainView.showOverviewViewButton.text"));
        this.overviewButton.setTooltip(GuiUtil.createTooltip(Message.get("MainView.showOverviewViewButton.tooltip")));
        this.histogramButton = GuiUtil.getButtonOfStandardSize(Message.get("MainView.showHistogramViewButton.text"));
        this.histogramButton.setTooltip(GuiUtil.createTooltip(Message.get("MainView.showHistogramViewButton.tooltip")));
        tmpViewButtonsHBox.getChildren().addAll(tmpO, tmpOpenHistogramViewButton);
        this.addNodeToGridPane(tmpViewButtonsHBox, 2, 1, 1, 1);
    }


    /**
     * Creates a menu item to close all tabs upon clicking and confirming a warning message.
     * The warning message prompts the user to confirm that they want to close all fragmentation result tabs and
     * therefore is fine with deleting the fragmentation results if not exported.
     *
     * @return the menu item
     */
    private MenuItem createCloseAllMenuItem() {
    }


    /**
     * Creates a new menu item to close the tab of the displayed menu with a
     * confirmation dialogue if this would clean up the data.
     *
     * @param aGridTableView the tab to close for this menu item.
     * @return the menu item to close this specific tab.
     */
    private MenuItem createCloseTabMenuItem(GridTabForTableView aGridTableView) {
        return new MenuItem(Message.get("MainView.tabMenuBar.closeTab.text"));
    }

}
