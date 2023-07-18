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

package de.unijena.cheminf.mortar.model.Fingerprints;

import de.unijena.cheminf.fragmentFingerprinter.FragmentFingerprinter;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FragmentFingerprinterWrapper implements IMortarFingerprinter {
    public static enum FingerprintTyp {
        BIT_FINGERPRINTS,
        COUNT_FINGERPRINTS;
    }
    public static final String FINGERPRINTER_NAME = "Fragment Fingerprinter";
    public FragmentFingerprinter fragmentFingerprinterInstance;
    public static final FingerprintTyp COUNT_FINGERPRINTS_DEFAULT = FingerprintTyp.COUNT_FINGERPRINTS;
    private SimpleEnumConstantNameProperty fingerprintTyp;
    private SimpleIntegerProperty fingerprintDimensionality;
    private SimpleIntegerProperty fingerprintFrequencyThreshold;
    private final int defaultFingerprintDimensionalityValue;
    private final int DEFAULT_FINGERPRINT_FREQUENCY_THRESHOLD = 1;
    private int dimensionality;
    private final List<Property> settings;
    private List<FragmentDataModel> fragmentDataModelList;
    private final HashMap<String, String> settingNameTooltipTextMap;
    private final Logger logger = Logger.getLogger(FragmentFingerprinterWrapper.class.getName());

    public FragmentFingerprinterWrapper() {
        int tmpNumberOfSettings = 3;
        this.settings = new ArrayList<>(tmpNumberOfSettings);
        int tmpInitialCapacityForSettingNameToolTipTextMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpNumberOfSettings,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap<>(tmpInitialCapacityForSettingNameToolTipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.fingerprintTyp = new SimpleEnumConstantNameProperty(this, "Fragment Fingerprint Typ",
                FragmentFingerprinterWrapper.COUNT_FINGERPRINTS_DEFAULT.name(), FragmentFingerprinterWrapper.FingerprintTyp.class) {
            @Override
            public void set(String newValue) {
                super.set(newValue);
            }
        };
        this.settings.add(this.fingerprintTyp);
        this.settingNameTooltipTextMap.put(this.fingerprintTyp.getName(), Message.get("FragmentFingerprinterGenerator.fingerprintTyp.tooltip"));
        this.defaultFingerprintDimensionalityValue = 1; // TODO
        this.fingerprintDimensionality = new SimpleIntegerProperty(this, "Fragment Fingerprint Dimensionality") {
            @Override
            public void set(int newValue) {
                super.set(newValue);
            }
        };
        this.settings.add(this.fingerprintDimensionality);
        this.settingNameTooltipTextMap.put(this.fingerprintDimensionality.getName(), Message.get("FragmentFingerprinterGenerator.fingerprintDimensionality.tooltip"));
        this.fingerprintFrequencyThreshold = new SimpleIntegerProperty(this, "Fragment Fingerprint Frequency Threshold", this.DEFAULT_FINGERPRINT_FREQUENCY_THRESHOLD);
        this.settings.add(this.fingerprintFrequencyThreshold);
        this.settingNameTooltipTextMap.put(this.fingerprintFrequencyThreshold.getName(), Message.get("FragmentFingerprinterGenerator.fingerprintFrequencyThreshold.tooltip"));
    }
    public String getFingerprintTyp() {
        return this.fingerprintTyp.get();
    }
    public int getFingerprintDimensionality() {
        return this.fingerprintDimensionality.get();
    }
    public int getFingerprintFrequencyThreshold() {
        return this.fingerprintFrequencyThreshold.get();
    }
    public void setFingerprintDimensionality(int aNumberOfFingerprintDimensionality) {
        this.fingerprintDimensionality.set(aNumberOfFingerprintDimensionality);
    }
    public void setFingerprintFrequencyThreshold(int aFingerprintFrequencyThreshold) {
        this.fingerprintFrequencyThreshold.set(aFingerprintFrequencyThreshold);
    }
    public void setFingerprintTyp(String aTypName){
        Objects.requireNonNull(aTypName, "Given option name is null.");
        FragmentFingerprinterWrapper.FingerprintTyp tmpConstant = FragmentFingerprinterWrapper.FingerprintTyp.valueOf(aTypName);
        this.setFingerprintTyp(tmpConstant);
    }
    public void setFingerprintTyp(FingerprintTyp aTyp) {
        Objects.requireNonNull(aTyp, "Given option is null.");
        this.fingerprintTyp.set(aTyp.name());
    }
    @Override
    public List<Property> settingsProperties() {
        return this.settings;
    }

    @Override
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return this.settingNameTooltipTextMap;
    }

    @Override
    public String getFingerprinterName() {
        return FragmentFingerprinterWrapper.FINGERPRINTER_NAME;
    }

    @Override
    public void restoreDefaultSettings(int defaultFingerprintDimensionalityValue) { // TODO maybe add a parameter to the method
        this.fingerprintTyp.set(FingerprintTyp.COUNT_FINGERPRINTS.name());
        this.fingerprintDimensionality.set(defaultFingerprintDimensionalityValue);
        this.fingerprintFrequencyThreshold.set(this.DEFAULT_FINGERPRINT_FREQUENCY_THRESHOLD);

    }
    public int[][] getFragmentFingerprints(List<MoleculeDataModel> aMoleculeDataModelList, List<FragmentDataModel> aFragmentDataModelList, String aFragmentationName,
                                           String aFingerintTypEnum) {
       int tmpMaximumFingerprintDimensionalityValue = getFingerprintDimensionality();
        if(tmpMaximumFingerprintDimensionalityValue == aFragmentDataModelList.size()) {
            System.out.println("size full");
        } else {
            System.out.println("nicht full");
        }
        String tmpCount = "Count fingerprints";
        String tmpBit = "Bit fingerprints";
        String tmpSortProperty = "absoluteFrequency";
        CollectionUtil.sortGivenFragmentListByPropertyAndSortType(aFragmentDataModelList,tmpSortProperty, "DESCENDING");
        List<FragmentDataModel> tmpSubList = aFragmentDataModelList.subList(0,tmpMaximumFingerprintDimensionalityValue);
        int[][] tmpDataMatrix =  new int[aMoleculeDataModelList.size()][tmpSubList.size()];
        ArrayList<String> tmpKeyFragmentsToGenerateBitFingerprints = new ArrayList<>(tmpSubList.size());
        System.out.println(this.fingerprintFrequencyThreshold.get() + "------------gesetzer fingerprint threshold");
        for(FragmentDataModel tmpFragmentDataModel : tmpSubList) {
            if(tmpFragmentDataModel.getAbsoluteFrequency() >= this.fingerprintFrequencyThreshold.get()) {
                tmpKeyFragmentsToGenerateBitFingerprints.add(tmpFragmentDataModel.getUniqueSmiles());
            }
        }
        if(tmpKeyFragmentsToGenerateBitFingerprints.isEmpty()) {
            for(FragmentDataModel tmpFragmentDataModel : tmpSubList) {
                tmpKeyFragmentsToGenerateBitFingerprints.add(tmpFragmentDataModel.getUniqueSmiles());
            }
        }
        FragmentFingerprinter tmpFragmentFingerprinter = new FragmentFingerprinter(tmpKeyFragmentsToGenerateBitFingerprints);
        int tmpIterator = 0;
        for(MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
            ArrayList<String> tmpMoleculeFragmentsToGenerateBitFingerprints = new ArrayList<>(); // TODO add initial size
            HashMap<String, Integer> tmpFragmentSmilesToFrequencyMapToGenerateCountFingerprints = new HashMap<>();
            if(!tmpMoleculeDataModel.hasMoleculeUndergoneSpecificFragmentation(aFragmentationName)){
                continue;
            }
            List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificAlgorithm(aFragmentationName);
            for(FragmentDataModel tmpFragmentDataModel : tmpFragmentList) {
                if(this.getFingerprintTyp().equals(FragmentFingerprinterWrapper.FingerprintTyp.BIT_FINGERPRINTS.name()) || tmpBit.equals(aFingerintTypEnum)) {
                    tmpMoleculeFragmentsToGenerateBitFingerprints.add(tmpFragmentDataModel.getUniqueSmiles());
                } else{
                   tmpFragmentSmilesToFrequencyMapToGenerateCountFingerprints.put(tmpFragmentDataModel.getUniqueSmiles(),
                           tmpMoleculeDataModel.getFragmentFrequencyOfSpecificAlgorithm(aFragmentationName).get(tmpFragmentDataModel.getUniqueSmiles()));
                }
            }
            if(this.getFingerprintTyp().equals(FragmentFingerprinterWrapper.FingerprintTyp.BIT_FINGERPRINTS.name()) || tmpBit.equals(aFingerintTypEnum)) {
                tmpFragmentFingerprinter.getBitFingerprint(tmpMoleculeFragmentsToGenerateBitFingerprints);
                tmpDataMatrix[tmpIterator] = tmpFragmentFingerprinter.getBitArray(tmpMoleculeFragmentsToGenerateBitFingerprints);
            } else {
                tmpFragmentFingerprinter.getCountFingerprint(tmpFragmentSmilesToFrequencyMapToGenerateCountFingerprints);
                tmpDataMatrix[tmpIterator] = tmpFragmentFingerprinter.getCountArray(tmpFragmentSmilesToFrequencyMapToGenerateCountFingerprints);
            }
            tmpIterator++;
        }
     //   System.out.println(java.util.Arrays.toString(tmpDataMatrix[9]));
        return tmpDataMatrix;
    }
}
