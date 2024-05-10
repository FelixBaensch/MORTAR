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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.effect.Blend;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
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
    private final SimpleBooleanProperty switchState;
    /**
     * transition of the switch from one side to the other.
     */
    private final TranslateTransition switchAnimation;
    /**
     * Color transition.
     */
    private final FillTransition fillAnimation;
    /**
     * Parallel transition.
     */
    private final ParallelTransition switchTransition;
    /**
     * Label.
     */
    private final Label switchLabel;
    //</editor-fold>
    /**
     * Constructor.
     */
    public ToggleSwitch() {
        //inspired by https://www.youtube.com/watch?v=maX5ymmQixM
        super();
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
        this.switchLabel = new Label("OFF");
        this.switchLabel.setTextFill(Color.BLACK);
        this.switchLabel.setLayoutX(-60);
        this.switchAnimation = new TranslateTransition(Duration.seconds(0.25));
        this.switchAnimation.setNode(this.switchButton);
        this.switchState = new SimpleBooleanProperty(false);
        this.fillAnimation = new FillTransition(Duration.seconds(0.25));
        this.switchTransition = new ParallelTransition(this.switchAnimation, this.fillAnimation);
        this.switchAnimation.setNode(this.switchButton);
        this.fillAnimation.setShape(this.switchBackground);
        getChildren().addAll(this.switchBackground, this.switchButton, this.switchLabel);
        //Listener
        this.switchState.addListener((observable, oldValue, newValue) -> {
            boolean tmpIsOn = newValue.booleanValue();
            this.switchAnimation.setToX(tmpIsOn ? (44 - 18) : 0);
            this.fillAnimation.setFromValue(tmpIsOn ? Color.LIGHTGRAY : Color.web("#6495ED"));
            this.fillAnimation.setToValue(tmpIsOn ? Color.web("#6495ED") : Color.LIGHTGRAY);
            if (newValue) {
                this.switchLabel.setText("ON");
                this.switchLabel.setTextFill(Color.WHITE);
                this.switchLabel.setLayoutX(-30);
            } else {
                this.switchLabel.setText("OFF");
                this.switchLabel.setTextFill(Color.BLACK);
                this.switchLabel.setLayoutX(-35);
            }
            this.switchTransition.play();
        });
        //Mouse listener.
        setOnMouseClicked(event -> this.switchState.set(!this.switchState.get()));
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
    public TranslateTransition getSwitchAnimation() { return this.switchAnimation; }
    /**
     * returns fillAnimation.
     *
     * @return FillTransition
     */
    public FillTransition getFillAnimation() { return this.fillAnimation; }
    /**
     * returns switchTransition.
     *
     * @return ParallelTransition
     */
    public ParallelTransition getSwitchTransition() { return this.switchTransition; }
    /**
     * returns Label.
     *
     * @return Label
     */
    public Label getSwitchLabel() { return this.switchLabel; }} // ?
    //</editor-fold>
