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
package org.dlese.dpc.repository.action;

import org.dlese.dpc.repository.action.form.*;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.dds.action.DDSQueryAction;
import org.dlese.dpc.services.dds.action.DDSServicesAction;
import org.dlese.dpc.webapps.servlets.filters.GzipFilter;

import java.util.*;
import java.text.*;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

/**
 *  Implementation of <strong>Action</strong> that handles all OAI-PMH and ODL requests.
 *
 * @author     John Weatherley
 * @version    $Id: RepositoryAction.java,v 1.37 2010/07/14 00:18:49 jweather Exp $
 */
public final class RepositoryAction extends Action {
	private static boolean debug = false;
	private SimpleLuceneIndex webLogIndex;
	private WebLogWriter webLogWriter = new WebLogWriter();

	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the OAI-PMH request and creates the corresponding HTTP response by forwarding to a JSP page for
	 *  rendering.
	 *
	 * @param  mapping        The ActionMapping used to select this instance
	 * @param  request        The HTTP request we are processing
	 * @param  response       The HTTP response we are creating
	 * @param  form           The ActionForm for the given page
	 * @return                The ActionForward instance describing where and how control should be forwarded
	 * @exception  Exception  If error.
	 */
	public ActionForward execute(
	                             ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response)
		 throws Exception {
		/*
		 *  Design note:
		 *  Only one instance of this class gets created for the app and shared by
		 *  all threads. To be thread-safe, use only local variables, not instance
		 *  variables (the JVM will handle these properly using the stack). Pass
		 *  all variables via method signatures rather than instance vars.
		 */
		try {

			Locale locale = getLocale(request);
			MessageResources messages = getResources(request);
			RepositoryForm rf = (RepositoryForm) form;

			int numListIdentifiersResults = 1000;
			int numListRecordsResults = 300;

			//Common.initContextUrl(request);

			// Set the return type, for debugging:
			if (true) {
				request.setAttribute("rt", "text");
			}

			// Basic OAI request validation:
			Enumeration params = request.getParameterNames();
			int num_args = 0;

			// A count of the number of args that aren't "rt" or "verb" for further validation
			String arg = null;
			String param = null;

			// Echo the request params for debugging
			/* prtln("Params: ");
			while (params.hasMoreElements()) {
				arg = ((String) params.nextElement());
				prtln(arg + "=" + request.getParameter(arg));
				if (!arg.equals("rt") && !arg.equals(OAIArgs.VERB))
					num_args++;
			} */
			RepositoryManager rm =
				(RepositoryManager) servlet.getServletContext().getAttribute("repositoryManager");

			webLogIndex =
				(SimpleLuceneIndex) servlet.getServletContext().getAttribute("webLogIndex");

			// Set up data in the bean
			rf.setBaseURL(rm.getProviderBaseUrl(request));
			try {
				numListIdentifiersResults = Integer.parseInt(rm.getNumIdentifiersResults());
				numListRecordsResults = Integer.parseInt(rm.getNumRecordsResults());
			} catch (NumberFormatException e) {
				prtln("Error parsing int from String: " + e);
			}

			// Grab the OAI-PMH verb, and validate:
			String verb = request.getParameter(OAIArgs.VERB);
			if (verb == null) {
				rf.addOaiError(OAICodes.BAD_VERB, "The verb argument is required but does not exist");
				return (mapping.findForward("oaipmh.error"));
			}
			
			// Ensure no more than one verb indicated:
			String [] verbs = request.getParameterValues(OAIArgs.VERB);
			if(verbs.length > 1) {
				rf.addOaiError(OAICodes.BAD_VERB, "Only one verb argument must be present, but " + verbs.length + " were found.");
				return (mapping.findForward("oaipmh.error"));				
			}
			
			// Basic validation of request arguments:
			boolean hasNonValidArgument = validateRequestArguments(request,rf);
			if (hasNonValidArgument)
				return (mapping.findForward("oaipmh.error"));

			// Handle disabled status:
			if (rm.getProviderStatus().toLowerCase().equals("disabled")) {
				return (mapping.findForward("oaipmh.disabled"));
			}
			// Handle the OAI-PMH requests:
			else if (verb.equals(OAIArgs.IDENTIFY)) {
				return doIdentify(request, mapping, num_args, rm, rf);
			}
			else if (verb.equals(OAIArgs.GET_RECORD)) {
				return doGetRecord(request, mapping, num_args, rm, rf);
			}
			else if (verb.equals(OAIArgs.LIST_RECORDS)) {
				return doListRecordsOrIdentifiers(request, mapping, num_args, rm, rf, OAIArgs.LIST_RECORDS, numListRecordsResults);
			}
			else if (verb.equals(OAIArgs.LIST_IDENTIFIERS)) {
				return doListRecordsOrIdentifiers(request, mapping, num_args, rm, rf, OAIArgs.LIST_IDENTIFIERS, numListIdentifiersResults);
			}
			else if (verb.equals(OAIArgs.LIST_METADATA_FORMATS)) {
				return doListMetadataFormats(request, mapping, num_args, rm, rf);
			}
			else if (verb.equals(OAIArgs.LIST_SETS)) {
				return doListSets(request, mapping, num_args, rm, rf);
			}
			// The verb is not valid for OAI-PMH
			else {
				rf.addOaiError(OAICodes.BAD_VERB, "The verb '" + verb + "' is illegal");
				return (mapping.findForward("oaipmh.error"));
			}
		} catch (NullPointerException npe) {
			prtln("ProviderAction caught exception. " + npe);
			npe.printStackTrace();
			return null;
		} catch (Throwable e) {
			prtln("ProviderAction caught exception. " + e);
			return null;
		}
	}



	/**
	 *  Handles the OAI Identify request. From the spec, this request has the following properties: <p>
	 *
	 *  Arguments: None <p>
	 *
	 *  Error Exception Conditions: <br>
	 *  badArgument - The request includes illegal arguments.
	 *
	 * @param  request   The HTTP request.
	 * @param  mapping   The Struts ActionMapping used for forwarding.
	 * @param  num_args  The number of arguments other than the OAI verb or rt.
	 * @param  rm        The model used to fetch OAI data.
	 * @param  rf        The bean used to handle the response.
	 * @return           The page to forward to that will render the response, or null if handled here.
	 */
	protected ActionForward doIdentify(
	                                   HttpServletRequest request,
	                                   ActionMapping mapping,
	                                   int num_args,
	                                   RepositoryManager rm,
	                                   RepositoryForm rf) {

		if (num_args > 0) {
			rf.addOaiError(
				OAICodes.BAD_ARGUMENT,
				"There must be no arguments included with the '" +
				OAIArgs.IDENTIFY +
				"' request");
		}

		if (rf.hasErrors()) {
			logRequest(request, OAIArgs.IDENTIFY, rf.getErrors());
			return (mapping.findForward("oaipmh.error"));
		}
		else {
			logRequest(request, OAIArgs.IDENTIFY);
			return (mapping.findForward("oaipmh." + OAIArgs.IDENTIFY));
		}
	}


	/**
	 *  Handles the OAI GetRecord request. From the spec, this request has the following properties: <p>
	 *
	 *  Arguments:<br>
	 *  identifier - a required argument that specifies the unique identifier of the item in the repository from
	 *  which the record must be disseminated.<br>
	 *  metadataPrefix - a required argument that specifies the metadataPrefix of the format that should be
	 *  included in the metadata part of the returned record . A record should only be returned if the format
	 *  specified by the metadataPrefix can be disseminated from the item identified by the value of the
	 *  identifier argument. The metadata formats supported by a repository and for a particular record can be
	 *  retrieved using the ListMetadataFormats request.<p>
	 *
	 *  Error and Exception Conditions:<br>
	 *  badArgument - The request includes illegal arguments or is missing required arguments.<br>
	 *  cannotDisseminateFormat - The value of the metadataPrefix argument is not supported by the item
	 *  identified by the value of the identifier argument <br>
	 *  idDoesNotExist - The value of the identifier argument is unknown or illegal in this repository.<br>
	 *
	 *
	 * @param  request   The HTTP request.
	 * @param  mapping   The Struts ActionMapping used for forwarding.
	 * @param  num_args  The number of arguments other than the OAI verb or rt.
	 * @param  rm        The model used to fetch OAI data.
	 * @param  rf        The bean used to handle the response.
	 * @return           The page to forward to that will render the response, or null if handled here.
	 */
	protected ActionForward doGetRecord(
	                                    HttpServletRequest request,
	                                    ActionMapping mapping,
	                                    int num_args,
	                                    RepositoryManager rm,
	                                    RepositoryForm rf) {

		String identifier = request.getParameter(OAIArgs.IDENTIFIER);
		if (identifier != null)
			identifier = identifier.trim();
		String format = request.getParameter(OAIArgs.METADATA_PREFIX);

		if (identifier == null)
			rf.addOaiError(OAICodes.BAD_ARGUMENT,
				"GetRecord must include the argument '" + OAIArgs.IDENTIFIER + "'");
		else if (identifier.length() == 0)
			rf.addOaiError(OAICodes.BAD_ARGUMENT,
				"The argument '" + OAIArgs.IDENTIFIER + "' is required but was empty");
		if (format == null)
			rf.addOaiError(OAICodes.BAD_ARGUMENT,
				"GetRecord must include the argument '" + OAIArgs.METADATA_PREFIX + "'");
		if (identifier != null &&
			format != null &&
			num_args > 2)
			rf.addOaiError(OAICodes.BAD_ARGUMENT,
				"The " + OAIArgs.GET_RECORD +
				" request must only include the arguments '" +
				OAIArgs.IDENTIFIER +
				"' and '" +
				OAIArgs.METADATA_PREFIX + "'");

		if (identifier != null && identifier.length() > 0 && format != null) {
			String oaiIdPfx = rm.getOaiIdPrefix();
			String content = null;
			String datestamp = null;
			List setSpecs = null;
			String idStripped = null;
			// Remove the oai-identifier prefix prior to query:
			if (oaiIdPfx.length() > 0) {
				try {
					idStripped = identifier.replaceFirst(oaiIdPfx, "");
				} catch (Throwable e) {}
			}
			else
				idStripped = identifier;

			// Query for a result:
			ResultDoc resultDoc = rm.getRecordOai(idStripped);
			boolean statusDeleted = false;
			boolean isMySetDisabled = true;
			if (resultDoc != null) {
				DocReader docReader = resultDoc.getDocReader();
				if (docReader instanceof XMLDocReader) {
					XMLDocReader reader = (XMLDocReader) resultDoc.getDocReader();
					if (reader != null) {
						statusDeleted = reader.isDeleted();
						isMySetDisabled = reader.getIsMyCollectionDisabled();
						if (!statusDeleted && !isMySetDisabled)
							content = reader.getXmlFormat(format, true);
						datestamp = reader.getOaiDatestamp();
						setSpecs = reader.getOaiSets();
					}
				}
			}

			// If set is disabled, return ID does not exist:
			if (isMySetDisabled) {
				rf.addOaiError(OAICodes.ID_DOES_NOT_EXIST,
					"The identifier '" + identifier +
					"' does not exist in this repository.");
			}
			else {
				if (!statusDeleted && (content == null || content.trim().length() == 0)) {
					if (!rm.isIdInRepository(idStripped))
						rf.addOaiError(OAICodes.ID_DOES_NOT_EXIST,
							"The identifier '" + identifier +
							"' does not exist in this repository.");
					else
						rf.addOaiError(OAICodes.CANNOT_DISSEMINATE_FORMAT,
							"The format '" + format +
							"' is not available for identifier '" +
							identifier + "'");
				}
				// Put the data into the bean:
				else {
					rf.setIdentifier(idStripped);
					if (statusDeleted)
						rf.setDeletedStatus("true");
					else
						rf.setDeletedStatus("false");
					rf.setRecord(content);
					rf.setDatestamp(datestamp);
					rf.setSetSpecs(setSpecs);
					rf.setOaiIdPfx(oaiIdPfx);
				}
			}
		}

		if (rf.hasErrors()) {
			logRequest(request, OAIArgs.GET_RECORD, rf.getErrors());
			return (mapping.findForward("oaipmh.error"));
		}
		else {
			logRequest(request, OAIArgs.GET_RECORD);
			return (mapping.findForward("oaipmh." + OAIArgs.GET_RECORD));
		}
	}


	/**
	 *  Handles the OAI ListMetadataFormats request. From the spec, this request has the following properties:
	 *  <p>
	 *
	 *  Arguments:<br>
	 *  identifier - an optional argument that specifies the unique identifier of the item for which available
	 *  metadata formats are being requested. If this argument is omitted, then the response includes all
	 *  metadata formats supported by this repository. Note that the fact that a metadata format is supported by
	 *  a repository does not mean that it can be disseminated from all items in the repository.<p>
	 *
	 *  Error and Exception Conditions:<br>
	 *  badArgument - The request includes illegal arguments or is missing required arguments.<br>
	 *  idDoesNotExist - The value of the identifier argument is unknown or illegal in this repository.<br>
	 *  noMetadataFormats - There are no metadata formats available for the specified item. <br>
	 *
	 *
	 * @param  request   The HTTP request.
	 * @param  mapping   The Struts ActionMapping used for forwarding.
	 * @param  num_args  The number of arguments other than the OAI verb or rt.
	 * @param  rm        The model used to fetch OAI data.
	 * @param  rf        The bean used to handle the response.
	 * @return           The page to forward to that will render the response, or null if handled here.
	 */
	protected ActionForward doListMetadataFormats(
	                                              HttpServletRequest request,
	                                              ActionMapping mapping,
	                                              int num_args,
	                                              RepositoryManager rm,
	                                              RepositoryForm rf) {

		if (request.getParameter(OAIArgs.IDENTIFIER) == null &&
			num_args >= 1)
			rf.addOaiError(OAICodes.BAD_ARGUMENT,
				"The " + OAIArgs.LIST_METADATA_FORMATS +
				" request may only include the optional argument '" +
				OAIArgs.IDENTIFIER + "'");

		String originalIdentifier = request.getParameter(OAIArgs.IDENTIFIER);
		String identifier = originalIdentifier;
		if (identifier != null) {
			// Remove the oai-identifier prefix prior to query:
			String oaiIdPfx = rm.getOaiIdPrefix();
			if (oaiIdPfx.length() > 0) {
				try {
					identifier = identifier.replaceFirst(oaiIdPfx, "");
				} catch (Throwable e) {}
			}
			
			// Verify that the record exists:
			ResultDocList resultDocs = rm.getIndex().searchDocs("id:" + SimpleLuceneIndex.encodeToTerm(identifier));
			if(resultDocs == null || resultDocs.size() == 0)
				rf.addOaiError(OAICodes.ID_DOES_NOT_EXIST, "The identifier '" + originalIdentifier + "' does not exist.");
			else
				rf.setMetadataFormats(rm.getAvailableFormats(identifier));
		}
		else
			rf.setMetadataFormats(rm.getAvailableFormats());

		if (rf.hasErrors()) {
			logRequest(request, OAIArgs.LIST_METADATA_FORMATS, rf.getErrors());
			return (mapping.findForward("oaipmh.error"));
		}
		else {
			logRequest(request, OAIArgs.LIST_METADATA_FORMATS);
			return (mapping.findForward("oaipmh." + OAIArgs.LIST_METADATA_FORMATS));
		}
	}


	/**
	 *  Handles the OAI ListRecords or ListIdentifiers requests. From the spec, these requests have the following
	 *  properties: <p>
	 *
	 *  Arguments:<br>
	 *  from - an optional argument with a UTCdatetime value , which specifies a lower bound for datestamp-based
	 *  selective harvesting.<br>
	 *  until - an optional argument with a UTCdatetime value , which specifies a upper bound for datestamp-based
	 *  selective harvesting.<br>
	 *  metadataPrefix - a required argument, which specifies that headers should be returned only if the
	 *  metadata format matching the supplied metadataPrefix is available or, depending on the repository's
	 *  support for deletions, has been deleted. The metadata formats supported by a repository and for a
	 *  particular item can be retrieved using the ListMetadataFormats request.<br>
	 *  set - an optional argument with a setSpec value , which specifies set criteria for selective harvesting.
	 *  <br>
	 *  resumptionToken - an exclusive argument with a value that is the flow control token returned by a
	 *  previous ListIdentifiers request that issued an incomplete list.<p>
	 *
	 *  Error and Exception Conditions:<br>
	 *  badArgument - The request includes illegal arguments or is missing required arguments.<br>
	 *  badResumptionToken - The value of the resumptionToken argument is invalid or expired. <br>
	 *  cannotDisseminateFormat - The value of the metadataPrefix argument is not supported by the repository.
	 *  <br>
	 *  noRecordsMatch- The combination of the values of the from, until , and set arguments results in an empty
	 *  list.<br>
	 *  noSetHierarchy - The repository does not support sets.<br>
	 *
	 *
	 * @param  request      The HTTP request.
	 * @param  mapping      The Struts ActionMapping used for forwarding.
	 * @param  num_args     The number of arguments other than the OAI verb or rt.
	 * @param  rm           The model used to fetch OAI data.
	 * @param  rf           The bean used to handle the response.
	 * @param  numResults   The number of results to provide in the response before returning a resumptionToken.
	 * @param  requestType  The requestType, which must be either ListIdentifiers or ListRecords.
	 * @return              The page to forward to that will render the response, or null if handled here.
	 */
	protected ActionForward doListRecordsOrIdentifiers(
	                                                   HttpServletRequest request,
	                                                   ActionMapping mapping,
	                                                   int num_args,
	                                                   RepositoryManager rm,
	                                                   RepositoryForm rf,
	                                                   String requestType,
	                                                   int numResults) {

		String until = request.getParameter(OAIArgs.UNTIL);
		String from = request.getParameter(OAIArgs.FROM);
		String format = request.getParameter(OAIArgs.METADATA_PREFIX);
		String set = request.getParameter(OAIArgs.SET);
		String resumptionToken = request.getParameter(OAIArgs.RESUMPTION_TOKEN);
		ResultDocList results = null;
		ResumptionTokenHandler rth = null;
		ODLSearchHandler odl = null;
		String requestTypeForLog = requestType;

		if (resumptionToken != null &&
			(set != null || format != null || from != null || until != null)) {
			rf.addOaiError(OAICodes.BAD_ARGUMENT,
				"The argument '" + OAIArgs.RESUMPTION_TOKEN +
				"' must be supplied without other arguments");
		}
		else if (format == null && resumptionToken == null)
			rf.addOaiError(OAICodes.BAD_ARGUMENT,
				"Missing argument. " + requestType + " must include the argument '" +
				OAIArgs.METADATA_PREFIX + "' or '" + OAIArgs.RESUMPTION_TOKEN + "'");
		else {
			if (format != null && !rm.canDisseminateFormat(format))
				rf.addOaiError(OAICodes.CANNOT_DISSEMINATE_FORMAT,
					"This repository has no items available in format '" + format + "'");
		}

		// -------  Handle ODL search ---------

		if (set != null && set.startsWith("dleseodlsearch")) {
			requestTypeForLog = requestTypeForLog + "ODL";
			try {
				odl = new ODLSearchHandler(set);
			} catch (Exception e) {
				rf.addOaiError(OAICodes.BAD_ARGUMENT,
					"Error in the ODL parameter: " + e.getMessage() +
					". The ODL parameter must be of the form " +
					"set=dleseodlsearch/[query string]/[set = setSpec or null for all]/[int = offset to start results]/[int = number of result to return]");
			}

			// Limit max results to the value specified...
			if (!rf.hasErrors() && odl.getNumResultsToReturn() > Integer.parseInt(rm.getNumRecordsResults())) {
				rf.addOaiError(OAICodes.BAD_ARGUMENT,
					"Error in the ODL parameter: the maximum allowable length of results to return is " + rm.getNumRecordsResults() +
					", however the number requested was " + odl.getNumResultsToReturn());
			}

			// Query the provider and get the results:
			if (!rf.hasErrors() && odl != null) {
				try {
					results = rm.getOdlQueryResults(format, odl.getSet(), from, until, odl.getQueryString());
				} catch (Exception e) {
					//prtln("throwing bad argument error..." + e);
					//e.printStackTrace();

					// Note: this should return http type 500
					rf.addOaiError(OAICodes.BAD_ARGUMENT, e.getMessage());
				}
			}
			// Construct the response
			if (!rf.hasErrors() && odl != null) {

				// Log the query that the client requested:
				DDSQueryAction.logQuery(odl.getQueryString(),
					servlet.getServletContext(),
					request,
					200,
					-1,
					(results == null ? 0 : results.size()),
					DDSServicesAction.getNumRecords(rm),
					odl.getOffsetInt(),
					DDSQueryAction.SEARCHTYPE_ODL_SEARCH);

				// If no results:
				if (results == null || results.size() == 0)
					if (odl.getSet() == null)
						rf.addOaiError(OAICodes.NO_RECORDS_MATCH,
							"The odl search query '" + odl.getQueryString() +
							"' with format '" + format + "' had no matches.");
					else
						rf.addOaiError(OAICodes.NO_RECORDS_MATCH,
							"The odl search query '" + odl.getQueryString() +
							"' with set '" + odl.getSet() +
							"' and format '" + format + "' had no matches.");
				else if (results.size() <= odl.getOffsetInt()) {
					rf.addOaiError(OAICodes.NO_RECORDS_MATCH,
						"There were " + results.size() + " matching results, " +
						" however he requested offset of " + odl.getOffsetInt() +
						" is greater than or equal to the number of results.");
				}
				// If one or more results:
				else {
					rf.setResumptionToken("<resumptionToken completeListSize=\"" + results.size() + "\" cursor=\"" + odl.getOffset() + "\" />");
					rf.setResultsLength(odl.getLength());
					rf.setResultsOffset(odl.getOffset());
					rf.setRequestedFormat(format);
					rf.setResults(results);
					rf.setOaiIdPfx(rm.getOaiIdPrefix());
				}
			}
		}

		// -------  Handle standard OAI-PMH ---------

		else {
			requestTypeForLog = requestTypeForLog + "NoResumption";

			// If regular OAI-PMH ListRecords and ListIdentifiers requests are disabled, indicate as such
			if (!rm.getIsOaiPmhEnabled()) {
				rf.addOaiError(OAICodes.BAD_ARGUMENT, "This data provider only accepts ODL search requests. " +
					"Regular ListRecords and ListIdentifiers requests are disabled.");
			}

			// Handle resumptionTokens:
			if (!rf.hasErrors()) {
				if (resumptionToken != null) {
					requestTypeForLog = requestTypeForLog + "WithResumption";
					try {
						rth = new ResumptionTokenHandler(resumptionToken, numResults);
					} catch (Exception e) {
						rf.addOaiError(OAICodes.BAD_RESUMPTION_TOKEN,
							"The '" + OAIArgs.RESUMPTION_TOKEN + "' argument is unrecognizable");
					}
				}
				else
					rth = new ResumptionTokenHandler(set, format, from, until, numResults);
			}

			// Query the provider and get the results:
			if (!rf.hasErrors() && rth != null) {
				try {
					results = rm.getOaiQueryResults(rth.getFormat(), rth.getSet(), rth.getFrom(), rth.getUntil());
					//if (results != null)
					//prtln("Number of results: " + results.length);
				} catch (OAIErrorException oai_e) {
					rf.addOaiError(oai_e.getErrorCode(), oai_e.getMessage());
				} catch (Throwable e) {
					e.printStackTrace();
					//prtlnErr("Query error: " + e);

					// Note: this should return http 500 instead...
					rf.addOaiError(OAICodes.CANNOT_DISSEMINATE_FORMAT, "There was an internal server error: " + e);
				}
			}
			// Compile the response:
			if (!rf.hasErrors() && rth != null) {
				// If no results:
				if (results == null || results.size() == 0) {
					String msg = "";
					if (rth.getFrom() != null)
						msg += " from " + rth.getFrom() + ", ";
					if (rth.getUntil() != null)
						msg += " until " + rth.getUntil() + ", ";
					if (rth.getSet() != null)
						msg += " set " + rth.getSet() + ", ";
					msg += " format " + rth.getFormat() + ".";
					rf.addOaiError(OAICodes.NO_RECORDS_MATCH,
						"There are no matching records for request: " + msg);
				}
				// If one or more results:
				else {
					if (rth.isLastToken(results.size()))
						requestTypeForLog = requestTypeForLog + "FinalResumption";
					rf.setResumptionToken(rth.getNextToken(results.size()));
					rf.setResultsLength(rth.getResultsLength());
					rf.setResultsOffset(rth.getResultsOffset(results.size()));
					rf.setRequestedFormat(rth.getFormat());
					rf.setResults(results);
					rf.setOaiIdPfx(rm.getOaiIdPrefix());
				}
			}
		}

		if (rf.hasErrors()) {
			logRequest(request, requestTypeForLog, rf.getErrors());
			return (mapping.findForward("oaipmh.error"));
		}

		// Forward to either ListIdentifiers or ListRecords jsp handlers.
		else {
			String nrtr = "";
			if (rth != null)
				nrtr = rth.getNumRecordsBeingReturned(results.size());
			if (odl != null)
				nrtr = odl.getNumRecordsBeingReturned(results.size());
			logRequest(request,
				requestTypeForLog,
				null,
				Integer.toString(results.size()),
				rf.getResultsOffset(),
				nrtr);
			return (mapping.findForward("oaipmh." + requestType));
		}
	}


	/**
	 *  Logs an OAI request to an index for tracking.
	 *
	 * @param  request      The HTTP request.
	 * @param  requestType  The request type, for example Identify.
	 */
	private final void logRequest(HttpServletRequest request, String requestType) {
		logRequest(request, requestType, null);
	}



	/**
	 *  Logs an OAI request to an index for tracking.
	 *
	 * @param  request      The request
	 * @param  requestType  The type of OAI request (ListRecords, GetRecord, etc)
	 * @param  errors       If error
	 */
	private final void logRequest(
	                              HttpServletRequest request,
	                              String requestType,
	                              ArrayList errors) {
		logRequest(request, requestType, errors, null, null, null);
	}


	/**
	 *  Logs an OAI request to an index for tracking.
	 *
	 * @param  request             The request.
	 * @param  requestType         The request type, for example Identify or ListRecords.
	 * @param  errors              OAI errors, or null.
	 * @param  repositorySize      Size of the repository, or null.
	 * @param  resultsOffset       Offset to the beginning of the results returned, or null.
	 * @param  numResultsReturned  The total number of results being returned to the client, or null.
	 */
	private final void logRequest(
	                              HttpServletRequest request,
	                              String requestType,
	                              ArrayList errors,
	                              String repositorySize,
	                              String resultsOffset,
	                              String numResultsReturned) {
		if (webLogIndex == null)
			return;
		StringBuffer webNote = new StringBuffer();
		if (errors != null && errors.size() != 0) {
			webNote.append(requestType + " request resulted in the followng OAI errors: ");
			for (int i = 0; i < errors.size(); i++) {
				OAIError error = (OAIError) errors.get(i);
				webNote.append(error.getErrorCode() + " - " + error.getMessage() + ". ");

			}
		}
		else {
			webNote.append(requestType + " request was successful.");
			if (resultsOffset != null && numResultsReturned != null && repositorySize != null)
				webNote.append(" " + numResultsReturned +
					" records were returned out of " + repositorySize + ", beginning at record number " +
					resultsOffset + ".");
		}
		if (GzipFilter.isGzipSupported(request))
			webNote.append(" Compression: gzip.");
		else
			webNote.append(" Compression: none.");

		boolean doLog = false;
		String logLevel = getServlet().getServletContext().getInitParameter("dataProviderAccessLogLevel");
		if (logLevel != null) {
			if (logLevel.equalsIgnoreCase("Full"))
				doLog = true;
			else if (logLevel.equalsIgnoreCase("FinalResumption") && requestType.lastIndexOf("FinalResumption") > 0)
				doLog = true;
			else if (logLevel.equalsIgnoreCase("NoLog"))
				doLog = false;
		}
		//prtln("logging request '" + webNote + "' log level: " + logLevel + " means: " + doLog);
		if (doLog)
			webLogIndex.addDoc(webLogWriter.log(request, webNote.toString()), false);
	}


	/**
	 *  Handles the OAI ListSets request. From the spec, this request has the following properties: <p>
	 *
	 *  Arguments<br>
	 *  resumptionToken - an exclusive argument with a value that is the flow control token returned by a
	 *  previous ListSets request that issued an incomplete list<p>
	 *
	 *  Error and Exception Conditions<br>
	 *  badArgument - The request includes illegal arguments or is missing required arguments.<br>
	 *  badResumptionToken - The value of the resumptionToken argument is invalid or expired. <br>
	 *  noSetHierarchy - The repository does not support sets.<br>
	 *
	 *
	 * @param  request   The HTTP request.
	 * @param  mapping   The Struts ActionMapping used for forwarding.
	 * @param  num_args  The number of arguments other than the OAI verb or rt.
	 * @param  rm        The model used to fetch OAI data.
	 * @param  rf        The bean used to handle the response.
	 * @return           The page to forward to that will render the response, or null if handled here.
	 */
	protected ActionForward doListSets(
	                                   HttpServletRequest request,
	                                   ActionMapping mapping,
	                                   int num_args,
	                                   RepositoryManager rm,
	                                   RepositoryForm rf) {
		//prtln("doListSets()");

		if (request.getParameter(OAIArgs.RESUMPTION_TOKEN) == null &&
			num_args >= 1)
			rf.addOaiError(OAICodes.BAD_ARGUMENT,
				"The " + OAIArgs.LIST_SETS +
				" request may only include the argument '" +
				OAIArgs.RESUMPTION_TOKEN + "'");

		if (!rm.getHasOaiSetsConfigured())
			rf.addOaiError(OAICodes.NO_SET_HIERARCHY,
				"This repository does not currently have any sets defined");

		// All info is extracted directly from the rm so no set up here...

		if (rf.hasErrors()) {
			logRequest(request, OAIArgs.LIST_SETS, rf.getErrors());
			return (mapping.findForward("oaipmh.error"));
		}
		else {
			logRequest(request, OAIArgs.LIST_SETS);
			return (mapping.findForward("oaipmh." + OAIArgs.LIST_SETS));
		}
	}


	/**
	 *  Handles parsing ODL search requests.
	 *
	 * @author     John Weatherley
	 * @version    $Id: RepositoryAction.java,v 1.37 2010/07/14 00:18:49 jweather Exp $
	 */
	private class ODLSearchHandler {
		private String offset, length, queryString, set;
		private int cursor = 0, numResultsToReturn = 0;
		private final static String DELIM = "/";


		/**
		 *  Constructor for the ODLSearchHandler object
		 *
		 * @param  setSpec        The odl set
		 * @exception  Exception  If unable to parse the odl set.
		 */
		public ODLSearchHandler(String setSpec)
			 throws Exception {
			String[] odlParams = setSpec.split("\\/");

			if (odlParams.length != 5)
				throw new Exception("Wrong number of ODL parameters. Be sure to escape / with %2F in the query string and set");

			queryString = odlParams[1];
			if (queryString == null)
				queryString = "";
			else
				// Unescape query string (replace %2F with /)
				queryString = queryString.replaceAll("%2[Ff]", "/");

			set = odlParams[2];
			// Unescape set (replace %2F with /) (slashes are not allowed in the setSpec, but just in case...)
			if (set != null)
				set = set.replaceAll("%2[Ff]", "/");

			offset = odlParams[3];
			length = odlParams[4];

			try {
				cursor = Integer.parseInt(offset);
				numResultsToReturn = Integer.parseInt(length);
			} catch (Throwable e) {
				throw new Exception("offset and length must be integers");
			}
			if (numResultsToReturn <= 0)
				throw new Exception("the number of results requested must be greater than zero");
			if (numResultsToReturn > 1000)
				throw new Exception("the number of results requested must be less than 1000");
			if (cursor < 0)
				throw new Exception("offset must be greater than zero");
		}


		/**
		 *  Gets the numRecordsBeingReturned from the ODL search request.
		 *
		 * @param  totalNumResults  The total number of results that will be returened.
		 * @return                  The numRecordsBeingReturned value
		 */
		public String getNumRecordsBeingReturned(int totalNumResults) {
			if (totalNumResults < cursor)
				return "0";
			if ((cursor + numResultsToReturn) < totalNumResults)
				return Integer.toString(numResultsToReturn);
			else
				return Integer.toString((totalNumResults - cursor));
		}


		/**
		 *  Gets the numResultsToReturn attribute of the ODLSearchHandler object
		 *
		 * @return    The numResultsToReturn value
		 */
		public int getNumResultsToReturn() {
			return numResultsToReturn;
		}


		/**
		 *  Gets the offset attribute of the ODLSearchHandler object
		 *
		 * @return    The offset value
		 */
		public String getOffset() {
			return offset;
		}


		/**
		 *  Gets the offsetInt attribute of the ODLSearchHandler object
		 *
		 * @return    The offsetInt value
		 */
		public int getOffsetInt() {
			return cursor;
		}


		/**
		 *  Gets the length attribute of the ODLSearchHandler object
		 *
		 * @return    The length value
		 */
		public String getLength() {
			return length;
		}


		/**
		 *  Gets the queryString attribute of the ODLSearchHandler object
		 *
		 * @return    The queryString value
		 */
		public String getQueryString() {
			return queryString;
		}


		/**
		 *  Gets the set attribute of the ODLSearchHandler object
		 *
		 * @return    The set value
		 */
		public String getSet() {
			if (set == null ||
				set.toLowerCase().equals("null") ||
				set.toLowerCase().equals("all"))
				return null;
			return set;
		}
	}



	/**
	 *  Creates a resumptionToken from an OAI-PMH request for ListIdentifiers or ListRecords, and provides the
	 *  set, format, from and until arguments that are associted with the request.
	 *
	 * @author     John Weatherley
	 * @version    $Id: RepositoryAction.java,v 1.37 2010/07/14 00:18:49 jweather Exp $
	 */
	private class ResumptionTokenHandler {
		private String set, format, from, until;
		private int numResultsToReturn, cursor;

		//private final static String DELIM = "%2B";
		private final static String DELIM = "/";
		private final static String NP = "null";


		/**
		 *  Constructor to use when resumptionToken is present in the request.
		 *
		 * @param  previousToken       The previous ResumptionToken taken from the request.
		 * @param  numResultsToReturn  The number or results to return.
		 * @exception  Exception       If the resumptionToken can not be parsed properly.
		 */
		public ResumptionTokenHandler(String previousToken, int numResultsToReturn)
			 throws Exception {
			this.numResultsToReturn = numResultsToReturn;

			//prtln("resumptionToken is: " + previousToken);

			//prtln("resumptionToken unescaped is: " + previousToken);

			// Split the plus ('+') symbol or it's encoded value,
			// which occurs if the token passed in via a web browser form.
			String[] args = previousToken.split("\\/");
			//String[] args = previousToken.split("\\+|%2B");

			if (args.length != 7)
				throw new Exception("Wrong number of args were parsed from the token");

			/* prtln("resumptionToken parsed args are: ");
			for (int i = 0; i < args.length; i++)
				prtln(args[i]); */
			cursor = Integer.parseInt(args[1]);
			format = args[3];
			set = args[4].equals(NP) ? null : args[4];
			from = args[5].equals(NP) ? null : args[5];
			until = args[6].equals(NP) ? null : args[6];
		}


		/**
		 *  Constructor to use when no resumptionToken is present in the request.
		 *
		 * @param  set                 The set OAI-PMH argument, or null.
		 * @param  format              The format OAI-PMH argument.
		 * @param  from                The from OAI-PMH argument, or null.
		 * @param  until               The until OAI-PMH argument, or null.
		 * @param  numResultsToReturn  The number or results to return.
		 */
		public ResumptionTokenHandler(String set,
		                              String format,
		                              String from,
		                              String until,
		                              int numResultsToReturn) {
			this.set = set;
			this.format = format;
			this.from = from;
			this.until = until;
			this.numResultsToReturn = numResultsToReturn;
			this.cursor = 0;
		}


		/**
		 *  Gets the set attribute of the ResumptionTokenFields object
		 *
		 * @return    The set value
		 */
		public String getSet() {
			return set;
		}


		/**
		 *  Gets the format attribute of the ResumptionTokenFields object
		 *
		 * @return    The format value
		 */
		public String getFormat() {
			return format;
		}


		/**
		 *  Gets the from attribute of the ResumptionTokenFields object
		 *
		 * @return    The from value
		 */
		public String getFrom() {
			return from;
		}


		/**
		 *  Gets the until attribute of the ResumptionTokenFields object
		 *
		 * @return    The until value
		 */
		public String getUntil() {
			return until;
		}


		/**
		 *  Gets the resultsLength attribute of the ResumptionTokenHandler object
		 *
		 * @return    The resultsLength value
		 */
		public String getResultsLength() {
			return Integer.toString(numResultsToReturn);
		}


		/**
		 *  Gets the resultsOffset attribute of the ResumptionTokenHandler object
		 *
		 * @param  totalNumResults  DESCRIPTION
		 * @return                  The resultsOffset value
		 */
		public String getResultsOffset(int totalNumResults) {
			if (totalNumResults < cursor)
				return Integer.toString(totalNumResults);
			return Integer.toString(cursor);
		}


		/**
		 *  Gets the numRecordsBeingReturned attribute of the ResumptionTokenHandler object
		 *
		 * @param  totalNumResults  The total number of results.
		 * @return                  The numRecordsBeingReturned value
		 */
		public String getNumRecordsBeingReturned(int totalNumResults) {
			if (totalNumResults < cursor)
				return "0";
			if ((cursor + numResultsToReturn) < totalNumResults)
				return Integer.toString(numResultsToReturn);
			else
				return Integer.toString((totalNumResults - cursor));
		}


		/**
		 *  Determines whether there is a need to resume harvesting.
		 *
		 * @param  totalNumResults  The total number of results
		 * @return                  True if there is no resumption token after this one.
		 */
		public boolean isLastToken(int totalNumResults) {
			return (totalNumResults <= (cursor + numResultsToReturn));
		}


		/**
		 *  Gets the nextToken attribute of the ResumptionTokenFields object
		 *
		 * @param  totalNumResults  The total number of results that are in the complete result set.
		 * @return                  The nextToken value
		 */
		public String getNextToken(int totalNumResults) {
			int nextCursor = cursor + numResultsToReturn;

			// Per protocol, show no resumption token on an initial complete set.
			if (totalNumResults <= numResultsToReturn)
				return "";

			// This is the last token...
			if (totalNumResults <= nextCursor) {
				return "<resumptionToken completeListSize=\"" + totalNumResults +
					"\" cursor=\"" + cursor + "\"/>";
			}

			// This is not yet the last token...
			StringBuffer out = new StringBuffer();
			out.append("<resumptionToken completeListSize=\"" + totalNumResults +
				"\" cursor=\"" + cursor + "\">");

			out.append(
				cursor + DELIM +
				nextCursor + DELIM +
				totalNumResults + DELIM +
				((format == null) ? NP : format.trim()) + DELIM +
				((set == null) ? NP : set.trim()) + DELIM +
				((from == null) ? NP : from.trim()) + DELIM +
				((until == null) ? NP : until.trim()));
			out.append("</resumptionToken>\n");

			return out.toString();
		}
	}


	private static Map validArgsMap = null;


	/**
	 *  Gets the nonValidRequestArguments, or null if none were found.
	 *
	 * @param  request  The HttpServletRequest
	 */
	private boolean validateRequestArguments(HttpServletRequest request, RepositoryForm rf) {
		boolean hasNonValidArg = false;
		
		if (validArgsMap == null) {
			validArgsMap = new HashMap(8);
			validArgsMap.putAll(OAIArgs.ALL_VALID_OAI_ARGUMENTS_MAP);

			// Allow the rt argument, used in the webapps to validate OAI responses
			validArgsMap.put("rt", "rt");
		}

		String msg = "The following request arguments are not valid for OAI-PMH: ";		
		Enumeration params = request.getParameterNames();
		while (params.hasMoreElements()) {
			
			// Check if the arg is valid in OAI-PMH:
			String param = (String) params.nextElement();
			if (!validArgsMap.containsKey(param)) {
				msg += (hasNonValidArg ? ", " : " ") + param;
				hasNonValidArg = true;
			}
			// Verify that the arg does not repeat:
			else if (request.getParameterValues(param).length > 1) {
				msg += (hasNonValidArg ? ", " : " ") + param + " (repeated not allowed)";
				hasNonValidArg = true;
			}			
		}
		
		// Write the error message and code:
		if(hasNonValidArg)
			rf.addOaiError(OAICodes.BAD_ARGUMENT, msg);

		return hasNonValidArg;
	}


	// --------------- Debug output ------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}


