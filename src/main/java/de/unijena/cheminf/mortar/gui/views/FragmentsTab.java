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
import de.unijena.cheminf.mortar.controller.OverviewViewController;
import de.unijena.cheminf.mortar.controller.TabNames;
import de.unijena.cheminf.mortar.gui.controls.GridTabForTableView;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.io.Exporter;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.layout.HBox;

public class FragmentsTab extends GridTabForTableView {

    private final MainViewController mainViewController;

    private static final Button exportCsvButton;

    public static final Button exportPdfButton;


    private static final Button viewButtons;

    private

    /**
     * Creates and a fragmentation result tab, which visualizes the resulting fragments of the fragmentation with given name.
     *
     * @param aFragmentationName String, unique name for fragmentation job
     * @return Tab
     */
    public FragmentsTab(
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
        super.setOnCloseRequest(tmpEvent -> this.mainViewController.closeTabWithEvent(tmpEvent, this));
        super.setClosable(true);

        // add close all and close Tab context menu and confirmation menu
        MenuItem tmpCloseAllItem = this.createCloseAllMenuItem();
        MenuItem tmpCloseTabItem = this.createCloseTabMenuItem(this);
        aFragmentsDataTableView.setContextMenu(new ContextMenu(tmpCloseTabItem, tmpCloseAllItem));

        for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
            tmpMoleculeDataModel.setStructureImageWidth(aFragmentsDataTableView.getStructureColumn().getWidth());
        }
        aFragmentsDataTableView.setItemsList(aMoleculeDataModelList);
        Pagination tmpPagination = this.mainViewController.createPaginationWithSuitablePageCount(aMoleculeDataModelList.size());
        tmpPagination.setPageFactory(pageIndex -> aFragmentsDataTableView.createFragmentsTableViewPage(pageIndex, aSettingsContainer));
        this.addPaginationToGridPane(tmpPagination);
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
        tmpExportPdfButton.setOnAction(event -> this.mainViewController.exportFile(Exporter.ExportTypes.FRAGMENT_PDF_FILE));
        tmpExportCsvButton.setOnAction(event -> this.mainViewController.exportFile(Exporter.ExportTypes.FRAGMENT_CSV_FILE));
        tmpCancelExportButton.setOnAction(event -> this.mainViewController.interruptExport());
        HBox tmpViewButtonsHBox = new HBox();
        tmpViewButtonsHBox.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE));
        tmpViewButtonsHBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpViewButtonsHBox.setAlignment(Pos.CENTER_RIGHT);
        tmpViewButtonsHBox.setMaxWidth(GuiDefinitions.GUI_GRIDPANE_FOR_NODE_ALIGNMENT_THIRD_COL_WIDTH);
        Button tmpOpenOverviewViewButton = GuiUtil.getButtonOfStandardSize(Message.get("MainView.showOverviewViewButton.text"));
        tmpOpenOverviewViewButton.setTooltip(GuiUtil.createTooltip(Message.get("MainView.showOverviewViewButton.tooltip")));
        Button tmpOpenHistogramViewButton = GuiUtil.getButtonOfStandardSize(Message.get("MainView.showHistogramViewButton.text"));
        tmpOpenHistogramViewButton.setTooltip(GuiUtil.createTooltip(Message.get("MainView.showHistogramViewButton.tooltip")));
        tmpViewButtonsHBox.getChildren().addAll(tmpOpenOverviewViewButton, tmpOpenHistogramViewButton);
        this.addNodeToGridPane(tmpViewButtonsHBox, 2, 1, 1, 1);
        tmpOpenOverviewViewButton.setOnAction(event -> this.mainViewController.openOverviewView(OverviewViewController.DataSources.FRAGMENTS_TAB));
        tmpOpenHistogramViewButton.setOnAction(event -> this.mainViewController.openHistogramView());
    }


    /**
     * Creates a menu item to close all tabs upon clicking and confirming a warning message.
     * The warning message prompts the user to confirm that they want to close all fragmentation result tabs and
     * therefore is fine with deleting the fragmentation results if not exported.
     *
     * @return the menu item
     */
    private MenuItem createCloseAllMenuItem() {
        return new MenuItem(Message.get("MainView.tabMenuBar.closeAll.text"));
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
