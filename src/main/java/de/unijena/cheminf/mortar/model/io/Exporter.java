/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
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
package de.unijena.cheminf.mortar.model.io;

import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Exporter {

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(Exporter.class.getName());

    /**
     * Constructor
     */
    public Exporter() {
    }

    /**
     * Exports the results of the fragmentation as a Csv file
     * @param aParentStage  Stage to show the FileChooser
     * @param aList a list to iterate through FragmentDataModel
     * @param aSeperator is the seperator for the csv file
     * @return  Csv file which contains the results of the fragmentation
     */
    public File csvFile(Stage aParentStage, ObservableList<FragmentDataModel> aList, char aSeperator) {
        try {
            File tmpCsvFile = this.saveFile(aParentStage);
            PrintWriter tmpWriter = new PrintWriter(tmpCsvFile.getPath());
            StringBuffer tmpCsvHeader = new StringBuffer();
            tmpCsvHeader.append("SmilesString" + aSeperator + "Frequency" + aSeperator + "Percentage" + aSeperator + "MolecularFrequency"
                    + aSeperator + "MolecularPercentage\n");
            tmpWriter.write(tmpCsvHeader.toString());
            for (FragmentDataModel tmpKey : aList) {
                tmpWriter.printf("%s"+aSeperator+ "%d" +aSeperator+ "%.3f"+aSeperator+"%d"+aSeperator+ "%.3f\n", tmpKey.getUniqueSmiles(), tmpKey.getAbsoluteFrequency(),
                        tmpKey.getAbsolutePercentage(), tmpKey.getMoleculeFrequency(), tmpKey.getMoleculePercentage());
            }
            tmpWriter.close();
            return tmpCsvFile;
        } catch (FileNotFoundException anException) {
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }

    /**
     * Opens a FileChooser to be able to save a file
     * @param aParentStage Stage where FileChooser should be shown
     * @return File
     */
    private File saveFile(Stage aParentStage){
        Objects.requireNonNull(aParentStage, "aParentStage (instance of Stage) is null");
        FileChooser tmpFileChooser = new FileChooser();
        tmpFileChooser.setTitle((Message.get("Exporter.fileChooser.title")));
        tmpFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Csv File", "*.csv"));
        File tmpFile = null;
        try{ 
            tmpFile = tmpFileChooser.showSaveDialog(aParentStage);
        } catch(Exception anException){
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
        } finally {
            return tmpFile;
        }
    }

}
