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

import de.unijena.cheminf.mortar.controller.TabNames;
import de.unijena.cheminf.mortar.gui.controls.CustomPaginationSkin;
import de.unijena.cheminf.mortar.gui.controls.GridTabForTableView;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;

import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class FragmentsTabView extends GridTabForTableView {

    private final Pagination pagination;

    private final Button exportCsvButton;

    private final Button exportPdfButton;

    private final Button cancelExportButton;

    private final Button overviewButton;

    private final Button histogramButton;

    private final MenuItem closeTabItem;

    private final MenuItem closeAllTabsItem;

    private Runnable onExportCsv;
    private Runnable onExportPdf;
    private Runnable onCancelExport;
    private Runnable onOverview;
    private Runnable onHistogram;
    private Consumer<SortEvent<TableView>> onSort;
    private Consumer<Number> onTableWidthChanged;

    /**
     * Creates and a fragmentation result tab, which visualizes the resulting fragments of the fragmentation with given name.
     *
     * @param aFragmentsDataTableView the fragments table view
     * @param aFragmentationName String, unique name for fragmentation job
     * @param aObservableFragmentsList observable list of fragment data models
     * @param aRowsPerPageSetting rows per page setting
     */
    public FragmentsTabView(
            FragmentsDataTableView aFragmentsDataTableView,
            String aFragmentationName,
            ObservableList<MoleculeDataModel> aObservableFragmentsList,
            int aRowsPerPageSetting
    ) {
        super(Message.get(
                        "MainTabPane.fragmentsTab.title") + " - " + aFragmentationName,
                TabNames.FRAGMENTS.name(),
                aFragmentsDataTableView
        );
        this.setClosable(true);
        this.closeTabItem = new MenuItem(Message.get("MainView.tabMenuBar.closeTab.text"));
        this.closeAllTabsItem = new MenuItem(Message.get("MainView.tabMenuBar.closeAll.text"));
        aFragmentsDataTableView.setContextMenu(new ContextMenu(this.closeTabItem, this.closeAllTabsItem));
        for (MoleculeDataModel tmpMoleculeDataModel : aObservableFragmentsList) {
            tmpMoleculeDataModel.setStructureImageWidth(aFragmentsDataTableView.getStructureColumn().getWidth());
        }
        aFragmentsDataTableView.setItemsList(aObservableFragmentsList);
        this.pagination = this.createPaginationWithSuitablePageCount(
                aObservableFragmentsList.size(),
                aRowsPerPageSetting
        );
        super.addPaginationToGridPane(this.pagination);
        this.exportCsvButton = GuiUtil.getButtonOfStandardSize(Message.get("MainTabPane.fragments.buttonCSV.txt"));
        this.exportCsvButton.setTooltip(GuiUtil.createTooltip(Message.get("MainTabPane.fragments.buttonCSV.tooltip")));
        this.exportPdfButton = GuiUtil.getButtonOfStandardSize(Message.get("MainTabPane.fragments.buttonPDF.txt"));
        this.exportPdfButton.setTooltip(GuiUtil.createTooltip(Message.get("MainTabPane.fragments.buttonPDF.tooltip")));
        this.cancelExportButton = GuiUtil.getButtonOfStandardSize(Message.get("MainTabPane.fragments.buttonCancelExport.txt"));
        this.cancelExportButton.setTooltip(GuiUtil.createTooltip(Message.get("MainTabPane.fragments.buttonCancelExport.tooltip")));
        HBox tmpExportButtonsHBox = this.createExportButtonsBox();
        this.addNodeToGridPane(tmpExportButtonsHBox, 0, 1, 1, 1);
        this.overviewButton = GuiUtil.getButtonOfStandardSize(Message.get("MainView.showOverviewViewButton.text"));
        this.overviewButton.setTooltip(GuiUtil.createTooltip(Message.get("MainView.showOverviewViewButton.tooltip")));
        this.histogramButton = GuiUtil.getButtonOfStandardSize(Message.get("MainView.showHistogramViewButton.text"));
        this.histogramButton.setTooltip(GuiUtil.createTooltip(Message.get("MainView.showHistogramViewButton.tooltip")));
        if (aObservableFragmentsList.isEmpty()) {
            this.overviewButton.setDisable(true);
            this.histogramButton.setDisable(true);
        }
        HBox tmpViewButtonsHBox = this.createViewButtonsBox();
        this.addNodeToGridPane(tmpViewButtonsHBox, 2, 1, 1, 1);
        aFragmentsDataTableView.widthProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (this.onTableWidthChanged != null) {
                        this.onTableWidthChanged.accept(newValue);
                    }
                }
        );
    }

    private HBox createExportButtonsBox() {
        HBox tmpBox = new HBox();
        tmpBox.setPadding(new Insets(
                GuiDefinitions.GUI_INSETS_VALUE,
                GuiDefinitions.GUI_INSETS_VALUE,
                GuiDefinitions.GUI_INSETS_VALUE,
                GuiDefinitions.GUI_INSETS_VALUE
        ));
        tmpBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpBox.setAlignment(Pos.CENTER_LEFT);
        tmpBox.getChildren().addAll(this.exportCsvButton, this.exportPdfButton, this.cancelExportButton);
        return tmpBox;
    }

    private HBox createViewButtonsBox() {
        HBox tmpBox = new HBox();
        tmpBox.setPadding(new Insets(
                GuiDefinitions.GUI_INSETS_VALUE,
                GuiDefinitions.GUI_INSETS_VALUE,
                GuiDefinitions.GUI_INSETS_VALUE,
                GuiDefinitions.GUI_INSETS_VALUE
        ));
        tmpBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpBox.setAlignment(Pos.CENTER_RIGHT);
        tmpBox.setMaxWidth(GuiDefinitions.GUI_GRIDPANE_FOR_NODE_ALIGNMENT_THIRD_COL_WIDTH);
        tmpBox.getChildren().addAll(this.overviewButton, this.histogramButton);
        return tmpBox;
    }

    @Override
    public Pagination getPagination() {
        return this.pagination;
    }

    public Button getExportCsvButton() {
        return this.exportCsvButton;
    }

    public Button getExportPdfButton() {
        return this.exportPdfButton;
    }

    public Button getCancelExportButton() {
        return this.cancelExportButton;
    }

    public Button getOverviewButton() {
        return this.overviewButton;
    }

    public Button getHistogramButton() {
        return this.histogramButton;
    }

    public MenuItem getCloseTabItem() {
        return this.closeTabItem;
    }

    public MenuItem getCloseAllTabsItem() {
        return this.closeAllTabsItem;
    }

    public void setOnExportCsv(Runnable aCallback) {
        this.onExportCsv = aCallback;
        this.exportCsvButton.setOnAction(event -> aCallback.run());
    }

    public void setOnExportPdf(Runnable aCallback) {
        this.onExportPdf = aCallback;
        this.exportPdfButton.setOnAction(event -> aCallback.run());
    }

    public void setOnCancelExport(Runnable aCallback) {
        this.onCancelExport = aCallback;
        this.cancelExportButton.setOnAction(event -> aCallback.run());
    }

    public void setOnOverview(Runnable aCallback) {
        this.onOverview = aCallback;
        this.overviewButton.setOnAction(event -> aCallback.run());
    }

    public void setOnHistogram(Runnable aCallback) {
        this.onHistogram = aCallback;
        this.histogramButton.setOnAction(event -> aCallback.run());
    }

    public void setOnSort(Consumer<SortEvent<TableView>> aCallback) {
        this.onSort = aCallback;
    }

    public void setOnTableWidthChanged(Consumer<Number> aCallback) {
        this.onTableWidthChanged = aCallback;
    }

    public void bindCancelButtonVisibility(BooleanProperty aProperty) {
        this.cancelExportButton.visibleProperty().bind(aProperty);
    }

    /**
     * Creates a new JavaFx pagination control that is configured with a suitable page count for the given number
     * of molecules/fragments taking into account the rows per page setting. Also sets the MORTAR custom pagination skin
     * as skin of the new pagination instance and configures its growth behavior. The page factory is *NOT* set.
     *
     * @param aListSize number of molecules/fragments to display
     * @param aRowsPerPageSetting rows per page setting
     * @return configured pagination control instance
     */
    private Pagination createPaginationWithSuitablePageCount(int aListSize, int aRowsPerPageSetting) {
        int tmpPageCount = aListSize / aRowsPerPageSetting;
        if (aListSize % aRowsPerPageSetting > 0) {
            tmpPageCount++;
        }
        if (aListSize == 0) {
            tmpPageCount = 1;
        }
        Pagination tmpPagination = new Pagination(tmpPageCount, 0);
        tmpPagination.setSkin(new CustomPaginationSkin(tmpPagination));
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        return tmpPagination;
    }
}
