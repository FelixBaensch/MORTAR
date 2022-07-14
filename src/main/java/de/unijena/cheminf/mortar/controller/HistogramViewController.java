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
     * Y axis of the histogram
     */
    private CategoryAxis categoryAxis;
    List<Integer> tmpSublistOfFrequency;
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
        CheckBox tmplog = this.histogramView.getLogarithmicScale();
        this.categoryAxis = new CategoryAxis();
        InputStream tmpImageInputStream = HistogramViewController.class.getResourceAsStream("/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png");
        this.histogramStage.getIcons().add(new Image(tmpImageInputStream));
        double defaulthohe = 50.0;
        TextField field = this.histogramView.getFrequencyTextField();
        BarChart tmpHistogramBarChart;
        if(this.copyList.size()>=30) {
            field.setText("30");
            tmpHistogramBarChart = this.createHistogram(GuiDefinitions.HISTOGRAM_DEFAULT_FRAGMENT_FREQUENCY,
                    this.histogramView, GuiDefinitions.HISTOGRAM_DEFAULT_SMILES_LENGTH, tmpBarLabelCheckBox, tmpGridlineCheckBox, defaulthohe, tmplog);
        } else {
            field.setText(String.valueOf(this.copyList.size()));
            tmpHistogramBarChart = this.createHistogram(this.copyList.size(),
                    this.histogramView, GuiDefinitions.HISTOGRAM_DEFAULT_SMILES_LENGTH, tmpBarLabelCheckBox, tmpGridlineCheckBox, defaulthohe, tmplog);
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
                    System.out.println(imageWidth +" -1");
                    double tmpIntermediateImageHeight = GuiDefinitions.GUI_IMAGE_HEIGHT+tmpImageHeightChange;
                    double tmpImageHeight = tmpHeight-tmpIntermediateImageHeight;
                    imageHeight = tmpIntermediateImageHeight+tmpImageHeight;
                    imageWidth = 100+imageHeight;
                    System.out.println(imageWidth+ " -2");
                    imageZoomFactor = (GuiDefinitions.GUI_IMAGE_ZOOM_FACTOR/GuiDefinitions.GUI_IMAGE_WIDTH)*imageWidth;
                }
            }
        });
        this.histogramStage.show();
         this.setMaxBarWidth(30, this.categoryAxis, tmpHistogramBarChart);
     //   tmpHistogramBarChart.setCategoryGap( this.getBarSpacing(this.tmpSublistOfFrequency.size()));

       // System.out.println(tmpHistogramBarChart.getBarGap());
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
    private BarChart createHistogram(int aNumber, HistogramView aHistogramView, int aSmilesLength, CheckBox aBarLabelCheckBox, CheckBox aGridlineCheckBox, double h, CheckBox abox)  {
        XYChart.Series tmpSeries = new XYChart.Series();
        NumberAxis tmpNumberAxis = new NumberAxis();
        BarChart tmpHistogramBarChart = new BarChart(tmpNumberAxis, this.categoryAxis);
        this.categoryAxis.setTickLabelFill(Color.BLACK);
        this.categoryAxis.setTickMarkVisible(true);
        this.categoryAxis.setTickLength(GuiDefinitions.HISTOGRAM_TICK_LABEL_LENGTH);
        this.categoryAxis.setTickLabelsVisible(true);
        tmpNumberAxis.setSide(Side.TOP);
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
        List<String> tmpSublistSmiles = null;
        List<Integer> tmpSublistOfFrequency = null;
        List<String> tmpSmilesToDepict = null;
        Label tmpDisplayMaxFrequency = aHistogramView.getDefaultLabel();
        /**
        if (aNumber == 0) {
            tmpSublistSmiles = tmpSmilesList.subList(aNumber, tmpSmilesList.size());
            tmpSublistOfFrequency = tmpFrequencyList.subList(aNumber, tmpFrequencyList.size());
            tmpSmilesToDepict = tmpFullSmilesLength.subList(aNumber, tmpFullSmilesLength.size());
            Label tmpDisplayMaxFrequency = aHistogramView.getDefaultLabel();
            tmpDisplayMaxFrequency.setText("Display Fragments (max."+this.copyList.size()+"):");
        }
        else {
            */
            try {
                if (aNumber <= tmpSmilesList.size()) {
                    tmpSublistSmiles = tmpSmilesList.subList(tmpSmilesList.size() - aNumber, tmpSmilesList.size());
                    tmpSublistOfFrequency = tmpFrequencyList.subList(tmpFrequencyList.size() - aNumber, tmpFrequencyList.size());
                    tmpSmilesToDepict = tmpFullSmilesLength.subList(tmpFullSmilesLength.size()- aNumber, tmpFullSmilesLength.size());
                    tmpDisplayMaxFrequency.setText("Display Fragments");
                } else {
                    throw new IllegalArgumentException("the given number exceeds the maximum number of fragments");
                }
            } catch (IllegalArgumentException anException) {
                HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                GuiUtil.guiExceptionAlert(Message.get("HistogramViewController.HistogramRefreshError.Title"), Message.get("HistogramViewController.HistogramError.Header"),
                        Message.get("HistogramViewController.HistogramError.Content"), anException);
            }
            List<Double> log = new ArrayList<>();
            for(int a : tmpSublistOfFrequency) {
                double f = Math.log10(a);
                log.add(f);
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
        double tmpHistogramSize = h*tmpSublistOfFrequency.size();
        tmpHistogramBarChart.setPrefHeight(tmpHistogramSize);
        tmpHistogramBarChart.setMinHeight(tmpHistogramSize);
        tmpHistogramBarChart.getData().add(tmpSeries);
        tmpHistogramBarChart.setLegendVisible(false);
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
         this.digitsTxtFld(tmpFrequencyTextField);

        TextField tmpSmilesTextField = this.histogramView.getSmilesTextField();
        this.digitsTxtFld(tmpSmilesTextField);

            this.histogramView.getRefreshButton().disableProperty().bind(
                    Bindings.isEmpty(tmpFrequencyTextField.textProperty()).and(Bindings.isEmpty(tmpSmilesTextField.textProperty()))
            );
        this.histogramView.getRefreshButton().setOnAction(event -> {
              //  BarChart tmpRefreshBarChart
                String tmpStringMaxFrequency = this.histogramView.getTextField();
                String tmpMaxSmilesLength = this.histogramView.getSmilesField();
                CheckBox tmpBarLabelCheckBox = this.histogramView.getCheckbox();
                CheckBox tmpGridlinesCheckBox = this.histogramView.getGridLinesCheckBox();
                CheckBox ertz = this.histogramView.getLogarithmicScale();
                ComboBox box = this.histogramView.getComboBox();
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
                                Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Header"),
                                Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Content"));
                        return;
                    }
                }
                ArrayList<Double> liste = this.getBarSpacing(tmpFragmentNumber,box);
                BarChart tmpRefreshBarChart=  this.createHistogram(tmpFragmentNumber, this.histogramView, tmpSmilesLengthInField, tmpBarLabelCheckBox, tmpGridlinesCheckBox,liste.get(0), ertz);
                tmpRefreshBarChart.setCategoryGap(liste.get(1));
            System.out.println("deneme ");
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
       // tmpNodePane.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3))));
        MenuItem tmpContextMenuItem = new MenuItem(Message.get("HistogramViewController.MenuItem.text"));
        ContextMenu tmpContextMenu = new ContextMenu();
        tmpContextMenu.getItems().add(tmpContextMenuItem);

        Label tmpContextMenuLabel = new Label();


        tmpContextMenuLabel.setPrefWidth(10);
        tmpContextMenuLabel.setMaxWidth(10);
        tmpContextMenuLabel.setMinWidth(10);
        tmpContextMenuLabel.setTranslateX(20);



       // tmpContextMenuLabel.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3))));
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
                    tmpContextMenuLabel.setPrefWidth(50);
                    tmpContextMenuLabel.setMaxWidth(50);
                    tmpContextMenuLabel.setMinWidth(50);
                    tmpContextMenuLabel.setTranslateX(20);
                    System.out.println(tmpContextMenuLabel.getWidth());
                }
                /**
                    tmpContextMenuLabel.setPrefWidth(tmpNodePane.getWidth());
                    tmpContextMenuLabel.setMaxWidth(tmpNodePane.getWidth());
                    tmpContextMenuLabel.setMinWidth(tmpNodePane.getWidth());
                System.out.println(tmpNodePane.getWidth() + " breite Node");

                /**
                tmpContextMenuLabel.setPrefWidth(tmpNodePane.getWidth());
                tmpContextMenuLabel.setMaxWidth(tmpNodePane.getWidth());
                tmpContextMenuLabel.setMinWidth(tmpNodePane.getWidth());
                tmpContextMenuLabel.setTranslateX(0);
                 */
               // tmpContextMenuLabel.setRotate(-180);
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
                    //tmpNodePane.setCursor(Cursor.HAND);
                }
                try {
                //kekulization done separately below
                    tmpSmiPar.kekulise(false);
                    IAtomContainer tmpAtomContainer = tmpSmiPar.parseSmiles(aSmiles);
                    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomContainer);
                    Kekulization.kekulize(tmpAtomContainer);
                    Image image = DepictionUtil.depictImageWithPadding(tmpAtomContainer,2,imageWidth, imageHeight, -15.0);
                    anImageView.setImage(image);
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
                        tmpContextMenuLabel.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3))));
                        // tmpContextMenuLabel.setTranslateX(50);
                        tmpContextMenu.show(tmpContextMenuLabel, tmpNodePane.getWidth() / 2, tmpNodePane.getHeight()); // versuch label width und height
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
                aStackPane.getChildren().remove(tmpBarLabel);
            }
        });
        return aStackPane;
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
    private ArrayList getBarSpacing(int aNumber, ComboBox aComboBox){
        ArrayList  <Double> liste = new ArrayList<>();
        double tmpHistogramHeight;
        double tmpGapDeviation;
        double tmpGapSpacing;
        double tmpCategoryGap = 0;
        double hohe = 0;
        String value = (String) aComboBox.getValue();
        switch (value) {
            case "Low":
                if (aNumber <= 26) {
                    tmpHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / aNumber;
                    tmpGapDeviation = tmpHistogramHeight / (706.4 / 26);
                    tmpGapSpacing = 3.2461538461538453 * tmpGapDeviation;
                    double tmpz = tmpHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpz - 15;  //GuiDefinitions.GUI_BAR_WIDTH
                } else {
                    hohe = 27;
                    tmpHistogramHeight = hohe * aNumber - 84; //84
                    tmpGapSpacing = tmpHistogramHeight / aNumber;
                    tmpCategoryGap = tmpGapSpacing - 15;
                }
                break;
            case "Medium":
                if(aNumber <= 19) {
                    tmpHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / aNumber;
                    tmpGapDeviation = tmpHistogramHeight / (706.4 / 19);
                    tmpGapSpacing = 4.442105263157892 * tmpGapDeviation;
                    double tmpz = tmpHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpz - 20;  //GuiDefinitions.GUI_BAR_WIDTH
                } else {
                    hohe = 37;
                    tmpHistogramHeight = hohe * aNumber - 84; //84
                    tmpGapSpacing = tmpHistogramHeight / aNumber;
                    tmpCategoryGap = tmpGapSpacing - 20;
                }
                break;
            case "High":
                if( aNumber <= 14) {
                    hohe = 50;
                    tmpHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / aNumber;
                    tmpGapDeviation = tmpHistogramHeight / (706.4 / 14);
                    tmpGapSpacing = 6.028571428571425 * tmpGapDeviation;
                    double tmpz = tmpHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpz - 30;  //GuiDefinitions.GUI_BAR_WIDTH
                } else {
                    hohe = 50;
                    tmpHistogramHeight = hohe * aNumber - 85; //84
                    tmpGapSpacing = tmpHistogramHeight / aNumber;
                    tmpCategoryGap = tmpGapSpacing - 30;
                }
                break;
        }
        liste.add(hohe);
        liste.add(tmpCategoryGap);
        // return capitalCities;
        return liste;
    }
    public  void digitsTxtFld(TextField field) {
        field.setTextFormatter(new TextFormatter<Integer>(c -> {
            String newText = c.getControlNewText();
            if (newText.equals("0")) {
                return null;
            }
            if (newText.matches("[0-9]*")) {
                return c;
            }
            return null;
        }));
    }
    private int getDoubleDigit(double aN) {
        int lengthOfInt = String.valueOf(aN).length();
        return lengthOfInt;
    }

    //</editor-fold>
    //
}








