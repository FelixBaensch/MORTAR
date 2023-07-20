package de.unijena.cheminf.mortar.gui.views;

import de.unijena.cheminf.mortar.controller.ClusteringViewController;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.message.Message;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;

public class ClusteringView extends AnchorPane {
    public final String VIEW_STYLE_FOR_ART_2A_CLUSTERING = "ART 2-A Clustering";
    /**
     * Button to close view
     */
    private Button closeButton;
    private ToggleButton vigilanceParameter1Button;
    private ToggleButton vigilanceParameter2Button;
    private ToggleButton vigilanceParameter3Button;
    private ToggleButton vigilanceParameter4Button;
    private ToggleButton vigilanceParameter5Button;
    private ToggleButton vigilanceParameter6Button;
    private ToggleButton vigilanceParameter7Button;
    private ToggleButton vigilanceParameter8Button;
    private ToggleButton vigilanceParameter9Button;
    private Button clusterRepresentativesButton;
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
     *
     */
    private ToggleGroup toggleGroup;

    /**
     * Constructor, layouts the view and all its components. Current settings need to be adjusted externally.
     *
     */
    public ClusteringView(String aClusteringTypToStyleClusteringView) {
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
        VBox vbox = new VBox();
        Color borderColor1 = Color.LIGHTGREY;
        double borderWidth1 = 1;
        double cornerRadius1 = 10; // Sie können die Rundung je nach Bedarf anpassen
        BorderStroke borderStroke1 = new BorderStroke(
                borderColor1, BorderStrokeStyle.SOLID, new CornerRadii(cornerRadius1), new BorderWidths(borderWidth1));
        DropShadow dropShadow1 = new DropShadow(10, Color.GRAY);
        Color backgroundColor1 = Color.LIGHTGREY;
        BackgroundFill backgroundFill1 = new BackgroundFill(backgroundColor1, new CornerRadii(cornerRadius1), null);
        Background background1 = new Background(backgroundFill1);

        // main HBox containing grids for controls
        tmpMainHBoxControls.setStyle("-fx-background-color: LightGrey");
        tmpBorderPane.setBottom(tmpMainHBoxControls);
        tmpBorderPane.setCenter(tmpMainGrid);
        HBox tmpHBoxLeftSideControls = new HBox();
        tmpHBoxLeftSideControls.setSpacing(10);
        HBox tmplabelContainer = new HBox();
        tmplabelContainer.setSpacing(30);
       // tmplabelContainer.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        // left side controls
        Label tmpDescriptionRoughLabel = new Label("rough...");
        Label tmpVigilanceParameterLabel = new Label("vigilance parameter");
        Label tmpDescriptionFineLabel = new Label("...fine");
        Label tmpDummyLabel = new Label(" ");
        /*
        tmpDescriptionFineLabel.setPrefWidth(100);
        tmpDescriptionFineLabel.setMinWidth(100);
        tmpDescriptionFineLabel.setMaxWidth(100);

         */
        if(aClusteringTypToStyleClusteringView.equals(this.VIEW_STYLE_FOR_ART_2A_CLUSTERING)) {
            this.vigilanceParameter1Button = new ToggleButton();
            this.vigilanceParameter1Button.setPrefWidth(40);
            this.vigilanceParameter1Button.setMinWidth(40);
            this.vigilanceParameter1Button.setMaxWidth(40);
            this.vigilanceParameter2Button = new ToggleButton();
            this.vigilanceParameter2Button.setPrefWidth(40);
            this.vigilanceParameter2Button.setMinWidth(40);
            this.vigilanceParameter2Button.setMaxWidth(40);
            this.vigilanceParameter3Button = new ToggleButton();
            this.vigilanceParameter3Button.setPrefWidth(40);
            this.vigilanceParameter3Button.setMinWidth(40);
            this.vigilanceParameter3Button.setMaxWidth(40);
            this.vigilanceParameter4Button = new ToggleButton();
            this.vigilanceParameter4Button.setPrefWidth(40);
            this.vigilanceParameter4Button.setMinWidth(40);
            this.vigilanceParameter4Button.setMaxWidth(40);
            this.vigilanceParameter5Button = new ToggleButton();
            this.vigilanceParameter5Button.setPrefWidth(40);
            this.vigilanceParameter5Button.setMinWidth(40);
            this.vigilanceParameter5Button.setMaxWidth(40);
            this.vigilanceParameter6Button = new ToggleButton();
            this.vigilanceParameter6Button.setPrefWidth(40);
            this.vigilanceParameter6Button.setMinWidth(40);
            this.vigilanceParameter6Button.setMaxWidth(40);
            this.vigilanceParameter7Button = new ToggleButton();
            this.vigilanceParameter7Button.setPrefWidth(40);
            this.vigilanceParameter7Button.setMinWidth(40);
            this.vigilanceParameter7Button.setMaxWidth(40);
            this.vigilanceParameter8Button = new ToggleButton();

            this.vigilanceParameter8Button.setPrefWidth(40);
            this.vigilanceParameter8Button.setMinWidth(40);
            this.vigilanceParameter8Button.setMaxWidth(40);
            this.vigilanceParameter9Button = new ToggleButton();
            this.vigilanceParameter9Button.setPrefWidth(40);
            this.vigilanceParameter9Button.setMinWidth(40);
            this.vigilanceParameter9Button.setMaxWidth(40);
            this.toggleGroup = new ToggleGroup();
            this.vigilanceParameter1Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter2Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter3Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter4Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter5Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter6Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter7Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter8Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter9Button.setToggleGroup(this.toggleGroup);
            tmpHBoxLeftSideControls.setAlignment(Pos.CENTER_LEFT);
            tmpHBoxLeftSideControls.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
            tmpHBoxLeftSideControls.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            tmpHBoxLeftSideControls.setPrefWidth(420);
            tmpHBoxLeftSideControls.setMinWidth(420);
            tmpHBoxLeftSideControls.setMaxWidth(420);
          //  HBox.setHgrow(tmpHBoxLeftSideControls, Priority.ALWAYS);
            tmpHBoxLeftSideControls.getChildren().addAll(
                    this.vigilanceParameter1Button, this.vigilanceParameter2Button, this.vigilanceParameter3Button,
                    this.vigilanceParameter4Button, this.vigilanceParameter5Button, this.vigilanceParameter6Button,
                    this.vigilanceParameter7Button, this.vigilanceParameter8Button, this.vigilanceParameter9Button
            );
         //   tmpHBoxLeftSideControls.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, null , null)));

            Color borderColor = Color.LIGHTGREY;
            double borderWidth = 1;
            double cornerRadius = 7; // Sie können die Rundung je nach Bedarf anpassen

            BorderStroke borderStroke = new BorderStroke(
                    borderColor, BorderStrokeStyle.SOLID, new CornerRadii(cornerRadius), new BorderWidths(borderWidth));
            tmpHBoxLeftSideControls.setBorder(new Border(borderStroke));
            DropShadow dropShadow = new DropShadow(10, Color.GRAY);
            tmpHBoxLeftSideControls.setEffect(dropShadow);
            Color backgroundColor = Color.LIGHTGREY;
            BackgroundFill backgroundFill = new BackgroundFill(backgroundColor, new CornerRadii(cornerRadius), null);
            Background background = new Background(backgroundFill);
            tmpHBoxLeftSideControls.setBackground(background);
           // bar.getButtons().add(tmpHBoxLeftSideControls);


            tmpDescriptionRoughLabel.setBorder(new Border(borderStroke));
            tmpDescriptionRoughLabel.setEffect(dropShadow);
            tmpDescriptionRoughLabel.setBackground(background);
            tmpDescriptionRoughLabel.setPrefWidth(80);
            tmpDescriptionRoughLabel.setMinWidth(80);
            tmpDescriptionRoughLabel.setMaxWidth(80);
            tmpDescriptionRoughLabel.setPrefHeight(25);
            tmpDescriptionRoughLabel.setMinHeight(25);
            tmpDescriptionRoughLabel.setMinHeight(25);
            tmpDescriptionRoughLabel.setStyle("-fx-font-style: italic;");
            tmpDescriptionRoughLabel.setAlignment(Pos.CENTER);

            tmpDescriptionFineLabel.setBorder(new Border(borderStroke));
            tmpDescriptionFineLabel.setEffect(dropShadow);
            tmpDescriptionFineLabel.setBackground(background);
            tmpDescriptionFineLabel.setPrefWidth(80);
            tmpDescriptionFineLabel.setMinWidth(80);
            tmpDescriptionFineLabel.setMaxWidth(80);
            tmpDescriptionFineLabel.setPrefHeight(25);
            tmpDescriptionFineLabel.setMinHeight(25);
            tmpDescriptionFineLabel.setMinHeight(25);
            tmpDescriptionFineLabel.setAlignment(Pos.CENTER);
            tmpDescriptionFineLabel.setStyle("-fx-font-style: italic;");

            tmpVigilanceParameterLabel.setBorder(new Border(borderStroke));
            tmpVigilanceParameterLabel.setEffect(dropShadow);
            tmpVigilanceParameterLabel.setBackground(background);
            tmpVigilanceParameterLabel.setPrefWidth(200);
            tmpVigilanceParameterLabel.setMinWidth(200);
            tmpVigilanceParameterLabel.setMaxWidth(200);
            tmpVigilanceParameterLabel.setPrefHeight(25);
            tmpVigilanceParameterLabel.setMinHeight(25);
            tmpVigilanceParameterLabel.setMinHeight(25);
            tmpVigilanceParameterLabel.setAlignment(Pos.CENTER);
            tmpVigilanceParameterLabel.setStyle("-fx-font-style: italic;");
        //    tmplabelContainer.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null , null)));
            tmplabelContainer.getChildren().addAll(tmpDescriptionRoughLabel, tmpVigilanceParameterLabel, tmpDescriptionFineLabel);
            vbox.getChildren().addAll(tmpHBoxLeftSideControls, tmplabelContainer);
            vbox.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, null , null)));
            System.out.println(vbox.getWidth() +"---vbox");
             vbox.setPadding(new Insets(10,25,0,25));
            vbox.setSpacing(10);
        }
        this.clusterRepresentativesButton = new Button("Representatives");
        this.barWidthsComboBox = new ComboBox<>();
        for (ClusteringViewController.BarWidthOption tmpBarWidthOptionConstant : ClusteringViewController.BarWidthOption.values()) {
            this.barWidthsComboBox.getItems().add(tmpBarWidthOptionConstant.getDisplayName());
        }
        tmpLeftSideGrid.setVgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpLeftSideGrid.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpLeftSideGrid.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        tmpLeftSideGrid.setGridLinesVisible(true);
        // grid positions
        if(aClusteringTypToStyleClusteringView.equals(this.VIEW_STYLE_FOR_ART_2A_CLUSTERING)) {
            /*
            tmpLeftSideGrid.add(tmpLabel1, 0,0);
            tmpLeftSideGrid.add(this.vigilanceParameter1Button, 1, 0);
            tmpLeftSideGrid.add(tmpLabel2,2,0);
            tmpLeftSideGrid.add(this.vigilanceParameter2Button, 3, 0);
            tmpLeftSideGrid.add(tmpLabel3,4,0);
            tmpLeftSideGrid.add(this.vigilanceParameter3Button, 5, 0);
            tmpLeftSideGrid.add(tmpLabel4,6,0);
            tmpLeftSideGrid.add(this.vigilanceParameter4Button, 7, 0);
            tmpLeftSideGrid.add(tmpLabel5,8,0);
            tmpLeftSideGrid.add(this.vigilanceParameter5Button, 9, 0);
            tmpLeftSideGrid.add(tmpLabel6, 0,1);
            tmpLeftSideGrid.add(this.vigilanceParameter6Button, 1, 1);
            tmpLeftSideGrid.add(tmpLabel7, 2,1);
            tmpLeftSideGrid.add(this.vigilanceParameter7Button, 3, 1);
            tmpLeftSideGrid.add(tmpLabel8, 4,1);
            tmpLeftSideGrid.add(this.vigilanceParameter8Button, 5, 1);
            tmpLeftSideGrid.add(tmpLabel9, 6,1);
            tmpLeftSideGrid.add(this.vigilanceParameter9Button, 7, 1);
            tmpLeftSideGrid.add(this.clusterRepresentativesButton, 18, 0);


            tmpLeftSideGrid.add(this.vigilanceParameter1Button,0,0);
            tmpLeftSideGrid.add(this.vigilanceParameter2Button, 1,0);
            tmpLeftSideGrid.add(this.vigilanceParameter3Button,2,0);
            tmpLeftSideGrid.add(this.vigilanceParameter4Button,3,0);
            tmpLeftSideGrid.add(this.vigilanceParameter5Button,4,0);
            tmpLeftSideGrid.add(this.vigilanceParameter6Button,5,0);
            tmpLeftSideGrid.add(this.vigilanceParameter7Button,6,0);
            tmpLeftSideGrid.add(this.vigilanceParameter8Button,7,0);
            tmpLeftSideGrid.add(this.vigilanceParameter9Button,8,0);
            tmpLeftSideGrid.add(tmpHbox1,0,1);

            tmpLeftSideGrid.add(tmpDescriptionRoughLabel, 0,1);
            tmpLeftSideGrid.add(tmpVigilanceParameterLabel,5,1);
            tmpLeftSideGrid.add(tmpDescriptionFineLabel, 7,1);




             */


          //  tmpHBoxLeftSideControls.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, null , null)));
           // tmpHBoxLeftSideControls.getChildren().add(tmpLeftSideGrid);
          //  tmpHBoxLeftSideControls.getChildren().add(bar);
         //   tmpMainHBoxControls.getChildren().add(tmpHBoxLeftSideControls);
          //  repButton.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
           // repButton.setPadding(new Insets(10,25,30,25));
            tmpMainHBoxControls.getChildren().add(vbox);
        }
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
        tmpRightSideGrid.add(this.barStylingCheckBox,2,0);
        tmpRightSideGrid.add(this.clusterRepresentativesButton,0,1);
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
    public ToggleButton getVigilanceParameter1Button(){
        return this.vigilanceParameter1Button;
    }
    public ToggleButton getVigilanceParameter2Button(){
        return this.vigilanceParameter2Button;
    }
    public ToggleButton getVigilanceParameter3Button(){
        return this.vigilanceParameter3Button;
    }
    public ToggleButton getVigilanceParameter4Button(){
        return this.vigilanceParameter4Button;
    }
    public ToggleButton getVigilanceParameter5Button(){
        return this.vigilanceParameter5Button;
    }
    public ToggleButton getVigilanceParameter6Button(){
        return this.vigilanceParameter6Button;
    }
    public ToggleButton getVigilanceParameter7Button(){
        return this.vigilanceParameter7Button;
    }
    public ToggleButton getVigilanceParameter8Button(){
        return this.vigilanceParameter8Button;
    }
    public ToggleButton getVigilanceParameter9Button(){
        return this.vigilanceParameter9Button;
    }
    public Button getCloseButton() {
        return this.closeButton;
    }
    public ArrayList<ToggleButton> getButtons() {
        ArrayList<ToggleButton> tmpButtonsList = new ArrayList<>(9);
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
