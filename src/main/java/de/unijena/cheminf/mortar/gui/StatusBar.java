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

package de.unijena.cheminf.mortar.gui;

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

/**
 * StatusBar to show status of the application and progress of a running task
 *
 * @author Felix Baensch, Jonas Schaub
 */
public class StatusBar extends FlowPane {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private Label fileNameLabel;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Task task;
    //</editor-fold>

    /**
     * Constructor
     */
    public StatusBar(){
        super();
        this.setStyle("-fx-background-color: DarkGrey");
        this.fileNameLabel = new Label("Hier koennte ihre Werbung stehen.");
        this.statusLabel = new Label();
        this.progressBar = new ProgressBar();
        this.progressBar.visibleProperty().setValue(false);
        this.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        this.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        this.getChildren().addAll(fileNameLabel, statusLabel, progressBar);
    }

    /**
     *
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

    /**
     * Set file name to fileNameLabel
     * @param aFileName String
     */
    public void setFileNameLabelText(String aFileName) {
        if(aFileName.isEmpty() || aFileName == null)
            throw new IllegalArgumentException();
        this.fileNameLabel.setText(aFileName);
    }

    /**
     * Returns the task
     * @return task
     */
    public Task getTask(){
        return this.task;
    }

    /**
     * Returns the fileNameLabel
     * @return
     */
    public Label getFileNameLabel() {
        return fileNameLabel;
    }

    /**
     * Returns statusLabel
     * @return
     */
    public Label getStatusLabel() {
        return statusLabel;
    }

    /**
     * Returns the progressBar
     * @return
     */
    public ProgressBar getProgressBar() {
        return progressBar;
    }
}

