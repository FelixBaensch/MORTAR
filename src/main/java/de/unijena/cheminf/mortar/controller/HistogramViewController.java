/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2022  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.HistogramView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import de.unijena.cheminf.mortar.model.util.ListUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class for the HistogramView
 *
 * @author Betuel Sevindik
 */
public class HistogramViewController {

    //<editor-fold desc="private final class variables" defaultstate="collapsed">
    /**
     * Stage for the MainView
     */
    private final Stage mainStage;
    /**
     * Logger of this class
     */
    private static final Logger LOGGER = Logger.getLogger(HistogramViewController.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * HistogramView
     */
    private HistogramView histogramView;
    /**
     * Stage of the HistogramView
     */
    private Stage histogramStage;
    /**
     * Copy of list
     */
    private List<MoleculeDataModel> copyList;
    /**
     * Width of the images
     */
    private double imageWidth = GuiDefinitions.GUI_IMAGE_WIDTH;
    /**
     * Height of the images
     */
    private double imageHeight = GuiDefinitions.GUI_IMAGE_HEIGHT;
    /**
     * Zoom factor of the images
     */
    private double imageZoomFactor = GuiDefinitions.GUI_IMAGE_ZOOM_FACTOR;
    /**
     * Y axis of the histogram
     */
    private CategoryAxis categoryAxis;
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param aMainStage        Stage of the MainView
     * @param aMoleculeDataList
     */
    public HistogramViewController(Stage aMainStage, List<MoleculeDataModel> aMoleculeDataList)  {
        this.mainStage = aMainStage;
        this.copyList = new ArrayList<>(aMoleculeDataList);
        this.openHistogramView();
    }
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Initialises stage and view and opens view in the initialised stage
     */
    private void openHistogramView()  {
        this.histogramStage = new Stage();
        this.histogramStage.initModality(Modality.WINDOW_MODAL);
        this.histogramStage.initOwner(this.mainStage);
        this.histogramStage.setTitle(Message.get("HistogramView.title"));
        this.histogramStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.histogramStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        this.histogramView = new HistogramView();
        CheckBox tmpBarLabelCheckBox = this.histogramView.getCheckbox();
        CheckBox tmpGridlineCheckBox = this.histogramView.getGridLinesCheckBox();
        this.categoryAxis = new CategoryAxis();
        BarChart tmpHistogramBarChart =  this.createHistogram(GuiDefinitions.HISTOGRAM_DEFAULT_FRAGMENT_FREQUENCY,
                this.histogramView, GuiDefinitions.HISTOGRAM_DEFAULT_SMILES_LENGTH, tmpBarLabelCheckBox, tmpGridlineCheckBox);
        this.addListener();
        Scene tmpScene = new Scene(this.histogramView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.histogramStage.setScene(tmpScene);
        tmpScene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number tmpOldNumber, Number tmpNewNumber) {
                double tmpWidthChange =((tmpScene.getWidth()-GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE)/GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE)*100;
                double tmpImageWidthChange = (GuiDefinitions.GUI_IMAGE_WIDTH/100)*tmpWidthChange;
                imageWidth = GuiDefinitions.GUI_IMAGE_WIDTH+ tmpImageWidthChange;
                imageHeight = imageWidth-100;
                imageZoomFactor = (GuiDefinitions.GUI_IMAGE_ZOOM_FACTOR/GuiDefinitions.GUI_IMAGE_WIDTH)*imageWidth;
            }
        });
        tmpScene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                double tmpHeightChange =((tmpScene.getHeight()-GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE)/GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE)*100;
                double tmpImageHeightChange = (GuiDefinitions.GUI_IMAGE_HEIGHT/100)*tmpHeightChange;
                if (tmpScene.getWidth() == GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE) {
                    imageHeight = GuiDefinitions.GUI_IMAGE_HEIGHT + tmpImageHeightChange;
                    imageWidth = 100+imageHeight; // TODO ???
                    imageZoomFactor = (GuiDefinitions.GUI_IMAGE_ZOOM_FACTOR/ GuiDefinitions.GUI_IMAGE_WIDTH) * imageWidth;
                } else {
                    double tmpHeight = imageWidth-100; //
                    double tmpIntermediateImageHeight = GuiDefinitions.GUI_IMAGE_HEIGHT+tmpImageHeightChange;
                    double tmpImageHeight = tmpHeight-tmpIntermediateImageHeight;
                    imageHeight = tmpIntermediateImageHeight+tmpImageHeight;
                    imageWidth = 100+imageHeight;
                    imageZoomFactor = (GuiDefinitions.GUI_IMAGE_ZOOM_FACTOR/GuiDefinitions.GUI_IMAGE_WIDTH)*imageWidth;
                }
            }
        });
        this.histogramStage.show();
        this.setMaxBarWidth(GuiDefinitions.GUI_BAR_WIDTH, this.categoryAxis, tmpHistogramBarChart);
    }
    //
    /**
     * Create a configurable histogram
     *
     * @param aNumber to set the fragment number
     * @param aHistogramView to display the images of the fragments
     * @param aSmilesLength to set the SMILES length
     * @return a BarChart to show
     */
    private BarChart createHistogram(int aNumber, HistogramView aHistogramView, int aSmilesLength, CheckBox aBarLabelCheckBox, CheckBox aGridlineCheckBox)  {
        XYChart.Series tmpSeries = new XYChart.Series();
        NumberAxis tmpNumberAxis = new NumberAxis();
        BarChart tmpHistogramBarChart = new BarChart(tmpNumberAxis, this.categoryAxis);
        this.categoryAxis.setTickLabelFill(Color.BLACK);
        this.categoryAxis.setTickMarkVisible(true);
        this.categoryAxis.setTickLength(GuiDefinitions.HISTOGRAM_TICK_LABEL_LENGTH);
        this.categoryAxis.setTickLabelFill(Color.BLACK);
        this.categoryAxis.setTickLabelGap(GuiDefinitions.HISTOGRAM_TICK_LABEL_GAP);
        this.categoryAxis.setLabel(Message.get("HistogramViewController.YAxisLabel.text"));
        tmpHistogramBarChart.setCategoryGap(0);
        tmpHistogramBarChart.setBarGap(0);
        ScrollPane tmpScrollPane = aHistogramView.getScrollPane();
        tmpScrollPane.setContent(tmpHistogramBarChart);
        ListUtil.sortGivenFragmentListByPropertyAndSortType(this.copyList, "absoluteFrequency", "ASCENDING");
        String tmpNewSmiles;
        ArrayList<Double> tmpArrayFrequency = new ArrayList<>();
        ArrayList<String> tmpSmilesList = new ArrayList<>();
        ArrayList<Integer> tmpFrequencyList = new ArrayList<>();
        int tmpIterator = this.copyList.size();
        FragmentDataModel tmpFragmentData = null;
        double tmpMaxOfFrequency;
        ImageView tmpStructureViewer = aHistogramView.getImageStructure();
        ArrayList<String> tmpFullSmilesLength = new ArrayList<>();
        for (MoleculeDataModel tmpMoleculeData : this.copyList) {
             tmpFragmentData = (FragmentDataModel) tmpMoleculeData;
            if (tmpFragmentData.getUniqueSmiles().length() > aSmilesLength) {
                tmpNewSmiles ="SMILES too long ("+tmpIterator+")";
                tmpSmilesList.add(tmpNewSmiles);
            } else {
                tmpNewSmiles = tmpFragmentData.getUniqueSmiles();
                tmpSmilesList.add(tmpNewSmiles);
            }
            tmpIterator--;
            tmpFullSmilesLength.add(tmpFragmentData.getUniqueSmiles());
            tmpFrequencyList.add(tmpFragmentData.getAbsoluteFrequency());
            int tmpAbsolutFragmentFrequency = tmpFragmentData.getAbsoluteFrequency();
            double tmpAbsolutFrequencyInDouble = tmpAbsolutFragmentFrequency;
            tmpArrayFrequency.add(tmpAbsolutFrequencyInDouble);
        }
        tmpMaxOfFrequency = Collections.max(tmpArrayFrequency);
        // set the number axis dynamically  //TODO TEST !!!
        tmpNumberAxis.setAutoRanging(false);
        tmpNumberAxis.setMinorTickCount(1);
        tmpNumberAxis.setForceZeroInRange(true);
        tmpNumberAxis.setTickLabelFill(Color.BLACK);
        tmpNumberAxis.setLabel(Message.get("HistogramViewController.XAxisLabel.text"));
        double tmpXAxisTicks = 5.0/100.0*tmpMaxOfFrequency; // magic number
        double tmpXAxisExtension = 15.0/100.0*tmpMaxOfFrequency; // magic number
        int tmpIntTmpXAxisTicks = (int) tmpXAxisTicks;
        int tmpIntXAxisExtension = (int) tmpXAxisExtension;
        if(tmpIntTmpXAxisTicks == 0 || tmpIntXAxisExtension == 0) {
           tmpNumberAxis.setTickUnit(1);
           tmpNumberAxis.setUpperBound(tmpMaxOfFrequency+1);
       }
       else {
           tmpNumberAxis.setTickUnit(tmpIntTmpXAxisTicks);
           int tmpAdjustedAxis;
           for(tmpAdjustedAxis = (int) (tmpIntXAxisExtension+tmpMaxOfFrequency); tmpAdjustedAxis % 5 != 0; tmpAdjustedAxis++) {
           }
           tmpNumberAxis.setUpperBound(tmpAdjustedAxis);
       }
        List<String> tmpSublistSmiles = null;
        List<Integer> tmpSublistOfFrequency = null;
        List<String> tmpSmilesToDepict = null;
        if (aNumber == 0) {
            tmpSublistSmiles = tmpSmilesList.subList(aNumber, tmpSmilesList.size());
            tmpSublistOfFrequency = tmpFrequencyList.subList(aNumber, tmpFrequencyList.size());
            tmpSmilesToDepict = tmpFullSmilesLength.subList(aNumber, tmpFullSmilesLength.size());
            Label tmpDisplayMaxFrequency = aHistogramView.getDefaultLabel();
            tmpDisplayMaxFrequency.setText("Fragment frequency (max."+this.copyList.size()+"):");
        }
        else {
            try {
                if (aNumber <= tmpSmilesList.size()) {
                    tmpSublistSmiles = tmpSmilesList.subList(tmpSmilesList.size() - aNumber, tmpSmilesList.size());
                    tmpSublistOfFrequency = tmpFrequencyList.subList(tmpFrequencyList.size() - aNumber, tmpFrequencyList.size());
                    tmpSmilesToDepict = tmpFullSmilesLength.subList(tmpFullSmilesLength.size()- aNumber, tmpFullSmilesLength.size());
                } else {
                    throw new IllegalArgumentException("the given number exceeds the maximum number of fragments");
                }
            } catch (IllegalArgumentException anException) {
                HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                GuiUtil.guiExceptionAlert(Message.get("HistogramViewController.HistogramRefreshError.Title"), Message.get("HistogramViewController.HistogramError.Header"),
                        Message.get("HistogramViewController.HistogramError.Content"), anException);
            }
        }
        int tmpDigitMaxFrequency = (int) tmpMaxOfFrequency;
        for (Iterator tmpStringIterator = tmpSublistSmiles.iterator(), tmpIntegerIterator = tmpSublistOfFrequency.iterator(), tmpSmilesIterator = tmpSmilesToDepict.iterator(); tmpStringIterator.hasNext() && tmpIntegerIterator.hasNext() && tmpSmilesIterator.hasNext();) {
            Integer tmpCurrentFrequency = (Integer) tmpIntegerIterator.next();
            String tmpCurrentSmiles = (String) tmpStringIterator.next();
            String tmpSmiles = (String) tmpSmilesIterator.next();
            XYChart.Data<Number, String> tmpStringNumberData = new XYChart.Data(tmpCurrentFrequency, tmpCurrentSmiles);
            StackPane tmpNode = this.histogramHover(tmpStructureViewer, tmpSmiles);
            tmpStringNumberData.setNode(tmpNode);
            tmpNode.setStyle("-fx-bar-fill: #0000ff");
            this.getFrequencyDigit(tmpDigitMaxFrequency);
            this.getBar(aBarLabelCheckBox, tmpNode, tmpCurrentFrequency, this.getFrequencyDigit(tmpDigitMaxFrequency));
            tmpSeries.getData().add(tmpStringNumberData);
        }
        double tmpHistogramSize = GuiDefinitions.GUI_HISTOGRAM_GROWTH_VALUE*tmpSublistOfFrequency.size();
        tmpHistogramBarChart.setPrefHeight(tmpHistogramSize);
        tmpHistogramBarChart.setMinHeight(tmpHistogramSize);
        tmpHistogramBarChart.getData().add(tmpSeries);
        tmpHistogramBarChart.setLegendVisible(false);
        tmpHistogramBarChart.setAnimated(false);
        tmpHistogramBarChart.setHorizontalGridLinesVisible(false);
        tmpHistogramBarChart.setVerticalGridLinesVisible(false);
        if(aGridlineCheckBox.isSelected()) {
            tmpHistogramBarChart.setVerticalGridLinesVisible(true);
            tmpHistogramBarChart.setHorizontalGridLinesVisible(true);
        }
        aGridlineCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (aGridlineCheckBox.isSelected()) {
                tmpHistogramBarChart.setVerticalGridLinesVisible(true);
                tmpHistogramBarChart.setHorizontalGridLinesVisible(true);
            } else {
                tmpHistogramBarChart.setVerticalGridLinesVisible(false);
                tmpHistogramBarChart.setHorizontalGridLinesVisible(false);
            }
        });
       return tmpHistogramBarChart;
    }
    //
    /**
     * Add listeners
     *
     */
    private void addListener() {
        this.histogramView.getCancelButton().setOnAction(event -> {
            this.histogramStage.close();
        });
        TextField tmpFrequencyTextField = this.histogramView.getFrequencyTextField();
        TextField tmpSmilesTextField = this.histogramView.getSmilesTextField();
            this.histogramView.getRefreshButton().disableProperty().bind(
                    Bindings.isEmpty(tmpFrequencyTextField.textProperty()).and(Bindings.isEmpty(tmpSmilesTextField.textProperty()))
            );
        this.histogramView.getRefreshButton().setOnAction(event -> {
            try {
                BarChart tmpRefreshBarChart;
                String tmpStringMaxFrequency = this.histogramView.getTextField();
                String tmpMaxSmilesLength = this.histogramView.getSmilesField();
                CheckBox tmpBarLabelCheckBox = this.histogramView.getCheckbox();
                CheckBox tmpGridlinesCheckBox = this.histogramView.getGridLinesCheckBox();
                int tmpFragmentNumber;
                int tmpSmilesLengthInField;
                if (tmpMaxSmilesLength.isEmpty()) {
                    tmpFragmentNumber = Integer.parseInt(tmpStringMaxFrequency);
                    if (tmpFragmentNumber > this.copyList.size() || tmpFragmentNumber <= 0 ) {
                        GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                                Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Header"),
                                Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Content"));
                        return;
                    }
                    tmpSmilesLengthInField = GuiDefinitions.HISTOGRAM_DEFAULT_SMILES_LENGTH;
                } else if (tmpStringMaxFrequency.isEmpty()) {
                    tmpSmilesLengthInField = Integer.parseInt(tmpMaxSmilesLength);
                    if (tmpSmilesLengthInField < 0) {
                        GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                                Message.get("HistogramViewController.HistogramSmilesRefreshWarning.Header"),
                                Message.get("HistogramViewController.HistogramSmilesRefreshWarning.Content"));
                        return;
                    }
                    tmpFragmentNumber = GuiDefinitions.HISTOGRAM_DEFAULT_FRAGMENT_FREQUENCY;

                } else {
                    tmpFragmentNumber = Integer.parseInt(tmpStringMaxFrequency);
                    tmpSmilesLengthInField = Integer.parseInt(tmpMaxSmilesLength);
                    if (tmpFragmentNumber > this.copyList.size() || tmpFragmentNumber < 0 || tmpSmilesLengthInField < 0) {
                        GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                                Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Header"),
                                Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Content"));
                        return;
                    }
                }
                tmpRefreshBarChart=  this.createHistogram(tmpFragmentNumber, this.histogramView, tmpSmilesLengthInField, tmpBarLabelCheckBox, tmpGridlinesCheckBox);
                tmpRefreshBarChart.setCategoryGap(this.getBarSpacing(tmpFragmentNumber));
            } catch (NumberFormatException anException) {
                HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                GuiUtil.guiExceptionAlert(Message.get("HistogramViewController.HistogramRefreshError.Title"), Message.get
                                ("HistogramViewController.HistogramRefreshError.Header"),
                        Message.get("HistogramViewController.HistogramRefreshError.Content"), anException);
            }
        });
    }
    //
    /**
     * Make the histogram hoverable
     *
     * @param anImageView to display the structures
     * @return a StackPane that is used as a node
     */
    private StackPane histogramHover(ImageView anImageView, String aSmiles) {
        StackPane tmpNodePane = new StackPane();
        tmpNodePane.setAlignment(Pos.CENTER_RIGHT); // TODO better position
        MenuItem tmpContextMenuItem = new MenuItem(Message.get("HistogramViewController.MenuItem.text"));
        ContextMenu tmpContextMenu = new ContextMenu();
        tmpContextMenu.getItems().add(tmpContextMenuItem);
        Label tmpContextMenuLabel = new Label();
        tmpContextMenuItem.setGraphic(new ImageView(new Image("de/unijena/cheminf/mortar/images/copy_icon_16x16.png")));
        tmpContextMenuLabel.setContextMenu(tmpContextMenu);
        tmpNodePane.getChildren().add(tmpContextMenuLabel);
        boolean tmpKeepAtomContainer = true;
        tmpNodePane.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                tmpNodePane.setStyle("-fx-bar-fill: #00008b");
                tmpContextMenuLabel.setPrefWidth(tmpNodePane.getWidth());
                tmpContextMenuLabel.setMaxWidth(tmpNodePane.getWidth());
                tmpContextMenuLabel.setMinWidth(tmpNodePane.getWidth());
                SmilesParser tmpSmiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
                IAtomContainer tmpAtomCon = null;
                if (tmpKeepAtomContainer) {
                    if (tmpAtomCon == null) {
                        try {
                            tmpSmiPar.kekulise(false);
                            tmpAtomCon = tmpSmiPar.parseSmiles(aSmiles);
                            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomCon);
                            Kekulization.kekulize(tmpAtomCon);
                        } catch (CDKException anException) {
                            HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                        }
                    }
                    Image tmpStructureImage = DepictionUtil.depictImageWithZoom(tmpAtomCon, imageZoomFactor, imageWidth, imageHeight);
                    anImageView.setImage(tmpStructureImage);
                    tmpNodePane.setCursor(Cursor.HAND);
                }
                try {
                //kekulization done separately below
                    tmpSmiPar.kekulise(false);
                    IAtomContainer tmpAtomContainer = tmpSmiPar.parseSmiles(aSmiles);
                    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomContainer);
                    Kekulization.kekulize(tmpAtomContainer);
                    Image image = DepictionUtil.depictImageWithZoom(tmpAtomContainer, imageZoomFactor,imageWidth, imageHeight);
                    anImageView.setImage(image);
                    tmpNodePane.setCursor(Cursor.HAND);
                } catch (CDKException anException) {
                   HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                }
            }
        });
        // Listener ContextMenuItem
        tmpContextMenuItem.setOnAction(event -> {
            ClipboardContent tmpClipboardContent = new ClipboardContent();
            tmpClipboardContent.putString(aSmiles);
            Clipboard.getSystemClipboard().setContent(tmpClipboardContent);
                });
        tmpNodePane.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                tmpNodePane.setStyle("-fx-bar-fill: #0000ff");
                anImageView.setImage(null);
                tmpNodePane.setCursor(Cursor.HAND);
            }
        });
        tmpContextMenuLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (MouseButton.SECONDARY.equals(mouseEvent.getButton())) {
                    tmpContextMenuLabel.setStyle("-fx-bar-fill: #00008b");
                    tmpContextMenu.show(tmpContextMenuLabel, tmpNodePane.getWidth() / 2, tmpNodePane.getHeight());
                } else {
                    return;
                }
            }
        });
        return tmpNodePane;
    }
    //
    /**
     * Make the bar width in the histogram uniform
     *
     * @param aMaxBarWidth sets the bar widths
     * @param aXAxis
     * @param aBarChart
     * @author José Pereda, Betuel Sevindik
     * @source https://stackoverflow.com/questions/27302875/set-bar-chart-column-width-size
     */
    private double setMaxBarWidth(double aMaxBarWidth, CategoryAxis aXAxis, BarChart aBarChart) {
        double tmpBarWidth = 0;
        do {
            double tmpCategorySpacing = aXAxis.getCategorySpacing();
            double tmpAvailableBarSpace = tmpCategorySpacing - (aBarChart.getCategoryGap() + aBarChart.getBarGap());
            tmpBarWidth = (tmpAvailableBarSpace / aBarChart.getData().size()) - aBarChart.getBarGap();
            if (tmpBarWidth > aMaxBarWidth) {
                tmpAvailableBarSpace = (aMaxBarWidth + aBarChart.getBarGap()) * aBarChart.getData().size();
                aBarChart.setCategoryGap(tmpCategorySpacing - tmpAvailableBarSpace - aBarChart.getBarGap());
            }
        } while (tmpBarWidth > aMaxBarWidth);
        do{
            double  tmpCategorySpacing = aXAxis.getCategorySpacing();
            double tmpAvailableBarSpace  = tmpCategorySpacing - (0 + aBarChart.getBarGap());
            tmpBarWidth = Math.min(aMaxBarWidth, (tmpAvailableBarSpace / aBarChart.getData().size()) - aBarChart.getBarGap());
            tmpAvailableBarSpace=(tmpBarWidth + aBarChart.getBarGap())* aBarChart.getData().size();
            aBarChart.setCategoryGap(tmpCategorySpacing-tmpAvailableBarSpace-aBarChart.getBarGap());
        } while(tmpBarWidth< aMaxBarWidth && aBarChart.getCategoryGap()>0);
        return tmpBarWidth;
    }
    //
    /**
     * Enables the labelling of the histogram
     *
     * @param aCheckBox to make the display of the fragment labels adjustable
     * @param aStackPane
     * @param aFrequency values of the frequencies
     * @param aDigitNumber
     * @return StackPane
     */
    private StackPane getBar(CheckBox aCheckBox, StackPane aStackPane, int aFrequency, double aDigitNumber) {
        Label tmpBarLabel = new Label();
        tmpBarLabel.setTranslateY(0);
        tmpBarLabel.setAlignment(Pos.CENTER_RIGHT);
        tmpBarLabel.setPrefWidth(GuiDefinitions.GUI_BAR_LABEL_SIZE*aDigitNumber);
        tmpBarLabel.setMinWidth(GuiDefinitions.GUI_BAR_LABEL_SIZE*aDigitNumber);
        tmpBarLabel.setMaxWidth(GuiDefinitions.GUI_BAR_LABEL_SIZE*aDigitNumber);
        // TODO TEST
        tmpBarLabel.setTranslateX(aDigitNumber*GuiDefinitions.GUI_BAR_LABEL_SIZE);
        tmpBarLabel.setText(String.valueOf(aFrequency));
        if(aCheckBox.isSelected()) {
            aStackPane.getChildren().add(tmpBarLabel);
        }
        aCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (aCheckBox.isSelected()) {
                aStackPane.getChildren().add(tmpBarLabel);
            } else {
                aStackPane.getChildren().clear();
            }
        });
        return aStackPane;
    }
    //
    /**
     * Sets a fixed bar width
     *
     * @param aNumber of fragments displayed
     * @return the gap between the bars
     */
    private double getBarSpacing(int aNumber) {
        double tmpHistogramHeight;
        double tmpGapDeviation;
        double tmpGapSpacing;
        double tmpCategoryGap;
        if (aNumber <= 10) {
            tmpHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT/aNumber;
            tmpGapDeviation = (tmpHistogramHeight/GuiDefinitions.GUI_HISTOGRAM_CALCULATED_BAR_GAP)*GuiDefinitions.GUI_HISTOGRAM_HEIGHT_DEVIATION;
            tmpGapSpacing = tmpHistogramHeight-tmpGapDeviation;
            tmpCategoryGap = tmpGapSpacing-GuiDefinitions.GUI_BAR_WIDTH;
        } else if (aNumber <= 14) {
            tmpHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT/aNumber;
            tmpGapDeviation = GuiDefinitions.GUI_HISTOGRAM_HEIGHT_DEVIATION/(GuiDefinitions.GUI_HISTOGRAM_CALCULATED_BAR_GAP/tmpHistogramHeight);
            tmpGapSpacing = tmpHistogramHeight-tmpGapDeviation;
            tmpCategoryGap = tmpGapSpacing-GuiDefinitions.GUI_BAR_WIDTH;
        } else {
            tmpHistogramHeight = GuiDefinitions.GUI_HISTOGRAM_GROWTH_VALUE*aNumber-84;
            tmpGapSpacing = tmpHistogramHeight/aNumber;
            tmpCategoryGap = tmpGapSpacing-GuiDefinitions.GUI_BAR_WIDTH;
        }
        return tmpCategoryGap;
    }
    //
    /**
     * Method to find out the number of digits displayed in the BarLabel
     *
     * @param aDigit max. value of the frequency
     * @return number of digits
     */
    private int getFrequencyDigit(int aDigit) {
        int tmpCountDigits = 0;
        while (aDigit!= 0) {
            aDigit = aDigit/10;
            ++tmpCountDigits;
        }
        return tmpCountDigits;
    }
    //</editor-fold>
    //
}








