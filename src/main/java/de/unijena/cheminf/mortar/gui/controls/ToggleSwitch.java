package de.unijena.cheminf.mortar.gui.controls;

import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
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
public class ToggleSwitch extends Button {
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
    //</editor-fold>
    /**
     * Constructor.
     */
    public ToggleSwitch() {
        //inspired by https://www.youtube.com/watch?v=maX5ymmQixM
        this.switchButton = new Circle();
        this.switchButton.setCenterX(15);
        this.switchButton.setCenterY(15);
        this.switchButton.setFill(Color.LIGHTGRAY);
        this.switchBackground = new Rectangle();
        this.switchBackground.setArcWidth(15);
        this.switchBackground.setArcHeight(25);
        this.switchBackground.setFill(Color.LIGHTGRAY);
        getChildren().addAll(this.switchBackground,this.switchButton);
        this.switchAnimation = new TranslateTransition(Duration.seconds(0.25));
        this.switchAnimation.setNode(this.switchButton);
        this.switchState = new SimpleBooleanProperty(false);
        //Listener
        this.switchState.addListener((observable, oldValue, newValue) -> {
            boolean tmpIsOn = newValue.booleanValue();
            this.switchAnimation.setToX( newValue ? (130 - 15) : 0);
            this.switchAnimation.play();
        });
        //Mouse listener.
        setOnMouseClicked(event ->{
            this.switchState.set(!this.switchState.get());
        });
        }

        //
        //<editor-fold desc="properties" defaultstate="collapsed">
    //
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
}





