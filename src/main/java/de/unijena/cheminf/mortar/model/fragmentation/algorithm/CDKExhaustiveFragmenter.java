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

package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;
import javafx.beans.property.Property;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;
import java.util.Map;

public class CDKExhaustiveFragmenter implements IMoleculeFragmenter {
    @Override
    public List<Property<?>> settingsProperties() {
        return null;
    }

    @Override
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return null;
    }

    @Override
    public Map<String, String> getSettingNameToDisplayNameMap() {
        return null;
    }

    @Override
    public String getFragmentationAlgorithmName() {
        return null;
    }

    @Override
    public String getFragmentationAlgorithmDisplayName() {
        return null;
    }

    @Override
    public FragmentSaturationOption getFragmentSaturationSetting() {
        return null;
    }

    @Override
    public SimpleIDisplayEnumConstantProperty fragmentSaturationSettingProperty() {
        return null;
    }

    @Override
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException {

    }

    @Override
    public IMoleculeFragmenter copy() {
        return null;
    }

    @Override
    public void restoreDefaultSettings() {

    }

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        return null;
    }

    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        return false;
    }

    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
        return false;
    }

    @Override
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
        return false;
    }

    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        return null;
    }
}
