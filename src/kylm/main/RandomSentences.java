package kylm.main;

import java.io.IOException;
import java.util.Random;

import kylm.model.LanguageModel;
import kylm.model.ngram.NgramLM;
import kylm.model.ngram.NgramNode;
import kylm.model.ngram.reader.ArpaNgramReader;
import kylm.model.ngram.reader.NgramReader;
import kylm.model.ngram.reader.SerializedNgramReader;
import kylm.util.KylmConfigUtils;
import kylm.util.SymbolSet;

public class RandomSentences {

	private static Random rand = null;

	public static void main(String args[]) {

		final String br = System.getProperty("line.separator");
		KylmConfigUtils config = new KylmConfigUtils(
				"RandomSentences"+br+
				"A program to generate random sentences given an Ngram lanugage model"+br+
		"Example: java -cp kylm.jar kylm.main.RandomSentences -num 10 -arpa model.arpa");

		// Input format options
		config.addEntry("arpa", KylmConfigUtils.BOOLEAN_TYPE, true, true, "model in arpa format");
		config.addEntry("bin", KylmConfigUtils.BOOLEAN_TYPE, false, false, "model in binary format");

		// Output options
		config.addEntry("num", KylmConfigUtils.INT_TYPE, 10, false, "number of sentences to generate");

		// parse the arguments
		args = config.parseArguments(args);
		if(args.length != 1)
			config.exitOnUsage();

		// read in the model
		System.err.println("Reading model");
		NgramReader nr = (!config.getBoolean("bin")?new ArpaNgramReader():new SerializedNgramReader());
		NgramLM lm = null;
		try { lm = nr.read(args[0]); } catch(IOException e) {
			System.err.println("Problem reading model from file "+args[0]+": "+e.getMessage());
			System.exit(1);
		}

		unLog(lm.getRoot());
		if(lm.getUnknownModels() != null)
			for(LanguageModel clm : lm.getUnknownModels())
				unLog(((NgramLM)clm).getRoot());

		rand = new Random(System.currentTimeMillis());

		for(int i = 0; i < config.getInt("num"); i++) {
			printSentence(lm, " ", "\n");
		}

	}

	private static void printSentence(NgramLM lm, String divider, String endSymbol) {
		SymbolSet vocab = lm.getVocab();
		NgramNode node = lm.getRoot().getChild(0);
		boolean done = false;
		LanguageModel[] uklm = lm.getUnknownModels();
		while(!done) {
			// fall back if necessary
			if(!node.hasChildren()) {
				node = node.getFallback();
				continue;
			}
			float val = rand.nextFloat();
			for(NgramNode child : node) {
				val -= child.getScore();
				if(val <= 0) {
					if(child.getId() != 0) {
						System.out.print(vocab.getSymbol(child.getId()));
						if(uklm != null && child.getId() <= uklm.length)
							printSentence((NgramLM)uklm[child.getId()-1], "", "");
						System.out.print(divider);
						node = child;
					}
					else
						done = true;
					break;
				}
			}
			if(val > 0)
				node = node.getFallback();
		}
		System.out.print(endSymbol);
	}

	// remove the logs
	private static void unLog(NgramNode node) {
		node.setScore((float)Math.pow(10, node.getScore()));
		if(node.hasChildren()) {
			for(NgramNode child : node)
				unLog(child);
			node.setBackoffScore((float)Math.pow(10, node.getBackoffScore()));
		}
	}

}
