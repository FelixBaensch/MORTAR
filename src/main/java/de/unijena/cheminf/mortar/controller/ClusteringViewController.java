package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.art2aClustering.interfaces.IArt2aClustering;
import de.unijena.cheminf.art2aClustering.interfaces.IArt2aClusteringResult;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.views.ClusteringView;
import de.unijena.cheminf.mortar.gui.views.HistogramView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClusteringViewController implements IViewToolController {
    //<editor-fold desc="Enums" defaultstate="collapsed">
    /**
     * Enum for the available bar spacing width options.
     */
    public static enum BarWidthOption {
        /**
         * Small bar width
         */
        SMALL(Message.get("HistogramView.barWidths.small")),
        /**
         * Medium bar width
         */
        MEDIUM(Message.get("HistogramView.barWidths.medium")),
        /**
         * Large bar width
         */
        LARGE(Message.get("HistogramView.barWidths.large"));
        /**
         * A name for the respective constant that is meant for display, i.e. taken from the Message file.
         */
        private final String displayName;
        /**
         * Constructor that sets the display name.
         *
         * @param aDisplayName a name for the respective constant that is meant for display, i.e. taken from the Message file.
         */
        BarWidthOption(String aDisplayName) {
            this.displayName = aDisplayName;
        }
        /**
         * Returns a name for the respective constant that is meant for display, i.e. taken from the Message file.
         *
         * @return display name of the constant
         */
        public String getDisplayName() {
            return this.displayName;
        }
    }
    /**
     * Default of bar spacing width.
     */
    public static final ClusteringViewController.BarWidthOption DEFAULT_BAR_WIDTH = ClusteringViewController.BarWidthOption.LARGE;
    /**
     * Default for whether bar labels (the fragment frequencies) should be displayed.
     */
    public static final boolean DEFAULT_DISPLAY_BAR_LABELS_SETTING = true;
    /**
     * Default for whether bar shadows should be displayed in the histogram.
     */
    public static final boolean DEFAULT_DISPLAY_BAR_SHADOWS_SETTING = true;
    /**
     * Default for whether to display vertical and horizontal grid lines in the histogram.
     */
    public static final boolean DEFAULT_DISPLAY_GRID_LINES_SETTING = false;
    /**
     * Default for whether the fragment SMILES codes should be displayed on the y-axis.
     */
    /**
     * View tool name for display in the GUI.
     */
    public static final String VIEW_TOOL_NAME_FOR_DISPLAY = "Clustering Result";
    /**
     * Color for the histogram bars.
     */
    public static final String HISTOGRAM_BARS_COLOR_HEX_VALUE = "#1E90FF"; //dodger blue
    /**
     * Color for the histogram bars when the mouse hovers over them.
     */
    public static final String HISTOGRAM_BARS_SELECTED_COLOR_HEX_VALUE = "#00008b"; //dark blue
    /**
     * Value for the width of the image corresponding to the structure of the fragments
     */
    public static final double STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH = 250.0;
    /**
     * Value for the height of the image corresponding to the structure of the fragments
     */
    public static final double STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT = 150.0;
    /**
     * Image zoom factor value
     */
    public static final double STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR = 3.0;
    /**
     * Value for the small bar gap
     */
    public static final double GUI_HISTOGRAM_SMALL_BAR_GAP_CONST = 3.5416;
    /**
     * Value for the medium bar gap
     */
    public static final double GUI_HISTOGRAM_MEDIUM_BAR_GAP_CONST = 5.0;
    /**
     * Value for the large bar gap
     */
    public static final double GUI_HISTOGRAM_LARGE_BAR_GAP_CONST = 6.5384;
    /**
     * Value for the small bar width
     */
    public static final double GUI_HISTOGRAM_SMALL_BAR_WIDTH = 15.0;
    /**
     * Value for the medium bar width
     */
    public static final double GUI_HISTOGRAM_MEDIUM_BAR_WIDTH = 20.0;
    /***
     * Value for the large bar width
     */
    public static final double GUI_HISTOGRAM_LARGE_BAR_WIDTH = 30.0;
    /**
     * Value for the small histogram growth factor
     */
    public static final double GUI_HISTOGRAM_SMALL_HISTOGRAM_HEIGHT_VALUE = 27.0;
    /**
     * Value for the medium histogram growth factor
     */
    public static final double GUI_HISTOGRAM_MEDIUM_HISTOGRAM_HEIGHT_VALUE = 37.0;
    /**
     * Value for the large histogram growth factor
     */
    public static final double GUI_HISTOGRAM_LARGE_HISTOGRAM_HEIGHT_VALUE = 50.0;
    /**
     * Value of the bar label sizes
     */
    public static final double GUI_BAR_LABEL_SIZE = 10.0;
    /**
     * Value of tickLabel length
     */
    public static final double HISTOGRAM_TICK_LABEL_LENGTH = 15.0;
    /**
     * Value of tickLabel gap
     */
    public static final double HISTOGRAM_TICK_LABEL_GAP = 10.0;
    /**
     * Logger of this class
     */
    private static final Logger LOGGER = Logger.getLogger(HistogramViewController.class.getName());
    /**
     * Setting for whether to display bar labels, i.e. the frequencies of the respective fragment.
     */
    private final SimpleBooleanProperty displayBarLabelsSetting;
    /**
     * Setting for width of spaces between the histogram bars.
     */
    private final SimpleEnumConstantNameProperty barWidthSetting;
    /**
     * Setting for whether to display bar shadows.
     */
    private final SimpleBooleanProperty displayBarShadowsSetting;
    /**
     * Setting for whether to display vertical and horizontal grid lines in the histogram.
     */
    private final SimpleBooleanProperty displayGridLinesSetting;
    /**
     * All settings of this view tool, encapsulated in JavaFX properties for binding in GUI and persistence.
     */
    private final List<Property> settings;
    /**
     * Stage of the main application view
     */
    private Stage mainStage;
    /**
     * HistogramView to display
     */
    private ClusteringView clusteringView;
    /**
     * Stage of the HistogramView
     */
    private Stage histogramStage;
    /**
     * Scene of the histogram stage
     */
    private Scene histogramScene;
    private IArt2aClusteringResult[] clusteringResult;
    /**
     * Width of molecule depictions displayed when the cursor hovers over a bar. Changes when histogram is resized.
     */
    private double imageWidth;
    /**
     * Height of molecule depictions displayed when the cursor hovers over a bar. Changes when histogram is resized.
     */
    private double imageHeight;
    /**
     * Zoom factor of molecule depictions displayed when the cursor hovers over a bar. Changes when histogram is resized.
     */
    private double imageZoomFactor;
    /**
     * Y-axis of the histogram
     */
    private CategoryAxis categoryAxis;
    /**
     * X-axis of the histogram
     */
    private NumberAxis numberAxis;
    /**
     * Histogram chart
     */
    private BarChart histogramChart;
    /**
     * Atom container to depict the respective structure when the cursor hovers over a bar
     */
    private IAtomContainer atomContainerForDisplayCache;

    /**
     * Constructor, initialises all settings with their default values. Does *not* open the view.
     */
    public ClusteringViewController() {
        this.settings = new ArrayList<>(4);
        this.displayBarLabelsSetting = new SimpleBooleanProperty(this,
                //the name could be displayed but is not used for that currently
                "Bar Label",
                ClusteringViewController.DEFAULT_DISPLAY_BAR_LABELS_SETTING) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //in addFrequencyBarLabelToBarAndAddListenersToBarCheckBoxes(), listener is added
            }
        };
        this.barWidthSetting = new SimpleEnumConstantNameProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("HistogramView.barWidthSetting.name"),
                HistogramViewController.DEFAULT_BAR_WIDTH.name(),
                HistogramViewController.BarWidthOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //value updated in addListenersToHistogramView(), listener of apply-button
            }
        };
        this.settings.add(this.barWidthSetting);
        this.settings.add(this.displayBarLabelsSetting);
        this.displayBarShadowsSetting = new SimpleBooleanProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("HistogramView.displayBarShadowsSetting.name"),
                ClusteringViewController.DEFAULT_DISPLAY_BAR_SHADOWS_SETTING) {
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
                ClusteringViewController.DEFAULT_DISPLAY_GRID_LINES_SETTING) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //value also used in createHistogram() and addListenersToHistogramView()
            }
        };
        this.settings.add(this.displayGridLinesSetting);

    }

    @Override
    public List<Property> settingsProperties() {
        return this.settings;
    }

    @Override
    public String getViewToolNameForDisplay() {
        return ClusteringViewController.VIEW_TOOL_NAME_FOR_DISPLAY;
    }

    @Override
    public void restoreDefaultSettings() {
        this.displayBarLabelsSetting.set(ClusteringViewController.DEFAULT_DISPLAY_BAR_LABELS_SETTING);
        this.displayBarShadowsSetting.set(ClusteringViewController.DEFAULT_DISPLAY_BAR_SHADOWS_SETTING);
        this.displayGridLinesSetting.set(ClusteringViewController.DEFAULT_DISPLAY_GRID_LINES_SETTING);
        this.barWidthSetting.set(ClusteringViewController.DEFAULT_BAR_WIDTH.name());

    }

    @Override
    public boolean canBeUsedOnTab(TabNames aTabNameEnumConstant) {
        return switch (aTabNameEnumConstant) {
            case FRAGMENTS, ITEMIZATION -> true;
            default -> false;
        };
    }

    public void openClusteringView(Stage aMainStage, IArt2aClusteringResult[] aClusteringResult) throws NullPointerException {
        //<editor-fold desc="Checks" defaultstate="collapsed">
        Objects.requireNonNull(aMainStage, "Main stage is null.");
        //</editor-fold>
        //
        //<editor-fold desc="initialisation of stage and view" defaultstate="collapsed">
        //note: these need to be reset to default here because the histogram view is re-initialised with default size and
        // the image dimensions are adjusted when the view is resized
        this.imageWidth = ClusteringViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH;
        this.imageHeight = ClusteringViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT;
        this.imageZoomFactor = ClusteringViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR;
        this.mainStage = aMainStage;
        this.clusteringResult = aClusteringResult;
        this.histogramStage = new Stage();
        this.histogramStage.initModality(Modality.WINDOW_MODAL);
        this.histogramStage.initOwner(this.mainStage);
        this.histogramStage.setTitle(Message.get("HistogramView.title"));
        this.histogramStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.histogramStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        InputStream tmpImageInputStream = ClusteringViewController.class.getResourceAsStream("/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png");
        if (!Objects.isNull(tmpImageInputStream)) {
            this.histogramStage.getIcons().add(new Image(tmpImageInputStream));
        } else {
            ClusteringViewController.LOGGER.log(Level.WARNING, "Mortar_Logo_Icon1.png could not be imported.");
        }
        this.clusteringView = new ClusteringView();
        //</editor-fold>
        //
        //<editor-fold desc="set setting values in view" defaultstate="collapsed>
        //note: no binding between the text field and the setting property is established because the setting is only updated
        // when the "apply" button is clicked
        String tmpCurrentlySetBarWidthOptionDisplayName = null;
        for (HistogramViewController.BarWidthOption tmpBarWidthOption : HistogramViewController.BarWidthOption.values()) {
            if (tmpBarWidthOption.name().equals(this.barWidthSetting.get())) {
                tmpCurrentlySetBarWidthOptionDisplayName = tmpBarWidthOption.getDisplayName();
            }
        }
        if (Objects.isNull(tmpCurrentlySetBarWidthOptionDisplayName)) {
            this.barWidthSetting.set(HistogramViewController.DEFAULT_BAR_WIDTH.name());
            tmpCurrentlySetBarWidthOptionDisplayName = HistogramViewController.DEFAULT_BAR_WIDTH.getDisplayName();
        }
        this.clusteringView.getBarWidthsComboBox().setValue(tmpCurrentlySetBarWidthOptionDisplayName);
        //note: settings are updated for persistence in the listeners on the checkboxes added below
        this.clusteringView.getDisplayBarLabelsCheckBox().setSelected(this.displayBarLabelsSetting.get());
        this.clusteringView.getDisplayBarShadowsCheckBox().setSelected(this.displayBarShadowsSetting.get());
        this.clusteringView.getDisplayGridLinesCheckBox().setSelected(this.displayGridLinesSetting.get());
        //</editor-fold>
        //
        //<editor-fold desc="Initialise histogram with given settings and add listeners" defaultstate="collapsed">
        Double[] tmpHistogramHeightFactorAndCategoryGap = this.calculateBarSpacing(
                aClusteringResult[0].getNumberOfDetectedClusters(),
                this.getBarWidthOptionEnumConstantFromDisplayName((String)this.clusteringView.getBarWidthsComboBox().getValue()));
        this.histogramChart = this.createHistogram(this.clusteringView, this.clusteringView.getDisplayBarLabelsCheckBox(), this.clusteringView.getDisplayBarShadowsCheckBox(),tmpHistogramHeightFactorAndCategoryGap[0]);
        this.histogramChart.setCategoryGap(tmpHistogramHeightFactorAndCategoryGap[1]);
        this.histogramScene = new Scene(
                this.clusteringView,
                GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE,
                GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.histogramStage.setScene(this.histogramScene);
        // this.addListenersToHistogramView();
        this.histogramStage.show();
        //</editor-fold>
    }
    private Double[] calculateBarSpacing(int aNumberOfDisplayedFragments, ClusteringViewController.BarWidthOption aBarWidthOptionConstant) {
        Double[] tmpHistogramHeightFactorAndCategoryGap = new Double[2];
        double tmpCurrentHistogramHeight;
        double tmpGapDeviation;
        double tmpGapSpacing;
        double tmpCategoryGap;
        double tmpFinalHistogramHeight = 0.0; //return value is initialised here with a default value
        double tmpFinalGapSpacing;
        switch (aBarWidthOptionConstant) {
            case SMALL:
                if (aNumberOfDisplayedFragments <= 24) { //magic number
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / (double) aNumberOfDisplayedFragments;
                    tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 24.0);
                    tmpGapSpacing = HistogramViewController.GUI_HISTOGRAM_SMALL_BAR_GAP_CONST * tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing - HistogramViewController.GUI_HISTOGRAM_SMALL_BAR_WIDTH;
                } else {
                    tmpFinalHistogramHeight = HistogramViewController.GUI_HISTOGRAM_SMALL_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight * (double) aNumberOfDisplayedFragments - 85.0;
                    tmpGapSpacing = tmpCurrentHistogramHeight / aNumberOfDisplayedFragments;
                    tmpCategoryGap = tmpGapSpacing - HistogramViewController.GUI_HISTOGRAM_SMALL_BAR_WIDTH;
                }
                break;
            case MEDIUM:
                if (aNumberOfDisplayedFragments <= 17) { //magic number
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / (double) aNumberOfDisplayedFragments;
                    tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 17.0);
                    tmpGapSpacing = HistogramViewController.GUI_HISTOGRAM_MEDIUM_BAR_GAP_CONST * tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing - HistogramViewController.GUI_HISTOGRAM_MEDIUM_BAR_WIDTH;
                } else {
                    tmpFinalHistogramHeight = HistogramViewController.GUI_HISTOGRAM_MEDIUM_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight * (double) aNumberOfDisplayedFragments - 85.0;
                    tmpGapSpacing = tmpCurrentHistogramHeight / (double) aNumberOfDisplayedFragments;
                    tmpCategoryGap = tmpGapSpacing - HistogramViewController.GUI_HISTOGRAM_MEDIUM_BAR_WIDTH ;
                }
                break;
            case LARGE:
            default:
                if (aNumberOfDisplayedFragments <= 13) { //magic number
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / (double) aNumberOfDisplayedFragments;
                    tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 13.0);
                    tmpGapSpacing = HistogramViewController.GUI_HISTOGRAM_LARGE_BAR_GAP_CONST * tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing - HistogramViewController.GUI_HISTOGRAM_LARGE_BAR_WIDTH;
                } else {
                    tmpFinalHistogramHeight = HistogramViewController.GUI_HISTOGRAM_LARGE_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight * (double) aNumberOfDisplayedFragments - 85.0;
                    tmpGapSpacing = tmpCurrentHistogramHeight / (double) aNumberOfDisplayedFragments;
                    tmpCategoryGap = tmpGapSpacing - HistogramViewController.GUI_HISTOGRAM_LARGE_BAR_WIDTH;
                }
                break;
        }
        tmpHistogramHeightFactorAndCategoryGap[0] = tmpFinalHistogramHeight;
        tmpHistogramHeightFactorAndCategoryGap[1] = tmpCategoryGap;
        return tmpHistogramHeightFactorAndCategoryGap;
    }
    private ClusteringViewController.BarWidthOption getBarWidthOptionEnumConstantFromDisplayName(String aDisplayName) {
        if(Objects.isNull(aDisplayName) || aDisplayName.isBlank()) {
            ClusteringViewController.LOGGER.log(Level.WARNING, "Given string is null or empty, default bar width" +
                    "option is returned.");
            return ClusteringViewController.DEFAULT_BAR_WIDTH;
        }
        ClusteringViewController.BarWidthOption tmpEnumConstantBarWidth = null;
        for (ClusteringViewController.BarWidthOption tmpOption : ClusteringViewController.BarWidthOption.values()) {
            if (tmpOption.getDisplayName().equals(aDisplayName)) {
                tmpEnumConstantBarWidth = tmpOption;
            }
        }
        if (Objects.isNull(tmpEnumConstantBarWidth)) {
            ClusteringViewController.LOGGER.log(Level.WARNING, "Output of histogram view bar spacing combo box \""
                    + aDisplayName + "\"did not equal any of the pre-set enum values and was reset to default.");
            tmpEnumConstantBarWidth = ClusteringViewController.DEFAULT_BAR_WIDTH;
        }
        return tmpEnumConstantBarWidth;
    }
    private BarChart createHistogram(ClusteringView aClusteringView, CheckBox aBarLabelCheckBox, CheckBox aStyingCheckBox, double aHistogramDefaultSize) {
        this.categoryAxis = new CategoryAxis();
        this.categoryAxis.setTickLabelFill(Color.BLACK);
        this.categoryAxis.setTickLength(HistogramViewController.HISTOGRAM_TICK_LABEL_LENGTH);
        this.categoryAxis.setTickLabelGap(HistogramViewController.HISTOGRAM_TICK_LABEL_GAP);
        this.categoryAxis.setLabel(Message.get("HistogramViewController.YAxisLabel.text"));
        this.categoryAxis.setTickMarkVisible(true);
        this.categoryAxis.setTickLabelsVisible(true);
        this.numberAxis = new NumberAxis();
        this.numberAxis.setSide(Side.TOP);
        this.numberAxis.setAutoRanging(false);
        this.numberAxis.setMinorTickCount(1);
        this.numberAxis.setForceZeroInRange(true);
        this.numberAxis.setTickLabelFill(Color.BLACK);
        this.numberAxis.setLabel(Message.get("HistogramViewController.XAxisLabel.text"));
        BarChart tmpHistogramBarChart = new BarChart(this.numberAxis, this.categoryAxis);
        tmpHistogramBarChart.setCategoryGap(0.0);
        tmpHistogramBarChart.setBarGap(0.0);
        ScrollPane tmpScrollPane = aClusteringView.getHistogramScrollPane();
        tmpScrollPane.setContent(tmpHistogramBarChart);
        ArrayList<Integer> tmpNumberOfDetectedClusters = new ArrayList<>();
        ArrayList<Integer> tmpNumberOfEpochs = new ArrayList<>();
        ArrayList<ArrayList<int[]>> listOfLists = new ArrayList<>();
        ArrayList<Integer> tmpClusterRepresentatives = new ArrayList<>();
        for(IArt2aClusteringResult tmpClusteringResult : this.clusteringResult) {
            System.out.println(tmpClusteringResult.getVigilanceParameter() + "----------vigilance parameter");
            tmpNumberOfDetectedClusters.add(tmpClusteringResult.getNumberOfDetectedClusters());
            tmpNumberOfEpochs.add(tmpClusteringResult.getNumberOfEpochs());
            ArrayList<int[]> tmpClusterIndices = new ArrayList<>();
            for(int i = 0; i < tmpClusteringResult.getNumberOfDetectedClusters(); i++) {
                tmpClusterIndices.add(tmpClusteringResult.getClusterIndices(i));
                tmpClusterRepresentatives.add(tmpClusteringResult.getClusterRepresentatives(i));
            }
            listOfLists.add(tmpClusterIndices);
        }
        ArrayList<int[]> a = listOfLists.get(0); // TODO 0 ersetzen durch ein Button property
        int maxLength = 0;
        int[] maxLengthArray = null;

        for (int[] array : a) {
            if (array.length > maxLength) {
                maxLength = array.length;
                maxLengthArray = array;
            }
        }
        double tmpMaxFrequency = maxLengthArray.length;
        double tmpXAxisTicks = 0.05 * tmpMaxFrequency; // magic number
        double tmpXAxisExtension = 0.15 * tmpMaxFrequency; // magic number
        int tmpIntTmpXAxisTick = (int) Math.round(tmpXAxisTicks);
        int tmpIntXAxisExtension = (int) Math.round(tmpXAxisExtension);
        if (tmpIntTmpXAxisTick == 0 || tmpIntXAxisExtension == 0) {
            this.numberAxis.setTickUnit(1.0);
            this.numberAxis.setUpperBound(tmpMaxFrequency + 1.0);
        } else {
            if (tmpIntTmpXAxisTick < 10) {
                this.numberAxis.setTickUnit(tmpIntTmpXAxisTick);
                this.numberAxis.setUpperBound(this.calculateXAxisUpperBoundWithSpaceForLabels((int) tmpMaxFrequency, tmpIntTmpXAxisTick));
            } else {
                int tmpNewXAxisTick = tmpIntTmpXAxisTick;
                String tmpTickStringRepresentation = String.valueOf(tmpNewXAxisTick);
                String tmpFirstValue = String.valueOf(tmpTickStringRepresentation.charAt(0));
                int tmpFirstIntValue = Integer.parseInt(tmpFirstValue);
                if (tmpFirstIntValue <= 5) {
                    //If the first digit of tmpIntTmpXAxisTick is smaller than 5, we look for a suitable "round" number
                    // for the ticks, e.g. tmpIntTmpXAxisTick = 356 -> tmpNewXAxisTick = 400
                    int tmpDigit = tmpTickStringRepresentation.length() - 1;
                    int tmpPowerOfTen = (int) Math.pow(10, tmpDigit);
                    if (tmpNewXAxisTick % tmpPowerOfTen != 0) {
                        do {
                            tmpNewXAxisTick++;
                        } while (tmpNewXAxisTick % (tmpPowerOfTen) != 0);
                    }
                } else {
                    //If the first digit of tmpIntTmpXAxisTick is greater than 5, we choose a suitable, "round" power of 10 for
                    // the ticks, e.g. tmpIntTmpXAxisTick = 7896 -> tmpNewXAxisTick = 10.000
                    tmpNewXAxisTick = (int) Math.pow(10, tmpTickStringRepresentation.length());
                }
                this.numberAxis.setTickUnit(tmpNewXAxisTick);
                this.numberAxis.setUpperBound(this.calculateXAxisUpperBoundWithSpaceForLabels((int) tmpMaxFrequency, tmpNewXAxisTick));
            }
        }
        XYChart.Series tmpSeries = new XYChart.Series();
        for(int i = 0; i<tmpNumberOfDetectedClusters.get(0); i++) {
            XYChart.Data<Number, String> tmpTestData = new XYChart.Data<>(a.get(i).length,String.valueOf(i));
            tmpSeries.getData().add(tmpTestData);
        }
        tmpHistogramBarChart.getData().add(tmpSeries);
        return tmpHistogramBarChart;

    }
    private int calculateXAxisUpperBoundWithSpaceForLabels(int aMaxValue, int aTickValue) {
        int tmpTickNumber = (int) Math.round(aMaxValue / aTickValue);
        int tmpXAxisExtensionValue;
        if ((aTickValue * tmpTickNumber) > aMaxValue) {
            tmpXAxisExtensionValue = (aTickValue * tmpTickNumber) + aTickValue;
        } else {
            tmpXAxisExtensionValue = (aTickValue * tmpTickNumber) + (2 * aTickValue);
        }
        return tmpXAxisExtensionValue;
    }
}