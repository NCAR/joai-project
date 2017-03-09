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
package org.dlese.dpc.services.mmd;

import java.util.Date;

/**
 *  Describes a single metadata record.
 *
 */

public class MmdRec implements Comparable {

	/**  Record status: unknown (error) */
	public final static int STATUS_UNKNOWN = 0;
	/**  Record status: accessioned and discoverable */
	public final static int STATUS_ACCESSIONED_DISCOVERABLE = 1;
	/**  Record status: accessioned and not discoverable */
	public final static int STATUS_ACCESSIONED_NONDISCOVERABLE = 2;
	/**  Record status: deaccessioned */
	public final static int STATUS_DEACCESSIONED = 3;
	/**  Record status: new */
	public final static int STATUS_NEW = 4;
	/**  Record status: rejected */
	public final static int STATUS_REJECTED = 5;

	public final static String[] statusNames = {
		"unknown",
		"accessioneddiscoverable",
		"accessionednondiscoverable",
		"deaccessioned",
		"new",
		"rejected"};

	/**  Metadata style: unknown (error) */
	public final static int MS_UNKNOWN = 0;
	/**  Metadata style: adn */
	public final static int MS_ADN = 1;
	/**  Metadata style: briefmeta */
	public final static int MS_BRIEFMETA = 2;
	/**  Metadata style: dc_qual */
	public final static int MS_DC_QUAL = 3;
	/**  Metadata style: dc_simple */
	public final static int MS_DC_SIMPLE = 4;
	/**  Metadata style: dlese_anno */
	public final static int MS_DLESE_ANNO = 5;
	/**  Metadata style: dlese_collect */
	public final static int MS_DLESE_COLLECT = 6;
	/**  Metadata style: dlese_ims */
	public final static int MS_DLESE_IMS = 7;
	/**  Metadata style: nsdl_dc */
	public final static int MS_NSDL_DC = 8;
	/**  Metadata style: news_opps */
	public final static int MS_NEWS_OPPS = 9;
	/**  Metadata style: oai_dc */
	public final static int MS_OAI_DC = 10;


	public final static String[] metastyleNames = {
		"unknown",
		"adn",
		"briefmeta",
		"dc_qual",
		"dc_simple",
		"dlese_anno",
		"dlese_collect",
		"dlese_ims",
		"nsdl_dc",
		"news_opps",
		"oai_dc"};


	private String collKey = null;
	private String id = null;
	private String fileName = null;
	private int status = 0;
	private int metastyle = 0;
	private long firstAccessionDate;
	private long lastMetaModDate;
	private long recCheckDate;
	private MmdWarning[] warnings;
	private String primarycontentType = null;
	private String primaryContent = null;


	/**
	 *  Creates new MmdRec with the specified values.
	 *
	 * @param  collKey             DESCRIPTION
	 * @param  id                  DESCRIPTION
	 * @param  fileName            DESCRIPTION
	 * @param  statusStg           DESCRIPTION
	 * @param  metastyleStg        DESCRIPTION
	 * @param  firstAccessionDate  DESCRIPTION
	 * @param  lastMetaModDate     DESCRIPTION
	 * @param  recCheckDate        DESCRIPTION
	 * @param  primaryContent      DESCRIPTION
	 * @param  primarycontentType  DESCRIPTION
	 * @param  warnings            DESCRIPTION
	 * @exception  MmdException    DESCRIPTION
	 */

	public MmdRec(
	              String collKey,
	              String id,
	              String fileName,
	              String statusStg,
	              String metastyleStg,
	              long firstAccessionDate,
	              long lastMetaModDate,
	              long recCheckDate,
	              String primaryContent,
	              String primarycontentType,
	              MmdWarning[] warnings)
		 throws MmdException {
		int ii;
		boolean foundit;

		this.collKey = collKey;
		this.id = id;
		this.fileName = fileName;

		status = STATUS_UNKNOWN;
		for (ii = 0; ii < statusNames.length; ii++) {
			if (statusNames[ii].equals(statusStg)) {
				status = ii;
				break;
			}
		}
		if (status == STATUS_UNKNOWN)
			mkerror("MmdRec.const: invalid status: \"" + statusStg + "\"");

		metastyle = MS_UNKNOWN;
		for (ii = 0; ii < metastyleNames.length; ii++) {
			if (metastyleNames[ii].equals(metastyleStg)) {
				metastyle = ii;
				break;
			}
		}
		if (metastyle == MS_UNKNOWN)
			mkerror("MmdRec.const: invalid metastyle: \""
				 + metastyleStg + "\"");

		this.firstAccessionDate = firstAccessionDate;
		this.lastMetaModDate = lastMetaModDate;
		this.recCheckDate = recCheckDate;
		this.primaryContent = primaryContent;
		this.primarycontentType = primarycontentType;
		this.warnings = warnings;
	}



	public String toString() {
		int ii;

		String res = "    collKey: " + collKey + "\n    id: " + id + "\n";
		res += "    fileName: \"" + fileName + "\"\n";
		res += "    status: " + status + "\n";
		res += "    metastyle: " + metastyle + "\n";
		res += "    firstAccessionDate: " + new Date(firstAccessionDate) + "\n";
		res += "    lastMetaModDate: " + new Date(lastMetaModDate) + "\n";
		res += "    recCheckDate: " + new Date(recCheckDate) + "\n";
		//res += "    primaryContent: " + getPrimaryContent() + "\n\n\n\n";
		res += "    primarycontentType: " + primarycontentType + "\n";
		if (warnings == null)
			res += "    warnings: null\n";
		else {
			res += "    warnings:\n";
			for (ii = 0; ii < warnings.length; ii++) {
				res += "    warning " + ii + ": " + warnings[ii] + "\n";
			}
		}
		return res;
	}


	/**
	 *  Gets the primaryContent attribute of the MmdRec object
	 * and returns it after cleaning up the illegal character sequences "0x000d" and "0x0009"
	 * @return    The primaryContent value
	 */
	public String getPrimaryContent() {

		String cleaned = null;
		String cleaned2 = null;
		if (primaryContent != null) {
			// remove the hex illegal characters in the string
			cleaned = primaryContent.replaceAll("0x000d", "");
			cleaned2 = cleaned.replaceAll("0x0009", "");
		}

		return cleaned2;
	}


	/**
	 *  Gets the primaryContentType attribute of the MmdRec object
	 *
	 * @return    The primaryContentType value
	 */
	public String getPrimaryContentType() {
		return primarycontentType;
	}



	/**
	 *  Returns the collection key.
	 *
	 * @return    The collKey value
	 */
	public String getCollKey() {
		return collKey;
	}


	/**
	 *  Returns the record id.
	 *
	 * @return    The id value
	 */
	public String getId() {
		return id;
	}


	/**
	 *  Returns the file name of the record's XML file.
	 *
	 * @return    The fileName value
	 */
	public String getFileName() {
		return fileName;
	}


	/**
	 *  Returns the record status: for example, STATUS_ACCESSIONED_DISCOVERABLE. Must be one
	 *  of the STATUS_ values above.
	 *
	 * @return    The status value
	 */
	public int getStatus() {
		return status;
	}


	/**
	 *  Returns a string representing the record status, for example,
	 *  "accessioneddiscoverable".
	 *
	 * @return    The statusString value
	 */
	public String getStatusString() {
		return statusNames[status];
	}


	/**
	 *  Checks that the specified nm is valid; if not throws MmdException.
	 *
	 * @param  nm                DESCRIPTION
	 * @exception  MmdException  DESCRIPTION
	 */

	public static void checkStatusString(String nm)
		 throws MmdException {
		int ii;
		if (nm == null)
			mkerror("checkStatusName: invalid status: null");
		boolean foundit = false;
		for (ii = 0; ii < statusNames.length; ii++) {
			if (nm.equals(statusNames[ii])) {
				foundit = true;
				break;
			}
		}
		if (!foundit)
			mkerror("checkStatusName: invalid status: \"" + nm + "\"");
	}



	/**
	 *  Returns the metadata format: for example, MS_ADN. Must be one of the MS_ values
	 *  above.
	 *
	 * @return    The metastyle value
	 */
	public int getMetastyle() {
		return metastyle;
	}


	/**
	 *  Returns a string representing the metadata style, for example, "adn".
	 *
	 * @return    The metastyleString value
	 */
	public String getMetastyleString() {
		return metastyleNames[metastyle];
	}



	/**
	 *  Returns the date this record was first accessioned.
	 *
	 * @return    The firstAccessionDate value
	 */

	public long getFirstAccessionDate() {
		return firstAccessionDate;
	}


	/**
	 *  Returns the date this record was last changed.
	 *
	 * @return    The lastMetaModDate value
	 */

	public long getLastMetaModDate() {
		return lastMetaModDate;
	}


	/**
	 *  Returns the date this record was last checked by the idmapper.
	 *
	 * @return    The recCheckDate value
	 */

	public long getRecCheckDate() {
		return recCheckDate;
	}


	/**
	 *  Returns an array of MmdWarning for messages generated during the most recent Idmapper
	 *  run.
	 *
	 * @return    The warnings value
	 */
	public MmdWarning[] getWarnings() {
		return warnings;
	}



	/**
	 *  Define equality for HashMap
	 *
	 * @param  obj  DESCRIPTION
	 * @return      DESCRIPTION
	 */
	public boolean equals(Object obj) {
		boolean bres = false;
		if (obj != null && obj instanceof MmdRec) {
			MmdRec mrec = (MmdRec) obj;
			if (collKey.equals(mrec.collKey) && id.equals(mrec.id))
				bres = true;
		}
		return bres;
	}


	/**
	 *  Implement Comparable interface
	 *
	 * @param  obj  DESCRIPTION
	 * @return      DESCRIPTION
	 */
	public int compareTo(Object obj) {
		int ires = 0;
		if (obj != null && obj instanceof MmdRec) {
			MmdRec mrec = (MmdRec) obj;
			ires = collKey.compareTo(mrec.collKey);
			if (ires == 0)
				ires = id.compareTo(mrec.id);
		}
		return ires;
	}





	/**
	 *  Simply throws an MmdException.
	 *
	 * @param  msg               DESCRIPTION
	 * @exception  MmdException  DESCRIPTION
	 */

	static void mkerror(String msg)
		 throws MmdException {
		throw new MmdException(msg);
	}



	/**
	 *  Prints a String without a trailing newline.
	 *
	 */

	static void prtstg(String msg) {
		System.out.print(msg);
	}



	/**
	 *  Prints a String with a trailing newline.
	 *
	 */

	static void prtln(String msg) {
		System.out.println(msg);
	}


}// end class MmdRec


