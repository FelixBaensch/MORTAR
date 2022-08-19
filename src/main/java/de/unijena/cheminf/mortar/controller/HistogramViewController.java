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
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextFormatter;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class for the HistogramView
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public class HistogramViewController {
    //<editor-fold desc="public static final class variables" defaultstate="collapsed">
    /**
     * Default value for the number of initially displayed fragments.
     */
    public static final int DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS = 30;
    /**
     * Default SMILES length at creation of histogram view.
     */
    public static final int DEFAULT_MAX_SMILES_LENGTH = 25;
    //</editor-fold>
    //
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
    private double imageWidth;
    /**
     * Height of the images
     */
    private double imageHeight;
    /**
     * Zoom factor of the images
     */
    private double imageZoomFactor;
    /**
     * Y-axis of the histogram
     */
    private CategoryAxis categoryAxis;
    /**
     * Histogram chart
     */
   private BarChart histogramChart;
    /**
     * AtomContainer to depict structures
     */
   private IAtomContainer atomContainer;
    /**
     * Current value for the number of fragments displayed
     */
    private int displayedFragmentsNumber;
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param aMainStage Stage of the MainView
     * @param aMoleculeDataList ObservableList to hold MoleculeDataModels for visualisation in histogram
     */
    public HistogramViewController(Stage aMainStage, List<MoleculeDataModel> aMoleculeDataList)  {
        this.mainStage = aMainStage;
        this.copyList = new ArrayList<>(aMoleculeDataList);
        this.imageWidth = GuiDefinitions.GUI_IMAGE_WIDTH;
        this.imageHeight = GuiDefinitions.GUI_IMAGE_HEIGHT;
        this.imageZoomFactor = GuiDefinitions.GUI_IMAGE_ZOOM_FACTOR;
        this.displayedFragmentsNumber = HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS;
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
        this.categoryAxis = new CategoryAxis();
        InputStream tmpImageInputStream = HistogramViewController.class.getResourceAsStream("/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png");
        this.histogramStage.getIcons().add(new Image(tmpImageInputStream));
        this.histogramView.getSmilesTextField().setText(Integer.toString(HistogramViewController.DEFAULT_MAX_SMILES_LENGTH));
        this.histogramView.getCheckbox().setSelected(true);
        this.histogramView.getStylingCheckBox().setSelected(true);
        if (this.copyList.size() >= HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS) {
            this.displayedFragmentsNumber = HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS;
            this.histogramView.getFrequencyTextField().setText(Integer.toString(HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS));
            ArrayList<Double> tmpDefaultBarWidth = this.getBarSpacing(HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS, this.histogramView.getComboBox());
            this.histogramChart = this.createHistogram(GuiDefinitions.HISTOGRAM_DEFAULT_FRAGMENT_FREQUENCY,
                    this.histogramView, GuiDefinitions.HISTOGRAM_DEFAULT_SMILES_LENGTH, this.histogramView.getCheckbox(),this.histogramView.getStylingCheckBox(),
                    tmpDefaultBarWidth.get(0));
            this.histogramChart.setCategoryGap(tmpDefaultBarWidth.get(1));
        } else {
            this.histogramView.getFrequencyTextField().setText(String.valueOf(this.copyList.size()));
            this.displayedFragmentsNumber = this.copyList.size();
            ArrayList<Double> tmpDefaultBarGap = this.getBarSpacing(this.copyList.size(), this.histogramView.getComboBox());
            this.histogramChart = this.createHistogram(this.copyList.size(),
                    this.histogramView ,GuiDefinitions.HISTOGRAM_DEFAULT_SMILES_LENGTH, this.histogramView.getCheckbox(), this.histogramView.getStylingCheckBox(),
                    tmpDefaultBarGap.get(0));
             this.histogramChart.setCategoryGap(tmpDefaultBarGap.get(1));
        }
        this.addListener();
        Scene tmpHistogramScene = new Scene(this.histogramView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.histogramStage.setScene(tmpHistogramScene);
        tmpHistogramScene.widthProperty().addListener((observable, oldValue, newValue) -> {
            double tmpWidthChange = ((tmpHistogramScene.getWidth() - GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE) / GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE) * 100;
            double tmpImageWidthChange = (GuiDefinitions.GUI_IMAGE_WIDTH / 100) * tmpWidthChange;
            this.imageWidth = GuiDefinitions.GUI_IMAGE_WIDTH+tmpImageWidthChange;
            this.imageHeight = this.imageWidth - 100;
            this.imageZoomFactor = (GuiDefinitions.GUI_IMAGE_ZOOM_FACTOR / GuiDefinitions.GUI_IMAGE_WIDTH) * this.imageWidth;
        });
        tmpHistogramScene.heightProperty().addListener((observable, oldValue, newValue) -> {
            double tmpHeightChange = ((tmpHistogramScene.getHeight() - GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE) / GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE) * 100;
            double tmpImageHeightChange = (GuiDefinitions.GUI_IMAGE_HEIGHT / 100) * tmpHeightChange;
            if (tmpHistogramScene.getWidth() == GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE) {
                this.imageHeight = GuiDefinitions.GUI_IMAGE_HEIGHT + tmpImageHeightChange;
                this.imageWidth = 100 + this.imageHeight;
                this.imageZoomFactor = (GuiDefinitions.GUI_IMAGE_ZOOM_FACTOR / GuiDefinitions.GUI_IMAGE_WIDTH) * this.imageWidth;
            } else {
                double tmpHeight = imageWidth - 100;
                double tmpIntermediateImageHeight = GuiDefinitions.GUI_IMAGE_HEIGHT + tmpImageHeightChange;
                double tmpImageHeight = tmpHeight - tmpIntermediateImageHeight;
                imageHeight = tmpIntermediateImageHeight + tmpImageHeight;
                imageWidth = 100 + imageHeight;
                imageZoomFactor = (GuiDefinitions.GUI_IMAGE_ZOOM_FACTOR / GuiDefinitions.GUI_IMAGE_WIDTH) * imageWidth;
            }
        });
        this.histogramStage.show();
    }
    //
    /**
     * Create a configurable histogram.
     * All data is stored in corresponding lists (the x data are either fragment or molecule frequencies).
     * Depending on how aFragmentNumber is selected, these lists/data are adjusted.
     * After they have been run through, they are displayed accordingly.
     * The x-axis of the histogram is divided into readable steps depending on the maximum frequency ( manual adjustment of the
     * x-axis ticks)
     *
     * @param aFragmentNumber to set the fragment number. The number of fragments that are displayed can be set via this parameter.
     * @param aHistogramView to access the view elements
     * @param aSmilesLength to set the SMILES length. The maximum SMILES length can be set via this parameter.
     * @param aHistogramDefaultSize a double value that determines the height of the histogram.
     *                              The height of the histogram increases with the increase in fragments that are displayed.
     * @return a BarChart to show
     */
    private BarChart createHistogram(int aFragmentNumber, HistogramView aHistogramView, int aSmilesLength, CheckBox aBarLabelCheckBox, CheckBox aStylingCheckBox,
                                     double aHistogramDefaultSize) {
        XYChart.Series tmpSeries = new XYChart.Series();
        NumberAxis tmpNumberAxis = new NumberAxis();
        BarChart tmpHistogramBarChart = new BarChart(tmpNumberAxis, this.categoryAxis);
        this.categoryAxis.setTickLabelFill(Color.BLACK);
        this.categoryAxis.setTickLength(GuiDefinitions.HISTOGRAM_TICK_LABEL_LENGTH);
        tmpNumberAxis.setSide(Side.TOP);
        tmpNumberAxis.setAutoRanging(false);
        tmpNumberAxis.setMinorTickCount(1);
        tmpNumberAxis.setForceZeroInRange(true);
        tmpNumberAxis.setTickLabelFill(Color.BLACK);
        tmpNumberAxis.setLabel(Message.get("HistogramViewController.XAxisLabel.text"));
        this.categoryAxis.setTickLabelGap(GuiDefinitions.HISTOGRAM_TICK_LABEL_GAP);
        this.categoryAxis.setLabel(Message.get("HistogramViewController.YAxisLabel.text"));
        tmpHistogramBarChart.setCategoryGap(0);
        tmpHistogramBarChart.setBarGap(0);
        ScrollPane tmpScrollPane = aHistogramView.getScrollPane();
        tmpScrollPane.setContent(tmpHistogramBarChart);
        String tmpNewSmiles;
        ArrayList<String> tmpSmilesList = new ArrayList<>();
        ArrayList<Integer> tmpFrequencyList = new ArrayList<>();
        int tmpIterator = this.copyList.size();
        FragmentDataModel tmpFragmentData = null;
        double tmpMaxOfFrequency;
        ArrayList<String> tmpFullSmilesLength = new ArrayList<>();
        String tmpSelectedData = (String) aHistogramView.getChooseDataComoBox().getValue();
        if (tmpSelectedData.equals(Message.get("HistogramView.chooseDataComboBoxFragmentFrequency.text"))) {
            ListUtil.sortGivenFragmentListByPropertyAndSortType(this.copyList, "absoluteFrequency", "ASCENDING");
            for (MoleculeDataModel tmpMoleculeData : this.copyList) {
                tmpFragmentData = (FragmentDataModel) tmpMoleculeData;
                if (tmpFragmentData.getUniqueSmiles().length() > aSmilesLength) {
                    tmpNewSmiles = "SMILES too long (" + tmpIterator + ")";
                    tmpSmilesList.add(tmpNewSmiles);
                } else {
                    tmpNewSmiles = tmpFragmentData.getUniqueSmiles();
                    tmpSmilesList.add(tmpNewSmiles);
                }
                tmpIterator--;
                tmpFullSmilesLength.add(tmpFragmentData.getUniqueSmiles());
                tmpFrequencyList.add(tmpFragmentData.getAbsoluteFrequency());
            }
        } else {
            ListUtil.sortGivenFragmentListByPropertyAndSortType(this.copyList, "moleculeFrequency", "ASCENDING");
            for (MoleculeDataModel tmpMoleculeData : this.copyList) {
                tmpFragmentData = (FragmentDataModel) tmpMoleculeData;
                if (tmpFragmentData.getUniqueSmiles().length() > aSmilesLength) {
                    tmpNewSmiles = "SMILES too long (" + tmpIterator + ")";
                    tmpSmilesList.add(tmpNewSmiles);
                } else {
                    tmpNewSmiles = tmpFragmentData.getUniqueSmiles();
                    tmpSmilesList.add(tmpNewSmiles);
                }
                tmpIterator--;
                tmpFullSmilesLength.add(tmpFragmentData.getUniqueSmiles());
                tmpFrequencyList.add(tmpFragmentData.getMoleculeFrequency());
            }
        }
        tmpMaxOfFrequency = Collections.max(tmpFrequencyList);
        // make readable x-axis
        double tmpXAxisTicks = 5.0/100.0*tmpMaxOfFrequency; // magic number
        double tmpXAxisExtension = 15.0/100.0*tmpMaxOfFrequency; // magic number
        int tmpIntTmpXAxisTick = (int) Math.round(tmpXAxisTicks);
        int tmpIntXAxisExtension = (int) Math.round(tmpXAxisExtension);
        if (tmpIntTmpXAxisTick == 0 || tmpIntXAxisExtension == 0) {
           tmpNumberAxis.setTickUnit(1);
           tmpNumberAxis.setUpperBound(tmpMaxOfFrequency+1);
       }
       else {
            int tmpNewXAxisTick = 0;
            if (tmpIntTmpXAxisTick >= 10) {
                tmpNewXAxisTick = tmpIntTmpXAxisTick;
                String tmpTickLength = String.valueOf(tmpNewXAxisTick);
                String tmpFirstValue = String.valueOf(tmpTickLength.charAt(0));
                int tmpFirstIntValue = Integer.parseInt(tmpFirstValue);
                if (tmpFirstIntValue > 5) {
                    tmpNewXAxisTick = (int) Math.pow(10, tmpTickLength.length());
                    tmpNumberAxis.setTickUnit(tmpNewXAxisTick);
                } else {
                    int tmpDigit = tmpTickLength.length() - 1;
                    int tmpCheckModulo = (int) Math.pow(10, tmpDigit);
                    if (tmpNewXAxisTick % tmpCheckModulo == 0) {
                        tmpNumberAxis.setTickUnit(tmpNewXAxisTick);
                    } else {
                        do {
                            tmpNewXAxisTick++;
                        } while (tmpNewXAxisTick % (tmpCheckModulo) != 0);
                        tmpNumberAxis.setTickUnit(tmpNewXAxisTick);
                    }
                }
                tmpNumberAxis.setUpperBound(this.calculateXAxisExtension((int) tmpMaxOfFrequency, tmpNewXAxisTick));
            } else {
                tmpNumberAxis.setTickUnit(tmpIntTmpXAxisTick);
                tmpNumberAxis.setUpperBound(this.calculateXAxisExtension((int) tmpMaxOfFrequency, tmpIntTmpXAxisTick));
            }
        }
        Label tmpDisplayFrequency = aHistogramView.getDefaultFragmentLabel();
        List<String> tmpSublistSmiles;
        List<Integer> tmpSublistFrequency;
        List<String> tmpSmilesToDepict;
        if (aFragmentNumber <= tmpSmilesList.size()) {
            tmpSublistSmiles = tmpSmilesList.subList(tmpSmilesList.size() - aFragmentNumber, tmpSmilesList.size());
            tmpSublistFrequency = tmpFrequencyList.subList(tmpFrequencyList.size() - aFragmentNumber, tmpFrequencyList.size());
            tmpSmilesToDepict = tmpFullSmilesLength.subList(tmpFullSmilesLength.size()- aFragmentNumber, tmpFullSmilesLength.size());
            tmpDisplayFrequency.setText("Displayed fragments:");
        } else {
            throw new IllegalArgumentException("the given number exceeds the maximum number of fragments");
        }
        for (Iterator tmpStringIterator = tmpSublistSmiles.iterator(), tmpIntegerIterator = tmpSublistFrequency.iterator(),
             tmpSmilesIterator = tmpSmilesToDepict.iterator(); tmpStringIterator.hasNext() && tmpIntegerIterator.hasNext() && tmpSmilesIterator.hasNext();) {
            Integer tmpCurrentFrequency = (Integer) tmpIntegerIterator.next();
            String tmpCurrentSmiles = (String) tmpStringIterator.next();
            String tmpSmiles = (String) tmpSmilesIterator.next();
            XYChart.Data<Number, String> tmpStringNumberData = new XYChart.Data(tmpCurrentFrequency, tmpCurrentSmiles);
            StackPane tmpNode = this.histogramHover(aHistogramView.getImageStructure(), tmpSmiles);
            tmpStringNumberData.setNode(tmpNode);
            tmpNode.setStyle("-fx-bar-fill: #1E90FF");
            int tmpDigitLength = String.valueOf(tmpCurrentFrequency).length();
            this.getBarLabel(aBarLabelCheckBox, aStylingCheckBox, tmpNode, tmpCurrentFrequency, tmpDigitLength);
            tmpSeries.getData().add(tmpStringNumberData);
        }
        double tmpHistogramSize = aHistogramDefaultSize *tmpSublistFrequency.size();
        tmpHistogramBarChart.setPrefHeight(tmpHistogramSize);
        tmpHistogramBarChart.setMinHeight(tmpHistogramSize);
        tmpHistogramBarChart.getData().add(tmpSeries);
        tmpHistogramBarChart.setLegendVisible(false);
        tmpHistogramBarChart.layout();
        tmpHistogramBarChart.setHorizontalGridLinesVisible(false);
        tmpHistogramBarChart.setVerticalGridLinesVisible(false);
        tmpHistogramBarChart.setAnimated(false);
        return tmpHistogramBarChart;
    }
    //
    /**
     * Add listeners
     *
     */
    private void addListener() {
        this.histogramView.getCloseButton().setOnAction(event -> {
            this.histogramStage.close();
        });
        this.histogramView.getFrequencyTextField().setTextFormatter(
                new TextFormatter<>(GuiUtil.getStringToIntegerConverter(), this.displayedFragmentsNumber, GuiUtil.getPositiveIntegerWithoutZeroFilter()));
        this.histogramView.getSmilesTextField().setTextFormatter(
                new TextFormatter<>(GuiUtil.getStringToIntegerConverter(), HistogramViewController.DEFAULT_MAX_SMILES_LENGTH, GuiUtil.getPositiveIntegerWithoutZeroFilter()));
        this.histogramView.getApplyButton().disableProperty().bind(
                Bindings.isEmpty(this.histogramView.getFrequencyTextField().textProperty()).
                            and(Bindings.isEmpty(this.histogramView.getSmilesTextField().textProperty()))
        );
        this.histogramView.getApplyButton().setOnAction(event -> {
            int tmpSmilesLengthInField;
            if (this.histogramView.getSmilesField().isEmpty()) {
                this.displayedFragmentsNumber = Integer.parseInt(this.histogramView.getFragmentTextField());
                if (this.displayedFragmentsNumber > this.copyList.size()) {
                    GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Header"),
                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Content"));
                    return;
                }
                tmpSmilesLengthInField = GuiDefinitions.HISTOGRAM_DEFAULT_SMILES_LENGTH;
            } else if (this.histogramView.getFragmentTextField().isEmpty()) {
                tmpSmilesLengthInField = Integer.parseInt(this.histogramView.getSmilesField());
                if (this.copyList.size() >= HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS) {
                    this.displayedFragmentsNumber = HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS;
                } else {
                    this.displayedFragmentsNumber = this.copyList.size();
                }
            } else {
                this.displayedFragmentsNumber = Integer.parseInt(this.histogramView.getFragmentTextField());
                tmpSmilesLengthInField = Integer.parseInt(this.histogramView.getSmilesField());
                if (this.displayedFragmentsNumber > this.copyList.size()) {
                    GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Header"),
                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Content"));
                    return;
                }
            }
            ArrayList<Double> tmpHistogramSizeGap = this.getBarSpacing(this.displayedFragmentsNumber,this.histogramView.getComboBox());
            this.histogramChart=  this.createHistogram(this.displayedFragmentsNumber, this.histogramView, tmpSmilesLengthInField,
                    this.histogramView.getCheckbox(), this.histogramView.getStylingCheckBox(), tmpHistogramSizeGap.get(0));
            this.histogramChart.setCategoryGap(tmpHistogramSizeGap.get(1));
            if(this.histogramView.getGridLinesCheckBox().isSelected()) {
                this.histogramChart.setVerticalGridLinesVisible(true);
                this.histogramChart.setHorizontalGridLinesVisible(true);
            }
            if (this.histogramView.getSmilesTickLabel().isSelected()) {
                this.categoryAxis.setTickMarkVisible(false);
                this.categoryAxis.setTickLabelsVisible(false);
            }
        });
        this.histogramView.getGridLinesCheckBox().selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (this.histogramView.getGridLinesCheckBox().isSelected()) {
                this.histogramChart.setVerticalGridLinesVisible(true);
                this.histogramChart.setHorizontalGridLinesVisible(true);
            } else {
                this.histogramChart.setVerticalGridLinesVisible(false);
                this.histogramChart.setHorizontalGridLinesVisible(false);
            }
        });
        this.histogramView.getSmilesTickLabel().selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (this.histogramView.getSmilesTickLabel().isSelected()) {
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
     * Make the histogram hoverable and create the structure images, that are displayed in the histogram
     * For this purpose: Add a StackPane to each bar as a node, and use the StackPanes to call the corresponding listener
     * Hovering over the bars displays the corresponding structures.
     * In addition, by right-clicking on the bars, the corresponding structures (1500x1000) can be copied to SMILES anyway (also about adding labels)
     *
     * @param anImageView to display the structures
     * @param aSmiles to depict the structures and copy them
     * @return a StackPane that is used as a node in the histogram
     */
    private StackPane histogramHover(ImageView anImageView, String aSmiles) {
        StackPane tmpNodePane = new StackPane();
        tmpNodePane.setAlignment(Pos.CENTER_RIGHT);
        MenuItem tmpCopySmilesMenuItem = new MenuItem(Message.get("HistogramViewController.MenuItemSmiles.text"));
        MenuItem tmpCopyStructureMenuItem = new MenuItem(Message.get("HistogramViewController.MenuItemStructure.text"));
        ContextMenu tmpContextMenu = new ContextMenu();
        tmpContextMenu.getItems().addAll(tmpCopySmilesMenuItem, tmpCopyStructureMenuItem);
        Label tmpContextMenuLabel = new Label();
        tmpContextMenuLabel.setPrefWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL);
        tmpContextMenuLabel.setMaxWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL);
        tmpContextMenuLabel.setMinWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL);
        tmpContextMenuLabel.setTranslateX(20);
        tmpCopySmilesMenuItem.setGraphic(new ImageView(new Image("de/unijena/cheminf/mortar/images/copy_icon_16x16.png")));
        tmpCopyStructureMenuItem.setGraphic(new ImageView(new Image("de/unijena/cheminf/mortar/images/copy_icon_16x16.png")));
        tmpContextMenuLabel.setContextMenu(tmpContextMenu);
        tmpNodePane.getChildren().addAll(tmpContextMenuLabel);
        tmpNodePane.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            tmpNodePane.setStyle("-fx-bar-fill: #00008b");
            if (tmpNodePane.getWidth() >= 10) {
                tmpContextMenuLabel.setPrefWidth(tmpNodePane.getWidth());
                tmpContextMenuLabel.setMaxWidth(tmpNodePane.getWidth());
                tmpContextMenuLabel.setMinWidth(tmpNodePane.getWidth());
                tmpContextMenuLabel.setTranslateX(0);
                tmpContextMenuLabel.addEventHandler(MouseEvent.MOUSE_CLICKED,event1 -> {
                    if (MouseButton.SECONDARY.equals(event1.getButton())) {
                        tmpContextMenuLabel.setStyle("-fx-bar-fill: #00008b");
                        tmpContextMenu.show(tmpContextMenuLabel, tmpNodePane.getWidth() / 2, tmpNodePane.getHeight());
                    }
                });
            } else {
                tmpContextMenuLabel.setPrefWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL_NEW);
                tmpContextMenuLabel.setMaxWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL_NEW);
                tmpContextMenuLabel.setMinWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL_NEW);
                tmpContextMenuLabel.setTranslateX(20);
                tmpNodePane.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event2 -> {
                    tmpContextMenu.show(tmpNodePane, event2.getScreenX(), event2.getScreenY());
                });
            }
            // TODO check MoleculeDataModel getAtomContainer
            SmilesParser tmpSmiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            this.atomContainer = null;
            try {
                tmpSmiPar.kekulise(false);
                this.atomContainer = tmpSmiPar.parseSmiles(aSmiles);
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(this.atomContainer);
                Kekulization.kekulize(this.atomContainer);
            } catch (CDKException anException) {
                HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            }
            Image tmpImage = DepictionUtil.depictImageWithZoom(this.atomContainer,this.imageZoomFactor, this.imageWidth, this.imageHeight);
            anImageView.setImage(tmpImage);
        });
        // Listener ContextMenuItems
        tmpCopySmilesMenuItem.setOnAction(event -> {
            ClipboardContent tmpSmilesClipboardContent = new ClipboardContent();
            tmpSmilesClipboardContent.putString(aSmiles);
            Clipboard.getSystemClipboard().setContent(tmpSmilesClipboardContent);
        });
        tmpCopyStructureMenuItem.setOnAction(event -> {
            ClipboardContent tmpStructureClipboardContent = new ClipboardContent();
            Image tmpCopyImageOnBar = DepictionUtil.depictImageWithZoom(this.atomContainer,12.0,1500, 1000);
            tmpStructureClipboardContent.putImage(tmpCopyImageOnBar);
            Clipboard.getSystemClipboard().setContent(tmpStructureClipboardContent);
        });
        tmpNodePane.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            tmpNodePane.setStyle("-fx-bar-fill: #1E90FF");
            anImageView.setImage(null);
        });
        return tmpNodePane;
    }
    //
    /**
     * Enables the labelling of the histogram.
     * Clicking on the CheckBox displays the frequencies next to the bars.
     * In this method, the labels are also added to the StackPanes that will later display the frequencies next to the bars.
     * The bar labels are an extension of the StackPanes.
     * The method enables the display of the bars with shadows when the checkbox provided for this purpose is clicked.
     *
     * @param aLabelCheckBox to make the display of the fragment labels adjustable
     * @param aStackPane to add the labels
     * @param aFrequency values of the frequencies
     * @param aDigitNumber to assign the correct size to the labels
     * @return StackPane with the frequency labels added to the histogram
     */
    private StackPane getBarLabel(CheckBox aLabelCheckBox,CheckBox aBarStylingCheckBox, StackPane aStackPane, int aFrequency, int aDigitNumber) {
        Label tmpBarLabel = new Label();
        tmpBarLabel.setTranslateY(0);
        tmpBarLabel.setAlignment(Pos.CENTER_RIGHT);
        tmpBarLabel.setPrefWidth(GuiDefinitions.GUI_BAR_LABEL_SIZE*aDigitNumber);
        tmpBarLabel.setMinWidth(GuiDefinitions.GUI_BAR_LABEL_SIZE*aDigitNumber);
        tmpBarLabel.setMaxWidth(GuiDefinitions.GUI_BAR_LABEL_SIZE*aDigitNumber);
        tmpBarLabel.setTranslateX(aDigitNumber*GuiDefinitions.GUI_BAR_LABEL_SIZE + 5);
        tmpBarLabel.setStyle(null);
        tmpBarLabel.setText(String.valueOf(aFrequency));
        if(aLabelCheckBox.isSelected()) {
           aStackPane.getChildren().add(tmpBarLabel);
        }
        aLabelCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (aLabelCheckBox.isSelected()) {
                aStackPane.getChildren().add(tmpBarLabel);
            } else {
                aStackPane.getChildren().remove(tmpBarLabel);
            }
        });
        if(aBarStylingCheckBox.isSelected()) {
            aStackPane.setEffect(new DropShadow(10,2,3, Color.BLACK));
        }
        aBarStylingCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if(aBarStylingCheckBox.isSelected()) {
                aStackPane.setEffect(new DropShadow(10, 2, 3, Color.BLACK));
            } else
                aStackPane.setEffect(null);
        });
        return aStackPane;
    }
    //
    /**
     * Method to create the bar gaps depending on the bar width and histogram height factor value (category gap).
     * Method offers 3 different options (small, medium, large)
     *
     * @param aNumber fragment frequency currently displayed in the HistogramView
     * @param aComboBox to select one of the 3 options
     * @return ArrayList which contains both the value of the category gap and the value of the histogram height factor.
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
            //TODO: Use enums here!
            case "Small":
                if (aNumber <= 24) {
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / aNumber;
                    tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 24);
                    tmpGapSpacing = GuiDefinitions.GUI_HISTOGRAM_SMALL_BAR_GAP_CONST * tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing - GuiDefinitions.GUI_HISTOGRAM_SMALL_BAR_WIDTH;
                } else {
                    tmpFinalHistogramHeight = GuiDefinitions.GUI_HISTOGRAM_SMALL_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight * aNumber - 85;
                    tmpGapSpacing = tmpCurrentHistogramHeight / aNumber;
                    tmpCategoryGap = tmpGapSpacing - GuiDefinitions.GUI_HISTOGRAM_SMALL_BAR_WIDTH;
                }
                break;
            case "Medium":
                if (aNumber <= 17) {
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / aNumber;
                    tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 17);
                    tmpGapSpacing = GuiDefinitions.GUI_HISTOGRAM_MEDIUM_BAR_GAP_CONST * tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing - GuiDefinitions.GUI_HISTOGRAM_MEDIUM_BAR_WIDTH;
                } else {
                    tmpFinalHistogramHeight = GuiDefinitions.GUI_HISTOGRAM_MEDIUM_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight * aNumber - 85;
                    tmpGapSpacing = tmpCurrentHistogramHeight / aNumber;
                    tmpCategoryGap = tmpGapSpacing - GuiDefinitions.GUI_HISTOGRAM_MEDIUM_BAR_WIDTH ;
                }
                break;
            case "Large":
                if (aNumber <= 13) {
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / aNumber;
                    tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 13);
                    tmpGapSpacing = GuiDefinitions.GUI_HISTOGRAM_LARGE_BAR_GAP_CONST * tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing - GuiDefinitions.GUI_HISTOGRAM_LARGE_BAR_WIDTH;
                } else {
                    tmpFinalHistogramHeight = GuiDefinitions.GUI_HISTOGRAM_LARGE_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight * aNumber - 85;
                    tmpGapSpacing = tmpCurrentHistogramHeight / aNumber;
                    tmpCategoryGap = tmpGapSpacing - GuiDefinitions.GUI_HISTOGRAM_LARGE_BAR_WIDTH;
                }
                break;
        }
        tmpHistogramList.add(tmpFinalHistogramHeight);
        tmpHistogramList.add(tmpCategoryGap);
        return tmpHistogramList;
    }
    //
    /**
     * Method which calculates an optimal value for the x-axis extension
     * in order to be able to display the frequencies in the labels without abbreviations.
     *
     * @param aMaxValue is the value of the highest frequency that occurs in the data set.
     * @param aTickValue is the calculated tick
     * @return tmpXAxisExtensionValue is the upper limit of the X-axis
     */
    private int calculateXAxisExtension(int aMaxValue, int aTickValue) {
        int tmpTickNumber = (int) Math.round(aMaxValue/ aTickValue);
        int tmpXAxisExtensionValue;
        if ((aTickValue * tmpTickNumber) > aMaxValue) {
            tmpXAxisExtensionValue = (aTickValue * tmpTickNumber) + aTickValue;
        } else {
            tmpXAxisExtensionValue = (aTickValue * tmpTickNumber) + (2 * aTickValue);
        }
        return tmpXAxisExtensionValue;
    }
    //</editor-fold>
    //
}








