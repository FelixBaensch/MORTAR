package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.graph.SpanningTree;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.qsar.descriptors.molecular.LargestChainDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.SmallRingDescriptor;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openscience.cdk.tools.manipulator.AtomContainerManipulator.extractSubstructure;


/**
 * Java class implementing an algorithm for detection and fragmentation of alkyl
 * structures in MORTAR using the CDK.
 *
 * @author Maximilian Rottmann
 */
public class AlkylStructureFragmenter implements IMoleculeFragmenter{
    /**
     * Enum for options concerning maximum fragment length of carbohydrate chain created by fragmenter.
     */
    public enum ChainFragmentLengthOption {
        METHANE,
        ETHANE,
        PROPANE,
        BUTANE
    }

    /**
     * Enum for maximum size options for rings to be preserved by the algorithm.
     */
    public enum PreserveRingMaxSizeOption {
        CYCLO_PROPANE,
        CYCLO_BUTANE,
        CYCLO_PENTANE,
        CYCLO_HEXANE
    }

    /**
     * Name of the fragmenter.
     */
    public static final String ALGORITHM_NAME = "Alkyl structure";
    /**
     * Default option for maximum fragment length of carbohydrate chain, set to Methane.
     */
    public static final ChainFragmentLengthOption Chain_Fragment_LENGTH_OPTION_DEFAULT = ChainFragmentLengthOption.METHANE;
    /**
     * Default option for spiro carbon dissection.
     */
    public static final boolean DISSECT_SPIRO_CARBON_OPTION_DEFAULT = false;
    /**
     * Default option for ring dissection.
     */
    public static final boolean DISSECT_RINGS_OPTION_DEFAULT = false;
    /**
     * Default option for maximum size of preserved rings.
     */
    public static final PreserveRingMaxSizeOption Preserve_Ring_MAX_SIZE_OPTION_DEFAULT = PreserveRingMaxSizeOption.CYCLO_HEXANE;
    /**
     * Default option for ring system preservation.
     */
    public static final boolean PRESERVE_RING_SYSTEM_OPTION_DEFAULT = true;
    /**
     * Default option for maximum rings contained in preserved ring system.
     */
    public static final int PRESERVE_RING_SYSTEM_MAX_OPTION_DEFAULT = 1;
    /**
     * A property that has a constant name from the ChainFragmentLengthOption enum as value.
     */
    private final SimpleEnumConstantNameProperty chainFragmentLengthSetting;
    /**
     * Boolean property whether spiro carbons should be dissected.
     */
    public final SimpleBooleanProperty dissectSpiroCarbonSetting;
    /**
     * Boolean property whether rings should be dissected.
     */
    public final SimpleBooleanProperty dissectRingsSetting;
    /**
     * Enum property for maximum size of preserved rings.
     */
    public final SimpleEnumConstantNameProperty preserveRingMaxSizeSetting;
    /**
     * Boolean property whether ring systems should be preserved.
     */
    public final SimpleBooleanProperty preserveRingSystemSetting;
    /**
     * Integer property for maximum rings contained in preserved ring system.
     */
    public final SimpleIntegerProperty preserveRingSystemMaxSetting;
    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private final HashMap<String, String> settingNameTooltipTextMap;
    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding to GUI.
     */
    private final List<Property> settings;
    /**
     * Logger of this class.
     */
    private final Logger logger = Logger.getLogger(AlkylStructureFragmenter.class.getName());
    /**
     * largestChainDescriptor for detection of longest carbon chain in given IAtomContainer.
     */
    private final LargestChainDescriptor largestChainDescriptor = new LargestChainDescriptor();
    /**
     * smallRingDescriptor for detection of number of rings in given IAtomContainer.
     */
    private final SmallRingDescriptor smallRingDescriptor = new SmallRingDescriptor();

    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their respective default values.
     */
    public AlkylStructureFragmenter(){
        this.settingNameTooltipTextMap = new HashMap(12, 0.75f);
        this.chainFragmentLengthSetting = new SimpleEnumConstantNameProperty(this, "Chain fragment length setting",
                AlkylStructureFragmenter.Chain_Fragment_LENGTH_OPTION_DEFAULT.name(),
                ChainFragmentLengthOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.chainFragmentLengthSetting.getName(),
                Message.get("AlkylStructureFragmenter.chainFragmentLengthSetting.tooltip"));
        this.dissectSpiroCarbonSetting = new SimpleBooleanProperty(this, "Dissect spiro carbon setting",
                AlkylStructureFragmenter.DISSECT_SPIRO_CARBON_OPTION_DEFAULT);
        this.settingNameTooltipTextMap.put(this.dissectSpiroCarbonSetting.getName(),
                Message.get("AlkylStructureFragmenter.dissectSpiroCarbonSetting.tooltip"));
        this.dissectRingsSetting = new SimpleBooleanProperty(this, "Dissect rings setting",
                AlkylStructureFragmenter.DISSECT_RINGS_OPTION_DEFAULT);
        this.settingNameTooltipTextMap.put(this.dissectRingsSetting.getName(),
                Message.get("AlkylStructureFragmenter.dissectRingsSetting.tooltip"));
        this.preserveRingMaxSizeSetting = new SimpleEnumConstantNameProperty(this, "Preserve ring max size setting (preserves if Dissect rings setting is set false)",
                AlkylStructureFragmenter.Preserve_Ring_MAX_SIZE_OPTION_DEFAULT.name(),
                PreserveRingMaxSizeOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.preserveRingMaxSizeSetting.getName(),
                Message.get("AlkylStructureFragmenter.preserveRingMaxSizeSetting.tooltip"));
        this.preserveRingSystemSetting = new SimpleBooleanProperty(this, "Preserve ring system setting (preserves if Dissect rings setting is set false)",
                AlkylStructureFragmenter.PRESERVE_RING_SYSTEM_OPTION_DEFAULT);
        this.settingNameTooltipTextMap.put(this.preserveRingSystemSetting.getName(),
                Message.get("AlkylStructureFragmenter.preserveRingSystemSetting.tooltip"));
        this.preserveRingSystemMaxSetting = new SimpleIntegerProperty(this, "Preserve ring system up to maximum size",
                AlkylStructureFragmenter.PRESERVE_RING_SYSTEM_MAX_OPTION_DEFAULT);
        this.settingNameTooltipTextMap.put(this.preserveRingSystemMaxSetting.getName(),
                Message.get("AlkylStructureFragmenter.preserveRingSystemMaxSetting.tooltip"));
        //
        this.settings = new ArrayList<Property>(6);
        this.settings.add(this.chainFragmentLengthSetting);
        this.settings.add(this.dissectSpiroCarbonSetting);
        this.settings.add(this.dissectRingsSetting);
        this.settings.add(this.preserveRingMaxSizeSetting);
        this.settings.add(this.preserveRingSystemSetting);
        this.settings.add(this.preserveRingSystemMaxSetting);
    }
    //</editor-fold>

    //<editor-fold desc="Get Settings">
    /**
     * Returns a list of all available settings represented by properties for the given fragmentation algorithm.
     *
     * @return list of settings represented by properties
     */
    @Override
    public List<Property> settingsProperties() {
       return this.settings;
    }

    /**
     * Returns a map containing descriptive texts (values) for the settings with the given names (keys) to be used as
     * tooltips in the GUI.
     *
     * @return map with tooltip texts
     */
    @Override
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return settingNameTooltipTextMap;
    }

    /**
     * Returns a string representation of the algorithm name, e.g. "ErtlFunctionalGroupsFinder" or "Ertl algorithm".
     * The given name must be unique among the available fragmentation algorithms!
     *
     * @return algorithm name
     */
    @Override
    public String getFragmentationAlgorithmName() {
        return AlkylStructureFragmenter.ALGORITHM_NAME;
    }

    /**
     * Returns the currently set option for saturating free valences on returned fragment molecules.
     *
     * @return the set option
     */
    @Override
    public String getFragmentSaturationSetting() {
        return null;
    }

    /**
     * Returns the property representing the setting for fragment saturation.
     *
     * @return setting property for fragment saturation
     */
    @Override
    public SimpleEnumConstantNameProperty fragmentSaturationSettingProperty() {
        return null;
    }

    /**
     * Returns the currently set fragment saturation option as the respective enum constant.
     *
     * @return fragment saturation setting enum constant
     */
    @Override
    public FragmentSaturationOption getFragmentSaturationSettingConstant() {
        return null;
    }
    //</editor-fold>

    //<editor-fold desc="Set Settings">
    /**
     * Sets the option for saturating free valences on returned fragment molecules.
     *
     * @param anOptionName constant name (use name()) from FragmentSaturationOption enum
     * @throws NullPointerException     if the given name is null
     * @throws IllegalArgumentException if the given string does not represent an enum constant
     */
    @Override
    public void setFragmentSaturationSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {

    }

    /**
     * Sets the option for saturating free valences on returned fragment molecules.
     *
     * @param anOption the saturation option to use
     * @throws NullPointerException if the given option is null
     */
    @Override
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException {

    }

    /**
     * Sets the enum option for maximum length of fragment chain.
     *
     * @param anOptionName the enum option name to use
     * @throws NullPointerException if the given option is null
     * @throws IllegalArgumentException if the given option does not exist
     */
    public void setChainFragmentLengthSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        ChainFragmentLengthOption tmpConstant = ChainFragmentLengthOption.valueOf(anOptionName);
        this.setChainFragmentLengthSetting(tmpConstant);
    }

    /**
     * Sets the option for maximum length of fragment chain.
     *
     * @param anOption the option to use
     * @throws NullPointerException if given option is null
     */
    public void setChainFragmentLengthSetting(ChainFragmentLengthOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        this.chainFragmentLengthSetting.set(anOption.name());
    }

    /**
     * Sets the bool option for spiro carbon dissection.
     *
     * @param aBoolean the bool option to use
     */
    public void setDissectSpiroCarbonSetting(boolean aBoolean) {
        this.dissectSpiroCarbonSetting.set(aBoolean);
    }

    /**
     * Sets the bool option for ring dissection.
     *
     * @param aBoolean the bool option to use
     */
    public void setDissectRingsSetting(boolean aBoolean) {
        this.dissectRingsSetting.set(aBoolean);
    }

    /**
     * Sets the enum option for maximum size of preserved ring.
     *
     * @param anOptionName the enum option to use
     * @throws NullPointerException if given option is null
     * @throws IllegalArgumentException if given option does not exist
     */
    public void setPreserveRingMaxSizeSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option is null.");
        PreserveRingMaxSizeOption tmpConstant = PreserveRingMaxSizeOption.valueOf(anOptionName);
        this.setPreserveRingMaxSizeSetting(tmpConstant);
    }

    /**
     * Sets the option for maximum size of preserved ring.
     *
     * @param anOption the option to use
     * @throws NullPointerException if given option is null
     */
    public void setPreserveRingMaxSizeSetting(PreserveRingMaxSizeOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option name is null.");
        this.preserveRingMaxSizeSetting.set(anOption.name());
    }

    /**
     * Sets bool option for ring system preservation.
     *
     * @param aBoolean the bool option to use
     */
    public void setPreserveRingSystemSetting(boolean aBoolean) {
        this.preserveRingSystemSetting.set(aBoolean);
    }

    /**
     * Sets the maximum of rings contained in preserved system.
     *
     * @param anInt the int option to use
     */
    public void setPreserveRingSystemMaxSetting(int anInt) {
        this.preserveRingSystemMaxSetting.set(anInt);
    }
    //</editor-fold>

    /**
     * Returns a new instance of the respective fragmenter with the same settings as this instance. Intended for
     * multi-threaded work where every thread needs its own fragmenter instance.
     *
     * @return new fragmenter instance with the same settings
     */
    @Override
    public IMoleculeFragmenter copy() {
        AlkylStructureFragmenter tmpCopy = new AlkylStructureFragmenter();
        tmpCopy.setChainFragmentLengthSetting(this.chainFragmentLengthSetting.get());
        tmpCopy.setDissectSpiroCarbonSetting(this.dissectSpiroCarbonSetting.get());
        tmpCopy.setDissectRingsSetting(this.dissectRingsSetting.get());
        tmpCopy.setPreserveRingMaxSizeSetting(this.preserveRingMaxSizeSetting.get());
        tmpCopy.setPreserveRingSystemSetting(this.preserveRingSystemSetting.get());
        tmpCopy.setPreserveRingSystemMaxSetting(this.preserveRingSystemMaxSetting.get());
        return tmpCopy;
    }

    /**
     * Restore all settings of the fragmenter to their default values.
     */
    @Override
    public void restoreDefaultSettings() {

    }

    /**
     * Fragments a clone(!) of the given molecule according to the respective algorithm and returns the resulting fragments.
     *
     * @param aMolecule to fragment
     * @return a list of fragments (the list may be empty if no fragments are extracted, but the fragments should not be!)
     * @throws NullPointerException       if aMolecule is null
     * @throws IllegalArgumentException   if the given molecule cannot be fragmented but should be filtered or preprocessed
     * @throws CloneNotSupportedException if cloning the given molecule fails
     */
    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        IAtomContainer tmpClone = aMolecule.clone();
        //<editor-fold desc="Parameter Checks">
        List<IAtomContainer> tmpFragments = new ArrayList<>(1);
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpCanBeFragmented = this.canBeFragmented(aMolecule);
        if (!tmpCanBeFragmented) {
            throw new IllegalArgumentException("Given molecule cannot be fragmented but should be filtered or preprocessed first.");
        }
        boolean hasRings = false;
        //</editor-fold>

        //<editor-fold desc="Descriptor Checks">
        /**
         * ToDo descriptor
         */
        /*
        Object[] tmpParamsLCD = new Object[2];
        tmpParamsLCD[0] = false;
        tmpParamsLCD[1] = true;
        try {
            this.largestChainDescriptor.setParameters(tmpParamsLCD);
            DescriptorValue tmpChainDescriptorValue = this.largestChainDescriptor.calculate(aMolecule);
            IDescriptorResult tmpValue = tmpChainDescriptorValue.getValue();
            int tmpLargestChain = ((IntegerResult) tmpValue).intValue();
            if (tmpLargestChain < 1) {
                tmpFragments.add(aMolecule);
            }
            System.out.println("largest chain " + tmpLargestChain);
        } catch (CDKException cdkException) {
            throw new RuntimeException(cdkException);
        }
        */
        //</editor-fold>

        //<editor-fold desc="CycleFinder">
        CycleFinder tmpMCBCycleFinder = Cycles.mcb();
        CycleFinder tmpRelevantCycleFinder = Cycles.relevant();
        CycleFinder tmpEssentialCycleFinder = Cycles.essential();

        /**
         * ToDo: option/setting which algorithm to use?
         */
        /*
        try {
            Cycles tmpMCBCycles = tmpMCBCycleFinder.find(aMolecule);
            IRingSet tmpMCBCyclesSet = tmpMCBCycles.toRingSet();
            int tmpCount = tmpMCBCyclesSet.getAtomContainerCount();
            for (int i = 0; i < tmpCount; i++) {
                if (tmpMCBCyclesSet.getAtomContainer(i) == null) {
                    this.logger.log(Level.WARNING, "AtomContainer in tmpMCBCyclesSet is null");
                    continue;
                }
                tmpFragments.add(tmpMCBCyclesSet.getAtomContainer(i));
            }
        } catch (Intractable e) {
            throw new RuntimeException(e);
        }
        */
        /*
        try {
            Cycles tmpRelevantCycles = tmpRelevantCycleFinder.find(aMolecule);
            IRingSet tmpRelevantCyclesSet = tmpRelevantCycles.toRingSet();
            int tmpCount = tmpRelevantCyclesSet.getAtomContainerCount();
            for (int i = 0; i < tmpCount; i++) {
                if (tmpRelevantCyclesSet.getAtomContainer(i) == null) {
                    this.logger.log(Level.WARNING, "AtomContainer in tmpRelevantCyclesSet is null");
                    continue;
                }
                tmpFragments.add(tmpRelevantCyclesSet.getAtomContainer(i));
            }
        } catch (Intractable e) {
            throw new RuntimeException(e);
        }
        */
        /*
        try {
            Cycles tmpEssentialCycles = tmpEssentialCycleFinder.find(aMolecule);
            IRingSet tmpEssentialCyclesSet = tmpEssentialCycles.toRingSet();
            int tmpCount = tmpEssentialCyclesSet.getAtomContainerCount();
            for (int i = 0; i < tmpCount; i++) {
                if (tmpEssentialCyclesSet.getAtomContainer(i) == null) {
                    this.logger.log(Level.WARNING, "AtomContainer in tmpEssentialCyclesSet is null");
                    continue;
                }
                tmpFragments.add(tmpEssentialCyclesSet.getAtomContainer(i));
            }
        } catch (Intractable e) {
            throw new RuntimeException(e);
        }
        */
        //</editor-fold>

        //<editor-fold desc="SpanningTree">
        /**
         * ToDo: SpanningTree
         */
        try {
            SpanningTree tmpSpanningTree = new SpanningTree(aMolecule);
            Cycles.markRingAtomsAndBonds(tmpClone);

            final Set<IAtom> included = new HashSet<>();
            for (IAtom atom : tmpClone.atoms()) {
                if (!atom.isInRing() && atom.getAtomicNumber() != 1)
                    included.add(atom);
            }
            IAtomContainer subset = subsetMol(tmpClone, included);
            //ToDo: get List of substructure AtomContainers via private method
            //tmpFragments.add(subset);
            IAtomContainerSet tmpAtomContainerSet;// = findAlkylChain(subset);
            tmpAtomContainerSet = findAlkylChain(subset);
            int var = 0; //debugging var
            for (IAtomContainer atomContainer: tmpAtomContainerSet.atomContainers()) {
                System.out.println("extract atomcontainer from set " + var);
                if (atomContainer.isEmpty()) {
                    System.out.println(atomContainer.getAtomCount());
                    continue;
                }
                tmpFragments.add(atomContainer);
                var++;
            }
            //tmpFragments.add(subset);

            IRingSet tmpSpanTreeRingSet = tmpSpanningTree.getBasicRings();
            int tmpCount = tmpSpanTreeRingSet.getAtomContainerCount();
            for (int i = 0; i < tmpCount; i++) {
                if (tmpSpanTreeRingSet.getAtomContainer(i) == null) {
                    this.logger.log(Level.WARNING, "AtomContainer in tmpSpanTreeRingSet is null");
                    continue;
                }
                IAtomContainer tmpSTRSAtomContainer = tmpSpanTreeRingSet.getAtomContainer(i);
                tmpFragments.add(tmpSTRSAtomContainer);
            }
        } catch (Exception anException) {
            throw new RuntimeException(anException);
        }
        //</editor-fold>


        return tmpFragments;
    }

    //<editor-fold desc="Pre-Fragmentation Tasks">
    /**
     * Returns true if the given molecule cannot be fragmented by the respective algorithm, even after preprocessing.
     * If the molecule is null, true is returned and no exception thrown.
     *
     * <p>
     *     Checks the given IAtomContainer aMolecule for non-carbon and non-hydrogen atoms.
     * </p>
     *
     * @param aMolecule the molecule to check
     * @return true if the given molecule is not acceptable as input for the fragmentation algorithm, even if it would be
     * preprocessed
     */
    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        try {
            for (IAtom tmpAtom : aMolecule.atoms()) {
                return !(tmpAtom.getAtomicNumber() != IElement.C || tmpAtom.getAtomicNumber() != IElement.H);
                //pseudoatom handling!
            }
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + " Molecule ID: " + aMolecule.getID());
            return true;
        }
        return true;
    }

    /**
     * Returns true if the given molecule can be fragmented by the respective algorithm after preprocessing. Returns
     * false if the given molecule can be directly fragmented by the algorithm without preprocessing.
     * Does not check whether the molecule should be filtered! But throws an exception if it is null.
     *
     * @param aMolecule the molecule to check
     * @return true if the molecule needs to be preprocessed, false if it can be fragmented directly
     * @throws NullPointerException if the molecule is null
     */
    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
        return false;
    }

    /**
     * Returns true only if the given molecule can be passed to the central fragmentation method without any preprocessing
     * and without causing an exception. If 'false' is returned, check the methods for filtering and preprocessing.
     *
     * @param aMolecule the molecule to check
     * @return true if the molecule can be directly fragmented
     * @throws NullPointerException if the molecule is null
     */
    @Override
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        boolean tmpShouldBePreprocessed = this.shouldBePreprocessed(aMolecule);
        return !tmpShouldBeFiltered && !tmpShouldBePreprocessed;
        //throws NullpointerException if molecule is null
    }

    /**
     * Applies the needed preprocessing for fragmentation to the given molecule. Throws an exception if the molecule
     * should be filtered.
     *
     * @param aMolecule the molecule to preprocess
     * @return a copy of the given molecule that has been preprocessed
     * @throws NullPointerException       if the molecule is null
     * @throws IllegalArgumentException   if the molecule should be filtered, i.e. it cannot be fragmented even after
     *                                    preprocessing
     * @throws CloneNotSupportedException if cloning the given molecule fails
     */
    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        return null;
    }
    //</editor-fold>

    /**
     * copied from largestChainDescriptor
     * ToDo
     */
    private static IAtomContainer subsetMol(IAtomContainer mol, Set<IAtom> include) {
        IAtomContainer cpy = mol.getBuilder().newInstance(IAtomContainer.class, mol.getAtomCount(), mol.getBondCount(), 0, 0);
        for (IAtom atom : mol.atoms()) {
            if (include.contains(atom))
                cpy.addAtom(atom);
        }
        for (IBond bond : mol.bonds()) {
            if (include.contains(bond.getBegin()) && include.contains(bond.getEnd()))
                cpy.addBond(bond);
        }
        return cpy;
    }
    //copy end

    //ToDo routine to detect and return whole alkyl chains, use extractSubstructure()? -> Set of atoms marked to copy -> detection of connected atoms needed
    private static IAtomContainerSet findAlkylChain(IAtomContainer anAtomContainer) {
        IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
        IAtom tmpOldAtom = null;
        IAtom tmpNewAtom;
        IAtomContainer tmpChainAtomContainer = new AtomContainer(anAtomContainer.getAtomCount(),anAtomContainer.getBondCount(),anAtomContainer.getLonePairCount(),anAtomContainer.getSingleElectronCount());
        int tmpAtomCount = anAtomContainer.getAtomCount();
        List<IAtom> tmpConnectedAtoms = null;

        for (IAtom atom: anAtomContainer.atoms()) {
            List<IAtom> tmpAtomList = anAtomContainer.getConnectedAtomsList(atom);
            int k = tmpAtomList.size();
            System.out.println("find alkyl chain 1. for " + tmpAtomList.size());

            //List<IBond> tmpBondList = anAtomContainer.getConnectedBondsList(atom);
            //ToDo: iterate over atoms
            if (tmpAtomList.isEmpty()) {
                atom.setFlag(CDKConstants.VISITED, true);
                IAtomContainer tmpSingleAtomContainer = new AtomContainer();
                tmpSingleAtomContainer.addAtom(atom);
                tmpAtomContainerSet.addAtomContainer(tmpSingleAtomContainer);
                atom.setFlag(CDKConstants.ISPLACED, true);
                if (atom.getFlag(CDKConstants.VISITED) && atom.getFlag(CDKConstants.ISPLACED)) {
                    System.out.println("Debug 1: " + Arrays.toString(atom.getFlags()));
                }
            } else if (tmpConnectedAtoms != null) {
                if (tmpConnectedAtoms.contains(atom)) {
                    System.out.println("Debug 2");
                    if (atom.getFlag(CDKConstants.VISITED) && !atom.getFlag(CDKConstants.ISPLACED)) {
                        System.out.println("Debug 3");
                        atom.setFlag(CDKConstants.ISPLACED, true);
                        tmpChainAtomContainer.addAtom(atom);
                        for (IBond bond: atom.bonds()) {
                            tmpChainAtomContainer.addBond(bond);
                        }
                    } else {
                        System.out.println("Debug 6");
                        atom.setFlag(CDKConstants.VISITED, true);
                        tmpChainAtomContainer.addAtom(atom);
                        atom.setFlag(CDKConstants.ISPLACED, true);
                        for (IBond bond: atom.bonds()) {
                            tmpChainAtomContainer.addBond(bond);
                        }
                    }
                } else {
                    System.out.println("Debug 4");
                    atom.setFlag(CDKConstants.VISITED, true);
                    tmpChainAtomContainer.addAtom(atom);
                    atom.setFlag(CDKConstants.ISPLACED, true);
                    for (IBond bond: atom.bonds()) {
                        tmpChainAtomContainer.addBond(bond);
                    }
                }
            }
            System.out.println("Debug 5");
            tmpConnectedAtoms = tmpAtomList;
            System.out.println("connectedAtoms size " + tmpConnectedAtoms.size());
        }
        tmpAtomContainerSet.addAtomContainer(tmpChainAtomContainer);
        return tmpAtomContainerSet;
    }
}
