package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AlkylStructureFragmenter implements IMoleculeFragmenter{
    /**
     * Enum for options concerning maximum fragment length of carbohydrate chain created by fragmenter.
     */
    public static enum ChainFragmentLengthOption {
        METHANE,
        ETHANE,
        PROPANE,
        BUTANE;
    }
    /**
     * Name of the fragmenter.
     */
    public static final String ALGORITHM_NAME = "Alkyl structure fragmenter";
    /**
     * Default option for maximum fragment length of carbohydrate chain, set to Methane.
     */
    public static final ChainFragmentLengthOption Chain_Fragment_LENGTH_OPTION_DEFAULT = ChainFragmentLengthOption.METHANE;
    /**
     * A property that has a constant name from the ChainFragmentLengthOption enum as value.
     */
    private final SimpleEnumConstantNameProperty chainFragmentLengthSetting;
    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private final HashMap<String, String> settingNameTooltipTextMap;
    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding to GUI.
     */
    private List<Property> settings;
    /**
     * Logger of this class.
     */
    private final Logger logger = Logger.getLogger(AlkylStructureFragmenter.class.getName());

    /**
     * Constructor, all settings are initialised with their respective default values.
     */
    public AlkylStructureFragmenter(){
        this.settingNameTooltipTextMap = new HashMap(10, 0.9f);
        this.settings = new ArrayList<Property>(1);
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
        this.settings.add(this.chainFragmentLengthSetting);
    }
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
     * Returns a new instance of the respective fragmenter with the same settings as this instance. Intended for
     * multi-threaded work where every thread needs its own fragmenter instance.
     *
     * @return new fragmenter instance with the same settings
     */
    @Override
    public IMoleculeFragmenter copy() {
        return null;
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
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        //add Logic here
        return null;
    }

    /**
     * Returns true if the given molecule cannot be fragmented by the respective algorithm, even after preprocessing.
     * If the molecule is null, true is returned and no exception thrown.
     *
     * @param aMolecule the molecule to check
     * @return true if the given molecule is not acceptable as input for the fragmentation algorithm, even if it would be
     * preprocessed
     */
    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        return false;
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
        return false;
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
}
