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

package de.unijena.cheminf.mortar.model.util;

import java.util.regex.Pattern;

/**
 * Basic definitions of the model or application
 *
 * @author Jonas Schaub, Achim Zielesny
 */
public final class BasicDefinitions {
    //<editor-fold defaultstate="collapsed" desc="Basic patterns and formats">
    /**
     * Regex pattern for non-word or non-numeric characters
     */
    public static final Pattern NON_WORDNUMERIC_CHARACTER_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

    /**
     * Standard timestamp format
     */
    public static final String STANDARD_TIMESTAMP_FORMAT = "yyyy/MM/dd - HH:mm:ss";
    //</editor-fold>
    //
    // <editor-fold defaultstate="collapsed" desc="Buffer">
    /**
     * Buffer size (64 kByte = 65536, 256 kByte = 262144, 512 kByte = 524288, 1
     * MByte = 1048576 Byte)
     */
    public static final int BUFFER_SIZE = 65536;
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="System">
    /**
     * Path to local data of user
     */
    public static final String LOCAL_USER_DATA_DIRECTORY_PATH = System.getenv("APPDATA");

    /**
     * User directory
     */
    public static final String USER_DIRECTORY_PATH = System.getProperty("user.dir");

    /**
     * Directory name for MORTAR data
     */
    public static final String MORTAR_DATA_DIRECTORY = "MortarData";
    //</editor-fold>
}