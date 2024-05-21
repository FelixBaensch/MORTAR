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
 * A toggle switch to en- and disable features in settings.
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
    private final SimpleBooleanProperty switchedOn;
    /**
     * transition of the Circle from one side to the other.
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
    /**
     * Constructor.
     */
    public ToggleSwitch() {
        /**
         * code inspired by "JavaFX UI: iOS Style Toggle Switch", uploaded by Almas Baimagambetov on YouTube
         * https://youtu.be/maX5ymmQixM?si=v2ULa57-pjCmoQlf, 05/17/2024, 10:33
         */
        super();
        this.switchedOn = new SimpleBooleanProperty(false);
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
        getChildren().addAll(this.switchBackground, this.switchButton);
        //Listener
        this.switchedOn.addListener((observable, oldValue, newValue) -> {
            boolean tmpIsOn = newValue.booleanValue();
            this.switchAnimation.setToX(tmpIsOn ? (44 - 18) : 0);
            this.fillAnimation.setFromValue(tmpIsOn ? Color.LIGHTGRAY : Color.web("#0099cc"));
            this.fillAnimation.setToValue(tmpIsOn ? Color.web("#0099cc") : Color.LIGHTGRAY);
            this.switchTransition.play();
        });
        //Mouse listener.
        setOnMouseClicked(event -> this.switchedOn.set(!this.switchedOn.get()));
    }
    //
    //<editor-fold desc="properties" defaultstate="collapsed">
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
     * returns switchedOn to change the boolean state of the switch.
     *
     * @return SimpleBooleanProperty
     */
    public SimpleBooleanProperty getSwitchedOn() {
        return this.switchedOn;
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
     * returns  switchedOnProperty.
     *
     * @return BooleanProperty
     */
    public BooleanProperty getSwitchedOnProperty() {
        return this.switchedOn;
    }
    /**
     * returns isSwitchedOn to change boolean state to true.
     *
     * @return switch value.
     */
    public boolean isSwitchedOn() {
        return this.switchedOn.get();
    }
    /**
     * sets switchedOn to update new value.
     *
     * @param switchedOn boolean
     */
    public void setSwitchedOn(boolean switchedOn) {
        this.switchedOn.set(switchedOn);
    }
    /**
     * returns valueProperty.
     *
     * @return BooleanProperty
     */
    public BooleanProperty valueProperty() {
        return this.switchedOn;
    }
    //</editor-fold>
}
