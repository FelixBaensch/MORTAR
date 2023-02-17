/*
 * Copyright (C) 2023 Julian Zander, Jonas Schaub, Achim Zielesny, Christoph Steinbeck
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, version 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package de.unijena.cheminf.scaffoldGenerator;

/**
 * IMPORTANT NOTE: This is a copy of
 * https://github.com/Julian-Z98/ScaffoldGenerator/blob/main/ScaffoldGenerator/src/main/java/de/unijena/cheminf/scaffolds/ScaffoldNetwork.java
 * Therefore, do not make any changes here but in the original repository!
 * Last copied on October 27th 2022
 */

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Top-level class to organise the NetworkNodes
 * A network can have several roots and leaves.
 *
 * @author Julian Zander, Jonas Schaub (zanderjulian@gmx.de, jonas.schaub@uni-jena.de)
 * @version 1.0.2.1
 */
public class ScaffoldNetwork extends ScaffoldNodeCollectionBase {

    //<editor-fold desc="Constructors">
    /**
     * Constructor
     * @param aSmilesGenerator Used SMILES Generator
     */
    public ScaffoldNetwork(SmilesGenerator aSmilesGenerator) {
        super(aSmilesGenerator);
    }

    /**
     * Default Constructor
     */
    public ScaffoldNetwork() {
        super();
    }
    //</editor-fold>

    //<editor-fold desc="Public methods">
    @Override
    public void addNode(ScaffoldNodeBase aNode) throws CDKException, IllegalArgumentException, NullPointerException {
        /*Parameter checks*/
        Objects.requireNonNull(aNode, "Given NetworkNode is 'null'");
        if(!(aNode instanceof NetworkNode)) {
            throw new IllegalArgumentException("Node can not be added to ScaffoldNetwork. Parameter must be a NetworkNode.");
        }
        //Add to nodeMap
        this.nodeMap.put(this.nodeCounter, aNode);
        //Add to reverseNodeMap
        this.reverseNodeMap.put(aNode, this.nodeCounter);
        /*Add to smilesMap*/
        IAtomContainer tmpMolecule = (IAtomContainer) aNode.getMolecule();
        String tmpSmiles = this.smilesGenerator.create(tmpMolecule); //Convert molecule to SMILES
        this.smilesMap.put(tmpSmiles, aNode);
        int tmpLevel = aNode.getLevel();
        if(this.levelMap.get(tmpLevel) == null) {
            this.levelMap.put(tmpLevel, new HashSet<>());
        }
        this.levelMap.get(tmpLevel).add(aNode);
        HashMap<Integer, HashSet<ScaffoldNodeBase>> tmpLevelMap = new HashMap<>();
        for(ScaffoldNodeBase tmpNodeBase : this.getAllNodes()) {
            NetworkNode tmpNetworkNode = (NetworkNode) tmpNodeBase;
            int tmpLevelInternal = tmpNetworkNode.getLevel();
            if(tmpLevelMap.get(tmpLevelInternal) == null) {
                tmpLevelMap.put(tmpLevelInternal, new HashSet<>());
            }
            tmpLevelMap.get(tmpLevelInternal).add(tmpNetworkNode);
        }
        this.levelMap = tmpLevelMap;
        //Increase nodeCounter
        this.nodeCounter++;
    }

    @Override
    public void removeNode(ScaffoldNodeBase aNode) throws CDKException, IllegalArgumentException, NullPointerException {
        /*Parameter checks*/
        Objects.requireNonNull(aNode, "Given ScaffoldNode is 'null'");
        if(!(aNode instanceof NetworkNode)) {
            throw new IllegalArgumentException("Node can not be removed from ScaffoldNetwork. Parameter must be a NetworkNode.");
        }
        if(!this.reverseNodeMap.containsKey(aNode)) { //Check if the node exists in the Scaffold
            throw new IllegalArgumentException("Node is not in Scaffold");
        }
        /*Remove from nodeMap and reverseNodeMap*/
        int tmpNumberInNodeMap = this.reverseNodeMap.get(aNode); //get number in nodeMap
        this.nodeMap.remove(tmpNumberInNodeMap);
        this.reverseNodeMap.remove(aNode);
        /*Remove from smilesMap*/
        String tmpSmiles = this.smilesGenerator.create((IAtomContainer) aNode.getMolecule()); //Convert molecule to SMILES
        this.smilesMap.remove(tmpSmiles, aNode);
        /*Remove from levelMap*/
        levelMap.remove(aNode.getLevel(), aNode);
    }

    /**
     * Adds another ScaffoldNetwork to the existing one.
     * The new network is inserted at the node that both networks have in common.
     * All children of the new network at this node and there linkages are taken over if they do not already exist.
     *
     * The new network is simply taken over if the existing network is empty.
     * If there is no match between the two networks the new network is inserted without linkages.
     *
     * If a molecule generates an empty scaffold, it is stored as a node with empty SMILES and is treated normally.
     * All other empty nodes are then added to this network accordingly.
     * By querying the origins of this node, all molecules that do not produce a scaffold can be returned.
     * @param aScaffoldNetwork network to be inserted into the existing ScaffoldNetwork.
     * @throws CDKException In case of a problem with the SmilesGenerator
     */
    public void mergeNetwork(ScaffoldNetwork aScaffoldNetwork) throws CDKException {
        /*If the old ScaffoldNetwork is empty, transfer the new ScaffoldNetwork to be added.*/
        if(this.getAllNodes().size() == 0  || this.getAllNodes().isEmpty()) {
            for(Object tmpNode : aScaffoldNetwork.getAllNodes()) {
                this.addNode((NetworkNode) tmpNode);
            }
        }
        /*If the old Scaffold network is not empty*/
        else {
            ArrayList<IAtomContainer> tmpMoleculeList = new ArrayList(aScaffoldNetwork.getAllNodes().size());
            for(Object tmpNewNetworkObject : aScaffoldNetwork.getAllNodes()) {
                NetworkNode tmpNewNetworkNode = (NetworkNode) tmpNewNetworkObject;
                /*Node is not in network*/
                if(!this.containsMolecule((IAtomContainer) tmpNewNetworkNode.getMolecule())) {
                    /*Add node to lists so that it is added to the network later*/
                    NetworkNode tmpNewNode = new NetworkNode<>((IAtomContainer) tmpNewNetworkNode.getMolecule());
                    /*Add the nonVirtual SMILES to the OldSmilesNetwork fragment*/
                    for(Object tmpNonVirtualOriginSmiles : tmpNewNetworkNode.getNonVirtualOriginSmilesList()) {
                        tmpNewNode.addNonVirtualOriginSmiles((String) tmpNonVirtualOriginSmiles);
                    }
                    /*Add the nonVirtual SMILES to the OldSmilesNetwork fragment*/
                    for(Object tmpOriginSmiles : tmpNewNetworkNode.getOriginSmilesList()) {
                        tmpNewNode.addOriginSmiles((String) tmpOriginSmiles);
                    }
                    this.addNode(tmpNewNode);
                    tmpMoleculeList.add((IAtomContainer) tmpNewNetworkNode.getMolecule());
                } else { /*Node is already in the network*/
                    NetworkNode tmpOldNetworkNode = (NetworkNode) this.getNode((IAtomContainer) tmpNewNetworkNode.getMolecule());
                    /*Add the origin smiles to the OldSmilesNetwork fragment*/
                    for(Object tmpOriginSmiles : tmpNewNetworkNode.getOriginSmilesList()) {
                        tmpOldNetworkNode.addOriginSmiles((String) tmpOriginSmiles);
                    }
                    /*Add the nonVirtual SMILES to the OldSmilesNetwork fragment*/
                    if(tmpNewNetworkNode.hasNonVirtualOriginSmiles()) {
                        for(Object tmpNonVirtualOriginSmiles : tmpNewNetworkNode.getNonVirtualOriginSmilesList()) {
                            tmpOldNetworkNode.addNonVirtualOriginSmiles((String) tmpNonVirtualOriginSmiles);
                        }
                    }
                }
            }
            /*Add the matching parents to the newly added nodes. Children are automatically set when the parents are set.*/
            for(IAtomContainer tmpMolecule : tmpMoleculeList) {
                ScaffoldNodeBase tmpChildBase = aScaffoldNetwork.getNode(tmpMolecule);
                NetworkNode tmpChild = (NetworkNode) tmpChildBase;
                ArrayList<NetworkNode> tmpParentList = (ArrayList<NetworkNode>) tmpChild.getParents();
                for(NetworkNode tmpParentNode : tmpParentList) {
                    /*Only molecules that are in the network*/
                    if(this.containsMolecule((IAtomContainer) tmpParentNode.getMolecule())) {
                        NetworkNode tmpOldParentNode = (NetworkNode) this.getNode((IAtomContainer) tmpParentNode.getMolecule());
                        NetworkNode tmpOldChild = (NetworkNode) this.getNode(tmpMolecule);
                        tmpOldChild.addParent(tmpOldParentNode);
                    }
                }
            }
        }
    }

    //<editor-fold desc="get/set">
    @Override
    public Integer[][] getMatrix() throws IllegalStateException {
        int tmpSize = this.nodeMap.size();
        Integer[][] tmpMatrix = new Integer[tmpSize][tmpSize];
        /*Set all values of the matrix to 0*/
        for (int tmpRow = 0; tmpRow < tmpMatrix.length; tmpRow++) {
            Arrays.fill(tmpMatrix[tmpRow], 0);
        }
        /*Insert a 1 for each parent node*/
        int tmpCounter = 0;
        for (ScaffoldNodeBase tmpNodeBase : this.nodeMap.values()) {
            NetworkNode tmpNode = (NetworkNode) tmpNodeBase;
            if (tmpNode.getParents() != null) {
                for(Object tmpParentNode : tmpNode.getParents()) {
                    /*Check if a node has been removed*/
                    if(this.reverseNodeMap.get(tmpParentNode) != null){
                        //Set a 1 at the level of the parent and at the level of the node
                        tmpMatrix[tmpCounter][this.getMatrixNodesNumbers().indexOf(this.reverseNodeMap.get(tmpParentNode))] = 1;
                        //Set a 1 at the level of the node and at the level of the parent
                        tmpMatrix[this.getMatrixNodesNumbers().indexOf(this.reverseNodeMap.get(tmpParentNode))][tmpCounter] = 1;
                    }
                }
            }
            tmpCounter++;
        }
        return tmpMatrix;
    }

    /**
     * Outputs root nodes of the network.
     * @return root nodes of the network
     */
    public List<NetworkNode> getRoots() {
        List<NetworkNode> tmpNodeList = new ArrayList<>();
        for(ScaffoldNodeBase tmpNodeBase : this.nodeMap.values()) {
            NetworkNode tmpNode = (NetworkNode) tmpNodeBase;
            /*Is the parent of the node in the network*/
            if(tmpNode.getLevel() == 0) {
                //If the node has no parent, it is a root
                tmpNodeList.add(tmpNode);
            }
        }
        return tmpNodeList;
    }
    //</editor-fold>
    //</editor-fold>
}
