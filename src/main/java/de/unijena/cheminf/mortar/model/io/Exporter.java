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


//import com.itextpdf.kernel.pdf.PdfWriter;
//import com.itextpdf.layout.Document;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.*;

import com.itextpdf.text.Image;
import com.sun.javafx.font.FontFactory;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.freehep.graphicsio.pdf.PDF;
import org.openscience.cdk.exception.CDKException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Exporter {

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(Exporter.class.getName());

    /**
     *
     */
    private Chunk header;

    /**
     *
     */
    private Paragraph space;

    /**
     *
     */
    private Document pdfDocument;

    /**
     *
     */
    private  BufferedImage bufferedImageFragments;

    /**
     *
     */
    private File imageFile;

    /**
     *
     */
    private Image imageOfFragment;

    /**
     * Constructor
     */
    public Exporter() {
        this.header = new Chunk("FRAGMENTATION",
                FontFactory.getFont(FontFactory.TIMES_ROMAN, 18, Font.UNDERLINE));
        this.pdfDocument = new Document(PageSize.A4);
        this.space = new Paragraph(" ");
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
            /**
            this.pdfDocument.setPageSize(this.pdfDocument.getPageSize().rotate());
            PdfWriter.getInstance(this.pdfDocument, new FileOutputStream(tmpFile.getPath()));
            this.pdfDocument.open();
             */
            PdfWriter writer = new PdfWriter(tmpFile.getPath());
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document doc = new Document(pdfDoc);

            float tmpCellLength[] = {80f, 130f, 55f, 55f, 55f, 55f};
            //float tmpCellLength[] = {1f, 1f,1f,1f,1f,1f};
           Table tmpFragmentationTable = new Table(tmpCellLength);
            //tmpFragmentationTable.setWidthPercentage(50f);
            Cell tmpCell1 = new Cell();
            Cell tmpCell2 = new Cell();
            Cell tmpCell3 = new Cell();
            Cell tmpCell4 = new Cell();
            Cell tmpCell5 = new Cell();
            Cell tmpCell6 = new Cell();

            tmpCell1.add()


            tmpFragmentationTable.addCell(tmpCell6);
            tmpFragmentationTable.addCell(tmpCell1);
            tmpFragmentationTable.addCell(tmpCell2);
            tmpFragmentationTable.addCell(tmpCell3);
            tmpFragmentationTable.addCell(tmpCell4);
            tmpFragmentationTable.addCell(tmpCell5);

            for (FragmentDataModel tmpFragmentDataModel : aFragmentDataModelList) {
                int tmpAbsoluteFrequency = tmpFragmentDataModel.getAbsoluteFrequency();
                String tmpStringAbsoluteFrequency = String.format("%d", tmpAbsoluteFrequency);
                double tmpAbsolutePercentage = tmpFragmentDataModel.getAbsolutePercentage();
                int tmpMoleculeFrequency = tmpFragmentDataModel.getMoleculeFrequency();
                String tmpStringMoleculeFrequency = String.format("%d", tmpMoleculeFrequency);
                String tmpStringAbsolutePercentage = String.format("%.3f", tmpAbsolutePercentage);
                double tmpMoleculePercentage = tmpFragmentDataModel.getMoleculePercentage();
                String tmpStringMoleculePercentage = String.format("%.3f", tmpMoleculePercentage);

                ImageView tmpImageViewFragments = tmpFragmentDataModel.getStructure();
                javafx.scene.image.Image tmpImageOfFragments = tmpImageViewFragments.getImage();

                this.bufferedImageFragments = SwingFXUtils.fromFXImage(tmpImageOfFragments, null);
                //BufferedImage b = resize(this.bufferedImageFragments, 100, 100);

                
                //BufferedImage im = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
                //System.out.println(im.getWidth());
                // im = aBufferedImage;
                //BufferedImage buf = aBufferedImage.getSubimage(0, 0, 100, 100);
                //ImageIO.write(this.bufferedImageFragments, "PNG", tmpImageFile);
                this.imageFile = this.getImageFile(this.bufferedImageFragments);
                this.imageOfFragment = Image.getInstance(this.imageFile.getAbsolutePath());
                this.imageOfFragment.scaleToFit(80, 80);
                tmpCell2.add(this.imageOfFragment.setAut)
              // System.out.println( cell7.setCalculatedHeight(80f));
                 //cell7.setImage(this.imageOfFragment);
                //this.imageOfFragment.scaleAbsolute(100f, 100

                //tmpFragmentationTable.addCell(this.imageOfFragment);

                System.out.println(this.imageOfFragment.getHeight());
                System.out.println(this.imageOfFragment.getHeight());
                System.out.println(this.imageOfFragment.getScaledHeight());
                System.out.println(this.imageOfFragment.getScaledWidth());
                tmpFragmentationTable.addCell(tmpFragmentDataModel.getUniqueSmiles());
                tmpFragmentationTable.addCell(tmpStringAbsoluteFrequency);
                tmpFragmentationTable.addCell(tmpStringAbsolutePercentage);
                tmpFragmentationTable.addCell(tmpStringMoleculeFrequency);
                tmpFragmentationTable.addCell(tmpStringMoleculePercentage);
               this.imageFile.delete();
            }
           this.pdfDocument.add(this.header);
           this.pdfDocument.add(this.space);
           this.pdfDocument.add(this.createHeaderTable(aFragmentDataModelList, aMoleculeDataModelList));
           this.pdfDocument.add(this.space);
           this.pdfDocument.add(tmpFragmentationTable);
           this.pdfDocument.close();
           return this.pdfDocument;
        } catch (IOException | DocumentException anException) {
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }

    public Document ItemPdf(Stage aParentstage, ObservableList<FragmentDataModel> aFragmentDataModelList, ObservableList<MoleculeDataModel> aMoleculeDataModelList, String aFragmentationName) {

        try {
            File tmpFile = this.saveFile(aParentstage);
            PdfWriter.getInstance(this.pdfDocument, new FileOutputStream(tmpFile.getPath()));
            this.pdfDocument.setPageSize(this.pdfDocument.getPageSize().rotate());
            this.pdfDocument.open();

            float tmpCellLength[] = {100f, 40f};
            PdfPTable tmpItemTabFragmentationTable = new PdfPTable(tmpCellLength);
            PdfPCell tmpCell6 = new PdfPCell(new Paragraph("Name", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            PdfPCell tmpCell5 = new PdfPCell(new Paragraph("Structure", FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
            Paragraph para = new Paragraph(" ");

            for (MoleculeDataModel tmpKey : aMoleculeDataModelList) {
                tmpItemTabFragmentationTable.addCell(tmpCell6);
                String name = tmpKey.getName();
                tmpItemTabFragmentationTable.addCell(name);
                tmpItemTabFragmentationTable.addCell(tmpCell5);

                ImageView molStructure = tmpKey.getStructure();
                javafx.scene.image.Image tmpImageOfFragments = molStructure.getImage();
                this.bufferedImageFragments = SwingFXUtils.fromFXImage(tmpImageOfFragments, null);
                this.imageFile = this.getImageFile(bufferedImageFragments);
                Image ImageOfStructure = Image.getInstance(this.imageFile.getAbsolutePath());
                tmpItemTabFragmentationTable.addCell(ImageOfStructure);
                List<FragmentDataModel> hallo = tmpKey.getFragmentsOfSpecificAlgorithm(aFragmentationName);
                int iterrator = 1;
                for (FragmentDataModel tmpKey2 : hallo) {
                    PdfPCell tmpCell7 = new PdfPCell(new Paragraph("Fragment " + iterrator, FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
                    tmpItemTabFragmentationTable.addCell(tmpCell7);

                    ImageView hey = tmpKey2.getStructure();
                    javafx.scene.image.Image tmpImageOfFragments1 = hey.getImage();

                    this.bufferedImageFragments = SwingFXUtils.fromFXImage(tmpImageOfFragments1, null);
                    File fil = this.getImageFile(this.bufferedImageFragments);
                    this.imageOfFragment = Image.getInstance(fil.getAbsolutePath());
                    tmpItemTabFragmentationTable.addCell(this.imageOfFragment);
                    //tmpItemTabFragmentationTable.setSplitLate(false);
                    iterrator++;
                    fil.delete();
                }
            }
            this.pdfDocument.add(this.header);
            this.pdfDocument.add(this.space);
            this.pdfDocument.add(this.createHeaderTable(aFragmentDataModelList, aMoleculeDataModelList));
            this.pdfDocument.add(this.space);
            tmpItemTabFragmentationTable.setSplitLate(false);
            this.pdfDocument.add(tmpItemTabFragmentationTable);
            this.pdfDocument.close();
            return this.pdfDocument;
        } catch( IOException | DocumentException anException) {
            Exporter.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }

    /**
     *
     * @param aBufferedImage
     * @return
     * @throws IOException
     */
    private File getImageFile(BufferedImage aBufferedImage) throws IOException {
        File tmpImageFile = new File("Image1.png");
        //BufferedImage c = resize(aBufferedImage, 80, 80);
       //System.out.println( c.getWidth());
        int s = aBufferedImage.getWidth();
        int b = aBufferedImage.getHeight();
        //BufferedImage im = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
        //System.out.println(im.getWidth());
       // im = aBufferedImage;
        //BufferedImage buf = aBufferedImage.getSubimage(0, 0, 100, 100);
        ImageIO.write(aBufferedImage, "PNG", tmpImageFile);
        return tmpImageFile;
    }
    public  BufferedImage resize(BufferedImage img, int newW, int newH) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage dimg = new BufferedImage(newW, newH, img.getType());
        //Graphics2D g = dimg.createGraphics();
        //g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                //RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        //g.scale(100,100);
        //g.drawImage(img, 0, 0, newW, newH, 0, 0, w, h, null);
        //g.dispose();
        return dimg;
    }
    public PdfPCell cell(Image aim) {
        PdfPCell c1 = new PdfPCell(aim, false);
        return c1;
    }

        /**
         *
         * @param aFragmentDataModelList
         * @param aMoleculeDataModelList
         * @return
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
        float tmpCellLengthIntro[] = {60f, 60f};
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
