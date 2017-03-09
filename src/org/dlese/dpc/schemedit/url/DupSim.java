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
package org.dlese.dpc.schemedit.url;

import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.serviceclients.remotesearch.reader.ADNItemDocReader;
import java.util.Comparator;

/**
 *  Class to hold results of searches for duplicate or similar URLs. Associates
 *  a particular url with the record id and whether it represents a duplicate or
 *  similar url, as well as whether the url is from the PrimaryUrl or MirrorUrl
 *  fields.
 *
 * @author    ostwald<p>
 *
 *      $Id $
 */
public class DupSim {
	private boolean debug = true;
	private ADNItemDocReader reader = null;
	private String id = "";
	private String url = null;
	private String type = "";
	// sim | dup
	private String field = "";
	// xpath or maybe just mirror | primary
	private String xmlFormat = "";


	/**  Constructor for the DupSim object */
	public DupSim() { }


	/**
	 *  Constructor for the DupSim object given a ResultDoc instance
	 *
	 * @param  result     Description of the Parameter
	 * @param  field      Description of the Parameter
	 * @param  framework  NOT YET DOCUMENTED
	 */
/* 	public DupSim(ResultDoc result, String field, MetaDataFramework framework) {
		ResultDoc resultDoc = (ResultDoc) result;
		XMLDocReader docReader = (XMLDocReader) resultDoc.getDocReader();
		DocumentMap docMap = resultDoc.getDocMap();
		id = docReader.getId();

		xmlFormat = docReader.getNativeFormat();
		this.field = field;
		this.url = RepositoryService.getPrimaryUrl(resultDoc, framework);
	} */

	/**
	 *  Constructor for the DupSim object
	 *
	 * @param  id         Description of the Parameter
	 * @param  url        Description of the Parameter
	 * @param  type       Description of the Parameter
	 * @param  field      Description of the Parameter
	 * @param  xmlFormat  Description of the Parameter
	 */
	public DupSim(String id, String url, String type, String field, String xmlFormat) {
		this.id = id;
		this.url = url;
		this.type = type;
		this.field = field;
		this.xmlFormat = xmlFormat;
	}
	
	/**
	 *  Constructor for the DupSim object
	 *
	 * @param  reader  NOT YET DOCUMENTED
	 * @param  field   NOT YET DOCUMENTED
	 */
	public DupSim(ADNItemDocReader reader, String field) {
		id = reader.getId();
		xmlFormat = "adn";
		this.field = field;
		this.url = reader.getUrl();
	}

	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		String s = "DupSim: " + getId();
		s += "\n\t url: " + getUrl();
		s += "\n\t type: " + getType();
		s += "\n\t field: " + getField();
		s += "\n\t xmlFormat: " + getXmlFormat();
		return s;
	}


	/**
	 *  Gets the id attribute of the DupSim object
	 *
	 * @return    The id value
	 */
	public String getId() {
		return id;
	}


	/**
	 *  Gets the url attribute of the DupSim object
	 *
	 * @return    The url value
	 */
	public String getUrl() {
		return url;
	}


	/**
	 *  Gets the type attribute of the DupSim object
	 *
	 * @return    The type value
	 */
	public String getType() {
		return type;
	}


	/**
	 *  Gets the field attribute of the DupSim object
	 *
	 * @return    The field value
	 */
	public String getField() {
		return field;
	}


	/**
	 *  Gets the xmlFormat attribute of the DupSim object
	 *
	 * @return    The xmlFormat value
	 */
	public String getXmlFormat() {
		return xmlFormat;
	}


	/**
	 *  Gets the comparator attribute of the DupSim object
	 *
	 * @return    The comparator value
	 */
	public DupSimComparator getComparator() {
		return new DupSimComparator();
	}


	/**
	 *  Description of the Class
	 *
	 * @author    ostwald
	 */
	public class DupSimComparator implements Comparator {

		/**
		 *  Description of the Method
		 *
		 * @param  o1  Description of the Parameter
		 * @param  o2  Description of the Parameter
		 * @return     Description of the Return Value
		 */
		public int compare(Object o1, Object o2) {
			String string1 = ((DupSim) o1).getId().toLowerCase();
			String string2 = ((DupSim) o2).getId().toLowerCase();
			return string2.compareTo(string1);
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	protected void prtln(String s) {
		if (debug) {
			System.out.println("SchemEditForm: " + s);
		}
	}
}

