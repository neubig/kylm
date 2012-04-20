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
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

/**
 * A single node in a tree holding n-gram data
 * @author neubig
 *
 */
public class NgramNode implements Serializable, Iterable<NgramNode> {

	private static final long serialVersionUID = 7981382079826823765L;

	public static final int ADD_NONE = 0;
	public static final int ADD_LEAF = 1;
	public static final int ADD_BRANCH = 2;

	private static final float diff = 0.0001f;

	public static final float TRIM_SCORE = 9999.09f;

	protected int id = -1;
	protected int count = 0;
	protected float score = 0;
	protected NgramNode parent = null;

	public static class NgramNodeIdComparator implements Comparator<NgramNode> {
		@Override
		public int compare(NgramNode o1, NgramNode o2) {
			return o1.id-o2.id;
		}
	}

	/**
	 * Create a new NgramNode that represents id and has a parent parent
	 * @param id The id of the symbol represented by this ngram
	 * @param parent The parent of this ngram in the tree
	 */
	public NgramNode(int id, NgramNode parent) {
		this.id = id;
		this.parent = parent;
	}

	/**
	 * Get a child from the child vector, return null if no child exists.
	 * @param id The id of the child to find.
	 * @return The child.
	 */
	public NgramNode getChild(int id) {
		return getChild(id, ADD_NONE);
	}

	/**
	 * Get a child from the child vector. If add is true and no child exists, add
	 *  one. If add is false and no child exists, return null.
	 * @param id The id of the child to find.
	 * @param add Whether or not to add the child if it exists.
	 * @return The child.
	 */
	public NgramNode getChild(int id, int add) {
		if(add != ADD_NONE)
			throw new IllegalArgumentException("Cannot add a child for a leaf node");
		return null;
	}

//	/**
//	 * Get the vector of children
//	 * @return The vector
//	 */
//	protected Vector<NgramNode> getChildren() {
//		throw new IllegalArgumentException("Leaf nodes have no children");
//	}

	/**
	 * Return whether this node has children or not
	 */
	public boolean hasChildren() {
		return false;
	}

	/**
	 * Get the node to be fallen back to when the context doesn't exist.
	 * @return The fallback node
	 */
	public NgramNode getFallback() {
		// if the fallback doesn't exist, find it
		if(parent == null)
			return null;
		// if this is a unigram, the fallback is the root
		else if(parent.parent == null)
			return parent;
		// otherwise, get the parent's fallback and advance one
		else
			return parent.getFallback().getChild(id);
	}

	public String toString() {
		return id+"("+score+")"+parent;
	}

	public final NgramNode getParent() { return parent; }
	public final void setParent(NgramNode parent) { this.parent = parent; }
	public final int getId() { return id; }
	public final void setId(int id) { this.id = id; }
	public final int getCount() { return count; }
	public final void setCount(int count) { this.count = count; }
	public final float getScore() { return score; }
	public final void setScore(float score) {
		this.score = score;
	}
	public float getBackoffScore() {
		return Float.NaN;
	}
	public void setBackoffScore(float backoff) {
		throw new IllegalArgumentException("Leaf nodes have no backoff score");
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			// delete the entire tree
			parent = null;
		} finally {
			super.finalize();
		}
	}

	////////////////////
	// equal function //
	////////////////////
	protected static final boolean eq(Object o1, Object o2) {
		if(o1 == null) return o2 == null;
		return o1.equals(o2);
	}
	protected static final boolean eq(float d1, float d2) {
		return (d1 == d2 || Math.abs(d1-d2) < diff);
	}
	protected static final boolean eq(int d1, int d2) {
		return d1 == d2;
	}
	public boolean equals(Object obj) {
		try {
			NgramNode nod = (NgramNode)obj;
			return
				eq(id, nod.id) &&
				eq(score, nod.score);
		} catch(Exception e) { }
		return false;
	}

	///////////////////////////////
	// methods for serialization //
	///////////////////////////////
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(id);
		out.writeInt(count);
		out.writeFloat(score);
	}
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		id = in.readInt();
		count = in.readInt();
		score = in.readFloat();
	}

	public final void incrementCount() {
		count++;
	}

	public void setChildren(Vector<NgramNode> children) {
		throw new IllegalArgumentException("Leaf nodes have no children");
	}

	@Override
	public Iterator<NgramNode> iterator() {
		throw new IllegalArgumentException("Leaf nodes have no children");
	}

	public int getChildCount() {
		return 0;
	}

}
