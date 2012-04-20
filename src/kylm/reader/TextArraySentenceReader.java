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

package kylm.reader;

import java.util.Iterator;
import java.util.StringTokenizer;

import kylm.util.KylmTextUtils;

/**
 * Implementation of a loader that loads directly from a text array in memory
 * @author neubig
 *
 */
public class TextArraySentenceReader implements SentenceReader {
	
	// iterator implementation
	private class TASLIterator implements Iterator<String[]> {
		
		private int pos;
		private String[] sents = null;
		private String divider = null;
		
		public TASLIterator(String[] sents, String divider) {
			pos = 0;
			this.sents = sents;
			this.divider = divider;
		}
		
		@Override
		public boolean hasNext() {
			return pos != sents.length;
		}

		@Override
		public String[] next() {
			if(sents[pos].length() == 0)
				return new String[0];
			StringTokenizer st=new StringTokenizer(sents[pos++],divider); 
			String[] ret = new String[st.countTokens()];
			for(int i = 0; i < ret.length; i++)
				ret[i] = st.nextToken();
			return ret;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Remove is not implemented");
		}
		
	}
	
	private String[] sents = null;
	private String divider = null;
	
	/**
	 * The constructor, saves the sentence array and uses a single space as the default
	 * divider 
	 * @param sents the sentence array
	 */
	public TextArraySentenceReader(String[] sents) {
		this.sents = sents;
		divider = KylmTextUtils.whiteSpaceString;
	}
	
	/**
	 * The constructor, saves the sentence array and uses the passed in divider 
	 * @param sents the sentence arrays
	 */
	public TextArraySentenceReader(String[] sents, String divider) {
		this.sents = sents;
		this.divider = divider;
	}

	@Override
	public Iterator<String[]> iterator() {
		return new TASLIterator(sents, divider);
	}

	@Override
	public boolean supportsReset() {
		return true;
	}

}
