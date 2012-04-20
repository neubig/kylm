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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import kylm.model.ngram.NgramLM;

/**
 * A class that can read an n-gram file in binary format
 * @author neubig
 *
 */
public class SerializedNgramReader extends NgramReader {

	@Override
	public NgramLM read(InputStream is) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(is);
		try {
			return (NgramLM)ois.readObject();
		} catch(ClassNotFoundException e) {
			throw new IOException(e);
		}
	}

}
