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
package org.dlese.dpc.schemedit.action.form;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.DcsSetInfo;
import org.dlese.dpc.propertiesmgr.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.repository.action.form.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.util.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;

import java.net.URL;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.*;

/**
 *  This class uses the getter methods of the ProviderBean and then adds setter methods
 *  for editable fields.
 *
 * @author    John Weatherley, Jonathan Ostwald
 */
public final class DCSAdminForm extends RepositoryForm implements Serializable {
	private String message = null;
	private String xmlError = null;
	private String currentAdminEmail = null;
	private String currentDescription = null;
	private String repositoryName = null;
	private String repositoryIdentifier = null;
	private String currentSetSpec = null;
	private String currentSetName = null;
	private String currentSetDirectory = null;
	private String currentSetDescription = null;
	private String currentSetFormat = null;
	private String trustedWsIps = null;
	private String add = "";
	private String numIdentifiersResults = null;
	private String numRecordsResults = null;
	private ArrayList validCollectionKeys = null;
	private ArrayList validMetadataFormats = null;
	private String updateFrequency = "-1";
	private String numIndexingErrors = "-1";
	private String collectionRecordsLocation = null;
	private String metadataRecordsLocation = null;
	private Date indexingStartTimeDate = null;
	private String drcBoostFactor = "", titleBoostFactor = "", stemmingBoostFactor = "", multiDocBoostFactor = "";
	private String stemmingEnabled = "false";
	private Map lockedRecords = null;
	private List sessionBeans = null;
	
	private String collectionKey = null;
	private String collectionPrefix = null;
	
	private String dcsId = null;
	private String dcsXml = null;
	
	private Map frameworks = null;
	private List unloadedFrameworks = null;
	
	public String getDcsXml () {
		return dcsXml;
	}
	
	public void setDcsXml (String xml) {
		dcsXml = xml;
	}
	
	public String getDcsId () {
		return dcsId;
	}
	
	public void setDcsId (String id) {
		dcsId = id;
	}
	
	public List getSessionBeans () {
		return sessionBeans;
	}
	
	public void setSessionBeans (List sessionBeans) {
		this.sessionBeans = sessionBeans;
	}
	
	public Map getFrameworks () {
		return frameworks;
	}
	
	public void setFrameworks (Map map) {
		frameworks = map;
	}
	
	public void setUnloadedFrameworks (List formats) {
		this.unloadedFrameworks = formats;
	}
	
	public List getUnloadedFrameworks () {
		return this.unloadedFrameworks;
	}
	
	public String getCollectionPrefix () {
		return collectionPrefix;
	}
	
	public void setCollectionPrefix (String prefix) {
		collectionPrefix = prefix;
	}
	
	public String getCollectionKey () {
		return collectionKey;
	}
	
	public void setCollectionKey (String key) {
		collectionKey = key;
	}
	
	public String getTrustedWsIps(){
		return trustedWsIps;	
	}

	public void setTrustedWsIps(String val){
		trustedWsIps = val;	
	}	
	
/* 	public String getIdPrefixesPath () {
		return idPrefixesPath;
	}
	
	public void setIdPrefixesPath (String path) {
		idPrefixesPath = path;
	} */
	
	public String getMetadataGroupsLoaderFile() {
		return (String)getServlet().getServletContext().getInitParameter( "metadataGroupsLoaderFile" );
	}
	
	public void setLockedRecords (Map lockedRecords) {
		this.lockedRecords = lockedRecords;
	}
	
	public Map getLockedRecords () {
		return lockedRecords;
	}
	
	/**
	 *  Gets the numIdentifiersResults attribute of the DCSAdminForm object
	 *
	 * @return    The numIdentifiersResults value
	 */
	public String getNumIdentifiersResults() {
		if (numIdentifiersResults == null)
			return "value not initialized";
		else
			return numIdentifiersResults;
	}


	/**
	 *  Sets the numIdentifiersResults attribute of the DCSAdminForm object
	 *
	 * @param  numResults  The new numIdentifiersResults value
	 */
	public void setNumIdentifiersResults(String numResults) {
		numIdentifiersResults = numResults;
	}

	

	/**
	 *  Gets the numRecordsResults attribute of the DCSAdminForm object
	 *
	 * @return    The numRecordsResults value
	 */
	public String getNumRecordsResults() {
		if (numRecordsResults == null)
			return "value not initialized";
		else
			return numRecordsResults;
	}


	/**
	 *  Sets the numRecordsResults attribute of the DCSAdminForm object
	 *
	 * @param  numResults  The new numRecordsResults value
	 */
	public void setNumRecordsResults(String numResults) {
		numRecordsResults = numResults;
	}

	

	/**
	 *  Gets the frequency by which the index is updated to reflect changes that occur in the
	 *  metadata files. A return of 0 indicates no automatic updating occurs.
	 *
	 * @return    The updateFrequency, in minutes.
	 */
	public String getUpdateFrequency() {
		return updateFrequency;
	}


	/**
	 *  Sets the frequency by which the index is updated to reflect changes that occur in the
	 *  metadata files. A return of 0 indicates no automatic updating occurs.
	 *
	 * @param  frequency  The new updateFrequency, in minutes.
	 */
	public void setUpdateFrequency(int frequency) {
		updateFrequency = Integer.toString(frequency);
	}


	/**
	 *  Gets the date and time the indexer is/was scheduled to start, for example 'Dec 2,
	 *  2003 1:35 AM MST'.
	 *
	 * @return    The date and time the indexer is scheduled to start, or empty String if not
	 *      available.
	 */
	public String getIndexingStartDate() {
		if (indexingStartTimeDate == null)
			return "";
		try {
			return Utils.convertDateToString(indexingStartTimeDate, "EEE, MMM d, yyyy");
		} catch (ParseException e) {
			return "";
		}
	}


	/**
	 *  Gets the time of day the indexer isscheduled to start, for example '1:35 AM MST'.
	 *
	 * @return    the time of day the indexer isscheduled to start, or empty String if not
	 *      available.
	 */
	public String getIndexingTimeOfDay() {
		if (indexingStartTimeDate == null)
			return "";
		try {
			return Utils.convertDateToString(indexingStartTimeDate, "h:mm a zzz");
		} catch (ParseException e) {
			return "";
		}
	}


	/**
	 *  Sets the date and time the indexer is/was scheduled to start.
	 *
	 * @param  indexingStartTime  The new indexingStartTime value
	 */
	public void setIndexingStartTime(Date indexingStartTime) {
		this.indexingStartTimeDate = indexingStartTime;
	}



	/**
	 *  Sets the number of indexing errors that are present.
	 *
	 * @param  numErrors  The number of indexing errors that are present.
	 */
 	public void setNumIndexingErrors(int numErrors) {
		numIndexingErrors = Integer.toString(numErrors);
	}


	/**
	 *  Gets the number of indexing errors that are present. A value of -1 means no data is
	 *  available.
	 *
	 * @return    numErrors The number of indexing errors that are present.
	 */
 	public String getNumIndexingErrors() {
		return numIndexingErrors;
	}


	/**
	 *  Gets the removeInvalidRecords attribute of the DCSAdminForm object
	 *
	 * @return    The removeInvalidRecords value
	 */
	public String getRemoveInvalidRecords() {
		RepositoryManager rm = 
			(RepositoryManager)getServlet().getServletContext().getAttribute("repositoryManager");
		if(rm == null)
			return "";				
		
		return rm.getRemoveInvalidRecords();
	}


	/**
	 *  Gets the validateRecords attribute of the DCSAdminForm object
	 *
	 * @return    The validateRecords value
	 */
	public String getValidateRecords() {
		RepositoryManager rm = 
			(RepositoryManager)getServlet().getServletContext().getAttribute("repositoryManager");
		if(rm == null)
			return "";			
		
		return rm.getValidateRecords();
	}


	/**
	 *  Sets the repositoryName attribute of the DCSAdminForm object
	 *
	 * @param  name  The new repositoryName value
	 */
	public void setRepositoryName(String name) {
		repositoryName = name.trim();
	}


	/**
	 *  Sets the repositoryIdentifier attribute of the DCSAdminForm object
	 *
	 * @param  value  The new repositoryIdentifier value
	 */
	public void setRepositoryIdentifier(String value) {
		if (value != null)
			repositoryIdentifier = value.trim();
	}


	/**
	 *  Gets the repositoryIdentifier attribute of the DCSAdminForm object
	 *
	 * @return    The repositoryIdentifier value
	 */
	public String getRepositoryIdentifier() {
		return repositoryIdentifier;
	}


	/**
	 *  Sets the currentAdminEmail attribute of the DCSAdminForm object
	 *
	 * @param  value  The new currentAdminEmail value
	 */
	public void setCurrentAdminEmail(String value) {
		prtln("setCurrentAdminEmail( " + value + " )");
		currentAdminEmail = value;
	}


	/**
	 *  Gets the currentAdminEmail attribute of the DCSAdminForm object
	 *
	 * @return    The currentAdminEmail value
	 */
	public String getCurrentAdminEmail() {
		prtln("getCurrentAdminEmail( ) returning: " + currentAdminEmail);
		return currentAdminEmail;
	}


	/**
	 *  Gets the exampleId attribute of the DCSAdminForm object
	 *
	 * @return    The exampleId value
	 */
	public String getExampleId() {
		RepositoryManager rm = 
			(RepositoryManager)getServlet().getServletContext().getAttribute("repositoryManager");
		if(rm == null)
			return "";			
		
		return rm.getExampleID();
	}


	/**
	 *  Sets the currentDescription attribute of the DCSAdminForm object
	 *
	 * @param  value  The new currentDescription value
	 */
	public void setCurrentDescription(String value) {
		prtln("setDescription( " + value + " )");
		currentDescription = value;
	}


	/**
	 *  Gets the currentDescription attribute of the DCSAdminForm object
	 *
	 * @return    The currentDescription value
	 */
	public String getCurrentDescription() {
		prtln("getDescription( ) returning: " + currentDescription);
		return currentDescription;
	}



	/**
	 *  Sets the add attribute of the DCSAdminForm object
	 *
	 * @param  value  The new add value
	 */
	public void setAdd(String value) {
		this.add = value;
	}


	/**
	 *  Gets the add attribute of the DCSAdminForm object
	 *
	 * @return    The add value
	 */
	public String getAdd() {
		return add;
	}


	/**
	 *  Sets the absolute path to the metadataRecordsLocation.
	 *
	 * @param  metadataRecordsLocation  The absolute path to a directory containing
	 *      item-level metadata. All metadata files must reside in sub-directores by format
	 *      and collection, for example: metadataRecordsLocation +
	 *      /adn/dcc/DLESE-000-000-000-001.xml.
	 */
	public void setMetadataRecordsLocation(String metadataRecordsLocation) {
		this.metadataRecordsLocation = metadataRecordsLocation;
	}


	/**
	 *  Sets the absolute path to the collectionRecordsLocation.
	 *
	 * @param  collectionRecordsLocation  The absolute path to a directory of DLESE
	 *      collection-level XML records.
	 */
	public void setCollectionRecordsLocation(String collectionRecordsLocation) {
		this.collectionRecordsLocation = collectionRecordsLocation;
	}


	/**
	 *  Gets the path for the directory of collect-level records the RepositoryManager is
	 *  using, or empty string if none is configured.
	 *
	 * @return    The collectionRecordsLocation value
	 */
	public String getCollectionRecordsLocation() {
		if (collectionRecordsLocation != null)
			return collectionRecordsLocation;
		else
			return "";
	}


	/**
	 *  Gets the path for the directory of metadata records the RepositoryManager is using,
	 *  or empty string if none is configured.
	 *
	 * @return    The metadataRecordsLocation value
	 */
	public String getMetadataRecordsLocation() {
		if (this.metadataRecordsLocation != null)
			return this.metadataRecordsLocation;
		else
			return "";
	}


	/**
	 *  Sets the currentSetDescription attribute of the DCSAdminForm object
	 *
	 * @param  value  The new currentSetDescription value
	 */
	public void setCurrentSetDescription(String value) {
		prtln("setCurrentSetDescription( " + value + " )");
		currentSetDescription = value;
	}


	/**
	 *  Gets the currentSetDescription attribute of the DCSAdminForm object
	 *
	 * @return    The currentSetDescription value
	 */
	public String getCurrentSetDescription() {
		prtln("getCurrentSetDescription( ) returning: " + currentSetDescription);
		return currentSetDescription;
	}



	/**
	 *  Sets the currentSetName attribute of the DCSAdminForm object
	 *
	 * @param  value  The new currentSetName value
	 */
	public void setCurrentSetName(String value) {
		prtln("setCurrentSetName( " + value + " )");
		currentSetName = value;
	}


	/**
	 *  Gets the currentSetName attribute of the DCSAdminForm object
	 *
	 * @return    The currentSetName value
	 */
	public String getCurrentSetName() {
		prtln("getCurrentSetName( ) returning: " + currentSetName);
		return currentSetName;
	}


	/**
	 *  Sets the currentSetSpec attribute of the DCSAdminForm object
	 *
	 * @param  value  The new currentSetSpec value
	 */
	public void setCurrentSetSpec(String value) {
		prtln("setCurrentSetSpec( " + value + " )");
		currentSetSpec = value;
	}


	/**
	 *  Gets the currentSetSpec attribute of the DCSAdminForm object
	 *
	 * @return    The currentSetSpec value
	 */
	public String getCurrentSetSpec() {
		prtln("getCurrentSetSpec( ) returning: " + currentSetSpec);
		return currentSetSpec;
	}


	/**
	 *  Sets the currentSetDirectory attribute of the DCSAdminForm object
	 *
	 * @param  value  The new currentSetDirectory value
	 */
	public void setCurrentSetDirectory(String value) {
		currentSetDirectory = value;
	}


	/**
	 *  Gets the currentSetDirectory attribute of the DCSAdminForm object
	 *
	 * @return    The currentSetDirectory value
	 */
	public String getCurrentSetDirectory() {
		return currentSetDirectory;
	}


	/**
	 *  Gets the sortSetsBy attribute of the DCSAdminForm object
	 *
	 * @return    The sortSetsBy value
	 */
	public String getSortSetsBy() {
		return sortSetsBy;
	}


	private String sortSetsBy = "collection";


	/**
	 *  Sets the sortSetsBy attribute of the DCSAdminForm object
	 *
	 * @param  sortSetsBy  The new sortSetsBy value
	 */
	public void setSortSetsBy(String sortSetsBy) {
		this.sortSetsBy = sortSetsBy;
	}


	/**
	 *  Gets the sets configured in the RepositoryManager. Overloaded method from
	 *  RepositoryForm.
	 *
	 * @return    The sets value
	 */
	public ArrayList getSets() {
		if (sets == null) {
			prtln ("getSets() sets is null");
			return new ArrayList();
		}
		return sets;
	}
	
	private ArrayList sets = null;
	
	/**
	 *  Sets the sets attribute of the DCSAdminForm object
	 *
	 * @param  sets  The new sets value
	 */
	public void setSets(ArrayList setInfos) {
		if (setInfos == null) {
			prtln (" ... sets is null");
			sets = new ArrayList ();
		}
		else {
			sets = setInfos;
			String sortBy = getSortSetsBy();
			if (sortBy.equals("collection")) {
				Collections.sort(sets);
			}
			else {
				Collections.sort(sets, SetInfo.getComparator(sortBy));
			}
		}
	}


	/**
	 *  Sets the currentSetFormat attribute of the DCSAdminForm object
	 *
	 * @param  value  The new currentSetFormat value
	 */
	public void setCurrentSetFormat(String value) {
		prtln("setCurrentSetFormat( " + value + " )");
		currentSetFormat = value;
	}


	/**
	 *  Gets the currentSetFormat attribute of the DCSAdminForm object
	 *
	 * @return    The currentSetFormat value
	 */
	public String getCurrentSetFormat() {
		prtln("getCurrentSetFormat( ) returning: " + currentSetFormat);
		return currentSetFormat;
	}


	/**
	 *  Sets the boosting factor used to rank items in the DRC. Value must be zero or
	 *  greater.
	 *
	 * @param  boostFactor  The new boosting factor used to rank items in the DRC.
	 */
	public void setDrcBoostFactor(String boostFactor) {
		drcBoostFactor = boostFactor;

	}


	/**
	 *  Sets the boosting factor used to rank resources that have multiple records.
	 *
	 * @param  boostFactor  The boosting factor used to rank resources that have multiple
	 *      records.
	 */
	public void setMultiDocBoostFactor(String boostFactor) {
		multiDocBoostFactor = boostFactor;

	}


	/**
	 *  Sets the boosting factor used to rank items with matching terms in the title field.
	 *  Value must be zero or greater.
	 *
	 * @param  boostFactor  The boosting factor used to rank items with matching terms in the
	 *      title field.
	 */
	public void setTitleBoostFactor(String boostFactor) {
		titleBoostFactor = boostFactor;

	}


	/**
	 *  Sets whether stemming support is enabled.
	 *
	 * @param  stemmingEnabled  The new stemmingEnabled value
	 */
	public void setStemmingEnabled(String stemmingEnabled) {
		this.stemmingEnabled = stemmingEnabled;
	}



	/**
	 *  Sets the boosting factor used to rank items with matching stemmed terms. Value must
	 *  be zero or greater.
	 *
	 * @param  boostFactor  The boosting factor used to rank items with matching stemmed
	 *      terms.
	 */
	public void setStemmingBoostFactor(String boostFactor) {
		stemmingBoostFactor = boostFactor;

	}


	/**
	 *  Gets the boosting factor used to rank items with matching stemmed terms.
	 *
	 * @return    The boosting factor used to rank items with matching stemmed terms.
	 */
	public String getStemmingBoostFactor() {
		return stemmingBoostFactor;
	}



	/**
	 *  Gets the boosting factor used to rank items in the DRC.
	 *
	 * @return    The boosting factor used to rank items in the DRC.
	 */
	public String getDrcBoostFactor() {
		return drcBoostFactor;
	}


	/**
	 *  Gets the boosting factor used to rank resources that have multiple records.
	 *
	 * @return    The boosting factor used to rank resources that have multiple records.
	 */
	public String getMultiDocBoostFactor() {
		return multiDocBoostFactor;
	}


	/**
	 *  Gets the titleBoostFactor attribute of the DCSAdminForm object
	 *
	 * @return    The titleBoostFactor value
	 */
	public String getTitleBoostFactor() {
		return titleBoostFactor;
	}


	/**
	 *  Indicates whether stemming support is enabled.
	 *
	 * @return    true if stemming is enabled, false otherwise.
	 */
	public String getStemmingEnabled() {
		return stemmingEnabled;
	}



	/**  Constructor for the DCSAdminForm Bean object */
	public DCSAdminForm() {
		prtln("DCSAdminForm() ");
	}


	/**
	 *  Sets the message attribute of the DCSAdminForm object
	 *
	 * @param  message  The new message value
	 */
	public void setMessage(String message) {
		this.message = message;
	}


	/**
	 *  Gets the message attribute of the DCSAdminForm object
	 *
	 * @return    The message value
	 */
	public String getMessage() {
		return message;
	}


	/**
	 *  Sets the xmlError attribute of the DCSAdminForm object
	 *
	 * @param  xmlError  The new xmlError value
	 */
	public void setXmlError(String xmlError) {
		//prtln("setXmlError( "+xmlError+" )");
		this.xmlError = xmlError;
	}


	/**
	 *  Gets the xmlError attribute of the DCSAdminForm object
	 *
	 * @return    The xmlError value
	 */
	public String getXmlError() {
		return xmlError;
	}


	/**
	 *  Grabs the base directory where collections metadata files are located, or null if not
	 *  configured.
	 *
	 * @return    Base directory where the collections reside.
	 */
	public String getCollectionsBaseDir() {
		String collectionsBaseDir =
			getServlet().getServletContext().getInitParameter("collBaseDir");

		if (collectionsBaseDir == null || collectionsBaseDir.equalsIgnoreCase("null"))
			collectionsBaseDir = null;
		return collectionsBaseDir;
	}


	/**
	 *  Grabs the collection keys from the DPC keys schema.
	 *
	 * @return    A list of valid colleciton keys.
	 */
	public ArrayList getValidCollectionKeys() {
		String collectionKeySchemaUrl =
			getServlet().getServletContext().getInitParameter("collectionKeySchemaUrl");
		if (collectionKeySchemaUrl == null)
			return null;

		if (validCollectionKeys == null) {
			try {
				validCollectionKeys = new ArrayList();
				SAXReader reader = new SAXReader();
				Document document = reader.read(new URL(collectionKeySchemaUrl));
				validCollectionKeys.add("-- SELECT COLLECTION KEY --");
				List nodes = document.selectNodes("//xsd:simpleType[@name='keyType']/xsd:restriction/xsd:enumeration");
				for (Iterator iter = nodes.iterator(); iter.hasNext(); ) {
					Node node = (Node) iter.next();
					validCollectionKeys.add(node.valueOf("@value"));
				}
			} catch (Throwable e) {
				prtlnErr("Error getCollectionKeys(): " + e);
				validCollectionKeys = null;
			}
		}
		return validCollectionKeys;
	}


	/**
	 *  Grabs the valid metadata formats from the DPC schema.
	 *
	 * @return    A list of valid metadata formats.
	 */
	public ArrayList getValidMetadataFormats() {
		String metadataFormatSchemaUrl =
			getServlet().getServletContext().getInitParameter("metadataFormatSchemaUrl");
		if (metadataFormatSchemaUrl == null)
			return null;

		if (validMetadataFormats == null) {
			try {
				validMetadataFormats = new ArrayList();
				SAXReader reader = new SAXReader();
				Document document = reader.read(new URL(metadataFormatSchemaUrl));
				validMetadataFormats.add("-- SELECT FORMAT --");
				List nodes = document.selectNodes("//xsd:simpleType[@name='itemFormatType']/xsd:restriction/xsd:enumeration");
				for (Iterator iter = nodes.iterator(); iter.hasNext(); ) {
					Node node = (Node) iter.next();
					validMetadataFormats.add(node.valueOf("@value"));
				}
			} catch (Throwable e) {
				prtlnErr("Error getValidMetadataFormats(): " + e);
				validMetadataFormats = null;
			}
		}
		return validMetadataFormats;
	}


	// ************************ Validation methods ***************************

	/**
	 *  Validate the input. This method is called AFTER the setter method is called.
	 *
	 * @param  mapping  The ActionMapping used.
	 * @param  request  The HttpServletRequest for this request.
	 * @return          An ActionError containin any errors that had occured.
	 */
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		ActionErrors errors = new ActionErrors();
		//prtln("validate()");
		try {
			if (currentSetSpec != null && currentSetSpec.matches(".*--.*"))
				errors.add("currentSetSpec", new ActionError("generic.message", "You must select a collection key."));

			if (currentSetFormat != null && currentSetFormat.matches(".*--.*"))
				errors.add("currentSetFormat", new ActionError("generic.message", "You must select a metadata format."));

			if (currentAdminEmail != null && !emailIsValid(currentAdminEmail))
				errors.add("currentAdminEmail", new ActionError("errors.adminEmail"));

			if (currentSetDirectory != null && currentSetDirectory.length() != 0 &&
				request.getParameter("remove") == null &&
				!(new File(currentSetDirectory.trim()).isDirectory()))
				errors.add("setDirectory", new ActionError("errors.setDirectory"));

			if (repositoryIdentifier != null &&
				repositoryIdentifier.length() != 0 &&
				!repositoryIdentifier.matches("[a-zA-Z][a-zA-Z0-9\\-]*(\\.[a-zA-Z][a-zA-Z0-9\\-]+)+"))
				errors.add("repositoryIdentifier", new ActionError("errors.repositoryIdentifier"));

			if (numRecordsResults != null && !numRecordsResults.matches("[1-9]+[0-9]*"))
				errors.add("numResults", new ActionError("errors.numResults"));
			if (numIdentifiersResults != null && !numIdentifiersResults.matches("[1-9]+[0-9]*"))
				errors.add("numResults", new ActionError("errors.numResults"));
		} catch (NullPointerException e) {
			prtln("validate() error: " + e);
			e.printStackTrace();
		} catch (Throwable e) {
			prtln("validate() error: " + e);
		}

		if (!errors.isEmpty())
			prtln("validate() returning errors... ");

		return errors;
		/*
		 *  if (currentSetSpec != null || add.equals("t") && !setSpecIsValid(currentSetSpec))
		 *  errors.add("currentSetSpec", new ActionError("errors.setSpec"));
		 *  if (currentSetName != null || add.equals("t") && currentSetName.length() == 0 )
		 *  errors.add("currentSetName", new ActionError("errors.setName"));
		 */
	}


	/**
	 *  Validates the format of an e-mail address.
	 *
	 * @param  email  The e-mail address to validate.
	 * @return        True iff this e-mail has a valid format.
	 */
	private final boolean emailIsValid(String email) {
		if (email == null || email.trim().length() == 0)
			return true;
		return FormValidationTools.isValidEmail(email);
	}
}


