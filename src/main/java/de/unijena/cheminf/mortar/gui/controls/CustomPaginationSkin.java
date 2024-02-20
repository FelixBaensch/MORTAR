/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2024  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.gui.controls;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;

import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Pagination;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.PaginationSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;

/**
 * Customized pagination skin to add first and last page button and a text field to jump to a page specified by the user
 *
 * See kleopatra's comment on https://stackoverflow.com/questions/31540001/how-to-extend-javafx-pagination-navigation-to-display-additional-controls
 * (retrieved August 18, 2022) for more details
 * @author kleopatra (https://stackoverflow.com/users/203657/kleopatra, retireved August 18, 2022), Felix Baensch
 */
public class CustomPaginationSkin extends PaginationSkin {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * HBox to hold the control elements
     */
    private HBox controlHBox;
    /**
     * Button to jump to next page, necessary here to add the new control elements
     */
    private Button nextButton;
    /**
     * Button to jump to the first page
     */
    private Button firstButton;
    /**
     * Button to jump to the last page
     */
    private Button lastButton;
    /**
     * TextField to jump to a page specified by the user
     */
    private TextField jumpToTextField;
    //</editor-fold>
    //
    /**
     * Creates a new PaginationSkin instance, installing the necessary child
     * nodes into the Control {@link Control}  children list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public CustomPaginationSkin(Pagination control) {
        super(control);
        this.patchNavigation();
    }
    //
    /**
     * Adds new control elements and their functionality
     */
    private void patchNavigation() {
        Pagination tmpPagination = getSkinnable();
        Node tmpControl = tmpPagination.lookup(".control-box");
        if (!(tmpControl instanceof HBox))
            return;
        this.controlHBox = (HBox) tmpControl;
//        prev = (Button) controlBox.getChildren().get(0);
        this.nextButton = (Button) this.controlHBox.getChildren().get(this.controlHBox.getChildren().size() - 1);
        this.firstButton = new Button(Message.get("CustomPaginationSkin.controlBox.firstButton.text"));
        this.firstButton.setTooltip(new Tooltip(Message.get("CustomPaginationSkin.controlBox.firstButton.tooltip")));
        this.firstButton.setOnAction(e -> {
            tmpPagination.setCurrentPageIndex(0);
        });
        this.firstButton.disableProperty().bind(
                tmpPagination.currentPageIndexProperty().isEqualTo(0));
        this.lastButton = new Button(Message.get("CustomPaginationSkin.controlBox.lastButton.text"));
        this.lastButton.setTooltip(new Tooltip(Message.get("CustomPaginationSkin.controlBox.lastButton.tooltip")));
        this.lastButton.setOnAction(e -> {
            tmpPagination.setCurrentPageIndex(tmpPagination.pageCountProperty().get());
        });
        this.lastButton.disableProperty().bind(
                tmpPagination.currentPageIndexProperty().isEqualTo(
                        tmpPagination.pageCountProperty().subtract(1)));
        this.jumpToTextField = new TextField();
        this.jumpToTextField.setTooltip(new Tooltip(Message.get("CustomPaginationSkin.controlBox.textField.tooltip")));
        this.jumpToTextField.setMaxWidth(GuiDefinitions.PAGINATION_TEXT_FIELD_WIDTH);
        this.jumpToTextField.setMinWidth(GuiDefinitions.PAGINATION_TEXT_FIELD_WIDTH);
        this.jumpToTextField.setPrefWidth(GuiDefinitions.PAGINATION_TEXT_FIELD_WIDTH);
        this.jumpToTextField.setAlignment(Pos.CENTER_RIGHT);
        this.jumpToTextField.setTextFormatter( new TextFormatter<>(GuiUtil.getStringToIntegerConverter(), tmpPagination.getCurrentPageIndex()+1, GuiUtil.getPositiveIntegerWithoutZeroFilter()));
        this.jumpToTextField.setOnKeyPressed(key -> {
            if(key.getCode().equals(KeyCode.ENTER)){
                int tmpPageNumber = Integer.parseInt(jumpToTextField.getText()) - 1;
                if(tmpPageNumber > tmpPagination.pageCountProperty().get()){
                    tmpPageNumber = tmpPagination.pageCountProperty().get();
                }
                tmpPagination.setCurrentPageIndex(tmpPageNumber);
            }
        });
        tmpPagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
            if((newValue.intValue() + 1) != Integer.parseInt(this.jumpToTextField.getText())) {
                jumpToTextField.setText(Integer.toString(newValue.intValue() + 1));
            }
        });
        ListChangeListener childrenListener = c -> {
            while (c.next()) {
                // implementation detail: when nextButton is added, the setup is complete
                if (c.wasAdded() && !c.wasRemoved() // real addition
                        && c.getAddedSize() == 1 // single addition
                        && c.getAddedSubList().get(0) == nextButton) {
                    addCustomNodes();
                }
            }
        };
        this.controlHBox.getChildren().addListener(childrenListener);
        addCustomNodes();
    }
    //
    /**
     * Adds the control elements to the control box
     */
    protected void addCustomNodes() {
        // guarding against duplicate child exception
        // (some weird internals that I don't fully understand...)
        if (this.firstButton.getParent() == this.controlHBox) return;
        this.controlHBox.getChildren().add(0, this.firstButton);
        this.controlHBox.getChildren().add(this.lastButton);
        this.controlHBox.getChildren().add(this.jumpToTextField);
    }
}
