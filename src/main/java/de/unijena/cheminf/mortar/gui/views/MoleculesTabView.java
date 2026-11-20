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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.util.function.Consumer;

/**
 * A tab view for displaying molecules with pagination, fragmentation controls, and overview functionality.
 *
 * <p>This class extends {@link GridTabForTableView} and provides a user interface for viewing a list of
 * molecules in a paginated table format. It includes buttons for initiating fragmentation operations,
 * canceling ongoing fragmentations, and opening an overview window of all molecules in the tab.
 *
 * <p>The view supports the following features:
 * <ul>
 *     <li>Paginated display of molecule data with configurable rows per page</li>
 *     <li>Fragmentation and cancel fragmentation buttons with dynamic sizing based on fragmentation algorithm name</li>
 *     <li>Overview button to display all molecules in a separate window</li>
 *     <li>Event callbacks for fragmentation, cancellation, overview, sorting, and table width changes</li>
 *     <li>Binding of cancel button visibility to external properties</li>
 * </ul>
 *
 * @author Felix Bänsch
 * @author Tom Weiß
 * @version 1.0
 */
public class MoleculesTabView extends GridTabForTableView {
    //<editor-fold desc="private final instance variables">
    /**
     * The pagination component used to navigate through pages of molecules.
     */
    private final Pagination pagination;

    /**
     * The molecule data table view that is contained in this view (currently only used to add a width change listener).
     */
    private final MoleculesDataTableView moleculesDataTableView;
    //</editor-fold>
    //
    //<editor-fold desc="private variables">
    /**
     * the button to start a fragmentation.
     */
    private Button fragmentationButton;

    /**
     * the button to cancel a fragmentation.
     */
    private Button cancelFragmentationButton;

    /**
     * the button to open a new window with the overview of the molecules of this tab.
     */
    private Button overviewButton;
    //</editor-fold>
    //
    //<editor-fold desc="constructor">
    /**
     * Creates a molecules tab that displays all molecules with pagination and control buttons.
     *
     * <p>This constructor initializes the view with a molecules data table, sets up pagination
     * based on the provided rows per page setting, and creates control buttons for fragmentation
     * and overview operations. The fragmentation button text is dynamically bound to the currently
     * selected fragmentation algorithm display name.
     *
     * @param aMoleculesDataTableView the molecules data table view to be displayed in this tab;
     *                                must not be null
     * @param aMoleculeDataModelList an observable list of molecule data models to be displayed;
     *                               must not be null
     * @param aRowsPerPageSetting the number of rows to display per page for pagination; must be greater than 0
     * @param aFragmentationServiceSelectedFragmenterDisplayNameProperty a string property binding
     *        for the fragmentation button text that reflects the currently selected fragmenter's display name;
     *        must not be null
     *
     * @throws IllegalArgumentException if any required parameter is null or if aRowsPerPageSetting is &lt;= 0
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
        this.moleculesDataTableView = aMoleculesDataTableView;
        aMoleculesDataTableView.setItemsList(aMoleculeDataModelList);
        this.pagination = GuiUtil.createPaginationWithSuitablePageCount(
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
    }
    //</editor-fold>
    //
    //<editor-fold desc="private helper methods">
    /**
     * Creates the button to start and cancel a fragmentation.
     *
     * @param aFragmentationDisplayNameProperty name of the currently selected fragmentation algorithm
     * @return the created buttons with the cancel button being hidden
     */
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
    /**
     * Adjusts the size of the fragmentation button to fit the text description of the selected fragmentation
     * algorithm name.
     *
     * @param aFragmenterDisplayName the text to be placed in the button
     */
    private void updateFragmentationButtonSize(String aFragmenterDisplayName) {
        double tmpTextWidth = new Text(aFragmenterDisplayName).getLayoutBounds().getWidth() + GuiDefinitions.GUI_BUTTON_INSETS_VALUE;
        this.fragmentationButton.setPrefWidth(tmpTextWidth);
        this.fragmentationButton.setMinWidth(tmpTextWidth);
        this.fragmentationButton.setMaxWidth(tmpTextWidth);
        this.fragmentationButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
    }
    /**
     * Creates an overview button.
     *
     * @return the created overview button
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
        this.overviewButton = GuiUtil.getButtonOfStandardSize(
                Message.get("MainView.showOverviewViewButton.text")
        );
        this.overviewButton.setTooltip(GuiUtil.createTooltip(
                Message.get("MainView.showOverviewViewButton.tooltip")
        ));
        tmpBox.getChildren().add(this.overviewButton);
        return tmpBox;
    }
    //</editor-fold>
    //
    //<editor-fold desc="public getters">
    /**
     * Retrieves the pagination component used for navigating through molecule pages.
     *
     * @return the {@link Pagination} component; never null
     */
    @Override
    public Pagination getPagination() {
        return this.pagination;
    }
    /**
     * Retrieves the button that initiates a fragmentation operation.
     *
     * <p>The button text is dynamically bound to the selected fragmentation algorithm's display name.
     *
     * @return the fragmentation {@link Button}; never null
     */
    public Button getFragmentationButton() {
        return this.fragmentationButton;
    }
    /**
     * Retrieves the button that cancels an ongoing fragmentation operation.
     *
     * <p>This button is initially hidden and becomes visible only when a fragmentation operation
     * is in progress.
     *
     * @return the cancel fragmentation {@link Button}; never null
     */
    public Button getCancelFragmentationButton() {
        return this.cancelFragmentationButton;
    }
    /**
     * Retrieves the button that opens an overview window of all molecules in this tab.
     *
     * @return the overview {@link Button}; never null
     */
    public Button getOverviewButton() {
        return this.overviewButton;
    }
    //</editor-fold>
    //
    //<editor-fold desc="public callback setters">
    /**
     * Sets the callback function to be executed when the fragmentation button is pressed.
     *
     * @param aCallback the callback to execute on fragmentation button action; must not be null
     */
    public void setOnFragmentation(Runnable aCallback) {
        this.fragmentationButton.setOnAction(event -> aCallback.run());
    }
    /**
     * Sets the callback function to be executed when the cancel fragmentation button is pressed.
     *
     * @param aCallback the callback to execute on cancel fragmentation button action; must not be null
     *
     * @throws IllegalArgumentException if aCallback is null
     *
     * @see #setOnFragmentation(Runnable)
     */
    public void setOnCancelFragmentation(Runnable aCallback) {
        this.cancelFragmentationButton.setOnAction(event -> aCallback.run());
    }
    /**
     * Sets the callback function to be executed when the overview button is pressed.
     *
     * @param aCallback the callback to execute on overview button action; must not be null
     *
     * @throws IllegalArgumentException if aCallback is null
     */
    public void setOnOverview(Runnable aCallback) {
        this.overviewButton.setOnAction(event -> aCallback.run());
    }
    /**
     * Sets the callback function to be executed when the table view width changes and establishes
     * a listener on the underlying molecules data table view's width property.
     *
     * <p>The callback will be invoked whenever the width of the molecules data table view changes,
     * receiving the new width value.
     *
     * @param aCallback the callback to execute on table width change; receives the new width value;
     *                  must not be null
     */
    public void setOnTableWidthChanged(Consumer<Number> aCallback) {
        this.moleculesDataTableView.widthProperty().addListener(
                (observable, oldValue, newValue) -> aCallback.accept(newValue)
        );
    }
    /**
     * Binds the visibility of the cancel fragmentation button to an external boolean property.
     *
     * <p>When the provided property changes, the cancel fragmentation button's visibility will
     * automatically update to match. This is typically used to show the cancel button during
     * active fragmentation operations and hide it otherwise.
     *
     * @param aProperty the boolean property to bind to the cancel button's visibility;
     *                  must not be null
     *
     * @throws IllegalArgumentException if aProperty is null
     */
    public void bindCancelFragmentationButtonVisibility(BooleanProperty aProperty) {
        this.cancelFragmentationButton.visibleProperty().bind(aProperty);
    }
    //</editor-fold>
}
