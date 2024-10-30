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
 * The transition of the position of the circle is animated and accompanied by the rectangle's color change
 * which demonstrates the current state. For example, blue and circle on the right means "on", grey and circle
 * on the left means "off". This class extends Control. However, methods for resizing the switch
 * are not implemented (yet).
 * The following code is inspired by "JavaFX UI: iOS Style Toggle Switch", uploaded by Almas Baimagambetov on YouTube.
 * See <a href="https://youtu.be/maX5ymmQixM?si=v2ULa57-pjCmoQlf">link here</a>, (last time viewed on 06/06/2024, 18:10)
 *
 * @author Zeynep Dagtekin
 * @version 1.0.0.0
 */
public class ToggleSwitch extends Control {
    //<editor-fold desc="Public static final constants" defaultstate="collapsed">
    /**
     * Default value for the width of the background.
     */
    public static final int DEFAULT_RECTANGLE_WIDTH_VALUE = 45;
    /**
     * Default value for the height of the background.
     */
    public static final int DEFAULT_RECTANGLE_HEIGHT_VALUE = 18;
    /**
     * Default value for the radius of the button.
     */
    public static final int DEFAULT_CIRCLE_RADIUS_VALUE = 10;
    /**
     * Default value for the layout which sets the position of the background
     * of the switch on the x-axis of the dialogue box.
     */
    public static final int DEFAULT_RECTANGLE_POSITION_X_VALUE = -50;
    /**
     * Default value for the layout which sets the position of the button on
     * the x-axis of the dialogue box.
     */
    public static final int DEFAULT_CIRCLE_POSITION_X_VALUE = -40;
    /**
     * Default value for the position of the button on the y-axis of the dialogue box.
     */
    public static final int DEFAULT_CIRCLE_POSITION_Y_VALUE = 9;
    /**
     * Default value for the radius of the shadow on the button.
     */
    public static final int DEFAULT_CIRCLE_SHADOW_RADIUS = 5;
    /**
     * Default value for the duration of the animated transition in seconds.
     */
    public static final double DEFAULT_DURATION_VALUE = 0.25;
    /**
     * Default color of the background when the toggle switch is turned off.
     */
    public static final Color DEFAULT_RECTANGLE_COLOR_OFF = Color.LIGHTGRAY;
    /**
     * Default color for the outline of the Background.
     */
    public static final Color DEFAULT_RECTANGLE_OUTLINE_COLOR = Color.DARKGRAY;
    /**
     * Default color of the button.
     */
    public static final Color DEFAULT_CIRCLE_COLOR = Color.WHITE;
    /**
     * Default color for the outline of the button.
     */
    public static final Color DEFAULT_CIRCLE_OUTLINE_COLOR = Color.DARKGRAY;
    /**
     * Default color for the background when turned on, encodes "Rich Electric Blue".
     */
    public static final Color DEFAULT_RECTANGLE_COLOR_ON = Color.web("#0099cc");
    /**
     * Default color of the shadow on the button.
     */
    public static final Color DEFAULT_CIRCLE_SHADOW_COLOR = Color.GRAY;
    /**
     * Initial boolean state of the switch.
     */
    public static final boolean DEFAULT_SWITCH_STATE = false;
    //</editor-fold>
    //
    //<editor-fold desc="Private final class constants" defaultstate="collapsed">
    /**
     * Circle that serves as button of the switch.
     */
    private final Circle switchButton;
    /**
     * Rectangle that serves as background of the switch.
     */
    private final Rectangle switchBackground;
    /**
     * Boolean property that represents the switch state, on vs. off.
     */
    private final SimpleBooleanProperty switchStateBooleanProperty;
    /**
     * Transition animation of the Circle from one side to the other.
     */
    private final TranslateTransition switchCircleTranslateTransition;
    /**
     * Color transition of the switch background.
     */
    private final FillTransition switchBackgroundColorFillTransition;
    /**
     * Combined transition of background color and circle position.
     */
    private final ParallelTransition switchTransition;
    //</editor-fold>
    //
    //<editor-fold desc="Constructors" defaultstate="collapsed">
    /**
     * Default constructor.
     * The toggle switch is built by initializing a layout which includes the sizing of the background (a rectangle)
     * and the sizing of the button (a circle), as well as their colors. The transition of the button is initialized by
     * adding an animation which also entails a color change. The transition works by subtracting the diameter of the
     * circle from the width of the rectangle. A listener and a mouse listener trigger the animation
     * when the switch is clicked. The rectangle height variable is used for its arc width and arc height,
     * which provides the rounded shape of the switch.
     */
    public ToggleSwitch() {
        this(ToggleSwitch.DEFAULT_CIRCLE_COLOR,
                ToggleSwitch.DEFAULT_CIRCLE_OUTLINE_COLOR,
                ToggleSwitch.DEFAULT_CIRCLE_SHADOW_COLOR,
                ToggleSwitch.DEFAULT_CIRCLE_SHADOW_RADIUS,
                ToggleSwitch.DEFAULT_RECTANGLE_COLOR_ON,
                ToggleSwitch.DEFAULT_RECTANGLE_COLOR_OFF,
                ToggleSwitch.DEFAULT_RECTANGLE_OUTLINE_COLOR,
                ToggleSwitch.DEFAULT_RECTANGLE_WIDTH_VALUE,
                ToggleSwitch.DEFAULT_RECTANGLE_HEIGHT_VALUE,
                ToggleSwitch.DEFAULT_RECTANGLE_POSITION_X_VALUE,
                ToggleSwitch.DEFAULT_CIRCLE_RADIUS_VALUE,
                ToggleSwitch.DEFAULT_CIRCLE_POSITION_X_VALUE,
                ToggleSwitch.DEFAULT_CIRCLE_POSITION_Y_VALUE,
                ToggleSwitch.DEFAULT_DURATION_VALUE);
    }
    //
    /**
     * Constructor for setting the animation duration to use. All other parameters will be set to default values.
     *
     * @param anAnimationDuration duration of the animated transition in seconds
     * @throws IllegalArgumentException if the animation duration is negative
     */
    public ToggleSwitch(double anAnimationDuration) throws IllegalArgumentException {
        this(ToggleSwitch.DEFAULT_CIRCLE_COLOR,
                ToggleSwitch.DEFAULT_CIRCLE_OUTLINE_COLOR,
                ToggleSwitch.DEFAULT_CIRCLE_SHADOW_COLOR,
                ToggleSwitch.DEFAULT_CIRCLE_SHADOW_RADIUS,
                ToggleSwitch.DEFAULT_RECTANGLE_COLOR_ON,
                ToggleSwitch.DEFAULT_RECTANGLE_COLOR_OFF,
                ToggleSwitch.DEFAULT_RECTANGLE_OUTLINE_COLOR,
                ToggleSwitch.DEFAULT_RECTANGLE_WIDTH_VALUE,
                ToggleSwitch.DEFAULT_RECTANGLE_HEIGHT_VALUE,
                ToggleSwitch.DEFAULT_RECTANGLE_POSITION_X_VALUE,
                ToggleSwitch.DEFAULT_CIRCLE_RADIUS_VALUE,
                ToggleSwitch.DEFAULT_CIRCLE_POSITION_X_VALUE,
                ToggleSwitch.DEFAULT_CIRCLE_POSITION_Y_VALUE,
                anAnimationDuration);
    }
    //
    /**
     * Constructor for setting the dimensions of rectangle and circle. All other parameters will be set to default values.
     * @param aRectangleWidth width of the switch background
     * @param aRectangleHeight height of the switch background
     * @param aRectanglePositionXValue value for the layout which sets the position of the background
     *                                 of the switch on the x-axis of the dialogue box.
     * @param aCircleRadius radius of the button
     * @param aCirclePositionXValue value for the layout which sets the position of the button on
     *                              the x-axis of the dialogue box.
     * @param aCirclePositionYValue value for the position of the button on the y-axis of the dialogue box
     * @throws IllegalArgumentException if at least one parameter is negative
     */
    public ToggleSwitch(int aRectangleWidth, int aRectangleHeight, int aRectanglePositionXValue, int aCircleRadius,
                        int aCirclePositionXValue, int aCirclePositionYValue) throws IllegalArgumentException {
        this(ToggleSwitch.DEFAULT_CIRCLE_COLOR,
                ToggleSwitch.DEFAULT_CIRCLE_OUTLINE_COLOR,
                ToggleSwitch.DEFAULT_CIRCLE_SHADOW_COLOR,
                ToggleSwitch.DEFAULT_CIRCLE_SHADOW_RADIUS,
                ToggleSwitch.DEFAULT_RECTANGLE_COLOR_ON,
                ToggleSwitch.DEFAULT_RECTANGLE_COLOR_OFF,
                ToggleSwitch.DEFAULT_RECTANGLE_OUTLINE_COLOR,
                aRectangleWidth,
                aRectangleHeight,
                aRectanglePositionXValue,
                aCircleRadius,
                aCirclePositionXValue,
                aCirclePositionYValue,
                ToggleSwitch.DEFAULT_DURATION_VALUE);
    }
    //
    /**
     * Constructor for setting colors. All other parameters will be set to default values.
     *
     * @param aCircleColor the color of the button
     * @param aCircleOutline the color of the outline of the button
     * @param aCircleShadowColor the color of the shadow around the button
     * @param aDropShadowRadius the radius of the shadow around the circle
     * @param aRectangleColorOn the color of the background when the switch is turned on
     * @param aRectangleColorOff the color of the background whe the switch is turned off
     * @param aRectangleOutline the color of the outline of the background
     * @throws IllegalArgumentException if at least one parameter is null or negative
     */
    public ToggleSwitch(Color aCircleColor, Color aCircleOutline, Color aCircleShadowColor, int aDropShadowRadius,
                        Color aRectangleColorOn, Color aRectangleColorOff, Color aRectangleOutline)
            throws IllegalArgumentException {
        this(aCircleColor,
                aCircleOutline,
                aCircleShadowColor,
                aDropShadowRadius,
                aRectangleColorOn,
                aRectangleColorOff,
                aRectangleOutline,
                ToggleSwitch.DEFAULT_RECTANGLE_WIDTH_VALUE,
                ToggleSwitch.DEFAULT_RECTANGLE_HEIGHT_VALUE,
                ToggleSwitch.DEFAULT_RECTANGLE_POSITION_X_VALUE,
                ToggleSwitch.DEFAULT_CIRCLE_RADIUS_VALUE,
                ToggleSwitch.DEFAULT_CIRCLE_POSITION_X_VALUE,
                ToggleSwitch.DEFAULT_CIRCLE_POSITION_Y_VALUE,
                ToggleSwitch.DEFAULT_DURATION_VALUE);
    }
    //
    /**
     * Constructor for setting only important colors. All other parameters will be set to default values.
     *
     * @param aCircleColor the color of the button
     * @param aRectangleColorOn the color of the background when the switch is turned on
     * @param aRectangleColorOff the color of the background whe the switch is turned off
     * @throws IllegalArgumentException if at least one parameter is null
     */
    public ToggleSwitch(Color aCircleColor, Color aRectangleColorOn, Color aRectangleColorOff)
            throws IllegalArgumentException {
        this(aCircleColor,
                ToggleSwitch.DEFAULT_CIRCLE_OUTLINE_COLOR,
                ToggleSwitch.DEFAULT_CIRCLE_SHADOW_COLOR,
                ToggleSwitch.DEFAULT_CIRCLE_SHADOW_RADIUS,
                aRectangleColorOn,
                aRectangleColorOff,
                ToggleSwitch.DEFAULT_RECTANGLE_OUTLINE_COLOR,
                ToggleSwitch.DEFAULT_RECTANGLE_WIDTH_VALUE,
                ToggleSwitch.DEFAULT_RECTANGLE_HEIGHT_VALUE,
                ToggleSwitch.DEFAULT_RECTANGLE_POSITION_X_VALUE,
                ToggleSwitch.DEFAULT_CIRCLE_RADIUS_VALUE,
                ToggleSwitch.DEFAULT_CIRCLE_POSITION_X_VALUE,
                ToggleSwitch.DEFAULT_CIRCLE_POSITION_Y_VALUE,
                ToggleSwitch.DEFAULT_DURATION_VALUE);
    }
    //
    /**
     * Constructor with all parameters to make the toggle switch configurable. This way if it is needed in different
     * classes, the switch can be customized by calling this constructor and passing preferred values to
     * the parameters. If you only want to customize some settings, use the public static final constants of this class
     * for the default values.
     *
     * @param aCircleColor the color of the button
     * @param aCircleOutline the color of the outline of the button
     * @param aCircleShadowColor the color of the shadow around the button
     * @param aDropShadowRadius the radius of the shadow around the circle
     * @param aRectangleColorOn the color of the background when the switch is turned on
     * @param aRectangleColorOff the color of the background whe the switch is turned off
     * @param aRectangleOutline the color of the outline of the background
     * @param aRectangleWidth width of the switch background
     * @param aRectangleHeight height of the switch background
     * @param aRectanglePositionXValue value for the layout which sets the position of the background
     *                                 of the switch on the x-axis of the dialogue box.
     * @param aCircleRadius radius of the button
     * @param aCirclePositionXValue value for the layout which sets the position of the button on
     *                              the x-axis of the dialogue box.
     * @param aCirclePositionYValue value for the position of the button on the y-axis of the dialogue box
     * @param anAnimationDuration duration of the animated transition in seconds
     * @throws IllegalArgumentException if at least one of the given arguments is null or negative
     */
    public ToggleSwitch(Color aCircleColor, Color aCircleOutline, Color aCircleShadowColor, int aDropShadowRadius,
                        Color aRectangleColorOn, Color aRectangleColorOff, Color aRectangleOutline,
                        int aRectangleWidth, int aRectangleHeight, int aRectanglePositionXValue, int aCircleRadius,
                        int aCirclePositionXValue, int aCirclePositionYValue, double anAnimationDuration) throws IllegalArgumentException {
        super();
        if (aCircleColor == null || aCircleOutline == null || aCircleShadowColor == null || aDropShadowRadius < 0
                || aRectangleColorOn == null || aRectangleColorOff == null || aRectangleOutline == null
                || aRectangleWidth < 0 || aRectangleHeight < 0 || aCircleRadius < 0 || anAnimationDuration < 0){
            throw new IllegalArgumentException("At least one of the given arguments is null or negative.");
        }
        this.switchStateBooleanProperty = new SimpleBooleanProperty(ToggleSwitch.DEFAULT_SWITCH_STATE);
        this.switchBackground = new Rectangle(aRectangleWidth, aRectangleHeight);
        this.switchBackground.setArcWidth(aRectangleHeight);
        this.switchBackground.setArcHeight(aRectangleHeight);
        this.switchBackground.setLayoutX(aRectanglePositionXValue);
        this.switchBackground.setFill(this.switchStateBooleanProperty.get() ? aRectangleColorOn : aRectangleColorOff);
        this.switchBackground.setStroke(aRectangleOutline);
        this.switchButton = new Circle(aCircleRadius);
        this.switchButton.setCenterX(aCirclePositionXValue);
        this.switchButton.setCenterY(aCirclePositionYValue);
        this.switchButton.setFill(aCircleColor);
        this.switchButton.setStroke(aCircleOutline);
        this.switchButton.setEffect(new DropShadow(aDropShadowRadius, aCircleShadowColor));
        this.switchCircleTranslateTransition = new TranslateTransition(
                Duration.seconds(anAnimationDuration));
        this.switchCircleTranslateTransition.setNode(this.switchButton);
        this.switchBackgroundColorFillTransition = new FillTransition(
                Duration.seconds(anAnimationDuration));
        this.switchTransition = new ParallelTransition(
                this.switchCircleTranslateTransition,
                this.switchBackgroundColorFillTransition);
        this.switchCircleTranslateTransition.setNode(this.switchButton);
        this.switchBackgroundColorFillTransition.setShape(this.switchBackground);
        this.getChildren().addAll(this.switchBackground, this.switchButton);
        //Listener
        // note: a mouse listener on this control sets the boolean switch state property
        // and this value change listener plays the switch transition when the property changes
        this.switchStateBooleanProperty.addListener((observable, oldValue, newValue) -> {
            if (oldValue.booleanValue() == newValue.booleanValue()) {
                return;
            }
            this.switchCircleTranslateTransition.setToX(newValue ?
                    (this.switchBackground.getWidth() - 2 * this.switchButton.getRadius()) : 0);
            this.switchBackgroundColorFillTransition.setFromValue(newValue ?
                    aRectangleColorOff : aRectangleColorOn);
            this.switchBackgroundColorFillTransition.setToValue(newValue ?
                    aRectangleColorOn : aRectangleColorOff);
            this.switchTransition.play();
        });
        //Mouse listener.
        this.setOnMouseClicked(event -> this.switchStateBooleanProperty.set(
                !this.switchStateBooleanProperty.get()));
    }
    //</editor-fold>
    //
    //<editor-fold desc="Properties get" defaultstate="collapsed">
    /**
     * Returns the circle that serves as switch button.
     *
     * @return Circle
     */
    public Circle getSwitchButton() {
        return this.switchButton;
    }
    /**
     * Returns the rectangle that serves as switch background.
     *
     * @return Rectangle
     */
    public Rectangle getSwitchBackground() {
        return this.switchBackground;
    }
    /**
     * Returns the translation transition of the circle button.
     *
     * @return TranslateTransition
     */
    public TranslateTransition getSwitchCircleTranslateTransition() {
        return this.switchCircleTranslateTransition;
    }
    /**
     * Returns the color change transition of the rectangle switch background.
     *
     * @return FillTransition
     */
    public FillTransition getSwitchBackgroundColorFillTransition() {
        return this.switchBackgroundColorFillTransition;
    }
    /**
     * Returns the combined transition of translation and filling transition.
     *
     * @return ParallelTransition
     */
    public ParallelTransition getSwitchTransition() {
        return this.switchTransition;
    }
    /**
     * Returns the current boolean state of the toggle switch.
     *
     * @return true if the switch is turned on, false otherwise
     */
    public boolean getSwitchState() {
        return this.switchStateBooleanProperty.get();
    }
    /**
     * Returns boolean property that wraps the switch state.
     *
     * @return BooleanProperty
     */
    public BooleanProperty getSwitchStateProperty() {
        return this.switchStateBooleanProperty;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Properties set">
    /**
     * Turns the switch on or off.
     *
     * @param switchStateBooleanProperty boolean
     */
    public void setSwitchState(boolean switchStateBooleanProperty) {
        this.switchStateBooleanProperty.set(switchStateBooleanProperty);
    }
    //</editor-fold>
}
