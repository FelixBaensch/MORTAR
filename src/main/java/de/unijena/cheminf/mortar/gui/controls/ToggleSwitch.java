package de.unijena.cheminf.mortar.gui.controls;

import javafx.animation.FillTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * A toggle switch to en- and disable features in settings.
 *
 * @author Zeynep Dagtekin
 * @version
 */
public class ToggleSwitch extends Parent {
    //<editor-fold desc="private final class constants" defaultstate="collapsed">
    /**
     * Button.
     */
    private final Circle switchButton;

    /**
     * Background of the switch.
     */
    private final Rectangle switchBackground;
    /**
     * Boolean property to keep track of the state.
     */
    private final SimpleBooleanProperty switchState;
    /**
     * transition of the switch from one side to the other.
     */
    private final TranslateTransition switchAnimation;
    private final FillTransition fillAnimation;
    private final ParallelTransition switchTransition;
    //</editor-fold>
    /**
     * Constructor.
     */
    public ToggleSwitch() {
        //inspired by https://www.youtube.com/watch?v=maX5ymmQixM
        super();
        this.switchBackground = new Rectangle(120, 18);
        this.switchBackground.setArcWidth(20);
        this.switchBackground.setArcHeight(20);
        this.switchBackground.setFill(Color.WHITE);
        this.switchBackground.setStroke(Color.GRAY);
        this.switchButton = new Circle(10);
        this.switchButton.setCenterX(10);
        this.switchButton.setCenterY(9);
        this.switchButton.setFill(Color.LIGHTGRAY);
        this.switchButton.setStroke(Color.GRAY);
        this.switchAnimation = new TranslateTransition(Duration.seconds(0.25));
        this.switchAnimation.setNode(this.switchButton);
        this.switchState = new SimpleBooleanProperty(false);
        this.fillAnimation = new FillTransition(Duration.seconds(0.25));
        this.switchTransition = new ParallelTransition(this.switchAnimation, this.fillAnimation);;
        this.switchAnimation.setNode(this.switchButton);
        this.fillAnimation.setShape(this.switchBackground);
        getChildren().addAll(this.switchBackground, this.switchButton );
        //Listener
        this.switchState.addListener((observable, oldValue, newValue) -> {
            boolean tmpIsOn = newValue.booleanValue();
            this.switchAnimation.setToX(tmpIsOn ? (119 - 19) : 0);
            this.fillAnimation.setFromValue(tmpIsOn ? Color.WHITE : Color.LIGHTBLUE);
            this.fillAnimation.setToValue(tmpIsOn ? Color.LIGHTBLUE : Color.WHITE);
            this.switchTransition.play();
        });
        //Mouse listener.
        setOnMouseClicked(event -> {
            this.switchState.set(!this.switchState.get());
        });
    }
    //
    //<editor-fold desc="properties" defaultstate="collapsed">
    /**
     * returns switchButton
     *
     * @return Circle
     */
    public Circle getSwitchButton() { return this.switchButton; }
    /**
     * returns switchBackground
     *
     * @return Rectangle
     */
    public Rectangle getSwitchBackground() { return this.switchBackground; }
    /**
     * returns switchState.
     *
     * @return SimpleBooleanProperty
     */
    public SimpleBooleanProperty getSwitchState() { return this.switchState; }
    /**
     * returns switchAnimation.
     *
     * @return TranslateTransition
     */
    public TranslateTransition getSwitchAnimation() {return switchAnimation;}
    //</editor-fold>
}





