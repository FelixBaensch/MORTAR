/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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
import de.unijena.cheminf.mortar.controller.TabNames;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.LogUtil;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Writer;
import org.openscience.cdk.io.PDBWriter;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.layout.StructureDiagramGenerator;

import javax.imageio.ImageIO;
import javax.vecmath.Point3d;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exporter
 *
 * @author Betül Sevindik, Samuel Behr, Felix Bänsch, Jonas Schaub
 */
public class Exporter {
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * Logger of this class
     */
    private static final Logger LOGGER = Logger.getLogger(Exporter.class.getName());

    /**
     * Name of directory generated for exporting a stream of fragment files
     */
    private static final String FRAGMENTS_EXPORT_DIRECTORY_NAME = "MORTAR_Fragments_Export";

    /**
     * Font for cells in exported PDF files
     */
    private final Font fontFactory =  FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD);
    //</editor-fold>
    //
    // <editor-fold defaultstate="collapsed" desc="Private variables">
    /**
     * Container for general settings for managing, preserving, and reloading application settings
     */
    private SettingsContainer settingsContainer;
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
        String tmpRecentDirFromContainer = this.settingsContainer.getRecentDirectoryPathSetting();
        if(tmpRecentDirFromContainer == null || tmpRecentDirFromContainer.isEmpty()) {
            this.settingsContainer.setRecentDirectoryPathSetting(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public methods" defaultstate="collapsed">
    /**
     * Exports in a new thread depending on aTabName the fragmentation results as displayed on the Itemisation tab or
     * on the Fragments tab as a CSV file. Opens a file chooser dialog for the user to determine a directory and file
     * for the exported data.
     *
     * @param aParentStage stage to show the FileChooser
     * @param aMoleculeDataModelList a list of MoleculeDataModel instances to export along with their fragments
     * @param aFragmentationName fragmentation name to retrieve the specific set of fragments from the molecule data models
     * @param aSeparator the separator for the csv file
     * @param aTabName TabName to identify type of tab
     */
    public void exportCsvFile(Stage aParentStage, List<MoleculeDataModel> aMoleculeDataModelList, String aFragmentationName, String aSeparator, TabNames aTabName){
        try{
            File tmpCsvFile = null;
            String tmpFileName = "";
            if(aTabName.name().equals(TabNames.Fragments.name())){
                tmpFileName = "Fragments " + aFragmentationName;
            }else if (aTabName.name().equals(TabNames.Itemization.name())){
                tmpFileName = "Items  " + aFragmentationName;
            }
            tmpCsvFile = this.saveFile(aParentStage, "CSV", "*.csv", tmpFileName);
            if(tmpCsvFile == null)
                return;
            File tmpFinalCsvFile = tmpCsvFile;
            Task<Void> tmpTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if(aTabName.equals(TabNames.Fragments)){
                        //can throw FileNotFoundException, gets handled in setOnFailed()
                        createFragmentationTabCsvFile(tmpFinalCsvFile, aMoleculeDataModelList, aSeparator);
                    }else if(aTabName.equals(TabNames.Itemization)){
                        //can throw FileNotFoundException, gets handled in setOnFailed()
                        createItemizationTabCsvFile(tmpFinalCsvFile, aMoleculeDataModelList, aFragmentationName, aSeparator);
                    }
                    return null;
                }
            };
            tmpTask.setOnFailed(workerStateEvent -> {
                Thread tmpThread = Thread.currentThread();
                LogUtil.getUncaughtExceptionHandler().uncaughtException(tmpThread,
                        workerStateEvent.getSource().getException());
            });
            Thread tmpThread = new Thread(tmpTask);
            tmpThread.setDaemon(false);
            tmpThread.setUncaughtExceptionHandler(LogUtil.getUncaughtExceptionHandler());
            tmpThread.start();
        } catch(Exception anException){
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
        }
    }
    //
    /**
     * Exports in a new thread depending on aTabName the fragmentation results as displayed on the Itemisation tab or on the Fragments tab as a CSV file. Opens a file chooser
     * dialog for the user to determine a directory and file for the exported data.
     *
     * @param aParentStage stage to show the FileChooser
     * @param aFragmentDataModelList a list of FragmentDataModel instances to export
     * @param aMoleculeDataModelList a list MoleculeDataModel needed for the fragmentation report at the head of the exported document
     * @param aFragmentationName fragmentation name to be displayed in the header of the PDF file
     * @param aTabName TabName to identify type of tab
     */
    public void exportPdfFile(Stage aParentStage, List<MoleculeDataModel> aFragmentDataModelList, ObservableList<MoleculeDataModel> aMoleculeDataModelList, String aFragmentationName, TabNames aTabName){
        try{
            File tmpCsvFile = null;
            String tmpFileName = "";
            if(aTabName.name().equals(TabNames.Fragments.name())){
                tmpFileName = "Fragments " + aFragmentationName;
            }else if (aTabName.name().equals(TabNames.Itemization.name())){
                tmpFileName = "Items  " + aFragmentationName;
            }
            tmpCsvFile = this.saveFile(aParentStage, "PDF", "*.pdf", tmpFileName);
            if(tmpCsvFile == null)
                return;
            File tmpFinalCsvFile = tmpCsvFile;
            Task<Void> tmpTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if(aTabName.equals(TabNames.Fragments)){
                        //throws FileNotFoundException, gets handled in setOnFailed()
                        createFragmentationTabPdfFile(tmpFinalCsvFile, aFragmentDataModelList, aMoleculeDataModelList, aFragmentationName);
                    }else if(aTabName.equals(TabNames.Itemization)){
                        //throws FileNotFoundException, gets handled in setOnFailed()
                        createItemizationTabPdfFile(tmpFinalCsvFile, aFragmentDataModelList.size(), aMoleculeDataModelList, aFragmentationName);
                    }
                    return null;
                }
            };
            tmpTask.setOnFailed(workerStateEvent -> {
                Thread tmpThread = Thread.currentThread();
                LogUtil.getUncaughtExceptionHandler().uncaughtException(tmpThread,
                        workerStateEvent.getSource().getException());
            });
            Thread tmpThread = new Thread(tmpTask);
            tmpThread.setDaemon(false);
            tmpThread.setUncaughtExceptionHandler(LogUtil.getUncaughtExceptionHandler());
            tmpThread.start();
        } catch(Exception anException){
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
        }
    }
    //
    /**
     *
     * @param aParentStage stage to show the FileChooser
     * @param aFragmentDataModelList list of FragmentDataModel instances
     * @param aFragmentationName name of fragmentation
     */
    public void exportFragmentsAsChemicalFile(Stage aParentStage, List<MoleculeDataModel> aFragmentDataModelList, String aFragmentationName, ChemFileTypes aChemFileType){
        this.exportFragmentsAsChemicalFile(aParentStage, aFragmentDataModelList, aFragmentationName, aChemFileType, false);
    }
    /**
     *
     * @param aParentStage stage to show the FileChooser
     * @param aFragmentDataModelList list of FragmentDataModel instances
     * @param aFragmentationName name of fragmentation
     * @param isSingleExport boolean if fragments should be exported in one file or seperated, one file each fragment
     */
    public void exportFragmentsAsChemicalFile(Stage aParentStage, List<MoleculeDataModel> aFragmentDataModelList, String aFragmentationName, ChemFileTypes aChemFileType , boolean isSingleExport){
        if(aFragmentDataModelList == null || aFragmentDataModelList.size() == 0){
            GuiUtil.guiMessageAlert(
                    Alert.AlertType.INFORMATION,
                    Message.get("Exporter.MessageAlert.NoDataAvailable.title"),
                    Message.get("Exporter.MessageAlert.NoDataAvailable.header"),
                    null
            );
            return;
        }
        try{
            File tmpFile = null;
            String tmpFileName = "Fragments Export " + aFragmentationName;
            if(aChemFileType == ChemFileTypes.SDF && isSingleExport){
                tmpFile = this.saveFile(aParentStage, "SD-File", "*.sdf", tmpFileName);
            } else if(aChemFileType == ChemFileTypes.SDF || aChemFileType == ChemFileTypes.PDB){
                tmpFile = this.chooseDirectory(aParentStage);
            }
            if(tmpFile == null)
                return;
            File tmpFileFinal = tmpFile;
            boolean tmpGenerate2dAtomCoordinates = false;
            if(!ChemUtil.checkMoleculeListForCoordinates(aFragmentDataModelList)){
                ButtonType tmpConformationResult = GuiUtil.guiConformationAlert(
                        Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.title"),
                        Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.header"),
                        Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.text")
                );
                if (tmpConformationResult == ButtonType.OK) {
                    tmpGenerate2dAtomCoordinates = true;
                }
            }
            boolean tmpGenerate2dAtomCoordinatesFinal = tmpGenerate2dAtomCoordinates;
            Task<List<String>> tmpTask = new Task<>() {
                @Override
                protected List<String> call() throws Exception {
                    if (aChemFileType == ChemFileTypes.SDF && isSingleExport) {
                        return createFragmentationTabSingleSDFile(tmpFileFinal, aFragmentDataModelList, tmpGenerate2dAtomCoordinatesFinal);
                    } else if(aChemFileType == ChemFileTypes.SDF) {
                        return createFragmentationTabSeparateSDFiles(tmpFileFinal, aFragmentDataModelList, tmpGenerate2dAtomCoordinatesFinal);
                    } else if(aChemFileType == ChemFileTypes.PDB){
                        return createFragmentationTabPDBFiles(tmpFileFinal, aFragmentDataModelList, tmpGenerate2dAtomCoordinatesFinal);
                    }
                    return null;
                }
            };
            tmpTask.setOnSucceeded(event -> {
                List<String> tmpFailedExportFragments = tmpTask.getValue();
                if(tmpFailedExportFragments == null){
                    GuiUtil.guiMessageAlert(Alert.AlertType.WARNING,
                            Message.get("Exporter.FragmentsTab.ExportNotPossible.title"),
                            Message.get("Exporter.FragmentsTab.ExportNotPossible.header"),
                            null);
                }
                if(tmpFailedExportFragments.size() > 0){
                    StringBuilder tmpStringBuilder = new StringBuilder();
                    for(String tmpFragmentName : tmpFailedExportFragments){
                        tmpStringBuilder.append(tmpFragmentName + "\n");
                    }
                    GuiUtil.guiExpandableAlert(
                            Alert.AlertType.WARNING.toString(),
                            Message.get("Exporter.FragmentsTab.ExportNotPossible.title"),
                            Message.get("Exporter.FragmentsTab.ExportNotPossible.header"),
                            tmpFailedExportFragments.size() + " " + Message.get("Exporter.FragmentsTab.ExportNotPossible.label"),
                            tmpStringBuilder.toString()
                    );
                }
            });
            tmpTask.setOnFailed(event -> {
                Exporter.LOGGER.log(Level.WARNING, event.getSource().getException().toString(), event.getSource().getException());
                GuiUtil.guiMessageAlert(
                        Alert.AlertType.WARNING,
                        Message.get("Exporter.FragmentsTab.ExportNotPossible.title"),
                        Message.get("Exporter.FragmentsTab.ExportNotPossible.header"),
                        null);
            });
            tmpTask.setOnCancelled(event ->
                GuiUtil.guiMessageAlert(
                        Alert.AlertType.WARNING,
                        Message.get("Exporter.FragmentsTab.ExportNotPossible.title"),
                        Message.get("Exporter.FragmentsTab.ExportNotPossible.header"),
                        null)
            );
            Thread tmpThread = new Thread(tmpTask);
            tmpThread.setDaemon(false);
            tmpThread.setUncaughtExceptionHandler(LogUtil.getUncaughtExceptionHandler());
            tmpThread.start();
        } catch(Exception anException){
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    //TODO: Parameter tests necessary?
    //TODO: Move literals like header texts to properties file or constants?
    /**
     * Exports the fragmentation results as they are displayed on the itemization tab as a CSV file.
     *
     * @param aMoleculeDataModelList a list of MoleculeDataModel instances to export along with their fragments
     * @param aFragmentationName fragmentation name to retrieve the specific set of fragments from the molecule data models
     * @param aSeparator the separator for the csv file
     * @throws FileNotFoundException
     * @author Betül Sevindik
     */
    private void createItemizationTabCsvFile(File aCsvFile,
                                             List<MoleculeDataModel> aMoleculeDataModelList,
                                             String aFragmentationName,
                                             String aSeparator)
                                             throws FileNotFoundException {
        if (aCsvFile != null) {
            PrintWriter tmpWriter = new PrintWriter(aCsvFile.getPath());
            StringBuilder tmpCsvHeader = new StringBuilder();
            tmpCsvHeader.append("MoleculeName" + aSeparator + "SmilesOfStructure" + aSeparator + "SmilesOfFragments and frequency\n");
            tmpWriter.write(tmpCsvHeader.toString());
            for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
                tmpWriter.printf("%s" + aSeparator + "%s", tmpMoleculeDataModel.getName(), tmpMoleculeDataModel.getUniqueSmiles());
                List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificAlgorithm(aFragmentationName);
                for (FragmentDataModel tmpFragmentDataModel : tmpFragmentList) {
                    tmpWriter.append(aSeparator);
                    tmpWriter.printf("%s" + aSeparator + "%s", tmpFragmentDataModel.getUniqueSmiles(), tmpMoleculeDataModel.getFragmentFrequencyOfSpecificAlgorithm(aFragmentationName).get(tmpFragmentDataModel.getUniqueSmiles()).toString());
                }
                tmpWriter.append("\n");
            }
            tmpWriter.close();
        } else {
            return;
        }
    }
    //
    //TODO: Parameter tests necessary?
    //TODO: Move literals like header texts to properties file or constants?
    /**
     * Exports the fragmentation results as they are displayed on the fragments tab as a CSV file.
     *
     * @param aList a list of FragmentDataModel instances to export
     * @param aSeparator the separator for the csv file
     * @throws FileNotFoundException
     * @author Betül Sevindik
     */
    private void createFragmentationTabCsvFile(File aCsvFile, List<MoleculeDataModel> aList, String aSeparator)
            throws FileNotFoundException{
        if (aCsvFile != null) {
            PrintWriter tmpWriter = new PrintWriter(aCsvFile.getPath());
            StringBuilder tmpFragmentationCsvHeader = new StringBuilder();
            tmpFragmentationCsvHeader.append("SmilesString" + aSeparator + "Frequency" + aSeparator + "Percentage"
                    + aSeparator + "MolecularFrequency"
                    + aSeparator + "MolecularPercentage\n");
            tmpWriter.write(tmpFragmentationCsvHeader.toString());
            for (MoleculeDataModel tmpDataModel : aList) {
                FragmentDataModel tmpFragmentDataModel = (FragmentDataModel)tmpDataModel;
                tmpWriter.printf("%s" + aSeparator + "%d" + aSeparator + "%.3f" + aSeparator + "%d" + aSeparator + "%.2f\n",
                        tmpFragmentDataModel.getUniqueSmiles(), tmpFragmentDataModel.getAbsoluteFrequency(),
                        tmpFragmentDataModel.getAbsolutePercentage(), tmpFragmentDataModel.getMoleculeFrequency(),
                        tmpFragmentDataModel.getMoleculePercentage());
            }
            tmpWriter.close();

        } else {
            return;
        }
    }
    //
    //TODO: Improve exception handling so that it does not kill the whole export if one fragment is faulty
    //TODO: See tasks and issues document for further to dos concerning the PDF export
    /**
     * Exports the fragmentation results as they are displayed on the fragments tab as a PDF file. Opens a file chooser
     * dialog for the user to determine a directory and file for the exported data.
     *
     * @param aFragmentDataModelList a list of FragmentDataModel instances to export
     * @param aMoleculeDataModelList a list MoleculeDataModel needed for the fragmentation report at the head of the exported document
     * @param aFragmentationName fragmentation name to be displayed in the header of the PDF file
     * @return PDF file which contains the results of the fragmentation
     * @throws FileNotFoundException
     * @throws DocumentException
     * @author Betül Sevindik
     */
    private void createFragmentationTabPdfFile(File aPdfFile,
                                               List<MoleculeDataModel> aFragmentDataModelList,
                                               ObservableList<MoleculeDataModel> aMoleculeDataModelList,
                                               String aFragmentationName) throws FileNotFoundException, DocumentException {
        if (aPdfFile != null) {
            Document tmpDocument = new Document(PageSize.A4);
            tmpDocument.setPageSize(tmpDocument.getPageSize().rotate());
            PdfWriter.getInstance(tmpDocument, new FileOutputStream(aPdfFile.getPath()));
            tmpDocument.open();
            float tmpCellLength[] = {70f, 120f, 50f, 50f, 55f, 55f}; // relative sizes
            PdfPTable tmpFragmentationTable = new PdfPTable(tmpCellLength);
            PdfPCell tmpSmilesStringCell = new PdfPCell(new Paragraph("Smiles String ", fontFactory));
            PdfPCell tmpFrequencyCell = new PdfPCell(new Paragraph("Frequency", this.fontFactory));
            PdfPCell tmpPercentageCell = new PdfPCell(new Paragraph("Percentage", this.fontFactory));
            PdfPCell tmpMolFrequencyCell = new PdfPCell(new Paragraph("Molecule-frequency", this.fontFactory));
            PdfPCell tmpMolPercentageCell = new PdfPCell(new Paragraph(" Molecule-percentage", this.fontFactory));
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
            for (MoleculeDataModel tmpModel : aFragmentDataModelList) {
                FragmentDataModel tmpFragmentDataModel = (FragmentDataModel) tmpModel;
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
                IAtomContainer tmpStructureOfFragment;
                try{
                    tmpStructureOfFragment = tmpFragmentDataModel.getAtomContainer();
                } catch(CDKException anException){
                    Exporter.LOGGER.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, anException.toString() + "_" + tmpFragmentDataModel.getName(), anException);
                    continue;
                }
                javafx.scene.image.Image tmpImageStructureOfFragment = DepictionUtil.depictImageWithZoom(tmpStructureOfFragment,
                        4.0);
                BufferedImage tmpBufferedImageFragment = SwingFXUtils.fromFXImage(tmpImageStructureOfFragment, null);
                Image tmpImageFragment = this.getITextImage(tmpBufferedImageFragment);
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
            }
            tmpDocument.add(tmpHeader);
            tmpDocument.add(tmpSpace);
            tmpDocument.add(this.createHeaderTable(aFragmentDataModelList.size(), aMoleculeDataModelList.size(), aFragmentationName));
            tmpDocument.add(tmpSpace);
            tmpDocument.add(tmpFragmentationTable);
            tmpDocument.close();
        } else {
            return;
        }
    }
    //
    //TODO: Improve exception handling so that it does not kill the whole export if one fragment is faulty
    //TODO: See tasks and issues document for further to dos concerning the PDF export
    /**
     * Exports the fragmentation results as they are displayed on the itemization tab as a PDF file. Opens a file chooser
     * dialog for the user to determine a directory and file for the exported data.
     *
     * @param aFragmentDataModelListSize size of list of FragmentDataModel instances to export
     * @param aMoleculeDataModelList a list MoleculeDataModel needed for the fragmentation report at the head of the exported document
     * @param aFragmentationName fragmentation name to retrieve the specific set of fragments from the molecule data models
     * @return PDF file with the data that appears on the itemization tab
     * @throws FileNotFoundException
     * @throws DocumentException
     * @author Betül Sevindik
     */
    private void createItemizationTabPdfFile(File aPdfFile,
                                             int aFragmentDataModelListSize,
                                             ObservableList<MoleculeDataModel> aMoleculeDataModelList,
                                             String aFragmentationName) throws FileNotFoundException, DocumentException {
        if (aPdfFile != null) {
            Document tmpDocument = new Document(PageSize.A4);
            PdfWriter.getInstance(tmpDocument, new FileOutputStream(aPdfFile.getPath()));
            tmpDocument.open();
            // creates the pdf table
            Chunk tmpItemizationTabHeader = new Chunk("Export of the Itemization tab",
                    FontFactory.getFont(FontFactory.TIMES_ROMAN, 18, Font.UNDERLINE));
            Paragraph tmpSpace = new Paragraph(" ");
            tmpDocument.add(tmpItemizationTabHeader);
            tmpDocument.add(tmpSpace);
            tmpDocument.add(this.createHeaderTable(aFragmentDataModelListSize, aMoleculeDataModelList.size(), aFragmentationName));
            tmpDocument.add(tmpSpace);
            for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
                PdfPTable tmpTable = new PdfPTable(2);
                PdfPTable tmpFragmentTable = new PdfPTable(1);
                tmpTable.setWidths(new int[]{40, 80});
                PdfPCell tmpNameCell = new PdfPCell(new Paragraph("Name", this.fontFactory));
                tmpNameCell.setFixedHeight(55f);
                PdfPCell tmpStructureCell = new PdfPCell(new Paragraph("Structure", this.fontFactory));
                tmpStructureCell.setFixedHeight(120f);
                tmpTable.addCell(tmpNameCell);
                String tmpName = tmpMoleculeDataModel.getName();
                tmpTable.addCell(tmpName);
                tmpTable.addCell(tmpStructureCell);
                // Image of molecule
                IAtomContainer tmpMoleculeStructure;
                try{
                    tmpMoleculeStructure = tmpMoleculeDataModel.getAtomContainer();
                } catch(CDKException anException){
                    Exporter.LOGGER.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, anException.toString() + "_" + tmpMoleculeDataModel.getName(), anException);
                    continue;
                }
                PdfPCell tmpMoleculeStructureCell = new PdfPCell();
                tmpMoleculeStructureCell.setFixedHeight(120f);
                javafx.scene.image.Image tmpMoleculeImage = DepictionUtil.depictImageWithZoom(tmpMoleculeStructure,
                        3.0);
                BufferedImage tmpBufferedImageOfMolecule = SwingFXUtils.fromFXImage(tmpMoleculeImage, null);
                Image tmpMolecule = this.getITextImage(tmpBufferedImageOfMolecule);
                tmpMoleculeStructureCell.addElement(tmpMolecule);
                tmpTable.addCell(tmpMoleculeStructureCell);
                PdfPCell tmpCellOfFragment = new PdfPCell(new Paragraph("Fragments", this.fontFactory));
                tmpCellOfFragment.setHorizontalAlignment(Element.ALIGN_CENTER);
                tmpFragmentTable.addCell(tmpCellOfFragment);
                tmpDocument.add(tmpTable);
                tmpDocument.add(tmpFragmentTable);
                List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificAlgorithm(aFragmentationName);
                PdfPTable tmpFragmentationTable2 = new PdfPTable(3);
                for (int tmpFragmentNumber = 0; tmpFragmentNumber < tmpFragmentList.size(); ) {
                    ArrayList<PdfPCell> tmpCell = new ArrayList<PdfPCell>();
                    int tmpImagesNumbers = 0;
                    for (; tmpImagesNumbers < 3; tmpImagesNumbers++) {
                        if (tmpFragmentNumber >= tmpFragmentList.size()) {
                            break;
                        }
                        FragmentDataModel tmpFragmentDatModel = tmpFragmentList.get(tmpFragmentNumber);
                        IAtomContainer tmpFragmentStructure;
                        try{
                            tmpFragmentStructure = tmpFragmentDatModel.getAtomContainer();
                        } catch(CDKException anException){
                            Exporter.LOGGER.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, anException.toString() + "_" + tmpFragmentDatModel.getName(), anException);
                            continue;
                        }
                        String tmpFrequency = tmpMoleculeDataModel.getFragmentFrequencyOfSpecificAlgorithm(aFragmentationName).get(tmpFragmentDatModel.getUniqueSmiles()).toString();
                        javafx.scene.image.Image tmpFragmentImage = DepictionUtil.depictImageWithText(tmpFragmentStructure, 3.0, DepictionUtil.IMAGE_WIDTH_DEFAULT, DepictionUtil.IMAGE_HEIGHT_DEFAULT, tmpFrequency);
                        BufferedImage tmpBufferedImageOfFragment = SwingFXUtils.fromFXImage(tmpFragmentImage, null);
                        Image tmpFragment = this.getITextImage(tmpBufferedImageOfFragment);
                        PdfPCell cell = new PdfPCell();
                        cell.addElement(tmpFragment);
                        tmpCell.add(cell);
                        tmpFragmentNumber++;
                    }
                    for (int tmpCellIterator = 0; tmpCellIterator < 3; tmpCellIterator++) {
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
        } else {
            return;
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
     * using 2D information equally setting each z coordinate to 0. If no 2D information are available, the user can
     * choose via a confirmation alert to either generate (pseudo-) 2D atom coordinates (originally intended for layout
     * purposes) or to export without specifying the atom coordinates (x, y, z = 0).
     *
     * @param aFile File to save fragments
     * @param aFragmentDataModelList list of FragmentDataModel instances
     * @param generate2DCoordinates boolean value whether to generate 2D coordinates
     * @author Samuel Behr
     */
    private List<String> createFragmentationTabSingleSDFile(File aFile,
                                                            List<MoleculeDataModel> aFragmentDataModelList,
                                                            boolean generate2DCoordinates){
        if (aFragmentDataModelList == null) {
            return null;
        }
        try {
            if (aFile != null) {
                List<String> tmpFailedExportFragments = new ArrayList<>(aFragmentDataModelList.size());
                int tmpExportedFragmentsCounter = 0;
                int tmpFailedFragmentExportCounter = 0;
                try (
                        PrintWriter tmpWriter = new PrintWriter(aFile.getPath());
                        BufferedWriter tmpBufferedWriter = new BufferedWriter(tmpWriter);
                        SDFWriter tmpSDFWriter = new SDFWriter(tmpBufferedWriter);
                ) {
                    //specifying format of export
                    tmpSDFWriter.setAlwaysV3000(this.settingsContainer.getAlwaysMDLV3000FormatAtExportSetting());   //setting whether to always use MDL V3000 format
                    tmpSDFWriter.getSetting(MDLV2000Writer.OptWriteAromaticBondTypes).setSetting("true");   //accessing the WriteAromaticBondType setting
                    //iterating through the fragments held by the list of fragments
                    for (MoleculeDataModel tmpFragmentDataModel : aFragmentDataModelList) {
                        IAtomContainer tmpFragment;
                        try{
                            tmpFragment = tmpFragmentDataModel.getAtomContainer();
                        } catch(CDKException anException){
                            Exporter.LOGGER.log(Level.SEVERE, anException.toString() + "_" + tmpFragmentDataModel.getName(), anException);
                            tmpFailedExportFragments.add(tmpFragmentDataModel.getUniqueSmiles());
                            tmpFailedFragmentExportCounter++;
                            continue;
                        }
                        IAtomContainer tmpFragmentClone = null;
                        boolean tmpPoint3dAvailable = ChemUtil.has3DCoordinates(tmpFragmentDataModel);
                        boolean tmpPoint2dAvailable = ChemUtil.has2DCoordinates(tmpFragmentDataModel);
                        if (!tmpPoint3dAvailable) {
                            tmpFragmentClone = this.handleFragmentWithNo3dInformationAvailable(tmpFragment,
                                    tmpPoint2dAvailable, generate2DCoordinates, false);
                        }
                        //writing to file
                        try {
                            if (tmpPoint3dAvailable) {
                                tmpSDFWriter.write(tmpFragment);
                            } else {
                                tmpSDFWriter.write(tmpFragmentClone);
                            }
                            tmpExportedFragmentsCounter++;
                        } catch (CDKException anException) {
                            //retrying with a kekulized clone of the fragment
                            try {
                                if (tmpPoint3dAvailable) {
                                    tmpFragmentClone = tmpFragment.clone();
                                }
                                Kekulization.kekulize(tmpFragmentClone);
                                tmpSDFWriter.write(tmpFragmentClone);
                                tmpExportedFragmentsCounter++;
                            } catch (CDKException | CloneNotSupportedException anInnerException) {
                                Exporter.LOGGER.log(Level.SEVERE, anInnerException.toString(), anInnerException);
                                tmpFailedExportFragments.add(tmpFragmentDataModel.getUniqueSmiles());
                                tmpFailedFragmentExportCounter++;
                            }
                        }
                    }
                }
                Exporter.LOGGER.log(Level.INFO, String.format("Exported %d fragments as single SD file " +
                                "(export of %d fragments failed). File name: %s", tmpExportedFragmentsCounter,
                        tmpFailedFragmentExportCounter, aFile.getName()));
                return tmpFailedExportFragments;
            }
        } catch (NullPointerException | IOException | CDKException anException) {
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
        return null;
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
     * using 2D information equally setting each z coordinate to 0. If no 2D information are available, the user can
     * choose via a confirmation alert to either generate (pseudo-) 2D atom coordinates (originally intended for layout
     * purposes) or to export without specifying the atom coordinates (x, y, z = 0).
     *
     * @param aDirectory directory to save fragments
     * @param aFragmentDataModelList list of FragmentDataModel instances
     * @param generate2DCoordinates boolean value whether to generate 2D coordinates
     * @author Samuel Behr
     */
    private List<String> createFragmentationTabSeparateSDFiles(File aDirectory,
                                                               List<MoleculeDataModel> aFragmentDataModelList,
                                                               boolean generate2DCoordinates) {
        if (aFragmentDataModelList == null) {
            return null;
        }
        try {
            if (aDirectory != null && aDirectory.isDirectory()) {
                List<String> tmpFailedExportFragments = new ArrayList<>(aFragmentDataModelList.size());
                String tmpSDFFilesDirectoryPathName = aDirectory
                        + File.separator
                        + FRAGMENTS_EXPORT_DIRECTORY_NAME + "_" + FileUtil.getTimeStampFileNameExtension();
                String tmpFinalSDFilesDirectoryPathName = FileUtil.getNonExistingFilePath(tmpSDFFilesDirectoryPathName, File.separator);
                File tmpSDFilesDirectory = Files.createDirectory(Paths.get(tmpFinalSDFilesDirectoryPathName)).toFile();
                int tmpExportedFragmentsCounter = 0;
                int tmpFailedFragmentExportCounter = 0;
                //iterating through the fragments held by the list of fragments
                for (MoleculeDataModel tmpFragmentDataModel : aFragmentDataModelList) {
                    IAtomContainer tmpFragment;
                    try{
                        tmpFragment = tmpFragmentDataModel.getAtomContainer();
                    } catch(CDKException anException){
                        Exporter.LOGGER.log(Level.SEVERE, anException.toString() + "_" + tmpFragmentDataModel.getName(), anException);
                        tmpFailedExportFragments.add(tmpFragmentDataModel.getUniqueSmiles());
                        tmpFailedFragmentExportCounter++;
                        continue;
                    }
                    IAtomContainer tmpFragmentClone = null;
                    boolean tmpPoint3dAvailable = ChemUtil.has3DCoordinates(tmpFragmentDataModel);
                    boolean tmpPoint2dAvailable = ChemUtil.has2DCoordinates(tmpFragmentDataModel);
                    if (!tmpPoint3dAvailable) {
                        tmpFragmentClone = this.handleFragmentWithNo3dInformationAvailable(tmpFragment,
                                tmpPoint2dAvailable, generate2DCoordinates, false);
                    }
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
                        try {
                            //specifying format of export
                            tmpSDFWriter.setAlwaysV3000(this.settingsContainer.getAlwaysMDLV3000FormatAtExportSetting());   //setting whether to always use MDL V3000 format
                            tmpSDFWriter.getSetting(MDLV2000Writer.OptWriteAromaticBondTypes).setSetting("true");   //accessing the WriteAromaticBondType setting
                            if (tmpPoint3dAvailable) {
                                tmpSDFWriter.write(tmpFragment);
                            } else {
                                tmpSDFWriter.write(tmpFragmentClone);
                            }
                            tmpExportedFragmentsCounter++;
                        } catch (CDKException anException) {
                            //retrying with a kekulized clone of the fragment
                            try {
                                if (tmpPoint3dAvailable) {
                                    tmpFragmentClone = tmpFragment.clone();
                                }
                                Kekulization.kekulize(tmpFragmentClone);
                                tmpSDFWriter.write(tmpFragmentClone);
                                tmpExportedFragmentsCounter++;
                            } catch (CDKException | CloneNotSupportedException anInnerException) {
                                Exporter.LOGGER.log(Level.SEVERE, anInnerException.toString(), anInnerException);
                                tmpFailedExportFragments.add(tmpFragmentDataModel.getUniqueSmiles());
                                tmpFailedFragmentExportCounter++;
                            }
                        }
                    }
                }
                Exporter.LOGGER.log(Level.INFO, String.format("Exported %d fragments as separate SD files " +
                                "(export of %d fragments failed). Folder name: %s", tmpExportedFragmentsCounter,
                        tmpFailedFragmentExportCounter, tmpSDFilesDirectory.getName()));
                return tmpFailedExportFragments;
            }
        } catch (NullPointerException | IOException  | IllegalArgumentException anException) {
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
        return null;
    }
    //
    /**
     * Opens a directory chooser and exports the chemical data of the given fragments as PDB files to an empty folder
     * generated at the chosen path. The molecular formula of each fragment is used as name for the associated file. In
     * case no 3D information are being held in a fragment atom container, the respective PDB files are created using 2D
     * information equally setting each z coordinate to 0. If no 2D information are available, the user can choose via a
     * confirmation alert to either generate (pseudo-) 2D atom coordinates (originally intended for layout
     * purposes) or to export without specifying the atom coordinates (x, y, z = 0).
     *
     * @param aDirectory directory to save fragments
     * @param aFragmentDataModelList list of FragmentDataModel instances
     *
     * @author Samuel Behr
     */
    private List<String> createFragmentationTabPDBFiles(File aDirectory,
                                                        List<MoleculeDataModel> aFragmentDataModelList,
                                                        boolean generate2DCoordinates) {
        if (aFragmentDataModelList == null) {
            return null;
        }
        try {
            if (aDirectory != null && aDirectory.isDirectory()) {
                List<String> tmpFailedExportFragments = new ArrayList<>(aFragmentDataModelList.size());
                String tmpPDBFilesDirectoryPathName = aDirectory
                        + File.separator
                        + FRAGMENTS_EXPORT_DIRECTORY_NAME + "_" + FileUtil.getTimeStampFileNameExtension();
                String tmpFinalPDBFilesDirectoryPathName = FileUtil.getNonExistingFilePath(tmpPDBFilesDirectoryPathName, File.separator);
                File tmpPDBFilesDirectory = Files.createDirectory(Paths.get(tmpFinalPDBFilesDirectoryPathName)).toFile();
                int tmpExportedFragmentsCounter = 0;
                int tmpFailedFragmentExportCounter = 0;
                //iterating through the fragments held by the list of fragments
                for (MoleculeDataModel tmpFragmentDataModel : aFragmentDataModelList) {
                    IAtomContainer tmpFragment;
                    try{
                        tmpFragment = tmpFragmentDataModel.getAtomContainer();
                    } catch(CDKException anException){
                        Exporter.LOGGER.log(Level.SEVERE, anException.toString() + "_" + tmpFragmentDataModel.getName(), anException);
                        tmpFailedExportFragments.add(tmpFragmentDataModel.getUniqueSmiles());
                        tmpFailedFragmentExportCounter++;
                        continue;
                    }
                    IAtomContainer tmpFragmentClone = null;
                    boolean tmpPoint3dAvailable = ChemUtil.has3DCoordinates(tmpFragmentDataModel);
                    boolean tmpPoint2dAvailable = ChemUtil.has2DCoordinates(tmpFragmentDataModel);
                    //checking whether 3D information are available
                    if (!tmpPoint3dAvailable) {
                        tmpFragmentClone = this.handleFragmentWithNo3dInformationAvailable(tmpFragment,
                                tmpPoint2dAvailable, generate2DCoordinates, true);
                    }
                    //generating file
                    String tmpMolecularFormula = ChemUtil.generateMolecularFormula(tmpFragment);
                    String tmpPDBFilePathName = FileUtil.getNonExistingFilePath(tmpPDBFilesDirectory
                            + File.separator + tmpMolecularFormula, ".pdb");
                    File tmpPDBFile = new File(tmpPDBFilePathName);
                    //writing to file
                    try (
                            PDBWriter tmpPDBWriter = new PDBWriter(new FileOutputStream(tmpPDBFile));
                    ) {
                        if (tmpPoint3dAvailable) {
                            tmpPDBWriter.writeMolecule(tmpFragment);
                        } else {
                            tmpPDBWriter.writeMolecule(tmpFragmentClone);
                        }
                        tmpExportedFragmentsCounter++;
                    } catch (CDKException anException) {
                        Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                        tmpFailedExportFragments.add(tmpFragmentDataModel.getUniqueSmiles());
                        tmpFailedFragmentExportCounter++;
                    }
                }
                Exporter.LOGGER.log(Level.INFO, String.format("Exported %d fragments as PDB files " +
                                "(export of %d fragments failed). Folder name: %s", tmpExportedFragmentsCounter,
                        tmpFailedFragmentExportCounter, tmpPDBFilesDirectory.getName()));
                return tmpFailedExportFragments;
            }
        } catch (NullPointerException | IOException | IllegalArgumentException anException) {
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
        return null;
    }
    //
    /**
     * Converts a buffered image into a com.lowagie.text image (necessary for pdf export with iText) and returns it.
     *
     * @param aBufferedImage buffered image to be converted
     * @return com.lowagie.text image
     */
    private Image getITextImage(BufferedImage aBufferedImage){
        try {
            ByteArrayOutputStream tmpByteArrayOS = new ByteArrayOutputStream();
            ImageIO.write(aBufferedImage, "png", tmpByteArrayOS);
            byte[] bytes = tmpByteArrayOS.toByteArray();
            return Image.getInstance(bytes);
        } catch (IOException anException) {
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return  null;
        }
    }
    //
    /**
     * Creates a header with general information for the PDf files.
     *
     * @param aFragmentDataModelListSize size of list of fragments
     * @param aMoleculeDataModelListSize size of list of molecules
     * @param anAlgorithmName name of the used algorithm
     * @return fragmentation report table for a PDF file header
     * @author Betül Sevindik
     */
    private PdfPTable createHeaderTable(
            int aFragmentDataModelListSize,
            int aMoleculeDataModelListSize,
            String anAlgorithmName) {
        int tmpFragmentNumbers =  aFragmentDataModelListSize;
        int tmpMoleculeNumbers = aMoleculeDataModelListSize;
        //creates the header
        float tmpCellLengthIntro[] = {60f, 60f}; // relative sizes
        PdfPTable tmpTableIntro = new PdfPTable(tmpCellLengthIntro);
        PdfPCell tmpIntroCell1 = new PdfPCell(new Paragraph("Algorithm used",this.fontFactory));
        PdfPCell tmpIntroCell2 = new PdfPCell(new Paragraph(anAlgorithmName));
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
    //
    /**
     * Opens a FileChooser to be able to save a file.
     *
     * @param aParentStage Stage where the FileChooser should be shown
     * @param aDescription file type description to be used in the dialog (not the file extension)
     * @param anExtension file extension for extension filter of the file chooser dialog
     * @param aFileName initial file name to suggest to the user in the dialog
     * @return the selected file or null if no file has been selected
     * @throws NullPointerException if the given stage is null
     * @author Betül Sevindik
     */
    private File saveFile(Stage aParentStage, String aDescription, String anExtension, String aFileName) throws NullPointerException {
        Objects.requireNonNull(aParentStage, "aParentStage (instance of Stage) is null");
        FileChooser tmpFileChooser = new FileChooser();
        tmpFileChooser.setTitle((Message.get("Exporter.fileChooser.title")));
        FileChooser.ExtensionFilter tmpExtensionfilter2 = new FileChooser.ExtensionFilter(aDescription, anExtension);
        tmpFileChooser.getExtensionFilters().addAll(tmpExtensionfilter2);
        tmpFileChooser.setInitialFileName(aFileName);
        tmpFileChooser.setInitialDirectory(new File(this.settingsContainer.getRecentDirectoryPathSetting()));
        File  tmpFile = tmpFileChooser.showSaveDialog(aParentStage);
        if(tmpFile != null){
            this.settingsContainer.setRecentDirectoryPathSetting(tmpFile.getParent());
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
        if(!tmpRecentDirectory.isDirectory()) {
            tmpRecentDirectory = new File(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
            this.settingsContainer.setRecentDirectoryPathSetting(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
        }
        tmpDirectoryChooser.setInitialDirectory(tmpRecentDirectory);
        File tmpDirectory = tmpDirectoryChooser.showDialog(aParentStage);
        if(tmpDirectory != null){
            this.settingsContainer.setRecentDirectoryPathSetting(tmpDirectory.getPath());
        }
        return tmpDirectory;
    }
    //
    //TODO: Split this up or at least introduce input restrictions for  irreconcilable parameter combinations
    /**
     * Optionally completes 2D coordinates of a given fragment by setting all z-coordinates to 0 or generates new
     * pseudo-3D-coordinates for it using a structure diagram generator. As a third option, all coordinates of the given
     * atoms can be set to 0. Which option is applied depends on the given parameters. Initially, the given fragment is
     * cloned but if this fails, the original, given atom container is processed and the exception logged.
     *
     * @param aFragment atom container of a fragment to handle
     * @param aPoint2dAvailable whether 2D atom coordinates are available; this is not checked by this method
     * @param aGenerate2dAtomCoordinates whether 2D atom coordinates should be generated
     * @param aSetCoordinatesToZero whether all coordinates need to be set to 0
     * @return a clone of the given fragment with 3D atom coordinates created according to the given parameters
     * @author Samuel Behr
     */
    private IAtomContainer handleFragmentWithNo3dInformationAvailable(
            IAtomContainer aFragment,
            boolean aPoint2dAvailable,
            boolean aGenerate2dAtomCoordinates,
            boolean aSetCoordinatesToZero) {
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
            try {
                StructureDiagramGenerator tmpStructureDiagramGenerator = new StructureDiagramGenerator();
                tmpStructureDiagramGenerator.generateCoordinates(tmpFragmentClone);
            } catch (CDKException anException) {
                Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                tmpErrorAtGenerating2dAtomCoordinates = true;
            }
        }
        if (aPoint2dAvailable || (aGenerate2dAtomCoordinates && !tmpErrorAtGenerating2dAtomCoordinates)) {
            //transfer of 2D coordinates to 3D coordinates with z = 0
            for (IAtom tmpAtom : tmpFragmentClone.atoms()) {
                Point3d tmpPoint3d = new Point3d(tmpAtom.getPoint2d().x, tmpAtom.getPoint2d().y, 0.0);
                tmpAtom.setPoint3d(tmpPoint3d);
            }
        } else if (aSetCoordinatesToZero) {
            //setting all atom coordinates to 0
            for (IAtom tmpAtom : tmpFragmentClone.atoms()) {
                tmpAtom.setPoint3d(new Point3d(0.0, 0.0, 0.0));
            }
        }
        return tmpFragmentClone;
    }
    //</editor-fold>
}