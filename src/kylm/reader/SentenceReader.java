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

/**
 * An abstract class that allows the loading of sentences
 * @author neubig
 * TODO: Add a "close" function
 */
public interface SentenceReader extends Iterable<String[]> {

	@Override
	public abstract Iterator<String[]> iterator();
	
	/**
	 * Whether the sentence loader supports returning multiple iterators or not.
	 * If multiple iterators are not supported, input can only be read once.
	 * @return Whether multiple iterators can be returned.
	 */
	public abstract boolean supportsReset();

}
