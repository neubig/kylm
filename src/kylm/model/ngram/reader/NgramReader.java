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
import java.util.zip.GZIPInputStream;

import kylm.model.ngram.NgramLM;

public abstract class NgramReader {

	/**
	 * Read a language model from a file
	 * @param fileName The file to read the model from
	 * @return The model that has been read
	 * @throws IOException If the file could not be written to
	 */
	public NgramLM read(String fileName) throws IOException {
		// X.Yao, 2010-07-22, make it able to read gzip-compressed file
	    FileInputStream fin = new FileInputStream(fileName);
		String file = fileName.toLowerCase();
		if (file.endsWith(".gz") || file.endsWith(".gzip"))
			return read(new GZIPInputStream(fin));
		else
			return read(fin);
	}

	/**
	 * Read a language model from an input stream
	 * @param is The input stream to read the model from
	 * @return The model that has been read
	 * @throws IOException If the file could not be read from
	 */
	public abstract NgramLM read(InputStream is) throws IOException;

}
