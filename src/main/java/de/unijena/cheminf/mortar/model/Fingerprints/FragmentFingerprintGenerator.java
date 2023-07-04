package de.unijena.cheminf.mortar.model.Fingerprints;

import de.unijena.cheminf.fragmentFingerprinter.FragmentFingerprinter;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.clustering.Art2aClusteringAlgorithm;
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

public class FragmentFingerprintGenerator implements IMoleculeFingerprinter {
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
    private final Logger logger = Logger.getLogger(FragmentFingerprintGenerator.class.getName());

    public FragmentFingerprintGenerator() {
        int tmpNumberOfSettings = 3;
        this.settings = new ArrayList<>(tmpNumberOfSettings);
        int tmpInitialCapacityForSettingNameToolTipTextMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpNumberOfSettings,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap<>(tmpInitialCapacityForSettingNameToolTipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.fingerprintTyp = new SimpleEnumConstantNameProperty(this, "Fragment Fingerprint Typ",
                FragmentFingerprintGenerator.COUNT_FINGERPRINTS_DEFAULT.name(), FragmentFingerprintGenerator.FingerprintTyp.class) {
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
        FragmentFingerprintGenerator.FingerprintTyp tmpConstant = FragmentFingerprintGenerator.FingerprintTyp.valueOf(aTypName);
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
        return FragmentFingerprintGenerator.FINGERPRINTER_NAME;
    }

    @Override
    public void restoreDefaultSettings() {
        this.fingerprintTyp.set(FingerprintTyp.COUNT_FINGERPRINTS.name());
        this.fingerprintDimensionality.set(this.defaultFingerprintDimensionalityValue);
        this.fingerprintFrequencyThreshold.set(this.DEFAULT_FINGERPRINT_FREQUENCY_THRESHOLD);

    }
    public int[][] getFragmentFingerprints(List<MoleculeDataModel> aMoleculeDataModelList, List<FragmentDataModel> aFragmentDataModelList, String aFragmentationName) {
        /*
        int tmpMaximumDimensionalityNumber =  this.getFingerprintDimensionality();
        System.out.println(tmpMaximumDimensionalityNumber + "-------dim 0");
        if(tmpMaximumDimensionalityNumber == 1) {
            tmpMaximumDimensionalityNumber = aFragmentDataModelList.size();
        } else {
           tmpMaximumDimensionalityNumber =  this.getFingerprintDimensionality();
            System.out.println(this.getFingerprintDimensionality() + "---dimensionality ");
        }
        this.setFingerprintDimensionality(tmpMaximumDimensionalityNumber);
        String tmpSortProperty = "absoluteFrequency";
        CollectionUtil.sortGivenFragmentListByPropertyAndSortType( aFragmentDataModelList,tmpSortProperty, "DESCENDING");
        List<FragmentDataModel> tmpSubList = aFragmentDataModelList.subList(0,tmpMaximumDimensionalityNumber);
        System.out.println(tmpSubList.size() + "------sublist size ");
        System.out.println("hallllooooooooo");
        System.out.println(tmpSubList + "-------sublist");
        for(FragmentDataModel test : tmpSubList){
            System.out.println(test.getUniqueSmiles() + "--------uniques Smiles");
        }
        */
        int[][] tmpDataMatrix =  new int[aMoleculeDataModelList.size()][aFragmentDataModelList.size()];
        ArrayList<String> tmpKeyFragmentsToGenerateBitFingerprints = new ArrayList<>(aFragmentDataModelList.size());
        for(FragmentDataModel tmpFragmentDataModel : aFragmentDataModelList) {
            tmpKeyFragmentsToGenerateBitFingerprints.add(tmpFragmentDataModel.getUniqueSmiles());
        }
        FragmentFingerprinter tmpFragmentFingerprinter = new FragmentFingerprinter(tmpKeyFragmentsToGenerateBitFingerprints);
        int tmpIterator = 0;
        for(MoleculeDataModel tmpMoleculeDataModel : aMoleculeDataModelList) {
            ArrayList<String> tmpMoleculeFragmentsToGenerateBitFingerprints = new ArrayList<>(); // TODO add initial size
            if(!tmpMoleculeDataModel.hasMoleculeUndergoneSpecificFragmentation(aFragmentationName)){
                continue;
            }
            List<FragmentDataModel> tmpFragmentList = tmpMoleculeDataModel.getFragmentsOfSpecificAlgorithm(aFragmentationName);
            for(FragmentDataModel tmpFragmentDataModel : tmpFragmentList) {
                tmpMoleculeFragmentsToGenerateBitFingerprints.add(tmpFragmentDataModel.getUniqueSmiles());
            }
            tmpDataMatrix[tmpIterator] = tmpFragmentFingerprinter.getBitArray(tmpMoleculeFragmentsToGenerateBitFingerprints);
            tmpIterator++;
        }
        return tmpDataMatrix;
    }
}
