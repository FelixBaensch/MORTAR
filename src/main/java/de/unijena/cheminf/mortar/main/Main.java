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

package de.unijena.cheminf.mortar.main;

/**
 * Since the Java launcher checks if the main class extends javafx.application.Application,
 * and in that case it requires the JavaFX runtime available as modules (not as jars), a
 * possible workaround to make it work, should be adding a new Main class that will be the
 * main class of your project, and that class will be the one that calls your JavaFX
 * Application class.
 * See José Pereda's comment on https://stackoverflow.com/questions/52569724/javafx-11-create-a-jar-file-with-gradle
 * (retrieved December 18, 2019) for more details.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class Main {
    /**
     * Main method to start the application
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
