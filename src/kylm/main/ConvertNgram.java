package kylm.main;

import java.io.IOException;

import kylm.model.ngram.reader.*;
import kylm.model.ngram.writer.*;
import kylm.util.KylmConfigUtils;

/**
 * A program to convert n-gram models from one file format to another.
 * It can take ARPA or binary files as input, and can output ARPA, binary, or WFST files.
 * @author neubig
 *
 */
public class ConvertNgram {

	public static void main(String[] args) {
		
		final String br = System.getProperty("line.separator");
		KylmConfigUtils config = new KylmConfigUtils(
				"ConvertLM"+br+
				"A program to convert models from one format to another"+br+
		"Example: java -cp kylm.jar kylm.main.ConvertNgram -arpain model.arpa -wfstout model.wfst");

		// Input format options
		config.addGroup("Input format options");
		config.addEntry("arpain", KylmConfigUtils.BOOLEAN_TYPE, false, false, "input model is in arpa format");
		config.addEntry("binin", KylmConfigUtils.BOOLEAN_TYPE, false, false, "input model is in binary format");

		// Debugging options
		config.addGroup("Output format options");
		config.addEntry("arpaout", KylmConfigUtils.BOOLEAN_TYPE, false, false, "output model is in arpa format");
		config.addEntry("binout", KylmConfigUtils.BOOLEAN_TYPE, false, false, "output model is in binary format");
		config.addEntry("wfstout", KylmConfigUtils.BOOLEAN_TYPE, false, false, "output model is in wfst format");

		// parse the arguments
		args = config.parseArguments(args);
		if(args.length != 2)
			config.exitOnUsage();

		// get the reader
		NgramReader ngr = null;
		if(config.getBoolean("arpain"))
			ngr = new ArpaNgramReader();
		else if(config.getBoolean("binin"))
			ngr = new SerializedNgramReader();
		else {
			System.err.println("Must select an input format (-arpain/-binin)");
			System.exit(1);
		}
		
		// get the writer
		NgramWriter ngw = null;
		if(config.getBoolean("arpaout"))
			ngw = new ArpaNgramWriter();
		else if(config.getBoolean("binout"))
			ngw = new SerializedNgramWriter();
		else if(config.getBoolean("wfstout"))
			ngw = new WFSTNgramWriter();
		else {
			System.err.println("Must select an output format (-arpaout/-binout/-wfstout)");
			System.exit(1);
		}
		
		// convert
		try {
			ngw.write(ngr.read(args[0]), args[1]);
		} catch (IOException e) {
			System.err.println("Error while printing: "+e.getMessage());
			System.exit(1);
		}
			
	}

}
