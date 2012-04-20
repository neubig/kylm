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

/**
 * A data structure to handle a pair of values
 * @author neubig
 *
 * @param <T> The type of the first value
 * @param <U> The type of the second value
 */
public class KylmPair <T, U> {

	private final T first;
	private final U second;
	private transient final int hash;

	public KylmPair( T f, U s )
	{
		this.first = f;
		this.second = s;
		hash = (first == null? 0 : first.hashCode() * 31)
		+(second == null? 0 : second.hashCode());
	}

	public T getFirst()
	{
		return first;
	}
	public U getSecond()
	{
		return second;
	}

	@Override
	public int hashCode()
	{
		return hash;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals( Object oth )
	{
		if ( this == oth )
		{
			return true;
		}
		if ( oth == null || !(getClass().isInstance( oth )) )
		{
			return false;
		}
		KylmPair<T, U> other = this.getClass().cast(oth);
		return (first == null? other.first == null : first.equals( other.first ))
		&& (second == null? other.second == null : second.equals( other.second ));
	}

}
