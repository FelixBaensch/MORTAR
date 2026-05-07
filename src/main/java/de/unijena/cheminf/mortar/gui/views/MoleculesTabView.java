package de.unijena.cheminf.mortar.gui.views;

import de.unijena.cheminf.mortar.controller.TabNames;
import de.unijena.cheminf.mortar.gui.controls.CustomPaginationSkin;
import de.unijena.cheminf.mortar.gui.controls.GridTabForTableView;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Pagination;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.function.Consumer;

public class MoleculesTabView extends GridTabForTableView {
    //</editor-fold>
    //
    //<editor-fold desc="private final instance variables">
    private final Pagination pagination;
    //</editor-fold>
    //
    //<editor-fold desc="private variables">
    private Button fragmentationButton;
    private Button cancelFragmentationButton;
    private Button overviewButton;
    private Runnable onFragmentation;
    private Runnable onCancelFragmentation;
    private Runnable onOverview;
    private Consumer<SortEvent<TableView>> onSort;
    private Consumer<Number> onTableWidthChanged;
    //</editor-fold>
    //
    //<editor-fold desc="constructor">
    /**
     * Creates a molecules tab that displays all molecules.
     *
     * @param aMoleculesDataTableView the molecules data table view
     * @param aMoleculeDataModelList observable list of molecule data models
     * @param aRowsPerPageSetting rows per page setting
     * @param aFragmentationServiceSelectedFragmenterDisplayNameProperty binding for fragmentation button text
     */
    public MoleculesTabView(
            MoleculesDataTableView aMoleculesDataTableView,
            ObservableList<MoleculeDataModel> aMoleculeDataModelList,
            int aRowsPerPageSetting,
            StringProperty aFragmentationServiceSelectedFragmenterDisplayNameProperty
    ) {
        super(
                Message.get("MainTabPane.moleculesTab.title"),
                TabNames.MOLECULES.name(),
                aMoleculesDataTableView
        );
        aMoleculesDataTableView.setItemsList(aMoleculeDataModelList);
        this.pagination = this.createPaginationWithSuitablePageCount(
                aMoleculeDataModelList.size(),
                aRowsPerPageSetting
        );
        this.addPaginationToGridPane(this.pagination);
        HBox tmpFragmentationButtonsHBox = this.createFragmentationButtonsBox(
                aFragmentationServiceSelectedFragmenterDisplayNameProperty
        );
        this.addNodeToGridPane(tmpFragmentationButtonsHBox, 0, 1, 1, 1);
        HBox tmpViewButtonsHBox = this.createViewButtonsBox();
        this.addNodeToGridPane(tmpViewButtonsHBox, 2, 1, 1, 1);
        aMoleculesDataTableView.widthProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (this.onTableWidthChanged != null) {
                        this.onTableWidthChanged.accept(newValue);
                    }
                }
        );
    }
    //</editor-fold>
    //
    //<editor-fold desc="private helper methods">
    private HBox createFragmentationButtonsBox(StringProperty aFragmentationDisplayNameProperty) {
        HBox tmpBox = new HBox();
        tmpBox.setPadding(new Insets(
                GuiDefinitions.GUI_INSETS_VALUE,
                GuiDefinitions.GUI_INSETS_VALUE,
                GuiDefinitions.GUI_INSETS_VALUE,
                GuiDefinitions.GUI_INSETS_VALUE
        ));
        tmpBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpBox.setAlignment(Pos.CENTER_LEFT);
        this.fragmentationButton = new Button();
        this.fragmentationButton.textProperty().bind(aFragmentationDisplayNameProperty);
        Tooltip tmpTooltip = GuiUtil.createTooltip("");
        tmpTooltip.textProperty().bind(Bindings.format(
                Message.get("MainTabPane.moleculesTab.fragmentButton.text"),
                aFragmentationDisplayNameProperty
        ));
        this.fragmentationButton.setTooltip(tmpTooltip);
        this.updateFragmentationButtonSize(aFragmentationDisplayNameProperty.get());
        aFragmentationDisplayNameProperty.addListener((observable, oldValue, newValue) ->
                this.updateFragmentationButtonSize(newValue)
        );
        this.cancelFragmentationButton = GuiUtil.getButtonOfStandardSize(
                Message.get("MainTabPane.moleculesTab.cancelFragmentationButton.text")
        );
        this.cancelFragmentationButton.setTooltip(GuiUtil.createTooltip(
                Message.get("MainTabPane.moleculesTab.cancelFragmentationButton.tooltip")
        ));
        this.cancelFragmentationButton.setVisible(false);
        tmpBox.getChildren().addAll(this.fragmentationButton, this.cancelFragmentationButton);
        return tmpBox;
    }

    private void updateFragmentationButtonSize(String aFragmenterDisplayName) {
        double tmpTextWidth = new Text(aFragmenterDisplayName).getLayoutBounds().getWidth() + 20;
        this.fragmentationButton.setPrefWidth(tmpTextWidth);
        this.fragmentationButton.setMinWidth(tmpTextWidth);
        this.fragmentationButton.setMaxWidth(tmpTextWidth);
        this.fragmentationButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
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
        this.overviewButton = GuiUtil.getButtonOfStandardSize(
                Message.get("MainView.showOverviewViewButton.text")
        );
        this.overviewButton.setTooltip(GuiUtil.createTooltip(
                Message.get("MainView.showOverviewViewButton.tooltip")
        ));
        tmpBox.getChildren().add(this.overviewButton);
        return tmpBox;
    }

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

    //</editor-fold>
    //
    //<editor-fold desc="public getters">
    public Pagination getPagination() {
        return this.pagination;
    }

    public Button getFragmentationButton() {
        return this.fragmentationButton;
    }

    public Button getCancelFragmentationButton() {
        return this.cancelFragmentationButton;
    }

    public Button getOverviewButton() {
        return this.overviewButton;
    }
    //</editor-fold>
    //
    //<editor-fold desc="public callback setters">
    public void setOnFragmentation(Runnable aCallback) {
        this.onFragmentation = aCallback;
        this.fragmentationButton.setOnAction(event -> aCallback.run());
    }

    public void setOnCancelFragmentation(Runnable aCallback) {
        this.onCancelFragmentation = aCallback;
        this.cancelFragmentationButton.setOnAction(event -> aCallback.run());
    }

    public void setOnOverview(Runnable aCallback) {
        this.onOverview = aCallback;
        this.overviewButton.setOnAction(event -> aCallback.run());
    }

    public void setOnSort(Consumer<SortEvent<TableView>> aCallback) {
        this.onSort = aCallback;
    }

    public void setOnTableWidthChanged(Consumer<Number> aCallback) {
        this.onTableWidthChanged = aCallback;
    }

    public void bindCancelFragmentationButtonVisibility(BooleanProperty aProperty) {
        this.cancelFragmentationButton.visibleProperty().bind(aProperty);
    }
    //</editor-fold>
}
