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

package de.unijena.cheminf.mortar.gui.util;

/**
 * Wrapper class to visualize information about external tools used in MORTAR in the AboutView.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class ExternalTool {
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Name of the tool.
     */
    private String name;
    /**
     * Number string of the used version of the tool.
     */
    private String version;
    /**
     * Author(s) of the tool.
     */
    private String author;
    /**
     * License of the tool.
     */
    private String license;
    //</editor-fold>
    //
    /**
     * Constructor.
     *
     * @param aName Name of tool
     * @param aVersion Version used
     * @param anAuthor Author
     * @param aLicense License used
     */
    public ExternalTool(String aName, String aVersion, String anAuthor, String aLicense) {
        this.name = aName;
        this.version = aVersion;
        this.author = anAuthor;
        this.license = aLicense;
    }
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns name as String.
     *
     * @return {@link String}
     */
    public String getName() {
        return name;
    }
    //
    /**
     * Returns version number as String.
     *
     * @return {@link String}
     */
    public String getVersion() {
        return version;
    }
    //
    /**
     * Returns author(s) name.
     *
     * @return {@link String}
     */
    public String getAuthor() {
        return author;
    }
    //
    /**
     * Returns license.
     *
     * @return {@link String}
     */
    public String getLicense() {
        return license;
    }
    //
    /**
     * Sets given String as name.
     *
     * @param name {@link String}
     */
    public void setName(String name) {
        this.name = name;
    }
    //
    /**
     * Sets given String as version.
     *
     * @param version {@link String}
     */
    public void setVersion(String version) {
        this.version = version;
    }
    //
    /**
     * Sets given String as author.
     *
     * @param author {@link String}
     */
    public void setAuthor(String author) {
        this.author = author;
    }
    //
    /**
     * Sets given String as license.
     *
     * @param license {@link String}
     */
    public void setLicense(String license) {
        this.license = license;
    }
    //</editor-fold>
}
