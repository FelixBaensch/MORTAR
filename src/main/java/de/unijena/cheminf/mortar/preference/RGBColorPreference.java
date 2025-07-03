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

package de.unijena.cheminf.mortar.preference;

import de.unijena.cheminf.mortar.model.util.MiscUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A preference for colors.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class RGBColorPreference extends BasePreference {
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * The version of this class.
     */
    private static final String VERSION = "1.0.0.0";

    /**
     * The PreferenceType enum constant associated with this preference implementation.
     */
    private static final PreferenceType TYPE = PreferenceType.RGB_COLOR;

    /**
     * Character to separate the different color components when writing a representation of this object to file.
     * If this is altered, older versions can not be read anymore! So should you change this, hard-code the ':'
     * character in reloadVersion1000() and introduce a new version!
     */
    private static final String PERSISTENCE_VALUE_SEPARATOR = ":";

    /**
     * Upper range limit for RGB values.
     */
    private static final double RGB_MAX_VALUE = 255.0;

    /**
     * Decimal format for the returned string of getContentRepresentative().
     */
    private static final DecimalFormat FORMAT_FOR_REPRESENTATIVE = new DecimalFormat("0.##");

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(RGBColorPreference.class.getName());
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private class variables">
    /**
     * Red component of the color content, in range 0 - 1.
     */
    private double red;

    /**
     * Green component of the color content, in range 0 - 1.
     */
    private double green;

    /**
     * Blue component of the color content, in range 0 - 1.
     */
    private double blue;

    /**
     * Alpha component (transparency/opacity) of the color content, in range 0 - 1.
     */
    private double alpha;
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /**
     * Creates a new RGBColorPreference instance with the given name and color (constructed from the given red, green,
     * blue and alpha values). All double parameters must be in range 0 - 1.
     *
     * @param aName the new preference's name; leading and trailing whitespaces will not be considered;
     * @param aRedValue red component of the represented color
     * @param aGreenValue green component of the represented color
     * @param aBlueValue blue component of the represented color
     * @param anAlphaValue alpha (transparency/opacity) component of the represented color
     * @throws IllegalArgumentException if aName does not match the required pattern or is 'null' or one of the other
     * parameters is not in range 0 - 1
     */
    public RGBColorPreference(String aName, double aRedValue, double aGreenValue, double aBlueValue, double anAlphaValue)
            throws IllegalArgumentException {
        //<editor-fold defaultstate="collapsed" desc="Checks">
        if (!BasePreference.isValidName(aName)) {
            throw new IllegalArgumentException("Preference name " + aName + " does not match required pattern!");
        }
        if (!RGBColorPreference.isValidContent(aRedValue, aGreenValue, aBlueValue, anAlphaValue)) {
            throw new IllegalArgumentException("The given values " + aRedValue + ", " + aGreenValue + ", " + aBlueValue
                    + ", " +  anAlphaValue + " do not represent a valid content.");
        }
        //</editor-fold>
        this.initialize(aName, aRedValue, aGreenValue, aBlueValue, anAlphaValue);
    }

    /**
     * Creates a new RGBColorPreference instance with the given name and color (constructed from the given red, green,
     * blue and alpha values). All integer parameters must be in range 0 - 255.
     *
     * @param aName the new preference's name; leading and trailing whitespaces will not be considered;
     * @param aRedValue red component of the represented color
     * @param aGreenValue green component of the represented color
     * @param aBlueValue blue component of the represented color
     * @param anAlphaValue alpha (transparency/opacity) component of the represented color
     * @throws IllegalArgumentException if aName does not match the required pattern or is 'null' or one of the other
     * parameters is not in range 0 - 255
     */
    public RGBColorPreference(String aName, int aRedValue, int aGreenValue, int aBlueValue, int anAlphaValue)
            throws IllegalArgumentException {
        //<editor-fold defaultstate="collapsed" desc="Checks">
        if (!BasePreference.isValidName(aName)) {
            throw new IllegalArgumentException("Preference name " + aName + " does not match required pattern!");
        }
        if (!RGBColorPreference.isValidContent(aRedValue, aGreenValue, aBlueValue, anAlphaValue)) {
            throw new IllegalArgumentException("The given values " + aRedValue + ", " + aGreenValue + ", " + aBlueValue
                    + ", " +  anAlphaValue + " do not represent a valid content.");
        }
        //</editor-fold>
        double tmpRed = aRedValue / RGBColorPreference.RGB_MAX_VALUE;
        double tmpGreen = aGreenValue / RGBColorPreference.RGB_MAX_VALUE;
        double tmpBlue = aBlueValue / RGBColorPreference.RGB_MAX_VALUE;
        double tmpAlpha = anAlphaValue / RGBColorPreference.RGB_MAX_VALUE;
        this.initialize(aName, tmpRed, tmpGreen, tmpBlue, tmpAlpha);
    }

    /**
     * (Re-)instantiates a new ColorPreference object from a line-based text file.
     * <p>
     * Note: The succession of persisted variables must be exactly mirrored in writeRepresentation(PrintWriter).
     *
     * @param aReader to read the persisted representation from
     * @throws java.io.IOException if anything goes wrong
     */
    public RGBColorPreference(BufferedReader aReader) throws IOException {
        try {
            String tmpVersion = aReader.readLine();
            switch (tmpVersion) {
                case "1.0.0.0":
                    this.reloadVersion1000(aReader);
                    break;
                //case "1.0.0.1":
                //...
                //break;
                default:
                    throw new IOException("Invalid version.");
            }
        } catch (Exception anException) {
            RGBColorPreference.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            throw new IOException("Preference can not be instantiated from given reader.");
        }
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (get)">

    @Override
    public String getContentRepresentative() {
        String tmpContentRepresentative = "RGB Color [red=" + RGBColorPreference.FORMAT_FOR_REPRESENTATIVE.format(this.red)
                + ";green=" + RGBColorPreference.FORMAT_FOR_REPRESENTATIVE.format(this.green)
                + ";blue=" + RGBColorPreference.FORMAT_FOR_REPRESENTATIVE.format(this.blue)
                + ";alpha=" + RGBColorPreference.FORMAT_FOR_REPRESENTATIVE.format(this.alpha) + "]";
        return tmpContentRepresentative;
    }

    @Override
    public String getVersion() {
        return RGBColorPreference.VERSION;
    }

    @Override
    public PreferenceType getType() {
        return RGBColorPreference.TYPE;
    }

    /**
     * Returns the components of the content of this preference, i.e. the red, green, blue and alpha value, all in
     * range 0 - 1.
     *
     * @return the components of the represented color, all in range 0 - 1
     */
    public double[] getComponents() {
        double[] tmpComponents = {this.red, this.green, this.blue, this.alpha};
        return tmpComponents;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (set)">
    /**
     * Sets the components of the represented color. All parameters must be in range 0 - 1.
     *
     * @param aRedValue red component of the represented color
     * @param aGreenValue green component of the represented color
     * @param aBlueValue blue component of the represented color
     * @param anAlphaValue alpha (transparency/opacity) component of the represented color
     * @throws IllegalArgumentException if a parameter is not in range 0 - 1
     */
    public void setContent(double aRedValue, double aGreenValue, double aBlueValue, double anAlphaValue)
            throws IllegalArgumentException {
        if (!RGBColorPreference.isValidContent(aRedValue, aGreenValue, aBlueValue, anAlphaValue)) {
            throw new IllegalArgumentException("The given values " + aRedValue + ", " + aGreenValue + ", " + aBlueValue
                    + ", " +  anAlphaValue + " do not represent a valid content.");
        }
        this.red = aRedValue;
        this.green = aGreenValue;
        this.blue = aBlueValue;
        this.alpha = anAlphaValue;
    }

    /**
     * Sets the components of the represented color. All parameters must be in range 0 - 255.
     *
     * @param aRedValue red component of the represented color
     * @param aGreenValue green component of the represented color
     * @param aBlueValue blue component of the represented color
     * @param anAlphaValue alpha (transparency/opacity) component of the represented color
     * @throws IllegalArgumentException if a parameter is not in range 0 - 255
     */
    public void setContent(int aRedValue, int aGreenValue, int aBlueValue, int anAlphaValue)
            throws IllegalArgumentException {
        if (!RGBColorPreference.isValidContent(aRedValue, aGreenValue, aBlueValue, anAlphaValue)) {
            throw new IllegalArgumentException("The given values " + aRedValue + ", " + aGreenValue + ", " + aBlueValue
                    + ", " +  anAlphaValue + " do not represent a valid content.");
        }
        this.red = aRedValue / RGBColorPreference.RGB_MAX_VALUE;
        this.green = aGreenValue / RGBColorPreference.RGB_MAX_VALUE;
        this.blue = aBlueValue / RGBColorPreference.RGB_MAX_VALUE;
        this.alpha = anAlphaValue / RGBColorPreference.RGB_MAX_VALUE;
    }

    /**
     * Sets the opacity of the represented color. Parameter must be in range 0 - 1.
     *
     * @param anAlphaValue the new alpha value
     * @throws IllegalArgumentException if anAlphaValue is not in range 0 - 1
     */
    public void setAlpha(double anAlphaValue) throws IllegalArgumentException {
        if (anAlphaValue < 0.0 || anAlphaValue > 1.0) {
            throw new IllegalArgumentException("The given alpha value " + anAlphaValue + " is not valid.");
        }
        this.alpha = anAlphaValue;
    }

    /**
     * Sets the opacity of the represented color. Parameter must be in range 0 - 255.
     *
     * @param anAlphaValue the new alpha value
     * @throws IllegalArgumentException if anAlphaValue is not in range 0 - 255
     */
    public void setAlpha(int anAlphaValue) throws IllegalArgumentException {
        if (anAlphaValue < 0 || anAlphaValue > RGBColorPreference.RGB_MAX_VALUE) {
            throw new IllegalArgumentException("The given alpha value " + anAlphaValue + " is not valid.");
        }
        this.alpha = anAlphaValue / RGBColorPreference.RGB_MAX_VALUE;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public methods">

    @Override
    public RGBColorPreference copy() {
        RGBColorPreference tmpCopy = new RGBColorPreference(this.name,
                this.red,
                this.green,
                this.blue,
                alpha);
        tmpCopy.guid = this.guid;
        return tmpCopy;
    }

    @Override
    public void writeRepresentation(PrintWriter aPrintWriter) {
        aPrintWriter.println(RGBColorPreference.VERSION);
        aPrintWriter.println(this.name);
        aPrintWriter.println(this.guid);
        String tmpComponents = this.red + RGBColorPreference.PERSISTENCE_VALUE_SEPARATOR
                + this.green + RGBColorPreference.PERSISTENCE_VALUE_SEPARATOR
                + this.blue + RGBColorPreference.PERSISTENCE_VALUE_SEPARATOR
                + this.alpha;
        aPrintWriter.println(tmpComponents);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "_'" + this.name + "'_" + "Content:" + this.getContentRepresentative();
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * Tests whether the given values would be valid contents of this preference type, all parameters must be in range
     * 0 - 1.
     *
     * @param aRedValue red component of the represented color
     * @param aGreenValue green component of the represented color
     * @param aBlueValue blue component of the represented color
     * @param anAlphaValue alpha (transparency/opacity) component of the represented color
     * @return true if the given values would form a valid content
     */
    public static boolean isValidContent(double aRedValue, double aGreenValue, double aBlueValue, double anAlphaValue) {
        double[] tmpParams = {aRedValue, aGreenValue, aBlueValue, anAlphaValue};
        for (double tmpParameter : tmpParams) {
            if (tmpParameter < 0.0 || tmpParameter > 1.0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether the given values would be valid contents of this preference type, all parameters must be in range
     * 0 - 255.
     *
     * @param aRedValue red component of the represented color
     * @param aGreenValue green component of the represented color
     * @param aBlueValue blue component of the represented color
     * @param anAlphaValue alpha (transparency/opacity) component of the represented color
     * @return true if the given values would form a valid content
     */
    public static boolean isValidContent(int aRedValue, int aGreenValue, int aBlueValue, int anAlphaValue) {
        int[] tmpParams = {aRedValue, aGreenValue, aBlueValue, anAlphaValue};
        for (int tmpParameter : tmpParams) {
            if (tmpParameter < 0 || tmpParameter > RGBColorPreference.RGB_MAX_VALUE) {
                return false;
            }
        }
        return true;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private methods">
    /**
     * Creates a new RGBColorPreference instance with the given name and color (constructed from the given red, green,
     * blue and alpha values). All double parameters must be in range 0 - 1 and aName must match the specified pattern
     * for preference names.
     * <br>No checks are performed! This must be done by calling constructor!
     *
     * @param aName the new preference's name; leading and trailing whitespaces will not be considered;
     * @param aRedValue red component of the represented color
     * @param aGreenValue green component of the represented color
     * @param aBlueValue blue component of the represented color
     * @param anAlphaValue alpha (transparency/opacity) component of the represented color
     */
    private void initialize(String aName, double aRedValue, double aGreenValue, double aBlueValue, double anAlphaValue) {
        this.name = aName.trim();
        this.guid = MiscUtil.getGloballyUniqueID();
        this.red = aRedValue;
        this.green = aGreenValue;
        this.blue = aBlueValue;
        this.alpha = anAlphaValue;
    }

    /**
     * (Re-)instantiates a new RGBColorPreference object of version 1.0.0.0 from a line-based text file.
     */
    private void reloadVersion1000(BufferedReader aReader) throws IOException {
        Objects.requireNonNull(aReader, "Reader must not be null");
        this.name = aReader.readLine();
        this.guid = aReader.readLine();
        String[] tmpColorComponents = aReader.readLine().split(":");
        if (tmpColorComponents.length != 4) {
            throw new IOException("Invalid number of color components in persisted representation. Expected 4 components but found " + tmpColorComponents.length + ".");
        }
        this.red = Double.parseDouble(tmpColorComponents[0]);
        this.green = Double.parseDouble(tmpColorComponents[1]);
        this.blue = Double.parseDouble(tmpColorComponents[2]);
        this.alpha = Double.parseDouble(tmpColorComponents[3]);
    }
    //</editor-fold>
}
