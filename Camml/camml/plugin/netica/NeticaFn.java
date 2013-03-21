/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Rodney O'Donnell, Lloyd Allison, Kevin Korb
 *  Copyright (c) 2002-2011, Monash University
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products
 *       derived from this software without specific prior written permission.*
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

//
// Netica Plugin
//
// Various functions to allow interaction between CDMS and Netica
//

// File: NeticaFN.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.netica;

import java.io.FileWriter;
import java.io.IOException;

import cdms.core.*;

import norsys.netica.NeticaException;
import norsys.netica.Node;
import norsys.netica.NodeList;
import norsys.netica.Net;
import norsys.netica.Streamer;
import norsys.netica.Util;

import camml.core.models.bNet.BNet;
import camml.core.models.cpt.CPT;
import camml.core.models.multinomial.MultinomialLearner;

public class NeticaFn extends GenericNeticaFn
{

    /** static instance of loadNet. */
    public static LoadNet loadNet = new LoadNet();

    /** LoadNet loads in a netica network file.  "fileName.dnet" -> (BNetNetica,params) */
    public static class LoadNet extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 1900918945676015406L;
        static final Type.Function tt = new Type.Function( Type.STRING, Type.STRUCTURED );
        public LoadNet() { super(tt); }

        /** Load in Bayes Net. Str -> (Model,params) */
        public Value apply( Value v )
        {
            return _apply( ((Value.Str)v).getString() );
        }

        /** Load in Bayes Net. Str -> (Model,params) <br>
         *  Any zero-arity nodes are removed as they are usually title nodes. */
        public static Value.Structured _apply( String fileName )
        {
            synchronized (Netica.env) { try {
                    // initialise environment
                    //Environ env = Netica.env;

                    // read in network from input file.
                    System.out.println("filename = " + fileName);
                    Net net = new Net( new Streamer( fileName ) );

                    // Remove all nodes containing zero states.
                    // These are commonly used as "TITLE" nodes in netica examples.
                    NodeList nodeList = net.getNodes();
                    for (int i = 0; i < nodeList.size(); i++) {
                        Node node = nodeList.getNode(i);            
                        // If node contains zero states, remove it from the network.
                        if (node.getNumStates() == 0) {
                            node.delete();
                        }
                    }
        
                    nodeList = net.getNodes();
                    int numVars = nodeList.size();

                    // store type of each node.
                    Type.Symbolic[] typeArray = new Type.Symbolic[numVars];

                    // store parameters of each node
                    Value.Structured[] bNetParamArray = 
                        new Value.Structured[numVars];

                    // store list of all names.
                    String[] nameArray = new String[numVars];

                    // for each node, create CPT & Type
                    for ( int i = 0; i < numVars; i++ ) {
                        Node node = nodeList.getNode(i);
                        NodeList parents = node.getParents();
                        int arity = node.getNumStates();
                        Value.Str name = new Value.Str( node.getName() );
                        nameArray[i] = node.getName();

                        // create the appropriate type for this variable.
                        String[] stateName = new String[arity];
                        for ( int j = 0; j < stateName.length; j++ ) {
                            stateName[j] = node.state(j).getName();
                        }
                        typeArray[i] = new Type.Symbolic(false,false,false,false,stateName);


                        // set up to find CPT
                        int[] parentArity = new int[parents.size()];
                        int[] parentLWB = new int[parents.size()];
                        int[] parentUPB = new int[parents.size()];
                        int[] parentState = new int[parents.size()];
                        // store index into nodeList of parents
                        int[] parentIndex = new int[parents.size()]; 
                        int parentCombinations = 1;

                        for ( int j = 0; j < parentState.length; j++ ) {            
                            // number of states variable may take.
                            parentArity[j] = parents.getNode(j).getNumStates();

                            // set lower and upper bounds to {0,arity-1}
                            parentLWB[j] = 0; 
                            parentUPB[j] = parentArity[j] - 1;
                            parentCombinations *= parentArity[j];

                            // initialise the current parent state to {0,0,...0}
                            parentState[j] = 0;

                            Node parentJ = parents.getNode(j);
                            for ( int k = 0; k < numVars; k++ ) {
                                if ( nodeList.getNode(k) == parentJ ) {
                                    parentIndex[j] = k;
                                    break;
                                }
                                // this should never happen.
                                if ( k == numVars - 1 ) {
                                    throw new RuntimeException("Parent not found : " + parentJ);
                                }
                            }
                        }


                        // create CPT model
                        //Multinomial multinomialModel = new Multinomial(0,arity-1);
                        Value.Model multinomialModel = 
                            MultinomialLearner.getMultinomialModel(0,arity-1);
                        CPT cpt = new CPT( multinomialModel, parentLWB, parentUPB );
                        Value.Vector parentVec =
                            new VectorFN.FastDiscreteVector( parentIndex );

            

                        // create CPT parameters.
                        double[][] paramArray = new double[parentCombinations][];
                        for ( int j = 0; j < parentCombinations; j++ ) {
                            paramArray[j] = Util.toDoubles( node.getCPTable( parentState, null ) );
                            // BNet.incrementBitfield( parentState, parentArity );
            
                            // ordering in CPTs is backwards...
                            BNet.reverseIncrementBitfield( parentState, parentArity );
                        }
            
                        // create params for CPT
                        Value.Vector cptParams = cpt.makeCPTParams( paramArray );

                        Value.Structured modelParamStruct = 
                            new Value.DefStructured( new Value[] {cpt,cptParams} );

                        bNetParamArray[i] =
                            new Value.DefStructured(new Value[]
                                { name,
                                  parentVec, 
                                  modelParamStruct } );
                    }
        
                    Value.Vector paramVec = new VectorFN.FatVector( bNetParamArray );
                    BNet bNet = new BNetNetica( new Type.Structured(typeArray,nameArray) );



                    //        env.finalize();
        
                    return new Value.DefStructured( new Value[] {bNet,paramVec} );

                } catch( NeticaException e ) {
                    throw new RuntimeException(e);
                } }

        }

    }


    /** static instance of reorderNet. */
    public static ReorderNet reorderNet = new ReorderNet();

    /**
     * Netica has a bad habit of putting variables in a different order than you would like.
     * ReorderNet takes a list of names and a (model,params) struct and returns a (model,params)
     * struct with variables in the correct order.
     */
    public static class ReorderNet extends Value.Function
    {
        
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 2238869347640096380L;

        static final Type.Structured myType = 
            new Type.Structured( new Type[] {Type.MODEL, Type.VECTOR});
    
        static final Type.Function tt = 
            new Type.Function(new Type.Structured(new Type[] 
                {new Type.Vector(Type.STRING),myType}), myType );
    
        public ReorderNet() { super(tt); }

        /** Reorder nodes in network to order in newOrderNames.    myStruct = (model,params) */
        public static Value.Structured
            _apply( String[] newOrderNames, Value.Structured myStruct )
        {
            synchronized ( Netica.env ) {
                // deconstruct v into relevent components.
                Value.Model model = (Value.Model)myStruct.cmpnt(0);
                Value.Vector params = (Value.Vector)myStruct.cmpnt(1);
                Type.Structured dataType = (Type.Structured)((Type.Model)model.t).dataSpace;


                // extract names from old and new ordering.
                String[] oldOrderNames = new String[ params.length() ];
                for ( int i = 0; i < newOrderNames.length; i++ ) {
                    oldOrderNames[i] =
                        ((Value.Str)((Value.Structured)params.elt(i)).cmpnt(0)).getString();
                }
        

                // Find the mapping from old to new names.
                int[] newOrder = new int[ params.length() ];
                int[] oldOrder = new int[ params.length() ];
                boolean[] used = new boolean[ newOrder.length ];
                for ( int i = 0; i < newOrderNames.length; i++ ) {
                    boolean found = false;
                    for ( int j = 0; j < oldOrderNames.length; j++ ) {            
                        if ( newOrderNames[i].equals( oldOrderNames[j] ) ) {
                            if ( used[i] == true ) {
                                throw new RuntimeException("Attemtpting to use name twice.");
                            }
                            used[i] = true;
                            found = true;
                            newOrder[i] = j;
                            oldOrder[j] = i;
                            //System.out.println("NewOrder["+i+"] -> OldOrder["+j+"]");
                        }
                    }
                    if ( found == false ) {
                        throw new RuntimeException("Can't find match for " + newOrderNames[i]);
                    }
                }



                // Allocate space to store new parameter and type info
                Value.Structured[] paramArray = 
                    new Value.Structured[ params.length() ];
                Type[] typeArray = new Type[ params.length() ];


                // loop through and rearrange type and param arrays.
                for ( int i = 0; i < paramArray.length; i++ ) {
                    typeArray[i] = dataType.cmpnts[ newOrder[i] ];
                    paramArray[i] = (Value.Structured)params.elt(newOrder[i]);

                    // we also hava to reparent paramArray to reflect changes.
                    Value.Vector parents = (Value.Vector)paramArray[i].cmpnt(1);
                    int[] newParent = new int[parents.length()];
                    for ( int j = 0; j < parents.length(); j++ ) {
                        newParent[j] = oldOrder[ parents.intAt(j) ];
                    }

                    Value.Vector newParents = new VectorFN.FastDiscreteVector(newParent);
                    paramArray[i] = new Value.DefStructured( new Value[]
                        { paramArray[i].cmpnt(0),
                          newParents,
                          paramArray[i].cmpnt(2) } );
        
                }

                Value.Vector newParamVec = new VectorFN.FatVector( paramArray );
                BNet bNet = new BNetNetica( new Type.Structured(typeArray,newOrderNames) );

        
                return new Value.DefStructured(new Value[] 
                    { bNet, newParamVec } );
            }
        }
    
        /** Reorder in Bayes Net. ([Str],(Model,params)) -> (Model,params) */
        public Value apply( Value v )
        {
            // deconstruct v into relevent components.
            Value.Structured vStruct = (Value.Structured)v;
            Value.Vector stringVec = (Value.Vector)vStruct.cmpnt(0);
            // (model,param) struct
            Value.Structured myStruct = (Value.Structured)vStruct.cmpnt(1);
            Value.Vector params = (Value.Vector)myStruct.cmpnt(1);

            String[] name = new String[ params.length() ];
            for ( int i = 0; i < name.length; i++ ) {
                name[i] = ((Value.Str)stringVec.elt(i)).getString();
            }

            return _apply( name, myStruct );
        }
    }
}
