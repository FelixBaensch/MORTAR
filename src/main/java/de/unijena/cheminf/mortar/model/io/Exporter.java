/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.controller.TabNames;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.MiscUtil;

import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Writer;
import org.openscience.cdk.io.PDBWriter;
import org.openscience.cdk.io.SDFWriter;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exporter.
 *
 * @author Betuel Sevindik
 * @author Samuel Behr
 * @author Felix Baensch
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class Exporter {
    //<editor-fold desc="Enum ExportTypes" defaultstate="collapsed">
    /**
     * Enum for different file types to export.
     */
    public enum ExportTypes {
        /**
         * enum value for item csv file.
         */
        ITEM_CSV_FILE,
        /**
         * enum value for item pdf file.
         */
        ITEM_PDF_FILE,
        /**
         * enum value for fragments csv file.
         */
        FRAGMENT_CSV_FILE,
        /**
         * enum value for fragments pdf file.
         */
        FRAGMENT_PDF_FILE,
        /**
         * enum value for single sd file.
         */
        FRAGMENT_SINGLE_SD_FILE,
        /**
         * enum value for sd file.
         */
        FRAGMENT_MULTIPLE_SD_FILES,
        /**
         * enum value for pdb file.
         */
        FRAGMENT_PDB_FILE;
    }
    //</editor-fold>
    //
    /**
     * Enum for different file extension
     */
    public enum FileExtension {
        /**
         * Enum for csv extension
         */
        CSV(".csv"),
        /**
         * Enum for pdf extension
         */
        PDF(".pdf"),
        /**
         * Enum for sdf extension
         */
        SDF(".sdf"),
        /**
         * Enum for pdb extension
         */
        PDB(".pdb"),
        ;
        //
        /**
         * String value of enum
         */
        private final String extension;
        //
        /**
         * Enum for different file extension
         *
         * @param anExtension String value of extension
         */
        FileExtension (final String anExtension) {
            this.extension = anExtension;
        }
        //
        /**
         * Returns the string value of extension
         *
         * @return String value of extension
         */
        @Override
        public String toString() {
            return extension;
        }
    }
    //
    //<editor-fold desc="Enum CSVSeparator">
    /**
     * Enum for allowed CSV file export separator chars.
     */
    public enum CSVSeparator implements IDisplayEnum {
        /**
         * Comma.
         */
        COMMA(',', Message.get("Exporter.CSVSeparator.Comma.displayName"), Message.get("Exporter.CSVSeparator.Comma.tooltip")),
        /**
         * Semicolon.
         */
        SEMICOLON(';', Message.get("Exporter.CSVSeparator.Semicolon.displayName"), Message.get("Exporter.CSVSeparator.Semicolon.tooltip")),
        /**
         * Tab.
         */
        TAB('\t', Message.get("Exporter.CSVSeparator.Tab.displayName"), Message.get("Exporter.CSVSeparator.Tab.tooltip")),
        /**
         * Space.
         */
        SPACE(' ', Message.get("Exporter.CSVSeparator.Space.displayName"), Message.get("Exporter.CSVSeparator.Space.tooltip"));
        /**
         * Character representation of the wrapped separator char.
         */
        private final char separatorChar;
        /**
         * Language-specific name for display in GUI.
         */
        private final String displayName;
        /**
         * Language-specific tooltip text for display in GUI.
         */
        private final String tooltip;
        /**
         * Constructor setting the wrapped separator char, display name, and tooltip text.
         *
         * @param aSeparatorChar CSV separator character to use when this option is selected
         * @param aDisplayName display name
         * @param aTooltipText tooltip text
         */
        private CSVSeparator(char aSeparatorChar, String aDisplayName, String aTooltipText) {
            this.separatorChar = aSeparatorChar;
            this.displayName = aDisplayName;
            this.tooltip = aTooltipText;
        }
        /**
         * Returns the character representation of this separator.
         *
         * @return CSV separator char
         */
        public char getSeparatorChar() {
            return this.separatorChar;
        }
        //
        @Override
        public String getDisplayName() {
            return this.displayName;
        }
        //
        @Override
        public String getTooltipText() {
            return this.tooltip;
        }
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(Exporter.class.getName());
    //
    /**
     * Name of directory generated for exporting a stream of fragment files.
     */
    private static final String FRAGMENTS_EXPORT_DIRECTORY_NAME = "MORTAR_Fragments_Export";
    //
    /**
     * Font for cells in exported PDF files.
     */
    private static final Font PDF_CELL_FONT = FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD);
    //</editor-fold>
    //
    // <editor-fold defaultstate="collapsed" desc="Private final variables">
    /**
     * Container for general settings for managing, preserving, and reloading application settings.
     */
    private final SettingsContainer settingsContainer;
    //</editor-fold>
    //
    //<editor-fold desc="Constructors">
    /**
     * Constructor. Should the recent directory path provided by the container be faulty, it is set to its default
     * value as defined by the respective constant in the SettingsContainer class.
     *
     * @param aSettingsContainer the MORTAR general settings container providing a recent directory path and other
     *                           export-related settings
     * @throws NullPointerException if the settings container is null
     */
    public Exporter(SettingsContainer aSettingsContainer) throws NullPointerException {
        Objects.requireNonNull(aSettingsContainer, "Given settings container is null.");
        this.settingsContainer = aSettingsContainer;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public methods" defaultstate="collapsed">
    /**
     * Opens FileChooser to enable the user to choose an export destination file.
     *
     * @param aParentStage Stage
     * @param anExportType enum ExportType specifies file type
     * @param aFragmentationName String for name of fragmentation to be used as file name proposal
     * @return the chosen file or directory or null if the user chose to cancel the export in the file chooser dialog
     */
    public File openFileChooserForExportFileOrDir(Stage aParentStage, ExportTypes anExportType, String aFragmentationName){
        Objects.requireNonNull(aParentStage, "aParentStage must not be null");
        File tmpFile;
        String tmpFileName;
        String tmpFragmentationName = aFragmentationName.replaceAll("\\s+", "_");
        tmpFile = switch (anExportType) {
            case ExportTypes.FRAGMENT_CSV_FILE -> {
                tmpFileName = "Fragments_" + tmpFragmentationName;
                tmpFile = this.chooseFile(aParentStage, "CSV", "*" + FileExtension.CSV, tmpFileName);
                if (tmpFile != null && !tmpFile.getName().endsWith(FileExtension.CSV.extension)) {
                    tmpFile = new File(tmpFile.getAbsolutePath() + FileExtension.CSV);
                }
                yield tmpFile;
            }
            case ExportTypes.FRAGMENT_PDB_FILE, ExportTypes.FRAGMENT_MULTIPLE_SD_FILES ->
                    this.chooseDirectory(aParentStage);
            case ExportTypes.FRAGMENT_PDF_FILE -> {
                tmpFileName = "Fragments_" + tmpFragmentationName;
                tmpFile = this.chooseFile(aParentStage, "PDF", "*" + FileExtension.PDF, tmpFileName);
                if (tmpFile != null && !tmpFile.getName().endsWith(FileExtension.PDF.extension)) {
                    tmpFile = new File(tmpFile.getAbsolutePath() + FileExtension.PDF);
                }
                yield tmpFile;
            }
            case ExportTypes.FRAGMENT_SINGLE_SD_FILE -> {
                tmpFileName = "Fragments_Export_" + tmpFragmentationName;
                tmpFile = this.chooseFile(aParentStage, "SD-File", "*" + FileExtension.SDF, tmpFileName);
                if (tmpFile != null && !tmpFile.getName().endsWith(FileExtension.SDF.extension)) {
                    tmpFile = new File(tmpFile.getAbsolutePath() + FileExtension.SDF);
                }
                yield tmpFile;
            }
            case ExportTypes.ITEM_CSV_FILE -> {
                tmpFileName = "Items_" + tmpFragmentationName;
                tmpFile = this.chooseFile(aParentStage, "CSV", "*" + FileExtension.CSV, tmpFileName);
                if (tmpFile != null && !tmpFile.getName().endsWith(FileExtension.CSV.extension)) {
                    tmpFile = new File(tmpFile.getAbsolutePath() + FileExtension.CSV);
                }
                yield tmpFile;
            }
            case ExportTypes.ITEM_PDF_FILE -> {
                tmpFileName = "Items_" + tmpFragmentationName;
                tmpFile = this.chooseFile(aParentStage, "PDF", "*" + FileExtension.PDF, tmpFileName);
                if (tmpFile != null && !tmpFile.getName().endsWith(FileExtension.PDF.extension)) {
                    tmpFile = new File(tmpFile.getAbsolutePath() + FileExtension.PDF);
                }
                yield tmpFile;
            }
            default ->
                    throw new UnsupportedOperationException(String.format("Unsupported export type: %s", anExportType));
        };
        return tmpFile;
    }
    //
    /**
     * Exports the fragmentation results as displayed on the Itemisation tab or
     * on the Fragments tab, depending on aTabName, to a CSV file.
     * Returns a list containing SMILES of the molecules that cause an error when exported.
     *
     * @param aFile                  the file to export to; method returns null if the file is null
     * @param aMoleculeDataModelList a list of MoleculeDataModel instances to export along with their fragments
     * @param aFragmentationName     fragmentation name to retrieve the specific set of fragments from the molecule data models
     * @param aSeparator             the separator for the csv file
     * @param aTabName               TabName to identify type of tab
     * @return List {@literal <}String {@literal >} SMILES codes of the molecules that caused an error
     * @throws FileNotFoundException if the given file cannot be found
     */
    public List<String> exportCsvFile(File aFile, List<MoleculeDataModel> aMoleculeDataModelList, String aFragmentationName, char aSeparator, TabNames aTabName)
            throws FileNotFoundException {
        if (aFile == null) {
            return null;
        }
        if (aTabName.equals(TabNames.FRAGMENTS)) {
            //can throw FileNotFoundException, gets handled in setOnFailed()
            return this.createFragmentsTabCsvFile(aFile, aMoleculeDataModelList, aSeparator);
        } else if (aTabName.equals(TabNames.ITEMIZATION)) {
            //can throw FileNotFoundException, gets handled in setOnFailed()
            return this.createItemizationTabCsvFile(aFile, aMoleculeDataModelList, aFragmentationName, aSeparator);
        }
        return new ArrayList<>(0);
    }
    //
    /**
     * Exports depending on aTabName the fragmentation results as displayed on the Itemisation tab or on the Fragments tab as a CSV file.
     * Returns a list containing SMILES of the molecules that caused an error when exported.
     *
     * @param aFile                  the file to export to
     * @param aFragmentDataModelList a list of FragmentDataModel instances to export
     * @param aMoleculeDataModelList a list MoleculeDataModel to export items
     * @param aFragmentationName     fragmentation name to be displayed in the header of the PDF file
     * @param anImportedFileName name of the input file whose molecules were fragmented
     * @param aTabName               TabName to identify type of tab
     * @return List {@literal <}String {@literal >} SMILES codes of the molecules that caused an error
     * @throws FileNotFoundException if the given file cannot be found
     */
    public List<String> exportPdfFile(File aFile,
                                      List<MoleculeDataModel> aFragmentDataModelList,
                                      ObservableList<MoleculeDataModel> aMoleculeDataModelList,
                                      String aFragmentationName,
                                      String anImportedFileName,
                                      TabNames aTabName) throws FileNotFoundException {
        if (aFile == null) {
            return null;
        }
        if (aTabName.equals(TabNames.FRAGMENTS)) {
            //throws FileNotFoundException, gets handled in setOnFailed()
            return this.createFragmentsTabPdfFile(aFile, aFragmentDataModelList, aMoleculeDataModelList.size(), aFragmentationName, anImportedFileName);
        } else if (aTabName.equals(TabNames.ITEMIZATION)) {
            //throws FileNotFoundException, gets handled in setOnFailed()
            return this.createItemizationTabPdfFile(aFile, aFragmentDataModelList.size(), aMoleculeDataModelList, aFragmentationName, anImportedFileName);
        }
        return null;
    }
    //
    /**
     * Exports depending on aFragmentationName the results as displayed on the Itemisation tab or on the Fragments tab
     * as a chemical file.
     * Returns a list containing SMILES of the molecules that caused an error when exported. The fragments are exported
     * into separate files (single export is set to false).
     *
     * @param aFile the file/directory to export to
     * @param aFragmentDataModelList list of FragmentDataModel instances
     * @param aChemFileType ChemFileTypes specifies which file type should be exported
     * @param aGenerate2dAtomCoordinates boolean value whether to generate 2D coordinates
     * @return List {@literal <}String {@literal >} SMILES codes of the molecules that caused an error
     * @throws IOException if sth goes wrong
     */
    public List<String> exportFragmentsAsChemicalFile(File aFile,
                                                      List<MoleculeDataModel> aFragmentDataModelList,
                                                      ChemFileTypes aChemFileType,
                                                      boolean aGenerate2dAtomCoordinates)
            throws IOException {
        return this.exportFragmentsAsChemicalFile(aFile,
                aFragmentDataModelList,
                aChemFileType,
                aGenerate2dAtomCoordinates,
                false);
    }
    //
    /**
     * Exports in a new thread the results as displayed on the Itemisation tab or on the Fragments tab as a chemical file.
     * Returns a list containing SMILES of the molecules that caused an error when exported.
     *
     * @param aFile the file/directory to export to
     * @param aFragmentDataModelList list of FragmentDataModel instances
     * @param aChemFileType ChemFileTypes specifies which file type should be exported
     * @param aGenerate2dAtomCoordinates boolean value whether to generate 2D coordinates
     * @param anIsSingleExport true if fragments should be exported into one single file; false if separated, one file for each fragment
     * @return List {@literal <}String {@literal >} SMILES codes of the molecules that caused an error
     * @throws IOException if sth goes wrong
     */
    public List<String> exportFragmentsAsChemicalFile(File aFile,
                                                      List<MoleculeDataModel> aFragmentDataModelList,
                                                      ChemFileTypes aChemFileType,
                                                      boolean aGenerate2dAtomCoordinates,
                                                      boolean anIsSingleExport) throws IOException {
        if (aFile == null) {
            return null;
        }
        List<String> tmpReturnedList;
        if (aChemFileType == ChemFileTypes.SDF && anIsSingleExport) {
            tmpReturnedList = this.createFragmentationTabSingleSDFile(aFile, aFragmentDataModelList, aGenerate2dAtomCoordinates, this.settingsContainer.getAlwaysMDLV3000FormatAtExportSetting());
        } else if (aChemFileType == ChemFileTypes.SDF) {
            tmpReturnedList = this.createFragmentationTabSeparateSDFiles(aFile, aFragmentDataModelList, aGenerate2dAtomCoordinates, this.settingsContainer.getAlwaysMDLV3000FormatAtExportSetting());
        } else if (aChemFileType == ChemFileTypes.PDB) {
            tmpReturnedList = this.createFragmentationTabPDBFiles(aFile, aFragmentDataModelList, aGenerate2dAtomCoordinates);
        } else {
            tmpReturnedList = null;
        }
        return tmpReturnedList;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods" defaultstate="collapsed">
    /**
     * Exports the fragmentation results as they are displayed on the itemization tab as a CSV file. In the molecule names,
     * the given separator character is replaced by a placeholder character ('_').
     *
     * @param aMoleculeDataModelList a list of MoleculeDataModel instances to export along with their fragments
     * @param aFragmentationName     fragmentation name to retrieve the specific set of fragments from the molecule data models
     * @param aSeparator             the separator for the csv file
     * @return List {@literal <}String {@literal >} SMILES codes of the molecules that caused an error
     * @throws FileNotFoundException if given file cannot be found
     * @author Betül Sevindik
     */
    private List<String> createItemizationTabCsvFile(File aCsvFile,
                                             List<MoleculeDataModel> aMoleculeDataModelList,
                                             String aFragmentationName,
                                             char aSeparator) throws FileNotFoundException {
        if (aCsvFile == null || aMoleculeDataModelList == null || aFragmentationName == null) {
            return null;
        }
        List<String> tmpFailedExportFragments = new LinkedList<>();
        //the character used to replace all occurrences of the given separator char in the exported strings
        final char tmpReplacementChar = '_';
        try (PrintWriter tmpWriter = new PrintWriter(aCsvFile.getPath())) {
            String tmpCsvHeader = Message.get("Exporter.itemsTab.csvHeader.moleculeName") + aSeparator +
                    Message.get("Exporter.itemsTab.csvHeader.smilesOfStructure") + aSeparator +
                    Message.get("Exporter.itemsTab.csvHeader.smilesOfFragment") + aSeparator +
                    Message.get("Exporter.itemsTab.csvHeader.frequencyOfFragment");
            tmpWriter.write(tmpCsvHeader);
            for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                try {
                    tmpWriter.printf("%n%s%s%s",
                            tmpMoleculeDataModel.getName().replace(aSeparator, tmpReplacementChar),
                            aSeparator,
                            //note to developers: make sure no chars are offered as separator char options that can
                            // also occur in SMILES strings!
                            tmpMoleculeDataModel.getUniqueSmiles());
                } catch (Exception anException) {
                    Logger.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, String.format("%s molecule name: %s", anException.toString(), tmpMoleculeDataModel.getName()), anException);
                    tmpFailedExportFragments.add(tmpMoleculeDataModel.getUniqueSmiles());
                    continue;
                }
                if (!tmpMoleculeDataModel.hasMoleculeUndergoneSpecificFragmentation(aFragmentationName)) {
                    continue;
                }
                List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificFragmentation(aFragmentationName);
                for (FragmentDataModel tmpFragmentDataModel : tmpFragmentList) {
                    if (Thread.currentThread().isInterrupted()) {
                        return null;
                    }
                    tmpWriter.append(aSeparator);
                    try {
                        tmpWriter.printf("%s%s%s",
                                tmpFragmentDataModel.getUniqueSmiles(),
                                aSeparator,
                                tmpMoleculeDataModel.getFragmentFrequencyOfSpecificFragmentation(aFragmentationName).get(tmpFragmentDataModel.getUniqueSmiles()).toString());
                    } catch (Exception anException) {
                        Logger.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, String.format("%s molecule name: %s", anException.toString(), tmpFragmentDataModel.getName()), anException);
                        tmpFailedExportFragments.add(tmpFragmentDataModel.getUniqueSmiles());
                        //continue;
                    }
                }
            }
            return tmpFailedExportFragments;
        }
    }
    //
    /**
     * Exports the fragmentation results as they are displayed on the fragments tab as a CSV file.
     *
     * @param aList a list of FragmentDataModel instances to export
     * @param aSeparator the separator for the csv file
     * @return List {@literal <}String {@literal >} SMILES codes of the molecules that caused an error
     * @throws FileNotFoundException if given file cannot be found
     * @author Betül Sevindik
     */
    private List<String> createFragmentsTabCsvFile(File aCsvFile, List<MoleculeDataModel> aList, char aSeparator)
            throws FileNotFoundException {
        if (aCsvFile == null || aList == null) {
            return null;
        }
        List<String> tmpFailedExportFragments = new LinkedList<>();
        try (PrintWriter tmpWriter = new PrintWriter(aCsvFile.getPath())) {
            String tmpFragmentationCsvHeader = Message.get("Exporter.fragmentationTab.csvHeader.smiles") + aSeparator +
                    Message.get("Exporter.fragmentationTab.csvHeader.frequency") + aSeparator +
                    Message.get("Exporter.fragmentationTab.csvHeader.percentage") + aSeparator +
                    Message.get("Exporter.fragmentationTab.csvHeader.moleculeFrequency") + aSeparator +
                    Message.get("Exporter.fragmentationTab.csvHeader.moleculePercentage");
            tmpWriter.write(tmpFragmentationCsvHeader);
            for (MoleculeDataModel tmpDataModel : aList) {
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                try {
                    FragmentDataModel tmpFragmentDataModel = (FragmentDataModel) tmpDataModel;
                    tmpWriter.printf("%n%s%s%d%s%.4f%s%d%s%.4f",
                            tmpFragmentDataModel.getUniqueSmiles(), aSeparator,
                            tmpFragmentDataModel.getAbsoluteFrequency(), aSeparator,
                            tmpFragmentDataModel.getAbsolutePercentage(), aSeparator,
                            tmpFragmentDataModel.getMoleculeFrequency(), aSeparator,
                            tmpFragmentDataModel.getMoleculePercentage());
                } catch (Exception anException) {
                    Logger.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, String.format("%s molecule name: %s", anException.toString(), tmpDataModel.getName()), anException);
                    tmpFailedExportFragments.add(tmpDataModel.getUniqueSmiles());
                    //continue;
                }
            }
            return tmpFailedExportFragments;
        }
    }
    //
    /**
     * Exports the fragmentation results as they are displayed on the fragments tab as a PDF file. Opens a file chooser
     * dialog for the user to determine a directory and file for the exported data.
     *
     * @param aFragmentDataModelList a list of FragmentDataModel instances to export
     * @param aMoleculeDataModelListSize size of imported molecule list to display in the PDF document header
     * @param aFragmentationName fragmentation name to be displayed in the header of the PDF file
     * @param anImportedFileName name of the input file whose molecules were fragmented
     * @return List {@literal <}String {@literal >} SMILES codes of the molecules that caused an error
     * @throws FileNotFoundException if given file cannot be found
     * @throws DocumentException if something goes wrong writing the document
     * @author Betül Sevindik
     */
    private List<String> createFragmentsTabPdfFile(File aPdfFile,
                                                   List<MoleculeDataModel> aFragmentDataModelList,
                                                   int aMoleculeDataModelListSize,
                                                   String aFragmentationName,
                                                   String anImportedFileName) throws FileNotFoundException, DocumentException {
        if (aPdfFile == null || aFragmentDataModelList == null || aMoleculeDataModelListSize == 0 ||
                aFragmentationName == null || anImportedFileName == null) {
            return null;
        }
        try (Document tmpPDFDocument = new Document(PageSize.A4)) {
            List<String> tmpFailedExportFragments = new LinkedList<>();
            tmpPDFDocument.setPageSize(tmpPDFDocument.getPageSize().rotate());
            PdfWriter.getInstance(tmpPDFDocument, new FileOutputStream(aPdfFile.getPath()));
            tmpPDFDocument.open();
            float[] tmpCellLength = {70f, 120f, 50f, 50f, 55f, 55f}; // relative sizes, magic numbers
            PdfPTable tmpFragmentationTable = new PdfPTable(tmpCellLength);
            PdfPCell tmpSmilesStringCell = new PdfPCell(new Paragraph(Message.get("Exporter.fragmentationTab.pdfCellHeader.smiles"), Exporter.PDF_CELL_FONT));
            PdfPCell tmpFrequencyCell = new PdfPCell(new Paragraph(Message.get("Exporter.fragmentationTab.pdfCellHeader.frequency"), Exporter.PDF_CELL_FONT));
            PdfPCell tmpPercentageCell = new PdfPCell(new Paragraph(Message.get("Exporter.fragmentationTab.pdfCellHeader.percentage"), Exporter.PDF_CELL_FONT));
            PdfPCell tmpMolFrequencyCell = new PdfPCell(new Paragraph(Message.get("Exporter.fragmentationTab.pdfCellHeader.moleculeFrequency"), Exporter.PDF_CELL_FONT));
            PdfPCell tmpMolPercentageCell = new PdfPCell(new Paragraph(Message.get("Exporter.fragmentationTab.pdfCellHeader.moleculePercentage"), Exporter.PDF_CELL_FONT));
            PdfPCell tmpFragmentCell = new PdfPCell(new Paragraph(Message.get("Exporter.fragmentationTab.pdfCellHeader.fragment"), Exporter.PDF_CELL_FONT));
            Chunk tmpHeader = new Chunk(Message.get("Exporter.fragmentationTab.pdfCellHeader.header"),
                    FontFactory.getFont(FontFactory.TIMES_ROMAN, 18, Font.UNDERLINE));
            Paragraph tmpSpace = new Paragraph(" ");
            tmpFragmentationTable.addCell(tmpSmilesStringCell);
            tmpFragmentationTable.addCell(tmpFragmentCell);
            tmpFragmentationTable.addCell(tmpFrequencyCell);
            tmpFragmentationTable.addCell(tmpPercentageCell);
            tmpFragmentationTable.addCell(tmpMolFrequencyCell);
            tmpFragmentationTable.addCell(tmpMolPercentageCell);
            DecimalFormat tmpPercentageForm = new DecimalFormat("#.##%");
            for (MoleculeDataModel tmpModel : aFragmentDataModelList) {
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                FragmentDataModel tmpFragmentDataModel = (FragmentDataModel) tmpModel;
                int tmpAbsoluteFrequency = tmpFragmentDataModel.getAbsoluteFrequency();
                String tmpStringAbsoluteFrequency = String.format("%d", tmpAbsoluteFrequency);
                double tmpAbsolutePercentage = tmpFragmentDataModel.getAbsolutePercentage();
                int tmpMoleculeFrequency = tmpFragmentDataModel.getMoleculeFrequency();
                String tmpStringMoleculeFrequency = String.format("%d", tmpMoleculeFrequency);
                String tmpStringAbsolutePercentage = tmpPercentageForm.format(tmpAbsolutePercentage);
                double tmpMoleculePercentage = tmpFragmentDataModel.getMoleculePercentage();
                String tmpStringMoleculePercentage = tmpPercentageForm.format(tmpMoleculePercentage);
                //creates an image of the fragment
                PdfPCell tmpImageFragmentCell = new PdfPCell();
                tmpImageFragmentCell.setFixedHeight(85f);
                IAtomContainer tmpStructureOfFragment;
                try {
                    tmpStructureOfFragment = tmpFragmentDataModel.getAtomContainer();
                } catch (CDKException anException) {
                    Logger.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, String.format("%s molecule name: %s", anException.toString(), tmpFragmentDataModel.getName()), anException);
                    tmpFailedExportFragments.add(tmpFragmentDataModel.getUniqueSmiles());
                    continue;
                }
                //cannot be imported because com.lowagie.text.Image is already imported
                javafx.scene.image.Image tmpImageStructureOfFragment = DepictionUtil.depictImageWithZoom(tmpStructureOfFragment, 4.0);
                BufferedImage tmpBufferedImageFragment = SwingFXUtils.fromFXImage(tmpImageStructureOfFragment, null);
                Image tmpImageFragment = this.convertToITextImage(tmpBufferedImageFragment);
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
                tmpFragmentationTable.addCell(tmpFragmentDataModel.getUniqueSmiles());
                tmpFragmentationTable.addCell(tmpImageFragmentCell);
                tmpFragmentationTable.addCell(tmpCellOfFrequency);
                tmpFragmentationTable.addCell(tmpCellOfPercentage);
                tmpFragmentationTable.addCell(tmpCellOfMolFrequency);
                tmpFragmentationTable.addCell(tmpCellOfMolPercentage);
            }
            tmpPDFDocument.add(tmpHeader);
            tmpPDFDocument.add(tmpSpace);
            tmpPDFDocument.add(this.createHeaderTable(aFragmentDataModelList.size(), aMoleculeDataModelListSize, aFragmentationName, anImportedFileName));
            tmpPDFDocument.add(tmpSpace);
            tmpPDFDocument.add(tmpFragmentationTable);
            return tmpFailedExportFragments;
        }
    }
    //
    /**
     * Exports the fragmentation results as they are displayed on the itemization tab as a PDF file. Opens a file chooser
     * dialog for the user to determine a directory and file for the exported data.
     *
     * @param aFragmentDataModelListSize size of list of FragmentDataModel instances to export
     * @param aMoleculeDataModelList     a list MoleculeDataModel needed for the fragmentation report at the head of the exported document
     * @param aFragmentationName         fragmentation name to retrieve the specific set of fragments from the molecule data models
     * @param anImportedFileName name of the input file whose molecules were fragmented
     * @return List {@literal <}String {@literal >} SMILES codes of the molecules that caused an error
     * @throws FileNotFoundException if given file cannot be found
     * @throws DocumentException if something goes wrong writing the document
     * @author Betül Sevindik
     */
    private List<String> createItemizationTabPdfFile(File aPdfFile,
                                             int aFragmentDataModelListSize,
                                             ObservableList<MoleculeDataModel> aMoleculeDataModelList,
                                             String aFragmentationName,
                                             String anImportedFileName) throws FileNotFoundException, DocumentException {
        if (aPdfFile == null || aFragmentDataModelListSize == 0 ||
                aMoleculeDataModelList == null || aMoleculeDataModelList.isEmpty() ||
                aFragmentationName == null || aFragmentationName.isEmpty() ||
                anImportedFileName == null || anImportedFileName.isEmpty()) {
            return null;
        }
        try (Document tmpPDFDocument = new Document(PageSize.A4)) {
            List<String> tmpFailedExportFragments = new LinkedList<>();
            PdfWriter.getInstance(tmpPDFDocument, new FileOutputStream(aPdfFile.getPath()));
            tmpPDFDocument.open();
            // creates the pdf table
            Chunk tmpItemizationTabHeader = new Chunk(Message.get("Exporter.itemsTab.pdfCellHeader.header"),
                    FontFactory.getFont(FontFactory.TIMES_ROMAN, 18, Font.UNDERLINE));
            Paragraph tmpSpace = new Paragraph(" ");
            tmpPDFDocument.add(tmpItemizationTabHeader);
            tmpPDFDocument.add(tmpSpace);
            tmpPDFDocument.add(this.createHeaderTable(aFragmentDataModelListSize, aMoleculeDataModelList.size(), aFragmentationName, anImportedFileName));
            tmpPDFDocument.add(tmpSpace);
            for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                PdfPTable tmpTable = new PdfPTable(2);
                PdfPTable tmpFragmentTable = new PdfPTable(1);
                tmpTable.setWidths(new int[]{40, 80});
                PdfPCell tmpNameCell = new PdfPCell(new Paragraph(Message.get("Exporter.itemsTab.pdfCellHeader.name"), Exporter.PDF_CELL_FONT));
                tmpNameCell.setFixedHeight(55f);
                PdfPCell tmpStructureCell = new PdfPCell(new Paragraph(Message.get("Exporter.itemsTab.pdfCellHeader.structure"), Exporter.PDF_CELL_FONT));
                tmpStructureCell.setFixedHeight(120f);
                tmpTable.addCell(tmpNameCell);
                String tmpName = tmpMoleculeDataModel.getName();
                tmpTable.addCell(tmpName);
                tmpTable.addCell(tmpStructureCell);
                // Image of molecule
                IAtomContainer tmpMoleculeStructure;
                try {
                    tmpMoleculeStructure = tmpMoleculeDataModel.getAtomContainer();
                } catch (CDKException anException) {
                    Logger.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, String.format("%s molecule name: %s", anException.toString(), tmpMoleculeDataModel.getName()), anException);
                    tmpFailedExportFragments.add(tmpMoleculeDataModel.getUniqueSmiles());
                    continue;
                }
                PdfPCell tmpMoleculeStructureCell = new PdfPCell();
                tmpMoleculeStructureCell.setFixedHeight(120f);
                //cannot be imported because com.lowagie.text.Image is already imported
                javafx.scene.image.Image tmpMoleculeImage = DepictionUtil.depictImageWithZoom(tmpMoleculeStructure, 3.0);
                BufferedImage tmpBufferedImageOfMolecule = SwingFXUtils.fromFXImage(tmpMoleculeImage, null);
                Image tmpMolecule = this.convertToITextImage(tmpBufferedImageOfMolecule);
                tmpMoleculeStructureCell.addElement(tmpMolecule);
                tmpTable.addCell(tmpMoleculeStructureCell);
                PdfPCell tmpCellOfFragment = new PdfPCell(new Paragraph(Message.get("Exporter.itemsTab.pdfCellHeader.fragments"), Exporter.PDF_CELL_FONT));
                tmpCellOfFragment.setHorizontalAlignment(Element.ALIGN_CENTER);
                tmpFragmentTable.addCell(tmpCellOfFragment);
                tmpPDFDocument.add(tmpTable);
                tmpPDFDocument.add(tmpFragmentTable);
                if (!tmpMoleculeDataModel.hasMoleculeUndergoneSpecificFragmentation(aFragmentationName)) {
                    continue;
                }
                List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificFragmentation(aFragmentationName);
                int tmpFragmentsPerLine = 3; //magic number
                PdfPTable tmpFragmentationTable2 = new PdfPTable(tmpFragmentsPerLine);
                for (int tmpFragmentNumber = 0; tmpFragmentNumber < tmpFragmentList.size(); ) {
                    if (Thread.currentThread().isInterrupted()) {
                        return null;
                    }
                    ArrayList<PdfPCell> tmpCell = new ArrayList<>(3);
                    int tmpImagesNumbers = 0;
                    for (; tmpImagesNumbers < tmpFragmentsPerLine; tmpImagesNumbers++) {
                        if (Thread.currentThread().isInterrupted()) {
                            return null;
                        }
                        if (tmpFragmentNumber >= tmpFragmentList.size()) {
                            break;
                        }
                        FragmentDataModel tmpFragmentDatModel = tmpFragmentList.get(tmpFragmentNumber);
                        IAtomContainer tmpFragmentStructure;
                        try {
                            tmpFragmentStructure = tmpFragmentDatModel.getAtomContainer();
                        } catch (CDKException anException) {
                            Logger.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, String.format("%s molecule name: %s", anException.toString(), tmpMoleculeDataModel.getName()), anException);
                            tmpFailedExportFragments.add(tmpFragmentDatModel.getUniqueSmiles());
                            continue;
                        }
                        if (!tmpMoleculeDataModel.hasMoleculeUndergoneSpecificFragmentation(aFragmentationName)) {
                            continue;
                        }
                        String tmpFrequency = tmpMoleculeDataModel.getFragmentFrequencyOfSpecificFragmentation(aFragmentationName).get(tmpFragmentDatModel.getUniqueSmiles()).toString();
                        javafx.scene.image.Image tmpFragmentImage = DepictionUtil.depictImageWithText(
                                tmpFragmentStructure,
                                3.0,
                                BasicDefinitions.DEFAULT_IMAGE_WIDTH_DEFAULT,
                                BasicDefinitions.DEFAULT_IMAGE_HEIGHT_DEFAULT,
                                tmpFrequency);
                        BufferedImage tmpBufferedImageOfFragment = SwingFXUtils.fromFXImage(tmpFragmentImage, null);
                        Image tmpFragment = this.convertToITextImage(tmpBufferedImageOfFragment);
                        PdfPCell cell = new PdfPCell();
                        cell.addElement(tmpFragment);
                        tmpCell.add(cell);
                        tmpFragmentNumber++;
                    }
                    for (int tmpCellIterator = 0; tmpCellIterator < tmpFragmentsPerLine; tmpCellIterator++) {
                        if(Thread.currentThread().isInterrupted()){
                            return null;
                        }
                        if (tmpCellIterator < tmpImagesNumbers) {
                            tmpFragmentationTable2.addCell(tmpCell.get(tmpCellIterator));
                        } else {
                            tmpFragmentationTable2.addCell(new Paragraph(""));
                        }
                    }
                }
                tmpPDFDocument.add(tmpFragmentationTable2);
                tmpPDFDocument.newPage();
            }
            return tmpFailedExportFragments;
        }
    }
    //
    /**
     * Exports the chemical data of the given fragments as a single MDL SD file to the chosen
     * destination. Whether the fragments are written using the MDL V3000 format instead of the MDL V2000 format depends
     * on the current status of the alwaysMDLV3000FormatAtExportSetting of the instance's settingsContainer or whether a
     * fragment exceeds an atom count of 999 atoms. The option for writing aromatic bond types is enabled. If a fragment
     * could not be exported in the first place, a second attempt with a kekulized clone of the fragment's atom container
     * is made.
     * In case no 3D information are being held in a fragment atom container, the specific fragments are exported
     * using 2D information equally setting each z coordinate to 0. If no 2D information are available, it can be
     * chosen to either generate (pseudo-) 2D atom coordinates (originally intended for layout
     * purposes) or to export without specifying the atom coordinates (x, y, z = 0) via the last parameter.
     *
     * @param aFile                  File to save fragments
     * @param aFragmentDataModelList list of FragmentDataModel instances
     * @param generate2DCoordinates  boolean value whether to generate 2D coordinates
     * @param alwaysMDLV3000 whether to generate v3000 MOL/SD files as default
     * @return List {@literal <}String {@literal >} SMILES codes of the molecules that caused an error
     * @throws IOException if sth goes wrong
     * @author Samuel Behr
     */
    private List<String> createFragmentationTabSingleSDFile(File aFile,
                                                            List<MoleculeDataModel> aFragmentDataModelList,
                                                            boolean generate2DCoordinates,
                                                            boolean alwaysMDLV3000) throws IOException {
        if (aFile == null || aFragmentDataModelList == null) {
            return null;
        }
        List<String> tmpFailedExportFragments = new LinkedList<>();
        int tmpExportedFragmentsCounter = 0;
        int tmpFailedFragmentExportCounter = 0;
        try (
                PrintWriter tmpWriter = new PrintWriter(aFile.getPath());
                BufferedWriter tmpBufferedWriter = new BufferedWriter(tmpWriter);
                SDFWriter tmpSDFWriter = new SDFWriter(tmpBufferedWriter);
        ) {
            //specifying format of export
            //setting whether to always use MDL V3000 format
            tmpSDFWriter.setAlwaysV3000(alwaysMDLV3000);
            //accessing the WriteAromaticBondType setting
            try {
                tmpSDFWriter.getSetting(MDLV2000Writer.OptWriteAromaticBondTypes).setSetting("true");
            } catch (CDKException anException) {
                Exporter.LOGGER.log(Level.WARNING, "Exporting fragments with aromatic bond types not possible", anException);
            }
            //iterating through the fragments held by the list of fragments
            for (MoleculeDataModel tmpFragmentDataModel : aFragmentDataModelList) {
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                IAtomContainer tmpFragment;
                try {
                    tmpFragment = tmpFragmentDataModel.getAtomContainer();
                    IAtomContainer tmpFragmentClone = null;
                    boolean tmpPoint3dAvailable = ChemUtil.has3DCoordinates(tmpFragmentDataModel);
                    boolean tmpPoint2dAvailable = ChemUtil.has2DCoordinates(tmpFragmentDataModel);
                    if (!tmpPoint3dAvailable) {
                        tmpFragmentClone = this.handleFragmentWithNo3dInformationAvailable(tmpFragment,
                                tmpPoint2dAvailable, generate2DCoordinates);
                    } //else: given 3D info is used
                    //writing to file
                    try {
                        if (tmpPoint3dAvailable) {
                            tmpSDFWriter.write(tmpFragment);
                        } else {
                            tmpSDFWriter.write(tmpFragmentClone);
                        }
                        tmpExportedFragmentsCounter++;
                    } catch (CDKException anException) {
                        //retrying with a kekulized clone of the fragment - going to main catch block if sth goes wrong
                        if (tmpPoint3dAvailable) {
                            tmpFragmentClone = tmpFragment.clone();
                        }
                        Kekulization.kekulize(tmpFragmentClone);
                        tmpSDFWriter.write(tmpFragmentClone);
                        tmpExportedFragmentsCounter++;
                    }
                } catch (CDKException | CloneNotSupportedException anException) {
                    Exporter.LOGGER.log(Level.SEVERE, String.format("%s molecule name: %s", anException.toString(), tmpFragmentDataModel.getName()), anException);
                    tmpFailedExportFragments.add(tmpFragmentDataModel.getUniqueSmiles());
                    tmpFailedFragmentExportCounter++;
                    //continue;
                }
            }
            int finalTmpExportedFragmentsCounter = tmpExportedFragmentsCounter;
            int finalTmpFailedFragmentExportCounter = tmpFailedFragmentExportCounter;
            Exporter.LOGGER.log(Level.INFO, () -> String.format("Exported %d fragments as single SD file " +
                            "(export of %d fragments failed). File name: %s", finalTmpExportedFragmentsCounter,
                    finalTmpFailedFragmentExportCounter, aFile.getName()));
            return tmpFailedExportFragments;
        }
    }
    //
    /**
     * Exports the chemical data of the given fragments as separate MDL SD files to an
     * empty folder generated at the chosen path. The molecular formula of each fragment is used as name for each respective
     * file. Whether the fragments are written using the MDL V3000 format instead of the MDL V2000 format
     * depends on the current status of the alwaysMDLV3000FormatAtExportSetting of the instance's settingsContainer or
     * whether a fragment exceeds an atom count of 999 atoms. The option for writing aromatic bond types is enabled. If
     * a fragment could not be exported in the first place, a second attempt with a kekulized clone of the fragment's atom
     * container is made.
     * In case no 3D information are being held in a fragment atom container, the specific fragments are exported
     * using 2D information equally setting each z coordinate to 0. If no 2D information are available, it can be
     * chosen to either generate (pseudo-) 2D atom coordinates (originally intended for layout
     * purposes) or to export without specifying the atom coordinates (x, y, z = 0) via the last parameter.
     *
     * @param aDirectory             directory to save fragments
     * @param aFragmentDataModelList list of FragmentDataModel instances
     * @param generate2DCoordinates  boolean value whether to generate 2D coordinates
     * @param isAlwaysV3000MOLfile whether to use v3000 MOL files as default
     * @return List {@literal <}String {@literal >} SMILES codes of the molecules that caused an error
     * @throws IOException if sth goes wrong
     * @author Samuel Behr
     */
    private List<String> createFragmentationTabSeparateSDFiles(File aDirectory,
                                                               List<MoleculeDataModel> aFragmentDataModelList,
                                                               boolean generate2DCoordinates,
                                                               boolean isAlwaysV3000MOLfile) throws IOException {
        if (aDirectory == null || !aDirectory.isDirectory() || aFragmentDataModelList == null) {
            return null;
        }
        List<String> tmpFailedExportFragments = new LinkedList<>();
        String tmpSDFFilesDirectoryPathName = aDirectory
                + File.separator
                + Exporter.FRAGMENTS_EXPORT_DIRECTORY_NAME + "_" + FileUtil.getTimeStampFileNameExtension();
        // this is not how this FileUtil method should be used, but I'll allow it
        String tmpFinalSDFilesDirectoryPathName = FileUtil.getNonExistingFilePath(tmpSDFFilesDirectoryPathName, File.separator);
        File tmpSDFilesDirectory = Files.createDirectory(Paths.get(tmpFinalSDFilesDirectoryPathName)).toFile();
        int tmpExportedFragmentsCounter = 0;
        int tmpFailedFragmentExportCounter = 0;
        //iterating through the fragments held by the list of fragments
        for (MoleculeDataModel tmpFragmentDataModel : aFragmentDataModelList) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            IAtomContainer tmpFragment;
            try {
                tmpFragment = tmpFragmentDataModel.getAtomContainer();
                IAtomContainer tmpFragmentClone = null;
                boolean tmpPoint3dAvailable = ChemUtil.has3DCoordinates(tmpFragmentDataModel);
                boolean tmpPoint2dAvailable = ChemUtil.has2DCoordinates(tmpFragmentDataModel);
                //checking whether 3D information are available
                if (!tmpPoint3dAvailable) {
                    tmpFragmentClone = this.handleFragmentWithNo3dInformationAvailable(tmpFragment,
                            tmpPoint2dAvailable, generate2DCoordinates);
                } //else: given 3D info is used
                //generating file
                String tmpMolecularFormula = ChemUtil.generateMolecularFormula(tmpFragment);
                String tmpSDFilePathName = FileUtil.getNonExistingFilePath(tmpSDFilesDirectory
                        + File.separator + tmpMolecularFormula, ".sdf");
                File tmpSDFile = new File(tmpSDFilePathName);
                //writing to file
                try (
                        PrintWriter tmpWriter = new PrintWriter(tmpSDFile);
                        BufferedWriter tmpBufferedWriter = new BufferedWriter(tmpWriter);
                        SDFWriter tmpSDFWriter = new SDFWriter(tmpBufferedWriter);
                ) {
                    //specifying format of export
                    //setting whether to always use MDL V3000 format
                    tmpSDFWriter.setAlwaysV3000(isAlwaysV3000MOLfile);
                    //accessing the WriteAromaticBondType setting
                    try {
                        tmpSDFWriter.getSetting(MDLV2000Writer.OptWriteAromaticBondTypes).setSetting("true");
                    } catch (CDKException anException) {
                        Exporter.LOGGER.log(Level.WARNING, "Exporting fragments with aromatic bond types not possible", anException);
                    }
                    try {
                        if (tmpPoint3dAvailable) {
                            tmpSDFWriter.write(tmpFragment);
                        } else {
                            tmpSDFWriter.write(tmpFragmentClone);
                        }
                        tmpExportedFragmentsCounter++;
                    } catch (CDKException anException) {
                        //retrying with a kekulized clone of the fragment - going to main catch block if sth goes wrong
                        if (tmpPoint3dAvailable) {
                            tmpFragmentClone = tmpFragment.clone();
                        }
                        Kekulization.kekulize(tmpFragmentClone);
                        tmpSDFWriter.write(tmpFragmentClone);
                        tmpExportedFragmentsCounter++;
                    }
                }
            } catch (CDKException | CloneNotSupportedException anException) {
                Exporter.LOGGER.log(Level.SEVERE, String.format("%s molecule name: %s", anException.toString(), tmpFragmentDataModel.getName()), anException);
                tmpFailedExportFragments.add(tmpFragmentDataModel.getUniqueSmiles());
                tmpFailedFragmentExportCounter++;
                //continue;
            }
        }
        int finalTmpExportedFragmentsCounter = tmpExportedFragmentsCounter;
        int finalTmpFailedFragmentExportCounter = tmpFailedFragmentExportCounter;
        Exporter.LOGGER.log(Level.INFO, () -> String.format("Exported %d fragments as separate SD files " +
                        "(export of %d fragments failed). Folder name: %s", finalTmpExportedFragmentsCounter,
                finalTmpFailedFragmentExportCounter, tmpSDFilesDirectory.getName()));
        return tmpFailedExportFragments;
    }
    //
    /**
     * Opens a directory chooser and exports the chemical data of the given fragments as PDB files to an empty folder
     * generated at the chosen path. The molecular formula of each fragment is used as name for the associated file. In
     * case no 3D information are being held in a fragment atom container, the respective PDB files are created using 2D
     * information equally setting each z coordinate to 0. If no 2D information are available, it can be chosen to
     * either generate (pseudo-) 2D atom coordinates (originally intended for layout
     * purposes) or to export without specifying the atom coordinates (x, y, z = 0) via the last parameter.
     *
     * @param aDirectory             directory to save fragments
     * @param aFragmentDataModelList list of FragmentDataModel instances
     * @param generate2DCoordinates  boolean value whether to generate 2D coordinates
     * @return List {@literal <}String {@literal >} SMILES codes of the molecules that caused an error
     * @throws IOException if sth goes wrong
     * @author Samuel Behr
     */
    private List<String> createFragmentationTabPDBFiles(File aDirectory,
                                                        List<MoleculeDataModel> aFragmentDataModelList,
                                                        boolean generate2DCoordinates) throws IOException {
        if (aDirectory == null || !aDirectory.isDirectory() || aFragmentDataModelList == null) {
            return null;
        }
        List<String> tmpFailedExportFragments = new LinkedList<>();
        String tmpPDBFilesDirectoryPathName = aDirectory
                + File.separator
                + Exporter.FRAGMENTS_EXPORT_DIRECTORY_NAME + "_" + FileUtil.getTimeStampFileNameExtension();
        // this is not how this FileUtil method should be used, but I'll allow it
        String tmpFinalPDBFilesDirectoryPathName = FileUtil.getNonExistingFilePath(tmpPDBFilesDirectoryPathName, File.separator);
        File tmpPDBFilesDirectory = Files.createDirectory(Paths.get(tmpFinalPDBFilesDirectoryPathName)).toFile();
        int tmpExportedFragmentsCounter = 0;
        int tmpFailedFragmentExportCounter = 0;
        //iterating through the fragments held by the list of fragments
        for (MoleculeDataModel tmpFragmentDataModel : aFragmentDataModelList) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            IAtomContainer tmpFragment;
            try {
                tmpFragment = tmpFragmentDataModel.getAtomContainer();
                IAtomContainer tmpFragmentClone = null;
                boolean tmpPoint3dAvailable = ChemUtil.has3DCoordinates(tmpFragmentDataModel);
                boolean tmpPoint2dAvailable = ChemUtil.has2DCoordinates(tmpFragmentDataModel);
                //checking whether 3D information are available
                if (!tmpPoint3dAvailable) {
                    tmpFragmentClone = this.handleFragmentWithNo3dInformationAvailable(tmpFragment,
                            tmpPoint2dAvailable, generate2DCoordinates);
                } //else: given 3D info is used
                //generating file
                String tmpMolecularFormula = ChemUtil.generateMolecularFormula(tmpFragment);
                String tmpPDBFilePathName = FileUtil.getNonExistingFilePath(tmpPDBFilesDirectory
                        + File.separator + tmpMolecularFormula, ".pdb");
                File tmpPDBFile = new File(tmpPDBFilePathName);
                //writing to file
                try (
                        PDBWriter tmpPDBWriter = new PDBWriter(new FileOutputStream(tmpPDBFile));
                ) {
                    try {
                        if (tmpPoint3dAvailable) {
                            tmpPDBWriter.writeMolecule(tmpFragment);
                        } else {
                            tmpPDBWriter.writeMolecule(tmpFragmentClone);
                        }
                        tmpExportedFragmentsCounter++;
                    } catch (CDKException anException) {
                        //retrying with a kekulized clone of the fragment - going to main catch block if sth goes wrong
                        if (tmpPoint3dAvailable) {
                            tmpFragmentClone = tmpFragment.clone();
                        }
                        Kekulization.kekulize(tmpFragmentClone);
                        tmpPDBWriter.write(tmpFragmentClone);
                        tmpExportedFragmentsCounter++;
                    }
                }
            } catch (CDKException | CloneNotSupportedException anException) {
                Exporter.LOGGER.log(Level.SEVERE, String.format("%s molecule name: %s", anException.toString(), tmpFragmentDataModel.getName()), anException);
                tmpFailedExportFragments.add(tmpFragmentDataModel.getUniqueSmiles());
                tmpFailedFragmentExportCounter++;
                //continue;
            }
        }
        int finalTmpFailedFragmentExportCounter = tmpFailedFragmentExportCounter;
        int finalTmpExportedFragmentsCounter = tmpExportedFragmentsCounter;
        Exporter.LOGGER.log(Level.INFO, () -> String.format("Exported %d fragments as PDB files " +
                        "(export of %d fragments failed). Folder name: %s", finalTmpExportedFragmentsCounter,
                finalTmpFailedFragmentExportCounter, tmpPDBFilesDirectory.getName()));
        return tmpFailedExportFragments;
    }
    //
    /**
     * Converts a buffered image into a com.lowagie.text.Image (necessary for pdf export with iText) and returns it.
     *
     * @param aBufferedImage buffered image to be converted
     * @return com.lowagie.text.Image
     */
    private Image convertToITextImage(BufferedImage aBufferedImage) {
        try {
            ByteArrayOutputStream tmpByteArrayOS = new ByteArrayOutputStream();
            ImageIO.write(aBufferedImage, "png", tmpByteArrayOS);
            byte[] bytes = tmpByteArrayOS.toByteArray();
            return Image.getInstance(bytes);
        } catch (IOException anException) {
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }
    //
    /**
     * Creates a header with general information for the PDf files.
     *
     * @param aFragmentDataModelListSize size of list of fragments
     * @param aMoleculeDataModelListSize size of list of molecules
     * @param aFragmentationName name of the fragmentation task to display in the header
     * @param anImportedFileName name of the input file whose molecules were fragmented
     * @return fragmentation report table for a PDF file header
     * @author Betül Sevindik
     */
    private PdfPTable createHeaderTable(
            int aFragmentDataModelListSize,
            int aMoleculeDataModelListSize,
            String aFragmentationName,
            String anImportedFileName) {
        //creates the header
        float[] tmpCellLengthIntro = {60f, 60f}; // relative sizes
        PdfPTable tmpTableIntro = new PdfPTable(tmpCellLengthIntro);
        PdfPCell tmpIntroCell1 = new PdfPCell(new Paragraph(Message.get("Exporter.pdfHeader.fragmentationName"), Exporter.PDF_CELL_FONT));
        PdfPCell tmpIntroCell2 = new PdfPCell(new Paragraph(aFragmentationName));
        PdfPCell tmpIntroCell3 = new PdfPCell(new Paragraph(Message.get("Exporter.pdfHeader.numberOfMolecules"), Exporter.PDF_CELL_FONT));
        PdfPCell tmpIntroCell4 = new PdfPCell(new Paragraph(String.valueOf(aMoleculeDataModelListSize)));
        PdfPCell tmpIntroCell5 = new PdfPCell(new Paragraph(Message.get("Exporter.pdfHeader.numberOfFragments"), Exporter.PDF_CELL_FONT));
        PdfPCell tmpIntroCell6 = new PdfPCell(new Paragraph(String.valueOf(aFragmentDataModelListSize)));
        PdfPCell tmpIntroCell7 = new PdfPCell(new Paragraph(Message.get("Exporter.pdfHeader.fileName"), Exporter.PDF_CELL_FONT));
        PdfPCell tmpIntroCell8 = new PdfPCell(new Paragraph(anImportedFileName));
        PdfPCell tmpIntroCell9 = new PdfPCell(new Paragraph(Message.get("Exporter.pdfHeader.timeStamp"), Exporter.PDF_CELL_FONT));
        PdfPCell tmpIntroCell10 = new PdfPCell(new Paragraph(MiscUtil.getTimestampInStandardFormat()));
        tmpTableIntro.addCell(tmpIntroCell1);
        tmpTableIntro.addCell(tmpIntroCell2);
        tmpTableIntro.addCell(tmpIntroCell3);
        tmpTableIntro.addCell(tmpIntroCell4);
        tmpTableIntro.addCell(tmpIntroCell5);
        tmpTableIntro.addCell(tmpIntroCell6);
        tmpTableIntro.addCell(tmpIntroCell7);
        tmpTableIntro.addCell(tmpIntroCell8);
        tmpTableIntro.addCell(tmpIntroCell9);
        tmpTableIntro.addCell(tmpIntroCell10);
        return tmpTableIntro;
    }
    //
    /**
     * Opens a FileChooser to be able to save a file. Returns null if the user cancelled the dialog.
     *
     * @param aParentStage Stage where the FileChooser should be shown
     * @param aDescription file type description to be used in the dialog (not the file extension)
     * @param anExtension  file extension for extension filter of the file chooser dialog
     * @param aFileName    initial file name to suggest to the user in the dialog
     * @return the selected file or null if no file has been selected
     * @throws NullPointerException if the given stage is null
     * @author Betül Sevindik
     */
    private File chooseFile(Stage aParentStage, String aDescription, String anExtension, String aFileName) throws NullPointerException {
        Objects.requireNonNull(aParentStage, "aParentStage (instance of Stage) is null");
        FileChooser tmpFileChooser = new FileChooser();
        tmpFileChooser.setTitle((Message.get("Exporter.fileChooser.title")));
        FileChooser.ExtensionFilter tmpExtensionFilter2 = new FileChooser.ExtensionFilter(aDescription, anExtension);
        tmpFileChooser.getExtensionFilters().addAll(tmpExtensionFilter2);
        tmpFileChooser.setInitialFileName(aFileName);
        tmpFileChooser.setInitialDirectory(new File(this.settingsContainer.getRecentDirectoryPathSetting()));
        File tmpFile = tmpFileChooser.showSaveDialog(aParentStage);
        if (tmpFile != null) {
            this.settingsContainer.setRecentDirectoryPathSetting(tmpFile.getParent() + File.separator);
        }
        return tmpFile;
    }
    //
    /**
     * Opens a DirectoryChooser to choose a directory. Returns null if no directory has been selected.
     *
     * @param aParentStage Stage to show the DirectoryChooser
     * @return the selected directory or null if no directory has been selected
     * @throws NullPointerException if the given stage is null
     * @author Samuel Behr
     */
    private File chooseDirectory(Stage aParentStage) throws NullPointerException {
        Objects.requireNonNull(aParentStage, "aParentStage (instance of Stage) is null");
        DirectoryChooser tmpDirectoryChooser = new DirectoryChooser();
        tmpDirectoryChooser.setTitle(Message.get("Exporter.directoryChooser.title"));
        File tmpRecentDirectory = new File(this.settingsContainer.getRecentDirectoryPathSetting());
        if (!tmpRecentDirectory.isDirectory()) {
            tmpRecentDirectory = new File(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
            this.settingsContainer.setRecentDirectoryPathSetting(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
            Exporter.LOGGER.log(Level.INFO, "Recent directory could not be read, resetting to default.");
        }
        tmpDirectoryChooser.setInitialDirectory(tmpRecentDirectory);
        File tmpDirectory = tmpDirectoryChooser.showDialog(aParentStage);
        if (tmpDirectory != null) {
            this.settingsContainer.setRecentDirectoryPathSetting(tmpDirectory.getPath());
        }
        return tmpDirectory;
    }
    //
    /**
     * Optionally completes 2D coordinates of a given fragment by setting all z-coordinates to 0 or generates new
     * pseudo-3D-coordinates for it using a structure diagram generator. As a third option, all coordinates of the given
     * atoms can be set to 0 (or if an exception occurs at coordinate generation). Which option is applied depends on the
     * given parameters. Initially, the given fragment is cloned but if this fails, the original, given atom container
     * is processed and the exception logged.
     *
     * @param aFragment                  atom container of a fragment to handle
     * @param aPoint2dAvailable          whether 2D atom coordinates are available; this is not checked by this method!
     * @param aGenerate2dAtomCoordinates whether 2D atom coordinates should be generated (if the first parameter is true,
     *                                   this parameter does not matter)
     * @return a clone of the given fragment with 3D atom coordinates created according to the given parameters
     * @author Samuel Behr
     */
    private IAtomContainer handleFragmentWithNo3dInformationAvailable(
            IAtomContainer aFragment,
            boolean aPoint2dAvailable,
            boolean aGenerate2dAtomCoordinates) {
        //generating a clone of the fragment
        IAtomContainer tmpFragmentClone;
        try {
            tmpFragmentClone = aFragment.clone();
        } catch (CloneNotSupportedException anException) {
            tmpFragmentClone = aFragment;
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
        }
        //generating 2D atom coordinates if needed
        boolean tmpErrorAtGenerating2dAtomCoordinates = false;
        if (!aPoint2dAvailable && aGenerate2dAtomCoordinates) {
            //2D coords are not available but they should be generated
            try {
                ChemUtil.generate2DCoordinates(tmpFragmentClone);
            } catch (CDKException anException) {
                Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                tmpErrorAtGenerating2dAtomCoordinates = true;
            }
        }
        if (aPoint2dAvailable || (aGenerate2dAtomCoordinates && !tmpErrorAtGenerating2dAtomCoordinates)) {
            //2D coords are available or were successfully generated
            //transfer of 2D coordinates to 3D coordinates with z = 0
            ChemUtil.generatePseudo3Dfrom2DCoordinates(tmpFragmentClone);
        } else {
            //2D coords are not available and should not be generated or there was an error doing this
            //setting all atom coordinates to 0
            ChemUtil.generateZero3DCoordinates(tmpFragmentClone);
        }
        return tmpFragmentClone;
    }
    //</editor-fold>
}
