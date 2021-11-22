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

public class ExternalTool {

    private String name;
    private String version;
    private String author;
    private String license;

    public ExternalTool(String aName, String aVersion, String anAuthor, String aLicense){
        this.name = aName;
        this.version = aVersion;
        this.author = anAuthor;
        this.license = aLicense;
    }

    public String getName() {
        return name;
    }
    public String getVersion() {
        return version;
    }
    public String getAuthor() {
        return author;
    }
    public String getLicense() {
        return license;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public void setLicense(String license) {
        this.license = license;
    }
}
