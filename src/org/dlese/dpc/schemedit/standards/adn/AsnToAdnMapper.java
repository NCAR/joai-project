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
package org.dlese.dpc.schemedit.standards.adn;

import org.dom4j.*;
import org.dlese.dpc.schemedit.Constants;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.strings.FindAndReplace;

import java.util.*;
import java.io.File;

/**
 *  Provides services for mapping between different standards formats, namely ASN and ADN (dlese), as well as 
 convenience methods for translating between the different gradeRange representations. 
 *
 * @author     Jonathan Ostwald
 */
public class AsnToAdnMapper {

	private static boolean debug = true;
	private File source;
	private HashMap idMap;
	private HashMap adnTextMap;
	private HashMap gradeRangeMap;


	/**
	 *  Constructor for the AsnToAdnMapper object
	 *
	 * @param  path  path to xml file containing mappings from ansId to ansText and adnText
	 */
	public AsnToAdnMapper(String path) {

		Document doc = null;
		this.source = new File (path);
		try {
			doc = Dom4jUtils.getXmlDocument(source);
		} catch (Exception e) {
			prtln("Couldn't read mappings doc: " + e.getMessage());
			return;
		}

		// prtln (Dom4jUtils.prettyPrint (doc));
		gradeRangeMap = getGradeRangeMap();

		idMap = new HashMap();
		adnTextMap = new HashMap();
		List mappings = doc.selectNodes("/Adn-to-Asn-info/standard");
		prtln(mappings.size() + " items found");
		for (Iterator i = mappings.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			StandardMapping mapping = new StandardMapping(e);
			idMap.put(mapping.asnId, mapping);
			adnTextMap.put(mapping.adnText, mapping);
		}
		// prtln("idMap initialized with " + idMap.size() + " items");
		// prtln("adnTextMap initialized with " + adnTextMap.size() + " items");
		prtln("AsnToAdnMapper initialized mappings " + adnTextMap.size() + " items");
	}

	public File getSource () {
		return this.source;
	}

	/**
	 *  Given adnText, return asnId
	 *
	 * @param  adnText  NOT YET DOCUMENTED
	 * @return          The asnId value
	 */
	public String getAsfId(String adnText) {
		StandardMapping mapping = (StandardMapping) adnTextMap.get(adnText);
		if (mapping != null)
			return mapping.asnId;
		else
			return null;
	}


	/**
	 *  Gets the asnText corresponding to asnId
	 *
	 * @param  asnId  NOT YET DOCUMENTED
	 * @return        The asnText value
	 */
	public String getAsfText(String asnId) {
		StandardMapping mapping = (StandardMapping) idMap.get(asnId);
		if (mapping != null)
			return mapping.asnText;
		else
			return null;
	}


	/**
	 *  Gets the adnText corresponding to asnId
	 *
	 * @param  asnId  NOT YET DOCUMENTED
	 * @return        The adnText value
	 */
	public String getAdnText(String asnId) {
		StandardMapping mapping = (StandardMapping) idMap.get(asnId);
		if (mapping != null)
			return mapping.adnText;
		else
			return null;
	}


	/**
	 *  Converts a StandardsList into a list of suggestions represented as ADN standards
	 *
	 * @param  sl  NOT YET DOCUMENTED
	 * @return     NOT YET DOCUMENTED
	 */
/* 	public List toAdnStandardsList(StandardsList sl) {
		List suggestions = new ArrayList();
		int size = sl.getSize();
		for (int i = 0; i < size; i++) {
			StandardWrapper wrapper = sl.getStandardAt(i);
			String adnText = getAdnText(wrapper.getIdentifier());
			if (adnText != null)
				suggestions.add(adnText);
			else {
				prtln("WARNING: AsnToAdnMapper could not find ADN standard for ASF id: " + wrapper.getIdentifier());
			}
		}
		return suggestions;
	} */


	/**
	 *  Returns unordered set containing all registered asnIds;
	 *
	 * @return    The ids value
	 */
	public Collection getIds() {
		return idMap.keySet();
	}


	/**
	 *  The main program for the AsnToAdnMapper class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		// String path = "/devel/ostwald/projects/schemedit-project/web/WEB-INF/data/Adn-to-Asf-mappings.xml";
		String path = "C:/Documents and Settings/ostwald/devel/projects/dcs-project/web/WEB-INF/data/Adn-to-Asn-v1.2.5-info.xml";
		AsnToAdnMapper mapper = new AsnToAdnMapper(path);

		String id = Constants.ASN_PURL_BASE + "S1002B43";
		prtln("adnText for " + id + "\n" + mapper.getAdnText(id));
		String adnText = mapper.getAdnText(id);
		if (adnText != null)
			prtln("id back atcha: " + mapper.getAsfId(adnText));
	}


	/**
	 *  Returns unordered set of registered gradeRanges  represented as
	 controlled DLESE vocab (e.g., "DLESE:Primary elementary") corresponding to the grades represented by
	 ASF standards, namely, K-12.
	 *
	 * @return    The gradeRanges value
	 */
	public Collection getGradeRanges() {
		return gradeRangeMap.keySet();
	}


	/**
	 *  Gets the label (for UI purposes) of a gradeRange value. E.g., for the value of "DLESE:Primary elementary",
	 the label is "Primary elementary".
	 *
	 * @param  gradeRangeValue  NOT YET DOCUMENTED
	 * @return                  The gradeRangeLabel value
	 */
	public String getGradeRangeLabel(String gradeRangeValue) {
		GradeRangeHelper grh = (GradeRangeHelper) gradeRangeMap.get(gradeRangeValue);
		if (grh == null)
			return "";
		return grh.label;
	}


	/**
	 *  Gets the startGrade attribute of the AsnToAdnMapper object
	 *
	 * @param  gradeRange  NOT YET DOCUMENTED
	 * @return             The startGrade value
	 */
	public int getStartGrade(String gradeRange) {
		GradeRangeHelper grh = (GradeRangeHelper) gradeRangeMap.get(gradeRange);
		if (grh == null) {
			return Integer.MAX_VALUE;
		}
		// prtln ("getStartGrade for " + grh.label + ": " + grh.minGrade);
		return grh.minGrade;
	}


	/**
	 *  Gets the endGrade attribute of the AsnToAdnMapper object
	 *
	 * @param  gradeRange  NOT YET DOCUMENTED
	 * @return             The endGrade value
	 */
	public int getEndGrade(String gradeRange) {
		GradeRangeHelper grh = (GradeRangeHelper) gradeRangeMap.get(gradeRange);
		if (grh == null) {
			return -1;
		}
		// prtln ("getEndGrade for " + grh.label + ": " + grh.maxGrade);
		return grh.maxGrade;
	}


	/**
	 *  Gets the gradeRangeMap attribute of the AsnToAdnMapper object
	 *
	 * @return    The gradeRangeMap value
	 */
	private HashMap getGradeRangeMap() {
		HashMap map = new HashMap();
		GradeRangeHelper grh;
		grh = new GradeRangeHelper("Primary elementary", "DLESE:Primary elementary", 0, 2);
		map.put(grh.value, grh);
		grh = new GradeRangeHelper("Intermediate elementary", "DLESE:Intermediate elementary", 3, 5);
		map.put(grh.value, grh);
		grh = new GradeRangeHelper("Middle school", "DLESE:Middle school", 6, 8);
		map.put(grh.value, grh);
		grh = new GradeRangeHelper("High school", "DLESE:High school", 9, 12);
		map.put(grh.value, grh);
		return map;
	}


	class GradeRangeHelper {
		String label;
		String value;
		int maxGrade;
		int minGrade;


		/**
		 *  Constructor for the GradeRangeHelper object
		 *
		 * @param  label     NOT YET DOCUMENTED
		 * @param  value     NOT YET DOCUMENTED
		 * @param  minGrade  NOT YET DOCUMENTED
		 * @param  maxGrade  NOT YET DOCUMENTED
		 */
		GradeRangeHelper(String label, String value, int minGrade, int maxGrade) {
			this.label = label;
			this.value = value;
			this.minGrade = minGrade;
			this.maxGrade = maxGrade;
		}
	}


	class StandardMapping {
		String asnId;
		String asnText;
		String adnText;


		/**
		 *  Constructor for the StandardMapping object
		 *
		 * @param  e  NOT YET DOCUMENTED
		 */
		StandardMapping(Element e) {
			asnId = e.attributeValue("id");
			try {
				asnText = e.element("asnText").getText();
				adnText = e.element("adnText").getText();

			} catch (Throwable t) {
				prtln("StandardMapping init: " + t.getMessage());
				prtln(Dom4jUtils.prettyPrint(e));
			}
		}


		/**
		 *  Constructor for the StandardMapping object
		 *
		 * @param  id   NOT YET DOCUMENTED
		 * @param  asn  NOT YET DOCUMENTED
		 * @param  adn  NOT YET DOCUMENTED
		 */
		StandardMapping(String id, String asn, String adn) {
			asnId = id;
			asnText = asn;
			adnText = adn;
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("AsnToAdnMapper: " + s);
		}
	}
}


