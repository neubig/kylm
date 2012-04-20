package kylm.reader;


import java.io.*;

import kylm.model.ClassMap;
import kylm.util.SymbolSet;

public class TextFileClassMapReader extends TextStreamClassMapReader {

	protected File file = null;

	/**
	 * The constructor, saves the file and uses a single space as the default divider 
	 * @param fileName The name of the file to be opened
	 * @throws IOException if the file doesn't exist or is unreadable
	 */
	public TextFileClassMapReader(String fileName) throws IOException {
		super();
		file = new File(fileName);
		if(!file.canRead())
			throw new IOException("File "+fileName+" does not exist or is unreadable");
	}

	/**
	 * The constructor, saves the file name and uses the passed in divider 
	 * @param fileName The name of the file to be opened
	 * @param divider A regular expression that is used to divide strings in the corpus
	 * @throws IOException if the file doesn't exist or is unreadable
	 */
	public TextFileClassMapReader(String fileName, String divider) throws IOException {
		file = new File(fileName);
		if(!file.canRead())
			throw new IOException("File "+fileName+" does not exist or is unreadable");
	}

	@Override
	public ClassMap readClassMap(SymbolSet ss, int fixed, boolean hasNames) throws IOException {
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new IOException(e);
		}
		ClassMap ret = super.readClassMap(ss,fixed, hasNames);
		reader.close();
		return ret;

	}


}
