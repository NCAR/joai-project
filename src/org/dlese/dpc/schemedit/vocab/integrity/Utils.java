/*
	Copyright 2017 Digital Learning Sciences (DLS) at the
	University Corporation for Atmospheric Research (UCAR),
	P.O. Box 3000, Boulder, CO 80307

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package org.dlese.dpc.schemedit.vocab.integrity;

import org.dlese.dpc.schemedit.vocab.*;



import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.schema.DefinitionMiner;
import org.dlese.dpc.xml.schema.StructureWalker;
import org.dlese.dpc.xml.schema.SchemaReader;
import org.dlese.dpc.xml.schema.SchemaHelperException;
import org.dlese.dpc.xml.schema.SchemaNodeMap;
import org.dlese.dpc.xml.schema.SchemaNode;
import org.dlese.dpc.xml.schema.GlobalDef;
import org.dlese.dpc.xml.schema.GenericType;

import org.dlese.dpc.xml.XPathUtils;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;
import java.lang.*;

import org.dom4j.*;


/**
 *  Command line routine that checks fields files for well-formedness, and ensures that the xpaths associated
 *  with the field files exist within the given metadata framework.
 *
 * @author     ostwald <p>
 *
 *      $Id: Utils.java,v 1.4 2009/03/20 23:33:58 jweather Exp $
 * @version    $Id: Utils.java,v 1.4 2009/03/20 23:33:58 jweather Exp $
 */
public class Utils {
	
	private static boolean debug = true;
	/* private HashMap map = null; */
	
	
	public static SchemaHelper getSchemaHelper (URI schemaUri) throws Exception {
		SchemaHelper schemaHelper = null;
		String scheme = schemaUri.getScheme();
		try {
			if (scheme.equals("file"))
				schemaHelper = new SchemaHelper(new File(schemaUri.getPath()));
			else if (scheme.equals("http"))
				schemaHelper = new SchemaHelper(schemaUri.toURL());
			else
				throw new Exception("ERROR: Unrecognized scheme (" + scheme + ")");

			if (schemaHelper == null)
				throw new Exception();
		} catch (Exception e) {
			if (e.getMessage() != null && e.getMessage().trim().length() > 0)
				throw new Exception("Unable to instantiate SchemaHelper at " + schemaUri + ": " + e.getMessage());
			else
				throw new Exception("Unable to instantiate SchemaHelper at " + schemaUri);
		}
		return schemaHelper;
	}
	

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	public static void prtln(String s) {
		if (debug)
			System.out.println(s);
	}
	
	public static String getTimeStamp () {
		return new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}
	
	public static int DEFAULT_LINE_WIDTH = 72;
	
	public static String DEFAULT_LINE_CHAR = "-";
	
	public static String NL = "\n";
	
	public static String TAB = "\t";
	
	public static String line () {
		return line (DEFAULT_LINE_WIDTH);
	}
	
	public static String line (String ch) {
		return line (DEFAULT_LINE_WIDTH, ch);
	}
	
	public static String line (int width) {
		return line (width, DEFAULT_LINE_CHAR);
	}
	
	public static String line (int width, String ch) {
		String s = "";
		for (int i=0;i < (width / ch.length()); i++)
			s += ch;
		return s;
	}
	
	public static String underline (String s) {
		String xx = expandTabs (s);
		return xx + "\n" + line(s.length());
	}
	
	public static String overline (String s) {
		return line(s.length()) + "\n" + s;
	}
	
	public static String expandTabs (String s) {
		String xx = "";
		String tab = "....";
		for (int i=0;i<s.length();i++) {
			char ch = s.charAt(i);
			if (ch == '\t')
				xx += tab;
			else 
				xx += String.valueOf(ch);
		}
		return xx;
	}
		
	
	private static int getMaxLen (String [] args) {
		int max = 0;
		for (int i=0;i<args.length;i++)
			max = java.lang.Math.max (max, expandTabs (args[i]).length());
		return max;
	}
	
	public static String pad (String s, int len) {
		String padded = s;
		for (int i=s.length();i<len;i++)
			padded += " ";
		return padded;
	}
	
	public static void main (String [] args) {
		prtln (box ("asdfsadf\n\tthis is much longer\nhava a good day sir!"));
	}
	
	public static String box (String s, String line_ch) {
		// String out = line (s.length() + 4, line_ch) + NL;
		String [] splits = s.split("\n");
		int width = getMaxLen (splits);
		String hr = line (width + 4, line_ch) + NL;
		
		String out = hr;
		for (int i=0;i<splits.length;i++) {
			if (splits[i].length() > 0) 
				out += "| " + pad (expandTabs (splits[i]), width) + " |" + NL;
		}
		/* out += "| " + s + " |" + NL; */
		return out + hr;
	}
	
	public static String box (String s) {
		return box (s, DEFAULT_LINE_CHAR);
	}
		
	public static String comment (String s) {
		return ". . . " + s + " . . .";
	}
}
	
