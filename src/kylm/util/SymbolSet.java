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

package kylm.util;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * A class that holds a mapping from vocabulary to integers 
 * @author neubig
 *
 */
public class SymbolSet implements Serializable {

	/**
	 * version ID for the serialization
	 */
	private static final long serialVersionUID = 6295504917453701558L;

	// the data structures to hold the vocabulary
	public HashMap<String, Integer> ids = null;
	public Vector<String> syms = null;

	/**
	 * Create a new symbol set
	 */
	public SymbolSet() {
		ids = new HashMap<String, Integer>();
		syms = new Vector<String>();
	}

	/**
	 * Add a symbol to the vocabulary. If it doesn't exist, or exists 
	 * only as an alias, it is added. If it does exist, it is not added, 
	 * but it's number is returned.
	 * @param sym The symbol to be added.
	 * @param overrideAlias Whether or not to delete aliases.
	 * @return The ID assigned to the added symbol.
	 */
	public int addSymbol(String sym, boolean overrideAlias) {
		// check to see if the symbol already exists
		Integer idx = ids.get(sym);
		// if the symbol exists and is not an alias, return the proper number
		if(idx != null && (!overrideAlias || syms.get(idx).equals(sym)))
			return idx;
		// get the new id, add the symbol, and return
		idx = syms.size();
		syms.add(sym);
		ids.put(sym, idx);
		return idx;
	}

	public int addSymbol(String sym) {
		return addSymbol(sym,false);
	}

	/**
	 * Add multiple symbols to the vocabulary
	 * @param symbols The symbols to add
	 */
	public void addSymbols(String[] symbols) {
		for (String s : symbols)
			addSymbol(s);
	}

	/**
	 * Add an alias to the vocabulary.
	 * @param sym The symbol of the alias.
	 * @param id The id number that the alias should point to.
	 */
	public void addAlias(String sym, int id) {
		ids.put(sym, id);
	}

	/**
	 * Get the symbol associated with the ID.
	 * @param id The ID to search for.
	 * @return The symbol that is associated with id.
	 */
	public String getSymbol(int id) { return syms.get(id); }

	/**
	 * Get the size of the symbol set.
	 * @return The size.
	 */
	public int getSize() { return syms.size(); }

	/**
	 * Return every symbol in the symbol set.
	 * @return An array containing every symbol in the symbol set
	 */
	public String[] getSymbols() { return syms.toArray(new String[syms.size()]); }


	/**
	 * Get the ID associated with a symbol
	 * @param s The symbol
	 * @return The id, or null if the symbol doesn't exist
	 */
	public Integer getId(String s) {
		return ids.get(s);
	}

	/////////////////////////////
	// serialization functions //
	/////////////////////////////
	private void writeObject(ObjectOutputStream out) throws IOException {
		// print the ones in the vocabulary
		StringBuffer sb = new StringBuffer();
		for(String s : syms)
			sb.append(s).append('\n');
		sb.deleteCharAt(sb.length()-1);
		out.writeObject(sb.toString());
		// print the ones not in the vocabulary
		LinkedList<Integer> myList = new LinkedList<Integer>();
		sb  = new StringBuffer();
		for(Entry<String, Integer> e : ids.entrySet()) {
			if(!e.getKey().equals(syms.get(e.getValue()))) {
				sb.append(e.getKey()).append('\n');
				myList.add(e.getValue());
			}
		}
		if(sb.length() != 0)
			sb.deleteCharAt(sb.length()-1);
		out.writeObject(sb.toString());
		for(Integer i : myList)
			out.writeInt(i);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		String strings = (String)in.readObject();
		String[] vec = strings.split("\n");
		syms = new Vector<String>(Arrays.asList(vec));
		ids =  new HashMap<String, Integer>();
		for(int i = 0; i < syms.size(); i++)
			ids.put(syms.get(i), i);
		strings = (String)in.readObject();
		if(strings.length() > 0){
			vec = strings.split("\n");
			for(String s : vec)
				ids.put(s, in.readInt());
		}
	}

	////////////////////
	// equal function //
	////////////////////
	public boolean equals(Object obj) {
		try {
			SymbolSet set = (SymbolSet)obj;
			return ids.equals(set.ids) && syms.equals(set.syms);
		} catch(Exception e) { }
		return false;
	}

	/**
	 * Read a symbol set from a file <br>
	 * The format can either be one symbol per line, or one symbol
	 * followed by its ID per line, in which case the lines must be
	 * in ascending order starting with symbol number one
	 * @param fileName The name of the file to read from
	 * @return The symbol set read from that file
	 * @throws IOException If there is a problem with the input or the file is
	 *  not in the proper format
	 */
	public static SymbolSet readFromFile(String fileName) throws IOException {
		SymbolSet ret = new SymbolSet();
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String s = null;
		while(null != (s = br.readLine())) {
			StringTokenizer st = new StringTokenizer(s, " \t");
			int sym = ret.addSymbol(st.nextToken());
			if(st.hasMoreTokens() && sym != Integer.parseInt(st.nextToken()))
				throw new IOException("Illegal line in Symbol File: "+s);
		}
		return ret;
	}

	public void writeToFile(String string, boolean writeNumbers) throws IOException {
		PrintStream ps = new PrintStream(new FileOutputStream(string));
		HashSet<String> printed = new HashSet<String>();
		for(int i = 0; i < syms.size(); i++) {
			String str = syms.get(i);
			if(!printed.contains(str)) {
				printed.add(str);
				ps.print(str);
				if(writeNumbers) {
					ps.print(" ");
					ps.print(i);
				}
				ps.println();
			}
		}
		ps.close();
	}

	/**
	 * Add a symbol, but only if it already exists (used to fill reserved spaces,
	 * and should only be used sparingly). The ID for the symbols remains the same
	 * as before the method was called.
	 * @param sym The symbol to add
	 * @return The index of the symbol that was added
	 */
	public int pushSymbol(String sym) {
		// check to see if the symbol already exists
		Integer idx = ids.get(sym);
		// if the symbol exists and is not an alias, return the proper number
		if(idx == null || (!syms.get(idx).equals(sym)))
			throw new IllegalArgumentException("Attempt to push symbol that doesn't already exist");
		// get the new id, add the symbol, and return
		idx = syms.size();
		syms.add(sym);
		return idx;
	}

}
