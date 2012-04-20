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

/**
 * Calculate N-gram maximum-likelihood probabilities (no smoothing)
 * @author neubig
 *
 */
public class MLSmoother extends NgramSmoother {

	/**
	 * Serialization ID
	 */
	private static final long serialVersionUID = -4731041681002702062L;

	@Override
	public void smooth(NgramLM lm) {
		markTrimmed(lm);
		smoothRec(lm, lm.getRoot(), 0);
	}
	
	private void smoothRec(NgramLM lm, NgramNode node, int lev) {
		if(!node.hasChildren())
			return;
		this.trimNode(node, lev, lm.getNgramCounts());
		int count = 0;
		for(NgramNode child : node)
			count += child.getCount();
		float myLog = (float) Math.log10(count);
		for(NgramNode child : node) {
			smoothRec(lm, child, lev+1);
			child.setScore( (float) (Math.log10(child.getCount())-myLog) );
			node.setBackoffScore( Float.NEGATIVE_INFINITY );
		}
	}

	@Override
	public String getAbbr() { return "ml"; }

	@Override
	public String getName() { return "Maximum Likelihood"; }

}
