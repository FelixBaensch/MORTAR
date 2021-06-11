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

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exporter
 *
 */
public class Exporter {

    //<editor-fold defaultstate="collapsed" desc="Public static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(Exporter.class.getName());
    //</editor-fold>
    /**
     * Constructor
     */
    public Exporter() {
    }

    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     * Exports the fragmentation results that appear on the fragmentationtab as a CSV file
     * @param aParentStage  Stage to show the FileChooser
     * @param aList a list to iterate through FragmentDataModel
     * @param aSeperator is the seperator for the csv file
     * @return  Csv file which contains the results of the fragmentation
     */
    public File createFragmentationCsvFile(Stage aParentStage, ObservableList<FragmentDataModel> aList, char aSeperator) {
        try {
            File tmpFragmentationCsvFile = this.saveFile(aParentStage);
            PrintWriter tmpWriter = new PrintWriter(tmpFragmentationCsvFile.getPath());
            StringBuilder tmpFragmentationCsvHeader = new StringBuilder();
            tmpFragmentationCsvHeader.append("SmilesString" + aSeperator + "Frequency" + aSeperator + "Percentage" + aSeperator + "MolecularFrequency"
                    + aSeperator + "MolecularPercentage\n");
            tmpWriter.write(tmpFragmentationCsvHeader.toString());
            for (FragmentDataModel tmpFragmentDataModel : aList) {
                tmpWriter.printf("%s"+aSeperator+ "%d" +aSeperator+ "%.3f"+aSeperator+"%d"+aSeperator+ "%.3f\n", tmpFragmentDataModel.getUniqueSmiles(), tmpFragmentDataModel.getAbsoluteFrequency(),
                        tmpFragmentDataModel.getAbsolutePercentage(), tmpFragmentDataModel.getMoleculeFrequency(), tmpFragmentDataModel.getMoleculePercentage());
            }
            tmpWriter.close();
            return tmpFragmentationCsvFile;
        } catch (FileNotFoundException anException) {
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }

    /**
     * Exports the fragmentation results that appear on the fragmentationtab as a PDF file
     * @param aParentstage Stage to show the FileChooser
     * @param aFragmentDataModelList  a list to iterate through FragmentDataModel
     * @param aMoleculeDataModelList a list to iterate through MoleculeDataModel
     * @return PDF file which contains the results of the fragmentation
     */
    public Document createFragmentationTabPdfFile(Stage aParentstage, ObservableList<FragmentDataModel> aFragmentDataModelList, ObservableList<MoleculeDataModel> aMoleculeDataModelList) {
        try {
            File tmpFragmentationPdfFile = this.saveFile(aParentstage);
            Document tmpDocument = new Document(PageSize.A4);
            tmpDocument.setPageSize(tmpDocument.getPageSize().rotate());
            PdfWriter.getInstance(tmpDocument, new FileOutputStream(tmpFragmentationPdfFile.getPath()));
            tmpDocument.open();

            float tmpCellLength[] = {80f, 120f, 50f, 50f, 55f, 55f}; // relative sizes
            PdfPTable tmpFragmentationTable = new PdfPTable(tmpCellLength);
            PdfPCell tmpSmilesStringCell = new PdfPCell(new Paragraph("Smiles String ", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            PdfPCell tmpFrequencyCell = new PdfPCell(new Paragraph("Frequency", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            PdfPCell tmpPercentageCell = new PdfPCell(new Paragraph("Percentage", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            PdfPCell tmpMolFrequencyCell = new PdfPCell(new Paragraph("Molecule-frequency", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            PdfPCell tmpMolPercentageCell = new PdfPCell(new Paragraph(" Molecule-percentage", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            PdfPCell tmpFragmentCell = new PdfPCell(new Paragraph("Fragment", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            Chunk tmpHeader = new Chunk("FRAGMENTATION",
                    FontFactory.getFont(FontFactory.TIMES_ROMAN, 18, Font.UNDERLINE));
            Paragraph tmpSpace = new Paragraph(" ");
            tmpFragmentationTable.addCell(tmpFragmentCell);
            tmpFragmentationTable.addCell(tmpSmilesStringCell);
            tmpFragmentationTable.addCell(tmpFrequencyCell);
            tmpFragmentationTable.addCell(tmpPercentageCell);
            tmpFragmentationTable.addCell(tmpMolFrequencyCell);
            tmpFragmentationTable.addCell(tmpMolPercentageCell);

            for (FragmentDataModel tmpFragmentDataModel : aFragmentDataModelList) {
                int tmpAbsoluteFrequency = tmpFragmentDataModel.getAbsoluteFrequency();
                String tmpStringAbsoluteFrequency = String.format("%d", tmpAbsoluteFrequency);
                double tmpAbsolutePercentage = tmpFragmentDataModel.getAbsolutePercentage();
                int tmpMoleculeFrequency = tmpFragmentDataModel.getMoleculeFrequency();
                String tmpStringMoleculeFrequency = String.format("%d", tmpMoleculeFrequency);
                String tmpStringAbsolutePercentage = String.format("%.3f", tmpAbsolutePercentage);
                double tmpMoleculePercentage = tmpFragmentDataModel.getMoleculePercentage();
                String tmpStringMoleculePercentage = String.format("%.3f", tmpMoleculePercentage);
                //creates an image of the fragment
                IAtomContainer tmpStructureOfFragment = tmpFragmentDataModel.getAtomContainer();
                javafx.scene.image.Image tmpImageStructureOfFragment = DepictionUtil.depictImageWithZoom(tmpStructureOfFragment, 3.0);
                BufferedImage tmpBufferedImageFragment= SwingFXUtils.fromFXImage(tmpImageStructureOfFragment, null);
                File tmpFragmentFile = this.getImageFile(tmpBufferedImageFragment);
                Image tmpImageFragment = Image.getInstance(tmpFragmentFile.getAbsolutePath());
                //inserts the data into the table
                tmpFragmentationTable.addCell(tmpImageFragment);
                tmpFragmentationTable.addCell(tmpFragmentDataModel.getUniqueSmiles());
                tmpFragmentationTable.addCell(tmpStringAbsoluteFrequency);
                tmpFragmentationTable.addCell(tmpStringAbsolutePercentage);
                tmpFragmentationTable.addCell(tmpStringMoleculeFrequency);
                tmpFragmentationTable.addCell(tmpStringMoleculePercentage);
                tmpFragmentFile.delete();
            }
            tmpDocument.add(tmpHeader);
            tmpDocument.add(tmpSpace);
            tmpDocument.add(this.createHeaderTable(aFragmentDataModelList, aMoleculeDataModelList));
            tmpDocument.add(tmpSpace);
            tmpDocument.add(tmpFragmentationTable);
            tmpDocument.close();
            return tmpDocument;
        } catch (IOException | DocumentException | CDKException anException) {
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }

    /**
     * Exports the fragmentation results that appear on the Itemizationtab as a PDF file
     * @param aParentstage Stage to show the FileChooser
     * @param aFragmentDataModelList  a list to iterate through FragmentDataModel
     * @param aMoleculeDataModelList  a list to iterate through MoleculeDataModel
     * @param aFragmentationName
     * @return PDF file with the data that appears on the itemisation tab
     */
    public Document createItemizationTabPdfFile(Stage aParentstage, ObservableList<FragmentDataModel> aFragmentDataModelList, ObservableList<MoleculeDataModel> aMoleculeDataModelList, String aFragmentationName) {
        try {
            File tmpPdfFile = this.saveFile(aParentstage);
            Document tmpDocument = new Document(PageSize.A4);
            PdfWriter.getInstance(tmpDocument, new FileOutputStream(tmpPdfFile.getPath()));
            tmpDocument.open();
            // creates the pdf table
            float tmpCellLength[] = {25f, 50f}; // relative sizes
            PdfPTable tmpFragmentationTable = new PdfPTable(tmpCellLength);
            PdfPCell tmpNameCell = new PdfPCell(new Paragraph("Name", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            tmpNameCell.setFixedHeight(80f);
            PdfPCell tmpStructureCell = new PdfPCell(new Paragraph("Structure", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            tmpStructureCell.setFixedHeight(150f);
            PdfPCell tmpBlankLine1 = new PdfPCell(new Paragraph( ""));
            tmpBlankLine1.setBackgroundColor(Color.WHITE);
            tmpBlankLine1.setBorder(Rectangle.NO_BORDER);
            PdfPCell tmpBlankLine2 = new PdfPCell(new Paragraph(" "));
            tmpBlankLine2.setBackgroundColor(Color.WHITE);
            tmpBlankLine2.setBorder(Rectangle.NO_BORDER);
            Chunk tmpItemizationTabHeader = new Chunk("FRAGMENTATION OF ITEMIZATIONTAB",
                    FontFactory.getFont(FontFactory.TIMES_ROMAN, 18, Font.UNDERLINE));
            Paragraph tmpSpace = new Paragraph(" ");
            for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
                tmpFragmentationTable.addCell(tmpNameCell);
                String name = tmpMoleculeDataModel.getName();
                tmpFragmentationTable.addCell(name);
                tmpFragmentationTable.addCell(tmpStructureCell);
                // Image of molecule
                IAtomContainer tmpMoleculeStructure = tmpMoleculeDataModel.getAtomContainer();
                PdfPCell tmpMoleculeStructureCell = new PdfPCell();
                tmpMoleculeStructureCell.setFixedHeight(150f);
                javafx.scene.image.Image tmpMoleculeImage = DepictionUtil.depictImageWithZoom(tmpMoleculeStructure, 3.0);
                BufferedImage tmpBufferedImageOfMolecule = SwingFXUtils.fromFXImage(tmpMoleculeImage, null);
                File tmpMoleculeFile = this.getImageFile(tmpBufferedImageOfMolecule);
                Image tmpMolecule = Image.getInstance(tmpMoleculeFile.getAbsolutePath());
                tmpMoleculeStructureCell.addElement(tmpMolecule);
                tmpFragmentationTable.addCell(tmpMoleculeStructureCell);
                tmpMoleculeFile.delete();
                List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificAlgorithm(aFragmentationName);
                int tmpIteratorOfFragments = 1;
                for (FragmentDataModel tmpFragmentDataModel : tmpFragmentList) {
                    PdfPCell tmpCellOfFragment = new PdfPCell(new Paragraph("Fragment " + tmpIteratorOfFragments,
                            FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
                    tmpCellOfFragment.setFixedHeight(150f);
                    tmpFragmentationTable.addCell(tmpCellOfFragment);
                    // fragment image
                    PdfPCell tmpCellOfFragmentsStructure = new PdfPCell();
                    tmpCellOfFragmentsStructure.setFixedHeight(150f);
                    IAtomContainer tmpFragmentStructure = tmpFragmentDataModel.getAtomContainer();
                    javafx.scene.image.Image tmpFragmentImage = DepictionUtil.depictImageWithZoom(tmpFragmentStructure, 3.0);
                    BufferedImage tmpBufferedImageOfFragment = SwingFXUtils.fromFXImage(tmpFragmentImage, null);
                    File tmpFragmentFile = this.getImageFile(tmpBufferedImageOfFragment);
                    Image tmpFragment = Image.getInstance(tmpFragmentFile.getAbsolutePath());
                    tmpCellOfFragmentsStructure.addElement(tmpFragment);
                    tmpFragmentationTable.addCell(tmpCellOfFragmentsStructure);
                    tmpIteratorOfFragments++;
                    tmpFragmentFile.delete();
                }
                tmpFragmentationTable.addCell(tmpBlankLine2);
                tmpFragmentationTable.addCell(tmpBlankLine1);
            }
            tmpDocument.add(tmpItemizationTabHeader);
            tmpDocument.add(tmpSpace);
            tmpDocument.add(this.createHeaderTable(aFragmentDataModelList, aMoleculeDataModelList));
            tmpDocument.add(tmpSpace);
            tmpDocument.add(tmpFragmentationTable);
            tmpDocument.close();
            return tmpDocument;
        } catch(IOException | DocumentException | CDKException anException) {
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }

    /**
     * Exports the fragmentation results that appear on the Itemizationtab as a Csv file
     * @param aParentstage Stage to show the FileChooser
     * @param aMoleculeDataModelList  a list to iterate through FragmentDataModel
     * @param aFragmentationName
     * @param aSeperator is the seperator for the csv file
     * @return Csv file with the data that appears on the itemisation tab
     */
    public File createItemizationTabCsvFile(Stage aParentstage, ObservableList<MoleculeDataModel> aMoleculeDataModelList, String aFragmentationName, char aSeperator) {
        try {
            File tmpCsvFile = this.saveFile(aParentstage);
            PrintWriter tmpWriter = new PrintWriter(tmpCsvFile.getPath());
            StringBuilder tmpCsvHeader = new StringBuilder();
            tmpCsvHeader.append("MoleculeName" + aSeperator + "SmilesOfStructure" + aSeperator + "SmilesOfFragments\n");
            tmpWriter.write(tmpCsvHeader.toString());
            for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
                tmpWriter.printf("%s" +aSeperator+ "%s" , tmpMoleculeDataModel.getName(), tmpMoleculeDataModel.getSmiles());
                List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificAlgorithm(aFragmentationName);
                for (FragmentDataModel tmpFragmentDataModel: tmpFragmentList) {
                    tmpWriter.append(aSeperator);
                    tmpWriter.printf("%s", tmpFragmentDataModel.getUniqueSmiles());
                }
                tmpWriter.append("\n");
            }
            tmpWriter.close();
            return tmpCsvFile;
        } catch (FileNotFoundException anException) {
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }
    //</editor-fold>

    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Creates a ImageFile
     * @param aBufferedImage
     * @return File with a Image of Molecule or Structure
     */
    private File getImageFile(BufferedImage aBufferedImage) {
        try {
            File tmpImageFile = new File("Image.png");
            ImageIO.write(aBufferedImage, "PNG", tmpImageFile);
            return tmpImageFile;
        } catch (IOException anIoException) {
            Exporter.LOGGER.log(Level.SEVERE, anIoException.toString(), anIoException);
            return  null;
        }
    }

    /**
     * Creates a header with general information for the PDf files
     * @param aFragmentDataModelList a list to iterate through FragmentDataModel
     * @param aMoleculeDataModelList a list to iterate through MoleculeDataModel
     * @return  table with this informations
     */
    private PdfPTable createHeaderTable(ObservableList<FragmentDataModel> aFragmentDataModelList, ObservableList<MoleculeDataModel> aMoleculeDataModelList) {
        int tmpFragmentIterator = 0;
        int tmpMoleculeIterator = 0;
        for (FragmentDataModel tmpFragmentDataModel : aFragmentDataModelList) {
            tmpFragmentIterator++;
        }
        for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
            tmpMoleculeIterator++;
        }
        float tmpCellLengthIntro[] = {60f, 60f}; // relative sizes
        PdfPTable tmpTableIntro = new PdfPTable(tmpCellLengthIntro);
        PdfPCell tmpIntroCell1 = new PdfPCell(new Paragraph("Algorithm used", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
        PdfPCell tmpIntroCell2 = new PdfPCell(new Paragraph("Ertl algorithm"));
        PdfPCell tmpIntroCell3 = new PdfPCell(new Paragraph("Number of molecules",FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
        PdfPCell tmpIntroCell4 = new PdfPCell(new Paragraph(String.valueOf(tmpMoleculeIterator)));
        PdfPCell tmpIntroCell5 = new PdfPCell(new Paragraph("Number of fragments",FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
        PdfPCell tmpIntroCell6 = new PdfPCell(new Paragraph(String.valueOf(tmpFragmentIterator)));
        tmpTableIntro.addCell(tmpIntroCell1);
        tmpTableIntro.addCell(tmpIntroCell2);
        tmpTableIntro.addCell(tmpIntroCell3);
        tmpTableIntro.addCell(tmpIntroCell4);
        tmpTableIntro.addCell(tmpIntroCell5);
        tmpTableIntro.addCell(tmpIntroCell6);
        return tmpTableIntro;
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
        FileChooser.ExtensionFilter tmpExtensionFilter1 = new FileChooser.ExtensionFilter("PDF Files", "*.pdf");
        FileChooser.ExtensionFilter tmpExtensionfilter2 = new FileChooser.ExtensionFilter("Csv Files", "*.csv");
        tmpFileChooser.getExtensionFilters().addAll(tmpExtensionFilter1, tmpExtensionfilter2);
        File tmpFile = null;
        try{
            tmpFile = tmpFileChooser.showSaveDialog(aParentStage);
        } catch(Exception anException){
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
        } finally {
            return tmpFile;
        }
    }
    //</editor-fold>
}