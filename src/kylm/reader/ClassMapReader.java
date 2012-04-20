package kylm.reader;

import java.io.IOException;

import kylm.model.ClassMap;
import kylm.util.SymbolSet;

/**
 * A loader for a map file for class-based models
 * @author neubig
 *
 */
public interface ClassMapReader {
	
	ClassMap readClassMap(SymbolSet vocab, int fixed, boolean hasNames) throws IOException;

	
}
