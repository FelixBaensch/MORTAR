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

    /**
     * Timestamp format for filename extensions
     */
    public static final String FILENAME_TIMESTAMP_FORMAT = "uuuu_MM_dd_HH_mm";
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
    //<editor-fold defaultstate="collapsed" desc="System and directories">
    //TODO: Change vendor name?
    /**
     * Vendor name of application to include in data directory path
     */
    public static final String MORTAR_VENDOR = "UniJena";

    /**
     * Directory name for MORTAR data
     */
    public static final String MORTAR_DATA_DIRECTORY = "MortarData";

    /**
     * Directory name for storing log files
     */
    public static final String LOG_FILES_DIRECTORY = "Logs";

    /**
     * Name for Log files
     */
    public static final String LOG_FILE_NAME = "Mortar_Log";

    /**
     * Name extension (denoting the file type) of log files
     */
    public static final String LOG_FILE_NAME_EXTENSION = ".txt";
    //</editor-fold>
    //
    //<editor-fold desc="Session">
    /**
     * MORTAR session start log entry
     */
    public static final String MORTAR_SESSION_START_FORMAT = "MORTAR %s session start";

    /**
     * MORTAR session end log entry
     */
    public static final String MORTAR_SESSION_END = "MORTAR session end";
    //</editor-fold>
    //
    //<editor-fold desc="Application version">
    /**
     * Version of application
     */
    public static final String MORTAR_VERSION = "0.0.1.0";
    //</editor-fold>
}