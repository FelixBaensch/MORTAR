/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2022  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.gui.views;

import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;

import java.util.List;

/**
 * Interface for implementing TableViews for molecule data. It is necessary for the sorting method
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public interface IDataTableView {

    /**
     * Returns a list of MoleculeDataModel
     *
     * @return List of MoleculeDataModel
     */
    public List<MoleculeDataModel> getItemsList();
    //
    /**
     * Sets given list
     *
     * @param aListOfFragments List
     */
    public void setItemsList(List<MoleculeDataModel> aListOfFragments);
    //
    /**
     * Adds a change listener to the height property of table view which sets the height for structure images to
     * each MoleculeDataModel/FragmentDataModel object of the items list and refreshes the table view
     *
     * @param aSettingsContainer SettingsContainer
     */
    public void addTableViewHeightListener(SettingsContainer aSettingsContainer);
}
