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

package de.unijena.cheminf.mortar.gui.util;

import de.unijena.cheminf.mortar.configuration.Configuration;
import de.unijena.cheminf.mortar.configuration.IConfiguration;
import de.unijena.cheminf.mortar.gui.views.FragmentsDataTableView;
import de.unijena.cheminf.mortar.gui.views.IDataTableView;
import de.unijena.cheminf.mortar.gui.views.ItemizationDataTableView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * GUI utility.
 *
 * @author Jonas Schaub, Felix Baensch
 * @version 1.0.0.0
 */
public class GuiUtil {
    //<editor-fold defaultstate="collapsed" desc="Public static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(GuiUtil.class.getName());
    //
    /**
     * Configuration class to read resource file paths from.
     */
    private static final IConfiguration CONFIGURATION;
    static {
        try {
            CONFIGURATION = Configuration.getInstance();
        } catch (IOException anIOException) {
            //when MORTAR is run via MainApp.start(), the correct initialization of Configuration is checked there before
            // GuiUtil is accessed and this static initializer called
            throw new NullPointerException("Configuration could not be initialized");
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private constructor">
    /**
     * Private parameter-less constructor.
     * Introduced because javadoc build complained about classes without declared default constructor.
     */
    private GuiUtil() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="public static methods" defaultstate="collapsed">
    /**
     * Creates and shows an alert with arbitrary alert type.
     *
     * @param anAlertType  - pre-built alert type of the alert message that the Alert class can use to pre-populate
     *                     various properties, chosen of an enumeration containing the available
     * @param aTitle       Title of the alert message
     * @param aHeaderText  Header of the alert message
     * @param aContentText Text that the alert message contains
     * @return ButtonType selected by user, options depend on the given alert type (INFORMATION, WARNING, ERROR -> OK,
     * CONFIRMATION -> OK / CANCEL)
     */
    public static Optional<ButtonType> guiMessageAlert(Alert.AlertType anAlertType, String aTitle, String aHeaderText, String aContentText) {
        Alert tmpAlert = new Alert(anAlertType);
        tmpAlert.setTitle(aTitle);
        tmpAlert.setHeaderText(aHeaderText);
        tmpAlert.setContentText(aContentText);
        //tmpAlert.setResizable(true);
        tmpAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        tmpAlert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
        Stage tmpAlertStage = (Stage) tmpAlert.getDialogPane().getScene().getWindow();
        String tmpIconURL = GuiUtil.class.getClassLoader().getResource(
                GuiUtil.CONFIGURATION.getProperty("mortar.imagesFolder") + GuiUtil.CONFIGURATION.getProperty("mortar.logo.icon.name")).toExternalForm();
        tmpAlertStage.getIcons().add(new Image(tmpIconURL));
        return tmpAlert.showAndWait();
    }
    //
    /**
     * Creates and shows an alert with arbitrary alert type and the given hyperlink in the content section.
     *
     * @param anAlertType - pre-built alert type of the alert message that the Alert class can use to pre-populate
     *                    various properties, chosen of an enumeration containing the available
     * @param aTitle      Title of the alert message
     * @param aHeaderText Header of the alert message
     * @param aHyperlink  Hyperlink that the alert message contains
     * @return ButtonType selected by user, options depend on the given alert type (INFORMATION, WARNING, ERROR -> OK,
     * CONFIRMATION -> OK / CANCEL)
     */
    public static Optional<ButtonType> guiMessageAlertWithHyperlink(Alert.AlertType anAlertType,
                                                                    String aTitle,
                                                                    String aHeaderText,
                                                                    Hyperlink aHyperlink) {
        Alert tmpAlert = new Alert(anAlertType);
        tmpAlert.setTitle(aTitle);
        tmpAlert.setHeaderText(aHeaderText);
        tmpAlert.getDialogPane().setContent(aHyperlink);
        //tmpAlert.setResizable(true);
        tmpAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        tmpAlert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
        Stage tmpAlertStage = (Stage) tmpAlert.getDialogPane().getScene().getWindow();
        String tmpIconURL = GuiUtil.class.getClassLoader().getResource(
                GuiUtil.CONFIGURATION.getProperty("mortar.imagesFolder") + GuiUtil.CONFIGURATION.getProperty("mortar.logo.icon.name")).toExternalForm();
        tmpAlertStage.getIcons().add(new Image(tmpIconURL));
        return tmpAlert.showAndWait();
    }
    //
    /**
     * Creates and shows confirmation type alert and returns the button selected by user as ButtonType.
     * Two buttons are possible - ButtonType.OK and ButtonType.CANCEL.
     *
     * @param aTitle Title of the confirmation alert
     * @param aHeaderText Header of the confirmation alert
     * @param aContentText Text that the confirmation alert contains
     * @return ButtonType selected by user - ButtonType.OK or ButtonType.CANCEL
     */
    public static ButtonType guiConfirmationAlert(String aTitle, String aHeaderText, String aContentText) {
        Alert tmpAlert = new Alert(Alert.AlertType.CONFIRMATION);
        //tmpAlert.setResizable(true);
        tmpAlert.setTitle(aTitle);
        tmpAlert.setHeaderText(aHeaderText);
        tmpAlert.setContentText(aContentText);
        tmpAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        tmpAlert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
        Stage tmpAlertStage = (Stage) tmpAlert.getDialogPane().getScene().getWindow();
        String tmpIconURL = GuiUtil.class.getClassLoader().getResource(
                GuiUtil.CONFIGURATION.getProperty("mortar.imagesFolder") + GuiUtil.CONFIGURATION.getProperty("mortar.logo.icon.name")).toExternalForm();
        tmpAlertStage.getIcons().add(new Image(tmpIconURL));
        return tmpAlert.showAndWait().orElse(ButtonType.CANCEL);
    }
    //
    /**
     * Creates and shows confirmation type alert and returns the button selected by user as ButtonType.
     * Three buttons are possible - ButtonType.YES, ButtonType.NO, and ButtonType.CANCEL.
     *
     * @param aTitle Title of the confirmation alert
     * @param aHeaderText Header of the confirmation alert
     * @param aContentText Text that the confirmation alert contains
     * @return ButtonType selected by user - ButtonType.YES, ButtonType.NO, or ButtonType.CANCEL.
     */
    public static ButtonType guiYesNoCancelConfirmationAlert(String aTitle, String aHeaderText, String aContentText) {
        Alert tmpAlert = new Alert(Alert.AlertType.CONFIRMATION);
        tmpAlert.setTitle(aTitle);
        tmpAlert.setHeaderText(aHeaderText);
        tmpAlert.setContentText(aContentText);
        tmpAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        tmpAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        tmpAlert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
        Stage tmpAlertStage = (Stage) tmpAlert.getDialogPane().getScene().getWindow();
        String tmpIconURL = GuiUtil.class.getClassLoader().getResource(
                GuiUtil.CONFIGURATION.getProperty("mortar.imagesFolder") + GuiUtil.CONFIGURATION.getProperty("mortar.logo.icon.name")).toExternalForm();
        tmpAlertStage.getIcons().add(new Image(tmpIconURL));
        return tmpAlert.showAndWait().orElse(ButtonType.CANCEL);
    }
    //
    /**
     * Creates and shows an alert dialog to report an exception that occurred. The stack trace of the exception is also
     * given.
     *
     * @param aTitle title of the alert dialog
     * @param aHeaderText header of the alert dialog
     * @param aContentText Text of the alert dialog
     * @param anException exception to report, may be null
     */
    public static void guiExceptionAlert(String aTitle, String aHeaderText, String aContentText, Exception anException) {
        String tmpExceptionString;
        if (Objects.isNull(anException)) {
            tmpExceptionString = "Exception is null.";
        } else {
            StringWriter tmpStringWriter = new StringWriter();
            PrintWriter tmpPrintWriter = new PrintWriter(tmpStringWriter);
            anException.printStackTrace(tmpPrintWriter);
            tmpExceptionString = tmpStringWriter.toString();
        }
        GuiUtil.guiExpandableAlert(aTitle, aHeaderText, aContentText, Message.get("Error.ExceptionAlert.Label"), tmpExceptionString);
    }
    //
    /**
     * Creates and shows an alert explicit for exceptions, which contains the stack trace of the given exception in
     * an expandable pane.
     *
     * @param aTitle Title of the exception alert
     * @param aHeaderText Header of the exception alert
     * @param aContentText Text that the exception alert contains
     * @param aLabelText Text to show above expandable area
     * @param anExpandableString Text to show in expandable area
     */
    public static void guiExpandableAlert(String aTitle, String aHeaderText, String aContentText, String aLabelText, String anExpandableString) {
        try {
            Alert tmpAlert = new Alert(Alert.AlertType.ERROR);
            tmpAlert.setTitle(aTitle);
            tmpAlert.setHeaderText(aHeaderText);
            tmpAlert.setContentText(aContentText);
            Label tmpLabel = new Label(aLabelText);
            TextArea tmpExpandableTextArea = new TextArea(anExpandableString);
            tmpExpandableTextArea.setEditable(false);
            tmpExpandableTextArea.setWrapText(true);
            tmpExpandableTextArea.setMaxWidth(Double.MAX_VALUE);
            tmpExpandableTextArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(tmpExpandableTextArea, Priority.ALWAYS);
            GridPane.setHgrow(tmpExpandableTextArea, Priority.ALWAYS);
            GridPane tmpGridPane = new GridPane();
            tmpGridPane.setMaxWidth(Double.MAX_VALUE);
            tmpGridPane.add(tmpLabel, 0, 0);
            tmpGridPane.add(tmpExpandableTextArea, 0, 1);
            //Add expandable text to the dialog/alert pane
            tmpAlert.getDialogPane().setExpandableContent(tmpGridPane);
            Stage tmpAlertStage = (Stage) tmpAlert.getDialogPane().getScene().getWindow();
            String tmpIconURL = GuiUtil.class.getClassLoader().getResource(
                    GuiUtil.CONFIGURATION.getProperty("mortar.imagesFolder") + GuiUtil.CONFIGURATION.getProperty("mortar.logo.icon.name")).toExternalForm();
            tmpAlertStage.getIcons().add(new Image(tmpIconURL));
            //Show and wait alert
            tmpAlert.showAndWait();
        } catch(Exception aNewThrownException) {
            GuiUtil.LOGGER.log(Level.SEVERE, aNewThrownException.toString(), aNewThrownException);
            GuiUtil.guiMessageAlert(Alert.AlertType.ERROR,
                    Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    aNewThrownException.toString());
        }
    }
    //
    /**
     * Sorts the items of the TableView over all pages of the pagination and adds.
     *
     * @param anEvent SortEvent {@literal <}TableView {@literal >}
     * @param tmpPagination Pagination
     * @param tmpRowsPerPage int
     */
    public static void sortTableViewGlobally(SortEvent<TableView> anEvent, Pagination tmpPagination, int tmpRowsPerPage) {
        if (anEvent == null || anEvent.getSource().getSortOrder().isEmpty()) {
            return;
        }
        String tmpSortProp = ((PropertyValueFactory)((TableColumn) anEvent.getSource().getSortOrder().getFirst()).cellValueFactoryProperty().getValue()).getProperty().toString();
        TableColumn.SortType tmpSortType = ((TableColumn) anEvent.getSource().getSortOrder().getFirst()).getSortType();
        CollectionUtil.sortGivenFragmentListByPropertyAndSortType(((IDataTableView)anEvent.getSource()).getItemsList(), tmpSortProp, tmpSortType == TableColumn.SortType.ASCENDING);
        int fromIndex = tmpPagination.getCurrentPageIndex() * tmpRowsPerPage;
        int toIndex = Math.min(fromIndex + tmpRowsPerPage, ((IDataTableView)anEvent.getSource()).getItemsList().size());
        anEvent.getSource().getItems().clear();
        anEvent.getSource().getItems().addAll(((IDataTableView)anEvent.getSource()).getItemsList().subList(fromIndex,toIndex));
    }
    //
    /**
     * Binds height and width property of the child control to the parent pane properties.
     *
     * @param aParentPane Pane
     * @param aChildControl Control
     */
    public static void guiBindControlSizeToParentPane(Pane aParentPane, Control aChildControl) {
        aChildControl.prefHeightProperty().bind(aParentPane.heightProperty());
        aChildControl.prefWidthProperty().bind(aParentPane.widthProperty());
    }
    //
    /**
     * Returns an input pattern for integer values. "-" may be the first sign, the first number may not be 0.
     *
     * @return GUI input pattern for integer values
     */
    public static Pattern getIntegerPattern() {
        return Pattern.compile("-?(([1-9][0-9]*)|0)?");
    }
    //
    /**
     * Returns an input pattern for positive integer values, including 0.
     *
     * @return GUI input pattern for positive integer values
     */
    public static Pattern getPositiveIntegerInclZeroPattern() {
        return Pattern.compile("[0-9]*");
    }
    //
    /**
     * Returns an input pattern for double values. "-" may be the first sign, the first number may not be 0.
     *
     * @return GUI input pattern for double values
     */
    public static Pattern getDoublePattern() {
        return Pattern.compile("-?(([1-9][0-9]*)|0)?(\\.[0-9]*)?");
    }
    //
    /**
     * Returns an input pattern for positive double values, including 0.0 and equal notations of zero.
     *
     * @return GUI input pattern for positive double values
     */
    public static Pattern getPositiveDoublePattern() {
        return Pattern.compile("(([1-9][0-9]*)|0)?(\\.[0-9]*)?");
    }
    //
    /**
     * Returns an input filter for integer values. "-" may be the first sign, the first number may not be 0.
     *
     * @return GUI input filter for integer values
     */
    public static UnaryOperator<TextFormatter.Change> getIntegerFilter() {
        return c -> {
            String tmpText = c.getControlNewText();
            if (GuiUtil.getIntegerPattern().matcher(tmpText).matches()) {
                return c;
            } else {
                return null;
            }
        };
    }
    //
    /**
     * Method that creates an Integer filter to prevent the entry of unwanted
     * characters such as Strings or special characters and also 0 for first entry if specified.
     *
     * @param anIsZeroIncluded true if zero should be allowed as value (text: "0")
     * @return GUI input filter for positive integer values
     */
    public static UnaryOperator<TextFormatter.Change> getPositiveIntegerFilter(boolean anIsZeroIncluded) {
        return c -> {
            String tmpText = c.getControlNewText();
            if (tmpText.equals("0") && !anIsZeroIncluded) {
                return null;
            }
            if (GuiUtil.getPositiveIntegerInclZeroPattern().matcher(tmpText).matches()) {
                return c;
            }
            return null;
        };
    }
    //
    /**
     * Returns an input filter for double values. "-" may be the first sign, the first number may not be 0.
     *
     * @return GUI input filter for double values
     */
    public static UnaryOperator<TextFormatter.Change> getDoubleFilter() {
        return c -> {
            String text = c.getControlNewText();
            if (GuiUtil.getDoublePattern().matcher(text).matches()) {
                return c;
            } else {
                return null;
            }
        };
    }
    //
    /**
     * Returns an input filter for positive double values, including 0.0 and equal notations of zero.
     *
     * @return GUI input filter for positive double values
     */
    public static UnaryOperator<TextFormatter.Change> getPositiveDoubleFilter() {
        return c -> {
            String text = c.getControlNewText();
            if (GuiUtil.getPositiveDoublePattern().matcher(text).matches()) {
                return c;
            } else {
                return null;
            }
        };
    }
    //
    /**
     * Returns a String {@literal <->} Integer converter that mostly relies on the given toString() and fromString() methods
     * but additionally turns empty strings, "-", ".", and "-." into 0.
     *
     * @return String-Integer converter
     */
    public static StringConverter<Integer> getStringToIntegerConverter() {
        return new StringConverter<Integer>() {
            @Override
            public String toString(Integer anObject) {
                return anObject.toString();
            }
            @Override
            public Integer fromString(String aString) {
                if (aString.isEmpty() || "-".equals(aString) || ".".equals(aString) || "-.".equals(aString) || "0.".equals(aString)) {
                    return 0;
                } else {
                    return Integer.valueOf(aString);
                }
            }
        };
    }
    //
    /**
     * Returns a String {@literal <->} Double converter that mostly relies on the given toString() and fromString() methods
     * but additionally turns empty strings, "-", ".", and "-." into 0.0.
     *
     * @return String-Double converter
     */
    public static StringConverter<Double> getStringToDoubleConverter() {
        return new StringConverter<Double>() {
            @Override
            public String toString(Double anObject) {
                return anObject.toString();
            }
            @Override
            public Double fromString(String aString) {
                if (aString.isEmpty() || "-".equals(aString) || ".".equals(aString) || "-.".equals(aString)) {
                    return 0.0;
                } else {
                    return Double.valueOf(aString);
                }
            }
        };
    }
    //
    /**
     * Copies content of selected cell to system clipboard.
     *
     * @param aTableView TableView to copy from
     */
    public static void copySelectedTableViewCellsToClipboard(TableView<?> aTableView) {
        for (TablePosition tmpPos : aTableView.getSelectionModel().getSelectedCells()) {
            int tmpRowIndex = tmpPos.getRow();
            int tmpColIndex = tmpPos.getColumn();
            int tmpFragmentColIndexItemsTab = 2;
            Object tmpCell;
            if (aTableView.getClass() == ItemizationDataTableView.class && tmpColIndex > tmpFragmentColIndexItemsTab -1) {
                tmpCell = aTableView.getColumns().get(tmpFragmentColIndexItemsTab).getColumns().get(tmpColIndex - 2).getCellData(tmpRowIndex);
            } else {
                tmpCell = aTableView.getColumns().get(tmpColIndex).getCellData(tmpRowIndex);
            }
            if (tmpCell == null) {
                return;
            } else {
                ClipboardContent tmpClipboardContent = new ClipboardContent();
                if (tmpCell.getClass() == String.class) {
                    tmpClipboardContent.putString((String) tmpCell);
                } else if(tmpCell.getClass() == Integer.class) {
                    tmpClipboardContent.putString(((Integer)tmpCell).toString());
                } else if(tmpCell.getClass() == Double.class) {
                    tmpClipboardContent.putString(((Double)tmpCell).toString());
                } else if(tmpCell.getClass() == ImageView.class) {
                    IAtomContainer tmpAtomContainer;
                    try {
                        if (aTableView.getClass() == FragmentsDataTableView.class) {
                            // Check if header of column equals "Structure", then use AtomContainer of structure,
                            // else use AtomContainer of first parent molecule.
                            // If "label cast" throws an exception it is caught below.
                            if(Message.get("MainTabPane.fragmentsTab.tableView.structureColumn.header").equals(((Label)(tmpPos.getTableColumn().getGraphic())).getText())) {
                                tmpAtomContainer = ((FragmentDataModel) aTableView.getItems().get(tmpRowIndex)).getAtomContainer();
                            } else {
                                tmpAtomContainer = ((FragmentDataModel) aTableView.getItems().get(tmpRowIndex)).getFirstParentMolecule().getAtomContainer();
                            }
                        } else if(aTableView.getClass() == ItemizationDataTableView.class) {
                            if (tmpColIndex > 1) {
                                String tmpFragmentationName = ((ItemizationDataTableView) aTableView).getFragmentationName();
                                tmpAtomContainer = ((MoleculeDataModel) aTableView.getItems().get(tmpRowIndex)).getFragmentsOfSpecificFragmentation(tmpFragmentationName).get(tmpColIndex-2).getAtomContainer(); //magic number
                            } else {
                                tmpAtomContainer = ((MoleculeDataModel) aTableView.getItems().get(tmpRowIndex)).getAtomContainer();
                            }
                        } else {
                            tmpAtomContainer = ((MoleculeDataModel) aTableView.getItems().get(tmpRowIndex)).getAtomContainer();
                        }
                        Image tmpImage = DepictionUtil.depictImageWithZoomAndFillToFitAndWhiteBackground(tmpAtomContainer, 1, GuiDefinitions.GUI_COPY_IMAGE_IMAGE_WIDTH, GuiDefinitions.GUI_COPY_IMAGE_IMAGE_HEIGHT,true, true);
                        tmpClipboardContent.putImage(tmpImage);
                    } catch (CDKException | ClassCastException tmpException) {
                        tmpClipboardContent.putImage(((ImageView) tmpCell).getImage());
                    }
                } else {
                    return;
                }
                Clipboard.getSystemClipboard().setContent(tmpClipboardContent);
            }
        }
    }
    //
    /**
     * Sets the height for structure images to each MoleculeDataModel object of the items list of the tableView.
     * If image height is too small it will be set to GuiDefinitions.GUI_STRUCTURE_IMAGE_MIN_HEIGHT (50.0).
     *
     * @param aTableView TableView
     * @param aHeight double
     * @param aRowsPerPage int
     */
    public static void setImageStructureHeight(TableView aTableView, double aHeight, int aRowsPerPage) {
        double tmpHeight = (aHeight
                            - GuiDefinitions.GUI_TABLE_VIEW_HEADER_HEIGHT
                            - GuiDefinitions.GUI_PAGINATION_CONTROL_PANEL_HEIGHT)
                            / aRowsPerPage;
        if (aTableView.getClass().equals(ItemizationDataTableView.class)) {
            tmpHeight = (aHeight
                            - 2 * GuiDefinitions.GUI_TABLE_VIEW_HEADER_HEIGHT
                            - GuiDefinitions.GUI_PAGINATION_CONTROL_PANEL_HEIGHT
                            - GuiDefinitions.GUI_SCROLL_BAR_HEIGHT)
                            / aRowsPerPage;
        }
        if (tmpHeight < GuiDefinitions.GUI_STRUCTURE_IMAGE_MIN_HEIGHT) {
            tmpHeight = GuiDefinitions.GUI_STRUCTURE_IMAGE_MIN_HEIGHT;
        }
        if (aTableView.getClass().equals(ItemizationDataTableView.class)) {
            for (MoleculeDataModel tmpMoleculeDataModel : ((IDataTableView)aTableView).getItemsList()) {
                tmpMoleculeDataModel.setStructureImageHeight(tmpHeight);
                String tmpFragmentationName = ((ItemizationDataTableView) aTableView).getFragmentationName();
                if (!tmpMoleculeDataModel.hasMoleculeUndergoneSpecificFragmentation(tmpFragmentationName)) {
                    continue;
                }
                for (FragmentDataModel tmpFragmentDataModel : tmpMoleculeDataModel.getFragmentsOfSpecificFragmentation(tmpFragmentationName)) {
                    tmpFragmentDataModel.setStructureImageHeight(tmpHeight);
                }
            }
        } else {
            //case molecules tab or fragments tab
            //note: height of parent structures in fragments tab does not need to be set because it equals the height in the molecules tab
            for (MoleculeDataModel tmpMoleculeDataModel : ((IDataTableView)aTableView).getItemsList()) {
                tmpMoleculeDataModel.setStructureImageHeight(tmpHeight);
            }
        }
    }
    //
    /**
     * Returns the largest number of fragments of one molecule found in the given list for the given fragmentation name.
     *
     * @param aListOfMolecules List of MoleculeDataModels
     * @param aFragmentationName String for the fragmentation name
     * @return largest number of fragments of one molecule
     */
    public static int getLargestNumberOfFragmentsForGivenMoleculeListAndFragmentationName(List<MoleculeDataModel> aListOfMolecules, String aFragmentationName) {
        //tmpAmount is the number of fragments appearing in the molecule with the highest number of fragments
        int tmpAmount = 0;
        for (MoleculeDataModel aListOfMolecule : aListOfMolecules) {
            if (!aListOfMolecule.hasMoleculeUndergoneSpecificFragmentation(aFragmentationName)) {
                continue;
            }
            Map<String, Integer> tmpCurrentFragmentsMap = aListOfMolecule.getFragmentFrequencyOfSpecificFragmentation(aFragmentationName);
            if (tmpCurrentFragmentsMap == null) { //redundant, see if-clause above
                continue;
            }
            int tmpNrOfFragmentsOfCurrentMolecule = tmpCurrentFragmentsMap.size();
            tmpAmount = Math.max(tmpAmount, tmpNrOfFragmentsOfCurrentMolecule);
        }
        return tmpAmount;
    }
    //
    /**
     * Returns a button with the GUI's standard width and height for buttons and the given text string set as its label.
     *
     * @param aText A text string for its label
     * @return A Button of the GUI's standard size
     */
    public static Button getButtonOfStandardSize(String aText) {
        Button tmpButton = new Button(aText);
        tmpButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        tmpButton.setMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        tmpButton.setMaxWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        tmpButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        return tmpButton;
    }
    //
    /**
     * Returns a new tooltip with the given text, configured with a fixed maximum width, text wrap activated, and
     * a set show duration.
     *
     * @param aText text for the tooltip
     * @return new tooltip instance
     */
    public static Tooltip createTooltip(String aText) {
        Tooltip tmpTooltip = new Tooltip(aText);
        tmpTooltip.setMaxWidth(GuiDefinitions.GUI_TOOLTIP_MAX_WIDTH);
        tmpTooltip.setWrapText(true);
        tmpTooltip.setShowDuration(Duration.seconds(GuiDefinitions.GUI_TOOLTIP_SHOW_DURATION));
        return tmpTooltip;
    }
    //</editor-fold>
}
