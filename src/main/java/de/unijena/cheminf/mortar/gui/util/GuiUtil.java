/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.cheminf.mortar.gui.util;

import de.unijena.cheminf.mortar.message.Message;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * GUI utility
 *
 * @author Jonas Schaub, Felix Baensch
 */
public class GuiUtil {
    //<editor-fold defaultstate="collapsed" desc="Public static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(GuiUtil.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="public static methods" defaultstate="collapsed">
    /**
     * Creates and shows an alert with arbitrary alert type
     *
     * @param anAlertType - pre-built alert type of the alert message that the Alert class can use to pre-populate various properties, chosen of an enumeration containing the available
     * @param aTitle Title of the alert message
     * @param aHeaderText Header of the alert message
     * @param aContentText Text that the alert message contains
     */
    public static void GuiMessageAlert(Alert.AlertType anAlertType, String aTitle, String aHeaderText, String aContentText){
        Alert tmpAlert = new Alert(anAlertType);
        tmpAlert.setTitle(aTitle);
        tmpAlert.setHeaderText(aHeaderText);
        tmpAlert.setContentText(aContentText);
        tmpAlert.showAndWait();
    }
    //
    /**
     * Creates and shows conformation type alert and returns the button selected by user as ButtonType.
     * Two buttons are possible - ButtonType.OK and ButtonType.CANCEL.
     *
     * @param aTitle Title of the conformation alert
     * @param aHeaderText Header of the conformation alert
     * @param aContentText Text that the conformation alert contains
     * @return ButtonType selected by user - ButtonType.OK or ButtonType.CANCEL
     */
    public static ButtonType GuiConformationAlert(String aTitle, String aHeaderText, String aContentText){
        Alert tmpAlert = new Alert(Alert.AlertType.CONFIRMATION);
        tmpAlert.setTitle(aTitle);
        tmpAlert.setHeaderText(aHeaderText);
        tmpAlert.setContentText(aContentText);
        return tmpAlert.showAndWait().orElse(ButtonType.CANCEL);
    }
    //
    /**
     * Creates and shows an alert explicit for exceptions, which contains the stack trace of the given exception in
     * an expandable pane.
     *
     * @param aTitle Title of the exception alert
     * @param aHeaderText Header of the exception alert
     * @param aContentText Text that the exception alert contains
     * @param anException Exception that was thrown
     */
    public static void GuiExceptionAlert(String aTitle, String aHeaderText, String aContentText, Exception anException){
        if(anException == null){
            //TODO: What happens if anException is null? GuiMessageAlert!
            return;
        }
        try{
            Alert tmpAlert = new Alert(Alert.AlertType.ERROR);
            tmpAlert.setTitle(aTitle);
            tmpAlert.setHeaderText(aHeaderText);
            tmpAlert.setContentText(aContentText);
            //Create expandable exception info
            StringWriter tmpStringWriter = new StringWriter();
            PrintWriter tmpPrintWriter = new PrintWriter(tmpStringWriter);
            anException.printStackTrace(tmpPrintWriter);
            String tmpExceptionString = tmpStringWriter.toString();
            Label tmpLabel = new Label(Message.get("Error.ExceptionAlert.Label"));
            TextArea tmpExceptionTextArea = new TextArea(tmpExceptionString);
            tmpExceptionTextArea.setEditable(false);
            tmpExceptionTextArea.setWrapText(true);
            tmpExceptionTextArea.setMaxWidth(Double.MAX_VALUE);
            tmpExceptionTextArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(tmpExceptionTextArea, Priority.ALWAYS);
            GridPane.setHgrow(tmpExceptionTextArea, Priority.ALWAYS);
            GridPane tmpExceptionGridPane = new GridPane();
            tmpExceptionGridPane.setMaxWidth(Double.MAX_VALUE);
            tmpExceptionGridPane.add(tmpLabel, 0, 0);
            tmpExceptionGridPane.add(tmpExceptionTextArea, 0, 1);
            //Add expandable exception info to the dialog/alert pane
            tmpAlert.getDialogPane().setExpandableContent(tmpExceptionGridPane);
            //Show and wait exception alert
            tmpAlert.showAndWait();
        }catch(Exception aNewThrownException){
            GuiMessageAlert(Alert.AlertType.ERROR, Message.get("Error.ExceptionAlert.Title"), Message.get("Error.ExceptionAlert.Header"), aNewThrownException.toString());
            LOGGER.log(Level.SEVERE, aNewThrownException.toString(), aNewThrownException);
        }
    }
    //
    /**
     * Binds height and width property of the child control to the parent pane properties
     *
     * @param aParentPane
     * @param aChildControl
     */
    public static void GuiBindControlSizeToParentPane(Pane aParentPane, Control aChildControl){
        aChildControl.prefHeightProperty().bind(aParentPane.heightProperty());
        aChildControl.prefWidthProperty().bind(aParentPane.widthProperty());
    }
    //
    /**
     * TODO
     * @return
     */
    public static Pattern GetIntegerPattern(){
        return Pattern.compile("-?(([1-9][0-9]*)|0)?");

    }
    //
    /**
     * TODO
     * @return
     */
    public static Pattern GetDoublePattern(){
        return Pattern.compile("-?(([1-9][0-9]*)|0)?(\\.[0-9]*)?");
    }
    //
    /**
     * TODO
     * @return
     */
    public static UnaryOperator<TextFormatter.Change> GetIntegerFilter(){
        return c ->{
            String text = c.getControlNewText();
            if(GetIntegerPattern().matcher(text).matches()) {
                return c;
            } else {
                return null;
            }
        };
    }
    //
    /**
     * TODO
     * @return
     */
    public static UnaryOperator<TextFormatter.Change> GetDoubleFilter(){
        return c ->{
          String text = c.getControlNewText();
          if(GetDoublePattern().matcher(text).matches()) {
              return c;
          } else {
              return null;
          }
        };
    }
    //
    /**
     * TODO
     * @return
     */
    public static StringConverter<Integer> GetStringToIntegerConverter(){
        return new StringConverter<Integer>() {
            @Override
            public String toString(Integer anObject) {
                return anObject.toString();
            }
            @Override
            public Integer fromString(String aString) {
                if(aString.isEmpty() || "-".equals(aString) || ".".equals(aString) || "-.".equals(aString)){
                    return 0;
                }
                else{
                    return Integer.valueOf(aString);
                }
            }
        };
    }
    //
    /**
     * TODO
     * @return
     */
    public static StringConverter<Double> GetStringToDoubleConverter(){
        return new StringConverter<Double>() {
            @Override
            public String toString(Double anObject) {
                return anObject.toString();
            }
            @Override
            public Double fromString(String aString) {
                if(aString.isEmpty() || "-".equals(aString) || ".".equals(aString) || "-.".equals(aString)){
                    return 0.0;
                }
                else {
                    return Double.valueOf(aString);
                }
            }
        };
    }
    //</editor-fold>
}