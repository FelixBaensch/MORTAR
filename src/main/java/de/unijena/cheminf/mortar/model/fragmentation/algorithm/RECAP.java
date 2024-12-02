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
    /**
     * TODO doc
     * private: Limits access to within the enclosing class.
     * static: Makes the class independent of the enclosing class's instances.
     * final: Prevents the class from being extended.
     */
    private static final class CleavageRule {
        //TODO move to State?
        private final String smirksCode;

        private final String eductSmartsCode;

        private final String productSmartsCode;

        private final String name;

        private final SmirksTransform transformation;

        private final Pattern eductPattern;

        private CleavageRule(String smirksCode, String name) {
            //TODO checks
            this(smirksCode.split(">>")[0], smirksCode.split(">>")[1], name);
        }

        private CleavageRule(String eductSmarts, String productSmarts, String name) {
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

        private String getName() {
            return this.name;
        }

        private SmirksTransform getTransformation() {
            return this.transformation;
        }

        private String getEductSmartsCode() {
            return this.eductSmartsCode;
        }

        private String getProductSmartsCode() {
            return this.productSmartsCode;
        }

        private Pattern getEductPattern() {
            return this.eductPattern;
        }

        private String getSmirksCode() {
            return this.smirksCode;
        }
    }
    //TODO implement tests from RECAP paper and RDKit
    //TODO make individual rules able to be turned off and on?
    //TODO give option to add rules
    //TODO check whether atoms, bonds, etc are copied or whether they are new
    //TODO add methods that return meaningful fragment sets, not the whole hierarchy
    //TODO discard unused methods
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
     * public: Accessible by calling code since this is a return value.
     * static: Makes the class independent of the enclosing class's instances.
     * final: Prevents the class from being extended.
     */
    public static final class HierarchyNode {

        private final IAtomContainer structure;

        private final List<HierarchyNode> parents;

        private final List<HierarchyNode> children;

        private HierarchyNode(IAtomContainer molecule) {
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

        private void collectAllDescendants(List<HierarchyNode> childrenList, boolean onlyTerminal) {
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
     * private: Limits access to within the enclosing class.
     * static: Makes the class independent of the enclosing class's instances.
     * final: Prevents the class from being extended.
     */
    private static final class State {
        /*
        * Notes on SMIRKS/SMARTS:
        * - when using the any-atom "*", be aware that it also matches pseudo (R)
        *   atoms; so think about avoiding this by adding ";!#0" in the any-atom
        *   definition
        * */
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
         * TODO: insert ";!$([#7][#0]);!$([#7]([#0])[#0])" was inserted to avoid matching pseudo atoms that resulted from a previous cleavage?
         */
        //private final CleavageRule amide = new CleavageRule("[C;!$(C([#7])[#7]):1](=!@[O:2])!@[#7;+0;!D1;!$([#7][#0]);!$([#7]([#0])[#0]):3]", "*[C:1]=[O:2].*[#7:3]", "Amide");
        private final CleavageRule amide = new CleavageRule("[C;!$(C([#7])[#7]):1](=!@[O:2])!@[#7;+0;!D1:3]", "*[C:1]=[O:2].*[#7:3]", "Amide");
        /**
         * 2 = Ester -> aliphatic C (index 1), connected via a non-ring double
         * bond to aliphatic O (index 2) as a side chain, connected via a
         * non-ring bond to an aliphatic O with a neutral charge (index 3) ->
         * reacts to C index 1 connected to O index 2 and to any other atom and
         * O index 3 connected to any other atom -> note that the result is an
         * aldehyde (not a carboxylic acid) and an alcohol -> note also that the
         * atoms can potentially be in a ring, just not the bonds
         */
        private final CleavageRule ester = new CleavageRule("[C:1](=!@[O:2])!@[O;+0:3]", "*[C:1]=[O:2].[O:3]*", "Ester");
        //TODO does this also work for tertiary amines? I guess it matches multiple times?
        //TODO this does not align with the RECAP paper definition of the amine cleavage rule, i.e. it does not match the example structure; see also additional rule "cyclic amines" below
        /**
         * 3 = Amine -> aliphatic N with a neutral charge and a degree of NOT 1
         * that is also NOT connected to a C connected via a double bond to N,
         * O, P, or S (to avoid any form of amides) but connected via a non-ring
         * bond to any atom (index 1) as a side chain and via a non-ring bond to
         * any atom (index 2) -> reacts to the two any atoms with indices 1 and
         * 2, each connected to any other atom -> note that the amine / N is
         * discarded! -> note also that the atoms can potentially be in a ring,
         * just not the bonds -> simpler alternative would be (without excluding
         * any sort of amines): [N;!D1](!@[*:1])!@[*:2]>>*[*:1].[*:2]*
         * ";!#0" was added for the two any-atoms to avoid matching pseudo atoms that resulted from a previous cleavage
         */
        private final CleavageRule amine = new CleavageRule("[N;!D1;+0;!$(N-C=[#7,#8,#15,#16])](-!@[*;!#0:1])-!@[*;!#0:2]", "*[*:1].[*:2]*", "Amine");
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
        private final CleavageRule urea = new CleavageRule("[#7;+0;D2,D3:1]!@C(!@=O)!@[#7;+0;D2,D3:2]", "*[#7:1].[#7:2]*", "Urea");
        /**
         * 5 = Ether -> aliphatic or aromatic(!) C (index 1) connected via a
         * non-ring bond to an aliphatic O with a neutral charge, connected via
         * a non-ring bond to an aliphatic or aromatic(!) C (index 2) reacts to
         * the two carbon atoms connected to any other atom note that the ether
         * O is discarded, we do not get an alcohol or sth similar as result
         * note also that the atoms can potentially be in a ring, just not the
         * bonds
         */
        private final CleavageRule ether = new CleavageRule("[#6:1]-!@[O;+0]-!@[#6:2]", "[#6:1]*.*[#6:2]", "Ether");
        /**
         * 6 = Olefin -> an aliphatic C (index 1) connected via a non-ring
         * double bond to another aliphatic C (index 2) reacts to the two carbon
         * atoms each connected to any atom note that the double bond is simply
         * split, no assumption is made as to how it was synthesized note also
         * that the degree of the carbon atoms is not specified note also that
         * the atoms can potentially be in a ring, just not the bonds
         */
        private final CleavageRule olefin = new CleavageRule("[C:1]=!@[C:2]", "[C:1]*.*[C:2]", "Olefin");
        //TODO what about this? I do not think it is covered by nr 3 (amine)!
        /**
         * 7 = Quaternary nitrogen
         */
        private final CleavageRule quaternaryNitrogen = new CleavageRule("", "", "Quaternary nitrogen");
        /**
         * 8 = Aromatic nitrogen - aliphatic carbon -> an aromatic N with a
         * neutral charge (index 1) connected via a non-ring bond to an
         * aliphatic C (index 2) reacts to both atoms connected to any other
         * atom note that no assumption is made as to how the structure was
         * synthesized note also that the atoms can potentially be in a ring
         * (the n must be), just not the bonds
         */
        private final CleavageRule aromaticNitrogenToAliphaticCarbon = new CleavageRule("[n;+0:1]-!@[C:2]", "[n:1]*.[C:2]*", "Aromatic nitrogen to aliphatic carbon");
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
        private final CleavageRule lactamNitrogenToAliphaticCarbon = new CleavageRule("[O:3]=[C:4]-@[N;+0:1]-!@[C:2]", "[O:3]=[C:4]-[N:1]*.[C:2]*", "Lactam nitrogen to aliphatic carbon");
        /**
         * 10 = Aromatic carbon - aromatic carbon -> aromatic C (index 1)
         * connected via a non-ring bond(!) to another aromatic C (index 2)
         * reacts to the bond in between being split note that no assumption is
         * made as to how the structure was synthesized
         */
        private final CleavageRule aromaticCarbonToAromaticCarbon = new CleavageRule("[c:1]-!@[c:2]", "[c:1]*.*[c:2]", "Aromatic carbon to aromatic carbon");
        /**
         * 11 = Sulphonamide -> an aliphatic or aromatic N with a neutral charge
         * and a degree of 2 or 3 (index 1) connected via a non-ring bond to an
         * alipathic S (index 2) connected to two aliphatic O (indices 3 and 4)
         * via double bonds reacts to the bond between N and S being split note
         * that the atoms could be in rings, just not the bond that is split
         * note that no assumption is made as to how the structure was
         * synthesized
         */
        private final CleavageRule sulphonamide = new CleavageRule("[#7;+0;D2,D3:1]-!@[S:2](=[O:3])=[O:4]", "[#7:1]*.*[S:2](=[O:3])=[O:4]", "Sulphonamide");
        //TODO this is not part of the original RECAP, make it optional?
        /**
         * S1 = Cyclic amines -> an aliphatic or aromatic N in a ring, with a
         * degree of 3, and a neutral charge (index 1) connected via a non-ring
         * bond to any atom (index 2) reacts to the N connected to any atom and
         * the other atom connected to any atom note that no assumption is made
         * as to how the structure was synthesized
         * ";!#0" was inserted to avoid matching pseudo atoms that resulted from a previous cleavage
         */
        private final CleavageRule cyclicAmines = new CleavageRule("[#7;R;D3;+0:1]-!@[*;!#0:2]", "*[#7:1].[*:2]*", "Cyclic amines");
        //TODO this is not part of the original RECAP, make it optional?
        /**
         * S2 = Aromatic nitrogen - aromatic carbon -> aromatic N with a neutral
         * charge (index 1) connected via a non-ring bond(!) to an aromatic C
         * (index 2) reacts to the bond in between being split note that no
         * assumption is made as to how the structure was synthesized note also
         * that both atoms are in different rings
         */
        private final CleavageRule aromaticNitrogenToAromaticCarbon = new CleavageRule("[n;+0:1]-!@[c:2]", "[n:1]*.*[c:2]", "Aromatic nitrogen to aromatic carbon");
        /**
         * String array of SMIRKS reaction transform codes that describe the
         * cleavage rules.
         */
        private final CleavageRule[] cleavageRules = {this.amide, this.ester,
                this.amine, this.urea, this.ether, this.olefin, this.quaternaryNitrogen,
                this.aromaticNitrogenToAliphaticCarbon, this.lactamNitrogenToAliphaticCarbon,
                this.aromaticCarbonToAromaticCarbon, this.sulphonamide, this.cyclicAmines,
                this.aromaticNitrogenToAromaticCarbon};

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
