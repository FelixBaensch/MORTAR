/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2024  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.message;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message.
 *
 * @author Felix Baensch, Jonas Schaub
 * @version 1.0.0.0
 */
public class Message {
    //<editor-fold defaultstate="collapsed" desc="Public static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(Message.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Private class variables" defaultstate="collapsed">
    /**
     * Resource bundle name
     */
    private static final String BUNDLE_NAME = "de.unijena.cheminf.mortar.message.Message";
    /**
     * Locale default
     */
    private static final Locale LOCALE_DEFAULT = Locale.getDefault();
    /**
     * Resource bundle
     */
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, LOCALE_DEFAULT);
    //</editor-fold>
    //
    //<editor-fold desc="Private constructor" defaultstate="collapsed">
    /**
     * Private constructor.
     */
    private Message(){
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public methods" defaultstate="collapsed">
    /**
     * Return resource string for key.
     *
     * @param aKey Key
     * @return Resource string for key
     */
    public static String get(String aKey){
        try{
            return RESOURCE_BUNDLE.getString(aKey).trim();
        } catch (MissingResourceException | NullPointerException anException){
            Message.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return "Key '" + aKey + "' not found.";
        }
    }
    //</editor-fold>
}
