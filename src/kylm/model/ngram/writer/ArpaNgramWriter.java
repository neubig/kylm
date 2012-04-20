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

package kylm.model.ngram.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;

import kylm.model.ClassMap;
import kylm.model.LanguageModel;
import kylm.model.ngram.NgramLM;
import kylm.model.ngram.NgramNode;
import kylm.util.SymbolSet;
import kylm.writer.TextStreamClassMapWriter;

/**
 * A class to write language models to arpa files
 * @author neubig
 *
 */
public class ArpaNgramWriter extends NgramWriter {

	private Float negativeInfinity = null;

	private String startOfData = "\\data\\";
	private String endOfData = "\\end\\";

	private DecimalFormat df = new DecimalFormat("0.0000");
	private static int byteSize = 4096;
	private StringBuffer sb = null;
	private PrintStream out = null;
	private String br = null;
	private SymbolSet vocab = null;
	private NgramLM lm = null;

	private void printOne(String name, String value, String def) {
		if(value != null && !value.equals(def))
			sb.append('[').append(name).append(']').append(br).append(value).append(br).append(br);
	}

	@Override
	public void write(NgramLM lm, OutputStream os) throws IOException {

		out = new PrintStream(os);
		sb = new StringBuffer();
		br = System.getProperty("line.separator");
		this.lm = lm;

		// identity variables
		printOne("name", lm.getName(), null);
		printOne("symbol", lm.getSymbol(), null);
		printOne("pattern", (lm.getRegex() == null?null:lm.getRegex().toString()), null);

		// n-gram variables
		int n = lm.getN();
		printOne("n", n+"", null);
		printOne("smoother", (lm.getSmoother()==null?null:lm.getSmoother().getClass().getName()), null);
		printOne("smooth_unigrams", ""+(lm.getSmoother()==null?false:lm.getSmoother().getSmoothUnigrams()), "false");

		// setting variables
		printOne("closed", lm.isClosed()+"", "false");
		printOne("count_terminals", lm.getCountTerminals()+"", "true");
		printOne("max_length", lm.getMaxLength()+"", "0");

		// vocab variables
		printOne("vocab_cutoff", lm.getVocabFrequency()+"", null);
		printOne("vocab_size_limit", lm.getVocabLimit()+"", "0");
		printOne("start_symbol", lm.getStartSymbol()+"", null);
		printOne("terminal_symbol", lm.getTerminalSymbol()+"", null);

		// other language models
		LanguageModel[] ukModels = lm.getUnknownModels();
		if(ukModels == null)
			printOne("unknown_symbol", lm.getUnknownSymbol()+"", null);
		else
			printOne("unknown_model_count", ukModels.length+"", null);

		ClassMap cm = lm.getClassMap();
		if(cm != null) {
			sb.append("[classmap]").append(br);
			out.print(sb.toString());
			sb = new StringBuffer();
			TextStreamClassMapWriter tscmw = new TextStreamClassMapWriter(os, lm.getVocab());
			tscmw.writeClassMap(cm);
		}
		
		// start the data and print the counts
		int[] counts = lm.getNgramCounts();
		sb.append(startOfData).append(br);
		for(int i = 0; i < counts.length; i++) {
			int myCount = counts[i];
			sb.append("ngram ").append(i+1).append("=").append(myCount).append(br);
		}

		//		// add the root node to the stack
		//		LinkedList<KylmPair<String, NgramNode>> list = new LinkedList<KylmPair<String, NgramNode>>();
		//		list.add(new KylmPair<String, NgramNode>(null, lm.getRoot()));

		// print all levels of the nodes
		vocab = lm.getVocab();
		for(int i = 1; i <= n; i++) {
			sb.append("\\").append(i).append("-grams: ").append(br);
			printLevel(lm.getRoot(), null, i, i != n);
			sb.append(br);
		}

		sb.append(endOfData);
		out.println(sb.toString());

		// print the unknown models if they exist
		if(ukModels != null) {
			for(LanguageModel sub : ukModels) {
				out.println();
				write((NgramLM)sub, os);
			}
		}

		out.flush();

	}

	private void printLevel(NgramNode node, String string, int i, boolean printBackoff) {
		ClassMap classMap = lm.getClassMap();
		if(i == 1) {
			for(NgramNode child : node) {
				if(child.getId() == 0 && string==null && !lm.getTerminalSymbol().equals(lm.getStartSymbol())) {
					sb.append(df.format((negativeInfinity==null?-99.0:negativeInfinity))+"\t"+lm.getStartSymbol());
					if(child.hasChildren()) sb.append("\t"+df.format(child.getBackoffScore()));
					sb.append(br+df.format(child.getScore())+"\t"+lm.getTerminalSymbol());
					if(negativeInfinity != null && printBackoff)
						sb.append('\t').append(df.format(negativeInfinity));
					sb.append(br);
				}
				else {
					sb.append(df.format(child.getScore())).append('\t');
					if(string != null) sb.append(string).append(" ");
					String sym;
					if(child.getId()==0) sym = lm.getTerminalSymbol();
					else if(classMap != null) sym = classMap.getClassSymbol(child.getId());
					else sym = vocab.getSymbol(child.getId());
					sb.append(sym);
					// Print the appropriate value for negative infinity
					if(child.getBackoffScore() == Float.NEGATIVE_INFINITY) {
						if(negativeInfinity != null)
							sb.append('\t').append(df.format(negativeInfinity));
					}
					// Print the appropriate value for a child backoff score
					else if (child.getBackoffScore() == child.getBackoffScore())
						sb.append('\t').append(df.format(child.getBackoffScore()));
					sb.append(br);
					if(sb.length()>byteSize) {
						out.print(sb.toString());
						sb.setLength(0);
					}
				}
			}
		} else {
			for(NgramNode child : node) {
				if(child.hasChildren()) {
					String nextString;
					if(classMap != null) nextString = classMap.getClassSymbol(child.getId());
					else nextString = vocab.getSymbol(child.getId());
					if(string != null)
						nextString = string+" "+nextString;
					printLevel(child, nextString, i-1, printBackoff);
				}
			}
		}
	}

	public Float getNegativeInfinity() { return negativeInfinity; }
	public void setNegativeInfinity(Float negInf) { this.negativeInfinity = negInf; }

}
