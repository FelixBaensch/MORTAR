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

package de.unijena.cheminf.mortar.model.depict;

import javafx.scene.image.Image;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import javafx.embed.swing.SwingFXUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

public class DepictionUtil {

    /**
     *
     * @param anAtomContainer
     * @return
     */
    public static Image depictImage(IAtomContainer anAtomContainer) { //TODO: add , int aWidth, int aHeight
        try {
            DepictionGenerator tmpGenerator = new DepictionGenerator();
            tmpGenerator = tmpGenerator.withAtomColors().withAromaticDisplay().withSize(250,250); //TODO: add .withSize(aWidth, aHeight)
            BufferedImage tmpBufferedImage = null;
            tmpBufferedImage = tmpGenerator.depict(anAtomContainer).toImg();
            return SwingFXUtils.toFXImage(tmpBufferedImage, null);
        } catch (CDKException anException) {
            //TODO: Logging
            return createErrorImage(anException.getMessage(), 250,250);
        }

    }

    /**
     * Creates an image of the given message
     *
     * @param aMessage
     * @param aWidth
     * @param aHeight
     */
    private static Image createErrorImage(String aMessage, int aWidth, int aHeight) {
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
}
