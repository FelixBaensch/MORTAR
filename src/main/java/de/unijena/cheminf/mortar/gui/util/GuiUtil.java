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
import javafx.scene.control.CheckBox;
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
import javafx.scene.layout.VBox;
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
 * @author Jonas Schaub
 * @author Felix Baensch
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
     * Sealed interface that defines a contract for different types of content that can be displayed
     * in JavaFX Alert dialogs. Each implementation encapsulates the logic for configuring a specific
     * content type within an Alert's dialog pane.
     * <p>
     * Currently Permitted implementations (26.03.2026):
     * <ul>
     *     <li>{@link StringContent} - Simple text content</li>
     *     <li>{@link HyperlinkContent} - Interactive hyperlink content</li>
     *     <li>{@link CheckboxContent} - Text content with an interactive checkbox</li>
     *     <li>{@link MultiButtonContent} - Text content with custom button types</li>
     *     <li>{@link ExpandableContent} - Text content with an expandable text area</li>
     * </ul>
     * </p>
     */
    private interface Content {
        /**
         * Applies this content configuration to the given Alert dialog pane.
         * <p>
         * Implementations are responsible for configuring the Alert's dialog pane with their
         * specific content type and layout. This may include setting text content, adding custom
         * nodes, configuring buttons, or setting expandable content.
         * </p>
         *
         * @param anAlert the Alert dialog to configure with this content. Must not be null.
         */
        void applyTo(Alert anAlert);
    }
    /**
     * Record implementation of {@link Content} for displaying simple text content in an Alert.
     *
     * @param aContentText the text content to display in the alert. May contain newlines for
     *                     multi-line text. Must not be null.
     */
    private record StringContent(String aContentText) implements Content {
        /**
         * Applies the text content to the Alert by setting its content text property.
         *
         * @param anAlert the Alert dialog to configure. Must not be null.
         */
        @Override
        public void applyTo(Alert anAlert) {
            anAlert.setContentText(aContentText);
        }
    }
    /**
     * Record implementation of {@link Content} for displaying an interactive Hyperlink in an Alert.
     *
     * @param aHyperlink the Hyperlink node to display in the alert. Must not be null.
     */
    private record HyperlinkContent(Hyperlink aHyperlink) implements Content {
        /**
         * Applies the hyperlink content to the Alert by adding it to the dialog pane.
         *
         * @param anAlert the Alert dialog to configure. Must not be null.
         */
        @Override
        public void applyTo(Alert anAlert) {
            anAlert.getDialogPane().setContent(aHyperlink);
        }
    }
    /**
     * Record implementation of {@link Content} for displaying text content with an interactive checkbox.
     *
     * @param aContentText the main text content to display above the checkbox. Will be wrapped
     *                     if it exceeds the available width. Must not be null.
     * @param aCheckboxText the label text for the checkbox, displayed to the right of the checkbox.
     *                      Must not be null.
     * @param aCheckbox the CheckBox control whose selected state will be queried after the alert
     *                  is dismissed. Must not be null.
     */
    private record CheckboxContent(String aContentText, String aCheckboxText, CheckBox aCheckbox) implements Content {
        /**
         * Applies the checkbox content to the Alert by creating a layout with wrapped text and
         * a checkbox, and adding it to the dialog pane.
         * <p>
         * The layout is structured as:
         * <ul>
         *     <li>Wrapped text label (grows to fill available space)</li>
         *     <li>Checkbox with associated label text</li>
         * </ul>
         * </p>
         *
         * @param anAlert the Alert dialog to configure. Must not be null.
         */
        @Override
        public void applyTo(Alert anAlert) {
            Label tmpContentLabel = new Label(aContentText);
            tmpContentLabel.setWrapText(true);
            tmpContentLabel.maxWidthProperty().bind(anAlert.getDialogPane().widthProperty().subtract(40));
            VBox.setVgrow(tmpContentLabel, Priority.ALWAYS);
            VBox tmpContentBox = new VBox(20, tmpContentLabel, aCheckbox);
            tmpContentBox.setFillWidth(true);
            anAlert.getDialogPane().setContent(tmpContentBox);
        }
    }
    /**
     * Record implementation of {@link Content} for displaying text content with custom button types.
     *
     * @param aContentText the text content to display in the alert. Must not be null.
     * @param anButtonTypeArray an array of ButtonType objects representing the buttons to display
     *                          in the alert. The order in the array determines the button layout.
     *                          Must not be null or empty.
     */
    private record MultiButtonContent(String aContentText, ButtonType[] anButtonTypeArray) implements Content {
        /**
         * Applies the text content and custom button types to the Alert.
         *
         * @param anAlert the Alert dialog to configure. Must not be null.
         */
        @Override
        public void applyTo(Alert anAlert) {
            anAlert.setContentText(aContentText);
            anAlert.getButtonTypes().setAll(anButtonTypeArray);
        }
    }
    /**
     * Record implementation of {@link Content} for displaying text content with an expandable text area.
     * <p>
     * The layout consists of:
     * <ul>
     *     <li>Main content text (displayed in the alert's header/content area)</li>
     *     <li>A label describing the expandable content</li>
     *     <li>A read-only, word-wrapped TextArea containing the expandable information</li>
     * </ul>
     * The expandable section is collapsed by default and can be expanded by the user via a disclosure
     * triangle in the dialog pane.
     * </p>
     *
     * @param aContentText the main text content to display in the alert. Must not be null.
     * @param aLabelText the label text displayed above the expandable text area, typically describing
     *                   the type of information in the expandable section (e.g., "Stack Trace").
     *                   Must not be null.
     * @param anExpandableString the detailed text content to display in the expandable area. This is
     *                           typically a stack trace, error details, or other verbose information.
     *                           Must not be null.
     */
    private record ExpandableContent(
            String aContentText,
            String aLabelText,
            String anExpandableString
    ) implements Content {
        /**
         * Applies the expandable content to the Alert by creating a GridPane layout with a label
         * and read-only TextArea, and setting it as the dialog pane's expandable content.
         * <p>
         * The TextArea is configured to:
         * <ul>
         *     <li>Be read-only (non-editable)</li>
         *     <li>Wrap text to the available width</li>
         *     <li>Grow to fill available space both horizontally and vertically</li>
         * </ul>
         * </p>
         *
         * @param anAlert the Alert dialog to configure. Must not be null.
         */
        @Override
        public void applyTo(Alert anAlert) {
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
            anAlert.getDialogPane().setExpandableContent(tmpGridPane);
        }
    }
    /**
     * Generic factory method that creates and configures a fully-initialized Alert dialog with
     * specific content handling based on the {@link Content#applyTo} method of the passed {@link Content}.
     * <p>
     * The method performs the following configuration steps:
     * <ol>
     *     <li>Creates an Alert with the specified AlertType</li>
     *     <li>Sets the title and header text</li>
     *     <li>Delegates content configuration to the provided Content implementation</li>
     *     <li>Configures the dialog pane to use preferred sizing (auto-sizing)</li>
     *     <li>Retrieves the underlying Stage and applies the application icon</li>
     * </ol>
     * </p>
     *
     * @param <T> a type parameter bounded by {@link Content}, ensuring type safety while allowing
     *            any Content implementation to be passed
     * @param anAlertType the AlertType that determines the default styling and button configuration
     *                    of the Alert. Common values include:
     *                    <ul>
     *                        <li>{@link Alert.AlertType#INFORMATION} - for informational messages</li>
     *                        <li>{@link Alert.AlertType#WARNING} - for warning messages</li>
     *                        <li>{@link Alert.AlertType#ERROR} - for error messages</li>
     *                        <li>{@link Alert.AlertType#CONFIRMATION} - for confirmation dialogs</li>
     *                    </ul>
     *                    Must not be null.
     * @param aTitle the title text displayed in the Alert window's title bar. Must not be null.
     * @param aHeaderText the header text displayed prominently in the Alert dialog pane. This text
     *                    appears above the content area.
     *                    Must not be null.
     * @param aContent a Content implementation that encapsulates the specific content type and
     *                 configuration logic for this Alert. The content's {@link Content#applyTo(Alert)}
     *                 method will be invoked to configure the Alert's dialog pane. Must not be null.
     *
     * @return a fully-configured and initialized Alert dialog ready to be displayed. The returned
     *         Alert has:
     *         <ul>
     *             <li>The specified title and header text</li>
     *             <li>Content configured by the provided Content implementation</li>
     *             <li>Dialog pane sized to fit its content (preferred sizing)</li>
     *             <li>The application icon set on the window stage</li>
     *         </ul>
     *         The Alert is not yet displayed; call {@link Alert#showAndWait()} or
     *         {@link Alert#show()} to display it to the user.
     */
    private static <T extends Content> Alert createGenericAlert(
            Alert.AlertType anAlertType,
            String aTitle,
            String aHeaderText,
            T aContent
    ) {
        Alert tmpAlert = new Alert(anAlertType);
        tmpAlert.setTitle(aTitle);
        tmpAlert.setHeaderText(aHeaderText);
        aContent.applyTo(tmpAlert);
        //tmpAlert.setResizable(true);
        tmpAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        tmpAlert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
        Stage tmpAlertStage = (Stage) tmpAlert.getDialogPane().getScene().getWindow();
        String tmpIconURL = GuiUtil.class.getClassLoader().getResource(
                GuiUtil.CONFIGURATION.getProperty("mortar.imagesFolder") + GuiUtil.CONFIGURATION.getProperty("mortar.logo.icon.name")).toExternalForm();
        tmpAlertStage.getIcons().add(new Image(tmpIconURL));
        return tmpAlert;
    }
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
    public static Optional<ButtonType> guiMessageAlert(
            Alert.AlertType anAlertType,
            String aTitle,
            String aHeaderText,
            String aContentText
    ) {
        Alert tmpAlert = createGenericAlert(anAlertType, aTitle, aHeaderText, new StringContent(aContentText));
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
        Alert tmpAlert = createGenericAlert(
                anAlertType,
                aTitle,
                aHeaderText,
                new HyperlinkContent(aHyperlink)
        );
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
        Alert tmpAlert = createGenericAlert(
                Alert.AlertType.CONFIRMATION,
                aTitle,
                aHeaderText,
                new StringContent(aContentText)
        );
        return tmpAlert.showAndWait().orElse(ButtonType.CANCEL);
    }
    //
    /**
     * Small utility type to return the result of a gui confirmation dialogue with a checkbox and the button type
     * that was pressed.
     *
     * @param checkboxChecked true if the checkbox is checked upon closing the window.
     * @param buttonType the button tye that was pressed by the user
     */
    public record CheckboxAndButtonResult(boolean checkboxChecked, ButtonType buttonType) {}
    //
    /**
     * Creates and shows confirmation type alert and returns the button selected by user as ButtonType.
     * It also checks a global setting and also has the possibility to change that setting
     * if the checkbox in the alert window was checked.
     *
     * @param aTitle Title of the confirmation alert
     * @param aHeaderText Header of the confirmation alert
     * @param aContentText Text that the confirmation alert contains
     * @param aCheckboxText Explanation to display on the right side of the checkbox
     * @return a {@link CheckboxAndButtonResult} containing a boolean specifying if the checkbox was marked
     * and the button type selected by user - ButtonType.OK or ButtonType.CANCEL
     */
    public static CheckboxAndButtonResult guiConfirmationAlertWithCheckbox(
            String aTitle,
            String aHeaderText,
            String aContentText,
            String aCheckboxText
    ) {
        CheckBox tmpCheckBox = new CheckBox(aCheckboxText);
        Alert tmpAlert = createGenericAlert(
                Alert.AlertType.CONFIRMATION,
                aTitle, aHeaderText,
                new CheckboxContent(
                        aContentText,
                        aCheckboxText,
                        tmpCheckBox
                )
        );

        Optional<ButtonType> result = tmpAlert.showAndWait();
        return new CheckboxAndButtonResult(tmpCheckBox.isSelected(),
                result.orElse(ButtonType.CANCEL));
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
    public static ButtonType guiYesNoCancelConfirmationAlert(
            String aTitle,
            String aHeaderText,
            String aContentText
    ) {
        Alert tmpAlert = createGenericAlert(
                Alert.AlertType.CONFIRMATION,
                aTitle, aHeaderText,
                new MultiButtonContent(
                        aContentText,
                        new ButtonType[]{ButtonType.YES, ButtonType.NO, ButtonType.CANCEL}
                )
        );
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
    public static void guiExceptionAlert(
            String aTitle,
            String aHeaderText,
            String aContentText,
            Exception anException
    ) {
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
    public static void guiExpandableAlert(
            String aTitle,
            String aHeaderText,
            String aContentText,
            String aLabelText,
            String anExpandableString
    ) {
        try {
            Alert tmpAlert = createGenericAlert(
                    Alert.AlertType.ERROR,
                    aTitle,
                    aHeaderText,
                    new ExpandableContent(
                            aContentText,
                            aLabelText,
                            anExpandableString
                    )
            );
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
                GuiUtil.LOGGER.log(Level.WARNING, "Selected cell in table view is empty and could not be copied to clipboard.");
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
                        //note: making the background transparent leads to problems on Windows, where the background then appears black
                        Image tmpImage = DepictionUtil.depictImage(
                                tmpAtomContainer,
                                1.0,
                                GuiDefinitions.GUI_COPY_IMAGE_IMAGE_WIDTH,
                                GuiDefinitions.GUI_COPY_IMAGE_IMAGE_HEIGHT,
                                true,
                                true);
                        tmpClipboardContent.putImage(tmpImage);
                    } catch (CDKException | ClassCastException tmpException) {
                        //copies the exact image instance already on display in the cell to clipboard, instead of generating a bigger depiction
                        tmpClipboardContent.putImage(((ImageView) tmpCell).getImage());
                    }
                } else {
                    GuiUtil.LOGGER.log(Level.WARNING, "Unknown data type in table view cell ({0}) could not be copied to clipboard.", tmpCell.getClass());
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
