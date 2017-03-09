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
package org.dlese.dpc.schemedit.config;

import java.io.*;
import java.util.*;
import java.text.*;
import org.dlese.dpc.schemedit.SchemEditUtils;

/**
 *  Class repsonsible for generating unique IDs for a particular collection.<p>
 *
 *  IDGenerator instances are stashed in the {@link org.dlese.dpc.schemedit.ids.IDManager}.
 *
 * @author     ostwald <p>
 *
 *      $Id: IDGenerator.java,v 1.4 2009/03/20 23:33:56 jweather Exp $
 * @version    $Id: IDGenerator.java,v 1.4 2009/03/20 23:33:56 jweather Exp $
 */
public class IDGenerator {
	private static boolean debug = true;

	Config cfg = null;


	/**  Constructor for the IDGenerator object  */
	public IDGenerator() { }

	public IDGenerator (String idFileName, String idPrefix) {
		this (idFileName, idPrefix, new ArrayList());
	}
	
	/**
	 *  Constructor for the IDGenerator object
	 *
	 * @param  idFilename  NOT YET DOCUMENTED
	 * @param  idPrefix    NOT YET DOCUMENTED
	 * @param  idList      NOT YET DOCUMENTED
	 */
	public IDGenerator(String idFilename, String idPrefix, Collection idList) {
		this.cfg = new Config(idFilename, idPrefix);
		File idFile = new File(cfg.idFilename);

		if (!idFile.exists()) {
			// prtln ("idFile doesn't exist at " + cfg.idFilename);
			File idDir = new File(idFile.getParent());
			if (!idDir.exists()) {
				prtln("idFile directory doesn't exist: aborting");
				return;
			}
		}
		init(idList);
	}

		/**
	 *  Sets the count attribute of the IDGenerator object.
	 *
	 * @param  IDs  The new count value
	 */
	public synchronized void init(Collection IDs) {
		long max = 0;
		Iterator i = IDs.iterator();
		while (i.hasNext()) {
			long tstValue = parseID((String) i.next());
			// prtln ("testValue: " + tstValue);
			max = (tstValue > max) ? tstValue : max;
		}

		if (max > readID()) {
			// prtln ("bumping current ID to " + max);
			writeID(max);
		}
	}

	/**
	 *  Gets the idPrefix attribute of the IDGenerator object
	 *
	 * @return    The idPrefix value
	 */
	public String getIdPrefix() {
		return cfg.idPrefix;
	}

	public void setIdPrefix (String prefix) {
		// prtln ("setIdPrefix() to " + prefix);
		cfg.idPrefix = prefix;
	}

	/**
	 *  Gets the exampleID attribute of the IDGenerator object
	 *
	 * @return    The exampleID value
	 */
	public String getExampleID() {
		return getFirstID();
	}


	/**
	 *  Gets the firstID attribute of the IDGenerator object
	 *
	 * @return    The firstID value
	 */
	public String getFirstID() {
		return formatID(1, cfg.idPrefix, cfg.idSeparator, cfg.idNumberFormat);
	}


	/**
	 *  Gets next available number as string and updates id file
	 *
	 * @return    Description of the Return Value
	 */
	public synchronized String nextID() {
		// here we calculate and set the idFile, idPrefix, ... attributes
		// the attributes can be cleared before exiting.
		long id = readID() + 1;
		if (id > 0) {
			writeID(id);
			return formatID(id, cfg.idPrefix, cfg.idSeparator, cfg.idNumberFormat);
		}
		prtln("IDGenerator.nextID() returning an formatErrorID");
		return formatErrorID(cfg.idSeparator, cfg.idNumberFormat);
	}


	/**
	 *  Returns the last ID Generated
	 *
	 * @return    The lastID value
	 */
	public String getLastID() {
		long id = readID();
		return formatID(id, cfg.idPrefix, cfg.idSeparator, cfg.idNumberFormat);
	}


	/**
	 *  gets last used id number
	 *
	 * @return    Description of the Return Value
	 */
	synchronized long readID() {
		long id = -1;
		try {
			BufferedReader buf = new BufferedReader(new FileReader(new File(cfg.idFilename)));
			id = Long.parseLong(buf.readLine(), 16);
			buf.close();
		} catch (Exception e) {
			// prtln("Exception occurred reading ID from file, " + cfg.idFilename + " in IDGenerator.readID()");
			// prtln(e.getClass() + " with message: " + e.getMessage());
		}
		return id;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  val  Description of the Parameter
	 */
	private void writeID(long val) {
		try {
			String id = Long.toHexString(val);
			FileWriter buf = new FileWriter(new File(cfg.idFilename));
			buf.write(id, 0, id.length());
			buf.close();
		} catch (Exception e) {
			prtln("Exception occurred reading ID from file, " + cfg.idFilename + " in IDGenerator.readID()");
			prtln(e.getClass() + " with message: " + e.getMessage());
		}
	}



	/**
	 *  Description of the Method
	 *
	 * @param  id  Description of the Parameter
	 * @return     Description of the Return Value
	 */
	private long parseID(String id) {
		try {
			String numValue = id.substring(cfg.idPrefix.length() + 1, id.length());
			DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			symbols.setGroupingSeparator(cfg.idSeparator);
			DecimalFormat formatter = new DecimalFormat(cfg.idNumberFormat, symbols);
			long num = ((Long) formatter.parse(id, new ParsePosition(cfg.idPrefix.length() + 1))).longValue();
			return num;
		} catch (Exception e) {
			return -1;
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  sep     Description of the Parameter
	 * @param  numfmt  Description of the Parameter
	 * @return         Description of the Return Value
	 */
	private String formatErrorID(char sep, String numfmt) {
		StringBuffer id = new StringBuffer(30);
		id.append("ERROR").append(sep);
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setGroupingSeparator(sep);
		DecimalFormat formatter = new DecimalFormat(numfmt, symbols);
		id.append(formatter.format(new Date().getTime()));
		return id.toString();
	}


	/**
	 *  Description of the Method
	 *
	 * @param  value   Description of the Parameter
	 * @param  prefix  Description of the Parameter
	 * @param  sep     Description of the Parameter
	 * @param  numfmt  Description of the Parameter
	 * @return         Description of the Return Value
	 */
	private String formatID(long value, String prefix, char sep, String numfmt) {
		StringBuffer id = new StringBuffer(30);
		id.append(prefix).append(sep);
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setGroupingSeparator(sep);
		DecimalFormat formatter = new DecimalFormat(numfmt, symbols);
		id.append(formatter.format(value));
		return id.toString();
	}


	class Config {

		char idSeparator = '-';
		String idPrefix = "";
		String idFilename = "";
		String idNumberFormat = "000,000,000,000";
		String idFile = "";
		String collection = "";


		/**
		 *  Constructor for the Config object
		 *
		 * @param  idFilename  Description of the Parameter
		 * @param  idPrefix    Description of the Parameter
		 */
		public Config(String idFilename, String idPrefix) {
			this.idPrefix = idPrefix;
			this.idFilename = idFilename;
			File idFile = new File(idFilename);
			if (!idFile.exists()) {
				// prtln("idFilename (" + idFilename + ") does not point to an existing File .. creating");
				try {
					FileWriter buf = new FileWriter(new File(idFilename));
					buf.write("", 0, 0);
					buf.close();
				} catch (Exception e) {
					prtln("unable to create new idfild at " + idFilename);
				}
			}

			collection = idFile.getName();

		}
	}


	/**
	 *  The main program for the IDGenerator class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		String idFilesDir = "/Users/ostwald/Desktop/idfiles";
		String collection = "dwel";
		String prefix = "DWEL";
		// Config config = new Config(idFilesDir + "/" + collection, prefix);
		prtln("collection config created");
		IDGenerator idGen = new IDGenerator(idFilesDir + "/" + collection, prefix, new ArrayList());
		prtln("firstID: " + idGen.getFirstID());
		prtln("idGenerator created");
		String id = idGen.nextID();
		prtln("nextID: " + id);
	}

	public void report () {
		prtln("Current ID = " + String.valueOf(readID()) + " for " + cfg.idPrefix);
	}

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln (s, "IDGenerator");
		}
	}

}

