package kylm.model.ngram;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

public final class BranchNode extends NgramNode {

	private class SkipIterator implements Iterator<NgramNode> {

		private BranchNode root = null;
		private int pos = 0;
		private int last = -1;

		public SkipIterator(BranchNode branchNode) {
			root = branchNode;
		}

		@Override
		public boolean hasNext() {
			while(pos < root.children.size() && root.children.get(pos) == null)
				pos++;
			return pos < root.children.size();
		}

		@Override
		public NgramNode next() {
			while(pos < root.children.size() && root.children.get(pos) == null)
				pos++;
			if(pos >= root.children.size())
				throw new NoSuchElementException();
			last = pos;
			return root.children.get(pos++);
		}

		@Override
		public void remove() {
			root.children.set(last, null);
			root.childCount--;
		}

	}

	/**
	 * For serialization
	 */
	private static final long serialVersionUID = 435179356854434311L;

	protected float boscore = 0;
	protected Vector<NgramNode> children = null;
	protected int childCount;

	public BranchNode(int id, NgramNode parent) {
		super(id, parent);
	}

	@Override
	public String toString() {
		return id+"("+score+"/"+boscore+")"+parent;
	}

	@Override
	public NgramNode getChild(int id, int add) {
		NgramNode child = null;
		int bot = 0, top, mid = 0;
		if(children != null) {
			// root node is indexed lookup
			if(parent == null) {
				while(id >= children.size())
					children.add(null);
				child = children.get(id);
			}
			// otherwise search
			else {
				top = children.size();
				while(bot < top) {
					mid = (bot+top)/2;
					child = children.get(mid);
					if(child.id > id)
						top = mid;
					else if(child.id < id)
						bot = ++mid;
					else
						return child;
				}
				child = null;
			}
		}
		if(child == null && add != ADD_NONE) {
			if(children == null)
				children = new Vector<NgramNode>(1);
			child = ( add == ADD_LEAF ? new NgramNode(id, this) : new BranchNode(id, this) );
			if(parent == null)
				children.set(id, child);
			else
				children.insertElementAt(child, mid);
			childCount++;
		}
		return child;
	}

	@Override
	public boolean hasChildren() {
		return children != null;
	}

	@Override
	public float getBackoffScore() {
		return boscore;
	}

	@Override
	public void setBackoffScore(float backoff) {
		boscore = backoff;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			// delete the entire tree
			if(children != null)
				for(NgramNode child : children)
					if(child != null)
						child.finalize();
		} finally {
			super.finalize();
		}
	}

	@Override
	public boolean equals(Object obj) {
		try {
			BranchNode nod = (BranchNode)obj;
			return
				super.equals(nod) &&
				eq(childCount, nod.childCount) &&
				eq(boscore, nod.boscore) &&
				eq(children, nod.children);
		} catch(Exception e) { }
		return false;
	}

	@Override
	public void setChildren(Vector<NgramNode> newChildren) {
		if(parent == null) {
			Collections.fill(children, null);
			for(NgramNode child : newChildren)
				children.set(child.getId(), child);
		} else
			children = new Vector<NgramNode>(newChildren);
		childCount = newChildren.size();
	}

	///////////////////////////////
	// methods for serialization //
	///////////////////////////////
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(id);
		out.writeFloat(score);
		if(children == null)
			out.writeInt(0);
		else {
			if(children.size() == childCount)
				out.writeInt(children.size()*-1);
			else {
				out.writeInt(children.size());
				out.writeInt(childCount);
			}
			out.writeFloat(boscore);
			int check = 0;
			for(NgramNode node : children) {
				if(node != null) {
					out.writeObject(node);
					check++;
				}
			}
			if(check != childCount)
				throw new IllegalArgumentException("wrote a wrong number of values");
		}
	}
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		id = in.readInt();
		score = in.readFloat();
		int size = in.readInt();
		if(size != 0) {
			if(size < 0) {
				size *= -1;
				childCount = size;
			}
			else
				childCount = in.readInt();
			children = new Vector<NgramNode>(Math.abs(size));
			children.setSize(Math.abs(size));
			boscore = in.readFloat();
			for(int i = 0; i < childCount; i++) {
				NgramNode node = (NgramNode)in.readObject();
				if(size != childCount)
					children.set(node.id, node);
				else
					children.set(i, node);
				node.parent = this;
			}
		}
	}

	public void setChildrenSize(int size) {
		if(children == null)
			children = new Vector<NgramNode>(size);
		children.setSize(size);
	}

	@Override
	public Iterator<NgramNode> iterator() {
		if(parent != null)
			return children.iterator();
		else
			return new SkipIterator(this);
	}

	@Override
	public int getChildCount() {
		return childCount;
	}

}
