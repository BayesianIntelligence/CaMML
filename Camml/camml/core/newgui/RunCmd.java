package camml.core.newgui;

import java.util.*;

import com.beust.jcommander.*;
import java.io.*;

/** Add extra information when usage printed, such as accepted formats for input <data-file>s.**/
class CmdOptions {
	@Parameter(description = "<data-file> [<output-file>]")
	public List<String> files = new ArrayList<String>();
	
	@Parameter(names = {"-a", "--all-bns"}, description = "Output all representative networks retained. Without this, only one representative network is saved.")
	public boolean allBNs = false;
	
	@Parameter(names = {"-p", "--priors"}, description = "File containing the expert priors.")
	public String priorsFn = null;
	
	@Parameter(names = {"-P", "--priors-string"}, description = "Expert priors as a string.")
	public String priorsString = null;
	
	@Parameter(names = {"-q", "--quiet"}, description = "Only output the BN and errors; don't output status information.")
	public boolean quiet = false;

	@Parameter(names = {"-s", "--speed"}, description = "Search speed. Lower is faster (and less extensive).")
	public double speed = 1.0;

	@Parameter(names = {"--max-secs"}, description = "Maximum number of top SECs to retain. The more SECs retained, the slower the process to merge them to obtain a final BN.")
	public int maxSECs = 30;

	@Parameter(names = "--dbn", description = "Learn a DBN network from the data (assumes data is in time series format with Row 1 = t, Row 2 = t+1, etc.)")
	public boolean dbn = false;
	
	@Parameter(names = "--make-priors", description = "Save a default priors file template to the file name given.")
	public String makePriors = null;
}

public class RunCmd {
	public static void normalOutputStream() {
		PrintStream psOut = new PrintStream(new FileOutputStream(FileDescriptor.out));
		System.setOut(psOut);
	}
	
    public static void quietOutputStream(){
    	//Anonymous inner class for redirecting output stream to the specified text area:
    	OutputStream quietOut = new OutputStream(){
    		@Override
    	    public void write(int b) throws IOException {
    	    }
    	 
    	    @Override
    	    public void write(byte[] b, int off, int len) throws IOException {
    	    }
    	 
    	    @Override
    	    public void write(byte[] b) throws IOException {
    	    }
    	};
    	
    	PrintStream PSOut = new PrintStream( quietOut, true );
    	System.setOut( PSOut );
    }
    
    public static void makePriorsFile(String path) throws Exception {
		File newPriorsFile = new File(path);
		FileWriter fw = new FileWriter(newPriorsFile);
		
		fw.write(GUIParameters.defaultNewExpertPriorString);
		
		fw.close();
    }

    public static void main(String[] args) throws Exception {
    	System.out.println("yo!");
    	//quietOutputStream();
		CmdOptions opts = new CmdOptions();
		JCommander j = new JCommander(opts);
		j.setProgramName("camml");
		try {
			final GUIModel model = new GUIModel();
			normalOutputStream();
			
			//args = new String[]{"example-data/AsiaCases.1000.csv","testout.dne"};
			j.parse(args);
			System.out.println(args);
			
			if (opts.makePriors != null) {
				makePriorsFile(opts.makePriors);
				return;
			}
			
			if (opts.quiet) {
				quietOutputStream();
			}
			else {
				normalOutputStream();
			}
			
			if (opts.files.size() == 0) {
				j.usage();
				return;
			}
			String inFile = opts.files.get(0);
			
			model.selectedFile = new File(inFile);
			model.loadDataFile(inFile);
			model.MMLLearner = GUIModel.MMLLearners[0];
			
			if (opts.priorsFn != null) {
				Scanner tmps = new Scanner(new File(opts.priorsFn));
				opts.priorsString = tmps.useDelimiter("\\Z").next();
				tmps.close();
			}
			
			if (opts.priorsString != null) {
				model.useExpertPriors = true;
				model.expertPriorsString = opts.priorsString;
				System.out.println("Using priors: "+model.expertPriorsString);
			}
			
			model.searchFactor = opts.speed;
			model.maxSECs = opts.maxSECs;
			
	    	if( !model.dataValid() )  throw new Exception("Invalid data");
	    	
	    	//Check MMLLearner:
	    	if( !model.MMLLearnerValid() )  throw new Exception("Invalid MML learner");
	    	
	    	//Check Search factor value:
	    	if( !model.searchFactorValid() )  throw new Exception("Invalid search factor");
	    	
	    	//Check max SECs value:
	    	if( !model.maxSECsValid() )  throw new Exception("Invalid max SECs");
	    	
	    	//Check max SECs value:
	    	if( !model.minTotalPosteriorValid() )  throw new Exception("Invalid minimum total posterior");
	    	
	    	//Check expert priors value:
	    	if( !model.expertPriorsValid() ) {
		    	try {
		    		model.validateExpertPriors(model.expertPriorsString);
		    	}
		    	catch (Exception e) {
		    		throw new Exception("Invalid expert priors: "+e);
		    	}
	    	}
	    	
			model.runSearch(opts.dbn);
			
			if (opts.files.size() >= 2) {
				String outFile = opts.files.get(1);
				if (opts.allBNs) {
					System.out.println("\n\nSaving all BNs using name '"+outFile+"' as template.");
					model.outputFullResultsAllBNs(outFile);
				}
				else {
					System.out.println("\n\nSaving to '"+outFile+"'");
					model.exportFullResultsBN(outFile, 0, false);
				}
			}
			else {
				System.out.println("\n\nDisplaying Representative Network");
				System.out.println(    "---------------------------------");
				
				/// If not output file specified, print to console
				normalOutputStream();
				System.out.println(model.generateNetworkStringFullResults(0));
			}
	  	}
		catch (ParameterException e) {
			System.out.println(e.getMessage());
			j.usage();
		}
	}
}
