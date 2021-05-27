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


import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.*;

import com.itextpdf.text.Image;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.openscience.cdk.exception.CDKException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
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
    public File createCsvFile(Stage aParentStage, ObservableList<FragmentDataModel> aList, char aSeperator) {
        try {
            File tmpCsvFile = this.saveFile(aParentStage);
            PrintWriter tmpWriter = new PrintWriter(tmpCsvFile.getPath());
            StringBuilder tmpCsvHeader = new StringBuilder();
            tmpCsvHeader.append("SmilesString" + aSeperator + "Frequency" + aSeperator + "Percentage" + aSeperator + "MolecularFrequency"
                    + aSeperator + "MolecularPercentage\n");
            tmpWriter.write(tmpCsvHeader.toString());
            for (FragmentDataModel tmpFragmentDataModel : aList) {
                tmpWriter.printf("%s"+aSeperator+ "%d" +aSeperator+ "%.3f"+aSeperator+"%d"+aSeperator+ "%.3f\n", tmpFragmentDataModel.getUniqueSmiles(), tmpFragmentDataModel.getAbsoluteFrequency(),
                        tmpFragmentDataModel.getAbsolutePercentage(), tmpFragmentDataModel.getMoleculeFrequency(), tmpFragmentDataModel.getMoleculePercentage());
            }
            tmpWriter.close();
            return tmpCsvFile;
        } catch (FileNotFoundException anException) {
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }

    /**
     * Exports the results of the fragmentation as a Pdf file
     * @param aParentstage  Stage to show the FileChooser
     * @param aFragmentDataModelList a list to iterate through FragmentDataModel
     * @return  Pdf file with a table which contains the results of the fragmentation
     * @throws IOException
     * @throws DocumentException
     * @throws CDKException
     */
    public Document createPdfFile(Stage aParentstage, ObservableList<FragmentDataModel> aFragmentDataModelList, ObservableList<MoleculeDataModel> aMoleculeDataModelList) {
        try {
            File tmpFile = this.saveFile(aParentstage);
            Document tmpDocument = new Document();
            tmpDocument.setPageSize(tmpDocument.getPageSize().rotate());
            PdfWriter.getInstance(tmpDocument, new FileOutputStream(tmpFile.getPath()));
            tmpDocument.open();

            int tmpIteratorMolecule = 0;
            int tmpIteratorFragment = 0;
            Paragraph tmpSpace = new Paragraph(" ");
            float tmpCellLength[] = {80f, 130f, 65f, 65f, 60f, 60f};
            PdfPTable table = new PdfPTable(tmpCellLength);
            PdfPCell tmpCell1 = new PdfPCell(new Paragraph("Smiles String ", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            PdfPCell tmpCell2 = new PdfPCell(new Paragraph("Frequency", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            PdfPCell tmpCell3 = new PdfPCell(new Paragraph("Percentage", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            PdfPCell tmpCell4 = new PdfPCell(new Paragraph("Molecule-frequency", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            PdfPCell tmpCell5 = new PdfPCell(new Paragraph(" Molecule-percentage", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            PdfPCell tmpCell6 = new PdfPCell(new Paragraph("Fragment", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            Chunk tmpHeader = new Chunk("FRAGMENTATION",
                    FontFactory.getFont(FontFactory.TIMES_ROMAN, 18, Font.UNDERLINE));
            table.addCell(tmpCell6);
            table.addCell(tmpCell1);
            table.addCell(tmpCell2);
            table.addCell(tmpCell3);
            table.addCell(tmpCell4);
            table.addCell(tmpCell5);
            for (FragmentDataModel tmpFragmentDataModel : aFragmentDataModelList) {
                int tmpAbsoluteFrequency = tmpFragmentDataModel.getAbsoluteFrequency();
                String tmpStringAbsoluteFrequency = String.format("%d", tmpAbsoluteFrequency);
                double tmpAbsolutePercentage = tmpFragmentDataModel.getAbsolutePercentage();
                int tmpMoleculeFrequency = tmpFragmentDataModel.getMoleculeFrequency();
                String tmpStringMoleculeFrequency = String.format("%d", tmpMoleculeFrequency);
                String tmpStringAbsoultePercentage = String.format("%.3f", tmpAbsolutePercentage);
                double tmpMoleculePercentage = tmpFragmentDataModel.getMoleculePercentage();
                String tmpStringMoleculePercentage = String.format("%.3f", tmpMoleculePercentage);

                ImageView tmpImageViewFragments = tmpFragmentDataModel.getStructure();
                javafx.scene.image.Image tmpImageOfFragments = tmpImageViewFragments.getImage();
                BufferedImage tmpBufferedImage = SwingFXUtils.fromFXImage(tmpImageOfFragments, null);
                File tmpImageFile = new File("Image.png");
                ImageIO.write(tmpBufferedImage, "PNG", tmpImageFile);
                Image tmpFragment = Image.getInstance("Image.png");
                tmpFragment.scaleToFit(1, 1);
                table.addCell(tmpFragment);
                table.addCell(tmpFragmentDataModel.getUniqueSmiles());
                table.addCell(tmpStringAbsoluteFrequency);
                table.addCell(tmpStringAbsoultePercentage);
                table.addCell(tmpStringMoleculeFrequency);
                table.addCell(tmpStringMoleculePercentage);
                tmpIteratorFragment++;
                tmpImageFile.delete();
            }
            for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
                tmpIteratorMolecule++;
            }
            Paragraph tmpIntro = new Paragraph("These are the results of the Ertl-Algorithm. " +tmpIteratorMolecule+ " molecules were read in " +
                    "and the fragmentation algorithm yields " +tmpIteratorFragment+ " molecule fragments",
                    FontFactory.getFont(FontFactory.TIMES_ROMAN, 12));
            tmpDocument.add(tmpHeader);
            tmpDocument.add(tmpIntro);
            tmpDocument.add(tmpSpace);
            tmpDocument.add(table);
            tmpDocument.close();
            return tmpDocument;
        } catch (IOException | DocumentException anException) {
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
        FileChooser.ExtensionFilter exFilter1 = new FileChooser.ExtensionFilter("PDF Files", "*.pdf");
        FileChooser.ExtensionFilter exFilter2 = new FileChooser.ExtensionFilter("Csv Files", "*.csv");
        tmpFileChooser.getExtensionFilters().addAll(exFilter1, exFilter2);
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
