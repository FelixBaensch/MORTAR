package de.unijena.cheminf.mortar.gui.views;

import de.unijena.cheminf.mortar.controller.ClusteringViewController;
import de.unijena.cheminf.mortar.controller.HistogramViewController;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.clustering.ClusteringService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashMap;

public class ClusteringView extends AnchorPane {
    /**
     * Button to close view
     */
    private Button closeButton;
    private Button vigilanceParameter1Button;
    private Button vigilanceParameter2Button;
    private Button vigilanceParameter3Button;
    private Button vigilanceParameter4Button;
    private Button vigilanceParameter5Button;
    private Button vigilanceParameter6Button;
    private Button vigilanceParameter7Button;
    private Button vigilanceParameter8Button;
    private Button vigilanceParameter9Button;
    private Button clusterRepresentativesButton;
    private Button applyButton;
    private TextField field;

    /**
     * ImageView to display the structures when the cursor hovers over a bar
     */
    private ImageView structureDisplayImageView;
    /**
     * Checkbox to choose to show or hide the bar labels that display the exact frequency.
     */
    private CheckBox displayBarLabelsCheckBox;
    /**
     * CheckBox to display or hide histogram gridlines
     */
    private CheckBox displayGridLinesCheckBox;
    /**
     * ScrollPane to make histogram scrollable
     */
    private ScrollPane clusteringViewScrollPane;
    /**
     * CheckBox to show or hide the bar shadows
     */
    private CheckBox barStylingCheckBox;
    /**
     * ComboBox to make the gap between the bars adjustable
     */
    private ComboBox barWidthsComboBox;

    /**
     * Constructor, layouts the view and all its components. Current settings need to be adjusted externally.
     *
     */
    public ClusteringView() {
        super();
        this.clusteringViewScrollPane = new ScrollPane();
        this.clusteringViewScrollPane.setFitToHeight(true);
        this.clusteringViewScrollPane.setFitToWidth(true);
        this.clusteringViewScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        this.clusteringViewScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        //borderPane
        BorderPane tmpBorderPane = new BorderPane();
        ClusteringView.setTopAnchor(tmpBorderPane, 0.0);
        ClusteringView.setRightAnchor(tmpBorderPane, 0.0);
        ClusteringView.setLeftAnchor(tmpBorderPane, 0.0);
        ClusteringView.setBottomAnchor(tmpBorderPane, 0.0);
        //mainGrid (4x4 grid)
        GridPane tmpMainGrid = new GridPane();
        RowConstraints tmpRow1 = new RowConstraints();
        tmpRow1.setVgrow(Priority.ALWAYS);
        tmpRow1.setFillHeight(true);
        tmpMainGrid.getRowConstraints().add(tmpRow1);
        ColumnConstraints tmpCol1 = new ColumnConstraints();
        tmpCol1.setFillWidth(true);
        tmpCol1.setHgrow(Priority.ALWAYS);
        tmpMainGrid.getColumnConstraints().add(tmpCol1);
        ColumnConstraints tmpCol2 = new ColumnConstraints();
        tmpMainGrid.getColumnConstraints().add(tmpCol2);
        RowConstraints tmpRow2 = new RowConstraints();
        tmpMainGrid.getRowConstraints().add(tmpRow2);
        ColumnConstraints tmpCol3 = new ColumnConstraints();
        tmpMainGrid.getColumnConstraints().add(tmpCol3);
        RowConstraints tmpRow3 = new RowConstraints();
        tmpMainGrid.getRowConstraints().add(tmpRow3);
        RowConstraints tmpRow4 = new RowConstraints(20); // magic number
        tmpMainGrid.getRowConstraints().add(tmpRow4);
        ColumnConstraints tmpCol4 = new ColumnConstraints(20); // magic number
        tmpMainGrid.getColumnConstraints().add(tmpCol4);
        //grids for controls
        GridPane tmpLeftSideGrid = new GridPane();
        GridPane tmpRightSideGrid = new GridPane();
        HBox tmpMainHBoxControls = new HBox();
        // main HBox containing grids for controls
        tmpMainHBoxControls.setStyle("-fx-background-color: LightGrey");
        tmpBorderPane.setBottom(tmpMainHBoxControls);
        tmpBorderPane.setCenter(tmpMainGrid);
        HBox tmpHBoxLeftSideControls = new HBox();
        // left side controls
        this.vigilanceParameter1Button = new Button();
        this.vigilanceParameter2Button = new Button("0.2");
        this.vigilanceParameter3Button = new Button("0.3");
        this.vigilanceParameter4Button = new Button("0.4");
        this.vigilanceParameter5Button = new Button("0.5");
        this.vigilanceParameter6Button = new Button("0.6");
        this.vigilanceParameter7Button = new Button("0.7");
        this.vigilanceParameter8Button = new Button("0.8");
        this.vigilanceParameter9Button = new Button("0.9");

        this.clusterRepresentativesButton = new Button("Representatives");
        this.barWidthsComboBox = new ComboBox<>();
        for (ClusteringViewController.BarWidthOption tmpBarWidthOptionConstant : ClusteringViewController.BarWidthOption.values()) {
            this.barWidthsComboBox.getItems().add(tmpBarWidthOptionConstant.getDisplayName());
        }
        tmpLeftSideGrid.setVgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpLeftSideGrid.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpLeftSideGrid.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        // grid positions
        tmpLeftSideGrid.add(this.vigilanceParameter1Button, 0,0);
        tmpLeftSideGrid.add(this.vigilanceParameter2Button,1,0);
        tmpLeftSideGrid.add(this.vigilanceParameter3Button,2,0);
        tmpLeftSideGrid.add(this.vigilanceParameter4Button,3,0);
        tmpLeftSideGrid.add(this.vigilanceParameter5Button,4,0);
        tmpLeftSideGrid.add(this.vigilanceParameter6Button,0,1);
        tmpLeftSideGrid.add(this.vigilanceParameter7Button,1,1);
        tmpLeftSideGrid.add(this.vigilanceParameter8Button,2,1);
        tmpLeftSideGrid.add(this.vigilanceParameter9Button,3,1);
        tmpLeftSideGrid.add(this.clusterRepresentativesButton, 5,1);
        tmpHBoxLeftSideControls.setAlignment(Pos.CENTER_LEFT);
        tmpHBoxLeftSideControls.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxLeftSideControls.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxLeftSideControls, Priority.ALWAYS);
        tmpHBoxLeftSideControls.getChildren().add(tmpLeftSideGrid);
        tmpMainHBoxControls.getChildren().add(tmpHBoxLeftSideControls);

        this.structureDisplayImageView = new ImageView();
        this.structureDisplayImageView.setEffect(new DropShadow(10,2,3, Color.BLACK));
        this.structureDisplayImageView.setStyle("fx-padding: 50px; fx-margin: 50px");
        // right side controls
        this.closeButton = new Button(Message.get("HistogramView.cancelButton.text"));
        this.closeButton.setTooltip(new Tooltip(Message.get("HistogramView.cancelButton.toolTip")));
        this.closeButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.closeButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.displayBarLabelsCheckBox = new CheckBox(Message.get("HistogramView.checkBox.text"));
        this.displayBarLabelsCheckBox.setTooltip(new Tooltip(Message.get("HistogramView.checkBox.toolTip")));
        this.displayGridLinesCheckBox = new CheckBox(Message.get("HistogramView.checkBoxGridlines.text"));
        this.displayGridLinesCheckBox.setTooltip(new Tooltip(Message.get("HistogramView.checkBoxGridlines.toolTip")));
        this.barStylingCheckBox = new CheckBox(Message.get("HistogramView.stylingCheckBox.text"));
        this.barStylingCheckBox.setTooltip(new Tooltip(Message.get("HistogramView.stylingCheckBox.tooltip")));
        HBox tmpHBoxRightSideControls = new HBox();
        tmpRightSideGrid.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpRightSideGrid.setVgap(GuiDefinitions.GUI_INSETS_VALUE * 2);
        tmpRightSideGrid.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        // grid positions
        tmpRightSideGrid.add(this.displayBarLabelsCheckBox,0,0);
        tmpRightSideGrid.add(this.displayGridLinesCheckBox,1,0);
        tmpRightSideGrid.add(this.barStylingCheckBox,0,1);
       // tmpRightSideGrid.add(this.clusterRepresentativesButton,0,1);
        tmpRightSideGrid.add(this.barWidthsComboBox,1,1);
        tmpRightSideGrid.add(this.closeButton, 2,1);
        tmpHBoxRightSideControls.getChildren().add(tmpRightSideGrid);
        tmpHBoxRightSideControls.setAlignment(Pos.CENTER_RIGHT);
        tmpHBoxRightSideControls.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxRightSideControls.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxRightSideControls, Priority.ALWAYS);
        tmpMainHBoxControls.getChildren().add(tmpHBoxRightSideControls);
        // main grid
        tmpMainGrid.add(this.clusteringViewScrollPane,0,0,4,4);
        tmpMainGrid.add(this.structureDisplayImageView,2,2);
        this.getChildren().add(tmpBorderPane);
    }
    /**
     * Returns the display bar labels check box, i.e. the frequency labels on the right-hand side of the bars.
     *
     * @return CheckBox for choosing whether to label the bars with frequencies
     */
    public CheckBox getDisplayBarLabelsCheckBox() {
        return this.displayBarLabelsCheckBox;
    }
    /**
     * Returns the display grid lines check box.
     *
     * @return CheckBox to show or display the grid lines in the histogram
     */
    public CheckBox getDisplayGridLinesCheckBox() {
        return this.displayGridLinesCheckBox;
    }
    /**
     * Returns the display bar shadows check box.
     *
     * @return CheckBox for displaying or hiding bar shadows
     */
    public CheckBox getDisplayBarShadowsCheckBox() {
        return this.barStylingCheckBox;
    }
    /**
     * Returns an ImageView to enable the display of the structures when the cursor hovers over a bar.
     *
     * @return ImageView shows the different structures when hovering over the histogram
     */
    public ImageView getStructureDisplayImageView() {
        return this.structureDisplayImageView;
    }
    /**
     * Returns a ScrollPane in which the histogram is to be displayed.
     *
     * @return ScrollPane to make histogram scrollable
     */
    public ScrollPane getHistogramScrollPane() {
        return this.clusteringViewScrollPane;
    }
    /**
     * Returns combo box for setting bar width of the histogram.
     *
     * @return ComboBox for setting bar widths
     */
    public ComboBox getBarWidthsComboBox() {
        return this.barWidthsComboBox;
    }
    public Button getVigilanceParameter1Button(){
        return this.vigilanceParameter1Button;
    }
    public Button getVigilanceParameter2Button(){
        return this.vigilanceParameter2Button;
    }
    public Button getVigilanceParameter3Button(){
        return this.vigilanceParameter3Button;
    }
    public Button getVigilanceParameter4Button(){
        return this.vigilanceParameter4Button;
    }
    public Button getVigilanceParameter5Button(){
        return this.vigilanceParameter5Button;
    }
    public Button getVigilanceParameter6Button(){
        return this.vigilanceParameter6Button;
    }
    public Button getVigilanceParameter7Button(){
        return this.vigilanceParameter7Button;
    }
    public Button getVigilanceParameter8Button(){
        return this.vigilanceParameter8Button;
    }
    public Button getVigilanceParameter9Button(){
        return this.vigilanceParameter9Button;
    }
    public Button getCloseButton() {
        return this.closeButton;
    }
    public HashMap<String,Button> getVigilanceParameterButtons() {
        HashMap<String,Button> tmpVigilanceParameterButtonToIndexMap = new HashMap<>(); // TODO add init value
        tmpVigilanceParameterButtonToIndexMap.put("1",this.vigilanceParameter1Button);
        tmpVigilanceParameterButtonToIndexMap.put("2",this.vigilanceParameter2Button);
        tmpVigilanceParameterButtonToIndexMap.put("3",this.vigilanceParameter3Button);
        tmpVigilanceParameterButtonToIndexMap.put("4",this.vigilanceParameter4Button);
        tmpVigilanceParameterButtonToIndexMap.put("5",this.vigilanceParameter5Button);
        tmpVigilanceParameterButtonToIndexMap.put("6",this.vigilanceParameter6Button);
        tmpVigilanceParameterButtonToIndexMap.put("7",this.vigilanceParameter7Button);
        tmpVigilanceParameterButtonToIndexMap.put("8",this.vigilanceParameter8Button);
        tmpVigilanceParameterButtonToIndexMap.put("9",this.vigilanceParameter9Button);
        return tmpVigilanceParameterButtonToIndexMap;
    }
    public HashMap<Button, String> getVigilanceButtons() {
        HashMap<Button, String> tmpVigilanceParameterButtonToIndexMap = new HashMap<>(); // TODO add init value
        tmpVigilanceParameterButtonToIndexMap.put(this.vigilanceParameter1Button,"1");
        tmpVigilanceParameterButtonToIndexMap.put(this.vigilanceParameter2Button, "2");
        tmpVigilanceParameterButtonToIndexMap.put(this.vigilanceParameter3Button, "3");
        tmpVigilanceParameterButtonToIndexMap.put(this.vigilanceParameter4Button, "4");
        tmpVigilanceParameterButtonToIndexMap.put(this.vigilanceParameter5Button, "5");
        tmpVigilanceParameterButtonToIndexMap.put(this.vigilanceParameter6Button, "6");
        tmpVigilanceParameterButtonToIndexMap.put(this.vigilanceParameter7Button, "7");
        tmpVigilanceParameterButtonToIndexMap.put(this.vigilanceParameter8Button, "8");
        tmpVigilanceParameterButtonToIndexMap.put(this.vigilanceParameter9Button, "9");
        return tmpVigilanceParameterButtonToIndexMap;
    }
    public ArrayList<Button> getButtons() {
        ArrayList<Button> tmpButtonsList = new ArrayList<>(9);
        tmpButtonsList.add(this.vigilanceParameter1Button);
        tmpButtonsList.add(this.vigilanceParameter2Button);
        tmpButtonsList.add(this.vigilanceParameter3Button);
        tmpButtonsList.add(this.vigilanceParameter4Button);
        tmpButtonsList.add(this.vigilanceParameter5Button);
        tmpButtonsList.add(this.vigilanceParameter6Button);
        tmpButtonsList.add(this.vigilanceParameter7Button);
        tmpButtonsList.add(this.vigilanceParameter8Button);
        tmpButtonsList.add(this.vigilanceParameter9Button);
        return tmpButtonsList;
    }
    public Button getClusterRepresentativesButton() {
        return this.clusterRepresentativesButton;
    }

}
