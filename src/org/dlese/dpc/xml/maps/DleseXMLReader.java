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
import java.text.*;
import java.util.*;
import org.dlese.dpc.xml.*;

/**
 *  This class provides mapping between DLESE metadata values and the
 *  corresponding XML metadata record. <p>
 *
 *  The accessor methods act as an interface with the {@link XMLNode} and {@link
 *  XMLRecord} classes. The XML processing details are handled by those classes,
 *  but the one requirement of this class is to have knowledge of the paths
 *  structure. Paths are used as keys into the <tt>XMLRecord</tt> map.
 *
 * @author     Dave Deniman

 * @version    0.9b, 05/20/02
 */
public class DleseXMLReader implements XMLMap
{

	/**  boolean value indicates success of initialization  */
	protected boolean isValid;

	/**  source XML file  */
	protected File xmlFile;

	/**  convenience abstraction of the XML DOM  */
	protected XMLRecord xmlRecord;

	/**  mapping of user-defined names to metadata values  */
	protected HashMap map;


	/**
	 *  Default constructor is restricted to assure that a file directory is
	 *  provided on object creation.
	 */
	protected DleseXMLReader() {
		isValid = false;
	}


	/**
	 * @param  xmlfile
	 */
	public DleseXMLReader(File xmlfile) {
		isValid = false;
		xmlFile = xmlfile;
	}


	/**
	 * @param  filepath  A qualified filepath to a well-formed XML file.
	 */
	public DleseXMLReader(String filepath) {
		isValid = false;
		xmlFile = new File(filepath);
	}


	private String initErrorMsg;


	/**
	 *  Creates <tt>XMLRecord</tt> and intializes local map. If
	 *  an error occurs then an error message may be obtained 
	 *  by immediately calling the getInitErrorMsg() method.
	 *
	 * @return    <tt>true</tt> if successful, false otherwise.
	 */
	public boolean init() {
		initErrorMsg = "";
		isValid = true;
		try {
			xmlRecord = new XMLRecord(xmlFile);
			map = new HashMap();
		} catch (Throwable t) {
			//System.err.println( this.getClass() + " threw exception with message: " + t.getMessage() );
			//t.printStackTrace();
			initErrorMsg = "Error initializing DleseXMLReader. " + t.toString();
			isValid = false;
		} 
		return isValid;
	
	}


	/**
	 *  Gets a message describing the initialization error or empty string
	 *  if no error occured.
	 *
	 * @return    The initErrorMsg value
	 */
	public String getInitErrorMsg() {
		return initErrorMsg;
	}


	/**  Releases all resources and calls finalize method.  */
	public void destroy() {
		xmlFile = null;
		map.clear();
		map = null;
		try {
			finalize();
		} catch (Throwable t) {}
	}


	/**
	 *  Getter method to retrieve a specific metadata value.
	 *
	 * @param  name  the user-defined name identifying the specific value to
	 *      retrieve.
	 * @return       the requested value if it exists, null otherwise.
	 */
	public Object getValue(String name) {
		return map.get(name);
	}


	/**
	 *  Setter method for setting a specific metadata value.
	 *
	 * @param  name       the user-defined name identifying the specific value to
	 *      update.
	 * @param  xmlObject  the value to assoicate with the specified name.
	 */
	public void setValue(String name, Object xmlObject) {
		map.put(name, xmlObject);
	}


	/**
	 *  After successful initialization, this method creates the map of actual
	 *  name:value pairs defined within this specific <tt>XMLMap</tt> instance.
	 */
	public void setMap() {
		map.clear();
		map.put("ID", getID());
		map.put("resourceURL", getResourceURL());
		map.put("createdDate", getCreatedDate());
		map.put("accessionedDate", getAccessionedDate());
		map.put("title", getTitle());
		map.put("description", getDescription());
		map.put("cost", getCost());
		map.put("copyright", getCopyright());
		map.put("information", getInformation());
		map.put("technicalReqs", getTechnicalReqs());
		map.put("otherTechnicalInfo", getOtherTechnicalInfo());
		map.put("subjects", getSubjects());
		map.put("resourceTypes", getResourceTypes());
		map.put("audiences", getAudiences());
		map.put("creators", getLifecycleContributors());
		map.put("catalogers", getMetametadataContributors());
		map.put("annotations", getAnnotations());
		map.put("relations", getRelations());
		map.put("geoReferences", getGeoReferences());
		map.put("geographyStds", getGeographyStds());
		map.put("scienceStds", getScienceStds());
		map.put("keywords", getKeywords());
		map.put("urls", getURLs());
		map.put("technicalFormat", getTechnicalFormats());

		xmlRecord.clear();
		xmlRecord = null;
	}


	/**
	 *  Retrieves a list the keys contained in the map.
	 *
	 * @return    the list of keys used to hash the metadata values.
	 */
	public List getKeys() {
		ArrayList list = new ArrayList();
		Iterator i = map.keySet().iterator();
		while (i.hasNext()) {
			list.add(i.next());
		}
		return list;
	}


	/**
	 *  Retrieves the list of metadata values held in the map.
	 *
	 * @return    the list of values held in the map.
	 */
	public List getValues() {
		ArrayList list = new ArrayList();
		Iterator i = map.keySet().iterator();
		while (i.hasNext()) {
			String key = (String) i.next();
			Object obj = map.get(key);
			if (obj != null) {
				list.add(key + ": " + obj.toString());
			}
		}
		return list;
	}


	/**
	 *  Retrieves a specific comment from the XML node identified by the key. The
	 *  comment is specified using an identifier, which may be any portion of the
	 *  actual comment, or a label which is stored as part of the comment.
	 *
	 * @param  key         The path for the desired node.
	 * @param  identifier  A string used to identify the specific comment.
	 * @return             The specificied comment.
	 */
	protected String getComment(String key, String identifier) {
		XMLNode node = xmlRecord.getNode(key);
		if (node != null) {
			return node.getComment(identifier);
		}
		return "";
	}


	/**
	 *  Retrieves the record ID from XML file.
	 *
	 * @return    id ID of record.
	 */
	protected String getID() {
		return getStringValue("record:0.metametadata:0.catalogentry:0.entry:0.langstring:0");
	}


	/**
	 *  Retrieves the URL of the reource this record catalogs.
	 *
	 * @return    The resource URL, as a string.
	 */
	protected String getResourceURL() {
		return getStringValue("record:0.technical:0.location:0");
	}


	/**
	 *  Retrieves the date the catalog record was created.
	 *
	 * @return    The catalog record creation date, as a string.
	 */
	protected String getCreatedDate() {
		return getStringValue("record:0.metametadata:0.contribute:0.date:0.datetime:0");
	}


	/**
	 *  Retrieves the date the catalog record was created.
	 *
	 * @return    The catalog record creation date, as a string.
	 */
	protected String getAccessionedDate() {
		return getStringValue("record:0.metametadata:0.catalogentry:0.accession:0");
	}


	/**
	 *  Retrieves the additional information from the footnote comment of the
	 *  current record.
	 *
	 * @return    The footnote comment.
	 */
	protected String getInformation() {
		String str = xmlRecord.getFootnote();
		return str;
	}


	/**
	 *  Retreives the resource title from the current record.
	 *
	 * @return    The resource title.
	 */
	protected String getTitle() {
		return getStringValue("record:0.general:0.title:0.langstring:0");
	}


	/**
	 *  Retreives the description of a resource from the current record.
	 *
	 * @return    The description as a string.
	 */
	protected String getDescription() {
		return getStringValue("record:0.general:0.description:0.langstring:0");
	}


	/**
	 *  Retrieves the cost information of a resource from the current record.
	 *
	 * @return    Cost information as a string.
	 */
	protected String getCost() {
		return getStringValue("record:0.rights:0.cost:0.langstring:0");
	}


	/**
	 *  Retreives the copright information of a resource from the current record.
	 *
	 * @return    Copyright information as a string.
	 */
	protected String getCopyright() {
		return getStringValue("record:0.rights:0.description:0.langstring:0");
	}


	/**
	 *  Gets the otherTechnicalInfo attribute of the DleseXMLReader object
	 *
	 * @return    The otherTechnicalInfo value
	 */
	protected String getOtherTechnicalInfo() {
		return getStringValue("record:0.technical:0.otherrequirementsinfo:0");
	}


	/**
	 *  Retrieves the list of intended audiences for a resource from the current
	 *  record.
	 *
	 * @return    <code>List</code> of strings of intended audiences.
	 */
	protected List getAudiences() {
		/*
		 *  NOTE:
		 *  sets up the basic algorithm used by all the following methods...
		 */
		ArrayList result = new ArrayList();
		// identify the parent node
		XMLNode parent = xmlRecord.getNode("record:0.educational:0");
		// and for each element of this type
		int num = parent.likeChildren("learningcontext");
		if (num > 0) {
			StringBuffer key = new StringBuffer();
			// build the path string
			for (int i = 0; i < num; i++) {
				key.delete(0, key.length());
				key.append("record:0.educational:0.learningcontext:")
				// each path for this element is the same except for
				// this index
						.append(Integer.toString(i))
				// until we added multiple language support - then
				// IMS provides for us to add up to 8 langstrings
						.append(".langstring:0");
				// and get the element content
				String value = getStringValue(key.toString());
				// if there is valid content, add to the result array
				if (value != null) {
					insertToList(result, value);
				}
			}
		}
		return result;
	}


	/**
	 *  Gets the technicalFormats attribute of the DleseXMLReader object
	 *
	 * @return    The technicalFormats value
	 */
	protected List getTechnicalFormats() {
		ArrayList result = new ArrayList();
		// identify the parent node
		XMLNode parent = xmlRecord.getNode("record:0.technical:0");
		// and for each element of this type
		int num = parent.likeChildren("format");
		if (num > 0) {
			StringBuffer key = new StringBuffer();
			// build the path string
			for (int i = 0; i < num; i++) {
				key.delete(0, key.length());
				key.append("record:0.technical:0.format:")
				// each path for this element is the same except for
				// this index
						.append(Integer.toString(i))
				// until we added multiple language support - then
				// IMS provides for us to add up to 8 langstrings
						.append(".langstring:0");
				// and get the element content
				String value = getStringValue(key.toString());
				// if there is valid content, add to the result array
				if (value != null) {
					insertToList(result, value);
				}
			}
		}
		return result;
	}


	/**
	 *  Retrieves the list of subjects specified for a resource from the current
	 *  record.
	 *
	 * @return    <code>List</code> of strings of specified subjects.
	 */
	protected List getSubjects() {
		ArrayList result = new ArrayList();
		XMLNode parent = xmlRecord.getNode("record:0.general:0.extension:0");
		int num = parent.likeChildren("topic");
		if (num > 0) {
			StringBuffer key = new StringBuffer();
			for (int i = 0; i < num; i++) {
				key.delete(0, key.length());
				key.append("record:0.general:0.extension:0.topic:")
						.append(Integer.toString(i))
						.append(".langstring:0");
				String value = getStringValue(key.toString());
				if (value != null) {
					insertToList(result, value);
				}
			}
		}
		return result;
	}


	/**
	 *  Retrieves the list of subjects specified for a resource from the current
	 *  record.
	 *
	 * @return    <code>List</code> of strings of specified subjects.
	 */
	protected List getURLs() {
		ArrayList result = new ArrayList();
		XMLNode parent = xmlRecord.getNode("record:0.technical:0");
		int num = parent.likeChildren("location");
		if (num > 0) {
			StringBuffer key = new StringBuffer();
			for (int i = 0; i < num; i++) {
				key.delete(0, key.length());
				key.append("record:0.technical:0.location:")
						.append(Integer.toString(i));
				String value = getStringValue(key.toString());
				if (value != null) {
					//CatalogTools.insertToList(result, value);
					result.add(value);
				}
			}
		}

		return result;
	}


	/**
	 *  Retrieves the list of technical requirements for a resource from the
	 *  current record.
	 *
	 * @return    <code>List</code> of <code>MeccaMain.TechnicalReq</code> objects.
	 */
	protected List getTechnicalReqs() {
		/*
		 *  The only difference from the string lists above is that object
		 *  types require us to read/write more than one element's content.
		 */
		ArrayList result = new ArrayList();
		XMLNode parent = xmlRecord.getNode("record:0.technical:0");
		int num = parent.likeChildren("requirements");
		if (num > 0) {
			StringBuffer typenamekey = new StringBuffer();
			StringBuffer minversionkey = new StringBuffer();
			for (int i = 0; i < num; i++) {
				typenamekey.delete(0, typenamekey.length());
				typenamekey.append("record:0.technical:0.requirements:")
						.append(Integer.toString(i))
						.append(".typename:0");
				minversionkey.delete(0, minversionkey.length());
				minversionkey.append("record:0.technical:0.requirements:")
						.append(Integer.toString(i))
						.append(".minimumversion:0");

				String typename = getStringValue(typenamekey.toString());
				String minversion = getStringValue(minversionkey.toString());
				if ((typename.length() > 0) && !typename.equals("Missing:Missing")) {
					DleseBean.TechnicalReq req = new DleseBean.TechnicalReq();
					req.setValue("typename", typename);
					req.setValue("minversion", minversion);
					result.add(req);
				}
			}
		}
		return result;
	}


	/**
	 *  Retrieves the list of learning resource types for a resource from the
	 *  current record.
	 *
	 * @return    <code>List</code> of <code>DleseRecord.ResourceType</code>
	 *      objects.
	 */
	protected List getResourceTypes() {
		ArrayList result = new ArrayList();
		XMLNode parent = xmlRecord.getNode("record:0.educational:0");
		int num = parent.likeChildren("learningresourcetype");
		if (num > 0) {
			StringBuffer nodekey = new StringBuffer();
			StringBuffer langkey = new StringBuffer();
			StringBuffer typekey = new StringBuffer();
			for (int i = 0; i < num; i++) {
				langkey.delete(0, langkey.length());
				langkey.append("record:0.educational:0.learningresourcetype:")
						.append(Integer.toString(i))
						.append(".langstring:0");

				String lang = getStringValue(langkey.toString());
				nodekey.delete(0, nodekey.length());
				nodekey.append("record:0.educational:0.learningresourcetype:")
						.append(Integer.toString(i))
						.append(".extension:0.category:0");
				XMLNode node = xmlRecord.getNode(nodekey.toString());
				if (node == null) {
					continue;
				}

				XMLNode categoryNode = node.getChild(0);
				if (categoryNode == null) {
					continue;
				}
				String cat = categoryNode.getName();

				typekey.delete(0, typekey.length());
				typekey.append("record:0.educational:0.learningresourcetype:")
						.append(Integer.toString(i))
						.append(".extension:0.category:0.")
						.append(cat)
						.append(":0.langstring:0");

				String type = getStringValue(typekey.toString());

				DleseBean.ResourceType res = new DleseBean.ResourceType();
				res.setValue("category", cat);
				res.setValue("type", type);
				insertToList(result, res);
			}
		}
		return result;
	}


	/**
	 *  Retrieves the list of metadata contributors for a resource from the current
	 *  record.
	 *
	 * @return    <code>List</code> of <code>DleseRecord.Contributor</code> objects
	 *      identifying the metadata contributors.
	 */
	protected List getMetametadataContributors() {
		ArrayList result = new ArrayList();
		XMLNode parent = xmlRecord.getNode("record:0.metametadata:0");
		int num = parent.likeChildren("contribute");

		if (num > 0) {
			StringBuffer rolekey = new StringBuffer();
			StringBuffer titlekey = new StringBuffer();
			StringBuffer firstnamekey = new StringBuffer();
			StringBuffer mikey = new StringBuffer();
			StringBuffer lastnamekey = new StringBuffer();
			StringBuffer orgkey = new StringBuffer();
			StringBuffer addresskey = new StringBuffer();
			StringBuffer citykey = new StringBuffer();
			StringBuffer statekey = new StringBuffer();
			StringBuffer zipkey = new StringBuffer();
			StringBuffer countrykey = new StringBuffer();
			StringBuffer phonekey = new StringBuffer();
			StringBuffer faxkey = new StringBuffer();
			StringBuffer emailkey = new StringBuffer();
			StringBuffer homepagekey = new StringBuffer();
			StringBuffer datekey = new StringBuffer();

			for (int i = 0; i < num; i++) {
				rolekey.delete(0, rolekey.length());
				rolekey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".role:0.langstring:0");
				titlekey.delete(0, titlekey.length());
				titlekey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.nametitle:0");
				firstnamekey.delete(0, firstnamekey.length());
				firstnamekey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.firstname:0");
				mikey.delete(0, mikey.length());
				mikey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.mi:0");
				lastnamekey.delete(0, lastnamekey.length());
				lastnamekey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.lastname:0");
				orgkey.delete(0, orgkey.length());
				orgkey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.org:0");
				addresskey.delete(0, addresskey.length());
				addresskey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.adr:0");
				citykey.delete(0, citykey.length());
				citykey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.city:0");
				statekey.delete(0, statekey.length());
				statekey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.state:0");
				zipkey.delete(0, zipkey.length());
				zipkey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.zip:0");
				countrykey.delete(0, countrykey.length());
				countrykey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.country:0");
				phonekey.delete(0, phonekey.length());
				phonekey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.tel:0");
				faxkey.delete(0, faxkey.length());
				faxkey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.fax:0");
				emailkey.delete(0, emailkey.length());
				emailkey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.email:0");
				homepagekey.delete(0, homepagekey.length());
				homepagekey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.url:0");
				datekey.delete(0, datekey.length());
				datekey.append("record:0.metametadata:0.contribute:")
						.append(Integer.toString(i))
						.append(".date:0.datetime:0");

				DleseBean.Contributor person = new DleseBean.Contributor();
				person.setValue("role", getStringValue(rolekey.toString()));
				person.setValue("title", getStringValue(titlekey.toString()));
				person.setValue("firstname", getStringValue(firstnamekey.toString()));
				person.setValue("mi", getStringValue(mikey.toString()));
				person.setValue("lastname", getStringValue(lastnamekey.toString()));
				person.setValue("org", getStringValue(orgkey.toString()));
				person.setValue("address", getStringValue(addresskey.toString()));
				person.setValue("city", getStringValue(citykey.toString()));
				person.setValue("state", getStringValue(statekey.toString()));
				person.setValue("zip", getStringValue(zipkey.toString()));
				person.setValue("country", getStringValue(countrykey.toString()));
				person.setValue("phone", getStringValue(phonekey.toString()));
				person.setValue("fax", getStringValue(faxkey.toString()));
				person.setValue("email", getStringValue(emailkey.toString()));
				person.setValue("homepage", getStringValue(homepagekey.toString()));
				person.setValue("date", getStringValue(datekey.toString()));
				result.add(person);
			}
		}
		return result;
	}


	/**
	 *  Retrieves the list of lifecycle contributors to a resource from the current
	 *  record.
	 *
	 * @return    <code>List</code> of <code>DleseRecord.Contributor</code> objects
	 *      identifying the lifecycle contributors.
	 */
	protected List getLifecycleContributors() {
		ArrayList result = new ArrayList();
		XMLNode parent = xmlRecord.getNode("record:0.lifecycle:0");
		int num = parent.likeChildren("contribute");

		if (num > 0) {
			StringBuffer rolekey = new StringBuffer();
			StringBuffer titlekey = new StringBuffer();
			StringBuffer firstnamekey = new StringBuffer();
			StringBuffer mikey = new StringBuffer();
			StringBuffer lastnamekey = new StringBuffer();
			StringBuffer orgkey = new StringBuffer();
			StringBuffer addresskey = new StringBuffer();
			StringBuffer citykey = new StringBuffer();
			StringBuffer statekey = new StringBuffer();
			StringBuffer zipkey = new StringBuffer();
			StringBuffer countrykey = new StringBuffer();
			StringBuffer phonekey = new StringBuffer();
			StringBuffer faxkey = new StringBuffer();
			StringBuffer emailkey = new StringBuffer();
			StringBuffer homepagekey = new StringBuffer();
			StringBuffer datekey = new StringBuffer();

			for (int i = 0; i < num; i++) {
				rolekey.delete(0, rolekey.length());
				rolekey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".role:0.langstring:0");
				titlekey.delete(0, titlekey.length());
				titlekey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.nametitle:0");
				firstnamekey.delete(0, firstnamekey.length());
				firstnamekey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.firstname:0");
				mikey.delete(0, mikey.length());
				mikey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.mi:0");
				lastnamekey.delete(0, lastnamekey.length());
				lastnamekey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.lastname:0");
				orgkey.delete(0, orgkey.length());
				orgkey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.org:0");
				addresskey.delete(0, addresskey.length());
				addresskey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.adr:0");
				citykey.delete(0, citykey.length());
				citykey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.city:0");
				statekey.delete(0, statekey.length());
				statekey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.state:0");
				zipkey.delete(0, zipkey.length());
				zipkey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.zip:0");
				countrykey.delete(0, countrykey.length());
				countrykey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.country:0");
				phonekey.delete(0, phonekey.length());
				phonekey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.tel:0");
				faxkey.delete(0, faxkey.length());
				faxkey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.fax:0");
				emailkey.delete(0, emailkey.length());
				emailkey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.email:0");
				homepagekey.delete(0, homepagekey.length());
				homepagekey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.url:0");
				datekey.delete(0, datekey.length());
				datekey.append("record:0.lifecycle:0.contribute:")
						.append(Integer.toString(i))
						.append(".date:0.datetime:0");

				DleseBean.Contributor person = new DleseBean.Contributor();
				person.setValue("role", getStringValue(rolekey.toString()));
				person.setValue("title", getStringValue(titlekey.toString()));
				person.setValue("firstname", getStringValue(firstnamekey.toString()));
				person.setValue("mi", getStringValue(mikey.toString()));
				person.setValue("lastname", getStringValue(lastnamekey.toString()));
				person.setValue("org", getStringValue(orgkey.toString()));
				person.setValue("address", getStringValue(addresskey.toString()));
				person.setValue("city", getStringValue(citykey.toString()));
				person.setValue("state", getStringValue(statekey.toString()));
				person.setValue("zip", getStringValue(zipkey.toString()));
				person.setValue("country", getStringValue(countrykey.toString()));
				person.setValue("phone", getStringValue(phonekey.toString()));
				person.setValue("fax", getStringValue(faxkey.toString()));
				person.setValue("email", getStringValue(emailkey.toString()));
				person.setValue("homepage", getStringValue(homepagekey.toString()));
				person.setValue("date", getStringValue(datekey.toString()));
				result.add(person);
			}
		}
		return result;
	}


	/**
	 *  Retrieves the list of lifecycle contributors to a resource from the current
	 *  record.
	 *
	 * @return    <code>List</code> of <code>DleseRecord.Contributor</code> objects
	 *      identifying the lifecycle contributors.
	 */
	protected List getAnnotations() {

		ArrayList result = new ArrayList();
		XMLNode parent = xmlRecord.getNode("record:0");
		int num = parent.likeChildren("annotation");
		if (num > 0) {
			StringBuffer titlekey = new StringBuffer();
			StringBuffer firstnamekey = new StringBuffer();
			StringBuffer mikey = new StringBuffer();
			StringBuffer lastnamekey = new StringBuffer();
			StringBuffer orgkey = new StringBuffer();
			StringBuffer addresskey = new StringBuffer();
			StringBuffer citykey = new StringBuffer();
			StringBuffer statekey = new StringBuffer();
			StringBuffer zipkey = new StringBuffer();
			StringBuffer countrykey = new StringBuffer();
			StringBuffer phonekey = new StringBuffer();
			StringBuffer faxkey = new StringBuffer();
			StringBuffer emailkey = new StringBuffer();
			StringBuffer homepagekey = new StringBuffer();
			StringBuffer datekey = new StringBuffer();
			StringBuffer descriptionkey = new StringBuffer();

			for (int i = 0; i < num; i++) {
				titlekey.delete(0, titlekey.length());
				titlekey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.nametitle:0");
				firstnamekey.delete(0, firstnamekey.length());
				firstnamekey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.firstname:0");
				mikey.delete(0, mikey.length());
				mikey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.mi:0");
				lastnamekey.delete(0, lastnamekey.length());
				lastnamekey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.lastname:0");
				orgkey.delete(0, orgkey.length());
				orgkey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.org:0");
				addresskey.delete(0, addresskey.length());
				addresskey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.adr:0");
				citykey.delete(0, citykey.length());
				citykey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.city:0");
				statekey.delete(0, statekey.length());
				statekey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.state:0");
				zipkey.delete(0, zipkey.length());
				zipkey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.zip:0");
				countrykey.delete(0, countrykey.length());
				countrykey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.country:0");
				phonekey.delete(0, phonekey.length());
				phonekey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.tel:0");
				faxkey.delete(0, faxkey.length());
				faxkey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.fax:0");
				emailkey.delete(0, emailkey.length());
				emailkey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.email:0");
				homepagekey.delete(0, homepagekey.length());
				homepagekey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".centity:0.extension:0.url:0");
				datekey.delete(0, datekey.length());
				datekey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".date:0.datetime:0");
				descriptionkey.delete(0, descriptionkey.length());
				descriptionkey.append("record:0.annotation:")
						.append(Integer.toString(i))
						.append(".description:0.langstring:0");

				DleseBean.Annotation annotation = new DleseBean.Annotation();
				annotation.setValue("title", getStringValue(titlekey.toString()));
				annotation.setValue("firstname", getStringValue(firstnamekey.toString()));
				annotation.setValue("mi", getStringValue(mikey.toString()));
				annotation.setValue("lastname", getStringValue(lastnamekey.toString()));
				annotation.setValue("org", getStringValue(orgkey.toString()));
				annotation.setValue("address", getStringValue(addresskey.toString()));
				annotation.setValue("city", getStringValue(citykey.toString()));
				annotation.setValue("state", getStringValue(statekey.toString()));
				annotation.setValue("zip", getStringValue(zipkey.toString()));
				annotation.setValue("country", getStringValue(countrykey.toString()));
				annotation.setValue("phone", getStringValue(phonekey.toString()));
				annotation.setValue("fax", getStringValue(faxkey.toString()));
				annotation.setValue("email", getStringValue(emailkey.toString()));
				annotation.setValue("homepage", getStringValue(homepagekey.toString()));
				annotation.setValue("date", getStringValue(datekey.toString()));
				annotation.setValue("description", getStringValue(descriptionkey.toString()));
				result.add(annotation);
			}
		}
		return result;
	}


	/**
	 *  Retrieves the list of lifecycle contributors to a resource from the current
	 *  record.
	 *
	 * @return    <code>List</code> of <code>DleseRecord.Contributor</code> objects
	 *      identifying the lifecycle contributors.
	 */
	protected List getRelations() {
		ArrayList result = new ArrayList();
		XMLNode parent = xmlRecord.getNode("record:0");
		int num = parent.likeChildren("relation");

		if (num > 0) {
			StringBuffer kindkey = new StringBuffer();
			StringBuffer recordIDkey = new StringBuffer();
			StringBuffer titlekey = new StringBuffer();
			StringBuffer urlkey = new StringBuffer();

			for (int i = 0; i < num; i++) {
				kindkey.delete(0, kindkey.length());
				kindkey.append("record:0.relation:")
						.append(Integer.toString(i))
						.append(".kind:0.langstring:0");
				recordIDkey.delete(0, recordIDkey.length());
				recordIDkey.append("record:0.relation:")
						.append(Integer.toString(i))
						.append(".resource:0.extension:0.catalogentry:0.entry:0.langstring:0");
				titlekey.delete(0, titlekey.length());
				titlekey.append("record:0.relation:")
						.append(Integer.toString(i))
						.append(".resource:0.description:0.langstring:0");
				urlkey.delete(0, urlkey.length());
				urlkey.append("record:0.relation:")
						.append(Integer.toString(i))
						.append(".resource:0.extension:0.location:0");

				DleseBean.Relation relation = new DleseBean.Relation();
				relation.setValue("kind", getStringValue(kindkey.toString()));
				relation.setValue("recordID", getStringValue(recordIDkey.toString()));
				relation.setValue("title", getStringValue(titlekey.toString()));
				relation.setValue("url", getStringValue(urlkey.toString()));
				result.add(relation);
			}
		}
		return result;
	}


	/**
	 *  Retrieves the list of georeferences for a resource from the current record.
	 *
	 * @return    <code>List</code> of <code>DleseRecord.Contributor</code> objects
	 *      identifying the lifecycle contributors.
	 */
	protected List getGeoReferences() {
		ArrayList result = new ArrayList();
		XMLNode parent = xmlRecord.getNode("record:0.general:0");
		int num = parent.likeChildren("coverage");

		if (num > 0) {
			StringBuffer northkey = new StringBuffer();
			StringBuffer southkey = new StringBuffer();
			StringBuffer eastkey = new StringBuffer();
			StringBuffer westkey = new StringBuffer();
			StringBuffer minaltkey = new StringBuffer();
			StringBuffer maxaltkey = new StringBuffer();
			StringBuffer begtimekey = new StringBuffer();
			StringBuffer begdatekey = new StringBuffer();
			StringBuffer begdesckey = new StringBuffer();
			StringBuffer endtimekey = new StringBuffer();
			StringBuffer enddatekey = new StringBuffer();
			StringBuffer enddesckey = new StringBuffer();
			StringBuffer name1key = new StringBuffer();
			StringBuffer name2key = new StringBuffer();
			StringBuffer name3key = new StringBuffer();
			StringBuffer name4key = new StringBuffer();
			StringBuffer name5key = new StringBuffer();
			StringBuffer name6key = new StringBuffer();
			StringBuffer name7key = new StringBuffer();
			StringBuffer name8key = new StringBuffer();

			for (int i = 0; i < num; i++) {
				northkey.delete(0, northkey.length());
				northkey.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.locations:0.box:0.north:0");
				southkey.delete(0, southkey.length());
				southkey.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.locations:0.box:0.south:0");
				eastkey.delete(0, eastkey.length());
				eastkey.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.locations:0.box:0.east:0");
				westkey.delete(0, westkey.length());
				westkey.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.locations:0.box:0.west:0");
				minaltkey.delete(0, minaltkey.length());
				minaltkey.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.locations:0.box:0.min_vertical:0");
				maxaltkey.delete(0, maxaltkey.length());
				maxaltkey.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.locations:0.box:0.max_vertical:0");
				begtimekey.delete(0, begtimekey.length());
				begtimekey.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.begtime:0.time:0");
				begdatekey.delete(0, begdatekey.length());
				begdatekey.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.begtime:0.datecoverage:0");
				begdesckey.delete(0, begdesckey.length());
				begdesckey.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.begtime:0.description:0.langstring:0");
				endtimekey.delete(0, endtimekey.length());
				endtimekey.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.endtime:0.time:0");
				enddatekey.delete(0, enddatekey.length());
				enddatekey.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.endtime:0.datecoverage:0");
				enddesckey.delete(0, enddesckey.length());
				enddesckey.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.endtime:0.description:0.langstring:0");
				name1key.delete(0, name1key.length());
				name1key.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.place_event_name:0.langstring:0");
				name2key.delete(0, name2key.length());
				name2key.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.place_event_name:1.langstring:0");
				name3key.delete(0, name3key.length());
				name3key.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.place_event_name:2.langstring:0");
				name4key.delete(0, name4key.length());
				name4key.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.place_event_name:3.langstring:0");
				name5key.delete(0, name5key.length());
				name5key.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.place_event_name:4.langstring:0");
				name6key.delete(0, name6key.length());
				name6key.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.place_event_name:5.langstring:0");
				name7key.delete(0, name7key.length());
				name7key.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.place_event_name:6.langstring:0");
				name8key.delete(0, name8key.length());
				name8key.append("record:0.general:0.coverage:")
						.append(Integer.toString(i))
						.append(".extension:0.place_event_name:7.langstring:0");

				DleseBean.GeoReference geoReference = new DleseBean.GeoReference();
				geoReference.setValue("north", getStringValue(northkey.toString()));
				geoReference.setValue("south", getStringValue(southkey.toString()));
				geoReference.setValue("east", getStringValue(eastkey.toString()));
				geoReference.setValue("west", getStringValue(westkey.toString()));
				geoReference.setValue("min_altitude", getStringValue(minaltkey.toString()));
				geoReference.setValue("max_altitude", getStringValue(maxaltkey.toString()));
				geoReference.setValue("begintime", getStringValue(begtimekey.toString()));
				geoReference.setValue("begindate", getStringValue(begdatekey.toString()));
				geoReference.setValue("begintime_description", getStringValue(begdesckey.toString()));
				geoReference.setValue("endtime", getStringValue(endtimekey.toString()));
				geoReference.setValue("enddate", getStringValue(enddatekey.toString()));
				geoReference.setValue("endtime_description", getStringValue(enddesckey.toString()));
				geoReference.setValue("place_event_name1", getStringValue(name1key.toString()));
				geoReference.setValue("place_event_name2", getStringValue(name2key.toString()));
				geoReference.setValue("place_event_name3", getStringValue(name3key.toString()));
				geoReference.setValue("place_event_name4", getStringValue(name4key.toString()));
				geoReference.setValue("place_event_name5", getStringValue(name5key.toString()));
				geoReference.setValue("place_event_name6", getStringValue(name6key.toString()));
				geoReference.setValue("place_event_name7", getStringValue(name7key.toString()));
				geoReference.setValue("place_event_name8", getStringValue(name8key.toString()));

				result.add(geoReference);
			}
		}
		return result;
	}


	/**
	 *  Gets the keywords attribute of the DleseXMLReader object
	 *
	 * @return    The keywords value
	 */
	protected List getKeywords() {
		ArrayList result = new ArrayList();
		XMLNode parent = xmlRecord.getNode("record:0.general:0");
		int num = parent.likeChildren("keywords");
		if (num > 0) {
			StringBuffer key = new StringBuffer();
			for (int i = 0; i < num; i++) {
				key.delete(0, key.length());
				key.append("record:0.general:0.keywords:")
						.append(Integer.toString(i))
						.append(".langstring:0");
				String value = getStringValue(key.toString());
				if (value != null) {
					//CatalogTools.insertToList(result, value);
					result.add(value);
				}
			}
		}
		return result;
	}


	/**
	 *  Retrieves the list of geography standards defined for the current resource.
	 *
	 * @return    <code>List</code> of strings of intended audiences.
	 */
	protected List getGeographyStds() {
		ArrayList result = new ArrayList();
		// identify the parent node
		XMLNode parent = xmlRecord.getNode("record:0.educational:0");
		// and for each element of this type
		int num = parent.likeChildren("geogstd");
		if (num > 0) {
			StringBuffer key = new StringBuffer();
			// build the path string
			for (int i = 0; i < num; i++) {
				key.delete(0, key.length());
				key.append("record:0.educational:0.geogstd:")
				// each path for this element is the same except for
				// this index
						.append(Integer.toString(i));
				// and get the element content
				String value = getStringValue(key.toString());
				// if there is valid content, add to the result array
				if (value != null) {
					//CatalogTools.insertToList(result, value);
					result.add(value);
				}
			}
		}
		return result;
	}


	/**
	 *  Retrieves the list of intended audiences for a resource from the current
	 *  record.
	 *
	 * @return    <code>List</code> of strings of intended audiences.
	 */
	protected List getScienceStds() {
		ArrayList result = new ArrayList();
		// identify the parent node
		XMLNode parent = xmlRecord.getNode("record:0.educational:0");
		// and for each element of this type
		int num = parent.likeChildren("scistd");
		if (num > 0) {
			StringBuffer key = new StringBuffer();
			// build the path string
			for (int i = 0; i < num; i++) {
				key.delete(0, key.length());
				key.append("record:0.educational:0.scistd:")
				// each path for this element is the same except for
				// this index
						.append(Integer.toString(i));
				// and get the element content
				String value = getStringValue(key.toString());
				// if there is valid content, add to the result array
				if (value != null) {
					//CatalogTools.insertToList(result, value);
					result.add(value);
				}
			}
		}
		return result;
	}


	/**
	 *  Retrieves text content from the XML node identified by the key.
	 *
	 * @param  key  The path for the desired node.
	 * @return      The text content of the XML node, or an empty string if the
	 *      node does not exist.
	 */
	protected String getStringValue(String key) {
		XMLNode node = xmlRecord.getNode(key);
		if (node != null) {
			return node.getValue();
		}
		//System.err.println("node is null for " + key);
		return "";
	}


	/**
	 *  Simple insertion sort for building a sorted list of Strings.
	 *
	 * @param  list  <code>List</code> to add an item to.
	 * @param  s     <code>String</code> to add to the list.
	 */
	protected void insertToList(List list, String s) {
		if ((list != null) && (s != null)) {
			for (int i = 0; i < list.size(); i++) {
				if (s.compareToIgnoreCase((String) list.get(i)) <= 0) {
					list.add(i, s);
					return;
				}
			}
			list.add(s);
		}
	}


	/**
	 *  Simple insertion routine for constructing an ordered list of Objects
	 *
	 * @param  list  <code>List</code> to add an item to.
	 * @param  o     <code>Object</code> to add to the list.
	 */
	protected void insertToList(List list, Object o) {
		if ((list != null) && (o != null)) {
			String s = o.toString();
			for (int i = 0; i < list.size(); i++) {
				String c = (list.get(i)).toString();
				if (s.compareToIgnoreCase(c) <= 0) {
					list.add(i, o);
					return;
				}
			}
			list.add(o);
		}
	}

}

