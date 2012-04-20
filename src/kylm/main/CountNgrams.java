/*
$Rev$

The Kyoto Language Modeling Toolkit.
Copyright (C) 2009 Kylm Development Team

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package kylm.main;

import java.io.*;
import kylm.model.ClassMap;
import kylm.model.ngram.NgramLM;
import kylm.model.ngram.smoother.*;
import kylm.model.ngram.writer.*;
import kylm.reader.*;
import kylm.util.KylmConfigUtils;
import kylm.util.SymbolSet;

/**
 * A program to calculate a smoothed N-gram language model
 * @author neubig
 *
 */
public class CountNgrams {

	public static void main(String args[]) throws Exception {

		final String br = System.getProperty("line.separator");
		KylmConfigUtils config = new KylmConfigUtils(
				"CountNgrams"+br+
				"A program to calculate an n-gram language model given a training corpus"+br+
		"Example: java -cp kylm.jar kylm.main.CountNgrams training.txt model.arpa");

		// Ngram Model Options
		config.addGroup("N-gram model options");
		config.addEntry("n", KylmConfigUtils.INT_TYPE, 3, false, "the length of the n-gram context");
		config.addEntry("trim", KylmConfigUtils.INT_ARRAY_TYPE, null, false, 
		"the trimming for each level of the n-gram (example: 0:1:1)");
		config.addEntry("name", KylmConfigUtils.STRING_TYPE, null, false, "the name of the model");
		config.addEntry("smoothuni", KylmConfigUtils.BOOLEAN_TYPE, false, false, "whether or not to smooth unigrams");

		// vocabulary options
		config.addGroup("Symbol/Vocabulary options");
		config.addEntry("vocab", KylmConfigUtils.STRING_TYPE, null, false, "the vocabulary file to use");
		config.addEntry("startsym", KylmConfigUtils.STRING_TYPE, "<s>", false, "the symbol to use for sentence starts");
		config.addEntry("termsym", KylmConfigUtils.STRING_TYPE, "</s>", false, "the terminal symbol for sentences");
		config.addEntry("vocabout", KylmConfigUtils.STRING_TYPE, null, false, "the vocabulary file to write out to");
		config.addEntry("ukcutoff", KylmConfigUtils.INT_TYPE, 0, false, "the cut-off for unknown words");
		config.addEntry("uksym", KylmConfigUtils.STRING_TYPE, "<unk>", false, "the symbol to use for unknown words");
		config.addEntry("ukexpand", KylmConfigUtils.BOOLEAN_TYPE, false, false, "expand unknown symbols in the vocabulary");
		config.addEntry("ukmodel", KylmConfigUtils.STRING_ARRAY_TYPE, null, false, "model unknown words. Arguments are processed first to last, so the most general model should be specified last. Format: \"symbol:vocabsize[:regex(.*)][:order(2)][:smoothing(wb)]\"");

		// class options
		config.addGroup("Class options");
		config.addEntry("classes", KylmConfigUtils.STRING_TYPE, null, false, "a file containing word class definitions");
		
		// Smoothing options
		config.addGroup("Smoothing options [default: kn]");
		config.addEntry("ml", KylmConfigUtils.BOOLEAN_TYPE, false, false, "maximum likelihood smoothing");
		config.addEntry("gt", KylmConfigUtils.BOOLEAN_TYPE, false, false, "Good-Turing smoothing (Katz Backoff)");
		config.addEntry("wb", KylmConfigUtils.BOOLEAN_TYPE, false, false, "Witten-Bell smoothing");
		config.addEntry("abs", KylmConfigUtils.BOOLEAN_TYPE, false, false, "absolute smoothing");
		config.addEntry("kn", KylmConfigUtils.BOOLEAN_TYPE, true, false, "Kneser-Ney smoothing (default)");
		config.addEntry("mkn", KylmConfigUtils.BOOLEAN_TYPE, false, false, "Modified Kneser-Ney smoothing (of Chen & Goodman)");

		// Output format options
		config.addGroup("Output options [default: arpa]");
		config.addEntry("bin", KylmConfigUtils.BOOLEAN_TYPE, false, false, "output in binary format");
		config.addEntry("wfst", KylmConfigUtils.BOOLEAN_TYPE, false, false, "output in weighted finite state transducer format (WFST)");
		config.addEntry("arpa", KylmConfigUtils.BOOLEAN_TYPE, true, false, "output in ARPA format");
		config.addEntry("neginf", KylmConfigUtils.FLOAT_TYPE, null, false, "the number to print for non-existent backoffs (default: null, example: -99)");
		
		// Debugging options
		config.addGroup("Miscellaneous options");
		config.addEntry("debug", KylmConfigUtils.INT_TYPE, 0, false, "the level of debugging information to print"); // the level of debugging output to write

		// parse the arguments
		args = config.parseArguments(args);
		int debug = config.getInt("debug");

		// check the validity of the arguments
		int n = config.getInt("n");
		if(args.length > 2 || 
				n == -1)
			config.exitOnUsage();

		// choose the smoother
		NgramSmoother smoother = null;
		if(config.getBoolean("ml"))
			smoother = new MLSmoother();
		else if(config.getBoolean("gt"))
			smoother = new GTSmoother();
		else if(config.getBoolean("wb"))
			smoother = new WBSmoother();
		else if(config.getBoolean("abs"))
			smoother = new AbsoluteSmoother();
		else if(config.getBoolean("mkn"))
			smoother = new MKNSmoother();
		else if(config.getBoolean("kn"))
			smoother = new KNSmoother();
		if(smoother == null) {
			System.err.println("A type of smoothing must be chosen (ml|gt|wb|abs|kn|mkn)");
			config.exitOnUsage(1);
		}
		smoother.setDebugLevel(debug);
		smoother.setSmoothUnigrams(config.getBoolean("smoothuni"));

		// pick the writer type
		NgramWriter writer = null;
		if(config.getBoolean("bin"))
			writer = new SerializedNgramWriter();
		else if(config.getBoolean("wfst"))
			writer = new WFSTNgramWriter();
		else if(config.getBoolean("arpa")) {
			writer = new ArpaNgramWriter();
			Object negInf = config.getValue("neginf");
			if(negInf != null)
				((ArpaNgramWriter)writer).setNegativeInfinity((Float)negInf);
		}
		else {
			System.err.println("A type of writer must be chosen (arpa|bin|wfst)");
			config.exitOnUsage(1);
		}

		// create the input sentence loader
		SentenceReader loader =
			(args.length > 0 ?
					new TextFileSentenceReader(args[0]) :
						new TextStreamSentenceReader(System.in));

		// create the n-gram model
		NgramLM lm = new NgramLM(n, smoother);
		lm.getSmoother().setCutoffs(config.getIntArray("trim"));
		lm.setDebug(debug);
		lm.setName(config.getString("name"));
		lm.setUnknownSymbol(config.getString("uksym"));
		lm.setVocabFrequency(config.getInt("ukcutoff"));
		lm.setStartSymbol(config.getString("startsym"));
		lm.setTerminalSymbol(config.getString("termsym"));

		// load the unknown models
		String[] ukStrings = config.getStringArray("ukmodel");
		if(ukStrings != null) {
			NgramLM[] ukModels = new NgramLM[ukStrings.length];
			for(int i = 0; i < ukStrings.length; i++) {
				ukModels[i] = getUnknownModel(ukStrings[i]);
			}
			lm.setUnknownModels(ukModels);
		}

		// import the vocab if it exists
		if(config.getString("vocab") != null) {
			lm.setVocab(SymbolSet.readFromFile(config.getString("vocab")));
			if(debug > 0)
				System.err.println("CountNgrams, loaded "+lm.getVocab().getSize()+" vocabulary");
		}
		else if(!loader.supportsReset()) {
			System.err.println("CountNgrams only supports piped input if the vocabulary is specified.");
			System.err.println("Either specify a vocabulary or load the input directly from a file.");
			System.exit(1);
		}
		
		// get the classes if they exist
		if(config.getString("classes") != null) {
			TextFileClassMapReader tfcml = new TextFileClassMapReader(config.getString("classes"));
			ClassMap cm = tfcml.readClassMap(lm.getVocab(), lm.getUnknownModelCount()+2, false);
			cm.getClasses().addAlias(lm.getTerminalSymbol(), lm.getId(lm.getStartSymbol()));
			lm.setClassMap(cm);
		}
		
		// train the model
		lm.trainModel(loader);

		if(config.getString("vocabout") != null)
			lm.getVocab().writeToFile(config.getString("vocabout"), false);
		
		if(config.getBoolean("ukexpand"))
			lm.expandUnknowns();

		if(debug > 0)
			System.err.println("CountNgrams, Started writing");
		long time = System.currentTimeMillis();

		// print the model
		BufferedOutputStream os = new BufferedOutputStream(
				(args.length>1?new FileOutputStream(args[1]):System.out), 16384
		);

		writer.write(lm, os);
		os.close();

		if(debug > 0)
			System.err.println("CountNgrams, done writing - "+(System.currentTimeMillis()-time)+" ms");

	}

	// A function to get unknown models
	private static NgramLM getUnknownModel(String str) {

		// get the strings
		String[] strs = str.split(":");
		if(strs.length < 2) {
			System.err.println("Must specify at least a symbol and a vocabulary size for unknown models (e.g. <unk>:5000)");
			System.exit(1);
		}
		// get the model size
		int modelSize = 0;
		try { modelSize = Integer.parseInt(strs[1]); }
		catch (NumberFormatException e ) {
			System.err.println("Illegal vocabulary size for "+strs[0]+": "+strs[1]+". Must be an integer.");
		}
		// get the regex
		String regStr = (strs.length > 2?strs[2]:null);
		// get the n-gram order
		int nOrder = 2;
		if(strs.length > 3) {
			try { nOrder = Integer.parseInt(strs[3]); }
			catch (NumberFormatException e ) {
				System.err.println("Illegal ngram-order size for "+strs[0]+": "+strs[3]+". Must be an integer.");
			}
		}
		// load the smoother
		NgramSmoother mySmoother = null;
		String smoothStr = (strs.length > 4?strs[4]:"wb");
		if(smoothStr.equals("ml"))       mySmoother = new MLSmoother();
		else if(smoothStr.equals("gt"))  mySmoother = new GTSmoother();
		else if(smoothStr.equals("wb"))  mySmoother = new WBSmoother();
		else if(smoothStr.equals("abs")) mySmoother = new AbsoluteSmoother();
		else if(smoothStr.equals("mkn"))  mySmoother = new MKNSmoother();
		else if(smoothStr.equals("kn"))  mySmoother = new KNSmoother();
		else {
			System.err.println("Illegal smoother type in unknown model \""+str+"\"");
			System.exit(1);
		}
		NgramLM ret = new NgramLM(nOrder,mySmoother);
		ret.setSymbol(strs[0]);
		ret.setVocabLimit(modelSize);
		if(regStr != null) ret.setRegex(regStr);
		return ret;
	}

}
