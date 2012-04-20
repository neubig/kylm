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

import kylm.model.ngram.NgramLM;
import kylm.model.ngram.NgramNode;
import kylm.util.KylmMathUtils;

/**
 * Calculate N-gram probabilities with absolute smoothing
 * @author neubig
 *
 */
public class AbsoluteSmoother extends NgramSmoother {

	/**
	 * Serialization ID
	 */
	private static final long serialVersionUID = 199895368874934771L;

	protected Object discounts = null;
	protected float defaultDiscount = 0;

	protected NgramLM lm = null;

	/**
	 * Create a smoother and estimate the discounts automatically when it comes
	 *  time to smooth
	 */
	public AbsoluteSmoother() { }

	/**
	 * Create a smoother with pre-set discounts
	 * @param discounts The discounts to use
	 */
	public AbsoluteSmoother(float[] discounts) {
		this.discounts = discounts;
	}

	@Override
	public void smooth(NgramLM lm) {

		if(debug > 0)
			System.err.println("AbsoluteSmoother: calculating FOFs");

		this.lm = lm;

		// find the frequencies of frequencies
		final int[][] fofs = calcFofs(lm, 5);

		// mark the values to be trimmed If they haven't already been
		if(cutoffs == null)
			cutoffs = new int[lm.getN()];
		markTrimmed(lm);

		// adjust the discounts if necessary
		if(discounts == null) {
			calcDiscounts(fofs);
		}

		if(debug > 0)
			System.err.println("AbsoluteSmoother: interpolating model");

		for(int n = 0; n < lm.getN(); n++)
			process(lm.getRoot(), 0, n);

	}

	protected float getDiscount(int order, int freq) {
		float ret = ((float[])discounts)[order];
		// System.err.println("Abs.getDiscount("+order+","+freq+") = "+ret);
		return ret;
	}

	protected void calcDiscounts(int[][] fofs) {
		float[] newdisc = new float[fofs.length];
		for(int i = (smoothUnigrams?0:1); i < newdisc.length; i++) {
			newdisc[i] = fofs[i][0] / (float)(fofs[i][0] + 2*fofs[i][1]);
			if(newdisc[i] != newdisc[i]) newdisc[i] = defaultDiscount;
		}
		discounts = newdisc;
	}

	private void process(NgramNode node, int i, int n) {
		// if it has no children, nothing to be done
		if(!node.hasChildren())
			return;
		// if this is not the level to be processed, move on
		if(i < n) {
			for(NgramNode child : node)
				process(child, i+1, n);
			return;
		}
		// count the sum of the children
		int sum = 0, numChild = 0;
		for(NgramNode child : node) {
			if(child != null) {
				numChild++;
				sum += child.getCount();
			}
		}
		// calculate the backoff and scores
		double realBackoffScore = 0;
		// node.setBackoffScore( (float) Math.log10(node.getChildren().size()*discounts[n])-logSum );
		int good = 0;
		for(NgramNode child : node) {
			if(child == null) continue;
			double discount = getDiscount(n, child.getCount());
			double childScore = (child.getCount()-discount)/sum;
			realBackoffScore += discount/sum;
			if(child.getScore() != NgramNode.TRIM_SCORE) {
				child.setScore( (float) Math.log10(childScore) );
				good++;
			}
			else {
				// System.out.println("Deleting "+lm.getNodeName(child));
				realBackoffScore += childScore;
			}
		}
		this.trimNode(node, n, lm.getNgramCounts(), good);
		node.setBackoffScore((float)Math.log10(realBackoffScore));
		// interpolate with the fallback state
		if(i != 0) {
			for(NgramNode child : node)
				if(child != null && child.getScore() != NgramNode.TRIM_SCORE) 
					child.setScore(KylmMathUtils.logAddition(child.getScore(), node.getBackoffScore()+child.getFallback().getScore()));
		}
		else if (smoothUnigrams) {
			// TODO: This should be distributed evenly between all unknown models
			NgramNode unk = node.getChild(2, (lm.getN()>1?NgramNode.ADD_BRANCH:NgramNode.ADD_LEAF));
			if(unk.getScore() != 0.0f)
				unk.setScore(KylmMathUtils.logAddition(unk.getScore(), node.getBackoffScore()));
			else {
				lm.getNgramCounts()[0]++;
				unk.setScore(node.getBackoffScore());
			}
		}
	}


	//	public void setDiscounts(float discounts[]) {
	//		this.discounts = discounts;
	//	}
	//
	//	public float[] getDiscounts() {
	//		return discounts;
	//	}

	@Override
	public String getAbbr() { return "abs";	}

	@Override
	public String getName() { return "Absolute Smoothing"; }

	public float getDefaultDiscount() {
		return defaultDiscount;
	}

	public void setDefaultDiscount(float defaultDiscount) {
		this.defaultDiscount = defaultDiscount;
	}
	
}
