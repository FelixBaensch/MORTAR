/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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
package de.unijena.cheminf.mortar.message;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message
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
     * Private constructor
     */
    private Message(){
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public methods" defaultstate="collapsed">
    /**
     * Return resource string for key
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
