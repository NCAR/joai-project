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
package org.dlese.dpc.serviceclients.cat;

import org.dlese.dpc.schemedit.SchemEditUtils;

import java.util.*;
import java.net.URLEncoder;

/**
 *  Data Structure to manage information passed to the CATRest Service to
 *  constrain it's search.
 *
 * @author    ostwald
 */
public class CATRequestConstraints {
	private static boolean debug = true;

	public static final Integer ANY_GRADE = -1;

	private String id = null;
	private String author = null;
	private String topic = null;
	private int startGrade = -1;
	private int endGrade = -1;
	private String keywords = null;
	private int maxResults = 10;
	private String query = null;
	private String identifier = null;
	private List feedbackStandards = null;
	private List standardDocuments = null;


	/**  Constructor for the CATRequestConstraints object  */
	public CATRequestConstraints() { }


	/**
	 *  Gets the query attribute of the CATRequestConstraints object
	 *
	 * @return    The query value
	 */
	public String getQuery() {
		return this.query;
	}


	/**
	 *  Sets the id attribute of the CATRequestConstraints object
	 *
	 * @param  id  The new id value
	 */
	public void setId(String id) {
		this.id = id;
	}


	/**
	 *  Gets the id attribute of the CATRequestConstraints object
	 *
	 * @return    The id value
	 */
	public String getId() {
		return this.id;
	}


	/**
	 *  Sets the author attribute of the CATRequestConstraints object
	 *
	 * @param  author  The new author value
	 */
	public void setAuthor(String author) {
		this.author = author;
	}


	/**
	 *  Gets the author attribute of the CATRequestConstraints object
	 *
	 * @return    The author value
	 */
	public String getAuthor() {
		return this.author;
	}


	/**
	 *  Sets the topic attribute of the CATRequestConstraints object
	 *
	 * @param  topic  The new topic value
	 */
	public void setTopic(String topic) {
		this.topic = topic;
	}


	/**
	 *  Gets the topic attribute of the CATRequestConstraints object
	 *
	 * @return    The topic value
	 */
	public String getTopic() {
		return this.topic;
	}


	/**
	 *  Gets the standardDocuments attribute of the CATRequestConstraints object by creating
	 a comma-delimited string joining the items in the standardDocuments list
	 *
	 * @return    The standardDocuments value
	 */
	public String getStandardDocuments() {
		String sd = "";
		if (this.standardDocuments == null)
			return "";
		for (int i = 0; i < this.standardDocuments.size(); i++) {
			sd += (String) this.standardDocuments.get(i);
			if (i < this.standardDocuments.size() - 1)
				sd += ",";
		}
		return sd;
	}


	/**
	 *  Adds an item to the standardDocuments attribute of the CATRequestConstraints
	 *  object
	 *
	 * @param  docId  asn docId to be added to standardDocuments
	 */
	public void addStandardDocument(String docId) {
		if (this.standardDocuments == null)
			this.standardDocuments = new ArrayList();
		if (!this.standardDocuments.contains(docId))
			this.standardDocuments.add(docId);
	}


	/**
	 *  Sets the startGrade attribute of the CATRequestConstraints object
	 *
	 * @param  startGrade  The new startGrade value
	 */
	public void setStartGrade(int startGrade) {
		this.startGrade = startGrade;
	}


	/**
	 *  Gets the startGrade attribute of the CATRequestConstraints object
	 *
	 * @return    The startGrade value
	 */
	public int getStartGrade() {
		return this.startGrade;
	}


	/**
	 *  Sets the endGrade attribute of the CATRequestConstraints object
	 *
	 * @param  endGrade  The new endGrade value
	 */
	public void setEndGrade(int endGrade) {
		this.endGrade = endGrade;
	}


	/**
	 *  Gets the endGrade attribute of the CATRequestConstraints object
	 *
	 * @return    The endGrade value
	 */
	public int getEndGrade() {
		return this.endGrade;
	}


	/**
	 *  Sets the keywords attribute of the CATRequestConstraints object
	 *
	 * @param  keywords  The new keywords value
	 */
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}


	/**
	 *  Gets the keywords attribute of the CATRequestConstraints object
	 *
	 * @return    The keywords value
	 */
	public String getKeywords() {
		return this.keywords;
	}


	/**
	 *  Sets the query attribute of the CATRequestConstraints object
	 *
	 * @param  q  The new query value (used to specifiy resourceUrl)
	 */
	public void setQuery(String q) {
		this.query = q;
	}


	/**
	 *  Gets the maxResults attribute of the CATRequestConstraints object
	 *
	 * @return    The maxResults value
	 */
	public int getMaxResults() {
		return this.maxResults;
	}


	/**
	 *  Sets the maxResults attribute of the CATRequestConstraints object
	 *
	 * @param  max  The new maxResults value
	 */
	public void setMaxResults(int max) {
		this.maxResults = max;
	}


	/**
	 *  Gets the identifier attribute of the CATRequestConstraints object
	 *
	 * @return    The identifier value
	 */
	public String getIdentifier() {
		return this.identifier;
	}


	/**
	 *  Sets the identifier attribute of the CATRequestConstraints object
	 *
	 * @param  id  The new identifier value
	 */
	public void setIdentifier(String id) {
		this.identifier = id;
	}


	/**
	 *  Sets the feedbackStandards attribute of the CATRequestConstraints object
	 *
	 * @param  stds  The new feedbackStandards value
	 */
	public void setFeedbackStandards(List stds) {
		this.feedbackStandards = stds;
	}


	/**
	 *  Sets the feedbackStandards attribute of the CATRequestConstraints object
	 *
	 * @param  s  The new feedbackStandards value
	 */
	public void setFeedbackStandards(String s) {
		String[] splits = s.split(",");
		feedbackStandards = new ArrayList();
		for (int i = 0; i < splits.length; i++) {
			feedbackStandards.add(splits[i].trim());
		}
	}


	/**
	 *  Gets the feedbackStandards attribute of the CATRequestConstraints object
	 *
	 * @return    The feedbackStandards value
	 */
	public List getFeedbackStandards() {
		return this.feedbackStandards;
	}


	/**
	 *  Return Constraints as a map, keyed by parameter name
	 *
	 * @return    Contraints map
	 */
	public Map asMap() {
		Map map = new HashMap();

		map.put("query", this.query);

		if (hasValue(this.getAuthor()))
			map.put("author", this.getAuthor());
		if (hasValue(this.getTopic()))
			map.put("topic", this.getTopic());
		if (hasValue(this.getStandardDocuments()))
			map.put("standardDocuments", this.getStandardDocuments());
		if (hasValue(this.getKeywords()))
			map.put("keywords", this.getKeywords());

		int startGrade = this.getStartGrade();
		if (startGrade != -1)
			map.put("startGrade", String.valueOf(startGrade));

		int endGrade = this.getEndGrade();
		if (endGrade != -1)
			map.put("endGrade", String.valueOf(endGrade));
		map.put("maxResults", String.valueOf(this.maxResults));

		if (feedbackStandards != null && !feedbackStandards.isEmpty()) {
			String ids = "";
			for (Iterator i = feedbackStandards.iterator(); i.hasNext(); ) {
				ids += (String) i.next();
				if (i.hasNext())
					ids += ",";
			}
			map.put("selectedStandards", ids);
		}
		return map;
	}


	/**
	 *  Generate a queryString to be passed with CAT service request
	 *
	 * @return    a queryString
	 */
	public String toQueryString() {
		Map constraintsMap = this.asMap();
		String queryString = "";
		for (Iterator i = constraintsMap.keySet().iterator(); i.hasNext(); ) {
			String param = (String) i.next();
			String value = (String) constraintsMap.get(param);
			if (value == null || value.trim().length() == 0)
				continue;
			if (param.equals("selectedStandards")) {
				// prtln ("selectedStandards: " + value);
				String[] stds = value.split(",");
				String stdsValue = "";
				for (int ii = 0; ii < stds.length; ii++) {
					String std = stds[ii].trim();
					if (std.length() > 0)
						stdsValue += URLEncoder.encode(std);
					if (ii + 1 < stds.length)
						stdsValue += ",";
				}
				queryString += param + "=" + stdsValue;
			}
			else {
				queryString += param + "=" + URLEncoder.encode(value);
			}
			if (i.hasNext())
				queryString += "&";
		}
		return queryString;
	}


	/**
	 *  Return string representation of constraints map, used for debugging
	 *
	 * @return    string representation of this CATRequestConstraints
	 */
	public String toString() {
		String s = "CAT Request Constraints";
		Map args = this.asMap();
		for (Iterator i = args.keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			s += "\n\t" + key + ": " + (String) args.get(key);
		}
		return s;
	}


	/**
	 *  Returns true if provided string is non-null and is not empty string.
	 *
	 * @param  s  specified value
	 * @return    true if specified value actually has a value
	 */
	private boolean hasValue(String s) {
		return (s != null && s.trim().length() > 0);
	}



	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug)
			SchemEditUtils.prtln(s, "CATRequestConstraints");
	}
}

