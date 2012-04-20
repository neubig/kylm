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

package kylm.model.ngram.smoother;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import kylm.model.ngram.NgramLM;
import kylm.model.ngram.NgramNode;
import kylm.util.KylmMathUtils;
import kylm.util.KylmTextUtils;

/**
 * Calculate N-gram probabilities with Katz smoothing
 * TODO: This is not statistically sound when trimming
 * @author neubig
 */
public class GTSmoother extends NgramSmoother {

	/**
	 * Serialization ID
	 */
	private static final long serialVersionUID = -8438852797430608631L;

	private int k = 5;

	/**
	 * Create a smoother and estimate the discounts automatically when it comes
	 *  time to smooth
	 */
	public GTSmoother() { }

	@Override
	public void smooth(NgramLM lm) {

		// mark the values to be removed
		markTrimmed(lm);

		int i, j;
		float rstar, kstar;
		// find the fofs
		final int n = lm.getN();
		int[][] fofs = this.calcFofs(lm, k+1);

		// get rstar
		float[][] discounted = new float[n][k];
		for(i = 0; i < n; i++) {
			// make sure there are no zero values
			for(j = 0; j <= k && fofs[i][j] != 0; j++);
			// if adjusted do the calculations
			if(j > k) {
				kstar = (k+1.0f)*fofs[i][k]/fofs[i][0];
				for(j = 1; j <= k; j++) {
					// get rstar
					rstar = (j+1.0f)*fofs[i][j]/fofs[i][j-1];
					if(rstar > j)
						break;
					// get the discounted value
					discounted[i][j-1] = j*(rstar/j-kstar)/(1-kstar);
				}
			}
			// otherwise just fill with the normal values
			if(j <= k)
				discounted[i] = null;
		}

		if(debug > 1) {
			for(int[] fofarr : fofs)
				System.err.println("GTSmoother: fofs = "+KylmTextUtils.join(", ", fofarr));
		}

		// get the scores and back-offs
		for(i = 0; i < n; i++) 
			process(lm, lm.getRoot(), discounted[i], 0, i);
	}

	public void process(NgramLM lm, NgramNode node, float[] discounted, int i, int lev) {
		
		// if it has no children, nothing to be done
		if(!node.hasChildren())
			return;
		
		// if we're not on the right level, recurse
		if(i != lev) {
			for(NgramNode child : node) 
				process(lm, child, discounted, i+1, lev);
			return;
		}
		
		// if not discounted, simple maximum likelihood
		int good = 0;
		float bo = 0;
		NgramNode fb = node.getFallback();
		// get the non-discounted value
		if(discounted == null) {
			for(NgramNode child : node)
				if(child.getScore() == NgramNode.TRIM_SCORE)
					bo += child.getCount();
			bo /= node.getCount();
			for(NgramNode child : node) {
				if(child.getScore() != NgramNode.TRIM_SCORE) {
					float score = child.getCount()/(float)node.getCount();
					if(bo != 0)
						score += Math.pow(10, fb.getChild(child.getId()).getScore())*bo;
					child.setScore( (float) Math.log10(score) );
					good++;
				}
			}

		}
		// otherwise, perform katz backoff
		else {
			float numer = 1, denom = 1;
			for(NgramNode child : node) {
				if(child.getScore() == NgramNode.TRIM_SCORE)
					continue;
				float adj = ( child.getCount() <= k ? discounted[child.getCount()-1] : child.getCount() );
				child.setScore(adj / node.getCount());
				numer -= child.getScore();
				child.setScore( (float) Math.log10(child.getScore()) );
				if(fb != null)
					denom -= Math.pow(10, fb.getChild(child.getId()).getScore());
				good++;
			}
			bo = (denom <= 0.0f ? 0.0f : numer/denom);
		}
		// delete backoff values slightly less than one
		if(bo < -.0001f)
			throw new IllegalArgumentException("Illegal backoff value "+bo+" in "+lm.getNodeName(node));
		else if(bo < 0.0f)
			bo = 0.0f;
		if(i == 0 && smoothUnigrams) {
			// TODO: This should be distributed evenly between all unknown models
			NgramNode unk = node.getChild(2, (lm.getN()>1?NgramNode.ADD_BRANCH:NgramNode.ADD_LEAF));
			if(unk.getScore() != 0.0f)
				unk.setScore(KylmMathUtils.logAddition(unk.getScore(),(float)Math.log10(bo)));
			else {
				lm.getNgramCounts()[0]++;
				unk.setScore((float)Math.log10(bo));
			}
		} else
			node.setBackoffScore( bo == 0.0f ? Float.NEGATIVE_INFINITY : (float)Math.log10(bo));
		trimNode(node, i, lm.getNgramCounts(), good);
	}

	public void setK(int k) {
		this.k = k;
	}

	public int getK() {
		return k;
	}

	///////////////////////////////
	// methods for serialization //
	///////////////////////////////
	protected void writeObject(ObjectOutputStream out) throws IOException {
		super.writeObject(out);
		out.writeInt(k);
	}
	protected void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		super.readObject(in);
		k = in.readInt();
	}

	@Override
	public String getAbbr() { return "gt"; }

	@Override
	public String getName() { return "Good-Turing";	}

}
