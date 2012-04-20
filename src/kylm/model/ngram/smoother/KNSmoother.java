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
 * Calculate N-gram probabilities with Kneser-Ney smoothing
 * @author neubig
 */
public class KNSmoother extends AbsoluteSmoother {

	/**
	 * Serialization ID
	 */
	private static final long serialVersionUID = -17904685317993557L;

	@Override
	public void smooth(NgramLM lm) {
		
		if(debug > 0)
			System.err.println("KNSmoother: marking values to be trimmed");
		
		// mark the values to be trimmed If they haven't already been
		if(cutoffs == null)
			cutoffs = new int[lm.getN()];
		markTrimmed(lm);
		
		if(debug > 0)
			System.err.println("KNSmoother: adjusting counts");
		
		// adjust the probability of each n-gram based on how 
		//  many probabilities it occurs in
		NgramNode root = lm.getRoot();
		adjustCounts(root);
		root.setScore(0);

		super.smooth(lm);

	}

	// adjust the counts according to the Kneser-Ney criterion
	private void adjustCounts(NgramNode node) {
		if(node.getScore() == NgramNode.TRIM_SCORE)
			return;
		NgramNode myFallback = node.getFallback();
		if(myFallback != null) {
			// use the score being set to Double.MAX_VALUE as a sign that the count
			//  has already been reset. If it hasn't, reset to 0, then add one
			if(myFallback.getScore() != Float.MAX_VALUE) {
				myFallback.setScore( Float.MAX_VALUE );
				myFallback.setCount( 1 );
			}
			// otherwise, increment the count
			else
				myFallback.incrementCount();
		}
		// adjust for the children if they exist
		if(node.hasChildren())
			for(NgramNode child : node)
				adjustCounts(child);
	}
	
	public String getName() { return "Kneser-Ney"; }
	
	public String getAbbr() { return "kn"; }

}
