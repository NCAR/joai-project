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
package org.dlese.dpc.xml.maps;

import java.io.*;
import java.lang.*;
import java.util.*;
import javax.servlet.http.*;

/**
 *  Provided here only to support the DLESE metadata object types required by
 *  the the DleseXMLReader class. This scaled-down version removes most of the
 *  editing capability and all the UI support required by the DLESE Catalog
 *  System implementation
 *
 * @author     Dave Deniman
 * @version    0.9b, 05/20/02
 */

public class DleseBean
{

	/**  Description of the Field */
	protected boolean isValid;

	/**  Description of the Field */
	protected StringBuffer ID;
	/**  Description of the Field */
	protected StringBuffer resourceURL;
	/**  Description of the Field */
	protected StringBuffer createdDate;
	/**  Description of the Field */
	protected StringBuffer accessionDate;

	/**  Description of the Field */
	protected StringBuffer title;
	/**  Description of the Field */
	protected StringBuffer description;
	/**  Description of the Field */
	protected StringBuffer cost;
	/**  Description of the Field */
	protected StringBuffer copyright;
	/**  Description of the Field */
	protected StringBuffer information;
	/**  Description of the Field */
	protected StringBuffer otherTechnicalInfo;

	/**  Description of the Field */
	protected ArrayList technicalReqs;
	/**  Description of the Field */
	public final static int MAX_TECHNICALREQS = 8;

	/**  Description of the Field */
	protected ArrayList resourceTypes;
	/**  Description of the Field */
	public final static int MAX_RESOURCETYPES = 8;

	/**  Description of the Field */
	protected ArrayList subjects;
	/**  Description of the Field */
	public final static int MAX_SUBJECTS = 8;

	/**  Description of the Field */
	protected ArrayList audiences;
	/**  Description of the Field */
	public final static int MAX_AUDIENCES = 8;

	/**  Description of the Field */
	protected ArrayList creators;
	/**  Description of the Field */
	public final static int MAX_CREATORS = 8;

	/**  Description of the Field */
	protected ArrayList catalogers;
	/**  Description of the Field */
	public final static int MAX_CATALOGERS = 8;

	/**  Description of the Field */
	protected ArrayList relations;
	/**  Description of the Field */
	public final static int MAX_RELATIONS = 100;

	/**  Description of the Field */
	protected ArrayList annotations;
	/**  Description of the Field */
	public final static int MAX_ANNOTATIONS = 100;

	/**  Description of the Field */
	protected ArrayList geoReferences;
	/**  Description of the Field */
	public final static int MAX_GEOREFERENCES = 100;

	/**  Description of the Field */
	protected ArrayList sciStandards;
	/**  Description of the Field */
	public final static int MAX_SCISTANDARDS = 100;

	/**  Description of the Field */
	protected ArrayList geoStandards;
	/**  Description of the Field */
	public final static int MAX_GEOSTANDARDS = 100;

	/**  Description of the Field */
	protected ArrayList keywords;
	/**  Description of the Field */
	public final static int MAX_KEYWORDS = 8;

	/**  Description of the Field */
	protected ArrayList urls;
	/**  Description of the Field */
	public final static int MAX_URLS = 7;


	/**  Constructor for the DleseBean object */
	public DleseBean() {
		isValid = false;
	}


	/**
	 *  Description of the Method
	 *
	 */
	public boolean init() {
		isValid = true;
		try {
			ID = new StringBuffer();
			resourceURL = new StringBuffer();
			createdDate = new StringBuffer();
			accessionDate = new StringBuffer();
			title = new StringBuffer();
			description = new StringBuffer();
			cost = new StringBuffer();
			copyright = new StringBuffer();
			information = new StringBuffer();
			otherTechnicalInfo = new StringBuffer();
			technicalReqs = new ArrayList();
			resourceTypes = new ArrayList();
			subjects = new ArrayList();
			audiences = new ArrayList();
			creators = new ArrayList();
			catalogers = new ArrayList();
			annotations = new ArrayList();
			relations = new ArrayList();
			geoReferences = new ArrayList();
			sciStandards = new ArrayList();
			geoStandards = new ArrayList();
			keywords = new ArrayList();
			urls = new ArrayList();
		} catch (Throwable t) {
			isValid = false;
		} 
		return isValid;
	}


	/**  Description of the Method */
	public void destroy() {
		ID = null;
		resourceURL = null;
		createdDate = null;
		accessionDate = null;
		title = null;
		description = null;
		cost = null;
		copyright = null;
		information = null;
		otherTechnicalInfo = null;
		technicalReqs = null;
		resourceTypes = null;
		subjects = null;
		audiences = null;
		creators = null;
		catalogers = null;
		annotations = null;
		relations = null;
		geoReferences = null;
		sciStandards = null;
		geoStandards = null;
		keywords = null;
		urls = null;
		try {
			finalize();
		} catch (Throwable t) {}
	}


	/**
	 *  Gets the iD attribute of the DleseBean object
	 *
	 * @return    The iD value
	 */
	public String getID() {
		return ID.toString();
	}


	/**
	 *  Sets the iD attribute of the DleseBean object
	 *
	 * @param  id  The new iD value
	 */
	public boolean setID(String id) {
		if (id != null) {
			ID.delete(0, ID.length());
			ID.append(id);
			return true;
		}
		return false;
	}


	/**
	 *  Gets the resourceURL attribute of the DleseBean object
	 *
	 * @return    The resourceURL value
	 */
	public String getResourceURL() {
		return resourceURL.toString();
	}


	/**
	 *  Sets the resourceURL attribute of the DleseBean object
	 *
	 * @param  url  The new resourceURL value
	 */
	public boolean setResourceURL(String url) {
		if (url != null) {
			resourceURL.delete(0, resourceURL.length());
			resourceURL.append(url);
			if (urls.size() == 0) {
				urls.add(resourceURL.toString());
			}
			else {
				urls.set(0, resourceURL.toString());
			}
			return true;
		}
		return false;
	}


	/**
	 *  Gets the uRLs attribute of the DleseBean object
	 *
	 * @return    The uRLs value
	 */
	public List getURLs() {
		return urls;
	}


	/**
	 *  Sets the uRLs attribute of the DleseBean object
	 *
	 * @param  URLs  The new uRLs value
	 */
	public void setURLs(List URLs) {
		urls.clear();
		if (URLs != null) {
			urls.addAll(URLs);
		}
	}


	/**
	 *  Gets the createdDate attribute of the DleseBean object
	 *
	 * @return    The createdDate value
	 */
	public String getCreatedDate() {
		return createdDate.toString();
	}


	/**
	 *  Sets the createdDate attribute of the DleseBean object
	 *
	 * @param  dateStr  The new createdDate value
	 */
	public boolean setCreatedDate(String dateStr) {
		if (dateStr != null) {
			createdDate.delete(0, createdDate.length());
			createdDate.append(dateStr);
			return true;
		}
		return false;
	}


	/**
	 *  Gets the accessionedDate attribute of the DleseBean object
	 *
	 * @return    The accessionedDate value
	 */
	public String getAccessionedDate() {
		return accessionDate.toString();
	}


	/**
	 *  Sets the accessionedDate attribute of the DleseBean object
	 *
	 * @param  dateStr  The new accessionedDate value
	 */
	public boolean setAccessionedDate(String dateStr) {
		if (dateStr != null) {
			accessionDate.delete(0, accessionDate.length());
			accessionDate.append(dateStr);
			return true;
		}
		return false;
	}


	/**
	 *  Gets the scienceStds attribute of the DleseBean object
	 *
	 * @return    The scienceStds value
	 */
	public List getScienceStds() {
		return sciStandards;
	}


	/**
	 *  Sets the scienceStds attribute of the DleseBean object
	 *
	 * @param  standards  The new scienceStds value
	 */
	public void setScienceStds(String[] standards) {
		sciStandards.clear();
		for (int i = 0; i < standards.length; i++) {
			sciStandards.add(standards[i]);
		}

	}


	/**
	 *  Sets the scienceStds attribute of the DleseBean object
	 *
	 * @param  standards  The new scienceStds value
	 */
	public void setScienceStds(List standards) {
		sciStandards.clear();
		for (int i = 0; i < standards.size(); i++) {
			sciStandards.add(standards.get(i));
		}

	}


	/**  Description of the Method */
	public void clearScienceStds() {
		sciStandards.clear();
	}


	/**
	 *  Gets the geographyStds attribute of the DleseBean object
	 *
	 * @return    The geographyStds value
	 */
	public List getGeographyStds() {
		if (geoStandards.size() == 1) {
			String str = ((String) geoStandards.get(0)).trim();
			if (str.length() == 0) {
				return new ArrayList();
			}
		}
		return geoStandards;
	}


	/**
	 *  Sets the geographyStds attribute of the DleseBean object
	 *
	 * @param  standards  The new geographyStds value
	 */
	public void setGeographyStds(String[] standards) {
		geoStandards.clear();
		for (int i = 0; i < standards.length; i++) {
			geoStandards.add(standards[i]);
		}

	}


	/**
	 *  Sets the geographyStds attribute of the DleseBean object
	 *
	 * @param  standards  The new geographyStds value
	 */
	public void setGeographyStds(List standards) {
		geoStandards.clear();
		for (int i = 0; i < standards.size(); i++) {
			geoStandards.add(standards.get(i));
		}

	}


	/**  Description of the Method */
	public void clearGeographyStds() {
		geoStandards.clear();
	}


	/**
	 *  Gets the keywords attribute of the DleseBean object
	 *
	 * @return    The keywords value
	 */
	public List getKeywords() {
		return keywords;
	}


	/**
	 *  Sets the keywords attribute of the DleseBean object
	 *
	 * @param  keyWords  The new keywords value
	 */
	public void setKeywords(String[] keyWords) {
		keywords.clear();
		for (int i = 0; i < keyWords.length; i++) {
			if (keyWords[i].length() > 0) {
				keywords.add(keyWords[i]);
			}
		}
	}


	/**
	 *  Sets the keywords attribute of the DleseBean object
	 *
	 * @param  keyWords  The new keywords value
	 */
	public void setKeywords(List keyWords) {
		keywords.clear();
		if (keyWords != null) {
			keywords.addAll(keyWords);
		}
	}


	/**
	 *  Gets the title attribute of the DleseBean object
	 *
	 * @return    The title value
	 */
	public String getTitle() {
		return title.toString();
	}


	/**
	 *  Sets the title attribute of the DleseBean object
	 *
	 * @param  value  The new title value
	 */
	public void setTitle(String value) {
		title.delete(0, title.length());
		if (!value.equals(" ")) {
			title.append(value);
		}
		//System.err.println("title has been set to: " + title.toString());
		//System.err.println(" for instance " + getID());
	}


	/**
	 *  Gets the description attribute of the DleseBean object
	 *
	 * @return    The description value
	 */
	public String getDescription() {
		return description.toString();
	}


	/**
	 *  Sets the description attribute of the DleseBean object
	 *
	 * @param  value  The new description value
	 */
	public void setDescription(String value) {
		description.delete(0, description.length());
		if (!value.equals(" ")) {
			description.append(value);
		}
		//System.err.println("description has been set to: \n" + description.toString());
	}


	/**
	 *  Gets the cost attribute of the DleseBean object
	 *
	 * @return    The cost value
	 */
	public String getCost() {
		return cost.toString();
	}


	/**
	 *  Sets the cost attribute of the DleseBean object
	 *
	 * @param  value  The new cost value
	 */
	public void setCost(String value) {
		cost.delete(0, cost.length());
		if (!value.equals(" ")) {
			cost.append(value);
		}
		//System.err.println("cost has been set to: " + cost.toString());
	}


	/**
	 *  Gets the copyright attribute of the DleseBean object
	 *
	 * @return    The copyright value
	 */
	public String getCopyright() {
		return copyright.toString();
	}


	/**
	 *  Sets the copyright attribute of the DleseBean object
	 *
	 * @param  value  The new copyright value
	 */
	public void setCopyright(String value) {
		copyright.delete(0, copyright.length());
		if (!value.equals(" ")) {
			copyright.append(value);
		}
		//System.err.println("copyright type has been set to: \n" + copyright.toString());
	}


	/**
	 *  Gets the information attribute of the DleseBean object
	 *
	 * @return    The information value
	 */
	public String getInformation() {
		return information.toString();
	}


	/**
	 *  Sets the information attribute of the DleseBean object
	 *
	 * @param  value  The new information value
	 */
	public void setInformation(String value) {
		information.delete(0, information.length());
		if (!value.equals(" ")) {
			information.append(value);
		}
		//System.err.println("information has been set to: \n" + information.toString());
	}


	/**
	 *  Gets the subjects attribute of the DleseBean object
	 *
	 * @return    The subjects value
	 */
	public List getSubjects() {
		return subjects;
	}


	/**
	 *  Sets the subjects attribute of the DleseBean object
	 *
	 * @param  list  The new subjects value
	 */
	public void setSubjects(List list) {
		subjects.clear();
		if (!list.isEmpty()) {
			String subject = (String) list.get(0);
			if (subject.length() <= 0) {
				list.remove(0);
			}
			subjects.addAll(list);
		}
	}


	/**
	 *  Gets the audiences attribute of the DleseBean object
	 *
	 * @return    The audiences value
	 */
	public List getAudiences() {
		return audiences;
	}


	/**
	 *  Sets the audiences attribute of the DleseBean object
	 *
	 * @param  list  The new audiences value
	 */
	public void setAudiences(List list) {
		audiences.clear();
		if (!list.isEmpty()) {
			String audience = (String) list.get(0);
			if (audience.length() <= 0) {
				list.remove(0);
			}
			audiences.addAll(list);
		}
	}


	/**
	 *  Gets the technicalReqs attribute of the DleseBean object
	 *
	 * @return    The technicalReqs value
	 */
	public List getTechnicalReqs() {
		return technicalReqs;
	}


	/**
	 *  Sets the technicalReqs attribute of the DleseBean object
	 *
	 * @param  list  The new technicalReqs value
	 */
	public void setTechnicalReqs(List list) {
		technicalReqs.clear();
		if (!list.isEmpty()) {
			TechnicalReq technicalReq = (TechnicalReq) list.get(0);
			if (technicalReq.isEmpty()) {
				list.remove(0);
			}
			technicalReqs.addAll(list);
		}
	}


	/**
	 *  Gets the otherTechnicalInfo attribute of the DleseBean object
	 *
	 * @return    The otherTechnicalInfo value
	 */
	public String getOtherTechnicalInfo() {
		return otherTechnicalInfo.toString();
	}


	/**
	 *  Sets the otherTechnicalInfo attribute of the DleseBean object
	 *
	 * @param  value  The new otherTechnicalInfo value
	 */
	public void setOtherTechnicalInfo(String value) {
		otherTechnicalInfo.delete(0, otherTechnicalInfo.length());
		if (!value.equals(" ")) {
			otherTechnicalInfo.append(value);
		}
		//System.err.println("Other Technical Info has been set to: \n" + otherTechnicalInfo.toString());
	}


	/**
	 *  Gets the resourceTypes attribute of the DleseBean object
	 *
	 * @return    The resourceTypes value
	 */
	public List getResourceTypes() {
		return resourceTypes;
	}


	/**
	 *  Sets the resourceTypes attribute of the DleseBean object
	 *
	 * @param  list  The new resourceTypes value
	 */
	public void setResourceTypes(List list) {
		resourceTypes.clear();
		if (!list.isEmpty()) {
			ResourceType resType = (ResourceType) list.get(0);
			if (resType.isEmpty()) {
				list.remove(0);
			}
			resourceTypes.addAll(list);
		}
	}


	/**
	 *  Gets the catalogers attribute of the DleseBean object
	 *
	 * @return    The catalogers value
	 */
	public List getCatalogers() {
		return catalogers;
	}


	/**
	 *  Sets the catalogers attribute of the DleseBean object
	 *
	 * @param  list  The new catalogers value
	 */
	public void setCatalogers(List list) {
		catalogers.clear();
		if (!list.isEmpty()) {
			Contributor person = (Contributor) list.get(0);
			if (person.isEmpty()) {
				list.remove(0);
			}
			catalogers.addAll(list);
		}
	}


	/**
	 *  Gets the creators attribute of the DleseBean object
	 *
	 * @return    The creators value
	 */
	public List getCreators() {
		return creators;
	}


	/**
	 *  Sets the creators attribute of the DleseBean object
	 *
	 * @param  list  The new creators value
	 */
	public void setCreators(List list) {
		creators.clear();
		if (!list.isEmpty()) {
			Contributor person = (Contributor) list.get(0);
			if (person.isEmpty()) {
				list.remove(0);
			}
			creators.addAll(list);
		}
	}


	/**
	 *  Gets the annotations attribute of the DleseBean object
	 *
	 * @return    The annotations value
	 */
	public List getAnnotations() {
		return annotations;
	}


	/**
	 *  Sets the annotations attribute of the DleseBean object
	 *
	 * @param  list  The new annotations value
	 */
	public void setAnnotations(List list) {
		annotations.clear();
		if (!list.isEmpty()) {
			Annotation annotated = (Annotation) list.get(0);
			if (annotated.isEmpty()) {
				list.remove(0);
			}
			annotations.addAll(list);
		}
	}


	/**
	 *  Gets the relations attribute of the DleseBean object
	 *
	 * @return    The relations value
	 */
	public List getRelations() {
		return relations;
	}


	/**
	 *  Sets the relations attribute of the DleseBean object
	 *
	 * @param  list  The new relations value
	 */
	public void setRelations(List list) {
		relations.clear();
		if (!list.isEmpty()) {
			Relation related = (Relation) list.get(0);
			if (related.isEmpty()) {
				list.remove(0);
			}
			relations.addAll(list);
		}
	}


	/**
	 *  Gets the geoReferences attribute of the DleseBean object
	 *
	 * @return    The geoReferences value
	 */
	public List getGeoReferences() {
		return geoReferences;
	}


	/**
	 *  Sets the geoReferences attribute of the DleseBean object
	 *
	 * @param  list  The new geoReferences value
	 */
	public void setGeoReferences(List list) {
		geoReferences.clear();
		if (!list.isEmpty()) {
			GeoReference reference = (GeoReference) list.get(0);
			if (reference.isEmpty()) {
				list.remove(0);
			}
			geoReferences.addAll(list);
		}
	}



	/**
	 *  Provides an object definition for representing the technical requirements
	 *  of a DLESE resource, as specified by the DLESE metadata framework. <p>
	 *
	 *
	 *
	 * @author     Dave Deniman
	 * @version    1.0, 02/01/01
	 */
	public static class TechnicalReq implements Serializable
	{

		/**  DESCRIPTION */
		protected HashMap requirement;


		/**  Constructor for the TechnicalReq object */
		public TechnicalReq() {
			requirement = new HashMap();
		}


		/**
		 *  Gets the empty attribute of the TechnicalReq object
		 *
		 * @return    The empty value
		 */
		public boolean isEmpty() {
			if (getValue("typename").length() > 0) {
				return false;
			}
			return true;
		}


		/**
		 *  Constructor for the TechnicalReq object
		 *
		 * @param  request
		 */
		public TechnicalReq(HttpServletRequest request) {
			requirement = new HashMap();
			setValue("typename", request.getParameter("typename"));
			setValue("minversion", request.getParameter("minversion"));
		}


		/**
		 *  Sets the value attribute of the TechnicalReq object
		 *
		 * @param  key    The new value value
		 * @param  value  The new value value
		 */
		public void setValue(String key, String value) {
			if (value != null) {
				requirement.put(key, value);
				//System.err.println("setting technical " + key + " = " + value);
			}
		}


		/**
		 *  Gets the value attribute of the TechnicalReq object
		 *
		 * @param  key  DESCRIPTION
		 * @return      The value value
		 */
		public String getValue(String key) {
			String value = (String) requirement.get(key);
			if (value != null) {
				return value;
			}
			return "";
		}


		/**
		 *  Gets the values attribute of the TechnicalReq object
		 *
		 * @return    The values value
		 */
		public HashMap getValues() {
			return requirement;
		}


		/**
		 *  DESCRIPTION
		 *
		 * @return    DESCRIPTION
		 */
		public String toString() {
			return getValue("typename");
		}


		/**
		 *  DESCRIPTION
		 *
		 * @param  req  DESCRIPTION
		 * @return      DESCRIPTION
		 */
		public boolean equals(TechnicalReq req) {
			if (!getValue("typename").equals(req.getValue("typename"))) {
				return false;
			}
			if (!getValue("minversion").equals(req.getValue("minversion"))) {
				return false;
			}

			return true;
		}

	}



	/**
	 *  Provides an object definition for representing the learning resource types
	 *  of a DLESE resource, as specified by the DLESE metadata framework.
	 *
	 * @author     Dave Deniman
	 * @version    1.0, 02/01/01
	 */
	public static class ResourceType implements Serializable
	{

		/**  DESCRIPTION */
		protected HashMap resource;


		/**  Constructor for the ResourceType object */
		public ResourceType() {
			resource = new HashMap();
		}


		/**
		 *  Constructor for the ResourceType object
		 *
		 * @param  request
		 */
		public ResourceType(HttpServletRequest request) {
			resource = new HashMap();
			setValue("category", request.getParameter("category"));
			setValue("type", request.getParameter("type"));
		}


		/**
		 *  Gets the empty attribute of the ResourceType object
		 *
		 * @return    The empty value
		 */
		public boolean isEmpty() {
			if ((getValue("category").length() > 0) || (getValue("type").length() > 0)) {
				return false;
			}
			return true;
		}


		/**
		 *  Sets the value attribute of the ResourceType object
		 *
		 * @param  key    The new value value
		 * @param  value  The new value value
		 */
		public void setValue(String key, String value) {
			if (value != null) {
				resource.put(key, value);
			}
		}


		/**
		 *  Gets the value attribute of the ResourceType object
		 *
		 * @param  key  DESCRIPTION
		 * @return      The value value
		 */
		public String getValue(String key) {
			String value = (String) resource.get(key);
			if (value != null) {
				return value;
			}
			return "";
		}


		/**
		 *  Gets the values attribute of the ResourceType object
		 *
		 * @return    The values value
		 */
		public HashMap getValues() {
			return resource;
		}


		/**
		 *  DESCRIPTION
		 *
		 * @return    DESCRIPTION
		 */
		public String toString() {
			String tmp = null;
			StringBuffer str = new StringBuffer();
			tmp = getValue("category");
			if (tmp.length() > 0) {
				str.append(tmp.replace('_', ' '));
				str.append(": ");
			}
			tmp = getValue("type");
			str.append(tmp);
			return str.toString();
		}


		/**
		 *  DESCRIPTION
		 *
		 * @param  res  DESCRIPTION
		 * @return      DESCRIPTION
		 */
		public boolean equals(ResourceType res) {
			if (!getValue("category").equals(res.getValue("category"))) {
				return false;
			}
			if (!getValue("type").equals(res.getValue("type"))) {
				return false;
			}

			return true;
		}
	}



	/**
	 *  Provides an object definition for representing a contributor as specified
	 *  by the DLESE metadata framework.
	 *
	 * @author     Dave Deniman
	 * @version    1.0, 02/01/01
	 */
	public static class Contributor implements Serializable
	{

		/**  DESCRIPTION */
		protected HashMap person;


		/**  Constructor for the Contributor object */
		public Contributor() {
			person = new HashMap();
		}


		/**
		 *  Constructor for the Contributor object
		 *
		 * @param  request
		 */
		public Contributor(HttpServletRequest request) {
			person = new HashMap();
			setValue("role", request.getParameter("role"));
			setValue("title", request.getParameter("title"));
			setValue("firstname", request.getParameter("firstname"));
			setValue("mi", request.getParameter("mi"));
			setValue("lastname", request.getParameter("lastname"));
			setValue("org", request.getParameter("org"));
			setValue("email", request.getParameter("email"));
			setValue("homepage", request.getParameter("homepage"));
			setValue("address", request.getParameter("address"));
			setValue("city", request.getParameter("city"));
			setValue("state", request.getParameter("state"));
			setValue("zip", request.getParameter("zip"));
			setValue("country", request.getParameter("country"));
			setValue("phone", request.getParameter("phone"));
			setValue("fax", request.getParameter("fax"));
			setValue("date", request.getParameter("date"));
		}


		/**
		 *  Gets the empty attribute of the Contributor object
		 *
		 * @return    The empty value
		 */
		public boolean isEmpty() {
			if ((getValue("role").length() > 0) && ((getValue("lastname").length() > 0) || (getValue("org").length() > 0))) {
				return false;
			}
			return true;
		}


		/**
		 *  Sets the value attribute of the Contributor object
		 *
		 * @param  key    The new value value
		 * @param  value  The new value value
		 */
		public void setValue(String key, String value) {
			if (value != null) {
				person.put(key, value);
			}
		}


		/**
		 *  Gets the value attribute of the Contributor object
		 *
		 * @param  key  DESCRIPTION
		 * @return      The value value
		 */
		public String getValue(String key) {
			String value = (String) person.get(key);
			if (value != null) {
				return value;
			}
			return "";
		}


		/**
		 *  Gets the values attribute of the Contributor object
		 *
		 * @return    The values value
		 */
		public HashMap getValues() {
			return person;
		}


		/**
		 *  DESCRIPTION
		 *
		 * @return    DESCRIPTION
		 */
		public String name() {
			StringBuffer buf = new StringBuffer();
			buf.append(getValue("title")).append(' ')
					.append(getValue("firstname")).append(' ')
					.append(getValue("mi")).append(' ')
					.append(getValue("lastname")).append(' ');
			return buf.toString();
		}


		/**
		 *  DESCRIPTION
		 *
		 * @param  being  DESCRIPTION
		 * @return        DESCRIPTION
		 */
		public boolean equals(Contributor being) {
			if (!getValue("role").equals(being.getValue("role"))) {
				return false;
			}
			if (!getValue("title").equals(being.getValue("title"))) {
				return false;
			}
			if (!getValue("firstname").equals(being.getValue("firstname"))) {
				return false;
			}
			if (!getValue("mi").equals(being.getValue("mi"))) {
				return false;
			}
			if (!getValue("lastname").equals(being.getValue("lastname"))) {
				return false;
			}
			if (!getValue("org").equals(being.getValue("org"))) {
				return false;
			}
			if (!getValue("email").equals(being.getValue("email"))) {
				return false;
			}
			if (!getValue("homepage").equals(being.getValue("homepage"))) {
				return false;
			}
			if (!getValue("address").equals(being.getValue("address"))) {
				return false;
			}
			if (!getValue("city").equals(being.getValue("city"))) {
				return false;
			}
			if (!getValue("state").equals(being.getValue("state"))) {
				return false;
			}
			if (!getValue("zip").equals(being.getValue("zip"))) {
				return false;
			}
			if (!getValue("country").equals(being.getValue("country"))) {
				return false;
			}
			if (!getValue("phone").equals(being.getValue("phone"))) {
				return false;
			}
			if (!getValue("fax").equals(being.getValue("fax"))) {
				return false;
			}
			if (!getValue("date").equals(being.getValue("date"))) {
				return false;
			}

			return true;
		}
	}


	/**
	 *  Provides an object definition for representing an annotation as specified
	 *  by the DLESE metadata framework.
	 *
	 * @author     Dave Deniman
	 * @version    1.0, 02/01/01
	 */
	public static class Annotation implements Serializable
	{

		/**  DESCRIPTION */
		protected HashMap annotation;


		/**  Constructor for the Annotation object */
		public Annotation() {
			annotation = new HashMap();
		}


		/**
		 *  Constructor for the Annotation object
		 *
		 * @param  request
		 */
		public Annotation(HttpServletRequest request) {
			annotation = new HashMap();
			setValue("title", request.getParameter("title"));
			setValue("firstname", request.getParameter("firstname"));
			setValue("mi", request.getParameter("mi"));
			setValue("lastname", request.getParameter("lastname"));
			setValue("org", request.getParameter("org"));
			setValue("email", request.getParameter("email"));
			setValue("homepage", request.getParameter("homepage"));
			setValue("address", request.getParameter("address"));
			setValue("city", request.getParameter("city"));
			setValue("state", request.getParameter("state"));
			setValue("zip", request.getParameter("zip"));
			setValue("country", request.getParameter("country"));
			setValue("phone", request.getParameter("phone"));
			setValue("fax", request.getParameter("fax"));
			setValue("date", request.getParameter("date"));
			setValue("description", request.getParameter("description"));
		}


		/**
		 *  Gets the empty attribute of the Annotation object
		 *
		 * @return    The empty value
		 */
		public boolean isEmpty() {
			if ((getValue("description").length() > 0) && ((getValue("lastname").length() > 0) || (getValue("org").length() > 0))) {
				return false;
			}
			return true;
		}


		/**
		 *  Sets the value attribute of the Annotation object
		 *
		 * @param  key    The new value value
		 * @param  value  The new value value
		 */
		public void setValue(String key, String value) {
			if (value != null) {
				annotation.put(key, value);
			}
		}


		/**
		 *  Gets the value attribute of the Annotation object
		 *
		 * @param  key  DESCRIPTION
		 * @return      The value value
		 */
		public String getValue(String key) {
			String value = (String) annotation.get(key);
			if (value != null) {
				return value;
			}
			return "";
		}


		/**
		 *  Gets the values attribute of the Annotation object
		 *
		 * @return    The values value
		 */
		public HashMap getValues() {
			return annotation;
		}


		/**
		 *  DESCRIPTION
		 *
		 * @return    DESCRIPTION
		 */
		public String name() {
			StringBuffer buf = new StringBuffer();
			buf.append(getValue("title")).append(' ')
					.append(getValue("firstname")).append(' ')
					.append(getValue("mi")).append(' ')
					.append(getValue("lastname")).append(' ');
			return buf.toString();
		}


		/**
		 *  DESCRIPTION
		 *
		 * @param  annotatated  DESCRIPTION
		 * @return              DESCRIPTION
		 */
		public boolean equals(Annotation annotatated) {
			if (!getValue("title").equals(annotatated.getValue("title"))) {
				return false;
			}
			if (!getValue("firstname").equals(annotatated.getValue("firstname"))) {
				return false;
			}
			if (!getValue("mi").equals(annotatated.getValue("mi"))) {
				return false;
			}
			if (!getValue("lastname").equals(annotatated.getValue("lastname"))) {
				return false;
			}
			if (!getValue("org").equals(annotatated.getValue("org"))) {
				return false;
			}
			if (!getValue("email").equals(annotatated.getValue("email"))) {
				return false;
			}
			if (!getValue("homepage").equals(annotatated.getValue("homepage"))) {
				return false;
			}
			if (!getValue("address").equals(annotatated.getValue("address"))) {
				return false;
			}
			if (!getValue("city").equals(annotatated.getValue("city"))) {
				return false;
			}
			if (!getValue("state").equals(annotatated.getValue("state"))) {
				return false;
			}
			if (!getValue("zip").equals(annotatated.getValue("zip"))) {
				return false;
			}
			if (!getValue("country").equals(annotatated.getValue("country"))) {
				return false;
			}
			if (!getValue("phone").equals(annotatated.getValue("phone"))) {
				return false;
			}
			if (!getValue("fax").equals(annotatated.getValue("fax"))) {
				return false;
			}
			if (!getValue("date").equals(annotatated.getValue("date"))) {
				return false;
			}
			if (!getValue("description").equals(annotatated.getValue("description"))) {
				return false;
			}
			return true;
		}
	}


	/**
	 *  Provides an object definition for representing a relation as specified by
	 *  the DLESE metadata framework.
	 *
	 * @author     Dave Deniman
	 * @version    1.0, 02/01/01
	 */
	public static class Relation implements Serializable
	{

		/**  DESCRIPTION */
		protected HashMap relation;


		/**  Constructor for the Relation object */
		public Relation() {
			relation = new HashMap();
		}


		/**
		 *  Constructor for the Relation object
		 *
		 * @param  request
		 */
		public Relation(HttpServletRequest request) {
			relation = new HashMap();
			setValue("kind", request.getParameter("kind"));
			setValue("recordID", request.getParameter("recordID"));
			setValue("title", request.getParameter("title"));
			setValue("url", request.getParameter("url"));
		}


		/**
		 *  Gets the empty attribute of the Relation object
		 *
		 * @return    The empty value
		 */
		public boolean isEmpty() {
			if (getValue("kind").length() > 0) {
				return false;
			}
			return true;
		}


		/**
		 *  Sets the value attribute of the Relation object
		 *
		 * @param  key    The new value value
		 * @param  value  The new value value
		 */
		public void setValue(String key, String value) {
			if (value != null) {
				relation.put(key, value);
			}
		}


		/**
		 *  Gets the value attribute of the Relation object
		 *
		 * @param  key  DESCRIPTION
		 * @return      The value value
		 */
		public String getValue(String key) {
			String value = (String) relation.get(key);
			if (value != null) {
				return value;
			}
			return "";
		}


		/**
		 *  Gets the values attribute of the Relation object
		 *
		 * @return    The values value
		 */
		public HashMap getValues() {
			return relation;
		}


		//public String toString() {
		//	StringBuffer buf = new StringBuffer();
		//	buf.append(getValue("title")).append(' ')
		//	   .append(getValue("firstname")).append(' ')
		//	   .append(getValue("mi")).append(' ')
		//	   .append(getValue("lastname")).append(' ');
		//	return buf.toString();
		//}


		/**
		 *  DESCRIPTION
		 *
		 * @param  related  DESCRIPTION
		 * @return          DESCRIPTION
		 */
		public boolean equals(Relation related) {
			if (!getValue("kind").equals(related.getValue("kind"))) {
				return false;
			}
			if (!getValue("recordID").equals(related.getValue("recordID"))) {
				return false;
			}
			if (!getValue("title").equals(related.getValue("title"))) {
				return false;
			}
			if (!getValue("url").equals(related.getValue("url"))) {
				return false;
			}
			return true;
		}
	}


	/**
	 *  Provides an object definition for representing a geo-reference as specified
	 *  by the DLESE metadata framework.
	 *
	 * @author     Dave Deniman
	 * @version    1.0, 02/01/01
	 */
	public static class GeoReference implements Serializable
	{

		/**  DESCRIPTION */
		protected HashMap geoReference;


		/**  Constructor for the GeoReference object */
		public GeoReference() {
			geoReference = new HashMap();
		}


		/**
		 *  Constructor for the GeoReference object
		 *
		 * @param  request
		 */
		public GeoReference(HttpServletRequest request) {
			geoReference = new HashMap();
			setValue("north", request.getParameter("north"));
			setValue("south", request.getParameter("south"));
			setValue("east", request.getParameter("east"));
			setValue("west", request.getParameter("west"));
			setValue("min_altitude", request.getParameter("min_altitude"));
			setValue("max_altitude", request.getParameter("max_altitude"));
			setValue("begintime", request.getParameter("begintime"));
			setValue("begindate", request.getParameter("begindate"));
			setValue("begintime_description", request.getParameter("begintime_description"));
			setValue("endtime", request.getParameter("endtime"));
			setValue("enddate", request.getParameter("enddate"));
			setValue("endtime_description", request.getParameter("endtime_description"));
			setValue("place_event_name1", request.getParameter("place_event_name1"));
			setValue("place_event_name2", request.getParameter("place_event_name2"));
			setValue("place_event_name3", request.getParameter("place_event_name3"));
			setValue("place_event_name4", request.getParameter("place_event_name4"));
			setValue("place_event_name5", request.getParameter("place_event_name5"));
			setValue("place_event_name6", request.getParameter("place_event_name6"));
			setValue("place_event_name7", request.getParameter("place_event_name7"));
			setValue("place_event_name8", request.getParameter("place_event_name8"));
		}


		/**
		 *  Gets the empty attribute of the GeoReference object
		 *
		 * @return    The empty value
		 */
		public boolean isEmpty() {
			if (((getValue("north").length() > 0)
					 && (getValue("east").length() > 0)
					 && (getValue("south").length() > 0)
					 && (getValue("west").length() > 0))
					 || ((getValue("begintime").length() > 0)
					 || (getValue("begindate").length() > 0)
					 || (getValue("begintime_description").length() > 0))
					) {
				return false;
			}
			return true;
		}


		/**
		 *  DESCRIPTION
		 *
		 * @return    DESCRIPTION
		 */
		public List placeNames() {
			String value = null;
			ArrayList list = new ArrayList();

			value = (String) geoReference.get("place_event_name1");
			if ((value != null) && !value.equals("")) {
				list.add(value);
			}
			value = (String) geoReference.get("place_event_name2");
			if ((value != null) && !value.equals("")) {
				list.add(value);
			}
			value = (String) geoReference.get("place_event_name3");
			if ((value != null) && !value.equals("")) {
				list.add(value);
			}
			value = (String) geoReference.get("place_event_name4");
			if ((value != null) && !value.equals("")) {
				list.add(value);
			}
			value = (String) geoReference.get("place_event_name5");
			if ((value != null) && !value.equals("")) {
				list.add(value);
			}
			value = (String) geoReference.get("place_event_name6");
			if ((value != null) && !value.equals("")) {
				list.add(value);
			}
			value = (String) geoReference.get("place_event_name7");
			if ((value != null) && !value.equals("")) {
				list.add(value);
			}
			value = (String) geoReference.get("place_event_name8");
			if ((value != null) && !value.equals("")) {
				list.add(value);
			}

			return list;
		}


		/**
		 *  Sets the value attribute of the GeoReference object
		 *
		 * @param  key    The new value value
		 * @param  value  The new value value
		 */
		public void setValue(String key, String value) {
			if (value != null) {
				geoReference.put(key, value);
			}
		}


		/**
		 *  Gets the value attribute of the GeoReference object
		 *
		 * @param  key  DESCRIPTION
		 * @return      The value value
		 */
		public String getValue(String key) {
			String value = (String) geoReference.get(key);
			if (value != null) {
				return value;
			}
			return "";
		}


		/**
		 *  Gets the values attribute of the GeoReference object
		 *
		 * @return    The values value
		 */
		public HashMap getValues() {
			return geoReference;
		}


		/**
		 *  DESCRIPTION
		 *
		 * @param  reference  DESCRIPTION
		 * @return            DESCRIPTION
		 */
		public boolean equals(GeoReference reference) {
			if (!getValue("north").equals(reference.getValue("north"))) {
				return false;
			}
			if (!getValue("south").equals(reference.getValue("south"))) {
				return false;
			}
			if (!getValue("east").equals(reference.getValue("east"))) {
				return false;
			}
			if (!getValue("west").equals(reference.getValue("west"))) {
				return false;
			}
			if (!getValue("min_altitude").equals(reference.getValue("min_altitude"))) {
				return false;
			}
			if (!getValue("max_altitude").equals(reference.getValue("max_altitude"))) {
				return false;
			}
			if (!getValue("begintime").equals(reference.getValue("begintime"))) {
				return false;
			}
			if (!getValue("begindate").equals(reference.getValue("begindate"))) {
				return false;
			}
			if (!getValue("begintime_description").equals(reference.getValue("begintime_description"))) {
				return false;
			}
			if (!getValue("endtime").equals(reference.getValue("endtime"))) {
				return false;
			}
			if (!getValue("enddate").equals(reference.getValue("enddate"))) {
				return false;
			}
			if (!getValue("endtime_description").equals(reference.getValue("endtime_description"))) {
				return false;
			}
			if (!getValue("place_event_name1").equals(reference.getValue("place_event_name1"))) {
				return false;
			}
			if (!getValue("place_event_name2").equals(reference.getValue("place_event_name2"))) {
				return false;
			}
			if (!getValue("place_event_name3").equals(reference.getValue("place_event_name3"))) {
				return false;
			}
			if (!getValue("place_event_name4").equals(reference.getValue("place_event_name4"))) {
				return false;
			}
			if (!getValue("place_event_name5").equals(reference.getValue("place_event_name5"))) {
				return false;
			}
			if (!getValue("place_event_name6").equals(reference.getValue("place_event_name6"))) {
				return false;
			}
			if (!getValue("place_event_name7").equals(reference.getValue("place_event_name7"))) {
				return false;
			}
			if (!getValue("place_event_name8").equals(reference.getValue("place_event_name8"))) {
				return false;
			}
			return true;
		}
	}

}

