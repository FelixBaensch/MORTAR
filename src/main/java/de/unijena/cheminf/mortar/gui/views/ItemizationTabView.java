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

public class ItemizationTabView extends GridTabForTableView {

    private final Pagination pagination;
    private final Button exportCsvButton;
    private final Button exportPdfButton;
    private final Button cancelExportButton;
    private final Button openHistogramViewButton;
    private final MenuItem closeTabItem;
    private final MenuItem closeAllTabsItem;

    // Callbacks for controller to attached listeners
    private Runnable onExportCsv;
    private Runnable onExportPdf;
    private Runnable onCancelExport;
    private Runnable onOpenHistogramView;
    private Consumer<SortEvent<TableView>> onSort;
    private Consumer<Number> onTableWidthChanged;

    public ItemizationTabView(
            ItemizationDataTableView aItemizationDataTableView,
            String aFragmentationName,
            ObservableList<MoleculeDataModel> aObservableItemsList,
            int aRowsPerPageSetting
    ) {
        super(
                Message.get("MainTabPane.itemizationTab.title") + " - " + aFragmentationName,
                TabNames.ITEMIZATION.name(),
                aItemizationDataTableView
        );
        this.setClosable(true);
        this.closeTabItem = new MenuItem(Message.get("MainView.tabMenuBar.closeTab.text"));
        this.closeAllTabsItem = new MenuItem(Message.get("MainView.tabMenuBar.closeAll.text"));
        aItemizationDataTableView.setContextMenu(
                new ContextMenu(this.closeTabItem, this.closeAllTabsItem)
        );
        for (MoleculeDataModel tmpModel : aObservableItemsList) {
            tmpModel.setStructureImageWidth(aItemizationDataTableView.getMoleculeStructureColumn().getWidth());
        }
        aItemizationDataTableView.setItemsList(aObservableItemsList);
        this.pagination = this.createPaginationWithSuitablePageCount(
                aObservableItemsList.size(),
                aRowsPerPageSetting
        );
        this.addPaginationToGridPane(this.pagination);
        this.exportCsvButton = GuiUtil.getButtonOfStandardSize(
                Message.get("MainTabPane.itemizationTab.csvButton.txt")
        );
        this.exportCsvButton.setTooltip(
                GuiUtil.createTooltip(Message.get("MainTabPane.itemizationTab.csvButton.tooltip"))
        );
        this.exportPdfButton = GuiUtil.getButtonOfStandardSize(
                Message.get("MainTabPane.itemizationTab.pdfButton.txt")
        );
        this.exportPdfButton.setTooltip(
                GuiUtil.createTooltip(Message.get("MainTabPane.itemizationTab.pdfButton.tooltip"))
        );
        this.cancelExportButton = GuiUtil.getButtonOfStandardSize(
                Message.get("MainTabPane.fragments.buttonCancelExport.txt")
        );
        this.cancelExportButton.setTooltip(
                GuiUtil.createTooltip(Message.get("MainTabPane.fragments.buttonCancelExport.tooltip"))
        );
        HBox tmpExportButtonsHBox = this.createExportButtonsBox();
        this.addNodeToGridPane(tmpExportButtonsHBox, 0, 1, 1, 1);
        this.openHistogramViewButton = GuiUtil.getButtonOfStandardSize(
                Message.get("MainView.showHistogramViewButton.text")
        );
        this.openHistogramViewButton.setTooltip(
                GuiUtil.createTooltip(Message.get("MainView.showHistogramViewButton.tooltip"))
        );
        if (aObservableItemsList.isEmpty()) {
            this.openHistogramViewButton.setDisable(true);
        }
        HBox tmpViewButtonsHBox = this.createViewButtonsBox();
        this.addNodeToGridPane(tmpViewButtonsHBox, 2, 1, 1, 1);
        aItemizationDataTableView.widthProperty().addListener(
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
        tmpBox.getChildren().addAll(
                this.exportCsvButton,
                this.exportPdfButton,
                this.cancelExportButton
        );
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
        tmpBox.getChildren().add(this.openHistogramViewButton);
        return tmpBox;
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

    // Getters for controller to attach listeners
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

    public Button getOpenHistogramViewButton() {
        return this.openHistogramViewButton;
    }

    public MenuItem getCloseTabItem() {
        return this.closeTabItem;
    }

    public MenuItem getCloseAllTabsItem() {
        return this.closeAllTabsItem;
    }

    // Callback setters for controller
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

    public void setOnOpenHistogramView(Runnable aCallback) {
        this.onOpenHistogramView = aCallback;
        this.openHistogramViewButton.setOnAction(event -> aCallback.run());
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
}
