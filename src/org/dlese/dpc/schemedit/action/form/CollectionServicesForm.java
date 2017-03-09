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
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.threadedservices.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.ResultDoc;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.serviceclients.remotesearch.RemoteResultDoc;
import org.dlese.dpc.serviceclients.remotesearch.reader.ADNItemDocReader;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.LabelValueBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.*;
import java.util.regex.*;

/**
 *  ActionForm bean for handling requests for Collections-based operations, such
 *  as creating, deleting, exporting and validating collections.
 *
 * @author    ostwald
 */
public class CollectionServicesForm extends DCSBrowseForm {

	private boolean debug = true;

	// private HttpServletRequest request;

	private String recId = null;
	private List formats = null;
	private List statusOptions = null;
	private String[] selectedStatuses = null;

	// input params
	private String fullTitle = null;
	private String shortTitle = null;
	private String collectionKey = null;
	private String idPrefix = null;

	private boolean userProvidedKey = false; // did the user provide a key?

	private String description = "description goes here";
	private String formatOfRecords = "adn";
	private String status = "Accessioned";
	private String policyUrl = "http://policyUrl.net";
	private String policyType = "Collection scope";
	private String editRecordLink = "";

	private String destPath = "";

	// collection creation questions
	private String termsOfUse = null;
	private String termsOfUseURI = null;
	private String copyright = null;
	private String serviceName = null;

	private DcsSetInfo dcsSetInfo = null;
	private String collection;
	// private String parent;
	private XMLDocReader docReader = null;
	private String editRec = "";
	// used to forward after move
	private ResultDoc resultDoc = null;

	private boolean isValidating = false;
	private boolean isExporting = false;
	private String validatingSession;
	private String exportingSession;
	private DcsSetInfo exportingSet;
	private ExportReport exportReport = null;
	private ValidationReport validationReport = null;
	private List archivedReports = null;
	private String exportBaseDir = null;
	private String numIndexingErrors = "-1";

	private String progress = null;
	// private List sets = null;

	/* private List exportedNdrCollectionHandles = null; */
	/**  Constructor  */
	public CollectionServicesForm() { }


	/**
	 *  Reset form attributes associated with multiple check inputs.
	 *
	 * @param  mapping  Description of the Parameter
	 * @param  request  Description of the Parameter
	 */
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		super.reset(mapping, request);
		selectedStatuses = null;
	}


	// accessors supporting collection creation questions
	/**
	 *  Gets the termsOfUse attribute of the CollectionServicesForm object
	 *
	 * @return    The termsOfUse value
	 */
	public String getTermsOfUse() {
		return termsOfUse;
	}


	/**
	 *  Sets the termsOfUse attribute of the CollectionServicesForm object
	 *
	 * @param  s  The new termsOfUse value
	 */
	public void setTermsOfUse(String s) {
		termsOfUse = s;
	}


	/**
	 *  Gets the termsOfUseURI attribute of the CollectionServicesForm object
	 *
	 * @return    The termsOfUseURI value
	 */
	public String getTermsOfUseURI() {
		return termsOfUseURI;
	}


	/**
	 *  Sets the termsOfUseURI attribute of the CollectionServicesForm object
	 *
	 * @param  s  The new termsOfUseURI value
	 */
	public void setTermsOfUseURI(String s) {
		termsOfUseURI = s;
	}


	/**
	 *  Gets the copyright attribute of the CollectionServicesForm object
	 *
	 * @return    The copyright value
	 */
	public String getCopyright() {
		return copyright;
	}


	/**
	 *  Sets the copyright attribute of the CollectionServicesForm object
	 *
	 * @param  s  The new copyright value
	 */
	public void setCopyright(String s) {
		copyright = s;
	}


	/**
	 *  Gets the serviceName attribute of the CollectionServicesForm object
	 *
	 * @return    The serviceName value
	 */
	public String getServiceName() {
		return serviceName;
	}


	/**
	 *  Sets the serviceName attribute of the CollectionServicesForm object
	 *
	 * @param  s  The new serviceName value
	 */
	public void setServiceName(String s) {
		serviceName = s;
	}


	// end accessors for collection creation questions
	/**
	 *  Gets the exportBaseDir attribute of the CollectionServicesForm object
	 *
	 * @return    The exportBaseDir value
	 */
	public String getExportBaseDir() {
		return exportBaseDir;
	}


	/**
	 *  Sets the exportBaseDir attribute of the CollectionServicesForm object
	 *
	 * @param  dir  The new exportBaseDir value
	 */
	public void setExportBaseDir(String dir) {
		exportBaseDir = dir;
	}


	/**
	 *  Gets the resultDoc attribute of the CollectionServicesForm object
	 *
	 * @return    The resultDoc value
	 */
	public ResultDoc getResultDoc() {
		return resultDoc;
	}


	/**
	 *  Sets the resultDoc attribute of the CollectionServicesForm object
	 *
	 * @param  resultDoc  The new resultDoc value
	 */
	public void setResultDoc(ResultDoc resultDoc) {
		this.resultDoc = resultDoc;
	}


	/**
	 *  Gets the docReader attribute of the CollectionServicesForm object
	 *
	 * @return    The docReader value
	 */
	public XMLDocReader getDocReader() {
		if (resultDoc != null) {
			return (XMLDocReader) resultDoc.getDocReader();
		}
		return null;
	}


	/**
	 *  Sets the docReader attribute of the CollectionServicesForm object
	 *
	 * @param  docReader  The new docReader value
	 */
	public void setDocReader(XMLDocReader docReader) {
		this.docReader = docReader;
	}


	/**
	 *  Gets the isExporting attribute of the CollectionServicesForm object
	 *
	 * @return    The isExporting value
	 */
	public boolean getIsExporting() {
		return isExporting;
	}


	/**
	 *  Sets the isExporting attribute of the CollectionServicesForm object
	 *
	 * @param  isExporting  The new isExporting value
	 */
	public void setIsExporting(boolean isExporting) {
		this.isExporting = isExporting;
	}


	/**
	 *  SessionId of the session currently preforming an export operation
	 *
	 * @return    The exportingSession value
	 */
	public String getExportingSession() {
		return exportingSession;
	}


	/**
	 *  Sets the exportingSession attribute of the CollectionServicesForm object
	 *
	 * @param  exportingSession  The new exportingSession value
	 */
	public void setExportingSession(String exportingSession) {
		this.exportingSession = exportingSession;
	}


	/**
	 *  the DcsSetInfo object for set currently being exported
	 *
	 * @return    The exportingSet value
	 */
	public DcsSetInfo getExportingSet() {
		return exportingSet;
	}


	/**
	 *  Sets the exportingSet attribute of the CollectionServicesForm object
	 *
	 * @param  exportingSet  The new exportingSet value
	 */
	public void setExportingSet(DcsSetInfo exportingSet) {
		this.exportingSet = exportingSet;
	}


	/**
	 *  Gets the archivedReports attribute of the CollectionServicesForm object
	 *
	 * @return    The archivedReports value
	 */
	public List getArchivedReports() {
		return archivedReports;
	}


	/**
	 *  Sets the archivedReports attribute of the CollectionServicesForm object
	 *
	 * @param  reports  The new archivedReports value
	 */
	public void setArchivedReports(List reports) {
		archivedReports = reports;
	}


	/**
	 *  Gets the validationReport attribute of the CollectionServicesForm object
	 *
	 * @return    The validationReport value
	 */
	public ValidationReport getValidationReport() {
		return validationReport;
	}


	/**
	 *  Sets the validationReport attribute of the CollectionServicesForm object
	 *
	 * @param  report  The new validationReport value
	 */
	public void setValidationReport(ValidationReport report) {
		validationReport = report;
	}


	/**
	 *  Gets the exportReport attribute of the CollectionServicesForm object
	 *
	 * @return    The exportReport value
	 */
	public ExportReport getExportReport() {
		return exportReport;
	}


	/**
	 *  Sets the exportReport attribute of the CollectionServicesForm object
	 *
	 * @param  report  The new exportReport value
	 */
	public void setExportReport(ExportReport report) {
		exportReport = report;
	}


	/**
	 *  Gets the isValidating attribute of the CollectionServicesForm object
	 *
	 * @return    The isValidating value
	 */
	public boolean getIsValidating() {
		return isValidating;
	}


	/**
	 *  Sets the isValidating attribute of the CollectionServicesForm object
	 *
	 * @param  isValidating  The new isValidating value
	 */
	public void setIsValidating(boolean isValidating) {
		this.isValidating = isValidating;
	}


	/**
	 *  Gets the progress attribute of the CollectionServicesForm object
	 *
	 * @return    The progress value
	 */
	public String getProgress() {
		return this.progress;
	}


	/**
	 *  Sets the progress attribute of the CollectionServicesForm object
	 *
	 * @param  progress  The new progress value
	 */
	public void setProgress(String progress) {
		this.progress = progress;
	}


	/**
	 *  Gets the validatingSession attribute of the CollectionServicesForm object
	 *
	 * @return    The validatingSession value
	 */
	public String getValidatingSession() {
		return validatingSession;
	}


	/**
	 *  Sets the validatingSession attribute of the CollectionServicesForm object
	 *
	 * @param  validatingSession  The new validatingSession value
	 */
	public void setValidatingSession(String validatingSession) {
		this.validatingSession = validatingSession;
	}


	/**
	 *  editRec parameter is used by handleMoveRecord to specify whether control is
	 *  forwarded back to editor. Seems there should be an easier way ...
	 *
	 * @return    The editRec value
	 */
	public String getEditRec() {
		return editRec;
	}


	/**
	 *  Sets the editRec attribute of the CollectionServicesForm object
	 *
	 * @param  s  The new editRec value
	 */
	public void setEditRec(String s) {
		editRec = s;
	}


	/**
	 *  Gets the destPath attribute of the CollectionServicesForm object
	 *
	 * @return    The destPath value
	 */
	public String getDestPath() {
		return destPath;
	}


	/**
	 *  Sets the destPath attribute of the CollectionServicesForm object
	 *
	 * @param  path  The new destPath value
	 */
	public void setDestPath(String path) {
		destPath = path;
	}


	/**
	 *  Gets the statusOptions attribute of the CollectionServicesForm object
	 *
	 * @return    The statusOptions value
	 */
	public List getStatusOptions() {
		return statusOptions;
	}


	/**
	 *  Sets the statusOptions attribute of the CollectionServicesForm object
	 *
	 * @param  options  The new statusOptions value
	 */
	public void setStatusOptions(List options) {
		this.statusOptions = options;
	}


	/**
	 *  Gets the sss (selectedStatuses) attribute of the DCSBrowseForm object
	 *
	 * @return    The sss value
	 */
	public String[] getSss() {
		if (selectedStatuses == null) {
			selectedStatuses = new String[]{};
		}
		return selectedStatuses;
	}


	/**
	 *  Sets the sss (selectedStatuses) attribute of the DCSBrowseForm object
	 *
	 * @param  selectedStatuses  The new sss value
	 */
	public void setSss(String[] selectedStatuses) {
		this.selectedStatuses = selectedStatuses;
	}


	/**
	 *  Gets the dcsSetInfo attribute of the CollectionServicesForm object
	 *
	 * @return    The dcsSetInfo value
	 */
	public DcsSetInfo getDcsSetInfo() {
		return dcsSetInfo;
	}


	/**
	 *  Sets the dcsSetInfo attribute of the CollectionServicesForm object
	 *
	 * @param  info  The new dcsSetInfo value
	 */
	public void setDcsSetInfo(DcsSetInfo info) {
		dcsSetInfo = info;
	}


	/**
	 *  Gets the collection attribute of the CollectionServicesForm object
	 *
	 * @return    The collection value
	 */
	public String getCollection() {
		return collection;
	}


	/**
	 *  Sets the collection attribute of the CollectionServicesForm object
	 *
	 * @param  collection  The new collection value
	 */
	public void setCollection(String collection) {
		this.collection = collection;
	}


	/**
	 *  Gets the parent attribute of the CollectionServicesForm object
	 *
	 * @param  formats  The new formats value
	 */
	/* 	public String getParent() {
		return parent;
	} */

	/**
	 *  Sets the parent attribute of the CollectionServicesForm object
	 *
	 * @param  formats  The new formats value
	 */
	/* 	public void setParent(String parent) {
		this.parent = parent;
	} */

	// ------------------------------

	/**
	 *  Sets the formats attribute of the CollectionServicesForm object
	 *
	 * @param  formats  The new formats value
	 */
	public void setFormats(List formats) {
		this.formats = formats;
	}


	/**
	 *  Gets the formats attribute of the CollectionServicesForm object
	 *
	 * @return    The formats value
	 */
	public List getFormats() {
		return formats;
	}


	/**
	 *  Gets the metadataGroupsLoaderFile attribute of the CollectionServicesForm
	 *  object
	 *
	 * @return    The metadataGroupsLoaderFile value
	 */
	public String getMetadataGroupsLoaderFile() {
		return (String) getServlet().getServletContext().getInitParameter("metadataGroupsLoaderFile");
	}


	/**  Description of the Method  */
	public void clear() {
		recId = null;
		fullTitle = null;
		shortTitle = null;
		collectionKey = null;
		idPrefix = null;
		formatOfRecords = null;
	}


	/**
	 *  Gets the recId attribute of the CollectionServicesForm object
	 *
	 * @return    The recId value
	 */
	public String getRecId() {
		return recId;
	}


	/**
	 *  Sets the recId attribute of the CollectionServicesForm object
	 *
	 * @param  id  The new recId value
	 */
	public void setRecId(String id) {
		recId = id;
	}


	/**
	 *  Gets the editRecordLink attribute of the CollectionServicesForm object
	 *
	 * @return    The editRecordLink value
	 */
	public String getEditRecordLink() {
		return editRecordLink;
	}


	/**
	 *  Sets the editRecordLink attribute of the CollectionServicesForm object
	 *
	 * @param  s  The new editRecordLink value
	 */
	public void setEditRecordLink(String s) {
		editRecordLink = s;
	}



	/**
	 *  Gets the fullTitle attribute of the CollectionServicesForm object
	 *
	 * @return    The fullTitle value
	 */
	public String getFullTitle() {
		return fullTitle;
	}


	/**
	 *  Sets the fullTitle attribute of the CollectionServicesForm object
	 *
	 * @param  fullTitle  The new fullTitle value
	 */
	public void setFullTitle(String fullTitle) {
		this.fullTitle = fullTitle;
	}


	/**
	 *  Gets the shortTitle attribute of the CollectionServicesForm object
	 *
	 * @return    The shortTitle value
	 */
	public String getShortTitle() {
		return shortTitle;
	}


	/**
	 *  Sets the shortTitle attribute of the CollectionServicesForm object
	 *
	 * @param  shortTitle  The new shortTitle value
	 */
	public void setShortTitle(String shortTitle) {
		this.shortTitle = shortTitle;
	}

	public boolean getUserProvidedKey () {
		return this.userProvidedKey;
	}
	
	public void setUserProvidedKey (boolean userSpecified) {
		this.userProvidedKey = userSpecified;
	}

	/**
	 *  Gets the collection attribute of the CollectionServicesForm object
	 *
	 * @return    The collectionKey value
	 */
	public String getCollectionKey() {
		return collectionKey;
	}


	/**
	 *  Sets the collectionKey attribute of the CollectionServicesForm object
	 *
	 * @param  collectionKey  The new collectionKey value
	 */
	public void setCollectionKey(String collectionKey) {
		this.collectionKey = collectionKey;
	}


	/**
	 *  Gets the description attribute of the CollectionServicesForm object
	 *
	 * @return    The description value
	 */
	public String getDescription() {
		return description;
	}


	/**
	 *  Sets the description attribute of the CollectionServicesForm object
	 *
	 * @param  description  The new description value
	 */
	public void setDescription(String description) {
		this.description = description;
	}


	/**
	 *  Gets the idPrefix attribute of the CollectionServicesForm object
	 *
	 * @return    The idPrefix value
	 */
	public String getIdPrefix() {
		return idPrefix;
	}


	/**
	 *  Sets the idPrefix attribute of the CollectionServicesForm object
	 *
	 * @param  idPrefix  The new idPrefix value
	 */
	public void setIdPrefix(String idPrefix) {
		this.idPrefix = idPrefix;
	}


	/**
	 *  Gets the formatOfRecords attribute of the CollectionServicesForm object
	 *
	 * @return    The formatOfRecords value
	 */
	public String getFormatOfRecords() {
		return formatOfRecords;
	}


	/**
	 *  Sets the formatOfRecords attribute of the CollectionServicesForm object
	 *
	 * @param  format  The new formatOfRecords value
	 */
	public void setFormatOfRecords(String format) {
		formatOfRecords = format;
	}


	/**
	 *  Gets the status attribute of the CollectionServicesForm object
	 *
	 * @return    The status value
	 */
	public String getStatus() {
		return status;
	}


	/**
	 *  Sets the status attribute of the CollectionServicesForm object
	 *
	 * @param  status  The new status value
	 */
	public void setStatus(String status) {
		this.status = status;
	}


	/**
	 *  Gets the policyUrl attribute of the CollectionServicesForm object
	 *
	 * @return    The policyUrl value
	 */
	public String getPolicyUrl() {
		return policyUrl;
	}


	/**
	 *  Sets the policyUrl attribute of the CollectionServicesForm object
	 *
	 * @param  policyUrl  The new policyUrl value
	 */
	public void setPolicyUrl(String policyUrl) {
		this.policyUrl = policyUrl;
	}


	/**
	 *  Gets the policyType attribute of the CollectionServicesForm object
	 *
	 * @return    The policyType value
	 */
	public String getPolicyType() {
		return policyType;
	}


	/**
	 *  Sets the policyType attribute of the CollectionServicesForm object
	 *
	 * @param  policyType  The new policyType value
	 */
	public void setPolicyType(String policyType) {
		this.policyType = policyType;
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
	 *  Gets the number of indexing errors that are present. A value of -1 means no
	 *  data is available.
	 *
	 * @return    numErrors The number of indexing errors that are present.
	 */
	public String getNumIndexingErrors() {
		return numIndexingErrors;
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println("CollectionServicesForm: " + s);
		}
	}

}

