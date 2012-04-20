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

import kylm.model.LanguageModel;
import kylm.model.ngram.NgramLM;
import kylm.model.ngram.NgramNode;
import kylm.util.SymbolSet;

/**
 * A class to write language models to text files that can be imported
 * as Weighted Finite State Transducers
 * @author neubig
 *
 */
public class WFSTNgramWriter extends NgramWriter {
	
	private static String FINAL_STRING = "__FINAL_STATE__";
	private static String START_STRING = "__START_STATE__";
	
	String minusInfinity = "99";
	private static int byteSize = 4096;
	
	private PrintStream out;
	private SymbolSet states, vocab;
	private int termId = 0, n = 0;
	private String epsString = "<eps>", termString = null, br = null;
	private StringBuffer sb = null;
	private DecimalFormat df = new DecimalFormat("0.0000");
	private LanguageModel lm = null;
	
	@Override
	public void write(NgramLM lm, OutputStream os) throws IOException {
		
		// save the values to global
		this.lm = lm;
		out = new PrintStream(os);
		states = new SymbolSet();
		vocab = lm.getVocab();
		n = lm.getN();
		termString = lm.getTerminalSymbol();
		termId = vocab.getId(termString);
		br = System.getProperty("line.separator");
		sb = new StringBuffer();
		
		// print the output of the terminal symbol separately
		NgramNode brNode = lm.getRoot().getChild(termId);
		String brParent = (n > 2?termString+" ":"");
		if(!brNode.hasChildren()) {
			brNode = lm.getRoot();
			brParent = "";
		}
		for(int i = 0; i < vocab.getSize(); i++) {
			// skip the terminal ID
			if(i == termId)
				continue;
			float score = 0;
			NgramNode child = brNode.getChild(i);
			
			String myParent = brParent;
			if(child == null && brNode.getFallback() != null) {
				score += brNode.getBackoffScore();
				child = brNode.getFallback().getChild(i);
				myParent = "";
			}
			if(child != null) {
				String nextSym = vocab.getSymbol(i);
				sb.append(states.addSymbol(START_STRING)).append('\t').append(states.addSymbol(n==1?"":myParent+nextSym))
					.append('\t').append(nextSym).append('\t').append(nextSym).append('\t')
					.append(df.format(Math.abs(child.getScore()))).append(br);
			}
		}
		
		sb.append(states.addSymbol(FINAL_STRING)).append(br);
		
		// print recursively
		recursivePrint(lm.getRoot(), 1, "", "");
		
		// print the remainder
		out.print(sb.toString());
		
	}

	private void recursivePrint(NgramNode node, int i, String pLab, String pFb) {
		
		// skip no-info nodes
		if(!node.hasChildren())
			return;
		
		// get the parent state
		int pState = states.addSymbol(pLab);
		
		// print recursively
		for(NgramNode child : node) {
			String cLab = null, cFb = null, nextSym = (child.getId()==0?lm.getTerminalSymbol():vocab.getSymbol(child.getId()));
			// for the full history level, remove the last fallback value
			if(i == n) {
				cLab = (i == 1?"":(pFb.length()>0?pFb+" "+nextSym:nextSym));
			}
			// if we're at the top, set up appropriately
			else if(i == 1) {
				cLab = nextSym;
				cFb = "";
			}
			// if we're in the middle, we need both
			else {
				cLab = pLab+" "+nextSym;
				cFb  = (pFb.length()>0?pFb+" "+nextSym:nextSym);
			}
			String nLab = (child.getId() == termId?FINAL_STRING:cLab);
			
			// print the transition to the child
			int nState = states.addSymbol(nLab);
			sb.append(pState).append('\t').append(nState).append('\t').append(nextSym).append('\t')
				.append(nextSym).append('\t').append(df.format(Math.abs(child.getScore()))).append(br);
			// print the fallback if necessary
			if(cFb != null) {
				int fbState = states.addSymbol(cFb);
				int cState = states.addSymbol(cLab);
				sb.append(cState).append('\t').append(fbState).append('\t').append(epsString).append('\t')
					.append(epsString).append('\t').append(df.format(Math.abs(child.getBackoffScore()))).append(br);
				recursivePrint(child, i+1, cLab, cFb);
			}
		}
		if(sb.length() > byteSize) {
			out.print(sb.toString());
			sb = new StringBuffer();
		}
	}

}
