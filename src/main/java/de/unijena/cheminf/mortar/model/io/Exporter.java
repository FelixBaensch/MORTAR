/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.model.io;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exporter
 *
 */
public class Exporter {
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(Exporter.class.getName());
    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Private class constants">
    /**
     * Font of any cells
     */
    private Font fontFactory =  FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD);
    //</editor-fold>
    //
    /**
     * Constructor
     */
    public Exporter() {
    }
    //<editor-fold desc="Public methods" defaultstate="collapsed">
    /**
     * Exports the fragmentation results that appear on the fragmentationtab as a CSV file
     * @param aParentStage  Stage to show the FileChooser
     * @param aList a list to iterate through FragmentDataModel
     * @param aSeperator is the seperator for the csv file
     * @return  Csv file which contains the results of the fragmentation
     */
    public File createFragmentationTabCsvFile(Stage aParentStage, ObservableList<FragmentDataModel> aList, char aSeperator) {
        try {
            File tmpFragmentationCsvFile = this.saveFile(aParentStage, "CSV", "*.csv",
                    "FragmentExport");
            PrintWriter tmpWriter = new PrintWriter(tmpFragmentationCsvFile.getPath());
            StringBuilder tmpFragmentationCsvHeader = new StringBuilder();
            tmpFragmentationCsvHeader.append("SmilesString"+aSeperator+"Frequency"+aSeperator+"Percentage"
                    +aSeperator+"MolecularFrequency"
                    + aSeperator + "MolecularPercentage\n");
            tmpWriter.write(tmpFragmentationCsvHeader.toString());
            for (FragmentDataModel tmpFragmentDataModel : aList) {
                tmpWriter.printf("%s"+aSeperator+"%d"+aSeperator+"%.3f"+aSeperator+"%d"+aSeperator+"%.2f\n",
                        tmpFragmentDataModel.getUniqueSmiles(), tmpFragmentDataModel.getAbsoluteFrequency(),
                        tmpFragmentDataModel.getAbsolutePercentage(), tmpFragmentDataModel.getMoleculeFrequency(),
                        tmpFragmentDataModel.getMoleculePercentage());
            }
            tmpWriter.close();
            return tmpFragmentationCsvFile;
        } catch (FileNotFoundException | NullPointerException anException) {
            GuiUtil.GuiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),Message.get("Error.ExceptionAlert.Header"),
                    anException.toString(), anException);
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
    public Document createFragmentationTabPdfFile(Stage aParentstage, ObservableList<FragmentDataModel> aFragmentDataModelList, ObservableList<MoleculeDataModel> aMoleculeDataModelList, String aName) {
        try {
            File tmpFragmentationPdfFile = this.saveFile(aParentstage, "PDF", "*.pdf",
                    "FragmentExport");
            Document tmpDocument = new Document(PageSize.A4);
            tmpDocument.setPageSize(tmpDocument.getPageSize().rotate());
            PdfWriter.getInstance(tmpDocument, new FileOutputStream(tmpFragmentationPdfFile.getPath()));
            tmpDocument.open();
            float tmpCellLength[] = {70f, 120f, 50f, 50f, 55f, 55f}; // relative sizes
            PdfPTable tmpFragmentationTable = new PdfPTable(tmpCellLength);
            PdfPCell tmpSmilesStringCell = new PdfPCell(new Paragraph("Smiles String ", fontFactory));
            PdfPCell tmpFrequencyCell = new PdfPCell(new Paragraph("Frequency",this.fontFactory));
            PdfPCell tmpPercentageCell = new PdfPCell(new Paragraph("Percentage",this.fontFactory));
            PdfPCell tmpMolFrequencyCell = new PdfPCell(new Paragraph("Molecule-frequency",this.fontFactory));
            PdfPCell tmpMolPercentageCell = new PdfPCell(new Paragraph(" Molecule-percentage",this.fontFactory));
            PdfPCell tmpFragmentCell = new PdfPCell(new Paragraph("Fragment", this.fontFactory));
            Chunk tmpHeader = new Chunk("Export of the fragmentation tab",
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
                String tmpStringMoleculePercentage = String.format("%.2f", tmpMoleculePercentage);
                //creates an image of the fragment
                PdfPCell tmpImageFragmentCell = new PdfPCell();
                tmpImageFragmentCell.setFixedHeight(85f);
                IAtomContainer tmpStructureOfFragment = tmpFragmentDataModel.getAtomContainer();
                javafx.scene.image.Image tmpImageStructureOfFragment = DepictionUtil.depictImageWithZoom(tmpStructureOfFragment,
                        4.0);
                BufferedImage tmpBufferedImageFragment= SwingFXUtils.fromFXImage(tmpImageStructureOfFragment, null);
                File tmpFragmentFile = this.getImageFile(tmpBufferedImageFragment);
                Image tmpImageFragment = Image.getInstance(tmpFragmentFile.getAbsolutePath());
                //inserts the data into the table
                PdfPCell tmpCellOfFrequency = new PdfPCell(new Paragraph(tmpStringAbsoluteFrequency));
                tmpCellOfFrequency.setHorizontalAlignment(Element.ALIGN_RIGHT);
                PdfPCell tmpCellOfPercentage = new PdfPCell(new Paragraph(tmpStringAbsolutePercentage));
                tmpCellOfPercentage.setHorizontalAlignment(Element.ALIGN_RIGHT);
                PdfPCell tmpCellOfMolFrequency = new PdfPCell(new Paragraph(tmpStringMoleculeFrequency));
                tmpCellOfMolFrequency.setHorizontalAlignment(Element.ALIGN_RIGHT);
                PdfPCell tmpCellOfMolPercentage = new PdfPCell(new Paragraph(tmpStringMoleculePercentage));
                tmpCellOfMolPercentage.setHorizontalAlignment(Element.ALIGN_RIGHT);
                tmpImageFragmentCell.addElement(tmpImageFragment);
                tmpFragmentationTable.addCell(tmpImageFragmentCell);
                tmpFragmentationTable.addCell(tmpFragmentDataModel.getUniqueSmiles());
                tmpFragmentationTable.addCell(tmpCellOfFrequency);
                tmpFragmentationTable.addCell(tmpCellOfPercentage);
                tmpFragmentationTable.addCell(tmpCellOfMolFrequency);
                tmpFragmentationTable.addCell(tmpCellOfMolPercentage);
                tmpFragmentFile.delete();
            }
            tmpDocument.add(tmpHeader);
            tmpDocument.add(tmpSpace);
            tmpDocument.add(this.createHeaderTable(aFragmentDataModelList, aMoleculeDataModelList,aName ));
            tmpDocument.add(tmpSpace);
            tmpDocument.add(tmpFragmentationTable);
            tmpDocument.close();
            return tmpDocument;
        } catch (IOException | DocumentException | CDKException | NullPointerException anException) {
            GuiUtil.GuiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),Message.get("Error.ExceptionAlert.Header"),
                    anException.toString(), anException);
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
    public Document createItemizationTabPdfFile(Stage aParentstage, ObservableList<FragmentDataModel> aFragmentDataModelList, ObservableList<MoleculeDataModel> aMoleculeDataModelList, String aFragmentationName, String aName) {
        try {
            File tmpPdfFile = this.saveFile(aParentstage, "PDF", "*.pdf","fragmentExport");
            Document tmpDocument = new Document(PageSize.A4);
            PdfWriter.getInstance(tmpDocument, new FileOutputStream(tmpPdfFile.getPath()));
            tmpDocument.open();
            // creates the pdf table
            Chunk tmpItemizationTabHeader = new Chunk("Export of the Itemization tab",
                    FontFactory.getFont(FontFactory.TIMES_ROMAN, 18, Font.UNDERLINE));
            Paragraph tmpSpace = new Paragraph(" ");
            tmpDocument.add(tmpItemizationTabHeader);
            tmpDocument.add(tmpSpace);
            tmpDocument.add(this.createHeaderTable(aFragmentDataModelList, aMoleculeDataModelList,aName));
            tmpDocument.add(tmpSpace);
            for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
                PdfPTable tmpTable = new PdfPTable(2);
                PdfPTable tmpFragmentTableversuch = new PdfPTable(1);
                tmpTable.setWidths( new int[]{40, 80} );
                PdfPCell tmpNameCell = new PdfPCell(new Paragraph("Name",this.fontFactory));
                tmpNameCell.setFixedHeight(55f);
                PdfPCell tmpStructureCell = new PdfPCell(new Paragraph("Structure", this.fontFactory));
                tmpStructureCell.setFixedHeight(120f);
                tmpTable.addCell(tmpNameCell);
                String tmpName = tmpMoleculeDataModel.getName();
                tmpTable.addCell(tmpName);
                tmpTable.addCell(tmpStructureCell);
                // Image of molecule
                IAtomContainer tmpMoleculeStructure = tmpMoleculeDataModel.getAtomContainer();
                PdfPCell tmpMoleculeStructureCell = new PdfPCell();
                tmpMoleculeStructureCell.setFixedHeight(120f);
                javafx.scene.image.Image tmpMoleculeImage = DepictionUtil.depictImageWithZoom(tmpMoleculeStructure,
                        3.0);
                BufferedImage tmpBufferedImageOfMolecule = SwingFXUtils.fromFXImage(tmpMoleculeImage, null);
                File tmpMoleculeFile = this.getImageFile(tmpBufferedImageOfMolecule);
                Image tmpMolecule = Image.getInstance(tmpMoleculeFile.getAbsolutePath());
                tmpMoleculeStructureCell.addElement(tmpMolecule);
                tmpTable.addCell(tmpMoleculeStructureCell);
                PdfPCell tmpCellOfFragment = new PdfPCell(new Paragraph("Fragements", this.fontFactory));
                tmpCellOfFragment.setHorizontalAlignment(Element.ALIGN_CENTER);
                tmpFragmentTableversuch.addCell(tmpCellOfFragment);
                tmpDocument.add(tmpTable);
                tmpDocument.add(tmpFragmentTableversuch);
                tmpMoleculeFile.delete();
                List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificAlgorithm(aFragmentationName);
                PdfPTable tmpFragmentationTable2 = new PdfPTable(3);
                for (int tmpFragmentNumber = 0; tmpFragmentNumber < tmpFragmentList.size();) {
                    ArrayList<PdfPCell> tmpCell = new ArrayList<PdfPCell>();
                    int tmpImagesNumbers = 0;
                    for ( ; tmpImagesNumbers < 3; tmpImagesNumbers++) {
                        if(tmpFragmentNumber >= tmpFragmentList.size()) {
                            break;
                        }
                        IAtomContainer tmpFragmentStructure = tmpFragmentList.get(tmpFragmentNumber).getAtomContainer();
                        javafx.scene.image.Image tmpFragmentImage = DepictionUtil.depictImageWithZoom(tmpFragmentStructure,
                                3.0);
                        BufferedImage tmpBufferedImageOfFragment = SwingFXUtils.fromFXImage(tmpFragmentImage, null);
                        File tmpFragmentFile = this.getImageFile(tmpBufferedImageOfFragment);
                        Image tmpFragment = Image.getInstance(tmpFragmentFile.getAbsolutePath());
                        PdfPCell cell = new PdfPCell();
                        cell.addElement(tmpFragment);
                        tmpCell.add(cell);
                        tmpFragmentNumber++;
                        tmpFragmentFile.delete();
                    }
                    for (int tmpCellIterator = 0; tmpCellIterator <3 ; tmpCellIterator++) {
                        if (tmpCellIterator < tmpImagesNumbers) {
                            tmpFragmentationTable2.addCell(tmpCell.get(tmpCellIterator));
                        } else {
                            tmpFragmentationTable2.addCell(new Paragraph(""));
                        }
                    }
                }
                tmpDocument.add(tmpFragmentationTable2);
                tmpDocument.newPage();
            }
            tmpDocument.close();
            return tmpDocument;
        } catch(IOException | DocumentException | CDKException | NullPointerException anException) {
            GuiUtil.GuiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),Message.get("Error.ExceptionAlert.Header"),
                    anException.toString(), anException);
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }

    /**
     * Exports the fragmentation results that appear on the Itemization tab as a Csv file
     * @param aParentstage Stage to show the FileChooser
     * @param aMoleculeDataModelList  a list to iterate through FragmentDataModel
     * @param aFragmentationName
     * @param aSeparator is the separator for the csv file
     * @return Csv file with the data that appears on the itemisation tab
     */
    public File createItemizationTabCsvFile(Stage aParentstage, ObservableList<MoleculeDataModel> aMoleculeDataModelList, String aFragmentationName, char aSeparator) {
        try {
            File tmpCsvFile = this.saveFile(aParentstage, "CSV", "*.csv", "FragmentExport");
            PrintWriter tmpWriter = new PrintWriter(tmpCsvFile.getPath());
            StringBuilder tmpCsvHeader = new StringBuilder();
            tmpCsvHeader.append("MoleculeName"+aSeparator+"SmilesOfStructure"+aSeparator+"SmilesOfFragments\n");
            tmpWriter.write(tmpCsvHeader.toString());
            for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
                tmpWriter.printf("%s"+aSeparator+"%s", tmpMoleculeDataModel.getName(), tmpMoleculeDataModel.getSmiles());
                List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificAlgorithm(aFragmentationName);
                for (FragmentDataModel tmpFragmentDataModel: tmpFragmentList) {
                    tmpWriter.append(aSeparator);
                    tmpWriter.printf("%s", tmpFragmentDataModel.getUniqueSmiles());
                }
                tmpWriter.append("\n");
            }
            tmpWriter.close();
            return tmpCsvFile;
        } catch (FileNotFoundException | NullPointerException anException) {
            GuiUtil.GuiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),Message.get("Error.ExceptionAlert.Header"),
                    anException.toString(), anException);
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Creates a ImageFile
     * @param aBufferedImage
     * @return File with a Image of Molecule or Structure
     */
    private File getImageFile(BufferedImage aBufferedImage) {
        try {
            String tmpImageDirectoryPathName = FileUtil.getAppDirPath() + File.separator
                    + BasicDefinitions.IMAGE_FILES_DIRECTORY+ File.separator;
            File tmpImageDirectoryFile = new File(tmpImageDirectoryPathName);
            String tmpImagesFilePathName = tmpImageDirectoryPathName + BasicDefinitions.IMAGE_FILE_NAME;
            if (!tmpImageDirectoryFile.exists()) {
                FileUtil.createDirectory(tmpImageDirectoryFile.getAbsolutePath());
            }
            String tmpFinalImageFilePathName = FileUtil.getNonExistingFilePath(tmpImagesFilePathName,
                    BasicDefinitions.IMAGE_FILE_NAME_EXTENSION);
            File tmpImageFile = new File(tmpFinalImageFilePathName);
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
    private PdfPTable createHeaderTable(ObservableList<FragmentDataModel> aFragmentDataModelList, ObservableList<MoleculeDataModel> aMoleculeDataModelList, String anAlgorthimName) {
        int tmpFragmentNumbers =  aFragmentDataModelList.size();
        int tmpMoleculeNumbers = aMoleculeDataModelList.size();
        // creates the header
        float tmpCellLengthIntro[] = {60f, 60f}; // relative sizes
        PdfPTable tmpTableIntro = new PdfPTable(tmpCellLengthIntro);
        PdfPCell tmpIntroCell1 = new PdfPCell(new Paragraph("Algorithm used",this.fontFactory));
        PdfPCell tmpIntroCell2 = new PdfPCell(new Paragraph(anAlgorthimName));
        PdfPCell tmpIntroCell3 = new PdfPCell(new Paragraph("Number of molecules",this.fontFactory));
        PdfPCell tmpIntroCell4 = new PdfPCell(new Paragraph(String.valueOf(tmpMoleculeNumbers)));
        PdfPCell tmpIntroCell5 = new PdfPCell(new Paragraph("Number of fragments",this.fontFactory));
        PdfPCell tmpIntroCell6 = new PdfPCell(new Paragraph(String.valueOf(tmpFragmentNumbers)));
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
    private File saveFile(Stage aParentStage, String aDescription, String anExtension, String aFileName) throws NullPointerException {
        Objects.requireNonNull(aParentStage, "aParentStage (instance of Stage) is null");
        FileChooser tmpFileChooser = new FileChooser();
        tmpFileChooser.setTitle((Message.get("Exporter.fileChooser.title")));
        FileChooser.ExtensionFilter tmpExtensionfilter2 = new FileChooser.ExtensionFilter(aDescription, anExtension);
        tmpFileChooser.getExtensionFilters().addAll(tmpExtensionfilter2);
        tmpFileChooser.setInitialFileName(aFileName);
        File  tmpFile = tmpFileChooser.showSaveDialog(aParentStage);
        return tmpFile;
    }
    //</editor-fold>
}