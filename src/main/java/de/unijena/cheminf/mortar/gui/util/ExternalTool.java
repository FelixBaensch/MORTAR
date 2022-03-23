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

package de.unijena.cheminf.mortar.gui.util;

/**
 * Wrapper class to visualize information about external tools used in MORTAR in the AboutView
 *
 * @author Felix Baensch
 * @version 1.0.0
 */
public class ExternalTool {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Name of the tool
     */
    private String name;
    /**
     * Number string of the used version of the tool
     */
    private String version;
    /**
     * Author(s) of the tool
     */
    private String author;
    /**
     * License of the tool
     */
    private String license;
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param aName Name of tool
     * @param aVersion Version used
     * @param anAuthor Author
     * @param aLicense License used
     */
    public ExternalTool(String aName, String aVersion, String anAuthor, String aLicense){
        this.name = aName;
        this.version = aVersion;
        this.author = anAuthor;
        this.license = aLicense;
    }
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns name as String
     *
     * @return {@link String}
     */
    public String getName() {
        return name;
    }
    //
    /**
     * Returns version number as String
     *
     * @return {@link String}
     */
    public String getVersion() {
        return version;
    }
    //
    /**
     * Returns author(s) name
     *
     * @return {@link String}
     */
    public String getAuthor() {
        return author;
    }
    //
    /**
     * Returns license
     *
     * @return {@link String}
     */
    public String getLicense() {
        return license;
    }
    //
    /**
     * Sets given String as name
     * @param name {@link String}
     */
    public void setName(String name) {
        this.name = name;
    }
    //
    /**
     * Sets given String as version
     *
     * @param version {@link String}
     */
    public void setVersion(String version) {
        this.version = version;
    }
    //
    /**
     * Sets given String as author
     *
     * @param author {@link String}
     */
    public void setAuthor(String author) {
        this.author = author;
    }
    //
    /**
     * Sets given String as license
     *
     * @param license {@link String}
     */
    public void setLicense(String license) {
        this.license = license;
    }
    //</editor-fold>
}
