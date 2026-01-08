/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.configuration.IConfiguration;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.ClusterHistogramView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClusterHistogramViewController implements IViewToolController{
    public static enum BarWidthOption implements IDisplayEnum {
        /**
         * Small bar width.
         */
        SMALL(Message.get("ClusterHistogramView.barWidths.small.displayName"),
                Message.get("ClusterHistogramView.barWidths.small.tooltip")),
        /**
         * Medium bar width.
         */
        MEDIUM(Message.get("ClusterHistogramView.barWidths.medium.displayName"),
                Message.get("ClusterHistogramView.barWidths.medium.tooltip")),
        /**
         * Large bar width.
         */
        LARGE(Message.get("ClusterHistogramView.barWidths.large.displayName"),
                Message.get("ClusterHistogramView.barWidths.large.tooltip"));
        /**
         * A name for the respective constant that is meant for display, i.e. taken from the Message file.
         */
        private final String displayName;
        /**
         * Language-specific tooltip text for display in GUI.
         */
        private final String tooltip;
        /**
         * Constructor setting the display name and tooltip.
         *
         * @param aDisplayName display name
         * @param aTooltip tooltip text
         */
        private BarWidthOption(String aDisplayName, String aTooltip) {
            this.displayName = aDisplayName;
            this.tooltip = aTooltip;
        }
        //
        @Override
        public String getDisplayName() {
            return this.displayName;
        }
        //
        @Override
        public String getTooltipText() {
            return this.tooltip;
        }
    }

    //todo: clean up
    public static final boolean DEFAULT_DISPLAY_BAR_LABELS_SETTING = true;
    public static final boolean DEFAULT_DISPLAY_BAR_SHADOWS_SETTING = true;
    public static final boolean DEFAULT_DISPLAY_GRID_LINES_SETTING = true;
    public static final int DEFAULT_NUMBER_OF_DISPLAYED_CLUSTER = 10;
    public static final String HISTOGRAM_BARS_COLOR_HEX_VALUE = "#1E90FF"; //dodger blue
    public static final String HISTOGRAM_BARS_SELECTED_COLOR_HEX_VALUE = "#00008b"; //dark blue
    /**
     * Value for the width of the image corresponding to the structure of the fragments.
     */
    public static final double STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH = 250.0;
    /**
     * Value for the height of the image corresponding to the structure of the fragments.
     */
    public static final double STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT = 150.0;
    /**
     * Image zoom factor value.
     */
    public static final double STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR = 3.0;
    /**
     * Value for the small bar gap.
     */
    public static final double GUI_HISTOGRAM_SMALL_BAR_GAP_CONST = 3.5416;
    /**
     * Value fpr the medium bar gap.
     */
    public static final double GUI_HISTOGRAM_MEDIUM_BAR_GAP_CONST = 5.0;
    /**
     * Value for the large bar gap.
     */
    public static final double GUI_HISTOGRAM_LARGE_BAR_GAP_CONST = 6.5384;
    /**
     * Value for the small bar width.
     */
    public static final double GUI_HISTOGRAM_SMALL_BAR_WIDTH = 15.0;
    /**
     * Value for the medium bar width.
     */
    public static final double GUI_HISTOGRAM_MEDIUM_BAR_WIDTH = 20.0;
    /***
     * Value for the large bar width.
     */
    public static final double GUI_HISTOGRAM_LARGE_BAR_WIDTH = 30.0;
    /**
     * Value for the small histogram growth factor.
     */
    public static final double GUI_HISTOGRAM_SMALL_HISTOGRAM_HEIGHT_VALUE = 27.0;
    /**
     * Value for the medium histogram growth factor.
     */
    public static final double GUI_HISTOGRAM_MEDIUM_HISTOGRAM_HEIGHT_VALUE = 37.0;
    /**
     * Value for the large histogram growth factor.
     */
    public static final double GUI_HISTOGRAM_LARGE_HISTOGRAM_HEIGHT_VALUE = 50.0;
    /**
     * Value of the bar label sizes.
     */
    public static final double GUI_BAR_LABEL_SIZE = 10.0;


    public static final ClusterHistogramViewController.BarWidthOption DEFAULT_BAR_WIDTH = ClusterHistogramViewController.BarWidthOption.LARGE;

    private static final Logger LOGGER = Logger.getLogger(ClusterHistogramViewController.class.getName());

    private final IConfiguration configuration;
    private final SimpleIntegerProperty displayedClustersNumberSetting;
    private final SimpleIDisplayEnumConstantProperty barWidthSetting;
    private final SimpleBooleanProperty displayBarLabelsSetting;
    private final SimpleBooleanProperty displayBarShadowsSetting;
    private final SimpleBooleanProperty displayGridLinesSetting;
    private final List<Property<?>> settings;

    private double imageWidth;
    private double imageHeight;
    private double imageZoomFactor;

    private IAtomContainer atomContainerForDisplayCache;

    //Todo: type -> general: data transfer? when clustering?
    private List<?> clusterListCopy;
    private BarChart<Number, String> clusterHistogramChart;

    private ClusterHistogramView clusterHistogramView;
    private Stage clusterHistogramStage;
    private Scene clusterHistogramScene;


    public ClusterHistogramViewController(IConfiguration aConfiguration) {
        this.configuration = aConfiguration;
        //todo: init cap
        this.settings = new ArrayList<>();
        //Todo: update -> recalc clustering
        this.displayedClustersNumberSetting = new SimpleIntegerProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("ClusterHistogramView.displayedClusterNumberSetting.name"),
                ClusterHistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_CLUSTER) {
            @Override
            public void set(int newValue) throws NullPointerException, IllegalArgumentException {
                super.setValue(newValue);
                //value transferred to GUI in openHistogramView()
                //value updated in addListenersToHistogramView(), listener of apply-button
            }
        };
        this.settings.add(this.displayedClustersNumberSetting);
        this.barWidthSetting = new SimpleIDisplayEnumConstantProperty(this,
                "Bar width setting",
                ClusterHistogramViewController.DEFAULT_BAR_WIDTH,
                ClusterHistogramViewController.BarWidthOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                super.setValue(newValue);
            }
        };
        this.settings.add(this.barWidthSetting);
        this.displayBarLabelsSetting = new SimpleBooleanProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("Display labels on bars setting"),
                ClusterHistogramViewController.DEFAULT_DISPLAY_BAR_LABELS_SETTING) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //in addFrequencyBarLabelToBarAndAddListenersToBarCheckBoxes(), listener is added
            }
        };
        this.settings.add(this.displayBarLabelsSetting);
        this.displayBarShadowsSetting = new SimpleBooleanProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("HistogramView.displayBarShadowsSetting.name"),
                ClusterHistogramViewController.DEFAULT_DISPLAY_BAR_SHADOWS_SETTING) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //in addFrequencyBarLabelToBarAndAddListenersToBarCheckBoxes(), listener is added
            }
        };
        this.settings.add(this.displayBarShadowsSetting);
        this.displayGridLinesSetting = new SimpleBooleanProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("HistogramView.displayGridLinesSetting.name"),
                ClusterHistogramViewController.DEFAULT_DISPLAY_GRID_LINES_SETTING) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //value also used in createHistogram() and addListenersToHistogramView()
            }
        };
        this.settings.add(this.displayGridLinesSetting);
    };

    @Override
    public List<Property<?>> settingsProperties() {
        return List.of();
    }

    @Override
    public String getViewToolNameForDisplay() {
        return "";
    }

    @Override
    public void restoreDefaultSettings() {
        this.barWidthSetting.set(ClusterHistogramViewController.DEFAULT_BAR_WIDTH);
    }

    @Override
    public boolean canBeUsedOnTab(TabNames aTabNameEnumConstant) {
        return false;
    }


    public void openClusterHistogramView(
            Stage aMainStage,
            List<MoleculeDataModel> aRepresentativeMoleculeDataModelList) {
        //ToDO: routine for opening cluster histogram (see HistogramView class)
        Objects.requireNonNull(aMainStage, "Main stage is null.");
        Objects.requireNonNull(aRepresentativeMoleculeDataModelList, "Given MoleculeDataModel list is null.");
        this.imageWidth = ClusterHistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH;
        this.imageHeight = ClusterHistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT;
        this.imageZoomFactor = ClusterHistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR;

        this.clusterHistogramView = new ClusterHistogramView(10);
        //set setting values
        this.clusterHistogramView.getDisplayBarLabelsCheckBox().setSelected(this.displayBarLabelsSetting.get());
        this.clusterHistogramView.getDisplayGridLinesCheckBox().setSelected(this.displayGridLinesSetting.get());
        this.clusterHistogramView.getDisplayBarShadowsCheckBox().setSelected(this.displayBarShadowsSetting.get());
        //check bar width setting and set value
        String tmpCurrentlySetBarWidthOptionDisplayName = null;
        for (ClusterHistogramViewController.BarWidthOption tmpBarWidthOption : ClusterHistogramViewController.BarWidthOption.values()) {
            if (tmpBarWidthOption.equals(this.barWidthSetting.get())) {
                tmpCurrentlySetBarWidthOptionDisplayName = tmpBarWidthOption.getDisplayName();
            }
        }
        if (Objects.isNull(tmpCurrentlySetBarWidthOptionDisplayName)) {
            this.barWidthSetting.set(ClusterHistogramViewController.DEFAULT_BAR_WIDTH);
            tmpCurrentlySetBarWidthOptionDisplayName = ClusterHistogramViewController.DEFAULT_BAR_WIDTH.getDisplayName();
        }
        this.clusterHistogramView.getBarWidthsComboBox().setValue(tmpCurrentlySetBarWidthOptionDisplayName);

        this.clusterHistogramStage = new Stage();
        //this.addListenersToComponents();
        //Todo: calculateBarSpacing()
        Double[] tmpClusterHistogramBarSpacing = this.calculateBarSpacing(
                this.displayedClustersNumberSetting.get(),
                this.getBarWidthOptionEnumConstantFromDisplayName(
                        this.clusterHistogramView.getBarWidthsComboBox().getValue())
        );
        this.clusterHistogramChart = this.createClusterHistogram(
                this.displayedClustersNumberSetting.get(),
                this.clusterHistogramView,
                this.clusterHistogramView.getDisplayBarLabelsCheckBox(),
                this.clusterHistogramView.getDisplayBarShadowsCheckBox(),
                tmpClusterHistogramBarSpacing
        );
        this.clusterHistogramScene = new Scene(
                this.clusterHistogramView,
                GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE,
                GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE
        );
        this.clusterHistogramStage.initOwner(aMainStage);
        this.clusterHistogramStage.setTitle(Message.get("ClusterHistogramView.title"));
        String tmpIconURL = this.getClass().getClassLoader().getResource(
          this.configuration.getProperty("mortar.imagesFolder")
                  + this.configuration.getProperty("mortar.logo.icon.name")).toExternalForm();
        this.clusterHistogramStage.getIcons().add(new Image(tmpIconURL));
        this.clusterHistogramStage.setScene(this.clusterHistogramScene);
        this.addListenersToHistogramView();
        this.clusterHistogramStage.show();

    }

    private Double[] calculateBarSpacing(
            int aNumberOfDisplayedClusters,
            BarWidthOption aBarWidthOption
    ) {
        //Todo: see histogram for reference
        return null;
    }

    private BarWidthOption getBarWidthOptionEnumConstantFromDisplayName(String aDisplayName) {
        //Todo: see histogram for reference
        return null;
    }

    private BarChart<Number, String> createClusterHistogram(
            int aClusterNumber,
            ClusterHistogramView aClusterHistogramView,
            CheckBox aDisplayBarLabelsCheckBox,
            CheckBox aDisplayBarShadowsCheckBox,
            Double[] aClusterHistogramBarSpacing
    ) {
        //Todo: get number of clusters -> check
        //y axis (clusters)
        CategoryAxis tmpYAxis = new CategoryAxis();
        tmpYAxis.setTickLabelFill(Color.BLACK);
        tmpYAxis.setLabel(Message.get("ClusterHistogramViewController.YAxisLabel.text"));
        System.out.println("y axis");
        //x axis (individual cluster size)
        NumberAxis tmpXAxis = new NumberAxis();
        tmpXAxis.setSide(Side.TOP);
        tmpXAxis.setAutoRanging(false);
        tmpXAxis.setMinorTickCount(1);
        tmpXAxis.setForceZeroInRange(true);
        tmpXAxis.setTickLabelFill(Color.BLACK);
        tmpXAxis.setLabel(Message.get("ClusterHistogramViewController.XAxisLabel.text"));
        System.out.println("x axis");
        //create bar chart
        BarChart<Number, String> tmpClusterHistogramBarChart = null;
        try {
            tmpClusterHistogramBarChart = new BarChart<>(tmpXAxis, tmpYAxis);
        } catch (Exception e) {
            System.out.println(e);
        }
        tmpClusterHistogramBarChart.setCategoryGap(0.0);
        tmpClusterHistogramBarChart.setBarGap(0.0);
        System.out.println("bar chart");
        ScrollPane tmpScrollPane = aClusterHistogramView.getClusterHistogramScrollPane();
        tmpScrollPane.setContent(tmpClusterHistogramBarChart);
        System.out.println("scroll pane");
        //create chart data
        //type order:    x   ,    y
        XYChart.Series<Number, String> tmpChartSeries = new XYChart.Series<>();
        //Todo: tmp! replace with actual data routine
        for (int i = 0; i < 10; i++) {
            XYChart.Data<Number, String> tmpClusterToSizeData
                    = new XYChart.Data<>(i, new String("# " + i));
            StackPane tmpClusterHistogramBarStackPane = this.createStackPaneWithContextMenuAndStructureDisplayForBar(
                    aClusterHistogramView.getStructureDisplayImageView(),
                    "C"
            );
            tmpClusterHistogramBarStackPane.setStyle("-fx-bar-fill: " + ClusterHistogramViewController.HISTOGRAM_BARS_COLOR_HEX_VALUE);
            this.addListenersToBarComponents(
                    aDisplayBarLabelsCheckBox,
                    aDisplayBarShadowsCheckBox,
                    tmpClusterHistogramBarStackPane,
                    //Todo: tmp! cluster size
                    10
            );
            tmpClusterToSizeData.setNode(tmpClusterHistogramBarStackPane);
            tmpChartSeries.getData().add(tmpClusterToSizeData);
        }
        //Todo: call createStackPane...()
        System.out.println("chart series");


        //Todo: get list of cluster representatives + show when hover over cluster
        //layout + data add
        //todo: calc height value
        double tmpPlaceholder = 10;
        tmpClusterHistogramBarChart.setPrefHeight(tmpPlaceholder);
        tmpClusterHistogramBarChart.setMinHeight(tmpPlaceholder);
        tmpClusterHistogramBarChart.getData().add(tmpChartSeries);
        tmpClusterHistogramBarChart.setLegendVisible(false);
        tmpClusterHistogramBarChart.layout();
        tmpClusterHistogramBarChart.setHorizontalGridLinesVisible(this.displayGridLinesSetting.get());
        tmpClusterHistogramBarChart.setVerticalGridLinesVisible(this.displayGridLinesSetting.get());
        tmpClusterHistogramBarChart.setAnimated(false);
        System.out.println("bar chart settings + data");
        //style settings
        //return created cluster histogram
        return tmpClusterHistogramBarChart;

    }

    /**
     * Add listeners and/or text formatters to close button, displayed fragments number text field, maximum smiles
     * length text field, apply button, display grid lines checkbox, display SMILES codes on y-axis check box, width
     * and height properties of the scene for resizing.
     */
    private void addListenersToHistogramView() {
        //close histogram stage with close button and stage window close request
        this.clusterHistogramView.getCloseButton().setOnAction(event -> {
            this.clusterHistogramStage.close();
            this.clearAllGUICaches();
        });
        this.clusterHistogramStage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, (this::closeWindowEvent));
        //adding text formatter that only accepts integers and turns the input strings into those to the two text fields
        this.clusterHistogramView.getDisplayedClustersNumberTextField().setTextFormatter(
                new TextFormatter<>(GuiUtil.getStringToIntegerConverter(),
                        this.displayedClustersNumberSetting.get(), //default value
                        GuiUtil.getPositiveIntegerFilter(false))
        );
//        //disable apply button if displayed cluster number text field is empty
//        this.clusterHistogramView.getApplyButton().disableProperty().bind(
//                Bindings.isEmpty(this.clusterHistogramView.getDisplayedClustersNumberTextField().textProperty())
//        );
        //todo: change update to recalc of cluster -> remove apply button
        //apply button, update histogram
//        this.clusterHistogramView.getApplyButton().setOnAction(event -> {
//            //if both text fields are empty, "apply" is disabled, see above
//            // but if only one text field is empty, it is reset to default here along with its tied setting property
//            if (this.clusterHistogramView.getMaximumSMILESLengthTextFieldContent().isEmpty()) {
//                //maximum SMILES length text field is empty -> reset this setting to default and parse displayed
//                // fragments number setting from text field
//                this.displayedClustersNumberSetting.set(Integer.parseInt(this.clusterHistogramView.getDisplayedFragmentsNumberTextFieldContent()));
//                if (this.displayedClustersNumberSetting.get() > this.fragmentListCopy.size()) {
//                    GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
//                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Header"),
//                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Content"));
//                    //no resets of settings or text field content, the user has to take care of that
//                    return;
//                }
//                this.maximumSMILESLengthSetting.set(HistogramViewController.DEFAULT_MAX_SMILES_LENGTH);
//                this.clusterHistogramView.getMaximumSMILESLengthTextField().setText(String.valueOf(this.maximumSMILESLengthSetting.get()));
//            } else if (this.clusterHistogramView.getDisplayedFragmentsNumberTextFieldContent().isEmpty()) {
//                //displayed fragments nr text field is empty -> reset this setting to default and parse maximum SMILES length
//                // setting from text field
//                this.maximumSMILESLengthSetting.set(Integer.parseInt(this.clusterHistogramView.getMaximumSMILESLengthTextFieldContent()));
//                this.displayedClustersNumberSetting.set(Math.min(this.fragmentListCopy.size(), HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS));
//                this.clusterHistogramView.getDisplayedClustersNumberTextField().setText(String.valueOf(this.displayedClustersNumberSetting.get()));
//            } else {
//                //both text fields have values -> parse and check
//                this.displayedClustersNumberSetting.set(Integer.parseInt(this.clusterHistogramView.getDisplayedFragmentsNumberTextFieldContent()));
//                this.maximumSMILESLengthSetting.set(Integer.parseInt(this.clusterHistogramView.getMaximumSMILESLengthTextFieldContent()));
//                if (this.displayedClustersNumberSetting.get() > this.fragmentListCopy.size()) {
//                    GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
//                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Header"),
//                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Content"));
//                    //no resets of settings or text field content, the user has to take care of that
//                    return;
//                }
//            }
//            HistogramViewController.BarWidthOption tmpBarWidthSettingEnumValue = this.getBarWidthOptionEnumConstantFromDisplayName(
//                    this.clusterHistogramView.getBarWidthsComboBox().getValue());
//            this.barWidthSetting.set(tmpBarWidthSettingEnumValue);
//            Double[] tmpHistogramSizeGap = this.calculateBarSpacing(
//                    this.displayedClustersNumberSetting.get(),
//                    tmpBarWidthSettingEnumValue);
//            this.displayFrequencySetting.set(this.getFrequencyOptionEnumConstantFromDisplayName(this.clusterHistogramView.getFrequencyComboBox().getValue()));
//            this.clusterHistogramChart = this.createHistogram(
//                    this.displayedClustersNumberSetting.get(),
//                    this.clusterHistogramView,
//                    this.maximumSMILESLengthSetting.get(),
//                    this.clusterHistogramView.getDisplayBarLabelsCheckBox(),
//                    this.clusterHistogramView.getDisplayBarShadowsCheckBox(),
//                    tmpHistogramSizeGap[0]);
//            this.clusterHistogramChart.setCategoryGap(tmpHistogramSizeGap[1]);
//            boolean tmpDisplayGridLines = this.displayGridLinesSetting.get();
//            this.clusterHistogramChart.setVerticalGridLinesVisible(tmpDisplayGridLines);
//            this.clusterHistogramChart.setHorizontalGridLinesVisible(tmpDisplayGridLines);
//            boolean tmpDisplaySMILES = this.displaySMILESSetting.get();
//            this.categoryAxis.setTickMarkVisible(tmpDisplaySMILES);
//            this.categoryAxis.setTickLabelsVisible(tmpDisplaySMILES);
//        });

        this.clusterHistogramView.getDisplayGridLinesCheckBox().selectedProperty()
                .addListener((ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> {
                    this.clusterHistogramChart.setVerticalGridLinesVisible(newVal);
                    this.clusterHistogramChart.setHorizontalGridLinesVisible(newVal);
                    //update setting for persistence
                    this.displayGridLinesSetting.set(newVal);
                });
        this.clusterHistogramScene.widthProperty().addListener((observable, oldValue, newValue) -> {
            double tmpWidthChange = ((this.clusterHistogramScene.getWidth() - GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE) / GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE) * 100.0;
            double tmpImageWidthChange = (ClusterHistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH / 100.0) * tmpWidthChange;
            this.imageWidth = ClusterHistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH + tmpImageWidthChange;
            this.imageHeight = this.imageWidth - 100.0;
            this.imageZoomFactor = (ClusterHistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR / ClusterHistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH) * this.imageWidth;
        });
        this.clusterHistogramScene.heightProperty().addListener((observable, oldValue, newValue) -> {
            double tmpHeightChange = ((this.clusterHistogramScene.getHeight() - GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE) / GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE) * 100.0;
            double tmpImageHeightChange = (ClusterHistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT / 100.0) * tmpHeightChange;
            if (this.clusterHistogramScene.getWidth() == GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE) {
                this.imageHeight = ClusterHistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT + tmpImageHeightChange;
            } else {
                double tmpHeight = this.imageWidth - 100.0;
                double tmpIntermediateImageHeight = ClusterHistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT + tmpImageHeightChange;
                double tmpImageHeight = tmpHeight - tmpIntermediateImageHeight;
                this.imageHeight = tmpIntermediateImageHeight + tmpImageHeight;
            }
            this.imageWidth = 100.0 + this.imageHeight;
            this.imageZoomFactor = (ClusterHistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR / ClusterHistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH) * this.imageWidth;
        });
        this.clusterHistogramView.getBarWidthsComboBox().valueProperty().addListener((observable, oldValue, newValue) -> {
            this.clusterHistogramChart = this.createClusterHistogram(
                    //Todo: tmp! cluster value
                    10,
                    this.clusterHistogramView,
                    this.clusterHistogramView.getDisplayBarLabelsCheckBox(),
                    this.clusterHistogramView.getDisplayBarShadowsCheckBox(),
                    //Todo: tmp! bar spacing
                    this.calculateBarSpacing(
                            this.displayedClustersNumberSetting.get(),
                            this.getBarWidthOptionEnumConstantFromDisplayName(this.clusterHistogramView.getBarWidthsComboBox().getValue())
                    )
            );
            System.out.println("refreshed histogram");
        });
    }

    private void addListenersToBarComponents(
            CheckBox aLabelCheckBox,
            CheckBox aBarShadowCheckBox,
            StackPane aStackPane,
            int aClusterSize
    ) {
        int tmpDigitLength = String.valueOf(aClusterSize).length();
        Label tmpBarLabel = new Label();
        tmpBarLabel.setTranslateY(0.0);
        tmpBarLabel.setAlignment(Pos.CENTER_RIGHT);
        tmpBarLabel.setPrefWidth(ClusterHistogramViewController.GUI_BAR_LABEL_SIZE * tmpDigitLength);
        tmpBarLabel.setMinWidth(ClusterHistogramViewController.GUI_BAR_LABEL_SIZE * tmpDigitLength);
        tmpBarLabel.setMaxWidth(ClusterHistogramViewController.GUI_BAR_LABEL_SIZE * tmpDigitLength);
        tmpBarLabel.setTranslateX(tmpDigitLength * ClusterHistogramViewController.GUI_BAR_LABEL_SIZE + 5.0);
        tmpBarLabel.setStyle(null);
        tmpBarLabel.setText(String.valueOf(aClusterSize));
        if (this.displayBarLabelsSetting.get()) {
            aStackPane.getChildren().add(tmpBarLabel);
        }
        aLabelCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                aStackPane.getChildren().add(tmpBarLabel);
            } else {
                aStackPane.getChildren().remove(tmpBarLabel);
            }
            this.displayBarLabelsSetting.set(newVal);
        });
        if (this.displayBarShadowsSetting.get()) {
            aStackPane.setEffect(new DropShadow(10,2,3, Color.BLACK));
        }
        aBarShadowCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                aStackPane.setEffect(new DropShadow(10, 2, 3, Color.BLACK));
            } else {
                aStackPane.setEffect(null);
            }
            this.displayBarShadowsSetting.set(newVal);
        });
    }
    //Todo: add settings listeners
    private void addListenersToComponents() {
        //close cluster histogram
        this.clusterHistogramView.getCloseButton().setOnAction(event -> {
            this.clusterHistogramStage.close();
            this.clearAllGUICaches();
        });
        //apply changes to display
        //ToDo: determine what to do on apply -> "rerun clustering" button for clustering parameter change
        this.clusterHistogramView.getApplyButton().setOnAction(event -> {
            //apply
            //see histogram controller l.835
        });
        //ensures proper stage closure on window close request
        this.clusterHistogramStage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, (this::closeWindowEvent));
        //adding text formatter that only accepts integers and turns the input strings into those to the two text fields
        this.clusterHistogramView.getDisplayedClustersNumberTextField().setTextFormatter(
                new TextFormatter<>(GuiUtil.getStringToIntegerConverter(),
                        this.displayedClustersNumberSetting.get(), //default value
                        GuiUtil.getPositiveIntegerFilter(false))
        );
        this.clusterHistogramView.getDisplayGridLinesCheckBox().selectedProperty()
                .addListener((ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> {
                    this.clusterHistogramChart.setVerticalGridLinesVisible(newVal);
                    this.clusterHistogramChart.setHorizontalGridLinesVisible(newVal);
                    //update setting
                    this.displayGridLinesSetting.set(newVal);
                });
        this.clusterHistogramView.getDisplayBarShadowsCheckBox().selectedProperty()
                .addListener((ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> {
                    //Todo: stackpane for bar labels -> see histogram controller l.940
                    //Todo: add stackpane in createClusterHistogram
                    //Todo: add listeners to stackpane components -> bar styling -> histogram controller l.1055
                });

    }

    private StackPane createStackPaneWithContextMenuAndStructureDisplayForBar(
            ImageView anImageView,
            String aRepresentativeSmilesString
    ) {
        StackPane tmpStackPane = new StackPane();
        tmpStackPane.setAlignment(Pos.CENTER_RIGHT);
        MenuItem tmpCopySmilesMenuItem = new MenuItem(Message.get("ClusterHistogramViewController.MenuItemSmiles.text"));
        MenuItem tmpCopyStructureMenuItem = new MenuItem(Message.get("ClusterHistogramViewController.MenuItemStructure.text"));
        ContextMenu tmpContextMenu = new ContextMenu();
        tmpContextMenu.getItems().addAll(tmpCopySmilesMenuItem, tmpCopyStructureMenuItem);
        try {
            String tmpCopyIconURL = this.getClass().getClassLoader().getResource(
                    this.configuration.getProperty("mortar.imagesFolder")
                            + this.configuration.getProperty("mortar.icon.copy.name")).toExternalForm();
            tmpCopySmilesMenuItem.setGraphic(new ImageView(new Image(tmpCopyIconURL)));
            tmpCopyStructureMenuItem.setGraphic(new ImageView(new Image(tmpCopyIconURL)));
        } catch (NullPointerException | IllegalArgumentException anException) {
            ClusterHistogramViewController.LOGGER.log(Level.WARNING, "Copy icon for context menus could not be imported.");
        }
        //Event to display representative on hover
        EventHandler<MouseEvent> tmpMouseHoverEventHandler = event -> {
            tmpStackPane.setStyle("-fx-bar-fill: " + ClusterHistogramViewController.HISTOGRAM_BARS_SELECTED_COLOR_HEX_VALUE);
            this.atomContainerForDisplayCache = null;
            try {
                boolean tmpShouldBeKekulized = true;
                boolean tmpShouldAtomTypesBePerceived = true;
                this.atomContainerForDisplayCache = ChemUtil.parseSmilesToAtomContainer(aRepresentativeSmilesString, tmpShouldBeKekulized, tmpShouldAtomTypesBePerceived);
            } catch (CDKException anException) {
                // no logging, this happens too often, e.g. for fragments of aromatic rings
                try {
                    this.atomContainerForDisplayCache = ChemUtil.parseSmilesToAtomContainer(aRepresentativeSmilesString, false, false);
                } catch (CDKException aSecondException) {
                    ClusterHistogramViewController.LOGGER.log(Level.WARNING, aSecondException.toString(), aSecondException);
                    this.atomContainerForDisplayCache = null;
                    //Note: the used depiction method returns an error image if image creation fails, so nothing else to do here
                }
            }
            Image tmpImage = DepictionUtil.depictImageWithZoomAndFillToFitAndWhiteBackground(
                    this.atomContainerForDisplayCache,
                    this.imageZoomFactor,
                    this.imageWidth,
                    this.imageHeight,
                    false,
                    true);
            anImageView.setImage(tmpImage);
        };
        /* Event to open context menu (right click) to copy SMILES string or structure.
           Context menu also opens, if  a right click on the frequency label is detected.
         */
        EventHandler<ContextMenuEvent> tmpContextMenuEventHandler = event -> tmpContextMenu.show(tmpStackPane, event.getScreenX(), event.getScreenY());
        tmpStackPane.addEventHandler(MouseEvent.MOUSE_ENTERED, tmpMouseHoverEventHandler);
        tmpStackPane.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, tmpContextMenuEventHandler);
        // Listener ContextMenuItems
        tmpCopySmilesMenuItem.setOnAction(event -> {
            ClipboardContent tmpSmilesClipboardContent = new ClipboardContent();
            tmpSmilesClipboardContent.putString(aRepresentativeSmilesString);
            Clipboard.getSystemClipboard().setContent(tmpSmilesClipboardContent);
        });
        tmpCopyStructureMenuItem.setOnAction(event -> {
            ClipboardContent tmpStructureClipboardContent = new ClipboardContent();
            Image tmpCopyImageOnBar = DepictionUtil.depictImageWithZoomAndFillToFitAndWhiteBackground(
                    this.atomContainerForDisplayCache,
                    12.0,
                    GuiDefinitions.GUI_COPY_IMAGE_IMAGE_WIDTH,
                    GuiDefinitions.GUI_COPY_IMAGE_IMAGE_HEIGHT,
                    true,
                    true);
            tmpStructureClipboardContent.putImage(tmpCopyImageOnBar);
            Clipboard.getSystemClipboard().setContent(tmpStructureClipboardContent);
        });
        tmpStackPane.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            tmpStackPane.setStyle("-fx-bar-fill: " + ClusterHistogramViewController.HISTOGRAM_BARS_COLOR_HEX_VALUE);
            anImageView.setImage(null);
        });
        return tmpStackPane;
    }

    /**
     * Closes the cluster histogram view (stage) and clears all GUI caches when close window event was fired.
     *
     * @param anEvent WindowEvent
     */
    private void closeWindowEvent(WindowEvent anEvent) {
        this.clusterHistogramStage.close();
        this.clearAllGUICaches();
    }
    //Todo: set all class variables null
    private void clearAllGUICaches() {
        this.clusterHistogramView = null;
        this.clusterHistogramStage = null;
        this.clusterHistogramScene = null;
    }
    //extend HistogramViewController?
}
