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

import java.io.*;

import kylm.model.ngram.NgramLM;

/**
 * An abstract class the defines the functions needed to write an n-gram
 * model to a file or output stream.
 * @author neubig
 *
 */
public abstract class NgramWriter {
	
	/**
	 * Write the language model to a file.
	 * @param lm The language model to write
	 * @param fileName The file to write it to
	 * @throws IOException If the file could not be written to
	 */
	public void write(NgramLM lm, String fileName) throws IOException {
		BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(fileName), 1024);
		write(lm, os);
		os.close();
	}
	
	/**
	 * Write the language model to a generic output stream
	 * @param lm The language model to write
	 * @param os The output stream to write to
	 * @throws IOException If there was an error during output
	 */
	public abstract void write(NgramLM lm, OutputStream os) throws IOException;
	
}