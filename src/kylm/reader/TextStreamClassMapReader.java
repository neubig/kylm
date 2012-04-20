package kylm.reader;

import java.io.*;
import java.util.StringTokenizer;

import kylm.model.ClassMap;
import kylm.util.SymbolSet;

public class TextStreamClassMapReader implements ClassMapReader {

	protected BufferedReader reader = null;

	public TextStreamClassMapReader() {
	}
	public TextStreamClassMapReader(InputStream is) throws IOException {
		if(is != null)
			reader = new BufferedReader(new InputStreamReader(is));
	}
	public TextStreamClassMapReader(BufferedReader br) throws IOException {
		this.reader = br;
	}

	@Override
	public ClassMap readClassMap(SymbolSet ss, int fixed, boolean readNames) throws IOException {
		ClassMap ret = new ClassMap();
		// load the names if necessary
		if(readNames) {
			String r = reader.readLine();
			int numNames = Integer.parseInt(r);
			while(numNames-- != 0)
				ret.addClass(reader.readLine());
		}
		// if there are fixed values, write them
		int size = ss.getSize();
		for(int i = 0; i < Math.min(size,fixed); i++) {
			int c = ret.addClass(ss.getSymbol(i));
			if(c != i)
				throw new IllegalArgumentException("Fatal error in loadClassMap");
			ret.addEntry(i, i, 0);
		}
		// read the rest of the entries
		while(reader.ready()) {
			String line = reader.readLine();
			if(line.length() == 0)
				break;

			StringTokenizer st = new StringTokenizer(line," ");
			int c = ret.addClass(st.nextToken());
			int v = ss.addSymbol(st.nextToken());
			float prob = 0;
			if(st.hasMoreTokens())
				prob = (float) Float.parseFloat(st.nextToken());
			if(v == 0 && c == 1) v = 1; // handle end-of-sentence
			ret.addEntry(v, c, prob);
		}
		return ret;
	}


}
