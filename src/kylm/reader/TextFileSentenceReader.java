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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;

import kylm.util.KylmTextUtils;

/**
 * Implementation of a loader that loads from a text file
 * @author neubig
 *
 */
public class TextFileSentenceReader implements SentenceReader {
	
	// iterator implementation
	private class TFSLIterator implements Iterator<String[]> {
		
		private BufferedReader reader = null;
		private String divider = null;
		
		public TFSLIterator(File file, String divider) throws FileNotFoundException {
			reader = new BufferedReader(new FileReader(file));
			this.divider = divider;
		}
		
		@Override
		public boolean hasNext() {
			try {
				return reader.ready();
			} catch (IOException e) {
				return false;
			}
		}

		@Override
		public String[] next() {
			try {
				String line = reader.readLine();
				if(line.length() == 0)
					return new String[0];
				StringTokenizer st=new StringTokenizer(line,divider); 
				String[] ret = new String[st.countTokens()];
				for(int i = 0; i < ret.length; i++)
					ret[i] = st.nextToken();
				return ret;
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Remove is not implemented");
		}
		
	}
	
	private File file = null;
	private String divider = null;
	
	/**
	 * The constructor, saves the file and uses a single space as the default divider 
	 * @param fileName The name of the file to be opened
	 * @throws IOException if the file doesn't exist or is unreadable
	 */
	public TextFileSentenceReader(String fileName) throws IOException {
		file = new File(fileName);
		if(!file.canRead())
			throw new IOException("File "+fileName+" does not exist or is unreadable");
		divider = KylmTextUtils.whiteSpaceString;
	}
	
	/**
	 * The constructor, saves the file name and uses the passed in divider 
	 * @param fileName The name of the file to be opened
	 * @param divider A regular expression that is used to divide strings in the corpus
	 * @throws IOException if the file doesn't exist or is unreadable
	 */
	public TextFileSentenceReader(String fileName, String divider) throws IOException {
		file = new File(fileName);
		if(!file.canRead())
			throw new IOException("File "+fileName+" does not exist or is unreadable");
		this.divider = divider;
	}

	@Override
	public Iterator<String[]> iterator() {
		try {
			return new TFSLIterator(file, divider);
		} catch(IOException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	@Override
	public boolean supportsReset() {
		return true;
	}

}
