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
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import javafx.collections.ObservableList;
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
 * @author Betül Sevindik, Samuel Behr
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
    //TODO: Get separator from general settings
    //TODO: Parameter tests necessary?
    //TODO: Move literals like header texts to properties file or constants?
    /**
     * Exports the fragmentation results as they are displayed on the fragments tab as a CSV file. Opens a file chooser
     * dialog for the user to determine a directory and file for the exported data.
     *
     * @param aParentStage Stage to show the FileChooser
     * @param aList a list of FragmentDataModel instances to export
     * @param aSeparator the separator for the csv file
     * @return Csv file which contains the results of the fragmentation
     * @author Betül Sevindik
     */
    public File createFragmentationTabCsvFile(Stage aParentStage, ObservableList<FragmentDataModel> aList, char aSeparator) {
        try {
            File tmpFragmentationCsvFile = null;
            tmpFragmentationCsvFile = this.saveFile(aParentStage, "CSV", "*.csv",
                    "FragmentExport");
            if (tmpFragmentationCsvFile != null) {
                PrintWriter tmpWriter = new PrintWriter(tmpFragmentationCsvFile.getPath());
                StringBuilder tmpFragmentationCsvHeader = new StringBuilder();
                tmpFragmentationCsvHeader.append("SmilesString" + aSeparator + "Frequency" + aSeparator + "Percentage"
                        + aSeparator + "MolecularFrequency"
                        + aSeparator + "MolecularPercentage\n");
                tmpWriter.write(tmpFragmentationCsvHeader.toString());
                for (FragmentDataModel tmpFragmentDataModel : aList) {
                    tmpWriter.printf("%s" + aSeparator + "%d" + aSeparator + "%.3f" + aSeparator + "%d" + aSeparator + "%.2f\n",
                            tmpFragmentDataModel.getUniqueSmiles(), tmpFragmentDataModel.getAbsoluteFrequency(),
                            tmpFragmentDataModel.getAbsolutePercentage(), tmpFragmentDataModel.getMoleculeFrequency(),
                            tmpFragmentDataModel.getMoleculePercentage());
                }
                tmpWriter.close();
                return tmpFragmentationCsvFile;
            } else {
                return null;
            }
        } catch (FileNotFoundException | NullPointerException anException) {
            GuiUtil.GuiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),Message.get("Error.ExceptionAlert.Header"),
                    anException.toString(), anException);
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }

    //TODO: Improve exception handling so that it does not kill the whole export if one fragment is faulty
    //TODO: See tasks and issues document for further to dos concerning the PDF export
    /**
     * Exports the fragmentation results as they are displayed on the fragments tab as a PDF file. Opens a file chooser
     * dialog for the user to determine a directory and file for the exported data.
     *
     * @param aParentStage Stage to show the FileChooser
     * @param aFragmentDataModelList a list of FragmentDataModel instances to export
     * @param aMoleculeDataModelList a list MoleculeDataModel needed for the fragmentation report at the head of the exported document
     * @param aName fragmentation name to be displayed in the header of the PDF file
     * @return PDF file which contains the results of the fragmentation
     * @author Betül Sevindik
     */
    public Document createFragmentationTabPdfFile(
            Stage aParentStage,
            ObservableList<FragmentDataModel> aFragmentDataModelList,
            ObservableList<MoleculeDataModel> aMoleculeDataModelList,
            String aName) {
        try {
            File tmpFragmentationPdfFile = null;
            tmpFragmentationPdfFile = this.saveFile(aParentStage, "PDF", "*.pdf",
                    "FragmentExport");
            if (tmpFragmentationPdfFile != null) {
                Document tmpDocument = new Document(PageSize.A4);
                tmpDocument.setPageSize(tmpDocument.getPageSize().rotate());
                PdfWriter.getInstance(tmpDocument, new FileOutputStream(tmpFragmentationPdfFile.getPath()));
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
                    BufferedImage tmpBufferedImageFragment = SwingFXUtils.fromFXImage(tmpImageStructureOfFragment, null);
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
                tmpDocument.add(this.createHeaderTable(aFragmentDataModelList, aMoleculeDataModelList, aName));
                tmpDocument.add(tmpSpace);
                tmpDocument.add(tmpFragmentationTable);
                tmpDocument.close();
                return tmpDocument;
            } else {
                return null;
            }
        } catch (IOException | DocumentException | CDKException | NullPointerException anException) {
            GuiUtil.GuiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),Message.get("Error.ExceptionAlert.Header"),
                    anException.toString(), anException);
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }

    //TODO: Return file as with the other export methods?
    /**
     * Opens a file chooser and exports the chemical data of the given fragments as a single MDL SD file to the chosen
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
     * @param aParentStage stage to show the FileChooser
     * @param aFragmentDataModelList list of FragmentDataModel instances
     * @author Samuel Behr
     */
    public void createFragmentationTabSingleSDFile(Stage aParentStage, ObservableList<FragmentDataModel> aFragmentDataModelList) {
        if (aFragmentDataModelList == null) {
            GuiUtil.GuiMessageAlert(Alert.AlertType.INFORMATION,
                    Message.get("Exporter.MessageAlert.NoDataAvailable.title"),
                    Message.get("Exporter.MessageAlert.NoDataAvailable.header"),
                    null);
            return;
        }
        try {
            //opening file chooser to determine the file to export to
            File tmpFile = this.saveFile(aParentStage, "SD-File", "*.sdf",
                    "FragmentExport");
            if (tmpFile != null) {
                int tmpExportedFragmentsCounter = 0;
                int tmpFailedFragmentExportCounter = 0;
                try (
                        PrintWriter tmpWriter = new PrintWriter(tmpFile.getPath());
                        BufferedWriter tmpBufferedWriter = new BufferedWriter(tmpWriter);
                        SDFWriter tmpSDFWriter = new SDFWriter(tmpBufferedWriter);
                ) {
                    //specifying format of export
                    tmpSDFWriter.setAlwaysV3000(this.settingsContainer.getAlwaysMDLV3000FormatAtExportSetting());   //setting whether to always use MDL V3000 format
                    tmpSDFWriter.getSetting(MDLV2000Writer.OptWriteAromaticBondTypes).setSetting("true");   //accessing the WriteAromaticBondType setting
                    boolean tmpHasNo2dInformationAlertBeenShown = false;    //whether the conformation alert has been shown yet
                    boolean tmpGenerate2dAtomCoordinates = false;           //whether coordinates should be generated
                    //iterating through the fragments held by the list of fragments
                    for (FragmentDataModel tmpFragmentDataModel : aFragmentDataModelList) {
                        IAtomContainer tmpFragment = tmpFragmentDataModel.getAtomContainer();
                        IAtomContainer tmpFragmentClone = null;
                        //looping through all the fragments atoms checking whether 3D or else 2D atom coordinates are available
                        boolean tmpPoint3dAvailable = true;
                        boolean tmpPoint2dAvailable = true;
                        for (IAtom tmpAtom : tmpFragment.atoms()) {
                            if (tmpPoint3dAvailable) {
                                if (tmpAtom.getPoint3d() != null) {
                                    continue;
                                }
                                tmpPoint3dAvailable = false;
                            }
                            if (tmpAtom.getPoint2d() == null) {
                                if (!tmpHasNo2dInformationAlertBeenShown) {
                                    ButtonType tmpConformationResult = GuiUtil.GuiConformationAlert(
                                            Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.title"),
                                            Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.header"),
                                            Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.text")
                                    );
                                    if (tmpConformationResult == ButtonType.OK) {
                                        tmpGenerate2dAtomCoordinates = true;
                                    }
                                    tmpHasNo2dInformationAlertBeenShown = true;
                                }
                                tmpPoint2dAvailable = false;
                                break;
                            }
                        }
                        //checking whether 3D information are available
                        if (!tmpPoint3dAvailable) {
                            tmpFragmentClone = this.handleFragmentWithNo3dInformationAvailable(tmpFragment,
                                    tmpPoint2dAvailable, tmpGenerate2dAtomCoordinates, false);
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
                                tmpFailedFragmentExportCounter++;
                            }
                        }
                    }
                }
                Exporter.LOGGER.log(Level.INFO, String.format("Exported %d fragments as single SD file " +
                        "(export of %d fragments failed). File name: %s", tmpExportedFragmentsCounter,
                        tmpFailedFragmentExportCounter, tmpFile.getName()));
                if (tmpFailedFragmentExportCounter > 0) {
                    GuiUtil.GuiMessageAlert(Alert.AlertType.WARNING,
                            Message.get("Exporter.FragmentsTab.ExportNotPossible.title"),
                            Message.get("Exporter.FragmentsTab.ExportNotPossible.header"),
                            null);
                }
            }
        } catch (NullPointerException | IOException | CDKException anException) {
            GuiUtil.GuiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    anException.toString(),
                    anException);
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
        }
    }

    //TODO: Return file as with the other export methods?
    /**
     * Opens a directory chooser and exports the chemical data of the given fragments as separate MDL SD files to an
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
     * @param aParentStage stage to show the DirectoryChooser
     * @param aFragmentDataModelList list of FragmentDataModel instances
     * @author Samuel Behr
     */
    public void createFragmentationTabSeparateSDFiles(Stage aParentStage, ObservableList<FragmentDataModel> aFragmentDataModelList) {
        if (aFragmentDataModelList == null) {
            GuiUtil.GuiMessageAlert(Alert.AlertType.INFORMATION,
                    Message.get("Exporter.MessageAlert.NoDataAvailable.title"),
                    Message.get("Exporter.MessageAlert.NoDataAvailable.header"),
                    null);
            return;
        }
        try {
            //opening a directory chooser and creating an empty directory as destination for the exported files
            File tmpDirectory = this.chooseDirectory(aParentStage);
            if (tmpDirectory != null && tmpDirectory.isDirectory()) {
                String tmpSDFFilesDirectoryPathName = tmpDirectory
                        + File.separator
                        + FRAGMENTS_EXPORT_DIRECTORY_NAME + "_" + FileUtil.getTimeStampFileNameExtension();
                String tmpFinalSDFilesDirectoryPathName = FileUtil.getNonExistingFilePath(tmpSDFFilesDirectoryPathName, File.separator);
                File tmpSDFilesDirectory = Files.createDirectory(Paths.get(tmpFinalSDFilesDirectoryPathName)).toFile();
                boolean tmpHasNo2dInformationAlertBeenShown = false;    //whether the conformation alert has been shown yet
                boolean tmpGenerate2dAtomCoordinates = false;           //whether coordinates should be generated
                int tmpExportedFragmentsCounter = 0;
                int tmpFailedFragmentExportCounter = 0;
                //iterating through the fragments held by the list of fragments
                for (FragmentDataModel tmpFragmentDataModel : aFragmentDataModelList) {
                    IAtomContainer tmpFragment = tmpFragmentDataModel.getAtomContainer();
                    IAtomContainer tmpFragmentClone = null;
                    //looping through all the fragments atoms checking whether 3D or else 2D atom coordinates are available
                    boolean tmpPoint3dAvailable = true;
                    boolean tmpPoint2dAvailable = true;
                    for (IAtom tmpAtom : tmpFragment.atoms()) {
                        if (tmpPoint3dAvailable) {
                            if (tmpAtom.getPoint3d() != null) {
                                continue;
                            }
                            tmpPoint3dAvailable = false;
                        }
                        if (tmpAtom.getPoint2d() == null) {
                            if (!tmpHasNo2dInformationAlertBeenShown) {
                                ButtonType tmpConformationResult = GuiUtil.GuiConformationAlert(
                                        Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.title"),
                                        Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.header"),
                                        Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.text")
                                );
                                if (tmpConformationResult == ButtonType.OK) {
                                    tmpGenerate2dAtomCoordinates = true;
                                }
                                tmpHasNo2dInformationAlertBeenShown = true;
                            }
                            tmpPoint2dAvailable = false;
                            break;
                        }
                    }
                    //checking whether 3D information are available
                    if (!tmpPoint3dAvailable) {
                        tmpFragmentClone = this.handleFragmentWithNo3dInformationAvailable(tmpFragment,
                                tmpPoint2dAvailable, tmpGenerate2dAtomCoordinates, false);
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
                                tmpFailedFragmentExportCounter++;
                            }
                        }
                    }
                }
                Exporter.LOGGER.log(Level.INFO, String.format("Exported %d fragments as separate SD files " +
                        "(export of %d fragments failed). Folder name: %s", tmpExportedFragmentsCounter,
                        tmpFailedFragmentExportCounter, tmpSDFilesDirectory.getName()));
                if (tmpFailedFragmentExportCounter > 0) {
                    GuiUtil.GuiMessageAlert(Alert.AlertType.WARNING,
                            Message.get("Exporter.FragmentsTab.ExportNotPossible.title"),
                            Message.get("Exporter.FragmentsTab.ExportNotPossible.header"),
                            null);
                }
            }
        } catch (NullPointerException | IOException | CDKException | IllegalArgumentException anException) {
            GuiUtil.GuiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    anException.toString(),
                    anException);
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
        }
    }

    //TODO: Return file as with the other export methods?
    /**
     * Opens a directory chooser and exports the chemical data of the given fragments as PDB files to an empty folder
     * generated at the chosen path. The molecular formula of each fragment is used as name for the associated file. In
     * case no 3D information are being held in a fragment atom container, the respective PDB files are created using 2D
     * information equally setting each z coordinate to 0. If no 2D information are available, the user can choose via a
     * confirmation alert to either generate (pseudo-) 2D atom coordinates (originally intended for layout
     * purposes) or to export without specifying the atom coordinates (x, y, z = 0).
     *
     * @param aParentStage stage to show the DirectoryChooser
     * @param aFragmentDataModelList list of FragmentDataModel instances
     * @author Samuel Behr
     */
    public void createFragmentationTabPDBFiles(Stage aParentStage, ObservableList<FragmentDataModel> aFragmentDataModelList) {
        if (aFragmentDataModelList == null) {
            GuiUtil.GuiMessageAlert(Alert.AlertType.INFORMATION,
                    Message.get("Exporter.MessageAlert.NoDataAvailable.title"),
                    Message.get("Exporter.MessageAlert.NoDataAvailable.header"),
                    null);
            return;
        }
        try {
            //opening a directory chooser and creating an empty directory as destination for the exported files
            File tmpDirectory = this.chooseDirectory(aParentStage);
            if (tmpDirectory != null && tmpDirectory.isDirectory()) {
                String tmpPDBFilesDirectoryPathName = tmpDirectory
                        + File.separator
                        + FRAGMENTS_EXPORT_DIRECTORY_NAME + "_" + FileUtil.getTimeStampFileNameExtension();
                String tmpFinalPDBFilesDirectoryPathName = FileUtil.getNonExistingFilePath(tmpPDBFilesDirectoryPathName, File.separator);
                File tmpPDBFilesDirectory = Files.createDirectory(Paths.get(tmpFinalPDBFilesDirectoryPathName)).toFile();
                boolean tmpHasNo2dInformationAlertBeenShown = false;    //whether the conformation alert has been shown yet
                boolean tmpGenerate2dAtomCoordinates = false;           //whether coordinates should be generated
                int tmpExportedFragmentsCounter = 0;
                int tmpFailedFragmentExportCounter = 0;
                //iterating through the fragments held by the list of fragments
                for (FragmentDataModel tmpFragmentDataModel : aFragmentDataModelList) {
                    IAtomContainer tmpFragment = tmpFragmentDataModel.getAtomContainer();
                    IAtomContainer tmpFragmentClone = null;
                    //looping through all the fragments atoms checking whether 3D or else 2D atom coordinates are available
                    boolean tmpPoint3dAvailable = true;
                    boolean tmpPoint2dAvailable = true;
                    for (IAtom tmpAtom : tmpFragment.atoms()) {
                        if (tmpPoint3dAvailable) {
                            if (tmpAtom.getPoint3d() != null) {
                                continue;
                            }
                            tmpPoint3dAvailable = false;
                        }
                        if (tmpAtom.getPoint2d() == null) {
                            if (!tmpHasNo2dInformationAlertBeenShown) {
                                ButtonType tmpConformationResult = GuiUtil.GuiConformationAlert(
                                        Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.title"),
                                        Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.header"),
                                        Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.text")
                                );
                                if (tmpConformationResult == ButtonType.OK) {
                                    tmpGenerate2dAtomCoordinates = true;
                                }
                                tmpHasNo2dInformationAlertBeenShown = true;
                            }
                            tmpPoint2dAvailable = false;
                            break;
                        }
                    }
                    //checking whether 3D information are available
                    if (!tmpPoint3dAvailable) {
                        tmpFragmentClone = this.handleFragmentWithNo3dInformationAvailable(tmpFragment,
                                tmpPoint2dAvailable, tmpGenerate2dAtomCoordinates, true);
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
                        tmpFailedFragmentExportCounter++;
                    }
                }
                Exporter.LOGGER.log(Level.INFO, String.format("Exported %d fragments as PDB files " +
                        "(export of %d fragments failed). Folder name: %s", tmpExportedFragmentsCounter,
                        tmpFailedFragmentExportCounter, tmpPDBFilesDirectory.getName()));
                if (tmpFailedFragmentExportCounter > 0) {
                    GuiUtil.GuiMessageAlert(Alert.AlertType.WARNING,
                            Message.get("Exporter.FragmentsTab.ExportNotPossible.title"),
                            Message.get("Exporter.FragmentsTab.ExportNotPossible.header"),
                            null);
                }
            }
        } catch (NullPointerException | IOException | CDKException | IllegalArgumentException anException) {
            GuiUtil.GuiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    anException.toString(),
                    anException);
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
        }
    }

    //TODO: Improve exception handling so that it does not kill the whole export if one fragment is faulty
    //TODO: See tasks and issues document for further to dos concerning the PDF export
    /**
     * Exports the fragmentation results as they are displayed on the itemization tab as a PDF file. Opens a file chooser
     * dialog for the user to determine a directory and file for the exported data.
     *
     * @param aParentStage Stage to show the FileChooser
     * @param aFragmentDataModelList a list of FragmentDataModel instances to export
     * @param aMoleculeDataModelList a list MoleculeDataModel needed for the fragmentation report at the head of the exported document
     * @param aFragmentationName fragmentation name to retrieve the specific set of fragments from the molecule data models
     * @param aName fragmentation name to be displayed in the header of the PDF file
     * @return PDF file with the data that appears on the itemization tab
     * @author Betül Sevindik
     */
    public Document createItemizationTabPdfFile(
            Stage aParentStage,
            ObservableList<FragmentDataModel> aFragmentDataModelList,
            ObservableList<MoleculeDataModel> aMoleculeDataModelList,
            String aFragmentationName,
            String aName) {
        try {
            File tmpPdfFile = null;
            tmpPdfFile = this.saveFile(aParentStage, "PDF", "*.pdf","FragmentExport");
            if (tmpPdfFile != null) {
                Document tmpDocument = new Document(PageSize.A4);
                PdfWriter.getInstance(tmpDocument, new FileOutputStream(tmpPdfFile.getPath()));
                tmpDocument.open();
                // creates the pdf table
                Chunk tmpItemizationTabHeader = new Chunk("Export of the Itemization tab",
                        FontFactory.getFont(FontFactory.TIMES_ROMAN, 18, Font.UNDERLINE));
                Paragraph tmpSpace = new Paragraph(" ");
                tmpDocument.add(tmpItemizationTabHeader);
                tmpDocument.add(tmpSpace);
                tmpDocument.add(this.createHeaderTable(aFragmentDataModelList, aMoleculeDataModelList, aName));
                tmpDocument.add(tmpSpace);
                for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
                    PdfPTable tmpTable = new PdfPTable(2);
                    PdfPTable tmpFragmentTableversuch = new PdfPTable(1);
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
                    PdfPCell tmpCellOfFragment = new PdfPCell(new Paragraph("Fragments", this.fontFactory));
                    tmpCellOfFragment.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tmpFragmentTableversuch.addCell(tmpCellOfFragment);
                    tmpDocument.add(tmpTable);
                    tmpDocument.add(tmpFragmentTableversuch);
                    tmpMoleculeFile.delete();
                    List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificAlgorithm(aFragmentationName);
                    PdfPTable tmpFragmentationTable2 = new PdfPTable(3);
                    for (int tmpFragmentNumber = 0; tmpFragmentNumber < tmpFragmentList.size(); ) {
                        ArrayList<PdfPCell> tmpCell = new ArrayList<PdfPCell>();
                        int tmpImagesNumbers = 0;
                        for (; tmpImagesNumbers < 3; tmpImagesNumbers++) {
                            if (tmpFragmentNumber >= tmpFragmentList.size()) {
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
                return tmpDocument;
            } else {
                return null;
            }
        } catch(IOException | DocumentException | CDKException | NullPointerException anException) {
            GuiUtil.GuiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),Message.get("Error.ExceptionAlert.Header"),
                    anException.toString(), anException);
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }

    //TODO: Get separator from general settings
    //TODO: Parameter tests necessary?
    //TODO: Move literals like header texts to properties file or constants?
    /**
     * Exports the fragmentation results as they are displayed on the itemization tab as a CSV file. Opens a file chooser
     * dialog for the user to determine a directory and file for the exported data.
     *
     * @param aParentStage Stage to show the FileChooser
     * @param aMoleculeDataModelList a list of MoleculeDataModel instances to export along with their fragments
     * @param aFragmentationName fragmentation name to retrieve the specific set of fragments from the molecule data models
     * @param aSeparator the separator for the csv file
     * @return Csv file with the data that appears on the itemization tab
     * @author Betül Sevindik
     */
    public File createItemizationTabCsvFile(Stage aParentStage, ObservableList<MoleculeDataModel> aMoleculeDataModelList, String aFragmentationName, char aSeparator) {
        try {
            File tmpCsvFile = null;
            tmpCsvFile = this.saveFile(aParentStage, "CSV", "*.csv", "FragmentExport");
            if (tmpCsvFile != null) {
                PrintWriter tmpWriter = new PrintWriter(tmpCsvFile.getPath());
                StringBuilder tmpCsvHeader = new StringBuilder();
                tmpCsvHeader.append("MoleculeName" + aSeparator + "SmilesOfStructure" + aSeparator + "SmilesOfFragments\n");
                tmpWriter.write(tmpCsvHeader.toString());
                for (MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
                    tmpWriter.printf("%s" + aSeparator + "%s", tmpMoleculeDataModel.getName(), tmpMoleculeDataModel.getUniqueSmiles());
                    List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificAlgorithm(aFragmentationName);
                    for (FragmentDataModel tmpFragmentDataModel : tmpFragmentList) {
                        tmpWriter.append(aSeparator);
                        tmpWriter.printf("%s", tmpFragmentDataModel.getUniqueSmiles());
                    }
                    tmpWriter.append("\n");
                }
                tmpWriter.close();
                return tmpCsvFile;
            } else {
                return null;
            }
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
     * Writes an image file from a buffered image in the application's scrap directory for images. The file intended to
     * be deleted after usage!
     *
     * @param aBufferedImage the buffered image to write
     * @return File with an image of Molecule or Structure or null if an exception occurred
     * @author Betül Sevindik
     */
    private File getImageFile(BufferedImage aBufferedImage) {
        try {
            String tmpImageDirectoryPathName = FileUtil.getAppDirPath() + File.separator
                    + BasicDefinitions.IMAGE_FILES_DIRECTORY + File.separator;
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

    //TODO: This method only needs the sizes, not the complete lists of fragments and molecules
    /**
     * Creates a header with general information for the PDf files.
     *
     * @param aFragmentDataModelList list of fragments
     * @param aMoleculeDataModelList list of molecules
     * @param anAlgorithmName name of the used algorithm
     * @return fragmentation report table for a PDF file header
     * @author Betül Sevindik
     */
    private PdfPTable createHeaderTable(
            ObservableList<FragmentDataModel> aFragmentDataModelList,
            ObservableList<MoleculeDataModel> aMoleculeDataModelList,
            String anAlgorithmName) {
        int tmpFragmentNumbers =  aFragmentDataModelList.size();
        int tmpMoleculeNumbers = aMoleculeDataModelList.size();
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

    //TODO: use and set recent directory
    /**
     * Opens a FileChooser to be able to save a file.
     *
     * @param aParentStage Stage where FileChooser should be shown
     * @param aDescription
     * @param anExtension
     * @param aFileName
     * @return
     * @author Betül Sevindik
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

    //TODO: use and set recent directory
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
        return tmpDirectoryChooser.showDialog(aParentStage);
    }

    /**
     * Completes the given information of a clone of the given fragment to 3D atom coordinates by using 2D atom
     * coordinates setting every z-coordinate to 0. In case no 2D information are being held by the fragments atom
     * container, either 2D atom coordinates for layout purposes can be generated or all coordinates can be equally set
     * to 0 (according to the given parameters).
     * If no clone could be generated, the exception is printed in the log file and the process is done without.
     *
     * @param aFragment fragment to handle
     * @param aPoint2dAvailable whether 2D atom coordinates of the fragment are available
     * @param aGenerate2dAtomCoordinates whether 2D atom coordinates should be generated
     * @param aSetCoordinatesToZero whether all coordinates need to be set to 0
     * @return a clone of the given fragment with 3D atom coordinates created according to the given parameters
     * @author Samuel Behr
     */
    private IAtomContainer handleFragmentWithNo3dInformationAvailable(IAtomContainer aFragment, boolean aPoint2dAvailable, boolean aGenerate2dAtomCoordinates, boolean aSetCoordinatesToZero) {
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
                Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException); //TODO: somehow specify at which fragment the exception appeared?
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