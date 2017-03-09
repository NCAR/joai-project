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
package org.dlese.dpc.schemedit.sif.action;

import org.dlese.dpc.repository.SetInfo;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dlese.dpc.index.ResultDoc;
import org.dlese.dpc.index.ResultDocList;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.action.DCSAction;

import org.dlese.dpc.schemedit.sif.SIFDocReader;
import org.dlese.dpc.schemedit.config.CollectionConfig;
import org.dlese.dpc.schemedit.sif.action.form.SIFReferenceForm;
import org.dlese.dpc.xml.schema.DocMap;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

import org.dom4j.Document;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.struts.util.LabelValueBean;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

/**
 *  A Struts Action controlling interaction between MetadataEditor and SIF
 *  helper, which helps users select and create SIF objects.
 *
 * @author    Jonathan Ostwald
 */
public final class SIFReferenceAction extends DCSAction {

	private static boolean debug = true;


	// --------------------------- Public Methods ------------------------------

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP
	 *  response by forwarding to a JSP that will create it. A {@link
	 *  org.dlese.dpc.repository.RepositoryManager} must be available to this class
	 *  via a ServletContext attribute under the key "repositoryManager." Returns
	 *  an {@link org.apache.struts.action.ActionForward} instance which must be
	 *  configured in struts-config.xml to forward to the JSP page that will handle
	 *  the request.
	 *
	 * @param  mapping               The ActionMapping used to select this instance
	 * @param  request               The HTTP request we are processing
	 * @param  response              The HTTP response we are creating
	 * @param  form                  The ActionForm for the given page
	 * @return                       The ActionForward instance describing where
	 *      and how control should be forwarded
	 * @exception  IOException       if an input/output error occurs
	 * @exception  ServletException  if a servlet exception occurs
	 */
	public ActionForward execute(
	                             ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest request,
	                             HttpServletResponse response)
		 throws IOException, ServletException {
		/*
		 *  Design note:
		 *  Only one instance of this class gets created for the app and shared by
		 *  all threads. To be thread-safe, use only local variables, not instance
		 *  variables (the JVM will handle these properly using the stack). Pass
		 *  all variables via method signatures rather than instance vars.
		 */
		prtln("execute");

		ActionErrors errors = initializeFromContext(mapping, request);
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (mapping.findForward("error.page"));
		}
		SIFReferenceForm sif = (SIFReferenceForm) form;
		String errorMsg = "";

		List sets = repositoryService.getAuthorizedSets(getSessionUser(request), this.requiredRole);
		SessionBean sessionBean = this.getSessionBean(request);
		sessionBean.setSets(sets);

		SchemEditUtils.showRequestParameters(request);

		String command = request.getParameter("command");
		try {
			if ("find".equals(command)) {
				initializeObjectMap(form, request);
				return mapping.findForward("sif.finder");
			}
			if ("create".equals(command)) {
				this.initializePickLists(sif, sessionBean);
				return mapping.findForward("sif.creator");
			}

			if ("Create Object".equals(command)) {
				return handleCreateObject(mapping, form, request, response);
			}

			throw new Exception("command required");
		} catch (Throwable e) {
			prtln("System Error: " + e);
			if (e instanceof NullPointerException)
				e.printStackTrace();
			return mapping.findForward("error.page");
		}
	}


	/**
	 *  Create a SIF Record and prepare to insert into target record open in
	 *  metadata editor.
	 *
	 * @param  mapping        NOT YET DOCUMENTED
	 * @param  form           NOT YET DOCUMENTED
	 * @param  request        NOT YET DOCUMENTED
	 * @param  response       NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected ActionForward handleCreateObject(ActionMapping mapping,
	                                           ActionForm form,
	                                           HttpServletRequest request,
	                                           HttpServletResponse response)
		 throws Exception {

		SIFReferenceForm sif = (SIFReferenceForm) form;
		ActionErrors errors = new ActionErrors();
		String errorMsg;

		String collection = sif.getCollection();
		CollectionConfig collectionConfig = this.collectionRegistry.getCollectionConfig(collection);
		if (collectionConfig == null)
			throw new Exception("unknown collection: " + collection);
		String xmlFormat = collectionConfig.getXmlFormat();
		boolean legalFormat = false;
		String[] sifTypes = sif.getSifTypes();
		for (int i = 0; i < sifTypes.length; i++) {
			if (sifTypes[i].equals(xmlFormat))
				legalFormat = true;
		}
		if (!legalFormat)
			throw new Exception("illegal record format requested: " + xmlFormat);

		String id;
		try {
			MetaDataFramework framework = this.getMetaDataFramework(xmlFormat);
			if (framework == null) {
				prtln("Framework is NULL?!?");
				prtln(frameworkRegistry.toString());
				throw new Exception("framework not found for \"" + xmlFormat + "\"");
			}

			id = collectionRegistry.getNextID(collection);
			Document record = framework.makeMinimalRecord(id, collectionConfig);

			DocMap docMap = new DocMap(record, framework.getSchemaHelper());

			String titlePath = framework.getNamedSchemaPathXpath("title");
			if (titlePath == null || titlePath.trim().length() == 0)
				throw new Exception("framework configuration error (title path not defined)");

			//title
			docMap.smartPut(titlePath, sif.getTitle());

			if (!framework.getSchemaHelper().getNamespaceEnabled()) {
				// now prepare document to write to file by inserting namespace information
				record = framework.getWritableRecord(record);
			}

			String username = getSessionUserName(request);
			repositoryService.saveNewRecord(id, record.asXML(), collection, username);
			sif.clear();
			sif.setNewRecId(id);
			sif.setSelectedType(framework.getName());
		} catch (Exception e) {
			prtln(e.getMessage());
			errors.add("error",
				new ActionError("generic.error", "Could not create new record: " + e.getMessage()));
		} catch (Throwable t) {
			prtln(t.getMessage());
			t.printStackTrace();
			errors.add("error",
				new ActionError("generic.error", "There has been a system error: " + t.getMessage()));
		}
		saveErrors(request, errors);
		if (!errors.isEmpty()) {
			return mapping.findForward("sif.confirm");
		}
		return mapping.findForward("sif.confirm");
	}


	/**
	 *  Initialize picklists for user-selection of frameworks and collections in
	 *  which to create a new SIF Record.
	 *
	 * @param  sif          form bean
	 * @param  sessionBean  NOT YET DOCUMENTED
	 */
	void initializePickLists(SIFReferenceForm sif, SessionBean sessionBean) {
		List objectTypeOptions = new ArrayList(); // formats
		Map collectionsMap = new HashMap(); // maps collections to format
		List authorizedSets = sessionBean.getSets();
		String[] types = sif.getSifTypes();
		if (types != null) {
			objectTypeOptions.add(new LabelValueBean(" -- all -- ", ""));
			for (int i = 0; i < types.length; i++) {
				String xmlFormat = types[i];
				MetaDataFramework framework = this.getMetaDataFramework(xmlFormat);
				if (framework == null) {
					prtln("WARNING: framework not found for " + xmlFormat);
					continue;
				}
				objectTypeOptions.add(new LabelValueBean(framework.getName(), xmlFormat));
				for (Iterator sets = authorizedSets.iterator(); sets.hasNext(); ) {
					SetInfo set = (SetInfo) sets.next();
					if (set.getFormat().equals(xmlFormat)) {
						// add collection (setspec) to collectionsMap keyed by xmlFormat
						List setBucket = null;
						if (collectionsMap.containsKey(xmlFormat))
							setBucket = (List) collectionsMap.get(xmlFormat);
						else
							setBucket = new ArrayList();
						setBucket.add(set);
						collectionsMap.put(xmlFormat, setBucket);
					}
				}
			}
		}
		sif.setTypeOptions(objectTypeOptions);
		sif.setSetMap(collectionsMap);
	}


	/**
	 *  Create a query string for use in searching for existing SIF Objects.
	 *
	 * @param  xmlFormat  NOT YET DOCUMENTED
	 * @param  sif        NOT YET DOCUMENTED
	 * @param  request    NOT YET DOCUMENTED
	 * @return            The query value
	 */
	String getQuery(String xmlFormat, SIFReferenceForm sif, HttpServletRequest request) {
		String searchString = sif.getSearchString();
		String query = this.getSessionBean(request).getCollectionsQueryClause();
		query += " AND xmlFormat:" + xmlFormat;
		if (searchString != null) {
			searchString = searchString.trim();
			sif.setSearchString(searchString);
			if (searchString.length() > 0)
				query += " AND default:" + searchString;
		}

		return query;
	}


	/**
	 *  Returns a map of Objects that are elgible to use as references in the
	 *  current SIF record.
	 *
	 * @param  form     NOT YET DOCUMENTED
	 * @param  request  NOT YET DOCUMENTED
	 */
	void initializeObjectMap(ActionForm form, HttpServletRequest request) {
		SIFReferenceForm sif = (SIFReferenceForm) form;
		SimpleLuceneIndex index = this.repositoryManager.getIndex();
		Map objectMap = new HashMap();
		String[] sifTypes = sif.getSifTypes();

		// TODO: we only want objects from the collections the user has access to!

		for (int i = 0; i < sifTypes.length; i++) {
			String xmlFormat = sifTypes[i];

			// ResultDoc [] results = index.searchDocs ("xmlFormat:" + xmlFormat);
			ResultDocList results = index.searchDocs(getQuery(xmlFormat, sif, request));

			if (results == null)
				continue;

			// prtln (results.length + " items found for " + xmlFormat);
			for (int ii = 0; ii < results.size(); ii++) {
				SIFDocReader sifReader = null;
				XMLDocReader xmlDocReader = (XMLDocReader) ((ResultDoc)results.get(ii)).getDocReader();
				if (xmlDocReader.getId().equals(sif.getRecId())) {
					continue;
				}

				try {
					sifReader = SIFDocReader.getReader(xmlFormat, xmlDocReader.getXml());
				} catch (Exception e) {
					prtln("WARNING: failed to initialize SIFDocReader for " + xmlDocReader.getId());
					continue;
				}
				String formatName = sifReader.getFormatName();
				List values = null;
				if (objectMap.containsKey(formatName))
					values = (List) objectMap.get(formatName);
				else
					values = new ArrayList();
				values.add(sifReader);
				objectMap.put(formatName, values);
			}
		}
		sif.setObjectMap(objectMap);
	}


	// -------------- Debug ------------------


	/**
	 *  Sets the debug attribute of the SIFReferenceAction class
	 *
	 * @param  isDebugOutput  The new debug value
	 */
	public static void setDebug(boolean isDebugOutput) {
		debug = isDebugOutput;
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private void prtln(String s) {
		if (debug)
			System.out.println("SIFReferenceAction: " + s);
	}
}

