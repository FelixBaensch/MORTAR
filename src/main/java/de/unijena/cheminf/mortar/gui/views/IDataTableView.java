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

package de.unijena.cheminf.mortar.gui.views;

import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;

import java.util.List;

/**
 * Interface for implementing TableViews for molecule data. It is necessary for the sorting method.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public interface IDataTableView {
    /**
     * Returns a list of MoleculeDataModel.
     *
     * @return List of MoleculeDataModel
     */
    List<MoleculeDataModel> getItemsList();
    //
    /**
     * Sets given list.
     *
     * @param aListOfFragments List
     */
    void setItemsList(List<MoleculeDataModel> aListOfFragments);
    //
    /**
     * Adds a change listener to the height property of table view which sets the height for structure images to
     * each MoleculeDataModel/FragmentDataModel object of the items list and refreshes the table view.
     *
     * @param aSettingsContainer SettingsContainer
     */
    void addTableViewHeightListener(SettingsContainer aSettingsContainer);
}
