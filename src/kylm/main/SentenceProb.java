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

package kylm.main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import kylm.model.ngram.NgramLM;
import kylm.model.ngram.reader.ArpaNgramReader;
import kylm.model.ngram.reader.NgramReader;
import kylm.model.ngram.reader.SerializedNgramReader;
import kylm.reader.TextStreamSentenceReader;
import kylm.util.KylmConfigUtils;
import kylm.util.KylmTextUtils;

/**
 * A program to calculate the probability of sentences given in a text file.
 *
 * @author Xuchen Yao
 */
public class SentenceProb {

	/**
	 * @param args
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException {
		final String br = System.getProperty("line.separator");
		KylmConfigUtils config = new KylmConfigUtils(
				"SentenceProb"+br+
				"A program to calculate the probability of sentences given in a text file"+br+
		"Example: java -cp kylm.jar kylm.main.SentenceProb -arpa model1.arpa test.txt");

		// Input format options
		config.addEntry("arpa", KylmConfigUtils.STRING_TYPE, null, true, "model in arpa format");
		config.addEntry("bin", KylmConfigUtils.STRING_TYPE, null, false, "model in binary format");

		// parse the arguments
		args = config.parseArguments(args);
		if(args.length != 1)
			config.exitOnUsage();

		// read in the model
		System.err.println("Reading model");
		String lmFile = config.getString("arpa");
		NgramReader nr;
		if (lmFile==null) {
			lmFile = config.getString("bin");
			nr = new SerializedNgramReader();
		} else
			nr = new ArpaNgramReader();
		NgramLM lm = null;
		try { lm = nr.read(lmFile); } catch(IOException e) {
			System.err.println("Problem reading model from file "+lmFile+": "+e.getMessage());
			System.exit(1);
		}

		// get the input stream to load the input
		InputStream is = (args.length == 0?System.in:new FileInputStream(args[0]));
		TextStreamSentenceReader tssl = new TextStreamSentenceReader(is);

		for(String[] sent : tssl) {
			// looks like getSentenceIds() adds <s> and </s> automatically
			//sent = KylmTextUtils.addStartEnd(sent);
			float prob = lm.getSentenceProb(sent);
			System.out.println("Log likelihood of sentence \""+KylmTextUtils.join(" ",sent)+
					"\": "+prob+"("+prob/sent.length+")");
		}
	}


}
