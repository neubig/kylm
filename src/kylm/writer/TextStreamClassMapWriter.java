package kylm.writer;

import java.io.OutputStream;
import java.io.PrintStream;

import kylm.model.ClassMap;
import kylm.util.SymbolSet;

public class TextStreamClassMapWriter implements ClassMapWriter {

	OutputStream os = null;
	SymbolSet vocab = null;
	
	public TextStreamClassMapWriter(OutputStream os, SymbolSet ss) {
		this.os = os;
		vocab = ss;
	}
	
	@Override
	public void writeClassMap(ClassMap cm) {
		PrintStream ps = new PrintStream(os);
		String[] classes = cm.getClasses().getSymbols();
		ps.println(classes.length);
		for(String s : classes)
			ps.println(s);
		for(int i = 0; i < cm.getWordSize(); i++)
			ps.println(cm.getClassSymbol(cm.getWordClass(i))+" "+vocab.getSymbol(i)+" "+cm.getWordProb(i));
		ps.println();
	}

}
