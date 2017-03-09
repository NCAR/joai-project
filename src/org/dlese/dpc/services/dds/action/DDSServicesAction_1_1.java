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
package org.dlese.dpc.services.dds.action;

import org.dlese.dpc.services.dds.action.form.*;
import org.dlese.dpc.dds.*;
import org.dlese.dpc.dds.action.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.analysis.*;
import org.apache.lucene.queryParser.QueryParser;
import org.dlese.dpc.util.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.webapps.servlets.filters.GzipFilter;
import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.schemedit.SchemEditServlet;

import org.apache.lucene.search.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.util.*;
import java.text.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.ServletContext;
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
 *  An <strong>Action</strong> that handles DDS Web service requests. This class handles DDSWS service version
 *  1.1, overriding methods in the base class as needed.
 *
 * @author     John Weatherley
 * @version    $Id: DDSServicesAction_1_1.java,v 1.5 2009/03/20 23:33:59 jweather Exp $
 * @see        org.dlese.dpc.services.dds.action.form.DDSServicesForm_1_1
 */
public final class DDSServicesAction_1_1 extends DDSServicesAction {

	/**
	 *  Gets the key used for caching the ListCollections response in the application scope. Should be overwritten by
	 *  the subclasses and updated in the ListCollections.jsp page when a new version is created.
	 */
	 protected String getListCollectionsCacheKey() {
		 return "ListCollectionsResponse11";
	 }
	
	/**
	 *  Sets the record XML in the form bean using the stripped but not localized version of XML.
	 *
	 * @param  df          The form
	 * @param  resultDoc   The resultDoc
	 */
	protected void setRecordXml(DDSServicesForm df, ResultDoc resultDoc) {
		df.setRecordXml(((XMLDocReader) resultDoc.getDocReader()).getXmlStripped());
	}
}


