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

/**
 * A tab view for displaying fragmentation results with pagination, export controls, and data visualization options.
 *
 * <p>This class extends {@link GridTabForTableView} and provides a user interface for viewing the resulting fragments
 * from a molecular fragmentation operation. It includes a paginated table display with buttons for exporting results
 * to CSV or PDF formats, canceling exports, and viewing overview and histogram visualizations of the fragment data.
 *
 * <p>The view supports the following features:
 * <ul>
 *     <li>Paginated display of fragment data with configurable rows per page</li>
 *     <li>CSV and PDF export buttons for exporting fragment results</li>
 *     <li>Cancel export button for interrupting ongoing export operations</li>
 *     <li>Overview button to display all fragments in a separate window</li>
 *     <li>Histogram button to display distribution data visualization</li>
 *     <li>Context menu items for tab management (close tab, close all tabs)</li>
 *     <li>Event callbacks for export, overview, histogram, sorting, and table width changes</li>
 *     <li>Binding of cancel button visibility to external properties</li>
 * </ul>
 *
 * <p>This tab is closable and created for each unique fragmentation job, with the tab title including
 * the fragmentation name for identification.
 *
 * @author [Your Name/Organization]
 * @version 1.0
 * @see GridTabForTableView
 * @see FragmentsDataTableView
 * @see Pagination
 */
public class FragmentsTabView extends GridTabForTableView {

    /**
     * The pagination component used to navigate through pages of fragments.
     */
    private final Pagination pagination;

    /**
     * The button to export fragmentation results in CSV format.
     */
    private final Button exportCsvButton;

    /**
     * The button to export fragmentation results in PDF format.
     */
    private final Button exportPdfButton;

    /**
     * The button to cancel an ongoing export operation.
     * Initially hidden, becomes visible when an export is in progress.
     */
    private final Button cancelExportButton;

    /**
     * The button to open a new window with the overview of all fragments in this tab.
     */
    private final Button overviewButton;

    /**
     * The button to open a new window with the histogram visualization of fragment data distribution.
     */
    private final Button histogramButton;

    /**
     * The context menu item to close the current fragments tab.
     */
    private final MenuItem closeTabItem;

    /**
     * The context menu item to close all open fragments tabs.
     */
    private final MenuItem closeAllTabsItem;

    /**
     * The callback function that runs when the CSV export button is pressed.
     */
    private Runnable onExportCsv;

    /**
     * The callback function that runs when the PDF export button is pressed.
     */
    private Runnable onExportPdf;

    /**
     * The callback function that runs when the cancel export button is pressed.
     */
    private Runnable onCancelExport;

    /**
     * The callback function that runs when the overview button is pressed.
     */
    private Runnable onOverview;

    /**
     * The callback function that runs when the histogram button is pressed.
     */
    private Runnable onHistogram;

    /**
     * The callback function that runs when the table view sort order changes.
     */
    private Consumer<SortEvent<TableView>> onSort;

    /**
     * The callback function that runs when the table view width changes.
     */
    private Consumer<Number> onTableWidthChanged;

    /**
     * Creates a fragmentation result tab that visualizes the resulting fragments from a fragmentation operation.
     *
     * <p>This constructor initializes the tab with the provided fragments data table, sets up pagination
     * based on the rows per page setting, and creates control buttons for exporting and visualizing the
     * fragmentation results. The tab title will include the fragmentation name for identification. A context menu
     * is added for tab management (close tab, close all tabs). The overview and histogram buttons are disabled
     * if the fragment list is empty.
     *
     * <p>Structure image widths in the provided fragments are configured to match the structure column width.
     *
     * @param aFragmentsDataTableView the fragments data table view to be displayed in this tab;
     *                                must not be null
     * @param aFragmentationName a unique string identifier for the fragmentation job; used in the tab title;
     *                           must not be null or empty
     * @param aObservableFragmentsList an observable list of fragment data models (MoleculeDataModel instances)
     *                                 to be displayed; must not be null
     * @param aRowsPerPageSetting the number of rows to display per page for pagination; must be greater than 0
     *
     * @throws IllegalArgumentException if any required parameter is null, if aFragmentationName is empty,
     *                                  or if aRowsPerPageSetting is &lt;= 0
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

    /**
     * Creates the export buttons box containing CSV export, PDF export, and cancel export buttons.
     *
     * @return an HBox containing the export buttons arranged horizontally with standard spacing and padding
     */
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

    /**
     * Creates the view buttons box containing overview and histogram buttons.
     *
     * @return an HBox containing the view buttons arranged horizontally with standard spacing and padding,
     *         aligned to the right
     */
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

    /**
     * Retrieves the pagination component used for navigating through fragment pages.
     *
     * @return the {@link Pagination} component; never null
     */
    @Override
    public Pagination getPagination() {
        return this.pagination;
    }

    /**
     * Retrieves the button that initiates CSV export of the fragmentation results.
     *
     * @return the CSV export {@link Button}; never null
     */
    public Button getExportCsvButton() {
        return this.exportCsvButton;
    }

    /**
     * Retrieves the button that initiates PDF export of the fragmentation results.
     *
     * @return the PDF export {@link Button}; never null
     */
    public Button getExportPdfButton() {
        return this.exportPdfButton;
    }

    /**
     * Retrieves the button that cancels an ongoing export operation.
     *
     * <p>This button is initially hidden and becomes visible only when an export operation is in progress.
     *
     * @return the cancel export {@link Button}; never null
     */
    public Button getCancelExportButton() {
        return this.cancelExportButton;
    }

    /**
     * Retrieves the button that opens an overview window displaying all fragments in this tab.
     *
     * <p>This button is disabled if the fragment list is empty at the time of tab creation.
     *
     * @return the overview {@link Button}; never null
     */
    public Button getOverviewButton() {
        return this.overviewButton;
    }

    /**
     * Retrieves the button that opens a histogram window displaying fragment data distribution.
     *
     * <p>This button is disabled if the fragment list is empty at the time of tab creation.
     *
     * @return the histogram {@link Button}; never null
     */
    public Button getHistogramButton() {
        return this.histogramButton;
    }

    /**
     * Retrieves the context menu item for closing the current fragments tab.
     *
     * @return the close tab {@link MenuItem}; never null
     */
    public MenuItem getCloseTabItem() {
        return this.closeTabItem;
    }

    /**
     * Retrieves the context menu item for closing all open fragments tabs.
     *
     * @return the close all tabs {@link MenuItem}; never null
     */
    public MenuItem getCloseAllTabsItem() {
        return this.closeAllTabsItem;
    }

    /**
     * Sets the callback function to be executed when the CSV export button is pressed.
     *
     * @param aCallback the callback to execute on CSV export button action; must not be null
     *
     * @throws IllegalArgumentException if aCallback is null
     *
     * @see #setOnExportPdf(Runnable)
     * @see #setOnCancelExport(Runnable)
     */
    public void setOnExportCsv(Runnable aCallback) {
        this.onExportCsv = aCallback;
        this.exportCsvButton.setOnAction(event -> aCallback.run());
    }

    /**
     * Sets the callback function to be executed when the PDF export button is pressed.
     *
     * @param aCallback the callback to execute on PDF export button action; must not be null
     *
     * @throws IllegalArgumentException if aCallback is null
     *
     * @see #setOnExportCsv(Runnable)
     * @see #setOnCancelExport(Runnable)
     */
    public void setOnExportPdf(Runnable aCallback) {
        this.onExportPdf = aCallback;
        this.exportPdfButton.setOnAction(event -> aCallback.run());
    }

    /**
     * Sets the callback function to be executed when the cancel export button is pressed.
     *
     * @param aCallback the callback to execute on cancel export button action; must not be null
     *
     * @throws IllegalArgumentException if aCallback is null
     *
     * @see #setOnExportCsv(Runnable)
     * @see #setOnExportPdf(Runnable)
     */
    public void setOnCancelExport(Runnable aCallback) {
        this.onCancelExport = aCallback;
        this.cancelExportButton.setOnAction(event -> aCallback.run());
    }

    /**
     * Sets the callback function to be executed when the overview button is pressed.
     *
     * @param aCallback the callback to execute on overview button action; must not be null
     *
     * @throws IllegalArgumentException if aCallback is null
     *
     * @see #setOnHistogram(Runnable)
     */
    public void setOnOverview(Runnable aCallback) {
        this.onOverview = aCallback;
        this.overviewButton.setOnAction(event -> aCallback.run());
    }

    /**
     * Sets the callback function to be executed when the histogram button is pressed.
     *
     * @param aCallback the callback to execute on histogram button action; must not be null
     *
     * @throws IllegalArgumentException if aCallback is null
     *
     * @see #setOnOverview(Runnable)
     */
    public void setOnHistogram(Runnable aCallback) {
        this.onHistogram = aCallback;
        this.histogramButton.setOnAction(event -> aCallback.run());
    }

    /**
     * Sets the callback function to be executed when the table view sort order changes.
     *
     * @param aCallback the callback to execute on table sort event; receives the sort event with the table view;
     *                  must not be null
     *
     * @throws IllegalArgumentException if aCallback is null
     */
    public void setOnSort(Consumer<SortEvent<TableView>> aCallback) {
        this.onSort = aCallback;
    }

    /**
     * Sets the callback function to be executed when the table view width changes.
     *
     * <p>The callback will be invoked whenever the width of the underlying fragments data table view changes,
     * @param aCallback the function to call if the table width changed.
     */
     public void setOnTableWidthChanged(Consumer<Number> aCallback) {
        this.onTableWidthChanged = aCallback;
    }

    /**
     * Bind a boolean property to control the visibility of the {@code cancel} fragmentation button.
     *
     * @param aProperty the property that will control the visibility.
     */
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
