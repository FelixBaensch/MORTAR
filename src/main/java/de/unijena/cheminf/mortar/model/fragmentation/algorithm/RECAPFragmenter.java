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

package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

import javafx.beans.property.Property;

import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;
import java.util.Map;

/**
 * Wrapper class that makes the
 * <a href="https://doi.org/10.1186/s13321-017-0225-z">RECAP - Retrosynthetic Combinatorial Analysis Procedure</a>
 * available in MORTAR, using an in-house
 * CDK implementation of the algorithm.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class RECAPFragmenter implements IMoleculeFragmenter {
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
