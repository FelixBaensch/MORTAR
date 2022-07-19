/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2022  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.model.depict;

import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import javafx.scene.image.Image;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import javafx.embed.swing.SwingFXUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Util class for depiction
 *
 * @author Felix Baensch, Jonas Schaub
 * @version 1.0.0.0
 */
public class DepictionUtil {

    //<editor-fold desc="private static final class variables" defaultstate="collapsed">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(DepictionUtil.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="public static methods" defaultstate="collapsed">
    /**
     * Creates and returns an Image of the given AtomContainer
     *
     * @param anAtomContainer IAtomContainer
     * @param aWidth double for image width
     * @param aHeight  double for image height
     * @return Image of 2D structure of IAtomContainer
     */
    public static Image depictImage(IAtomContainer anAtomContainer, double aWidth, double aHeight) {
        return depictImageWithZoom(anAtomContainer, 1.0, aWidth, aHeight);
    }
    //
    /**
     * Creates and returns an Image of the AtomContainer with given height and default width (250.0).
     *
     * @param anAtomContainer IAtomContainer
     * @param aHeight double
     * @return Image of 2D structure of IAtomContainer
     */
    public static Image depictImageWithHeight(IAtomContainer anAtomContainer, double aHeight) {
        return depictImageWithZoom(anAtomContainer, 1.0, BasicDefinitions.DEFAULT_IMAGE_WIDTH_DEFAULT, aHeight);
    }
    //
    /**
     * Creates and returns an Image of the AtomContainer with given width and default height (250.0).
     *
     * @param anAtomContainer IAtomContainer
     * @param aWidth double
     * @return Image of 2D structure of IAtomContainer
     */
    public static Image depictImageWithWidth(IAtomContainer anAtomContainer, double aWidth) {
        return depictImageWithZoom(anAtomContainer, 1.0, aWidth, BasicDefinitions.DEFAULT_IMAGE_HEIGHT_DEFAULT);
    }
    //
    /**
     * Creates and returns an Image of the AtomContainer with any zoom factor and default width (250.0) and height (250.0)
     *
     * @param anAtomContainer IAtomContainer
     * @param aZoom double
     * @return Image of 2D structure of IAtomContainer
     */
    public static Image depictImageWithZoom(IAtomContainer anAtomContainer, double aZoom){
        return depictImageWithZoom(anAtomContainer, aZoom, BasicDefinitions.DEFAULT_IMAGE_WIDTH_DEFAULT, BasicDefinitions.DEFAULT_IMAGE_HEIGHT_DEFAULT);
    }
    /**
     * Creates and returns an Image of the AtomContainer with any zoom factor and given width and height.
     *
     * @param anAtomContainer IAtomContainer
     * @param aZoom double
     * @param aWidth double
     * @param aHeight double
     * @return Image of 2D structure of IAtomContainer
     */
    public static Image depictImageWithZoom(IAtomContainer anAtomContainer, double aZoom, double aWidth, double aHeight) {
        return DepictionUtil.depictImageWithZoomAndFillToFit(anAtomContainer, aZoom, aWidth, aHeight, false);
    }
    //
    /**
     * Creates and returns an Image of the AtomContainer with any zoom factor and given width and height and fill to fit.
     *
     * @param anAtomContainer IAtomContainer
     * @param aZoom double
     * @param aWidth double
     * @param aHeight double
     * @param fillToFit boolean Resize depictions to fill all available space (only if a size is specified)
     * @return Image of 2D structure of IAtomContainer
     */
    public static Image depictImageWithZoomAndFillToFit(IAtomContainer anAtomContainer, double aZoom, double aWidth, double aHeight, boolean fillToFit) {
        try {
            BufferedImage tmpBufferedImage = DepictionUtil.depictBufferedImageWithZoom(anAtomContainer, aZoom, aWidth, aHeight, fillToFit);
            return SwingFXUtils.toFXImage(tmpBufferedImage, null);
        } catch (CDKException | NullPointerException anException) {
            DepictionUtil.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return DepictionUtil.depictErrorImage(anException.getMessage(), 250,250);
        }
    }
    //
    /**
     * Creates and returns an image of the given message.
     *
     * @param aMessage String
     * @param aWidth int
     * @param aHeight int
     * @return Image of given String
     */
    public static Image depictErrorImage(String aMessage, int aWidth, int aHeight) {
        String tmpMessage;
        if (aMessage == null) {
            tmpMessage = "Error";
        } else {
            tmpMessage = aMessage;
        }
        BufferedImage tmpBufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tmpGraphic = tmpBufferedImage.createGraphics();
        Font tmpFont = new Font("Arial", Font.PLAIN, 20);
        tmpGraphic.setFont(tmpFont);
        tmpGraphic.dispose();
        tmpBufferedImage = new BufferedImage(aWidth, aHeight, BufferedImage.TYPE_INT_ARGB);
        tmpGraphic = tmpBufferedImage.createGraphics();
        tmpGraphic.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        tmpGraphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        tmpGraphic.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        tmpGraphic.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        tmpGraphic.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        tmpGraphic.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        tmpGraphic.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        tmpGraphic.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        tmpGraphic.setFont(tmpFont);
        FontMetrics tmpFontMetrics = tmpGraphic.getFontMetrics();
        tmpGraphic.setColor(Color.BLACK);
        tmpGraphic.drawString(tmpMessage, 0, tmpFontMetrics.getAscent());
        tmpGraphic.dispose();
        tmpGraphic.drawImage(tmpBufferedImage, 0, 0, null);
        return SwingFXUtils.toFXImage(tmpBufferedImage, null);
    }
    //
    /**
     * Depicts and returns an image with the given text below
     *
     * @param anAtomContainer IAtomContainer
     * @param aZoom double
     * @param aWidth double
     * @param aHeight double
     * @param aString String
     * @return Image of 2D structure of IAtomContainer with given String below
     */
    public static Image depictImageWithText(IAtomContainer anAtomContainer, double aZoom, double aWidth, double aHeight, String aString){
        try{
            BufferedImage tmpMolBufferedImage = DepictionUtil.depictBufferedImageWithZoom(anAtomContainer, aZoom, aWidth, aHeight, false);
            BufferedImage tmpBufferedImage = new BufferedImage(tmpMolBufferedImage.getWidth(), tmpMolBufferedImage.getHeight() + BasicDefinitions.DEFAULT_IMAGE_TEXT_DISTANCE, BufferedImage.TRANSLUCENT);
            Graphics2D tmpGraphics2d = tmpBufferedImage.createGraphics();
            tmpGraphics2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            tmpGraphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            tmpGraphics2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            tmpGraphics2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
            tmpGraphics2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            tmpGraphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            tmpGraphics2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            tmpGraphics2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            tmpGraphics2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
            tmpGraphics2d.drawImage(tmpMolBufferedImage, 0, 0,null);
            tmpGraphics2d.setColor(Color.BLACK);
            tmpGraphics2d.setFont(new Font("Calibri", Font.BOLD, 20));
            FontMetrics tmpFontMetric = tmpGraphics2d.getFontMetrics();
            int tmpTextWidth = tmpFontMetric.stringWidth(aString);
            tmpGraphics2d.drawString(aString, (tmpBufferedImage.getWidth() / 2) - tmpTextWidth / 2, tmpBufferedImage.getHeight());
            tmpGraphics2d.dispose();
            return SwingFXUtils.toFXImage(tmpBufferedImage, null);
        } catch (CDKException anException){
            DepictionUtil.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return DepictionUtil.depictErrorImage(anException.getMessage(), 250,250);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="private static methods" defaultstate="collapsed">
    /**
     * Creates and returns a BufferedImage of given IAtomContainer
     *
     * @param anAtomContainer IAtomContainer
     * @param aZoom double
     * @param aWidth double
     * @param aHeight double
     * @return BufferedImage of given IAtomContainer
     * @throws CDKException
     */
    private static BufferedImage depictBufferedImageWithZoom(IAtomContainer anAtomContainer, double aZoom, double aWidth, double aHeight, boolean fillToFit) throws CDKException {
        DepictionGenerator tmpGenerator = new DepictionGenerator();
        if(fillToFit){
            tmpGenerator = tmpGenerator.withAtomColors().withAromaticDisplay().withSize(aWidth,aHeight).withFillToFit();
        }
        else {
            tmpGenerator = tmpGenerator.withAtomColors().withAromaticDisplay().withSize(aWidth,aHeight).withZoom(aZoom);
        }
        return tmpGenerator.depict(anAtomContainer).toImg();
    }
    //</editor-fold>
}
