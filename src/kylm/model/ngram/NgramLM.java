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

package kylm.model.ngram;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Vector;

import kylm.model.LanguageModel;
import kylm.model.ngram.smoother.NgramSmoother;
import kylm.reader.TextArraySentenceReader;
import kylm.util.KylmTextUtils;

/**
 * A class that implements a normal n-gram model
 * @author neubig
 *
 */
public class NgramLM extends LanguageModel implements Serializable {

	/**
	 * ID for serialization
	 */
	private static final long serialVersionUID = 8531298547172592816L;

	// the size of the n-gram context
	protected int n = 0;

	// the number of counts for the ngram
	protected int[] counts = null;

	// the root of the ngram node tree
	protected BranchNode root = null;

	// the smoother to use
	protected NgramSmoother smoother = null;

	// keep track of what hits have been made
	protected int hits[] = null;
	protected int sentHits = 0;

	// unknown words to store for training
	protected HashSet<String> ukWords = null;

	/**
	 * A constructor that creates a model of size n
	 * @param n the length of the context of the n-gram model
	 */
	public NgramLM(int n) {
		this.n = n;
		if(n != -1) {
			counts = new int[n];
			hits = new int[n+1];
		}
		root = new BranchNode(-1, null);
		sentHits = 0;
	}

	public NgramLM(int n, NgramSmoother smoother) {
		this.n = n;
		if(n != -1) {
			counts = new int[n];
			hits = new int[n+1];
		}
		root = new BranchNode(-1, null);
		sentHits = 0;
		this.smoother = smoother;

	}

	@Override
	public float[] getWordEntropies(int[] iids) {
		// check to make sure that nodes exist for every id
		for(int i = 0; i < iids.length; i++)
			if(root.getChild(iids[i]) == null)
				iids[i] = this.findUnknownId(vocab.getSymbol(iids[i]));
		wordEnts = new float[iids.length-1];
		simpleEnts = new float[iids.length-1];
		classEnts = new float[iids.length-1];
		// convert to classes
		int[] mids;
		if(classMap != null) {
			mids = new int[iids.length];
			mids[0] = classMap.getWordClass(iids[0]);
			for(int i = 0; i < wordEnts.length; i++) {
				int idx = iids[i+1];
				classEnts[i] = classMap.getWordProb(idx);
				wordEnts[i] += classEnts[i];
				mids[i+1] = classMap.getWordClass(idx);
			}
		} else
			mids = iids;
		int idx;
		sentHits++;
		// start with the terminal symbol as the context
		NgramNode context = root.getChild(0), child;
		int lev = 2;
		for(int i = 0; i < wordEnts.length; i++) {
			idx = mids[i+1];
			// first, fall back to a node that has children
			while(!context.hasChildren()) {
				lev--;
				context = context.getFallback();
			}
			// then, fall back to a node that actually can predict the word
			while((child = context.getChild(idx)) == null) {
				// add the fallback penalty
				lev--;
				simpleEnts[i] += context.getBackoffScore();
				context = context.getFallback();
				if(context == null)
					throw new IllegalArgumentException("Could not find word in unigram vocabulary.");
			}
			// add the level that we got a hit at
			// System.out.println(isInVocab(idx)?lev:0);
			hits[isInVocab(idx)?lev:0]++;
			lev++;

			// add the score
			simpleEnts[i] += child.score;
			wordEnts[i] += simpleEnts[i];
			context = child;
		}
		return wordEnts;
	}

	@Override
	public float getWordEntropy(int[] ids, int pos) {
		float ret = 0;
		int i, context, idx;
		NgramNode node = root, child;
		for(context = Math.max(0, pos-n+1); context <= pos; context++) {
			// go down the tree trying to find the ngram
			for(i = context; i <= pos; i++) {
				idx = (classMap==null?ids[i]:classMap.getWordClass(ids[i]));
				child = node.getChild(idx);
				if(child == null)
					break;
				node = child;
			}
			// if found, return the score
			if(i == pos+1)
				return ret+node.score;
			// if not found, but only one behind in context
			else if(i == pos) {
				ret += node.getBackoffScore();
				node = node.getFallback();
			}
			// if not found and several behind in context
			else
				node = root;
		}
		throw new IllegalArgumentException("could not find n-gram");
	}

	/**
	 * Get the perplexity of a sentence
	 * @param sent The string of words
	 * @return PP(sent)
	 */
	public float getSentencePerplexity(String[] sent) {

		return getSentenceProb(sent)/-(sent.length+2);
	}

	/**
	 * Get the log10-likelihood of a sentence normalized with length.
	 * This value in fact equals to the perplexity of a sentence
	 * @param sent
	 * @return PP(sent)
	 */
	public float getSentenceProbNormalized(String[] sent) {
		return getSentencePerplexity(sent);
	}

	/**
	 * Get the log10-likelihood of a sentence
	 * @param sent The string of words
	 * @return log10(P(sent))
	 */
	public float getSentenceProb(String[] sent) {
		float prob = 0.0f;
		// get the sentence IDs
		int[] iids = getSentenceIds(sent);

		// check to make sure that nodes exist for every id
		for(int i = 0; i < iids.length; i++)
			if(root.getChild(iids[i]) == null)
				iids[i] = this.findUnknownId(vocab.getSymbol(iids[i]));

		// convert to classes
//		int[] mids;
//		if(classMap != null) {
//			mids = new int[iids.length];
//			mids[0] = classMap.getWordClass(iids[0]);
//			for(int i = 0; i < wordEnts.length; i++) {
//				int idx = iids[i+1];
//				classEnts[i] = classMap.getWordProb(idx);
//				wordEnts[i] += classEnts[i];
//				mids[i+1] = classMap.getWordClass(idx);
//			}
//		} else
//			mids = iids;
		int idx;

		// start with the terminal symbol as the context
		NgramNode context = root.getChild(0), child;

		for(int i = 0; i < iids.length-1; i++) {
			idx = iids[i+1];
			// first, fall back to a node that has children
			while(!context.hasChildren()) {
				context = context.getFallback();
			}
			// then, fall back to a node that actually can predict the word
			while((child = context.getChild(idx)) == null) {
				// add the fallback penalty
				prob += context.getBackoffScore();
				context = context.getFallback();
				if(context == null)
					throw new IllegalArgumentException("Could not find word in unigram vocabulary.");
			}
			// add the score
			prob += child.score;
			context = child;
		}

		return prob;
	}

	@Override
	public void trainModel(Iterable<String[]> sl) throws IOException {
		if(debug > 0)
			System.err.println("NgramLM.trainModel(): Started for "+name);
		// count the n-grams
		countNgrams(sl);
		// smooth the n-grams
		if(smoother != null)
			smoother.smooth(this);
		// check to see if the model is closed
		if(!closed) {
			closed = true;
			for(int i = 2; i <= ukModelCount+1; i++)
				if(root.getChild(i) != null)
					closed = false;
		}
		// train the unknown models
		if(ukModels != null && ukWords != null) {
			Vector< LinkedList<String> > ukw = new Vector<LinkedList<String>>(ukModelCount);
			for(int i = 0; i < ukModelCount; i++)
				ukw.add(new LinkedList<String>());
			for(String s : ukWords)
				ukw.get(findUnknownId(s)-2).add(KylmTextUtils.join(" ", KylmTextUtils.splitChars(s)));
			for(int i = 0; i < ukModels.length; i++)
				ukModels[i].trainModel(new TextArraySentenceReader(ukw.get(i).toArray(new String[0])));
		}
		if(debug > 0)
			System.err.println("NgramLM.trainModel(): Finished for "+name);
	}

	/**
	 * Count the ngrams in the corpus
	 * @param sl An iterator of sentences in the corpus
	 */
	public void countNgrams(Iterable<String[]> sl) throws IOException {
		if(debug > 0)
			System.err.println("NgramLM.countNgrams(): Started for "+name);
		// import the vocabulary if necessary
		if(vocab == null)
			importVocabulary(sl);
		root.setChildrenSize(classMap==null?vocab.getSize():classMap.getClassSize());
		int[] vocabCounts = (classMap == null?null:new int[vocab.getSize()]);
		int[] classCounts = (classMap == null?null:new int[classMap.getClassSize()]);
		// set up the variables
		int[] buff = new int[maxLength];
		// buff[0] is always == 0;
		NgramNode node;
		int len, start;
		// count the unknown words for later ukModel training
		if(ukModels != null)
			ukWords = new HashSet<String>();

		int count = 0;
		// cycle through every sentence
		for(String[] sent : sl) {
			// output progress
			if(debug > 0 && ++count % 10000 == 0)
				System.err.print(count % 1000000==0?count:".");
			// skip empty sentences
			if(sent.length == 0)
				continue;
			// modify the buffer size if necessary
			if(sent.length+2 > maxLength) {
				maxLength = sent.length+2;
				buff = new int[maxLength];
			}
			// add the actual sentence symbols
			for(len = 1; len <= sent.length; len++) {
				buff[len] = getId(sent[len-1]);
				// add unknown words to process later
				if(ukModels != null && (modelAllWords || !isInVocab(sent[len-1])))
					ukWords.add(sent[len-1]);
				// if a class based model, get the class
				if(classMap != null) {
					vocabCounts[buff[len]]++;
					int myClass = classMap.getWordClass(buff[len]);
					classCounts[myClass]++;
					buff[len] = classMap.getWordClass(buff[len]);
				}
			}
			// add a terminal symbol at the start and end if necessary
			start = (buff[1] == 0?1:0);
			if(buff[sent.length] != 0)
				buff[len++] = 0;
			// add to the total word count
			root.count += len-start-1;
			// cycle through all, adding the n-grams one-by-one
			int i,j,k;
			for(i = start; i < len-1; i++) {
				node = root;
				for(j = 0; j < n && (k=i+j) < len; j++) {
					node = node.getChild(buff[k], (j==n-1?NgramNode.ADD_LEAF:NgramNode.ADD_BRANCH));
					if(node.count == 0)
						counts[j]++;
					node.count++;
				}
			}
		}
		// re-adjust for the number of nodes
		if(root.getChild(1)==null && !terminalSymbol.equals(startSymbol))
			counts[0]++;
		// get the class probabilities
		if(classMap != null) {
			for(int i = 0; i < vocabCounts.length; i++) {
				if(vocabCounts[i] > 0) {
					classMap.setWordProb(i, (float)Math.log10(vocabCounts[i]/(double)classCounts[classMap.getWordClass(i)]));
				}
			}
		}
		if(debug > 0) {
			System.err.println();
			System.err.println("NgramLM.countNgrams(): Finished for "+name);
		}
	}

	/**
	 * Get the root node of the n-gram Tree
	 * @return The root node of the n-gram tree
	 */
	public BranchNode getRoot() {
		return root;
	}

	/**
	 * Get the length of the n-gram context
	 * @return The length
	 */
	public int getN() {
		return n;
	}

	/**
	 * Set the length of the n-gram context
	 * @param n The length
	 */
	public void setN(int n) {
		this.n = n;
		counts = new int[n];
		hits = new int[n+1];
		sentHits = 0;
	}

	/**
	 * Expand unknown words in the vocabulary explicitly (useful for WFSTs)
	 * TODO: This only works for unigrams
	 * TODO: This assigns a uniform probability, doesn't take unknown word models into account
	 */
	public void expandUnknowns() {
		int unkNext = 0;
		Vector<Integer> vec = new Vector<Integer>();
		vec.add(vocab.getId(ukSymbol));
		for(NgramNode node : root) {
			while(unkNext < node.id)
				vec.add(unkNext++);
			unkNext++;
		}
		float score = (float) (root.getChild(vec.get(0)).getScore()+Math.log10(vec.size())*-1);
		if(debug > 0)
			System.err.println("Expanding "+vec.size()+" unknown words");
		int add = (n == 1?NgramNode.ADD_LEAF:NgramNode.ADD_BRANCH);
		for (int i : vec) {
			NgramNode child = root.getChild(i, add);
			child.setScore(score);
		}
	}

	///////////////////////////////
	// methods for serialization //
	///////////////////////////////
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(n);
		out.writeObject(counts);
		out.writeObject(smoother);
		out.writeInt(vocab.getSize());
		writeNgrams(out, root, 0);
	}
	private void writeNgrams(ObjectOutputStream out, NgramNode node, int lev) throws IOException {
		out.writeInt(node.getId());
		out.writeFloat(node.getScore());
		// skip ones that are already on the top level
		if(lev == n)
			return;
		// skip ones with no children
		if(!node.hasChildren()) {
			out.writeInt(0);
			return;
		}
		out.writeInt(node.getChildCount());
		out.writeFloat(node.getBackoffScore());
		for(NgramNode child : node)
			writeNgrams(out, child, lev+1);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		// the size of the n-gram context
		setN(in.readInt());
		counts = (int[]) in.readObject();
		smoother = (NgramSmoother) in.readObject();
		int vocabSize = in.readInt();
		root = new BranchNode(-1, null);
		root.setChildrenSize(classMap==null?vocabSize:classMap.getClassSize());
		root.setId(in.readInt());
		readNgrams(in, root, 0);
	}

	private void readNgrams(ObjectInputStream in, NgramNode node, int lev) throws IOException {
		node.setScore(in.readFloat());
		if(lev == n) return;
		int childCount = in.readInt();
		// skip ones with no children
		if(childCount!=0) {
			node.setBackoffScore(in.readFloat());
			for(int i = 0; i < childCount; i++) {
				NgramNode child = node.getChild(in.readInt(), (lev+1==n?NgramNode.ADD_LEAF:NgramNode.ADD_BRANCH));
				readNgrams(in, child, lev+1);
			}
		}
	}

	////////////////////
	// equal function //
	////////////////////
	private static boolean eq(Object o1, Object o2) {
		if(o1 == null) return o2 == null;
		return o1.equals(o2);
	}
	public boolean equals(Object obj) {
		try {
			NgramLM mod = (NgramLM)obj;
			return
			super.equals(mod) &&
			n == mod.n &&
			eq(root, mod.root) &&
			eq(smoother.getClass(), mod.smoother.getClass()) &&
			Arrays.equals(counts, mod.counts);
		} catch(Exception e) { }
		return false;
	}

	/**
	 * Get the number of n-grams at each level
	 * @return An array containing the number of n-gram counts at each level
	 */
	public int[] getNgramCounts() {
		return counts;
	}

	public NgramSmoother getSmoother() {
		return smoother;
	}

	public void setSmoother(NgramSmoother smoother) {
		this.smoother = smoother;
	}

	public void setNgramCounts(int[] cs) {
		counts = cs;
	}

	public String getNodeName(NgramNode child) {
		if(child.getParent() == null)
			return "";
		String parentString = getNodeName(child.getParent());
		String myString = vocab.getSymbol(child.getId());
		return (parentString.length()==0 ? myString : parentString+" "+myString);
	}

	@Override
	public String printReport() {
		StringBuffer sb = new StringBuffer();
		sb.append(name).append(" coverage: ");
		int[] newHits = new int[n+1], tries = new int[n+1];
		// hits of a greater order are also hits of a lesser order
		newHits[n] = hits[n];
		for(int i = n-1; i >= 0; i--)
			newHits[i] = hits[i] + newHits[i+1];
		// unigram and bigram tries are equal, but 3-gram up decrease
		tries[0] = newHits[0];
		for(int i = 1; i < tries.length; i++)
			tries[i] = tries[i-1] - (i>2?sentHits:0);
		for(int i = 1; i <= n; i++)
			sb.append(i).append("-gram ").append(newHits[i]*100.0/tries[i]).append("% ");
		return sb.toString();
	}

}
