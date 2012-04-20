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
import java.util.Vector;

import kylm.model.LanguageModel;
import kylm.model.ngram.reader.ArpaNgramReader;
import kylm.model.ngram.reader.SerializedNgramReader;
import kylm.reader.*;
import kylm.util.*;

/**
 * A program to calculate cross entropy and perplexity of one or more models
 * @author neubig
 *
 */
public class CrossEntropy {

	public static String makeEnt(float all, float simp, float cls, float unk, String unkSym) {
		StringBuffer sb = new StringBuffer();
		sb.append(all);
		if(simp != all) {
			sb.append("(s=");
			sb.append(simp);
			if(cls != 0)
				sb.append(",c=").append(cls);
			if(unk != 0) {
				sb.append(",u");
				if(unkSym != null)
					sb.append('[').append(unkSym).append(']');
				sb.append('=').append(unk);
			}
			sb.append(')');
		}
		return sb.toString();
	}

	public static void main(String args[]) throws Exception {

		final String br = System.getProperty("line.separator");
		KylmConfigUtils config = new KylmConfigUtils(
				"CrossEntropy"+br+
				"A program to find the cross-entropy of one or more language models over a test set"+br+
		"Example: java -cp kylm.jar kylm.main.CrossEntropy -arpa model1.arpa:model2.arpa test.txt");

		// Input format options
		config.addEntry("arpa", KylmConfigUtils.STRING_ARRAY_TYPE, null, false, "models in arpa format (model1.arpa:model2.arpa)");
		config.addEntry("bin", KylmConfigUtils.STRING_ARRAY_TYPE, null, false, "models in binary format (model3.bin:model4.bin)");

		// Debugging options
		config.addEntry("debug", KylmConfigUtils.INT_TYPE, 0, false, "the level of debugging information to print");

		// parse the arguments
		args = config.parseArguments(args);
		int debug = config.getInt("debug");

		// a vector to hold the models
		Vector<LanguageModel> models = new Vector<LanguageModel>();

		// load the arpa files
		String[] arpaFiles = config.getStringArray("arpa");
		if(arpaFiles != null) {
			ArpaNgramReader anr = new ArpaNgramReader();
			for(String arpa : arpaFiles) {
				LanguageModel next = anr.read(arpa);
				if(next.getName() == null) next.setName(arpa);
				models.add(next);
			}
		}

		// load the binary files
		String[] binFiles = config.getStringArray("bin");
		if(binFiles != null) {
			SerializedNgramReader snr = new SerializedNgramReader();
			for(String bin : binFiles) {
				LanguageModel next = snr.read(bin);
				if(next.getName() == null) next.setName(bin);
				models.add(next);
			}
		}

		// check to make sure at least one language model has been loaded
		if(models.size() == 0) {
			System.err.println("At least one language model must be specified."+br);
			config.exitOnUsage(1);
		}

		// get the input stream to load the input
		InputStream is = (args.length == 0?System.in:new FileInputStream(args[0]));
		TextStreamSentenceReader tssl = new TextStreamSentenceReader(is);

		// calculate the entropies
		float[] words = new float[models.size()], simples = new float[models.size()],
							unknowns = new float[models.size()], classes = new float[models.size()];
		float[] wordSents = new float[words.length], simpleSents = new float[words.length],
							unkSents = new float[words.length], classSents = new float[words.length];
		float[][] wordEnts = new float[words.length][], simpleEnts = new float[words.length][],
							unkEnts = new float[words.length][], classEnts = new float[words.length][];
		String[][] unkSyms = new String[words.length][];
		int wordCount = 0, sentenceCount = 0;
		for(String[] sent : tssl) {
			wordCount += sent.length;
			sentenceCount++;
			// calculate
			for(int i = 0; i < words.length; i++) {
				LanguageModel mod = models.get(i);
				wordEnts[i] = mod.getWordEntropies(sent); 
				words[i] += (wordSents[i] = KylmMathUtils.sum(wordEnts[i]));
				simpleEnts[i] = mod.getSimpleEntropies(); 
				simples[i] += (simpleSents[i] = KylmMathUtils.sum(simpleEnts[i]));
				classEnts[i] = mod.getClassEntropies(); 
				classes[i] += (classSents[i] = KylmMathUtils.sum(classEnts[i]));
				unkEnts[i] = mod.getUnknownEntropies(); 
				unknowns[i] +=  (unkSents[i] = KylmMathUtils.sum(unkEnts[i]));
				unkSyms[i] = new String[unkEnts[i].length];
				SymbolSet vocab = models.get(i).getVocab();
				for(int j = 0; j < unkEnts[i].length; j++)
					if(unkEnts[i][j] != 0)
						unkSyms[i][j] = vocab.getSymbol(models.get(i).findUnknownId(sent[j]));
			}

			if(debug > 0) {
				System.out.println(KylmTextUtils.join(" ", sent));
				for(int i = 0; i < wordSents.length; i++)
					System.out.println(models.get(i).getName()+": "+
							makeEnt(wordSents[i], simpleSents[i], classSents[i], unkSents[i], null));
				if(debug > 1) {
					for(int j = 0; j < wordEnts[0].length; j++) {
						System.out.print(" "+(j<sent.length?sent[j]:models.get(0).getTerminalSymbol())+"\tent: ");
						for(int i = 0; i < wordEnts.length; i++) {
							if(i != 0) System.out.print(", ");
							System.out.print(makeEnt(wordEnts[i][j], simpleEnts[i][j], classEnts[i][j], unkEnts[i][j], unkSyms[i][j]));
						}
						System.out.println();
					}
				}
				System.out.println();
			}
		}

		// change from log10
		final float log2 = (float)Math.log10(2);
		for(int i = 0; i < words.length; i++) {
			System.out.println("Found entropy over "+wordCount+" words, "+sentenceCount+" sentences");
			words[i] /= wordCount*log2*-1;
			simples[i] /= wordCount*log2*-1;
			unknowns[i] /= wordCount*log2*-1;
			classes[i] /= wordCount*log2*-1;
			System.out.print(models.get(i).getName()+": entropy="+
					makeEnt(words[i], simples[i], classes[i], unknowns[i], null));
			System.out.println(", perplexity="+Math.pow(2, words[i]));
			System.out.println(models.get(i).printReport());
		}

	}

}
