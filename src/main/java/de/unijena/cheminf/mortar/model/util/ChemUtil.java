/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2026  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.model.util;

import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.io.Importer;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.interfaces.ISingleElectron;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.LonePairElectronChecker;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chemistry utility.
 *
 * @author Samuel Behr
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public final class ChemUtil {
    //<editor-fold desc="Public static final class constants">
    /**
     * Maximum number of different tautomers with explicit Hydrogen atoms added to aromatic Nitrogen atoms generated to
     * get a structure that can be kekulized in {@link #fixAromaticNitrogenAndCreateSMILES(IAtomContainer)}.
     */
    public static final int MAX_TAUTOMER_COMBINATIONS = 1000;

    /**
     * Pattern to match both 'n' and '[nH]' in SMILES (but not 'n' in Rn, Sn, In, Cn, Zn, Mn)
     */
    public static final String AROMATIC_N_REGEX = "\\[nH]|(?<!\\[[RSICZM])n";
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(ChemUtil.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Private constructor" defaultstate="collapsed">
    /**
     * Private parameter-less constructor.
     * Introduced because javadoc build complained about classes without declared default constructor.
     */
    private ChemUtil() {
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * Creates a unique SMILES string out of the given atom container or returns null, if the creation was not possible.
     * If the SMILES could not be created in the first place, it is retried with a kekulized clone of the given atom
     * container. Aromaticity information is encoded in the returned SMILES string, if there is any given. Unique SMILES
     * codes do NOT encode stereochemistry by default! This can be turned on with the second parameter.
     *
     * @param anAtomContainer atom container the unique SMILES should be created of
     * @param isStereoChemEncoded whether stereochemistry should be encoded
     * @return unique SMILES of the given atom container or 'null' if no creation was possible
     */
    public static String createUniqueSmiles(IAtomContainer anAtomContainer, boolean isStereoChemEncoded) {
        return ChemUtil.createUniqueSmiles(anAtomContainer, isStereoChemEncoded, true);
    }

    /**
     * Creates a unique SMILES string out of the given atom container or returns null, if the creation was not possible.
     * If the SMILES could not be created in the first place, it is retried with a kekulized clone of the given atom
     * container. Unique SMILES codes do NOT encode stereochemistry or aromaticity by default! This can be turned on
     * with the parameters.
     *
     * @param anAtomContainer atom container the unique SMILES should be created of
     * @param isStereoChemEncoded whether stereochemistry should be encoded
     * @param isAromaticityEncoded whether aromaticity should be encoded
     * @return unique SMILES of the given atom container or 'null' if no creation was possible
     */
    public static String createUniqueSmiles(IAtomContainer anAtomContainer, boolean isStereoChemEncoded, boolean isAromaticityEncoded) {
        int tmpFlavor = SmiFlavor.Unique;
        if (isAromaticityEncoded) {
            tmpFlavor = tmpFlavor | SmiFlavor.UseAromaticSymbols;
        }
        if (isStereoChemEncoded && anAtomContainer.stereoElements().iterator().hasNext()) {
            tmpFlavor = tmpFlavor | SmiFlavor.Stereo;
        }
        String tmpSmiles = null;
        try {
            try {
                tmpSmiles = SmilesGenerator.create(anAtomContainer, tmpFlavor, new int[anAtomContainer.getAtomCount()]);
            } catch (CDKException anException) {
                String tmpFixedSmiles = ChemUtil.fixAromaticNitrogenAndCreateSMILES(anAtomContainer);
                if (tmpFixedSmiles != null) {
                    IAtomContainer tmpFixedMol = ChemUtil.parseSmilesToAtomContainer(tmpFixedSmiles, true, false);
                    tmpSmiles = SmilesGenerator.create(tmpFixedMol, tmpFlavor, new int[tmpFixedMol.getAtomCount()]);
                    ChemUtil.LOGGER.log(Level.INFO, String.format("Added hydrogen atom to aromatic nitrogen atom to produce valid Kekule structure of molecule %s", (String) anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY)));
                } else {
                    IAtomContainer tmpAtomContainer = anAtomContainer.clone();
                    Kekulization.kekulize(tmpAtomContainer);
                    tmpSmiles = SmilesGenerator.create(tmpAtomContainer, tmpFlavor, new int[anAtomContainer.getAtomCount()]);
                    ChemUtil.LOGGER.log(Level.INFO, String.format("Kekulized molecule %s", anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY)));
                }
            }
        } catch (CDKException | NullPointerException | IllegalArgumentException | CloneNotSupportedException | ArrayIndexOutOfBoundsException anException){
            ChemUtil.LOGGER.log(Level.SEVERE, String.format("%s; molecule name: %s", anException.toString(), anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY)), anException);
        }
        return tmpSmiles;
    }

    /**
     * Returns an IAtomContainer instance which represents the molecule parsed from the SMILES string. Bond types and
     * atom types are assigned to it (the former through kekulization) if required. Aromaticity flags are set only if
     * there is aromaticity information present in the SMILES code, no aromaticity perception is performed here.
     *
     * @param aSmilesCode SMILES representation
     * @param shouldBeKekulized whether explicit bond orders should be assigned or "aromatic bond" can be used if present;
     *                          does not affect aromaticity flags
     * @param shouldAtomTypesBePerceived whether atom types should be perceived and configured
     * @return IAtomContainer atom container of the molecule
     * @throws CDKException if the given SMILES is invalid or if kekulization or atom type matching fails
     */
    public static IAtomContainer parseSmilesToAtomContainer(String aSmilesCode, boolean shouldBeKekulized, boolean shouldAtomTypesBePerceived)
            throws CDKException {
        //no checks because .parseSmiles() checks and throws InvalidSmilesException (subclass of CDKException) if the SMILES cannot be parsed
        IAtomContainer tmpAtomContainer;
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        tmpSmiPar.kekulise(false);
        //throws InvalidSmilesException
        tmpAtomContainer = tmpSmiPar.parseSmiles(aSmilesCode);
        if (shouldBeKekulized) {
            //throws CDKException
            Kekulization.kekulize(tmpAtomContainer);
        }
        if (shouldAtomTypesBePerceived) {
            //throws CDKException
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomContainer);
        }
        return tmpAtomContainer;
    }

    /**
     * Call to {@link #parseSmilesToAtomContainer(String, boolean, boolean)} with kekulization and atom type perception
     * set to true.
     *
     * @param aSmilesCode SMILES representation
     * @return IAtomContainer atom container of the molecule
     * @throws CDKException if the given SMILES is invalid or if kekulization or atom type matching fails
     */
    public static IAtomContainer parseSmilesToAtomContainer(String aSmilesCode) throws CDKException {
        return ChemUtil.parseSmilesToAtomContainer(aSmilesCode, true, true);
    }

    /**
     * Generates the molecular formula of a given atom container.
     *
     * @param anAtomContainer AtomContainer to generate the molecular formula of
     * @return String containing the molecular formula of the given atom container
     * @author Samuel Behr, Jonas Schaub
     */
    public static String generateMolecularFormula(IAtomContainer anAtomContainer) {
        /* the doc of MolecularFormulaManipulator.getMolecularFormula() says "The hydrogens must be implicit." but the
           CDK tests indicate that both implicit and explicit hydrogens are working fine */
        IMolecularFormula tmpMolecularFormula = MolecularFormulaManipulator.getMolecularFormula(anAtomContainer);
        return MolecularFormulaManipulator.getString(tmpMolecularFormula);
    }

    /**
     * Checks whether 3D coordinates are set for all atoms in the given molecule data model.
     *
     * @param aMolecule to check for 3D coordinates
     * @return true if 3D coordinates are set for ALL atoms in the given molecule
     */
    public static boolean has3DCoordinates(MoleculeDataModel aMolecule) {
        IAtomContainer tmpFragment;
        try {
            tmpFragment = aMolecule.getAtomContainer();
        } catch(CDKException anException){
            ChemUtil.LOGGER.log(Level.SEVERE, String.format("%s molecule name: %s", anException.toString(), aMolecule.getName()), anException);
            return false;
        }
        boolean tmpHas3DCoords = true;
        for (IAtom tmpAtom : tmpFragment.atoms()) {
            if (tmpAtom.getPoint3d() == null) {
                tmpHas3DCoords = false;
                break;
            }
        }
        return tmpHas3DCoords;
    }

    /**
     * Checks whether 2D coordinates are set for all atoms in the given molecule data model.
     *
     * @param aMolecule to check for 2D coordinates
     * @return true if 2D coordinates are set for ALL atoms in the given molecule
     */
    public static boolean has2DCoordinates(MoleculeDataModel aMolecule) {
        IAtomContainer tmpFragment;
        try {
            tmpFragment = aMolecule.getAtomContainer();
        } catch(CDKException anException){
            ChemUtil.LOGGER.log(Level.SEVERE, String.format("%s molecule name: %s", anException.toString(), aMolecule.getName()), anException);
            return false;
        }
        boolean tmpHas2DCoords = true;
        for (IAtom tmpAtom : tmpFragment.atoms()) {
            if (tmpAtom.getPoint2d() == null) {
                tmpHas2DCoords = false;
                break;
            }
        }
        return tmpHas2DCoords;
    }

    /**
     * Checks all atoms in all molecules in the given list for 3D or 2D coordinates. If only one atom has neither,
     * false is returned.
     *
     * @param aListOfMolecules to check for 2D or 3D coordinates
     * @return true if 2D or 3D coordinates are set for EVERY atom in EVERY molecule in the given list
     */
    public static boolean checkMoleculeListForCoordinates(List<MoleculeDataModel> aListOfMolecules) {
        if (aListOfMolecules == null || aListOfMolecules.isEmpty()) {
            return false;
        }
        boolean tmpHasCoords = true;
        for (MoleculeDataModel tmpMolecule : aListOfMolecules) {
            if (!ChemUtil.has3DCoordinates(tmpMolecule) && !ChemUtil.has2DCoordinates(tmpMolecule)) {
                tmpHasCoords = false;
                break;
            }
        }
        return tmpHasCoords;
    }

    /**
     * Generates 2D coordinates for the atoms of the given molecule using the CDK StructureDiagramGenerator. Therefore,
     * these coordinates are originally intended for layout! Note that the given atm container is directly manipulated,
     * not cloned. And note that the z coordinate is not set to 0 but left undefined.
     *
     * @param aMolecule the molecule to generate coordinates for
     * @throws NullPointerException if given molecule is null
     * @throws CDKException if coordinates generation fails
     */
    public static void generate2DCoordinates(IAtomContainer aMolecule) throws NullPointerException, CDKException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            return;
        }
        StructureDiagramGenerator tmpStructureDiagramGenerator = new StructureDiagramGenerator();
        tmpStructureDiagramGenerator.generateCoordinates(aMolecule);
    }

    /**
     * Generates pseudo-3D coordinates for the atoms of the given molecule by retrieving their 2D coordinates and
     * setting z=0. Note that the given atm container is directly manipulated, not cloned.
     *
     * @param aMolecule the molecule to generate coordinates for
     * @throws NullPointerException if given molecule is null
     * @throws IllegalArgumentException if at least one atom of the molecule has no 2D coordinates defined
     */
    public static void generatePseudo3Dfrom2DCoordinates(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            return;
        }
        for (IAtom tmpAtom : aMolecule.atoms()) {
            Point2d tmpAtom2Dcoords = tmpAtom.getPoint2d();
            if (Objects.isNull(tmpAtom2Dcoords)) {
                throw new IllegalArgumentException("At least one atom has no 2D coordinates.");
            }
            Point3d tmpPoint3d = new Point3d(tmpAtom2Dcoords.x, tmpAtom2Dcoords.y, 0.0);
            tmpAtom.setPoint3d(tmpPoint3d);
        }
    }

    /**
     * Sets the 3D coordinates of all atoms in the given molecule to (0,0,0).
     * Note that the given atm container is directly manipulated, not cloned.
     * Use this with caution, if a molecular simulation with these molecules is attempted, it might crash!
     *
     * @param aMolecule the molecule to set (0,0,0) coordinates for
     * @throws NullPointerException if given molecule is null
     */
    public static void generateZero3DCoordinates(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            return;
        }
        for (IAtom tmpAtom : aMolecule.atoms()) {
            tmpAtom.setPoint3d(new Point3d(0.0, 0.0, 0.0));
        }
    }

    /**
     * Saturates free valences in the given molecule or molecular fragment with implicit hydrogen atoms.
     *
     * @param aMolecule to saturate
     * @throws NullPointerException if the given molecule is null
     * @throws CDKException if atom types cannot be assigned to all atoms of the molecule
     */
    public static void saturateWithHydrogen(IAtomContainer aMolecule) throws NullPointerException, CDKException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            //fragments might be empty on purpose, e.g. when there is no aglycone in a molecule, so throw no exception
            return;
        }
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(aMolecule);
        CDKHydrogenAdder.getInstance(SilentChemObjectBuilder.getInstance()).addImplicitHydrogens(aMolecule);
        //just a precaution, you never know what can happen with fragment atom containers
        for (IAtom tmpAtom : aMolecule.atoms()) {
            if (tmpAtom.getImplicitHydrogenCount() == CDKConstants.UNSET) {
                tmpAtom.setImplicitHydrogenCount(0);
            }
        }
    }

    /**
     * Checks whether atoms in the given molecule have free atom pairs correctly assigned if chemically needed. If not,
     * they are added. Note: it does not work really well on most fragments...
     *
     * @param aMolecule to check for missing free electron pairs
     * @throws NullPointerException if the given molecule is null
     * @throws CDKException if atom types cannot be assigned to all atoms of the molecule
     */
    public static void checkAndCorrectElectronConfiguration(IAtomContainer aMolecule)
            throws NullPointerException, CDKException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            //fragments might be empty on purpose, e.g. when there is no aglycon in a molecule, so throw no exception
            return;
        }
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(aMolecule);
        LonePairElectronChecker tmpElectronChecker = new LonePairElectronChecker();
        try {
            tmpElectronChecker.saturate(aMolecule);
        } catch (NullPointerException aNullPointerException) {
            /* Too many fragment molecules cause this exception, the log file would be overflowing
            ChemUtil.LOGGER.log(Level.WARNING, "Saturation with lone electron pairs not possible, molecule name: "
                    + aMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY), aNullPointerException);
             */
        }
    }

    /**
     * Fixes radical atoms in a molecule by replacing the single electron with an implicit hydrogen atom.
     * This method converts radical centers to closed-shell configurations by:
     * <ol>
     * <li>Adding an implicit hydrogen to each radical center</li>
     * <li>Removing all single electrons from the molecule</li>
     * <li>Reperceiving atom types to ensure correct configuration</li>
     * </ol>
     * This process effectively caps each radical site with a hydrogen atom, resulting in
     * a more stable molecular representation suitable for further processing.
     * <br><br><b>Important note: expects atom types to be perceived beforehand!</b>
     *
     * @param aMolecule The molecule to process; will be modified in place
     * @throws NullPointerException If the provided molecule is null
     * @throws CDKException If atom type perception fails or other CDK operations encounter problems
     */
    public static void fixRadicals(IAtomContainer aMolecule) throws NullPointerException, CDKException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            return;
        }
        if (aMolecule.getSingleElectronCount() > 0) {
            //fix properties of the atoms that are radicals
            for (ISingleElectron tmpSingleElectron : aMolecule.singleElectrons()) {
                IAtom tmpAtom = tmpSingleElectron.getAtom();
                //setting to null now, will be re-detected correctly by AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(aMolecule) below
                tmpAtom.setHybridization(null);
                tmpAtom.setValency(null);
                tmpAtom.setFormalNeighbourCount(null);
                Integer tmpHCount = tmpAtom.getImplicitHydrogenCount();
                if (tmpHCount == null) {
                    tmpHCount = 0;
                }
                tmpAtom.setImplicitHydrogenCount(tmpHCount + 1);
            }
            //remove all single electrons from the molecule
            int tmpSingleElectronCount = aMolecule.getSingleElectronCount();
            // the electron array is re-ordered after a removal, so we need to remove them in reverse order
            for (int i = tmpSingleElectronCount - 1; i >= 0; i--) {
                aMolecule.removeSingleElectron(i);
            }
            //needs to be redone now to set the correct atom types
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(aMolecule);
            ChemUtil.LOGGER.log(Level.INFO, "{0}", String.format("Fixed %d radicals in molecule with name %s.",
                    tmpSingleElectronCount, aMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY)));
        } //else: do nothing
    }

    /**
     * For cases where an atom container cannot be kekulized (which is among other things necessary for absolute SMILES code generation)
     * because of insufficient valence saturation of aromatic nitrogen atoms (i.e. the SMILES code that was originally
     * parsed was missing an "[nH]" instead of "n"), this method tries to fix this by adding such explicit hydrogen atoms
     * to aromatic nitrogen atoms in all possible combinations. If successful, a valid (*non-unique!*) SMILES code will
     * be returned that encodes aromaticity and stereochemistry. If this does not fix the non-kekulizable molecule or
     * the molecule does not contain aromatic nitrogen atoms, null is returned. This method does not check whether the
     * input molecule can be kekulized or not. If the molecule cannot be fixed with the first 1,000 possible solutions
     * generated, the routine is aborted and null is returned. This limit is imposed to prevent excessive computation time
     * (consider the exponential scaling of possible solutions) and most molecules should be fixed within this margin.
     * Note: we generate a SMILES code first, operate on it to generate all the possible solutions, and check for validity
     * by checking whether it can be parsed again by a SmilesParser that is kekulizing. This round-trip may not be optimal
     * but operating on the atom container directly would require making multiple copies of it. This solution here
     * is therefore less memory-intensive. Plus, since the problem originated from a faulty SMILES code at import in most
     * cases, it is also easiest to correct it this way.
     *
     * @param aMolecule the molecule to attempt to fix
     * @return a (*non-unique!*) SMILES code if successful, null otherwise
     * @throws NullPointerException if the given molecule is null
     */
    public static String fixAromaticNitrogenAndCreateSMILES(IAtomContainer aMolecule) throws NullPointerException{
        //checks:
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            return null;
        }
        //count aromatic nitrogen atoms to choose appropriate initial collection sizes below and reject molecules without any
        int tmpAromaticNCount = 0;
        for (IAtom tmpAtom : aMolecule.atoms()) {
            if (tmpAtom.getAtomicNumber().equals(IElement.N) && tmpAtom.isAromatic()) {
                tmpAromaticNCount++;
            }
        }
        if (tmpAromaticNCount == 0) {
            return null;
        }
        //end of checks
        //generate SMILES code of molecule to operate on it; encodes aromaticity and stereochemistry but is not(!) unique
        String tmpSmiles;
        try {
            tmpSmiles = SmilesGenerator.create(aMolecule, SmiFlavor.UseAromaticSymbols | SmiFlavor.Stereo, new int[aMolecule.getAtomCount()]);
        } catch (CDKException anException) {
            return null;
        }
        // Pattern to match both 'n' and '[nH]' in SMILES (but not 'n' in Rn, Sn, In, Cn, Zn, Mn)
        Pattern tmpNPattern = Pattern.compile(ChemUtil.AROMATIC_N_REGEX);
        Matcher tmpNMatcher = tmpNPattern.matcher(tmpSmiles);
        // Find all aromatic nitrogen positions
        List<Integer> tmpAromaticNPositions = new ArrayList<>(tmpAromaticNCount);
        List<String> tmpAromaticNTypes = new ArrayList<>(tmpAromaticNCount);
        while (tmpNMatcher.find()) {
            tmpAromaticNPositions.add(tmpNMatcher.start());
            tmpAromaticNTypes.add(tmpNMatcher.group());
        }
        if (tmpAromaticNPositions.isEmpty()) {
            return null;
        }
        //SmilesParser for validation
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        //needs to fail if kekulization is not possible
        tmpSmiPar.kekulise(true);
        // Generate all 2^n combinations but already validate while generating and return the first valid solution
        int tmpNCount = tmpAromaticNPositions.size();
        int tmpNrOfTotalCombinations = Math.min((int) Math.pow(2, tmpNCount), ChemUtil.MAX_TAUTOMER_COMBINATIONS);
        tautomerLoop:
        for (int i = 0; i < tmpNrOfTotalCombinations; i++) {
            StringBuilder tmpTautomerBuilder = new StringBuilder(tmpSmiles);
            int tmpOffset = 0;
            for (int j = 0; j < tmpNCount; j++) {
                int tmpPos = tmpAromaticNPositions.get(j) + tmpOffset;
                String tmpCurrentType = tmpAromaticNTypes.get(j);
                boolean tmpIsUseNH = ((i >> j) & 1) == 1;
                String tmpReplacement = tmpIsUseNH ? "[nH]" : "n";
                int tmpOldLength = tmpCurrentType.length();
                int tmpNewLength = tmpReplacement.length();
                tmpTautomerBuilder.replace(tmpPos, tmpPos + tmpOldLength, tmpReplacement);
                tmpOffset += (tmpNewLength - tmpOldLength);
            }
            String tmpTautomerSMILESCode = tmpTautomerBuilder.toString();
            //validation:
            IAtomContainer tmpTautomerAtomContainer;
            try {
                tmpTautomerAtomContainer = tmpSmiPar.parseSmiles(tmpTautomerSMILESCode);
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpTautomerAtomContainer);
            } catch (CDKException | NullPointerException anException) {
                continue;
            }
            for (IAtom tmpAtom : tmpTautomerAtomContainer.atoms()) {
                if (tmpAtom.getAtomicNumber().equals(IElement.N) && tmpAtom.isAromatic()) {
                    Integer tmpImplicitH = tmpAtom.getImplicitHydrogenCount();
                    int tmpValence = tmpTautomerAtomContainer.getConnectedBondsCount(tmpAtom) + (tmpImplicitH == null ? 0 : tmpImplicitH);
                    int tmpCharge = tmpAtom.getFormalCharge();
                    if (tmpValence == 4 && tmpCharge == 0) {
                        //the routine created a Nitrogen atom with valence 4 but no charge - invalid!
                        continue tautomerLoop;
                    }
                }
            }
            //if it can be parsed with kekulization, it is valid and therefore returned
            return tmpTautomerSMILESCode;
        }
        //no tautomer was valid, return null
        return null;
    }
    //</editor-fold>
}
