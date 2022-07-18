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
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.*;
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
     * Y-axis of the histogram
     */
    private CategoryAxis categoryAxis;
    /**
     * Manipulated SMILES list
     */
    private List<String> sublistSmiles;
    /**
     * Manipulated list used to generate the structure images
     */
    private List<String> smilesToDepict;
    /**
     * Manipulated frequency list
     */
    private List<Double> sublistOfFrequency;
    private int i;
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
        this.histogramView = new HistogramView(this.copyList.size());
        CheckBox tmpBarLabelCheckBox = this.histogramView.getCheckbox();
        CheckBox tmpGridlineCheckBox = this.histogramView.getGridLinesCheckBox();
        CheckBox tmpLogarithmicScale = this.histogramView.getLogarithmicScale();
        CheckBox tmpSmilesTickLabel = this.histogramView.getSmilesTickLabel();
        ComboBox tmpGapHistogram = this.histogramView.getComboBox();
        this.categoryAxis = new CategoryAxis();
        InputStream tmpImageInputStream = HistogramViewController.class.getResourceAsStream("/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png");
        this.histogramStage.getIcons().add(new Image(tmpImageInputStream));
        TextField tmpFrequencyField = this.histogramView.getFrequencyTextField();
        BarChart tmpHistogramBarChart;
        if(this.copyList.size()>=30) {
            tmpFrequencyField.setText("30");
            ArrayList<Double> tmpDefaultBarGap = this.getBarSpacing(30, tmpGapHistogram);
            tmpHistogramBarChart = this.createHistogram(GuiDefinitions.HISTOGRAM_DEFAULT_FRAGMENT_FREQUENCY,
                    this.histogramView, GuiDefinitions.HISTOGRAM_DEFAULT_SMILES_LENGTH, tmpBarLabelCheckBox, tmpGridlineCheckBox, tmpDefaultBarGap.get(0), tmpLogarithmicScale, tmpSmilesTickLabel);
            tmpHistogramBarChart.setCategoryGap(tmpDefaultBarGap.get(1));
        } else {
            tmpFrequencyField.setText(String.valueOf(this.copyList.size()));
            ArrayList<Double> tmpDefaultBarGap = this.getBarSpacing(this.copyList.size(), tmpGapHistogram);
            tmpHistogramBarChart = this.createHistogram(this.copyList.size(),
                    this.histogramView, GuiDefinitions.HISTOGRAM_DEFAULT_SMILES_LENGTH, tmpBarLabelCheckBox, tmpGridlineCheckBox, tmpDefaultBarGap.get(0), tmpLogarithmicScale, tmpSmilesTickLabel);
           tmpHistogramBarChart.setCategoryGap(tmpDefaultBarGap.get(1));
        }
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
                    double tmpHeight = imageWidth-100; // TODO Test
                    double tmpIntermediateImageHeight = GuiDefinitions.GUI_IMAGE_HEIGHT+tmpImageHeightChange;
                    double tmpImageHeight = tmpHeight-tmpIntermediateImageHeight;
                    imageHeight = tmpIntermediateImageHeight+tmpImageHeight;
                    imageWidth = 100+imageHeight;
                    imageZoomFactor = (GuiDefinitions.GUI_IMAGE_ZOOM_FACTOR/GuiDefinitions.GUI_IMAGE_WIDTH)*imageWidth;
                }
            }
        });
        this.histogramStage.show();
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
    private BarChart createHistogram(int aNumber, HistogramView aHistogramView, int aSmilesLength, CheckBox aBarLabelCheckBox,
                                     CheckBox aGridlineCheckBox, double aHistogramDefaultSize, CheckBox aLograithmicScale, CheckBox aSmilesTickLabel) {
        XYChart.Series tmpSeries = new XYChart.Series();
        NumberAxis tmpNumberAxis = new NumberAxis();
        BarChart tmpHistogramBarChart = new BarChart(tmpNumberAxis, this.categoryAxis);
        this.categoryAxis.setTickLabelFill(Color.BLACK);
        this.categoryAxis.setTickLength(GuiDefinitions.HISTOGRAM_TICK_LABEL_LENGTH);
        tmpNumberAxis.setSide(Side.TOP);
        this.categoryAxis.setTickLabelGap(GuiDefinitions.HISTOGRAM_TICK_LABEL_GAP);
        this.categoryAxis.setLabel(Message.get("HistogramViewController.YAxisLabel.text"));
        this.getTickLabelsVisible(aSmilesTickLabel);
        tmpHistogramBarChart.setCategoryGap(0);
        tmpHistogramBarChart.setBarGap(0);
        ScrollPane tmpScrollPane = aHistogramView.getScrollPane();
        tmpScrollPane.setContent(tmpHistogramBarChart);
        ListUtil.sortGivenFragmentListByPropertyAndSortType(this.copyList, "absoluteFrequency", "ASCENDING");
        String tmpNewSmiles;
        ArrayList<String> tmpSmilesList = new ArrayList<>();
        ArrayList<Double> tmpFrequencyList = new ArrayList<>();
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
            tmpFrequencyList.add((double) tmpFragmentData.getAbsoluteFrequency());
        }
        tmpMaxOfFrequency = Collections.max(tmpFrequencyList);
        // set the number axis dynamically  //TODO TEST !!!
        tmpNumberAxis.setAutoRanging(false);
        tmpNumberAxis.setMinorTickCount(1);
        tmpNumberAxis.setForceZeroInRange(true);
        tmpNumberAxis.setTickLabelFill(Color.BLACK);
        tmpNumberAxis.setLabel(Message.get("HistogramViewController.XAxisLabel.text"));
        double tmpXAxisTicks = 5.0/100.0*tmpMaxOfFrequency; // magic number
        double tmpXAxisExtension = 15.0/100.0*tmpMaxOfFrequency; // magic number
        int tmpIntTmpXAxisTicks = (int) Math.round(tmpXAxisTicks);
        int tmpIntXAxisExtension = (int) Math.round(tmpXAxisExtension);
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
       /**
        List<String> tmpSublistSmiles = null;
        List<Integer> tmpSublistOfFrequency = null;
        List<String> tmpSmilesToDepict = null;
        */
        Label tmpDisplayFrequency = aHistogramView.getDefaultLabel();
        if (aNumber <= tmpSmilesList.size()) {
            this.sublistSmiles = tmpSmilesList.subList(tmpSmilesList.size() - aNumber, tmpSmilesList.size());
            this.sublistOfFrequency = tmpFrequencyList.subList(tmpFrequencyList.size() - aNumber, tmpFrequencyList.size());
            this.smilesToDepict = tmpFullSmilesLength.subList(tmpFullSmilesLength.size()- aNumber, tmpFullSmilesLength.size());
            tmpDisplayFrequency.setText("Display Fragments:");
        } else {
            throw new IllegalArgumentException("the given number exceeds the maximum number of fragments");
        }
        // int tmpDigitMaxFrequency = (int) tmpMaxOfFrequency;
        List<Double> tmpFrequencyCopy = this.sublistOfFrequency;
        List<Double> tmpLogList = new ArrayList<>();
        for(double tmpSublistFrequency : this.sublistOfFrequency) {
            double tmpLogarithmicFrequency = Math.log10(tmpSublistFrequency);
            tmpLogList.add(tmpLogarithmicFrequency);
        }
        if(aLograithmicScale.isSelected()) {
            tmpNumberAxis.setLabel(Message.get("HistogramViewController.LogarithmicXAxisLabel.text"));
            tmpNumberAxis.setTickUnit(0.25);
            tmpNumberAxis.setUpperBound(Math.log10(tmpMaxOfFrequency)+1);
            this.sublistOfFrequency = tmpLogList;
        }
        aLograithmicScale.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (aLograithmicScale.isSelected()) {
                tmpNumberAxis.setLabel(Message.get("HistogramViewController.LogarithmicXAxisLabel.text"));
                tmpNumberAxis.setTickUnit(0.25);
                tmpNumberAxis.setUpperBound(Math.log10(tmpMaxOfFrequency)+1);
                this.sublistOfFrequency = tmpLogList;
                this.createXYChartSeries(tmpStructureViewer,aBarLabelCheckBox, tmpMaxOfFrequency, tmpSeries);
                tmpHistogramBarChart.setAnimated(false);
            } else {
                if(tmpIntTmpXAxisTicks == 0 || tmpIntXAxisExtension == 0) {
                    tmpNumberAxis.setTickUnit(1);
                    tmpNumberAxis.setUpperBound(tmpMaxOfFrequency+1);
                }
                else {
                    tmpNumberAxis.setLabel(Message.get("HistogramViewController.XAxisLabel.text"));
                    tmpNumberAxis.setTickUnit(tmpIntTmpXAxisTicks);
                    int tmpAdjustedAxis;
                    for(tmpAdjustedAxis = (int) (tmpIntXAxisExtension+tmpMaxOfFrequency); tmpAdjustedAxis % 5 != 0; tmpAdjustedAxis++) {
                    }
                    tmpNumberAxis.setUpperBound(tmpAdjustedAxis);
                }
                this.sublistOfFrequency = tmpFrequencyCopy;
                this.createXYChartSeries(tmpStructureViewer,aBarLabelCheckBox, tmpMaxOfFrequency, tmpSeries);
            }
        });
        this.createXYChartSeries(tmpStructureViewer,aBarLabelCheckBox, tmpMaxOfFrequency, tmpSeries);
        double tmpHistogramSize = aHistogramDefaultSize * sublistOfFrequency.size();
        tmpHistogramBarChart.setPrefHeight(tmpHistogramSize);
        tmpHistogramBarChart.setMinHeight(tmpHistogramSize);
        tmpHistogramBarChart.getData().add(tmpSeries);
        tmpHistogramBarChart.setLegendVisible(false);
        tmpHistogramBarChart.layout();
        tmpHistogramBarChart.setHorizontalGridLinesVisible(false);
        tmpHistogramBarChart.setVerticalGridLinesVisible(false);
        tmpHistogramBarChart.setAnimated(false);
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
         this.getIntegerFilter(tmpFrequencyTextField);

        TextField tmpSmilesTextField = this.histogramView.getSmilesTextField();
        this.getIntegerFilter(tmpSmilesTextField);
            this.histogramView.getRefreshButton().disableProperty().bind(
                    Bindings.isEmpty(tmpFrequencyTextField.textProperty()).and(Bindings.isEmpty(tmpSmilesTextField.textProperty()))
            );
        this.histogramView.getRefreshButton().setOnAction(event -> {
                String tmpStringMaxFrequency = this.histogramView.getTextField();
                String tmpMaxSmilesLength = this.histogramView.getSmilesField();
                CheckBox tmpBarLabelCheckBox = this.histogramView.getCheckbox();
                CheckBox tmpGridlinesCheckBox = this.histogramView.getGridLinesCheckBox();
                CheckBox tmpLogCheckBox = this.histogramView.getLogarithmicScale();
                CheckBox tmpTickCheckBox = this.histogramView.getSmilesTickLabel();
                ComboBox tmpComboBox = this.histogramView.getComboBox();
                int tmpFragmentNumber;
                int tmpSmilesLengthInField;
                if (tmpMaxSmilesLength.isEmpty()) {
                    tmpFragmentNumber = Integer.parseInt(tmpStringMaxFrequency);
                    if (tmpFragmentNumber > this.copyList.size()) {
                        GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                                Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Header"),
                                Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Content"));
                        return;
                    }
                    tmpSmilesLengthInField = GuiDefinitions.HISTOGRAM_DEFAULT_SMILES_LENGTH;
                } else if (tmpStringMaxFrequency.isEmpty()) {
                    tmpSmilesLengthInField = Integer.parseInt(tmpMaxSmilesLength);
                    if(this.copyList.size()>=30) {
                        tmpFragmentNumber = 30;
                    } else {
                        tmpFragmentNumber = this.copyList.size();
                    }
                } else {
                    tmpFragmentNumber = Integer.parseInt(tmpStringMaxFrequency);
                    tmpSmilesLengthInField = Integer.parseInt(tmpMaxSmilesLength);
                    if (tmpFragmentNumber > this.copyList.size()) {
                        GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                                Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Header"),
                                Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Content"));
                        return;
                    }
                }
                ArrayList<Double> tmpHistogramSizeGap = this.getBarSpacing(tmpFragmentNumber, tmpComboBox);
                BarChart tmpRefreshBarChart=  this.createHistogram(tmpFragmentNumber, this.histogramView, tmpSmilesLengthInField, tmpBarLabelCheckBox, tmpGridlinesCheckBox, tmpHistogramSizeGap.get(0), tmpLogCheckBox, tmpTickCheckBox);
                tmpRefreshBarChart.setCategoryGap(tmpHistogramSizeGap.get(1));
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
        tmpContextMenuLabel.setPrefWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL);
        tmpContextMenuLabel.setMaxWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL);
        tmpContextMenuLabel.setMinWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL);
        tmpContextMenuLabel.setTranslateX(20);
        tmpContextMenuItem.setGraphic(new ImageView(new Image("de/unijena/cheminf/mortar/images/copy_icon_16x16.png")));
        tmpContextMenuLabel.setContextMenu(tmpContextMenu);
        tmpNodePane.getChildren().addAll(tmpContextMenuLabel);
        boolean tmpKeepAtomContainer = true;
        tmpNodePane.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                tmpNodePane.setStyle("-fx-bar-fill: #00008b");
                if(tmpNodePane.getWidth() >= 10) {
                    tmpContextMenuLabel.setPrefWidth(tmpNodePane.getWidth());
                    tmpContextMenuLabel.setMaxWidth(tmpNodePane.getWidth());
                    tmpContextMenuLabel.setMinWidth(tmpNodePane.getWidth());
                    tmpContextMenuLabel.setTranslateX(0);
                } else {
                    tmpContextMenuLabel.setPrefWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL_NEW);
                    tmpContextMenuLabel.setMaxWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL_NEW);
                    tmpContextMenuLabel.setMinWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL_NEW);
                    tmpContextMenuLabel.setTranslateX(20);
                }
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
                }
                try {
                //kekulization done separately below
                    tmpSmiPar.kekulise(false);
                    IAtomContainer tmpAtomContainer = tmpSmiPar.parseSmiles(aSmiles);
                    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomContainer);
                    Kekulization.kekulize(tmpAtomContainer);
                    Image tmpImage = DepictionUtil.depictImageWithZoom(tmpAtomContainer,imageZoomFactor,imageWidth, imageHeight);
                    anImageView.setImage(tmpImage);
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
                tmpNodePane.setStyle("-fx-bar-fill: #1E90FF");
                anImageView.setImage(null);
            }
        });
            tmpNodePane.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
                @Override
                public void handle(ContextMenuEvent contextMenuEvent) {
                    tmpContextMenu.show(tmpNodePane, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()); // versuch label width und height
                }
            });
            /**
        tmpContextMenuLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (MouseButton.SECONDARY.equals(mouseEvent.getButton())) {
                    tmpContextMenuLabel.setStyle("-fx-bar-fill: #00008b");
                        // tmpContextMenuLabel.setTranslateX(50);
                        tmpContextMenu.show(tmpContextMenuLabel, tmpNodePane.getWidth() / 2, tmpNodePane.getHeight());
                } else {
                    return;
                }
            }
        });
             */
        return tmpNodePane;
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
    private StackPane getBar(CheckBox aCheckBox, StackPane aStackPane, double aFrequency, double aDigitNumber) {
        Label tmpBarLabel = new Label();
        tmpBarLabel.setTranslateY(0);
        tmpBarLabel.setAlignment(Pos.CENTER_RIGHT);
        tmpBarLabel.setPrefWidth(GuiDefinitions.GUI_BAR_LABEL_SIZE*aDigitNumber);
        tmpBarLabel.setMinWidth(GuiDefinitions.GUI_BAR_LABEL_SIZE*aDigitNumber);
        tmpBarLabel.setMaxWidth(GuiDefinitions.GUI_BAR_LABEL_SIZE*aDigitNumber);
        // TODO TEST
       // tmpBarLabel.setTranslateX(aDigitNumber*GuiDefinitions.GUI_BAR_LABEL_SIZE);
        DecimalFormat tmpDecimalFormat1 = new DecimalFormat("0.00");
        DecimalFormat tmpDecimalFormat2 = new DecimalFormat("0");
        if(aFrequency%1.0d==0.0d) {
            tmpBarLabel.setText( tmpDecimalFormat2.format(aFrequency));
            tmpBarLabel.setTranslateX((aDigitNumber-2) * GuiDefinitions.GUI_BAR_LABEL_SIZE);
        }
        else {
            tmpBarLabel.setText(tmpDecimalFormat1.format(aFrequency));
            tmpBarLabel.setTranslateX((aDigitNumber-1) * GuiDefinitions.GUI_BAR_LABEL_SIZE);
        }
        if(aCheckBox.isSelected()) {
           aStackPane.getChildren().add(tmpBarLabel);
        }
        aCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (aCheckBox.isSelected()) {
                aStackPane.getChildren().add(tmpBarLabel);
            } else {
                aStackPane.getChildren().remove(tmpBarLabel);
            }
        });
        return aStackPane;
    }
    //
    /**
     * Method to create the bar gaps
     *
     * @param aNumber fragment frequency
     * @param aComboBox
     * @return ArrayList
     */
    private ArrayList getBarSpacing(int aNumber, ComboBox aComboBox){
        ArrayList<Double> tmpHistogramList = new ArrayList<>();
        double tmpCurrentHistogramHeight;
        double tmpGapDeviation;
        double tmpGapSpacing;
        double tmpCategoryGap = 0;
        double tmpFinalHistogramHeight = 0;
        double tmpFinalGapSpacing;
        String tmpValue = (String) aComboBox.getValue();
        switch (tmpValue) {
            case "Small":
                if (aNumber <= 26) {
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT/aNumber;
                    tmpGapDeviation = tmpCurrentHistogramHeight/(GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT/26);
                    tmpGapSpacing = GuiDefinitions.GUI_HISTOGRAM_SMALL_BAR_GAP_CONST*tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight-tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing-GuiDefinitions.GUI_HISTOGRAM_SMALL_BAR_WIDTH;  //GuiDefinitions.GUI_BAR_WIDTH
                } else {
                    tmpFinalHistogramHeight = GuiDefinitions.GUI_HISTOGRAM_SMALL_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight*aNumber-84;
                    tmpGapSpacing = tmpCurrentHistogramHeight/aNumber;
                    tmpCategoryGap = tmpGapSpacing-GuiDefinitions.GUI_HISTOGRAM_SMALL_BAR_WIDTH;
                }
                break;
            case "Medium":
                if(aNumber <= 19) {
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT/aNumber;
                    tmpGapDeviation = tmpCurrentHistogramHeight/(GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT/19);
                    tmpGapSpacing = GuiDefinitions.GUI_HISTOGRAM_MEDIUM_BAR_GAP_CONST*tmpGapDeviation; //4.442
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight-tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing-GuiDefinitions.GUI_HISTOGRAM_MEDIUM_BAR_WIDTH;  //GuiDefinitions.GUI_BAR_WIDTH
                } else {
                    tmpFinalHistogramHeight =GuiDefinitions.GUI_HISTOGRAM_MEDIUM_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight*aNumber-84; //84
                    tmpGapSpacing = tmpCurrentHistogramHeight/aNumber;
                    tmpCategoryGap = tmpGapSpacing-GuiDefinitions.GUI_HISTOGRAM_MEDIUM_BAR_WIDTH ;
                }
                break;
            case "Large":
                if( aNumber <= 14) {
                   // tmpFinalHistogramHeight = 50;
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / aNumber;
                    tmpGapDeviation = tmpCurrentHistogramHeight/(GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT/14);
                    tmpGapSpacing =GuiDefinitions.GUI_HISTOGRAM_LARGE_BAR_GAP_CONST*tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight-tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing-GuiDefinitions.GUI_HISTOGRAM_LARGE_BAR_WIDTH;
                } else {
                    tmpFinalHistogramHeight = GuiDefinitions.GUI_HISTOGRAM_LARGE_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight*aNumber-85;
                    tmpGapSpacing = tmpCurrentHistogramHeight/aNumber;
                    tmpCategoryGap = tmpGapSpacing-GuiDefinitions.GUI_HISTOGRAM_LARGE_BAR_WIDTH;
                }
                break;
        }
        tmpHistogramList.add(tmpFinalHistogramHeight);
        tmpHistogramList.add(tmpCategoryGap);
        return tmpHistogramList;
    }
    private void getIntegerFilter(TextField aField) {
        aField.setTextFormatter(new TextFormatter<Integer>(c -> {
            String tmpText = c.getControlNewText();
            if (tmpText.equals("0")) {
                return null;
            }
            if (tmpText.matches("[0-9]*")) {
                return c;
            }
            return null;
        }));
    }
    private void getTickLabelsVisible(CheckBox aCheckBox) {
        if(aCheckBox.isSelected()) {
            this.categoryAxis.setTickMarkVisible(false);
            this.categoryAxis.setTickLabelsVisible(false);
        }
        aCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (aCheckBox.isSelected()) {
                this.categoryAxis.setTickMarkVisible(false);
                this.categoryAxis.setTickLabelsVisible(false);
            } else {
                this.categoryAxis.setTickMarkVisible(true);
                this.categoryAxis.setTickLabelsVisible(true);
            }
        });
    }
    //
    /**
     * Returns Digit of a double number
     *
     * @param aValue fragment frequency
     * @return Digit of fragment frequency
     */
    private double getFrequencyDoubleDigit(double aValue) {
        double tmpDoubleDigit = String.valueOf(aValue).length();
        return tmpDoubleDigit;
    }
    //
    /**
     * Method to iterate over the global lists to create XYChart data
     *
     * @param aStructureViewer to display the fragments
     * @param aBarLabelCheckBox for Labelling the frequencies
     * @param aMaxOfFrequency the highest fragment number
     * @param aXYChartSeries  passed to the histogram
     */
    private void createXYChartSeries(ImageView aStructureViewer, CheckBox aBarLabelCheckBox, double aMaxOfFrequency, XYChart.Series aXYChartSeries) {
       // tmpSeries.getData().clear();
        // i = 0;
        for (Iterator tmpStringIterator = this.sublistSmiles.iterator(), tmpIntegerIterator = this.sublistOfFrequency.iterator(), tmpSmilesIterator = this.smilesToDepict.iterator(); tmpStringIterator.hasNext() && tmpIntegerIterator.hasNext() && tmpSmilesIterator.hasNext();) {
            // Integer tmpCurrentFrequency = (Integer) tmpIntegerIterator.next();
            double tmpCurrentFrequency = (double) tmpIntegerIterator.next();
            String tmpCurrentSmiles = (String) tmpStringIterator.next();
            String tmpSmiles = (String) tmpSmilesIterator.next();
            XYChart.Data<Number, String> tmpStringNumberData = new XYChart.Data(tmpCurrentFrequency, tmpCurrentSmiles);
            if(tmpCurrentFrequency == 0) {
                i++;
                aStructureViewer.setImage(null);
               // tmpSeries.getData().clear();
            } else {
              //  XYChart.Data<Number, String> tmpStringNumberData = new XYChart.Data(tmpCurrentFrequency, tmpCurrentSmiles);
                StackPane tmpNode = this.histogramHover(aStructureViewer, tmpSmiles);
                tmpStringNumberData.setNode(tmpNode);
                tmpNode.setStyle("-fx-bar-fill:  #1E90FF");
                this.getBar(aBarLabelCheckBox, tmpNode, tmpCurrentFrequency, this.getFrequencyDoubleDigit(aMaxOfFrequency));
               // tmpSeries.getData().add(tmpStringNumberData);
            }
            aXYChartSeries.getData().add(tmpStringNumberData);
        }
    }
    //</editor-fold>
    //
}








