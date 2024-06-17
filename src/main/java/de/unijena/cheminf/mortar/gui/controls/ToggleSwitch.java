/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2024  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.gui.controls;

import javafx.animation.FillTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Control;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * This class implements a toggle switch to en- and disable features in settings. The toggle switch is built by putting
 * a circle on top of a rectangle. The corners of the rectangle were adjusted to fit the round shape of the circle.
 * The transition of the position of the circle is animated and parallel to the rectangle's color change
 * which demonstrates the current state. For example, blue and circle on the right means "on"/grey and circle
 * transitions to left means "off". This class extends Control, however methods for resizing the switch
 * are not implemented yet.
 * The following code is inspired by "JavaFX UI: iOS Style Toggle Switch", uploaded by Almas Baimagambetov on YouTube.
 * See https://youtu.be/maX5ymmQixM?si=v2ULa57-pjCmoQlf, (last time viewed on 06/06/2024, 18:10)
 *
 * @author Zeynep Dagtekin
 * @version 1.0.0.0
 */
public class ToggleSwitch extends Control {
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
    private final SimpleBooleanProperty switchStateBooleanProperty;
    /**
     * Transition of the Circle from one side to the other.
     */
    private final TranslateTransition switchAnimation;
    /**
     * Color transition.
     */
    private final FillTransition fillAnimation;
    /**
     * Parallel transition of color and Circle.
     */
    private final ParallelTransition switchTransition;
    //</editor-fold>
    //<editor-fold desc="default values" defaultstate="collapsed">
    /**
     * Default value for the height of the Background.
     */
    private static final int RECTANGLE_HEIGHT_VALUE = 45;
    /**
     * Default value for the width of the Background.
     */
    private static final int RECTANGLE_WIDTH_VALUE = 18;
    /**
     * Default value for the radius of the Button.
     */
    private static final int CIRCLE_RADIUS_VALUE = 10;
    /**
     * Default value for the Layout which sets the position of the Background of the switch on the x-axis.
     */
    private static final int RECTANGLE_POSITION_VALUE = -50;
    /**
     * Default value for the Layout which sets the position of the button on the x-axis.
     */
    private static final int CIRCLE_POSITION_X_VALUE = -40;
    /**
     * Default value for the position of the Button on the y-axis.
     */
    private static final int CIRCLE_POSITION_Y_VALUE = 9;
    /**
     * Default value for the radius of the shadow on the button.
     */
    private static final int CIRCLE_SHADOW_RADIUS = 5;
    /**
     * Default value for the duration of the animated transition in seconds.
     */
    private static final double DURATION_VALUE = 0.25;
    /**
     * Default color of the background when the toggle switch is turned off.
     */
    private static final Color RECTANGLE_COLOR_OFF = Color.LIGHTGRAY;
    /**
     * Default color for the outline of the Background.
     */
    private static final Color RECTANGLE_OUTLINE_COLOR = Color.DARKGRAY;
    /**
     * Default color of the button.
     */
    private static final Color CIRCLE_COLOR = Color.WHITE;
    /**
     * Default color for the outline of the button.
     */
    private static final Color CIRCLE_OUTLINE_COLOR = Color.DARKGRAY;
    /**
     * Default color for the background when turned on.
     */
    private static final Color RECTANGLE_COLOR_ON = Color.web("#0099cc");
    /**
     * Default color of the shadow on the button.
     */
    private static final Color CIRCLE_SHADOW_COLOR = Color.GRAY;
    //</editor-fold>
    /**
     * Constructor.
     * The toggle switch is built by initializing a layout which includes the sizing of the background (a rectangle)
     * and the sizing of the button (a circle), as well as their colors. The transition of the button is initialized by
     * adding an animation which also entails a color change. A listener and a mouse listener trigger the animation
     * when the button (circle) is clicked on.
     */
    public ToggleSwitch() {
        super();
        this.switchStateBooleanProperty = new SimpleBooleanProperty(false);
        this.switchBackground = new Rectangle(RECTANGLE_HEIGHT_VALUE, RECTANGLE_WIDTH_VALUE);
        this.switchBackground.setArcWidth(RECTANGLE_WIDTH_VALUE);
        this.switchBackground.setArcHeight(RECTANGLE_WIDTH_VALUE);
        this.switchBackground.setLayoutX(RECTANGLE_POSITION_VALUE);
        this.switchBackground.setFill(RECTANGLE_COLOR_OFF);
        this.switchBackground.setStroke(RECTANGLE_OUTLINE_COLOR);
        this.switchButton = new Circle(CIRCLE_RADIUS_VALUE);
        this.switchButton.setCenterX(CIRCLE_POSITION_X_VALUE);
        this.switchButton.setCenterY(CIRCLE_POSITION_Y_VALUE);
        this.switchButton.setFill(CIRCLE_COLOR);
        this.switchButton.setStroke(CIRCLE_OUTLINE_COLOR);
        this.switchButton.setEffect(new DropShadow(CIRCLE_SHADOW_RADIUS, CIRCLE_SHADOW_COLOR));
        this.switchAnimation = new TranslateTransition(Duration.seconds(DURATION_VALUE));
        this.switchAnimation.setNode(this.switchButton);
        this.fillAnimation = new FillTransition(Duration.seconds(DURATION_VALUE));
        this.switchTransition = new ParallelTransition(this.switchAnimation, this.fillAnimation);
        this.switchAnimation.setNode(this.switchButton);
        this.fillAnimation.setShape(this.switchBackground);
        this.getChildren().addAll(this.switchBackground, this.switchButton);
        //Listener
        this.switchStateBooleanProperty.addListener((observable, oldValue, newValue) -> {
            boolean tmpIsOn = newValue.booleanValue();
            this.switchAnimation.setToX(tmpIsOn ? (RECTANGLE_HEIGHT_VALUE - (2 * CIRCLE_RADIUS_VALUE)) : 0);
            this.fillAnimation.setFromValue(tmpIsOn ? RECTANGLE_COLOR_OFF : RECTANGLE_COLOR_ON);
            this.fillAnimation.setToValue(tmpIsOn ? RECTANGLE_COLOR_ON : RECTANGLE_COLOR_OFF);
            this.switchTransition.play();
        });
        //Mouse listener.
        this.setOnMouseClicked(event -> this.switchStateBooleanProperty.set(!this.switchStateBooleanProperty.get()));
    }
    public ToggleSwitch(boolean anInitialStateOfBooleanProperty){
        this.switchStateBooleanProperty = new SimpleBooleanProperty(anInitialStateOfBooleanProperty);
        this.switchBackground = new Rectangle(45, 18);
        this.switchBackground.setArcWidth(18);
        this.switchBackground.setArcHeight(18);
        this.switchBackground.setLayoutX(-50);
        this.switchBackground.setFill(Color.LIGHTGRAY);
        this.switchBackground.setStroke(Color.DARKGRAY);
        this.switchButton = new Circle(10);
        this.switchButton.setCenterX(-40);
        this.switchButton.setCenterY(9);
        this.switchButton.setFill(Color.WHITE);
        this.switchButton.setStroke(Color.DARKGRAY);
        this.switchButton.setEffect(new DropShadow(5, Color.GRAY));
        this.switchAnimation = new TranslateTransition(Duration.seconds(0.25));
        this.switchAnimation.setNode(this.switchButton);
        this.fillAnimation = new FillTransition(Duration.seconds(0.25));
        this.switchTransition = new ParallelTransition(this.switchAnimation, this.fillAnimation);
        this.switchAnimation.setNode(this.switchButton);
        this.fillAnimation.setShape(this.switchBackground);
        this.getChildren().addAll(this.switchBackground, this.switchButton);
        //Listener
        this.switchStateBooleanProperty.addListener((observable, oldValue, newValue) -> {
            boolean tmpIsOn = newValue.booleanValue();
            this.switchAnimation.setToX(tmpIsOn ? (45 - 20) : 0); //?
            this.fillAnimation.setFromValue(tmpIsOn ? Color.LIGHTGRAY : Color.web("#0099cc"));
            this.fillAnimation.setToValue(tmpIsOn ? Color.web("#0099cc") : Color.LIGHTGRAY);
            this.switchTransition.play();
        });
        //Mouse listener.
        this.setOnMouseClicked(event -> this.switchStateBooleanProperty.set(!this.switchStateBooleanProperty.get()));
    }
    //
    //<editor-fold desc="properties" defaultstate="collapsed">
    //
    //public setRectangleWidth(int aRectangleWidth) {
    //this.switchedBackground.setWidth(aRectangleWidth);
    //this.switchedButton.setCenterX(this.switchedButton.setConfigurableCenterX(50))
    //private final const RECTANGLE_HEIGHT = 45
    /**
     * returns switchButton.
     *
     * @return Circle
     */
    public Circle getSwitchButton() {
        return this.switchButton;
    }
    /**
     * returns switchBackground.
     *
     * @return Rectangle
     */
    public Rectangle getSwitchBackground() {
        return this.switchBackground;
    }
    /**
     * returns switchAnimation which shows the visual transition of the Circle switchButton.
     *
     * @return TranslateTransition
     */
    public TranslateTransition getSwitchAnimation() {
        return this.switchAnimation;
    }
    /**
     * returns fillAnimation to show the color change when clicked on.
     *
     * @return FillTransition
     */
    public FillTransition getFillAnimation() {
        return this.fillAnimation;
    }
    /**
     * returns switchTransition to maintain a parallel animation for fillAnimation and switchAnimation.
     *
     * @return ParallelTransition
     */
    public ParallelTransition getSwitchTransition() {
        return this.switchTransition;
    }
    /**
     * returns isSwitchedOn, the current boolean state of the Toggleswitch,
     * if it is turned on it returns true, otherwise false.
     *
     * @return switch value
     */
    public boolean getSwitchStateBooleanProperty() {
        return this.switchStateBooleanProperty.get();
    }
    /**
     * sets switchStateBooleanProperty to update new value.
     *
     * @param switchStateBooleanProperty boolean
     */
    public void setSwitchStateBooleanProperty(boolean switchStateBooleanProperty) {
        this.switchStateBooleanProperty.set(switchStateBooleanProperty);
    }
    /**
     * returns valueProperty.
     *
     * @return BooleanProperty
     */
    public BooleanProperty valueProperty() {
        return this.switchStateBooleanProperty;
    }
    //</editor-fold>
}
