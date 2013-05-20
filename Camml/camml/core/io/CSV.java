package camml.core.io;

import java.io.FileReader;
import camml.core.io.opencsv.*;
import cdms.core.*;

import java.util.*;

class ValueStr extends Value.Str implements Comparable<ValueStr> {
    /**
	 * 
	 */
	private static final long serialVersionUID = 2251887037484909295L;

	public ValueStr(String s) { super(s); }
    public ValueStr(ValueStatus status,String s) { super(status,s); }
    public ValueStr(Type.Str t, String s) { super(t,s); }
    public ValueStr(Type.Str t, ValueStatus status, String s) { super(t,status,s); }

	public int compareTo(ValueStr o) {
		return this.toString().compareTo(o.toString());
	}
}

public class CSV {
	public static int MAX_DISCRETE_STATES = 30;
	public static int MAX_STATES = 100;
	
	static public Value.Vector load(String path) throws Exception {
		FileReader fr = new FileReader(path);
		CSVReader reader = new CSVReader(fr);
		
		String[] headers = reader.readNext();
		String[] types = new String[headers.length];
		/// Assume ints (most restrictive), otherwise doubles, otherwise strings (least restrictive)
		for (int i=0; i<types.length;i++)  types[i] = "int";
		
		Vector<ValueStr>[] data = (Vector<ValueStr>[]) new Vector[ headers.length ];
		TreeMap<String,Integer>[] columnValues = (TreeMap<String,Integer>[])new TreeMap[headers.length];
		for (int c=0; c<headers.length; c++) {
			data[c] = new Vector<ValueStr>();
			columnValues[c] = new TreeMap<String,Integer>();
		}

		String[] row;
		while ((row = reader.readNext()) != null ) {
			for (int c=0; c<headers.length; c++) {
				ValueStr s = new ValueStr(row[c]);
				data[c].add(s);
				columnValues[c].put(row[c], 1);
				
				/// Test the type to see if we're dealing with doubles, ints or strings
				if (types[c].equals("int")) {
					try { Integer.parseInt(row[c]); }
					catch (NumberFormatException e) {
						/// Try as doubles instead
						types[c] = "double";
					}
				}
				if (types[c].equals("double")) {
					try { Double.parseDouble(row[c]); }
					catch (NumberFormatException e) {
						/// Give up and assume it's strings
						types[c] = "string";
					}
				}
				///
			}
		}
		
		Value.Vector[] vecArray = new Value.Vector[headers.length];
		
		for (int c=0; c<headers.length; c++) {
			/// Assume discrete
			/* SM: This doesn't work the way I want/thought.
			if (types[c].equals("int")) {
				int minVal = Integer.MAX_VALUE;
				int maxVal = Integer.MIN_VALUE;
				int[] intArray = new int[data[c].size()];
				for (int r=0; r<data[c].size(); r++) {
					ValueStr v = data[c].get(r);
					int val = Integer.parseInt(v.getString());
					intArray[r] = val;
					if (val < minVal)  minVal = val;
					if (val > maxVal)  maxVal = val;
				}
				System.out.println(c+":"+minVal+":"+maxVal);
				for (int r=0;r<intArray.length;r++) System.out.print(intArray[r]+",");
				System.out.println();
				/// Avoid creating overly large discrete nodes (either switch to continuous or nominal)
				/// FIX: (SM) Discrete doesn't work unless it starts from 0...
				if (maxVal-minVal > 30) {
					types[c] = "double";
				}
				else {
		            Type.Discrete discType = new Type.Discrete(minVal,maxVal,false,false,false,false);
	                vecArray[c] = new VectorFN.FastDiscreteVector( intArray, discType );
				}
			}*/
			/// Assume continuous
			if (types[c].equals("double")) {
				/// If there's only a "small" number of values, treat as nominal
				/// so that current CaMML can work with it
				/// (FIX: once discretisation available, possibly switch back to always continuous)
				if (columnValues[c].size() < CSV.MAX_DISCRETE_STATES) {
					types[c] = "string";
				}
				else {
					double[] doubleArray = new double[data[c].size()];
					for (int r=0; r<data[c].size(); r++) {
						ValueStr v = data[c].get(r);
						doubleArray[r] = Double.parseDouble(v.getString());
					}
	                vecArray[c] = new VectorFN.FastContinuousVector(doubleArray);
				}
			}
			/// Assume nominal
			if (types[c].equals("string") || types[c].equals("int")) {
				Set<String> keysPre = columnValues[c].keySet();
				String[] keys = new String[keysPre.size()];
				keysPre.toArray(keys);
				
				for (int i=0; i<keys.length; i++) {
					columnValues[c].put(keys[i], i);
				}
				try {
					keys = camml.plugin.netica.NeticaFn.makeValidNeticaNames(keys, true);
				}
				catch (Exception e) {
					throw new Exception("The data may contain missing values, which are not yet supported.");
				}
				
                Type.Symbolic type = new Type.Symbolic(false, false, false,
                        false, keys);
                
                int[] intArray = new int[data[c].size()];
                for (int j = 0; j < intArray.length; j++) {
                	ValueStr v = data[c].get(j);
                    intArray[j] = columnValues[c].get(v.getString());
                }
                
                // combine type and value together to form a vector of symbolic values.
                vecArray[c] = new VectorFN.FastDiscreteVector(intArray, type );
			}
		}
		
		Value.Structured vecStruct = new Value.DefStructured(vecArray,
                headers);
        Value.Vector vec = new VectorFN.MultiCol(vecStruct);

        fr.close();
        
        return vec;
	}
	
	//NYI
	public void save(String path, Object data){
		
	}
}
