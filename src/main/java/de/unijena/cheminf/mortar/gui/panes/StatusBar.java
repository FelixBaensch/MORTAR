/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.cheminf.mortar.gui.panes;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.message.Message;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;

import static de.unijena.cheminf.mortar.gui.util.GuiDefinitions.GUI_STATUSBAR_HEIGHT_VALUE;

/**
 * StatusBar to show status of the application and progress of a running task
 *
 * @author Felix Baensch, Jonas Schaub
 */
public class StatusBar extends FlowPane {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private Label statusLabel;
    private ProgressBar progressBar;
    private Task task;
    //</editor-fold>
    //
    /**
     * Constructor
     */
    public StatusBar(){
        super();
        this.setStyle("-fx-background-color: DarkGrey");
        this.statusLabel = new Label();
        this.progressBar = new ProgressBar();
        this.progressBar.visibleProperty().setValue(false);
        this.setMinHeight(GUI_STATUSBAR_HEIGHT_VALUE);
        this.setPrefHeight(GUI_STATUSBAR_HEIGHT_VALUE);
        this.setMaxHeight(GUI_STATUSBAR_HEIGHT_VALUE);
//        this.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        this.setPadding(new Insets( 3));
        this.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        this.getChildren().addAll(statusLabel, progressBar);
    }
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
//    public void startProgressBar(Thread aThread){
//        //bind progress property
//        this.progressBar.visibleProperty().setValue(true);
//        this.progressBar.progressProperty().unbind();
//        this.progressBar.progressProperty().bind(aThread.get));
//        //bind message property
//        this.statusLabel.textProperty().unbind();
//        this.statusLabel.textProperty().bind(this.task.messageProperty());
//    }




    /**
     * TODO: remove this from here and add to fragementer service class
     * @param aTask
     */
    public void setTaskAndStart(Task aTask){
        this.task = aTask;
        //bind progress property
        this.progressBar.visibleProperty().setValue(true);
        this.progressBar.progressProperty().unbind();
        this.progressBar.progressProperty().bind(this.task.progressProperty());
        //bind message property
        this.statusLabel.textProperty().unbind();
        this.statusLabel.textProperty().bind(this.task.messageProperty());
        //when task completed
        this.task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler() {
            @Override
            public void handle(Event anEvent) {
                progressBar.progressProperty().unbind();
                progressBar.visibleProperty().setValue(false);
                statusLabel.textProperty().unbind();
                statusLabel.setText(Message.get("Status.Ready"));
            }
        });
        //Start task
        new Thread(this.task).start();
    }
    //
    /**
     * Cancels task
     */
    public void CancelTask(){
        this.task.cancel(true);
        progressBar.progressProperty().unbind();
        progressBar.visibleProperty().setValue(false);
        statusLabel.textProperty().unbind();
        statusLabel.setText(Message.get("Status.Canceled"));
        try{
            Thread.sleep(1000);
        }
        catch (InterruptedException anException){
            statusLabel.setText(Message.get("Status.Ready"));
        }
        statusLabel.setText(Message.get("Status.Ready"));
    }
    //</editor-fold>
    //
    //<editor-fold desc="properties" defaultstate="collapsed">
    /**
     * Returns the task
     * @return task
     */
    public Task getTask(){
        return this.task;
    }
    //
    /**
     * Returns statusLabel
     * @return
     */
    public Label getStatusLabel() {
        return statusLabel;
    }
    //
    /**
     * Returns the progressBar
     * @return
     */
    public ProgressBar getProgressBar() {
        return progressBar;
    }
    //</editor-fold>
}

