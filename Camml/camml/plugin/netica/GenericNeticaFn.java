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

import camml.core.models.bNet.BNet;
import camml.core.models.cpt.CPT;
import camml.core.models.multinomial.MultinomialLearner;

/**
 * All the functions that work without the actual Netica library.
 * 
 * @author Steven
 *
 */
public class GenericNeticaFn
{

    /**
     *  if s is a valid netica name it is returned. Otherwise the name is mangled as appropriate<br>
     *  a "v_" is added to names not beginning with non-alphabetic characters. <br>
     *  All non alphanumeric characters are replaced with an underscore.
     *
     *  There may be more operations needed I am currently unaware of.  This should be further 
     *  investigated.
     */
    /*public*/ private static String makeValidNeticaName( String s ) 
    {
        // append "v_" to start it variable does not start with a letter
        if ( !Character.isLetter( s.charAt(0) ) ) {
            s = "v_" + s;
        }

        // If a non [a..z|A..Z|0..9|_] char is found, replace it with an underscore.
        for ( int i = 0; i < s.length(); i++ ) {
            char x = s.charAt(i);
            if ( !(Character.isDigit(x) || Character.isLetter(x) || x == '_') ) {
                s = s.replace( x, '_' );
            }
        }

        // Strings must have less than 30 chars.
        if ( s.length() > 30 ) { s = s.substring(0,30); }

        return s;
    }

    /**
     * Runs makeVelidNeticaName on each element of s, but adds the constraint that each resulting
     *  string must be different.  This is useful to ensure all variable or state names are unique.
     * If overwrite == false a new array is returned, if overwrite == true, the original array is
     *  returned (with modified names.)
     */
    public static String[] makeValidNeticaNames( String[] s, boolean overwrite ) 
    {
        // If overwriting s is not required, make a copy of s.
        if ( overwrite == false ) {
            s = (String[])s.clone();
        }

        for ( int i = 0; i < s.length; i++ ) {
            s[i] = makeValidNeticaName( s[i] );
        }

        // Try adding an _x to the end where x is the position in the original order.
        int numChanges = 0;
        for ( int i = 0; i < s.length; i++ ) {
            if ( s[i] == null ) { s[i] = "var_"+i;}
            for ( int j = i+1; j < s.length; j++ ) {
                if ( s[i].equals(s[j]) ) {
                    s[i] = s[i] + "_" + i;
                    s[j] = s[j] + "_" + j;
                    numChanges ++;
                }
            }
        }        
    
        // Do a quick check to make sure all elements are now different.
        boolean allDifferent = true;
        if ( numChanges > 0 ) {
            for ( int i = 0; i < s.length; i++ ) {
                for ( int j = i+1; j < s.length; j++ ) {
                    if ( s[i].equals(s[j]) ) {
                        allDifferent = false;
                    }
                }
            }                
        }

        // If all elements still not all different, give up and rename everything var_x
        if ( !allDifferent ) {
            for ( int i = 0; i < s.length; i++ ) {
                s[i] = "var_" + i;
            }
        }
    

        return s;
    }






    /** static instance of convertToNet. */
    public static ConvertToNeticaNet convertToNeticaNet = new ConvertToNeticaNet();

    /**
     * ConvertToNeticaNet takes a BNet and returns a BNetNetica
     * Useful for converting from BNetStochastic to BNetNetica.
     */
    public static class ConvertToNeticaNet extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -6203242109043154964L;
        
        static final Type.Function tt = new Type.Function( Type.MODEL, Type.MODEL );
        public ConvertToNeticaNet() { super(tt); }

        /** ConvertToNetica in Bayes Net. ([Str],(Model,params)) -> (Model,params) */
        public Value apply( Value v )
        {
            BNet oldNet = (BNet)v;
            Type.Structured dataType = (Type.Structured)((Type.Model)oldNet.t).dataSpace;
            return new BNetNetica( dataType );
        }
    }

    
    public static SaveNet saveNet = new SaveNet();
    /** 
     * Save network to a file.  Returns a string representation of the network.
     * (filename, model, params) -> String <br>*/
    public static class SaveNet extends Value.Function {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -7030134785705863399L;
        
        /** Static type (model,[]) -> String */
        static Type.Function tt =             
            new Type.Function(
                              new Type.Structured(new Type[] {Type.STRING,Type.MODEL,Type.VECTOR}),
                              Type.STRING
                              );
        
        /**     */
        public SaveNet() { super(tt); }

        /** Write netica net fo file and return a
         * Value.String representation of (bNet,params)*/
        public Value apply(Value v) {
            
            Value.Structured struct = (Value.Structured)v;
            String fName = ((Value.Str)struct.cmpnt(0)).getString();
            BNet bNet = (BNet)struct.cmpnt(1);
            Value.Vector params = (Value.Vector)struct.cmpnt(2);
            
            return _apply(fName, bNet, params);
        }

        /** Write netica net fo file and return a
         * Value.String representation of (bNet,params)*/
        public static Value
            _apply(String fName, BNet bNet, Value.Vector params) {
                        
            String netString = bNet.exportNetica("cammlNet",params);
            
            try {
                FileWriter out = new FileWriter(fName);
                out.write(netString);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            return new Value.Str( netString );
        }

    }
    
    
    /** BNetClassify return the classification probabilities for a
     *  given dataset where the BNet is used as a classifier. */
    public static class BNetClassify extends Value.Function {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -940885203937446080L;
        
        final static Type.Function tt =
            new Type.Function(new Type.Structured( new Type[] 
                {Type.MODEL,Type.VECTOR, Type.VECTOR} ), Type.VECTOR);
        
        public BNetClassify() {
            super(tt);
        }

        /** Take a parameterized BNet and a dataset. For each element in the
         *  dataset return the logP of each value given all other values. */
        Value.Vector
            _apply( BNet bNet, Value.Vector params, Value.Vector data) {
            // Ensure bn is a BNetNetica so efficient inference can be used.
            final BNetNetica bn;
            if ( bNet instanceof BNetNetica) { bn = (BNetNetica)bNet;}
            else { bn = new BNetNetica( bNet.getDataType() ); }
            
            int numVars = params.length();
            Value.Vector vecArray[] = new Value.Vector[numVars];
            for ( int i = 0; i < numVars; i++) {
                double[] logPArray = bn.probClassify( params, data, i);
                vecArray[i] = new VectorFN.FastContinuousVector(logPArray);
            }
            String[] names = bn.makeNameList(params);
            
            return new VectorFN.MultiCol( new Value.DefStructured(vecArray,names) );
        }
        
        public Value apply(Value v) {
            Value.Structured vStruct = (Value.Structured)v;
            BNet bNet = (BNet)vStruct.cmpnt(0);
            Value.Vector params = (Value.Vector)vStruct.cmpnt(1);
            Value.Vector data = (Value.Vector)vStruct.cmpnt(2);
            return _apply(bNet,params,data);
        }
        
    }

    public static BNetClassify bNetClassify = new BNetClassify();

    
    /** Classify returns the highest probability value for a given row. */
    public static class Classify extends Value.Function {
    
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -1229040283526770891L;
        
        final static Type.Function tt =
            new Type.Function(new Type.Structured( new Type[] 
                {Type.MODEL,Type.VECTOR, Type.VECTOR, Type.DISCRETE} ), Type.VECTOR);
        
        public Classify() {
            super(tt);
        }

        /** Classify values for a single BN variable. */
        public Value.Vector _apply( BNet bNet, Value.Vector params, Value.Vector data, int var) {
            // Ensure bn is a BNetNetica so efficient inference can be used.
            final BNetNetica bn;
            if ( bNet instanceof BNetNetica) { bn = (BNetNetica)bNet;}
            else { bn = new BNetNetica( bNet.getDataType() ); }
            
            return bn.classify(params, data, var);
        }
        
        public Value apply(Value v) {
            Value.Structured vStruct = (Value.Structured)v;
            BNet bNet = (BNet)vStruct.cmpnt(0);
            Value.Vector params = (Value.Vector)vStruct.cmpnt(1);
            Value.Vector data = (Value.Vector)vStruct.cmpnt(2);
            int var = ((Value.Discrete)vStruct.cmpnt(3)).getDiscrete();
            return _apply(bNet,params,data,var);
        }
        
    }

    public static Classify classify = new Classify();

    /** Returns probabilities for specified variable given other variables.. */
    public static class ClassifyProb extends Value.Function {
    
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -7641558140712305036L;
        
        final static Type.Function tt =
            new Type.Function(new Type.Structured( new Type[] 
                {Type.MODEL,Type.VECTOR, Type.VECTOR, Type.DISCRETE} ), Type.VECTOR);
        
        public ClassifyProb() {
            super(tt);
        }

        /** Classify values for a single BN variable. */
        public Value.Vector _apply( BNet bNet, Value.Vector params, Value.Vector data, int var) {

            // Ensure bn is a BNetNetica so efficient inference can be used.
            final BNetNetica bn;
            if ( bNet instanceof BNetNetica) { bn = (BNetNetica)bNet;}
            else { bn = new BNetNetica( bNet.getDataType() ); }

            double classProbs[][] = bn.getClassProbs( params, data, var);
            Value.Vector vecs[] = new Value.Vector[classProbs.length];
            for (int i = 0; i < vecs.length; i++) {
                vecs[i] = new VectorFN.FastContinuousVector( classProbs[i] );
            }
            
            return new VectorFN.MultiCol( new Value.DefStructured(vecs) );
        }
        
        public Value apply(Value v) {
            Value.Structured vStruct = (Value.Structured)v;
            BNet bNet = (BNet)vStruct.cmpnt(0);
            Value.Vector params = (Value.Vector)vStruct.cmpnt(1);
            Value.Vector data = (Value.Vector)vStruct.cmpnt(2);
            int var = ((Value.Discrete)vStruct.cmpnt(3)).getDiscrete();
            return _apply(bNet,params,data,var);
        }
        
    }

    public static ClassifyProb classifyProb = new ClassifyProb();
}
