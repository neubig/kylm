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

package kylm.model.ngram.reader;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kylm.model.ClassMap;
import kylm.model.LanguageModel;
import kylm.model.ngram.BranchNode;
import kylm.model.ngram.NgramLM;
import kylm.model.ngram.NgramNode;
import kylm.model.ngram.smoother.NgramSmoother;
import kylm.reader.TextStreamClassMapReader;
import kylm.util.SymbolSet;

/**
 * A class to read n-gram language models from ARPA files
 * @author neubig
 *
 */
public class ArpaNgramReader extends NgramReader {

	private String startOfData = "\\data\\";
	private String endOfData = "\\end\\";

	@Override
	public NgramLM read(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		return read(br);
	}

	public NgramLM read(BufferedReader br) throws IOException {

		NgramLM lm = new NgramLM(0);

		String s;
		// get parameters
		// TODO: Do this in a more efficient way?
		LanguageModel[] ukModels = null;
		while((s = br.readLine()) != null && !s.equals(startOfData)) {
			if(s.length() == 0) { }
			else if(s.equals("[name]")) lm.setName(br.readLine());
			else if(s.equals("[symbol]")) lm.setSymbol(br.readLine());
			else if(s.equals("[pattern]")) lm.setRegex(br.readLine());
			else if(s.equals("[n]")) lm.setN(Integer.parseInt(br.readLine()));
			else if(s.equals("[smoother]")) {
				try {
					lm.setSmoother((NgramSmoother)Class.forName(br.readLine()).newInstance());
				} catch (Exception e) { throw new IOException(e); }
			}
			else if(s.equals("[smooth_unigrams]")) {
				lm.getSmoother().setSmoothUnigrams(Boolean.parseBoolean(br.readLine()));
			}
			else if(s.equals("[count_terminals]")) lm.setCountTerminals(Boolean.parseBoolean(br.readLine()));
			else if(s.equals("[closed]")) lm.setClosed(Boolean.parseBoolean(br.readLine()));
			else if(s.equals("[max_length]")) lm.setMaxLength(Integer.parseInt(br.readLine()));
			else if(s.equals("[vocab_cutoff]")) lm.setVocabFrequency(Integer.parseInt(br.readLine()));
			else if(s.equals("[vocab_size_limit]")) lm.setVocabLimit(Integer.parseInt(br.readLine()));
			else if(s.equals("[start_symbol]")) lm.setStartSymbol(br.readLine());
			else if(s.equals("[terminal_symbol]")) lm.setTerminalSymbol(br.readLine());
			else if(s.equals("[unknown_symbol]")) lm.setUnknownSymbol(br.readLine());
			else if(s.equals("[unknown_model_count]"))
				ukModels = new LanguageModel[Integer.parseInt(br.readLine())];
			else if(s.equals("[classmap]"))
				lm.setClassMap(readClassMap(br, lm));
		}
		int n = lm.getN();

		// if the data has reached the end there's a problem
		if(s == null)
			throw new IOException("EOF found before reading data");



		// read the n-gram counts
		// TODO: more robust error checking
		/*
		 *  X. Yao. 2010-07-01: make the pattern matching work for irstlm
		 *  which provides not that standard headers.
		 */
		Pattern pat = Pattern.compile("ngram\\s*(\\d+)=\\s*(\\d+)");
		Matcher m = null;
		Vector<Integer> counts = new Vector<Integer>();
		while((s = br.readLine()) != null) {
			m = pat.matcher(s);
			if(!m.matches())
				break;
			counts.add(Integer.parseInt(m.group(2)));
		}
		// check consistency
		if(n == 0) {
			lm.setN(counts.size());
			n = lm.getN();
		}
		else if (lm.getN() != counts.size())
			throw new IllegalArgumentException("Header n ("+lm.getN()+
					") doesn't match number of counts ("+counts.size()+")");
		// convert to a normal array and pass to the lm
		int[] cs = new int[n];
		for(int i = 0; i < n; i++)
			cs[i] = counts.get(i);
		lm.setNgramCounts(cs);

		// actual data
		int size, level = 0;
		int addType = NgramNode.ADD_BRANCH;
		StringTokenizer st = null;
		String token = null;
		float score = 0;
		// initialize the root node
		SymbolSet vocab = null;
		if(lm.getClassMap() == null) {
			lm.getRoot().setChildrenSize(counts.get(0));
			vocab = lm.getVocab();
		} else {
			vocab = lm.getClassMap().getClasses();
			lm.getRoot().setChildrenSize(vocab.getSize());
		}

		pat = Pattern.compile("\\\\(\\d+)-grams:");
		BranchNode root = lm.getRoot();
		root.setBackoffScore( Float.NEGATIVE_INFINITY );
		for( ; s != null; s = br.readLine()) {
			// skip blank lines
			if(s.length() == 0) continue;
			st = new StringTokenizer(s);
			// System.err.println(s);
			size = st.countTokens();
			// String[] sent = KylmTextUtils.whiteSpace.split(s);
			// look for one-word lines
			if(size == 1) {
				token = st.nextToken();
				m = pat.matcher(token);
				if(m.matches()) {
					level++;
					if(level != Integer.parseInt(m.group(1)))
						throw new IOException("Level number "+m.group(1)+" out of order (should be "+level);
					addType = (level==n?NgramNode.ADD_LEAF:NgramNode.ADD_BRANCH);
				}
				else if(token.equals(endOfData))
					break;
				else
					throw new IOException("Bad line in data section: "+s);
			}
			// if the section is too short
			else if(size <= level)
				throw new IOException("Bad line in data section: "+s);
			// add nodes
			else {
				// get the score
				score = Float.parseFloat(st.nextToken());
				NgramNode node = root;
				// deal with unigrams
				String uniToken = null;
				if(level == 1) {
					uniToken = st.nextToken();
					node = node.getChild(vocab.addSymbol(uniToken), addType);
				}
				else
					for(int i = 0; i < level; i++)
						node = node.getChild(vocab.getId(st.nextToken()), addType);
				if(level != 1 || !uniToken.equals(lm.getStartSymbol()))
					node.setScore(score);
				if(st.hasMoreTokens()) {
					s = st.nextToken();
					try {
						if(level != 1 || !uniToken.equals(lm.getTerminalSymbol()))
							node.setBackoffScore( Float.parseFloat(s) );
					} catch (NumberFormatException e) {
						if(s.equals("-∞")) node.setBackoffScore(Float.NEGATIVE_INFINITY);
						else if(s.equals("∞")) node.setBackoffScore(Float.POSITIVE_INFINITY);
						else {
							while(node.getParent() != null) {
								System.err.println(vocab.getSymbol(node.getId()));
								node = node.getParent();
							}
							throw e;
						}
					}
				}
			}
		}

		if(ukModels != null) {
			for(int i = 0; i < ukModels.length; i++)
				ukModels[i] = read(br);
			lm.setUnknownModels(ukModels);
		}

		return lm;

	}

	private ClassMap readClassMap(BufferedReader br, LanguageModel lm) throws NumberFormatException, IOException {
		TextStreamClassMapReader tcr = new TextStreamClassMapReader(br);
		ClassMap cm = tcr.readClassMap(lm.getVocab(), 0, true);
		cm.getClasses().addAlias(lm.getTerminalSymbol(), lm.getId(lm.getStartSymbol()));
		return cm;
	}

}
