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

package kylm.util;

import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * A class to handle configuration of executable files
 * @author neubig
 * TODO: Add the ability to read configuration files
 */
public class KylmConfigUtils {
	public final static int STRING_TYPE = 0;
	public final static int INT_TYPE = 1;
	public final static int FLOAT_TYPE = 2;
	public final static int BOOLEAN_TYPE = 3;
	public static final int STRING_ARRAY_TYPE = 4;
	public static final int INT_ARRAY_TYPE = 5;
	
	Pattern colon = Pattern.compile(":");
	
	// a private class to hold a single configuration entry
	private class ConfigEntry {
		public int type;
		public Object value;
		public String arg;
		public String desc;
		public boolean req;
		public ConfigEntry(String arg, int type, Object value, boolean req, String desc) {
			this.arg = arg;
			this.type = type;
			this.value = value; 
			this.req = req;
			this.desc = desc;
		}
	}
	
	// the variables that hold the configuration
	private HashMap<Integer, String> groups = null;
	private HashMap<String, Integer> idx = null;
	private Vector<ConfigEntry> entries = null;
	private Vector<String> arguments = null;
	private String usageText = null;
	
	/**
	 * Initialize the class with no usage data to print
	 */
	public KylmConfigUtils() {
		init();
	}
	
	/**
	 * Initialize the configuration utilities with usage text to print in case
	 * @param usage The usage text
	 */
	public KylmConfigUtils(String usage) {
		usageText = usage;
		init();
	}
	
	private void init() {
		idx = new HashMap<String, Integer>();
		groups = new HashMap<Integer, String>();
		entries = new Vector<ConfigEntry>();
	}
	
	/**
	 * Add an entry to the configuration possibilities
	 * @param name The name of the entry to be added
	 * @param type The type, string, float, integer, boolean
	 * @param defaultVal The default value
	 * @param req Whether this is required or not
	 * @param desc The description of the parameter
	 */
	public void addEntry(String name, int type, Object defaultVal, boolean req, String desc) {
		if(idx.containsKey(name))
			throw new IllegalArgumentException(name+" float-added to ConfigUtils");
		idx.put(name, entries.size());
		entries.add(new ConfigEntry(name, type, defaultVal, req, desc));
	}

	
	/**
	 * Add an entry to the configuration possibilities
	 * @param name The name of the entry to be added
	 * @param type The type, string, float, integer, boolean
	 * @param defaultVal The default value
	 */
	public void addEntry(String name, int type, Object defaultVal) {
		addEntry(name, type, defaultVal, false, null);
	}

	/**
	 * Add a boolean entry to the configuration possibilities
	 * @param string The name of the entry to be added
	 */
	public void addEntry(String string) {
		addEntry(string, BOOLEAN_TYPE, false, false, null);
	}
	
	/**
	 * Add an alias that points from the long name of a parameter
	 *  to the short name. (e.g. -classpath = -cp)
	 * @param name The long name of the parameter
	 * @param alias The short name
	 */
	public void addAlias(String name, String alias) {
		if(idx.containsKey(alias))
			throw new IllegalArgumentException(alias+" double-added to ConfigUtils");
		if(!idx.containsKey(name))
			throw new IllegalArgumentException("alias "+alias+" assigned to non-existant"+name+" in ConfigUtils");
		idx.put(alias, idx.get(name));
	}
	
	/**
	 * Parse command line arguments
	 * @param args The command line arguments to parse
	 */
	public String[] parseArguments(String[] args) {
		arguments = new Vector<String>();
		for(int i = 0; i < args.length; i++) {
			// if this is an argument indicator
			if(args[i].charAt(0) == '-') {
				Integer id = idx.get(args[i].substring(1));
				if(id == null) {
					if(!args[i].equals("-help")) {
						System.err.println("Unknown argument "+args[i]);
						exitOnUsage(1);
					} else 
						exitOnUsage(0);
				}
				// appropriately parse the argument
				switch(entries.get(id).type) {
				case STRING_TYPE:
					entries.get(id).value = args[++i];
					break;
				case FLOAT_TYPE:
					entries.get(id).value = Float.valueOf(args[++i]);
					break;
				case INT_TYPE:
					entries.get(id).value =  Integer.valueOf(args[++i]);
					break;
				case BOOLEAN_TYPE:
					entries.get(id).value = true;
					break;
				case STRING_ARRAY_TYPE:
					String[] oldVal = getStringArray(args[i].substring(1));
					String[] newVal = new String[(oldVal == null?1:oldVal.length+1)];
					if(oldVal != null) System.arraycopy(oldVal, 0, newVal, 0, oldVal.length);
					newVal[newVal.length-1] = args[++i];
					entries.get(id).value = newVal;
					break;
				case INT_ARRAY_TYPE:
					int[] oldInts = getIntArray(args[i].substring(1));
					String s = args[++i];
					String[] sarr = s.split(":");
					int[] newInts = new int[(oldInts == null?sarr.length:oldInts.length+sarr.length)];
					if(oldInts != null) System.arraycopy(oldInts, 0, newInts, 0, oldInts.length);
					int start = (oldInts == null?0:oldInts.length);
					for(int j = 0; j < sarr.length; j++)
						newInts[start++] = Integer.parseInt(sarr[j]);
					entries.get(id).value = newInts;
					break;
				default:
					throw new IllegalArgumentException("Illegal configuration type for "+args[i]+": "+entries.get(id).type);
				}
			}
			else
				arguments.add(args[i]);
		}
		return arguments.toArray(new String[arguments.size()]);
	}

	/**
	 * Get the value assigned to a certain string ID
	 * @param str The string Id of the value
	 * @return The value
	 */
	public Object getValue(String str) {
		Integer id = idx.get(str);
		if(id == null)
			throw new IllegalArgumentException("Attempt to get the value for non-existant "+str);
		return entries.get(id).value;
	}

	/**
	 * Get the usage text
	 * @return The usage text
	 */
	public String getUsage() {
		return usageText;
	}

	/**
	 * Set the usage text
	 * @param usageText The usage text to be printed on -help
	 */
	public void setUsage(String usageText) {
		this.usageText = usageText;
	}

	/**
	 * Get the arguments that are not correlated with a flag
	 * @return The arguments
	 */
	public String[] getArguments() {
		return arguments.toArray(new String[0]);
	}

	public boolean getBoolean(String string) {
		return (Boolean)getValue(string);
	}
	
	public String getString(String string) {
		return (String)getValue(string);
	}
	
	public String[] getStringArray(String string) {
		return (String[])getValue(string);
	}
	
	public int getInt(String string) {
		return (Integer)getValue(string);
	}
	
	public float getFloat(String string) {
		return (Float)getValue(string);
	}

	/**
	 * Exit, printing the usage text. Default error code is 1.
	 */
	public void exitOnUsage() {
		exitOnUsage(1);
	}
	/**
	 * Exit, printing the usage text
	 * @param code The error code to exit with
	 */
//	private Vector<Integer> types = null;
//	private Vector<Object> values = null;
//	private Vector<String> arguments = null;
//	private Vector<String> descs = null;
	public void exitOnUsage(int code) {
		System.err.println(usageText);
		int maxlen = 0;
		for(ConfigEntry entry : entries)
			maxlen = Math.max(maxlen, entry.arg.length());
		for(int i = 0; i < entries.size(); i++) {
			if(groups.containsKey(i))
				System.err.println(groups.get(i));
			ConfigEntry entry = entries.get(i);
			System.err.print("    -"+entry.arg+":");
			for(int j = entry.arg.length(); j <= maxlen; j++)
				System.err.print(" ");
			System.err.println(entry.desc+
					(entry.value!=null && entry.type != BOOLEAN_TYPE?" [default: "+entry.value+"]":"")+
					(entry.req?" (required)":""));
		}
		System.exit(code);
	}

	public int[] getIntArray(String string) {
		return (int[])getValue(string);
	}

	public void addGroup(String string) {
		groups.put(entries.size(), string);
	}
	
}
