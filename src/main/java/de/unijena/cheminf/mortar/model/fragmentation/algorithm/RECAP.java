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
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IPseudoAtom;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.isomorphism.Transform;
import org.openscience.cdk.ringsearch.RingSearch;
import org.openscience.cdk.smarts.SmartsPattern;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smirks.Smirks;
import org.openscience.cdk.smirks.SmirksTransform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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
    //TODO implement tests from RECAP paper and RDKit
    //TODO RDKit generate a mapping of SMILES code to hierarchy node to reduce the search space (deduplication)
    // and be able to add more molecules and their fragments into the map (but not the hierarchy)!
    //TODO make individual rules able to be turned off and on?
    //TODO give option to add rules
    //TODO check whether atoms, bonds, etc are copied or whether they are new
    //TODO add methods that return meaningful fragment sets, not the whole hierarchy
    //TODO how to ensure fragments appearing multiple times in the molecule appear multiple times in a deduplicated fragment set?
    //TODO discard unused methods
    //TODO track information which cleavage rules were applied?
    //TODO make public method to check how many rule matches are in a molecule (and reject mols with more than 31)
    //TODO limit tree depth?
    //TODO option for hydrogen saturation instead of R?
    //TODO hydrogen atoms should be implicit when given here
    //TODO re-visit the minimum fragment size after returning "more sensible" educts for every transformation
    /*
     * Notes on SMIRKS/SMARTS:
     * - when using the any-atom "*", be aware that it also matches pseudo (R)
     *   atoms which can be the product of other cleavages; so think about
     *   avoiding this by adding ";!#0" in the any-atom definition
     * - bond that is supposed to be broken must always be non-cyclic to be in
     *   alignment with the RECAP rules
     * - when adding more rules, avoid matching the same structures as
     *   pre-existing rules
     *
     */
    /**
     * RECAP rule nr 1: Amide.
     * <br>An aliphatic C (index 1) that is...
     * <br>-> of degree 3, i.e. we do NOT want to match H-C(=O)-N-...
     * (degree 4 is impossible because of the double bond to O)
     * <br>-> connected to another aliphatic or aromatic C in its environment
     * (C index 1 has one more possible connection but this should be to another
     * C to not match any bigger functional groups like urea, also excludes pseudo atoms,
     * i.e. we do NOT want to match R-C(=O)-N-...
     * <br>-> this environmental C should not be connected to a hetero atom via
     * a double or triple bond
     * <br>-> connected via a non-ring double bond to an aliphatic O (index 2)
     * <br>-> connected via a non-ring single bond to N (index 3) that is...
     * <br>     -> aliphatic or aromatic(!)
     * <br>     -> charged neutrally
     * <br>     -> has a degree of not 1, i.e. we do NOT want to match ...-C(=O)-NH2
     * <br>     -> NOT connected to an atom that is neither carbon nor hydrogen
     * as to not match any bigger functional groups, also excludes pseudo atoms,
     * i.e. we do NOT want to match ...-C(=O)-N-R or -N(-R)-R
     * <br>     -> NOT connected to any atom via a double or triple bond
     * <br>     -> NOT connected to two(!) carbon atoms with double bonds to
     * hetero atoms, as to not match any bigger functional groups like imide
     * <br>     -> NOT connected to a carbon atom that is connected to a hetero atom via a triple bond
     * <br>Reacts to C (index 1) connected to O (index 2) via a double bond and a
     * newly added single-bound O which is connected to an R atom (carboxylic acid).
     * <br>On the other side, there is an N (index 3) connected to an R atom (primary or secondary amine).
     * <br>Note that the N can potentially be in a ring but not the bond that is
     * to be broken (no conflict with lactam rule).
     */
    public static final CleavageRule AMIDE = new CleavageRule(
            "[C;D3;$(C-[#6]);!$(C-[#6]=[!#1;!#6]);!$(C-[#6]#[!#1;!#6]):1]" +
                    "(=!@[O:2])" +
                    "-!@[#7;+0;!D1;!$([#7][!#1;!#6]);!$([#7]=,#*);!$([#7](C=[!#1;!#6])C=[!#1;!#6]);!$([#7]C#[!#1;!#6]):3]",
            "[C:1](=[O:2])O*.*[#7:3]",
            "Amide");
    /**
     * RECAP rule nr 2: Ester.
     * <br>An aliphatic C (index 1) that is...
     * <br>-> of degree 3, i.e. we do NOT want to match H-C(=O)-O-...
     * (degree 4 is impossible because of the double bond to O)
     * <br>-> connected to another aliphatic or aromatic C in its environment
     * (C index 1 has one more possible connection but this should be to another
     * C to not match any bigger functional groups like carbonate or carbamate ester,
     * also excludes pseudo atoms, i.e. we do NOT want to match R-C(=O)-O-...
     * <br>-> this environmental C should not be connected to a hetero atom via
     * a double or triple bond
     * <br>-> connected via a non-ring double bond to an aliphatic O (index 2)
     * <br>-> connected via a non-ring bond to an aliphatic O (index 3) that is...
     * <br>     -> charged neutrally
     * <br>     -> has a degree of 2, i.e. we do NOT want to match ...-C(=O)-OH
     * <br>     -> NOT connected to an atom that is neither carbon nor hydrogen
     * as to not match any bigger functional groups, also excludes pseudo atoms,
     * i.e. we do NOT want to match ...-C(=O)-O-R
     * <br>     -> NOT connected to two(!) carbon atoms with double bonds to
     * hetero atoms, as to not match any bigger functional groups like organic acid anhydride
     * <br>     -> NOT connected to a carbon atom that is connected to a hetero atom via a triple bond
     * <br>Reacts to C (index 1) connected to O (index 2) via a double bond and a
     * newly added single-bound O which is connected to an R atom (carboxylic acid).
     * <br>On the other side, there is an O (index 3) connected to an R atom (alcohol).
     * <br>Note that this group cannot be in a ring.
     */
    public static final CleavageRule ESTER = new CleavageRule(
            "[C;D3;$(C-[#6]);!$(C-[#6]=[!#1;!#6]);!$(C-[#6]#[!#1;!#6]):1]" +
                    "(=!@[O:2])" +
                    "-!@[O;+0;D2;!$(O[!#1;!#6]);!$(O(C=[!#1;!#6])C=[!#1;!#6]);!$(OC#[!#1;!#6]):3]",
            "[C:1](=[O:2])O*.*[O:3]",
            "Ester");
    /**
     * RECAP rule nr 3.1: (secondary) Amine.
     * <br>An aliphatic N that is...
     * <br>-> charged neutrally
     * <br>-> of degree 2, i.e. we do NOT want to match primary or tertiary amines
     * <br>-> connected via non-ring single bonds to two carbon atoms (indices 1
     * and 2) that are, respectively...
     * <br>     -> aliphatic or aromatic(!)
     * <br>     -> NOT connected to an atom that is neither carbon nor hydrogen
     * via a double or triple bond as to not match any bigger functional groups
     * like amide, urea, lactam, amidine, guanidine, imide, carbamate ester, etc.
     * <br>Reacts to two primary amines.
     * <br>Note that this group cannot be in a ring but the two carbon atoms can be.
     * <br>note also that the "amine" rule from the paper was split into three rules,
     * one for secondary amines, one for tertiary amines, and one for aliphatic N hetero cycles
     */
    public static final CleavageRule SECONDARY_AMINE = new CleavageRule(
            "[N;+0;D2]" +
                    "(-!@[#6;!$([#6]=[!#1;!#6]);!$([#6]#[!#1;!#6]):1])" +
                    "-!@[#6;!$([#6]=[!#1;!#6]);!$([#6]#[!#1;!#6]):2]",
            "[#6:1]-N*.*N-[#6:2]",
            "Secondary Amine");
    /**
     * RECAP rule nr 3.2: (tertiary) Amine.
     * <br>An aliphatic N that is...
     * <br>-> charged neutrally
     * <br>-> of degree 3, i.e. we do NOT want to match primary or secondary amines
     * <br>-> connected via non-ring single bonds to three carbon atoms (indices 1, 2,
     * and 3) that are, respectively...
     * <br>     -> aliphatic or aromatic(!)
     * <br>     -> NOT connected to an atom that is neither carbon nor hydrogen
     * via a double or triple bond as to not match any bigger functional groups
     * like amide, urea, lactam, amidine, guanidine, imide, carbamate ester, etc.
     * <br>Reacts to three primary amines.
     * <br>Note that this group cannot be in a ring but the three carbon atoms can be.
     * <br>note also that the "amine" rule from the paper was split into three rules,
     * one for secondary amines, one for tertiary amines, and one for aliphatic N hetero cycles
     */
    public static final CleavageRule TERTIARY_AMINE = new CleavageRule(
            "[N;+0;D3]" +
                    "(-!@[#6;!$([#6]=[!#1;!#6]);!$([#6]#[!#1;!#6]):1])" +
                    "(-!@[#6;!$([#6]=[!#1;!#6]);!$([#6]#[!#1;!#6]):2])" +
                    "-!@[#6;!$([#6]=[!#1;!#6]);!$([#6]#[!#1;!#6]):3]",
            "[*:1]-N*.*N-[*:2].*N-[*:3]",
            "Tertiary Amine");
    /**
     * RECAP rule nr 3.3: (cyclic tertiary) Amine.
     * <br>An aliphatic N that is...
     * <br>-> part of a ring
     * <br>-> charged neutrally
     * <br>-> of degree 3
     * <br>-> not connected to a carbon atom in that same ring which is connected
     * to a hetero atom via a non-ring double bond (e.g. lactam, see rule below)
     * <br>-> not connected to a pseudo atom (atomic nr 0)
     * <br>-> connected via a non-ring single bond to a carbon atom (index 2) that is...
     * <br>     -> aliphatic or aromatic(!)
     * <br>     -> not connected to a pseudo atom (atomic nr 0)
     * <br>     -> NOT connected to an atom that is neither carbon nor hydrogen
     * via a double or triple bond as to not match any bigger functional groups
     * <br>The bond between the carbon and the ring N is cleaved and the ring and
     * a primary amine remain.
     * <br>note also that the "amine" rule from the paper was split into three rules,
     * one for secondary amines, one for tertiary amines, and one for aliphatic N hetero cycles
     */
    public static final CleavageRule CYCLIC_TERTIARY_AMINES = new CleavageRule(
            "[N;R;+0;D3;!$(N~@C=!@[!#1;!#6]);!$(N[#0]):1]" +
                    "-!@[#6;!$([#6]=[!#1;!#6]);!$([#6]#[!#1;!#6]);!$([#6][#0]):2]",
            "[N:1]*.*N-[#6:2]",
            "Cyclic Tertiary Amine");
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
    public static final CleavageRule UREA = new CleavageRule("[#7;+0;D2,D3:1]!@C(!@=O)!@[#7;+0;D2,D3:2]", "*[#7:1].[#7:2]*", "Urea");
    /**
     * 5 = Ether -> aliphatic or aromatic(!) C (index 1) connected via a
     * non-ring bond to an aliphatic O with a neutral charge, connected via
     * a non-ring bond to an aliphatic or aromatic(!) C (index 2) reacts to
     * the two carbon atoms connected to any other atom note that the ether
     * O is discarded, we do not get an alcohol or sth similar as result
     * note also that the atoms can potentially be in a ring, just not the
     * bonds
     * ";!$(O-[#6]=O)" was added to the central O to avoid matching ester groups
     */
    public static final CleavageRule ETHER = new CleavageRule("[#6:1]-!@[O;+0;!$(O-[#6]=O)]-!@[#6:2]", "[#6:1]*.*[#6:2]", "Ether");
    /**
     * 6 = Olefin -> an aliphatic C (index 1) connected via a non-ring
     * double bond to another aliphatic C (index 2) reacts to the two carbon
     * atoms each connected to any atom note that the double bond is simply
     * split, no assumption is made as to how it was synthesized note also
     * that the degree of the carbon atoms is not specified note also that
     * the atoms can potentially be in a ring, just not the bonds
     */
    public static final CleavageRule OLEFIN = new CleavageRule("[C:1]=!@[C:2]", "[C:1]*.*[C:2]", "Olefin");
    //TODO what about this? I do not think it is covered by nr 3 (amine)!
    /**
     * 7 = Quaternary nitrogen
     */
    public static final CleavageRule QUATERNARY_NITROGEN = new CleavageRule("", "", "Quaternary nitrogen");
    /**
     * 8 = Aromatic nitrogen - aliphatic carbon -> an aromatic N with a
     * neutral charge (index 1) connected via a non-ring bond to an
     * aliphatic C (index 2) reacts to both atoms connected to any other
     * atom note that no assumption is made as to how the structure was
     * synthesized note also that the atoms can potentially be in a ring
     * (the n must be), just not the bonds
     */
    public static final CleavageRule AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON = new CleavageRule("[n;+0:1]-!@[C;!$(C=O):2]", "[n:1]*.[C:2]*", "Aromatic nitrogen to aliphatic carbon");
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
    public static final CleavageRule LACTAM_NITROGEN_TO_ALIPHATIC_CARBON = new CleavageRule("[O:3]=[C:4]-@[N;+0:1]-!@[C:2]", "[O:3]=[C:4]-[N:1]*.[C:2]*", "Lactam nitrogen to aliphatic carbon");
    /**
     * 10 = Aromatic carbon - aromatic carbon -> aromatic C (index 1)
     * connected via a non-ring bond(!) to another aromatic C (index 2)
     * reacts to the bond in between being split note that no assumption is
     * made as to how the structure was synthesized
     */
    public static final CleavageRule AROMATIC_CARBON_TO_AROMATIC_CARBON = new CleavageRule("[c:1]-!@[c:2]", "[c:1]*.*[c:2]", "Aromatic carbon to aromatic carbon");
    /**
     * 11 = Sulphonamide -> an aliphatic or aromatic N with a neutral charge
     * and a degree of 2 or 3 (index 1) connected via a non-ring bond to an
     * alipathic S (index 2) connected to two aliphatic O (indices 3 and 4)
     * via double bonds reacts to the bond between N and S being split note
     * that the atoms could be in rings, just not the bond that is split
     * note that no assumption is made as to how the structure was
     * synthesized
     */
    public static final CleavageRule SULPHONAMIDE = new CleavageRule("[#7;+0;D2,D3:1]-!@[S:2](=[O:3])=[O:4]", "[#7:1]*.*[S:2](=[O:3])=[O:4]", "Sulphonamide");
    //TODO this is not part of the original RECAP, make it optional?
    /**
     * S2 = Aromatic nitrogen - aromatic carbon -> aromatic N with a neutral
     * charge (index 1) connected via a non-ring bond(!) to an aromatic C
     * (index 2) reacts to the bond in between being split note that no
     * assumption is made as to how the structure was synthesized note also
     * that both atoms are in different rings
     */
    public static final CleavageRule AROMATIC_NITROGEN_TO_AROMATIC_CARBON = new CleavageRule("[n;+0:1]-!@[c:2]", "[n:1]*.*[c:2]", "Aromatic nitrogen to aromatic carbon");
    /**
     * String array of SMIRKS reaction transform codes that describe the
     * cleavage rules.
     */
    public static final CleavageRule[] CLEAVAGE_RULES = {
            RECAP.AMIDE,
            RECAP.ESTER,
            RECAP.SECONDARY_AMINE,
            RECAP.TERTIARY_AMINE,
            RECAP.CYCLIC_TERTIARY_AMINES,
            RECAP.UREA,
            RECAP.ETHER,
            RECAP.OLEFIN,
            RECAP.QUATERNARY_NITROGEN,
            RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON,
            RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON,
            RECAP.AROMATIC_CARBON_TO_AROMATIC_CARBON,
            RECAP.SULPHONAMIDE,
            RECAP.AROMATIC_NITROGEN_TO_AROMATIC_CARBON
    };
    /**
     *
     */
    public RECAP() {
        //TODO initialise default minimum fragment size of 5
    }

    /**
     * Settings closest to original RECAP.
     *
     * @param mol
     * @return
     * @throws CDKException
     */
    public List<IAtomContainer> fragment(IAtomContainer mol) throws CDKException {
        return this.fragment(mol, false, 5);
    }

    /**
     * rings must be detected before aromaticity must be detected before
     */
    public List<IAtomContainer> fragment(IAtomContainer mol, boolean includeIntermediates, int minimumFragmentSize) throws CDKException {
        //TODO reject input mols with dummy atoms
        if (mol == null)
            throw new NullPointerException("No molecule provided");
        if (mol.isEmpty())
            return Collections.emptyList();
        State state = new State();
        return includeIntermediates? state.applyTransformationsWithAllIntermediates(mol, minimumFragmentSize) : state.applyTransformationsSinglePass(mol, minimumFragmentSize);
    }

    public HierarchyNode buildHierarchy(IAtomContainer mol) throws CDKException {
        State state = new State();
        return state.buildHierarchy(mol, 5);
    }

    public HierarchyNode buildHierarchy(IAtomContainer mol, int minimumFragmentSize) throws CDKException {
        State state = new State();
        return state.buildHierarchy(mol, minimumFragmentSize);
    }
    /**
     * TODO doc
     * public: Accessible by calling code.
     * static: Makes the class independent of the enclosing class's instances.
     * final: Prevents the class from being extended.
     */
    public static final class CleavageRule {
        private final String smirksCode;

        private final String eductSmartsCode;

        private final String productSmartsCode;

        private final String name;

        private final SmirksTransform transformation;

        private final Pattern eductPattern;

        public CleavageRule(String smirksCode, String name) {
            //TODO checks
            this(smirksCode.split(">>")[0], smirksCode.split(">>")[1], name);
        }

        public CleavageRule(String eductSmarts, String productSmarts, String name) {
            this.eductSmartsCode = eductSmarts;
            this.productSmartsCode = productSmarts;
            this.name = name;
            this.smirksCode = String.format("%s>>%s", eductSmarts, productSmarts);
            //can throw IllegalStateException if code is invalid
            this.transformation = Smirks.compile(this.smirksCode);
            //TODO is this a problem? We want cycle detection and aromaticity detection to be done externally, explicitly
            this.transformation.setPrepare(false);
            this.eductPattern = SmartsPattern.create(eductSmarts);
        }

        public String getName() {
            return this.name;
        }

        public SmirksTransform getTransformation() {
            return this.transformation;
        }

        public String getEductSmartsCode() {
            return this.eductSmartsCode;
        }

        public String getProductSmartsCode() {
            return this.productSmartsCode;
        }

        public Pattern getEductPattern() {
            return this.eductPattern;
        }

        public String getSmirksCode() {
            return this.smirksCode;
        }

        public CleavageRule copy() {
            return new CleavageRule(this.eductSmartsCode, this.productSmartsCode, this.name);
        }
    }

    /**
     * public: Accessible by calling code since this is a return value.
     * static: Makes the class independent of the enclosing class's instances.
     * final: Prevents the class from being extended.
     */
    public static final class HierarchyNode {

        private final IAtomContainer structure;

        private final List<HierarchyNode> parents;

        private final List<HierarchyNode> children;

        public HierarchyNode(IAtomContainer molecule) {
            this.structure = molecule;
            this.parents = new ArrayList<>();
            this.children = new ArrayList<>();
        }

        public IAtomContainer getStructure() {
            return structure;
        }

        public List<HierarchyNode> getParents() {
            return this.parents;
        }

        public List<IAtomContainer> getParentMolecules() {
            List<IAtomContainer> list = new ArrayList<>(this.parents.size());
            for (HierarchyNode node : this.parents) {
                list.add(node.getStructure());
            }
            return list;
        }

        public List<HierarchyNode> getChildren() {
            return this.children;
        }

        public List<IAtomContainer> getChildrenMolecules() {
            List<IAtomContainer> list = new ArrayList<>(this.children.size());
            for (HierarchyNode node : this.children) {
                list.add(node.getStructure());
            }
            return list;
        }

        public boolean isTerminal() {
            return this.children.isEmpty();
        }

        public boolean isRoot() {
            return this.parents.isEmpty();
        }

        public List<HierarchyNode> getAllDescendants() {
            List<HierarchyNode> desc = new ArrayList<>(this.children.size()^2);
            for (HierarchyNode child : this.children) {
                child.collectAllDescendants(desc, false);
            }
            return desc;
        }

        public List<HierarchyNode> getOnlyTerminalDescendants() {
            List<HierarchyNode> desc = new ArrayList<>(this.children.size()^2);
            for (HierarchyNode child : this.children) {
                if (child.isTerminal()) {
                    desc.add(child);
                } else {
                    child.collectAllDescendants(desc, true);
                }
            }
            return desc;
        }

        public int getLevel() {
            if (this.isRoot()) {
                return 0;
            } else {
                return this.parents.getFirst().getLevel() + 1;
            }
        }

        public int getMaximumLevelOfAllDescendants() {
            if (this.isTerminal()) {
                return this.getLevel();
            } else {
                int maxLevel = this.getLevel();
                for (HierarchyNode child : this.children) {
                    if (child.getMaximumLevelOfAllDescendants() > maxLevel) {
                        maxLevel = child.getMaximumLevelOfAllDescendants();
                    }
                }
                return maxLevel;
            }
        }

        private void collectAllDescendants(List<HierarchyNode> childrenList, boolean onlyTerminal) {
            //note that this is breadth first because we traverse level by level
            Queue<HierarchyNode> queue = new LinkedList<>();
            queue.add(this);
            while(!queue.isEmpty()) {
                HierarchyNode current = queue.poll();
                if (!onlyTerminal || current.getChildren().isEmpty()) {
                    childrenList.add(current);
                }
                queue.addAll(current.getChildren());
            }
        }
    }

    /**
     * Encapsulating the state of the algorithm allows thread-safe calling.
     *
     * private: Limits access to within the enclosing class.
     * final: Prevents the class from being extended.
     */
    private final class State {
    private final CleavageRule[] cleavageRules;

    private State() {
        this.cleavageRules = new CleavageRule[RECAP.CLEAVAGE_RULES.length];
        for (int i = 0; i < RECAP.CLEAVAGE_RULES.length; i++) {
            this.cleavageRules[i] = RECAP.CLEAVAGE_RULES[i].copy();
        }
    }

        /**
         *
         *
         * @param inputMol
         * @param minimumFragmentSize
         * @return
         */
        private HierarchyNode buildHierarchy(IAtomContainer inputMol, int minimumFragmentSize) throws CDKException {
            HierarchyNode inputMolNode = new HierarchyNode(inputMol);
            Queue<HierarchyNode> queue = new LinkedList<>();
            queue.add(inputMolNode);
            while (!queue.isEmpty()) {
                HierarchyNode currentNode = queue.poll();
                for (CleavageRule rule : this.cleavageRules) {
                    if (rule.getEductPattern().matches(currentNode.getStructure())) {
                        //mode unique returns as many products as there are splittable bonds, so one product for every bond split
                        for (IAtomContainer product : rule.getTransformation().apply(currentNode.getStructure(), Transform.Mode.Unique)) {
                            List<IAtomContainer> parts = new ArrayList<>();
                            boolean containsForbiddenFragment = false;
                            for (IAtomContainer part : ConnectivityChecker.partitionIntoMolecules(product).atomContainers()) {
                                parts.add(part);
                                if (this.isFragmentForbidden(part, minimumFragmentSize)) {
                                    containsForbiddenFragment = true;
                                    break;
                                }
                            }
                            if (!containsForbiddenFragment) {
                                //Logger.getLogger(RECAP.class.getName()).log(Level.INFO, "Transformation rule " + rule.getName() + " matched and produced valid fragments.");
                                //Logger.getLogger(RECAP.class.getName()).log(Level.INFO, "Educt " + (new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols)).create(currentNode.getStructure()));
                                for (IAtomContainer part : parts) {
                                    //Logger.getLogger(RECAP.class.getName()).log(Level.INFO, "Product " + (new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols)).create(part));
                                    HierarchyNode newNode = new HierarchyNode(part);
                                    newNode.getParents().add(currentNode);
                                    currentNode.getChildren().add(newNode);
                                    queue.add(newNode);
                                }
                            }
                        }
                    }
                }

            }
            return inputMolNode;
        }

        /**
         *
         */
        private List<IAtomContainer> applyTransformationsSinglePass(IAtomContainer mol, int minimumFragmentSize) {
            //TODO what if mol has no cleaving bonds?
            //TODO this still includes intermediates!
            List<IAtomContainer> lastRoundFragments = new ArrayList<>(mol.getAtomCount() * 2);
            lastRoundFragments.add(mol);
            for (CleavageRule rule : this.cleavageRules) {
                List<IAtomContainer> newRoundFragments = new ArrayList<>(mol.getAtomCount() * 2);
                for (IAtomContainer fragment : lastRoundFragments) {
                    if (rule.getEductPattern().matches(fragment)) {
                        //mode unique returns as many products as there are splittable bonds, so one product for every bond split
                        Iterable<IAtomContainer> products = rule.getTransformation().apply(fragment, Transform.Mode.Unique);
                        for (IAtomContainer product : products) {
                            //TODO if it is connected, it was not split, so this should not happen!
                            if (ConnectivityChecker.isConnected(product)) {
                                if (!this.isFragmentForbidden(product, minimumFragmentSize)) {
                                    newRoundFragments.add(product);
                                }
                            } else {
                                List<IAtomContainer> parts = new ArrayList<>(mol.getAtomCount());
                                boolean containsForbiddenFragment = false;
                                for (IAtomContainer part : ConnectivityChecker.partitionIntoMolecules(product).atomContainers()) {
                                    parts.add(part);
                                    if (this.isFragmentForbidden(part, minimumFragmentSize)) {
                                        containsForbiddenFragment = true;
                                        break;
                                    }
                                }
                                if (!containsForbiddenFragment) {
                                    newRoundFragments.addAll(parts);
                                }
                            }
                        } // end of cleavage product iteration
                    } else {
                        // simply pass fragment on to next round if this rule does not match
                        newRoundFragments.add(fragment);
                    }
                } // end of iteration of last round (i.e. last cleavage rule) fragments
                //TODO can this be empty?
                if (!newRoundFragments.isEmpty()) {
                    lastRoundFragments = List.copyOf(newRoundFragments);
                    newRoundFragments.clear();
                }
            } // end of cleavage rule iteration
            return lastRoundFragments;
        }

        /**
         *
         */
        private List<IAtomContainer> applyTransformationsWithAllIntermediates(IAtomContainer mol, int minimumFragmentSize) throws CDKException {
            //TODO what if mol has no cleaving bonds?
            //TODO what if mol should have multiple fragments that are essentially the same?
            Map<String, IAtomContainer> finalFragments = new HashMap<>(mol.getAtomCount() * 2);
            SmilesGenerator smilesGenerator = new SmilesGenerator(SmiFlavor.Absolute | SmiFlavor.UseAromaticSymbols);
            //step 1 determine relevant transformation rules that have at least one match in the mol
            List<CleavageRule> matchingRules = new ArrayList<>(this.cleavageRules.length);
            for (CleavageRule rule : this.cleavageRules) {
                if (rule.getEductPattern().matches(mol)) {
                    matchingRules.add(rule);
                }
            }
            //step 2 do the first round of fragmentation, split each splittable bond once, determine nr of splittable bonds
            List<IAtomContainer> temporaryRoundFragments = new ArrayList<>(mol.getAtomCount());
            int splittableBondsNr = 0;
            for (CleavageRule transformation : matchingRules) {
                //mode unique returns as many products as there are splittable bonds, so one product for every bond split
                Iterable<IAtomContainer> products = transformation.getTransformation().apply(mol, Transform.Mode.Unique);
                List<IAtomContainer> temporaryProductsList = new ArrayList<>(mol.getAtomCount());
                for (IAtomContainer product : products) {
                    splittableBondsNr++;
                    if (ConnectivityChecker.isConnected(product)) {
                        if (!this.isFragmentForbidden(product, minimumFragmentSize)) {
                            temporaryProductsList.add(product);
                        }
                    } else {
                        for (IAtomContainer part : ConnectivityChecker.partitionIntoMolecules(product).atomContainers()) {
                            if (!this.isFragmentForbidden(part, minimumFragmentSize)) {
                                temporaryProductsList.add(part);
                            }
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
            //step 3 generate all intermediate and final fragments, starting with the fragments of the first round
            List<IAtomContainer> lastRoundFragments = List.copyOf(temporaryRoundFragments);
            temporaryRoundFragments.clear();
            for (int i = 1; i < splittableBondsNr; i++) {
                for (IAtomContainer fragment : lastRoundFragments) {
                    for (CleavageRule transformation : matchingRules) {
                        Iterable<IAtomContainer> products = transformation.getTransformation().apply(fragment, Transform.Mode.Unique);
                        for (IAtomContainer product : products) {
                            if (ConnectivityChecker.isConnected(product)) {
                                if (!this.isFragmentForbidden(product, minimumFragmentSize)) {
                                    temporaryRoundFragments.add(product);
                                }
                            } else {
                                for (IAtomContainer part : ConnectivityChecker.partitionIntoMolecules(product).atomContainers()) {
                                    if (!this.isFragmentForbidden(part, minimumFragmentSize)) {
                                        temporaryRoundFragments.add(part);
                                    }
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

        /**
         * "If the *terminal* fragment to be cleaved contains only small functional
         * groups *(hydrogen, methyl, ethyl, propyl, and butyl)*, the fragment is
         * *left uncleaved*. [...] The reason for this is, first, to avoid generating
         * "uninteresting" small fragments such as methyl in the analysis and,
         * second, with these smaller functional groups attached to the larger
         * fragments such as those shown in Figure 3, more "drug-like" features
         * are retained compared to the "barer" fragments where these small
         * functionalities are cleaved off."
         *
         * forbidden are too small terminal fragments that only consist of methyl, ethyl, propyl, etc.
         *
         * @param mol
         * @param minimumFragmentSize
         * @return
         */
        private boolean isFragmentForbidden(IAtomContainer mol, int minimumFragmentSize) {
            //note: mol.getAtomCount () < minimumFragmentSize ? would count pseudo atoms, so not a good option
            int pseudoAtomCounter = 0;
            for (IAtom atom : mol.atoms()) {
                boolean isPseudoAtom = this.isPseudoAtom(atom);
                if (isPseudoAtom) {
                    pseudoAtomCounter++;
                }
                if (pseudoAtomCounter >= 2) {
                    //fragment is not terminal, it has two cleavage sites
                    return false;
                }
                boolean isHeteroAtom = this.isHeteroAtom(atom);
                if (isHeteroAtom) {
                    //fragment is not a simple alkyl fragment
                    return false;
                }
            }
            //R count is 1 (or theoretically 0)
            //no hetero atoms in fragment
            if ((mol.getAtomCount() - pseudoAtomCounter) < minimumFragmentSize) {
                //costly ring search only for small fragments
                RingSearch ringSearch = new RingSearch(mol);
                //forbidden if linear, otherwise too interesting
                return ringSearch.numRings() == 0;
            } else {
                //not forbidden, bigger than or equal to minimum fragment size
                return false;
            }
        }
        //TODO: in the CDK integration, use the corresponding methods of functionalgroupsfinder or put them in a central utility class
        /**
         * Checks whether the given atom is a pseudo atom. Very strict, any atom
         * whose atomic number is null or 0, whose symbol equals "R" or "*", or that
         * is an instance of an IPseudoAtom implementing class will be classified as
         * a pseudo atom.
         *
         * @param atom the atom to test
         * @return true if the given atom is identified as a pseudo (R) atom
         */
        private boolean isPseudoAtom(IAtom atom) {
            Integer tmpAtomicNr = atom.getAtomicNumber();
            if (tmpAtomicNr == null) {
                return true;
            }
            String tmpSymbol = atom.getSymbol();
            return tmpAtomicNr == IElement.Wildcard ||
                    tmpSymbol.equals("R") ||
                    tmpSymbol.equals("*") ||
                    atom instanceof IPseudoAtom;
        }

        /**
         * Checks whether the given atom is a hetero-atom (i.e. non-carbon and
         * non-hydrogen). Pseudo (R) atoms will also return false.
         *
         * @param atom the atom to test
         * @return true if the given atom is neither a carbon nor a hydrogen or
         *         pseudo atom
         */
        private boolean isHeteroAtom(IAtom atom) {
            Integer tmpAtomicNr = atom.getAtomicNumber();
            if (tmpAtomicNr == null) {
                return false;
            }
            int tmpAtomicNumberInt = tmpAtomicNr;
            return tmpAtomicNumberInt != IElement.H && tmpAtomicNumberInt != IElement.C
                    && !this.isPseudoAtom(atom);
        }

    }
}
