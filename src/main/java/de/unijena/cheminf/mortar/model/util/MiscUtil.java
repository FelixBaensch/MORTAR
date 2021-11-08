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

package de.unijena.cheminf.mortar.model.util;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Miscellaneous utilities
 *
 * @author Achim Zielesny, Jonas Schaub
 */
public final class MiscUtil {
    //<editor-fold defaultstate="collapsed" desc="Public static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(MiscUtil.class.getName());
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * String with globally unique ID
     *
     * @return String with globally unique ID
     */
    public static String getGloballyUniqueID() {
        return BasicDefinitions.NON_WORDNUMERIC_CHARACTER_PATTERN.matcher((UUID.randomUUID()).toString()).replaceAll("");
    }

    /**
     * Returns current timestamp in standard form (see code)
     *
     * @return Current timestamp in standard form or null if none could be
     * created
     */
    public static String getTimestampInStandardFormat() {
        try {
            SimpleDateFormat tmpSimpleDateFormat = new SimpleDateFormat(BasicDefinitions.STANDARD_TIMESTAMP_FORMAT);
            Instant tmpInstant = Instant.now();
            Date tmpDate = Date.from(tmpInstant);
            return tmpSimpleDateFormat.format(tmpDate);
        } catch (Exception anException) {
            MiscUtil.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }
    //</editor-fold>
}