package de.unijena.cheminf.fragmentFingerprinter;

import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.ICountFingerprint;
import org.openscience.cdk.fingerprint.IFingerprinter;
import java.util.List;
import java.util.Map;

/**
 * Interface for implementing fragment fingerprinters.
 * The interface IFragmentFingerprinter inherits from the CDK interface IFingerprinter and extends it with
 * further methods. IFingerprinter is an interface for the calculation of fingerprints.
 * I.e. classes that implement IFingerprinter can calculate fingerprints for a given IAtomContainer,
 * including a bit and count fingerprint. The extension of the IFingerprinter by further methods now allows the
 * calculation of a fingerprint  by the mere comparison of strings. In this context, these strings are to correspond to
 * the unique SMILES of fragments. The extension of the IFingerprinter includes the overloading of the methods that are
 * responsible for the calculation of a count and bit fingerprint. These methods originally expect an
 * IAtomContainer as a parameter. By overloading these methods, the count and bit fingerprints can be calculated
 * without the need for an IAtomContainer, as this calculation is based only on the comparison of strings/unique SMILES
 * of fragments.
 *
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0 // TODO changes javadoc
 */
public interface IFragmentFingerprinter extends IFingerprinter{
    // <editor-fold defaultstate="collapsed" desc="Public methods">
    /**
     * Method for calculating a bit fingerprint. This method must be passed a list of unique SMILES/fragments as
     * argument. The fragments can belong to a molecule or represent arbitrary fragments. The method compares
     * this list of fragments with the list of predefined fragments/unique SMILES that was set when the
     * class was initialized. To get the expected fingerprint, unique SMILES should be used.
     * If unique SMILES are not used, the method does not deliver the expected result.
     *
     * @param aListOfUniqueSmiles is a list that stores fragments in the form of unique SMILES.
     *                            To be able to calculate the fingerprint for a molecule,
     *                            the fragments should belong to one molecule. The fragments are substructures
     *                            of the molecule.
     * @return the bit fingerprint
     */
    IBitFingerprint getBitFingerprint(List<String> aListOfUniqueSmiles);
    //
    /**
     * Method for calculating a count fingerprint. A map must be passed to this method. This map can represent a
     * molecule by its fragments, which are represented by unique SMILES in the key set and their frequencies in the
     * value set. However, this map can also contain arbitrary fragment sets. The method compares the unique SMILES
     * in the key set with the predefined fragments specified when the class is initialized to create the fingerprint.
     * To obtain the expected fingerprint, unique SMILES should be used and the keys in the input map
     * should correspond to the unique SMILES of a molecule.  If unique SMILES and molecule fragments are
     * not used, the method will not give the expected result.
     *
     * @param aUniqueSmilesToFrequencyMap is a map that maps fragments in the form of unique SMILES to
     *                                    the frequency of unique SMILES.
     *                                    To be able to calculate the fingerprint for a molecule, the fragments
     *                                    must belong to a molecule. The fragments are substructures
     *                                    of the molecule.
     * @return the count fingerprint
     */
    ICountFingerprint getCountFingerprint(Map<String, Integer> aUniqueSmilesToFrequencyMap);
    //
    /**
     * Method for calculating a count fingerprint. This method must be passed a list of unique SMILES/fragments as
     * argument. The fragments can belong to a molecule or represent arbitrary fragments. The method compares
     * this list of fragments with the list of predefined fragments/unique SMILES that was set when the class
     * was initialized.
     * To get the expected fingerprint, unique SMILES should be used on the one hand and on the other hand
     * the fragments in the input list should correspond to the unique SMILES of a molecule.
     * If unique SMILES and molecule fragments are not used, the method does not deliver the expected result.
     *
     * @param aUniqueSmilesList is a list that stores fragments in the form of unique SMILES.
     *                                     If a fragment occurs more than once in the molecule,
     *                                     it is also present more than
     *                                     once in the list. To be able to calculate the fingerprint for a molecule,
     *                                     the fragments should belong to one molecule.
     * @return the count fingerprint
     */
    ICountFingerprint getCountFingerprint(List<String> aUniqueSmilesList);
    // </editor-fold>

}
