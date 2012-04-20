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

package kylm.model;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import kylm.util.*;
/**
 * An abstract class representing many of the common functions of language models.
 * All probabilities/entropies are represented as log10 form floats.
 * @author neubig
 * TODO: allow limitation of vocabulary by size, not only by word frequency
 * TODO: allow conversion to and from regular/log probabilities
 */
public abstract class LanguageModel implements Serializable {

	/**
	 * The serialization version ID
	 */
	private static final long serialVersionUID = 4882315303447910570L;

	// debugging options
	protected int debug = 0;

	// identity variables
	protected String symbol = null;
	protected String name = null;
	protected Pattern regex = null; // the definition of all sentences accepted as a regular expression

	// setting variables
	protected boolean countTerminals = true; // whether or not to count terminals
	protected int maxLength = 0; // the maximum length of a sentence
	protected boolean closed = false; // whether the model is closed or not
	
	// vocabulary variables
	protected SymbolSet vocab = null;
	protected int vocabFrequency = 1; // all vocabulary words must be greater than this frequency
	protected int vocabLimit = 0; // the upper limit on vocab size, used to calculate unknown penalties
	// when no models are present
	protected String startSymbol = "<s>";
	protected String terminalSymbol = "</s>";
	protected String ukSymbol = "<unk>";

	// unknown model variables
	protected LanguageModel[] ukModels = null;
	protected int ukModelCount = 1;
	protected boolean modelAllWords = true;

	// class map
	protected ClassMap classMap;
	
	// buffers of the last sentence calculated
	protected float[] wordEnts = null;
	protected float[] simpleEnts = null;
	protected float[] unkEnts = null;
	protected float[] classEnts = null;

	////////////////////
	// equal function //
	////////////////////
	private static boolean eq(Object o1, Object o2) {
		if(o1 == null) return o2 == null;
		return o1.equals(o2);
	}
	public boolean equals(Object obj) {
		try {
			LanguageModel mod = (LanguageModel)obj;
			return
			closed == mod.closed &&
			countTerminals == mod.countTerminals &&
			vocabLimit == mod.vocabLimit &&
			ukModelCount == mod.ukModelCount &&
			eq(symbol, mod.symbol) &&
			eq(name, mod.name) &&
			eq(regex, mod.regex) &&
			(vocab == null?mod.vocab==null:vocab.syms.equals(mod.vocab.syms)) &&
			eq(startSymbol, mod.startSymbol) &&
			eq(terminalSymbol, mod.terminalSymbol) &&
			eq(ukSymbol, mod.ukSymbol) &&
			eq(classMap, mod.classMap) &&
			Arrays.equals(ukModels, mod.ukModels);	
		} catch(Exception e) { }
		return false;
	}

	///////////////////////
	// entropy functions //
	///////////////////////
	/**
	 * Get the entropies of the last word in the sequence by ID
	 * @param ids The IDs of the words in the sentence. Will always start
	 *  and end with the sentence terminal symbol.
	 * @param pos The position of the word to be judged in ids
	 * @return The entropy of the word at position pos given the rest as context 
	 */
	public abstract float getWordEntropy(int[] ids, int pos);

	/**
	 * Get the entropies of every word in a sentence by ID. The version implemented
	 *  in LanguageModel calls getWordEntropy individually for each value, but might
	 *  be overridden for higher efficiency.
	 * @param ids The IDs of the words in the sentence. Will always start
	 *  and end with the sentence terminal symbol.
	 * @return An array of entropies of length ids.length-1. The first non-terminal
	 *  symbol need not be assigned an entropy.
	 */
	public float[] getWordEntropies(int[] ids) {
		wordEnts = new float[ids.length-1];
		for(int i = 0; i < wordEnts.length; i++)
			wordEnts[i] = getWordEntropy(ids, i+1);
		return wordEnts;
	}

	/**
	 * Get the entropies of every word in a sentence. Sent should not contain
	 *  terminal symbols.
	 * @param sent The string of words
	 * @return Returns an array of float entropies.
	 */
	public float[] getWordEntropies(String[] sent) {
		// get the sentence IDs
		int[] ids = getSentenceIds(sent);
		// actually calculate
		wordEnts = getWordEntropies(ids);
		// add unknown word penalties for models if necessary
		unkEnts = new float[sent.length+1];
		if(ukModels != null) {
			for(int i = 0; i < sent.length; i++)
				if(!isInVocab(sent[i])) {
					unkEnts[i] = ukModels[ids[i+1]-2].getSentenceEntropy(KylmTextUtils.splitChars(sent[i]));
					wordEnts[i] += unkEnts[i];
				}
		}
		// if there are no models but there's a vocabulary limit, allow for that
		else if(vocabLimit > 0) {
			int remaining = vocabLimit-vocab.getSize();
			if(remaining <= 0)
				throw new IllegalArgumentException("vocab size has exceeded the vocab size limit");
			float ukPenalty = (float) Math.log10(1.0/remaining);
			for(int i = 0; i < sent.length; i++)
				if(!isInVocab(sent[i])) {
					unkEnts[i] += ukPenalty;
					wordEnts[i] += ukPenalty;
				}
		}
		return wordEnts;
	}
	
	public float[] getClassEntropies() { return classEnts; }
	public float[] getSimpleEntropies() { return simpleEnts; }
	public float[] getUnknownEntropies() { return unkEnts; }

	/**
	 * Returns the entropy of an entire sentence.
	 * @param sent The sentence to find the entropy of
	 * @return The entropy of the sentence
	 */
	public float getSentenceEntropy(String[] sent) {
		return KylmMathUtils.sum(getWordEntropies(sent));
	}
	public float getSentenceSimpleEntropy() { return KylmMathUtils.sum(simpleEnts); }
	public float getSentenceUnknownEntropy() { return KylmMathUtils.sum(unkEnts); }
	public float getSentenceClassEntropy() { return KylmMathUtils.sum(classEnts); }

	////////////////////////
	// training functions //
	////////////////////////

	public abstract void trainModel(Iterable<String[]> sl) throws IOException;

	////////////////////////
	// reporting function //
	////////////////////////
	public abstract String printReport();

	///////////////////////////////////////
	// vocabulary manipulation functions //
	///////////////////////////////////////

	/**
	 * Set the vocabulary to be used by the language model. 
	 * Everything else will be treated as an out of vocabulary word.
	 */
	public void setVocabulary(String[] voc) {

		// set the initial things
		initializeVocab();

		// add the rest of the vocabulary
		for(String s : voc)
			vocab.addSymbol(s);

	}

	private void initializeVocab() {

		// give a warning if the vocabulary is already set
		if(vocab != null && debug > 0)
			System.err.println("WARNING: Resetting vocabulary, could cause alignment problems");

		// create the set to hold the symbols
		vocab = new SymbolSet();
		// add the terminal, start, and unknown symbols
		vocab.addSymbol(startSymbol);
		vocab.addSymbol(terminalSymbol);
		vocab.addAlias(terminalSymbol, vocab.getId(startSymbol));
		// if symbols are identical, reserve space for where the 
		//  ending symbol would be
		if(vocab.getSize() == 1)
			vocab.pushSymbol(terminalSymbol);
		if(ukModels != null)
			for(LanguageModel lm : ukModels) {
				String sym = lm.getSymbol();
				if(sym == null)
					throw new IllegalArgumentException("A symbol must be specified for each unknown word model");
				if(sym.equals(terminalSymbol) || sym.equals(startSymbol))
					throw new IllegalArgumentException("Unknown model and terminal/start symbols cannot be equal");
				vocab.addSymbol(lm.getSymbol());
			}
		else
			vocab.addSymbol(ukSymbol);
	}

	/**
	 * Returns whether or not a string is in the vocabulary, returns false for
	 * special symbols as well.
	 * @param str The symbol to check
	 * @return whether or not the symbol is in the vocab
	 */
	public boolean isInVocab(String str) {
		Integer idx = vocab.getId(str);
		return idx != null && isInVocab(idx);
	}
	/**
	 * Returns whether or not an id is in the vocabulary.
	 * @param idx the index to check
	 * @return whether or not the symbol is in the vocab
	 */
	public boolean isInVocab(int idx) {
		return idx < 2 || idx > ukModelCount+1;
	}

	/**
	 * Get an array of IDs for a sentence, add terminal symbols on the left side,
	 * and on the right side if countTerminals is true.
	 * @param sent The sentence to convert
	 * @return The ID array
	 */
	public int[] getSentenceIds(String[] sent) {
		int[] ret = new int[sent.length+(countTerminals?2:1)];
		for(int i = 0; i < sent.length; i++)
			ret[i+1] = getId(sent[i]);
		return ret;
	}

	/**
	 * Get all in vocabulary words.
	 * @return An array containing every vocabulary word
	 */
	public String[] getVocabulary() { return vocab.getSymbols(); }

	/**
	 * Load the vocabulary and trim it at the appropriate level.
	 * @param sl The sentenceLoader containing the corpus sentences.
	 */
	public void importVocabulary(Iterable<String[]> sl) throws IOException {
		if(debug > 0)
			System.err.println("LanguageModel.importVocabulary(): Started for "+name);
		// initialize the hash table
		HashMap<String, Integer> counts = new HashMap<String, Integer>();

		// cycle through the sentences counting the vocabulary
		for(String[] sent : sl) {
			// keep track of the maximum sentence length to use later
			maxLength = Math.max(sent.length+2, maxLength);
			for(String s : sent) {
				Integer idx = counts.get(s);
				counts.put(s, idx==null?1:idx+1);
			}
		}

		if(debug > 0)
			System.err.println("LanguageModel.importVocabulary(): Vocab "+counts.size()+" before trimming");

		// create the symbol set
		initializeVocab();

		Vector<String> toAdd = new Vector<String>();
		// sort the vocabulary and add it to the symbol set
		for(Entry<String, Integer> e : counts.entrySet())
			if(e.getValue() > vocabFrequency)
				toAdd.add(e.getKey());
		Collections.sort(toAdd);
		for(String s : toAdd)
			vocab.addSymbol(s);

		if(debug > 0)
			System.err.println("LanguageModel.importVocabulary(): Finished with size "+vocab.getSize()+" for "+name);

	}

	//////////////////////////////////
	// functions regarding unknowns //
	//////////////////////////////////

	/**
	 * Find the appropriate unknown word model for the key
	 */
	public int findUnknownId(String key) {
		if(closed)
			throw new IllegalArgumentException("Unknown word "+key+" found in closed model");
		if(ukModels == null)
			return 2;
		for(int i = 0; i < ukModels.length; i++) {
			Pattern p = ukModels[i].getRegex();
			if(p == null || p.matcher(key).matches())
				return i+2;
		}
		throw new IllegalArgumentException("No unknown word model found to match "+key);
	}

	/////////////////////////////////
	// functions regarding symbols //
	/////////////////////////////////

	/**
	 * Get the ID assigned to a particular word
	 * @param word The word to find the id for
	 * @return The id of the word pointing to IVs or the unknown model for OOVs
	 */
	public int getId(String word) {
		Integer idx = vocab.getId(word);
		if(idx == null) {
			idx = findUnknownId(word);
			vocab.addAlias(word, idx);
		}
		return idx;
	}

	///////////////////////////////
	// methods for serialization //
	///////////////////////////////
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(debug);
		out.writeObject(symbol);
		out.writeObject(name);
		out.writeObject(regex);
		out.writeBoolean(closed);
		out.writeBoolean(countTerminals);
		out.writeInt(maxLength);
		out.writeObject(symbol);
		out.writeObject(vocab);
		out.writeInt(vocabFrequency);
		out.writeInt(vocabLimit);
		out.writeObject(startSymbol);
		out.writeObject(terminalSymbol);
		out.writeObject(ukSymbol);
		out.writeObject(ukModels);
		out.writeObject(classMap);
	}
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		debug = in.readInt();
		try { symbol = (String) in.readObject(); } catch(NullPointerException e) { }
		try { name = (String) in.readObject(); } catch(NullPointerException e) { }
		try { regex = (Pattern) in.readObject(); } catch(NullPointerException e) { }
		closed = in.readBoolean();
		countTerminals = in.readBoolean();
		maxLength = in.readInt();
		try { symbol = (String) in.readObject(); } catch(NullPointerException e) { }
		try { vocab = (SymbolSet) in.readObject(); } catch(NullPointerException e) { }
		vocabFrequency = in.readInt();
		vocabLimit = in.readInt();
		try { startSymbol = (String) in.readObject(); } catch(NullPointerException e) { }
		try { terminalSymbol = (String) in.readObject(); } catch(NullPointerException e) { }
		try { ukSymbol = (String) in.readObject(); } catch(NullPointerException e) { }
		try { ukModels = (LanguageModel[]) in.readObject(); } catch(NullPointerException e) { }
		if(ukModels != null) ukModelCount = ukModels.length;
		else ukModelCount = 1;
		try { classMap = (ClassMap) in.readObject(); } catch(NullPointerException e) { }
		
	}

	///////////////
	// accessors //
	///////////////
	public int getDebug() {	return debug; }
	public void setDebug(int debug) {	this.debug = debug;	}
	public String getSymbol() {	return symbol;	}
	public void setSymbol(String symbol) {	this.symbol = symbol;	}
	public String getName() {	return name;	}
	public void setName(String name) {	this.name = name;	}
	public SymbolSet getVocab() {	
		if(vocab == null)
			initializeVocab();
		return vocab;	
	}
	public void setVocab(SymbolSet newVocab) {
		initializeVocab();
		vocab.addSymbols(newVocab.getSymbols());
	}
	public String getStartSymbol() {	return startSymbol;	}
	public void setStartSymbol(String startSymbol) {	this.startSymbol = startSymbol;	}
	public String getTerminalSymbol() {	return terminalSymbol;	}
	public void setTerminalSymbol(String terminalSymbol) {	this.terminalSymbol = terminalSymbol;	}
	public String getUnknownSymbol() {	return ukSymbol;	}
	public void setUnknownSymbol(String ukSymbol) {	this.ukSymbol = ukSymbol;	}
	public LanguageModel[] getUnknownModels() {	return ukModels;	}
	public void setUnknownModels(LanguageModel[] ukModels) {
		this.ukModels = ukModels;
	}
	
	public boolean isClosed() { return closed; }
	public void setClosed(boolean closed) { this.closed = closed; }

	public boolean getCountTerminals() {
		return countTerminals;
	}

	public void setCountTerminals(boolean countTerminals) {
		this.countTerminals = countTerminals;
	}

	/**
	 * Get the frequency limit for the vocabulary.
	 * @return The frequency limit for the vocabulary. All words that occur this
	 * many times or fewer will be treated as unknowns.
	 */
	public int getVocabFrequency() {
		return vocabFrequency;
	}

	/**
	 * Set the limit on the vocabulary frequency.
	 * @param vocabFrequency All words that occur this many times or fewer will be treated
	 * as unknown words.
	 */
	public void setVocabFrequency(int vocabFrequency) {
		this.vocabFrequency = vocabFrequency;
	}

	public Pattern getRegex() { return regex; }
	public void setRegex(String regex) { 
		this.regex = Pattern.compile(regex); 
	}

	public int getVocabLimit() {
		return vocabLimit;
	}

	public void setVocabLimit(int vocabLimit) {
		this.vocabLimit = vocabLimit;
	}
	public int getMaxLength() {
		return maxLength;
	}
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	public ClassMap getClassMap() {
		return classMap;
	}
	public void setClassMap(ClassMap cm) {
		this.classMap =  cm;
	}
	public int getUnknownModelCount() {
		return this.ukModelCount;
	}

}
