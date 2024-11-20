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

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.isomorphism.Transform;
import org.openscience.cdk.smarts.SmartsPattern;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smirks.Smirks;
import org.openscience.cdk.smirks.SmirksTransform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the
 * <a href="https://doi.org/10.1186/s13321-017-0225-z">RECAP - Retrosynthetic
 * Combinatorial Analysis Procedure</a>
 * algorithm. The SMIRKS strings are taken from the
 * <a
 * href="https://github.com/rdkit/rdkit/blob/master/rdkit/Chem/Recap.py">RDKit
 * implementation</a>
 * done by Greg Landrum (last access 15th Nov 2024).
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class RECAP {

    private static final class CleavageRule {

        private final String SMIRKS_CODE;

        private final String EDUCT_SMARTS_CODE;

        private final String PRODUCT_SMARTS_CODE;

        private final String NAME;

        private final SmirksTransform transformation;

        private Pattern eductPattern;

        private CleavageRule(String eductSmarts, String productSmarts, String name) {
            this.EDUCT_SMARTS_CODE = eductSmarts;
            this.PRODUCT_SMARTS_CODE = productSmarts;
            this.NAME = name;
            this.SMIRKS_CODE = String.format("%s>>%s", eductSmarts, productSmarts);
            //can throw IllegalStateException if code is invalid
            this.transformation = Smirks.compile(this.SMIRKS_CODE);
            this.transformation.setPrepare(false);
            this.eductPattern = SmartsPattern.create(eductSmarts);
        }

        private String getName() {
            return this.NAME;
        }

        private SmirksTransform getTransformation() {
            return this.transformation;
        }

        private String getEductSmartsCode() {
            return this.EDUCT_SMARTS_CODE;
        }

        private String getProductSmartsCode() {
            return this.PRODUCT_SMARTS_CODE;
        }

        private Pattern getEductPattern() {
            return this.eductPattern;
        }
    }
    //TODO option for minimum fragment size (also described in RECAP paper)
    //TODO option for "exhaustive" fragmentation
    //TODO implement tests from RECAP paper and RDKit
    //TODO make individual rules able to be turned off and on?
    //TODO give option to add rules

    /**
     *
     */
    public RECAP() {

    }

    /**
     * rings must be detected before aromaticity must be detected before
     */
    public List<IAtomContainer> fragment(IAtomContainer mol) throws CDKException {
        if (mol == null)
            throw new NullPointerException("No molecule provided");
        if (mol.isEmpty())
            return Collections.emptyList();
        State state = new State();
        return state.applyTransformations(mol);
    }

    /**
     * Encapsulating the state of the algorithm allows thread-safe calling.
     */
    private static final class State {
        /**
         * 1 = Amide -> aliphatic C that is NOT connected to two N (as not to
         * match urea) (index 1), connected via a non-ring double bond to an
         * aliphatic O (index 2), connected via a non-ring bond to N with a
         * neutral charge and a degree of not 1, can be aliphatic or aromatic
         * (index 3) -> reacts to C index 1 connected to O index 2 and to any
         * other atom and N index 3 connected to any other atom -> note that the
         * result is an aldehyde and an amine, not a carboxylic acid or ester
         * and an amine -> note also that the atoms can potentially be in a
         * ring, just not the bonds
         */
        private final CleavageRule AMIDE = new CleavageRule("[C;!$(C([#7])[#7]):1](=!@[O:2])!@[#7;+0;!D1:3]", "*[C:1]=[O:2].*[#7:3]", "Amide");
        /**
         * 2 = Ester -> aliphatic C (index 1), connected via a non-ring double
         * bond to aliphatic O (index 2) as a side chain, connected via a
         * non-ring bond to an aliphatic O with a neutral charge (index 3) ->
         * reacts to C index 1 connected to O index 2 and to any other atom and
         * O index 3 connected to any other atom -> note that the result is an
         * aldehyde (not a carboxylic acid) and an alcohol -> note also that the
         * atoms can potentially be in a ring, just not the bonds
         */
        private final CleavageRule ESTER = new CleavageRule("[C:1](=!@[O:2])!@[O;+0:3]", "*[C:1]=[O:2].[O:3]*", "Ester");
        //TODO does this also work for tertiary amines? I guess it matches multiple times?
        /**
         * 3 = Amine -> aliphatic N with a neutral charge and a degree of NOT 1
         * that is also NOT connected to a C connected via a double bond to N,
         * O, P, or S (to avoid any form of amides) but connected via a non-ring
         * bond to any atom (index 1) as a side chain and via a non-ring bond to
         * any atom (index 2) -> reacts to the two any atoms with indices 1 and
         * 2, each connected to any other atom -> note that the amine / N is
         * discarded! -> note also that the atoms can potentially be in a ring,
         * just not the bonds -> simpler alternative would be (without excluding
         * any sort of amides): [N;!D1](!@[*:1])!@[*:2]>>*[*:1].[*:2]*
         */
        private final CleavageRule AMINE = new CleavageRule("[N;!D1;+0;!$(N-C=[#7,#8,#15,#16])](-!@[*:1])-!@[*:2]", "*[*:1].[*:2]*", "Amine");
        /**
         * 4 = Urea -> aliphatic or aromatic(!) N with a neutral charge and a
         * degree of 2 or 3 (index 1), connected via a non-ring bond to an
         * aliphatic C, connected via a non-ring double bond to an aliphatic O
         * as a side chain and via a non-ring bond to another aliphatic or
         * aromatic(!) N with a neutral charge and a degree of 2 or 3 (index 2)
         * reacts to two N atoms (indices 1 and 2) that are unconnected but each
         * to connected to any other atom note that the central keto group is
         * discarded! We get two amines as a result of the reaction (one
         * possible way to synthesize a urea functionality) note also that the
         * atoms can potentially be in a ring, just not the bonds
         */
        private final CleavageRule UREA = new CleavageRule("[#7;+0;D2,D3:1]!@C(!@=O)!@[#7;+0;D2,D3:2]", "*[#7:1].[#7:2]*", "Urea");
        /**
         * 5 = Ether -> aliphatic or aromatic(!) C (index 1) connected via a
         * non-ring bond to an aliphatic O with a neutral charge, connected via
         * a non-ring bond to an aliphatic or aromatic(!) C (index 2) reacts to
         * the two carbon atoms connected to any other atom note that the ether
         * O is discarded, we do not get an alcohol or sth similar as result
         * note also that the atoms can potentially be in a ring, just not the
         * bonds
         */
        private final CleavageRule ETHER = new CleavageRule("[#6:1]-!@[O;+0]-!@[#6:2]", "[#6:1]*.*[#6:2]", "Ether");
        /**
         * 6 = Olefin -> an aliphatic C (index 1) connected via a non-ring
         * double bond to another aliphatic C (index 2) reacts to the two carbon
         * atoms each connected to any atom note that the double bond is simply
         * split, no assumption is made as to how it was synthesized note also
         * that the degree of the carbon atoms is not specified note also that
         * the atoms can potentially be in a ring, just not the bonds
         */
        private final CleavageRule OLEFIN = new CleavageRule("[C:1]=!@[C:2]", "[C:1]*.*[C:2]", "Olefin");
        //TODO what about this? I do not think it is covered by nr 3 (amine)!
        /**
         * 7 = Quaternary nitrogen
         */
        private final CleavageRule QUATERNARY_NITROGEN = new CleavageRule("", "", "Quaternary nitrogen");
        /**
         * 8 = Aromatic nitrogen - aliphatic carbon -> an aromatic N with a
         * neutral charge (index 1) connected via a non-ring bond to an
         * aliphatic C (index 2) reacts to both atoms connected to any other
         * atom note that no assumption is made as to how the structure was
         * synthesized note also that the atoms can potentially be in a ring
         * (the n must be), just not the bonds
         */
        private final CleavageRule AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON = new CleavageRule("[n;+0:1]-!@[C:2]", "[n:1]*.[C:2]*", "Aromatic nitrogen to aliphatic carbon");
        /**
         * 9 = Lactam nitrogen - aliphatic carbon -> an aliphatic O (index 3)
         * connected via a double bond (ring or non-ring) to an aliphatic C
         * (index 4) connected via a ring bond(!) to an aliphatic N with a
         * neutral charge (index 1) connected via a non-ring bond to an
         * aliphatic C (index 2) reacts to the C index 2 being split from the
         * rest of the structure note that C index 2 could be in different ring
         * note also that no assumption is made as to how the structure was
         * synthesized
         */
        private final CleavageRule LACTAM_NITROGEN_TO_ALIPHATIC_CARBON = new CleavageRule("[O:3]=[C:4]-@[N;+0:1]-!@[C:2]", "[O:3]=[C:4]-[N:1]*.[C:2]*", "Lactam nitrogen to aliphatic carbon");
        /**
         * 10 = Aromatic carbon - aromatic carbon -> aromatic C (index 1)
         * connected via a non-ring bond(!) to another aromatic C (index 2)
         * reacts to the bond in between being split note that no assumption is
         * made as to how the structure was synthesized
         */
        private final CleavageRule AROMATIC_CARBON_TO_AROMATIC_CARBON = new CleavageRule("[c:1]-!@[c:2]", "[c:1]*.*[c:2]", "Aromatic carbon to aromatic carbon");
        /**
         * 11 = Sulphonamide -> an aliphatic or aromatic N with a neutral charge
         * and a degree of 2 or 3 (index 1) connected via a non-ring bond to an
         * alipathic S (index 2) connected to two aliphatic O (indices 3 and 4)
         * via double bonds reacts to the bond between N and S being split note
         * that the atoms could be in rings, just not the bond that is split
         * note that no assumption is made as to how the structure was
         * synthesized
         */
        private final CleavageRule SULPHONAMIDE = new CleavageRule("[#7;+0;D2,D3:1]-!@[S:2](=[O:3])=[O:4]", "[#7:1]*.*[S:2](=[O:3])=[O:4]", "Sulphonamide");
        //TODO this is not part of the original RECAP, make it optional?
        /**
         * S1 = Cyclic amines -> an aliphatic or aromatic N in a ring, with a
         * degree of 3, and a neutral charge (index 1) connected via a non-ring
         * bond to any atom (index 2) reacts to the N connected to any atom and
         * the other atom connected to any atom note that no assumption is made
         * as to how the structure was synthesized
         */
        private final CleavageRule CYCLIC_AMINES = new CleavageRule("[#7;R;D3;+0:1]-!@[*:2]", "*[#7:1].[*:2]*", "Cyclic amines");
        //TODO this is not part of the original RECAP, make it optional?
        /**
         * S2 = Aromatic nitrogen - aromatic carbon -> aromatic N with a neutral
         * charge (index 1) connected via a non-ring bond(!) to an aromatic C
         * (index 2) reacts to the bond in between being split note that no
         * assumption is made as to how the structure was synthesized note also
         * that both atoms are in different rings
         */
        private final CleavageRule AROMATIC_NITROGEN_TO_AROMATIC_CARBON = new CleavageRule("[n;+0:1]-!@[c:2]", "[n:1]*.*[c:2]", "Aromatic nitrogen to aromatic carbon");
        /**
         * String array of SMIRKS reaction transform codes that describe the
         * cleavage rules.
         */
        private final CleavageRule[] CLEAVAGE_RULES = {this.AMIDE, this.ESTER,
                this.AMINE, this.UREA, this.ETHER, this.OLEFIN, this.QUATERNARY_NITROGEN,
                this.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON, this.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON,
                this.AROMATIC_CARBON_TO_AROMATIC_CARBON, this.SULPHONAMIDE, this.CYCLIC_AMINES,
                this.AROMATIC_NITROGEN_TO_AROMATIC_CARBON
        };

        /**
         *
         */
        private List<IAtomContainer> applyTransformations(IAtomContainer mol) throws CDKException {
            Map<String, IAtomContainer> finalFragments = new HashMap<>(mol.getAtomCount() * 2);
            SmilesGenerator smilesGenerator = new SmilesGenerator(SmiFlavor.Absolute | SmiFlavor.UseAromaticSymbols);
            //step 1 determine relevant transformation rules that have at least one match in the mol
            List<CleavageRule> matchingRules = new ArrayList<>(this.CLEAVAGE_RULES.length);
            for (CleavageRule rule : this.CLEAVAGE_RULES) {
                if (rule.eductPattern.matches(mol)) {
                    matchingRules.add(rule);
                }
            }
            //step 2 do the first round of fragmentation, split each splittable bond once
            List<IAtomContainer> temporaryRoundFragments = new ArrayList<>(mol.getAtomCount());
            int splittableBondsNr = 0;
            for (CleavageRule transformation : matchingRules) {
                //mode exclusive returns one(!) product, respectively, where every matching group has been split
                //mode unique returns as many products as there are splittable bonds, so one product for every bond split
                Iterable<IAtomContainer> products = transformation.getTransformation().apply(mol, Transform.Mode.Unique);
                List<IAtomContainer> temporaryProductsList = new ArrayList<>(mol.getAtomCount());
                for (IAtomContainer product : products) {
                    splittableBondsNr++;
                    if (ConnectivityChecker.isConnected(product)) {
                        temporaryProductsList.add(product);
                    } else {
                        for (IAtomContainer part : ConnectivityChecker.partitionIntoMolecules(product).atomContainers()) {
                            temporaryProductsList.add(part);
                        }
                    }
                }
                if (!temporaryProductsList.isEmpty()) {
                    temporaryRoundFragments.addAll(temporaryProductsList);
                }
            }
            for (IAtomContainer fragment: temporaryRoundFragments) {
                finalFragments.putIfAbsent(smilesGenerator.create(fragment), fragment);
            }
            List<IAtomContainer> lastRoundFragments = List.copyOf(temporaryRoundFragments);
            temporaryRoundFragments.clear();
            for (int i = 1; i <= splittableBondsNr; i++) {
                for (IAtomContainer fragment : lastRoundFragments) {
                    for (CleavageRule transformation : matchingRules) {
                        Iterable<IAtomContainer> products = transformation.getTransformation().apply(fragment, Transform.Mode.Unique);
                        for (IAtomContainer product : products) {
                            if (ConnectivityChecker.isConnected(product)) {
                                temporaryRoundFragments.add(product);
                            } else {
                                for (IAtomContainer part : ConnectivityChecker.partitionIntoMolecules(product).atomContainers()) {
                                    temporaryRoundFragments.add(part);
                                }
                            }
                        }
                    }
                }
                for (IAtomContainer fragment: temporaryRoundFragments) {
                    finalFragments.putIfAbsent(smilesGenerator.create(fragment), fragment);
                }
                lastRoundFragments = List.copyOf(temporaryRoundFragments);
                temporaryRoundFragments.clear();
            }
            return finalFragments.values().stream().toList();
//            List<IAtomContainer> finalFragments = new ArrayList<>(mol.getAtomCount() * 2);
//            List<IAtomContainer> firstRoundFragments = new ArrayList<>(mol.getAtomCount());
//            List<CleavageRule> matchingRules = new ArrayList<>(this.CLEAVAGE_RULES.length);
//            for (CleavageRule transformation : this.CLEAVAGE_RULES) {
//                //mode exclusive returns one(!) product, respectively, where every matching group has been split
//                //mode unique returns as many products as there are splittable bonds, so one product for every bond split
//                Iterable<IAtomContainer> products = transformation.getTransformation().apply(mol, Transform.Mode.Unique);
//                List<IAtomContainer> temporaryProductsList = new ArrayList<>(mol.getAtomCount());
//                for (IAtomContainer product : products) {
//                    if (ConnectivityChecker.isConnected(product)) {
//                        temporaryProductsList.add(product);
//                    } else {
//                        for (IAtomContainer part : ConnectivityChecker.partitionIntoMolecules(product).atomContainers()) {
//                            temporaryProductsList.add(part);
//                        }
//                    }
//                }
//                if (!temporaryProductsList.isEmpty()) {
//                    firstRoundFragments.addAll(temporaryProductsList);
//                    matchingRules.add(transformation);
//                }
//            }
//            finalFragments.addAll(firstRoundFragments);
//            for (IAtomContainer fragment : firstRoundFragments) {
//                for (CleavageRule rule : matchingRules) {
//                    Iterable<IAtomContainer> products = rule.getTransformation().apply(fragment, Transform.Mode.Unique);
//                    List<IAtomContainer> temporaryProductsList = new ArrayList<>(mol.getAtomCount());
//                    for (IAtomContainer product : products) {
//                        if (ConnectivityChecker.isConnected(product)) {
//                            temporaryProductsList.add(product);
//                        } else {
//                            for (IAtomContainer part : ConnectivityChecker.partitionIntoMolecules(product).atomContainers()) {
//                                temporaryProductsList.add(part);
//                            }
//                        }
//                    }
//                    if (!temporaryProductsList.isEmpty()) {
//                        finalFragments.addAll(temporaryProductsList);
//                    }
//                }
//            }
//            return finalFragments;

//            List<IAtomContainer> fragments = null;
//            for (CleavageRule transformation : this.CLEAVAGE_RULES) {
//                if (fragments == null || fragments.isEmpty()) {
//                    fragments = new ArrayList<>(mol.getAtomCount() * 2);
//                    Iterable<IAtomContainer> products = transformation.getTransformation().apply(mol, Transform.Mode.Exclusive);
//                    for (IAtomContainer product : products) {
//                        if (ConnectivityChecker.isConnected(product)) {
//                            fragments.add(product);
//                        } else {
//                            for (IAtomContainer part : ConnectivityChecker.partitionIntoMolecules(product).atomContainers()) {
//                                fragments.add(part);
//                            }
//                        }
//                    }
//                } else {
//                    List<IAtomContainer> newFragments = new ArrayList<>(fragments.size() * 2);
//                    for (IAtomContainer fragment : fragments) {
//                        Iterable<IAtomContainer> products = transformation.getTransformation().apply(fragment, Transform.Mode.Exclusive);
//                        for (IAtomContainer product : products) {
//                            if (ConnectivityChecker.isConnected(product)) {
//                                newFragments.add(product);
//                            } else {
//                                for (IAtomContainer part : ConnectivityChecker.partitionIntoMolecules(product).atomContainers()) {
//                                    newFragments.add(part);
//                                }
//                            }
//                        }
//                    }
//                    if (!newFragments.isEmpty()) {
//                        fragments = newFragments;
//                    }
//                }
//            }
//            return fragments;
        }
    }
}
