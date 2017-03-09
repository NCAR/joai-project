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
package org.dlese.dpc.repository.action.form;

import org.dlese.dpc.propertiesmgr.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.dds.action.form.VocabForm;
import org.dlese.dpc.util.HTMLTools;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.URLEncoder;

/**
 *  A base class bean for creating output used in OAI requests. This class extends ActionFrom, however for
 *  security reasons it does not contain any setter functions. All setter functions are contained in the
 *  subclass ProviderAcminForm.
 *
 * @author     John Weatherley
 * @version    $Id: RepositoryForm.java,v 1.17 2010/07/14 00:18:49 jweather Exp $
 */
public class RepositoryForm extends VocabForm implements Serializable {
	ArrayList errors = null;
	
	private boolean hasBadVerbOrArgument = false;	
	private static boolean debug = false;
	//private static ServletContext servletContext = null;

	/**  The RepositoryManager model that holds the OAI data. */
	//protected static RepositoryManager rm = null;

	/**  A PropertiesManager that contains properties for this bean. */
	protected static PropertiesManager properties = null;

	// Bean properties:

	/**  exampleID */
	protected String exampleID = null;
	/**  ProtocolVersion */
	protected String ProtocolVersion = null;
	/**  providerStatus */
	protected String providerStatus = null;

	/**  earliestDatestamp */
	protected String earliestDatestamp = null;
	/**  deletedRecord */
	protected String deletedRecord = null;
	/**  granularity */
	protected String granularity = null;
	/**  compressions */
	protected ArrayList compressions = null;
	/**  metadataFormats */
	protected Hashtable metadataFormats = null;
	/**  record */
	protected String record = null;

	// OAI-PMH data:
	private String identifier = null;
	private String datestamp = null;
	private List setSpecs = null;
	private ResultDocList results = null;
	private String oaiIdPfx = null;
	private String resumptionToken = null;
	private String resultsOffset = null;
	private String resultsLength = null;
	private String requestedFormat = null;
	private String deletedStatus = null;
	private String baseURL = null;


	/**  Constructor for the RepositoryForm object */
	public RepositoryForm() {

		if (properties == null) {
			try {
				//prtln("loading RepositoryForm.properties");
				properties = new PropertiesManager("RepositoryForm.properties");
			} catch (IOException ioe) {
				prtlnErr("Error loading RepositoryForm properties: " + ioe);
			}
		}
	}



	/**
	 *  Inserts the BASE_URL for this OAI provider.
	 *
	 * @return    A string containing the BASE_URL to this OAI provider.
	 */
	public String getBaseURL() {
		return baseURL;
	}


	/**
	 *  Sets the baseURL to the OAI provider.
	 *
	 * @param  baseURL  The new baseURL value.
	 */
	public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}


	/**
	 *  Inserts the context URL for this OAI provider.
	 *
	 * @return    A string containing the context url to this OAI provider.
	 */
	/*
	 *  public String getContextURL() {
	 *  return rm.getProviderBaseUrl();
	 *  }
	 */
	/**
	 *  Gets the xMLDeclaration attribute of the RepositoryForm object
	 *
	 * @return    The xMLDeclaration value
	 */
	public String getXMLDeclaration() {
		return properties.getProperty("oaipmh.xmldeclaration");
	}


	/**
	 *  Gets the rootOpenTag attribute of the RepositoryForm object
	 *
	 * @return    The rootOpenTag value
	 */
	public String getRootOpenTag() {
		return properties.getProperty("oaipmh.rootopen");
	}


	/**
	 *  Gets the rootCloseTag attribute of the RepositoryForm object
	 *
	 * @return    The rootCloseTag value
	 */
	public String getRootCloseTag() {
		return properties.getProperty("oaipmh.rootclose");
	}



	/**
	 *  Get the <code>request</code> tag that is required in all responses to an OAI request. If the request
	 *  contained a badVerb or badArgument error then no argument=value pairs are returned. For all other
	 *  responses the <code>request</code> tag includes all argument=value pairs as atributes and the OAI
	 *  BASE_URL as the tag body.
	 *
	 * @param  req  Description of the Parameter
	 * @return      String The OAI-PMH <code>request</code> tag appropriate for the currnet OAI response.
	 */
	public String getOAIRequestTag(HttpServletRequest req) {

		//prtln("getOAIRequestTag()");
		
		String verb = req.getParameter("verb");
		
		// If bad argument, no verb arg, bad verb or no args whatsoever, return an empty <request> element with baseURL only...
		if (verb == null || hasBadVerbOrArgument || !req.getParameterNames().hasMoreElements())
			return ("<request>" + getBaseURL() + "</request>\n");

		//prtln("getOAIRequestTag() post... " + this.getClass().getName());

		StringBuffer tag = new StringBuffer();
		tag.append("<request");

		Enumeration params = req.getParameterNames();
		String param = null;

		String[] values = null;
		values = req.getParameterValues("verb");

		// Insert the verb param first
		tag.append(" verb=\"" + values[0] + "\"");

		String parmVal;
		while (params.hasMoreElements()) {
			// Insert all other params next
			param = (String) params.nextElement();
			values = req.getParameterValues(param);
			if (!param.equals("verb") && !param.equals("rt") && OAIArgs.ALL_VALID_OAI_ARGUMENTS_MAP.containsKey(param)) {

				if (param.equals("set") && values[0].startsWith("dleseodlsearch")) {
					try {
						//prtln("parm1: " + values[0]);
						parmVal = URLEncoder.encode(values[0], "utf-8");
						//prtln("parm2: " + parmVal);
					} catch (UnsupportedEncodingException e) {
						parmVal = "";
					}
				}
				else
					parmVal = HTMLTools.encodeCharacterEntityReferences(values[0], false);

				tag.append(" " + param + "=\"" + parmVal + "\"");
			}
		}
		tag.append(">" + getBaseURL() + "</request>");

		return tag.toString();
	}


	/**
	 *  Adds a feature to the Error attribute of the RepositoryForm object
	 *
	 * @param  error    The OAI Error.
	 * @param  message  The message that will be returned in the OAI response that describes the reason for the
	 *      error.
	 */
	public void addOaiError(String error, String message) {
		//prtln("addOaiError(): " + error + " " + this.getClass().getName());
		if (errors == null)
			errors = new ArrayList();

		errors.add(new OAIError(error, message));
		if(!hasBadVerbOrArgument)
			hasBadVerbOrArgument = (error.equals(OAICodes.BAD_VERB) || error.equals(OAICodes.BAD_ARGUMENT));
	}


	/**
	 *  Determines whether there has been at least one error generated for this request.
	 *
	 * @return    True iff there were no errors reported, else false.
	 */
	public boolean hasErrors() {
		return (errors != null && errors.size() != 0);
	}


	/**
	 *  Iff OAI errors exist, returns the error tag(s) appropriate for the given response, otherwise returns an
	 *  empty string.
	 *
	 * @return    The OAI errors appropritate for this response or empty string.
	 */
	public ArrayList getErrors() {
		if (errors == null)
			return new ArrayList();
		return errors;
	}



	//=============================== Data access methods =================================

	/**
	 *  Get an example ID that might be disiminated from this repository.
	 *
	 * @return    An example ID from this repository.
	 */
	public String getExampleID() {
		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return null;

		return rm.getExampleID();
	}


	/**
	 *  Gets the repositoryName attribute of the RepositoryForm object
	 *
	 * @return    The repositoryName value
	 */
	public String getRepositoryName() {
		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return "";

		return rm.getRepositoryName();
	}


	/**
	 *  Gets the metadataFormats attribute of the RepositoryForm object
	 *
	 * @return    The metadataFormats value
	 */
	public Hashtable getMetadataFormats() {
		if (metadataFormats != null)
			return metadataFormats;
		else
			return new Hashtable();
	}


	/**
	 *  Sets the deletedStatus attribute of the RepositoryForm object
	 *
	 * @param  val  The new deletedStatus value
	 */
	public void setDeletedStatus(String val) {
		this.deletedStatus = val;
	}


	/**
	 *  Gets the deletedStatus attribute of the RepositoryForm object
	 *
	 * @return    The deletedStatus value
	 */
	public String getDeletedStatus() {
		if (deletedStatus == null)
			return "unknown";
		else
			return deletedStatus;
	}


	/**
	 *  Gets the number of records that have a status of deleted in the OAI repository.
	 *
	 * @return    The number of deleted documents.
	 */
	public int getNumDeletedDocs() {
		RepositoryManager rm = (RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return 0;
		return rm.getNumDeletedDocs();
	}


	/**
	 *  Gets the number of records that have a status NOT deleted in the OAI repository.
	 *
	 * @return    The number of non-deleted documents in the repository.
	 */
	public int getNumNonDeletedDocs() {
		RepositoryManager rm = (RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return 0;
		return rm.getNumNonDeletedDocs();
	}


	/**
	 *  Sets the metadataFormats attribute of the RepositoryForm object
	 *
	 * @param  formats  The new metadataFormats value
	 */
	public void setMetadataFormats(Hashtable formats) {
		metadataFormats = formats;
	}


	/**
	 *  Sets the resumptionToken that should be returned with the results.
	 *
	 * @param  resumptionToken  The new resumptionToken value
	 */
	public void setResumptionToken(String resumptionToken) {
		this.resumptionToken = resumptionToken;
	}


	/**
	 *  Gets the resumptionToken that should be returned with the results.
	 *
	 * @return    The resumptionToken value
	 */
	public String getResumptionToken() {
		if (resumptionToken == null)
			return "";
		else
			return resumptionToken;
	}


	/**
	 *  Sets the array index offset for the result set that should be returned to the harvester.
	 *
	 * @param  resultsOffset  The new resultsOffset value
	 */
	public void setResultsOffset(String resultsOffset) {
		this.resultsOffset = resultsOffset;
	}


	/**
	 *  Gets the array index offset for the result set that should be returned to the harvester.
	 *
	 * @return    The resultsOffset value
	 */
	public String getResultsOffset() {
		//prtln("getResultsOffset() is: " + resultsOffset);
		if (resultsOffset == null)
			return "0";
		else
			return resultsOffset;
	}



	/**
	 *  Sets the total number of results that should be returned to the harvester.
	 *
	 * @param  resultsLength  The new resultsLength value
	 */
	public void setResultsLength(String resultsLength) {
		this.resultsLength = resultsLength;
	}



	/**
	 *  Gets the total number of results that should be returned to the harvester.
	 *
	 * @return    The resultsLength value
	 */
	public String getResultsLength() {
		//prtln("getResultsLength() is: " + resultsLength);
		if (resultsLength == null)
			return "0";
		else
			return resultsLength;
	}



	/**
	 *  Sets the requestedFormat attribute of the RepositoryForm object
	 *
	 * @param  requestedFormat  The new requestedFormat value
	 */
	public void setRequestedFormat(String requestedFormat) {
		this.requestedFormat = requestedFormat;
	}



	/**
	 *  Gets the requestedFormat attribute of the RepositoryForm object
	 *
	 * @return    The requestedFormat value
	 */
	public String getRequestedFormat() {
		if (requestedFormat == null)
			return "0";
		else
			return requestedFormat;
	}



	/**
	 *  Gets the repositoryIdentifier attribute of the RepositoryForm object
	 *
	 * @return    The repositoryIdentifier value
	 */
	public String getRepositoryIdentifier() {
		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return "";

		return rm.getRepositoryIdentifier();
	}


	/**
	 *  Gets the protocolVersion attribute of the RepositoryForm object
	 *
	 * @return    The protocolVersion value
	 */
	public String getProtocolVersion() {
		return properties.getProperty("identify.protocolversion");
	}


	/**
	 *  Gets the providerStatus attribute of the RepositoryForm object
	 *
	 * @return    The providerStatus value
	 */
	public String getProviderStatus() {
		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return "";

		return rm.getProviderStatus();
	}


	/**
	 *  Gets the adminEmails attribute of the RepositoryForm object. If this is the first run, the admin e-mail
	 *  will be set to the default value.
	 *
	 * @return    The adminEmails value
	 */
	public ArrayList getAdminEmails() {
		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return new ArrayList();

		return rm.getAdminEmails();
	}


	/**
	 *  Gets the sets attribute of the RepositoryForm object
	 *
	 * @return    The sets value
	 */
	public ArrayList getSets() {
		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return new ArrayList();

		try {
			ArrayList sets = rm.getSetInfos();
			if (sets == null)
				return new ArrayList();
			else
				return sets;
		} catch (Throwable e) {
			prtlnErr("getSets() error: " + e);
			return new ArrayList();
		}
	}


	/*
	 *  public SetInfo getSet(int i) {
	 *  return (SetInfo)rm.getSetInfos().get(i);
	 *  }
	 *  public SetInfo getSet(Integer i) {
	 *  return getSet(i.intValue());
	 *  }
	 */
	/**
	 *  Gets the earliestDatestamp attribute of the RepositoryForm object
	 *
	 * @return    The earliestDatestamp value
	 */
	public String getEarliestDatestamp() {
		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return "";
		return rm.getEarliestDatestamp();
	}


	/**
	 *  Gets the deletedRecord attribute of the RepositoryForm object. Legitimate values are no ; transient ;
	 *  persistent
	 *
	 * @return    The deletedRecord value
	 */
	public String getDeletedRecord() {
		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return "";
		return rm.getDeletedRecord();
	}


	/**
	 *  Gets the metadata schema URLs keyed by metadataPrefix.
	 *
	 * @return    The metadata schema URLs or null if none
	 */
	public Map getMetadataSchemaURLs() {
		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return null;
		return rm.getMetadataSchemaURLs();
	}


	/**
	 *  Gets the metadata namespacess keyed by metadataPrefix.
	 *
	 * @return    The metadata namespacess or null if none
	 */
	public Map getMetadataNamespaces() {
		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return null;
		return rm.getMetadataNamespaces();
	}


	/**
	 *  Gets the granularity attribute of the RepositoryForm object. Legitimate values are YYYY-MM-DD and
	 *  YYYY-MM-DDThh:mm:ssZ with meanings as defined in ISO8601.
	 *
	 * @return    The granularity value
	 */
	public String getGranularity() {
		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return "";
		return rm.getGranularity();
	}


	/**
	 *  Gets the optional compressionTag attribute of the RepositoryForm object. "none" indicates no compression
	 *  is supported, resulting in no compression tag being output. Other possible values may include: gzip ;
	 *  compress as defined in <a href="http://www.ietf.org/rfc/rfc2616.txt?number=2616">RFC 2616</a> . There can
	 *  be multiple supported compression schemes.
	 *
	 * @return    An ArrayList of the supported compression schemes or null if none are supported.
	 */
	public ArrayList getCompressions() {
		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return new ArrayList();
		return rm.getCompressions();
	}


	/**
	 *  Gets the optional descriptions that relate to this repository. See <a
	 *  href="http://www.openarchives.org/OAI/2.0/guidelines.htm">OAI implemnetation guidelines</a> for details.
	 *  The info here is the description container (the part between the <code>&lt;description&gt;<code> tag).
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 * @return    An ArrayList of descriptions for this repository.
	 */
	public ArrayList getDescriptions() {
		RepositoryManager rm =
			(RepositoryManager) getServlet().getServletContext().getAttribute("repositoryManager");
		if (rm == null)
			return new ArrayList();
		return rm.getDescriptions();
	}


	/**
	 *  Sets the results attribute of the RepositoryForm object
	 *
	 * @param  results  The new results value
	 */
	public void setResults(ResultDocList results) {
		this.results = results;
	}


	/**
	 *  Gets the results attribute of the RepositoryForm object
	 *
	 * @return    The results value
	 */
	public ResultDocList getResults() {
		if (results == null)
			return new ResultDocList();
		else
			return results;
	}


	/**
	 *  Gets the record attribute of the RepositoryForm object used in the OAI GetRecord request.
	 *
	 * @return    The record value
	 */
	public String getRecord() {
		return record;
	}


	/**
	 *  Sets the record attribute of the RepositoryForm object used in the OAI GetRecord request.
	 *
	 * @param  record  The new record value
	 */
	public void setRecord(String record) {
		this.record = record;
	}


	/**
	 *  Gets the identifier used in OAI requests.
	 *
	 * @return    The record value
	 */
	public String getIdentifier() {
		if (identifier == null)
			return "";
		return identifier;
	}


	/**
	 *  Sets the record attribute of the RepositoryForm object used in the OAI GetRecord request.
	 *
	 * @param  id  The new identifier value
	 */
	public void setIdentifier(String id) {
		this.identifier = id;
	}


	/**
	 *  Gets the oaiIdPfx attribute of the RepositoryForm object
	 *
	 * @return    The oaiIdPfx value
	 */
	public String getOaiIdPfx() {
		if (oaiIdPfx == null)
			return "";
		return oaiIdPfx;
	}


	/**
	 *  Sets the oaiIdPfx attribute of the RepositoryForm object
	 *
	 * @param  oaiIdPfx  The new oaiIdPfx value
	 */
	public void setOaiIdPfx(String oaiIdPfx) {
		this.oaiIdPfx = oaiIdPfx;
	}


	/**
	 *  Gets the datestamp attribute of the RepositoryForm object
	 *
	 * @return    The datestamp value
	 */
	public String getDatestamp() {
		if (datestamp == null)
			return "";
		return datestamp;
	}


	/**
	 *  Sets the datestamp attribute of the RepositoryForm object
	 *
	 * @param  s  The new datestamp value
	 */
	public void setDatestamp(String s) {
		this.datestamp = s;
	}


	/**
	 *  Gets the setSpecs attribute of the RepositoryForm object
	 *
	 * @return    The setSpecs value
	 */
	public List getSetSpecs() {
		if (setSpecs == null)
			return new ArrayList();
		return setSpecs;
	}


	/**
	 *  Sets the setSpecs attribute of the RepositoryForm object
	 *
	 * @param  s  The new setSpecs value
	 */
	public void setSetSpecs(List s) {
		setSpecs = s;
	}


	// --------------- Other utility methods ----------------------

	/**
	 *  Provides HTML encoding for XML resulting in text that looks good in a Web browser. Uses the indentation
	 *  that existed in the original text. Use this method to encode text that will be displayed in a web browser
	 *  (e.g. JSP or HTML page).
	 *
	 * @param  s  The string to convert.
	 * @return    A string suitable for display as HTML
	 */
	public String xmlToHtml(String s) {
		return OutputTools.xmlToHtml(s);
	}


	//================================================================

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected static String getDs() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final void prtlnErr(String s) {
		System.err.println(getDs() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug)
			System.out.println(getDs() + " " + s);
	}


	/**
	 *  Sets the debug attribute of the DocumentService object
	 *
	 * @param  isDebugOuput  The new debug value
	 */
	public static void setDebug(boolean isDebugOuput) {
		debug = isDebugOuput;
	}
}



