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
import org.dlese.dpc.dds.action.form.*;
import org.dlese.dpc.dds.action.*;
import org.dlese.dpc.dds.*;
import org.dlese.dpc.dds.action.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.index.writer.*;
import org.dlese.dpc.webapps.servlets.filters.GzipFilter;
import org.dlese.dpc.vocab.MetadataVocab;
import org.apache.lucene.search.*;

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
 *  An <strong>Action</strong> that handles RSS 2.0 requests.
 *
 * @see    org.dlese.dpc.services.dds.action.form.DDSRSS20Form
 */
public final class DDSRSS20Action extends Action {

	private static boolean debug = true;


	// --------------------------------------------------------- Public Methods

	/**
	 *  Processes the DDS web service request by forwarding to the appropriate
	 *  corresponding JSP page for rendering.
	 *
	 * @param  mapping        The ActionMapping used to select this instance
	 * @param  request        The HTTP request we are processing
	 * @param  response       The HTTP response we are creating
	 * @param  form           The ActionForm for the given page
	 * @return                The ActionForward instance describing where and how
	 *      control should be forwarded
	 * @exception  Exception  If error.
	 */
	public ActionForward execute(
	                              ActionMapping mapping,
	                              ActionForm form,
	                              HttpServletRequest request,
	                              HttpServletResponse response )
		 throws Exception {
		/*
		 *  Design note:
		 *  Only one instance of this class gets created for the app and shared by
		 *  all threads. To be thread-safe, use only local variables, not instance
		 *  variables (the JVM will handle these properly using the stack). Pass
		 *  all variables via method signatures rather than instance vars.
		 */
		DDSRSS20Form rssForm = (DDSRSS20Form)form;

		try {

			RepositoryManager rm =
				(RepositoryManager)servlet.getServletContext().getAttribute( "repositoryManager" );

			return doRssSearch( request, response, rm, rssForm, mapping );
		}
		catch ( NullPointerException npe ) {
			prtln( "DDSRSS20Action caught exception. " + npe );
			npe.printStackTrace();
			rssForm.setErrorMsg( "There was an internal error by the server: " + npe );
			return ( mapping.findForward( "rss20.error" ) );
		}
		catch ( Throwable e ) {
			prtln( "DDSRSS20Action caught exception. " + e );
			e.printStackTrace();
			rssForm.setErrorMsg( "There was an internal error by the server: " + e );
			return ( mapping.findForward( "rss20.error" ) );
		}
	}


	/**
	 *  Handles a request to perform a search over item-level records, returning a
	 *  response in RSS 2.0 format. This request exposes the same search options
	 *  that users experience when performing a search for educational resources in
	 *  the DDS, including what's new, etc..<p>
	 *
	 *
	 *
	 * @param  request        The HTTP request
	 * @param  response       The HTTP response
	 * @param  rm             The RepositoryManager used
	 * @param  mapping        ActionMapping used
	 * @param  rssForm        DESCRIPTION
	 * @return                An ActionForward to the JSP page that will handle the
	 *      response
	 * @exception  Exception  If error.
	 */
	protected ActionForward doRssSearch(
	                                     HttpServletRequest request,
	                                     HttpServletResponse response,
	                                     RepositoryManager rm,
	                                     DDSRSS20Form rssForm,
	                                     ActionMapping mapping )
		 throws Exception {

		// For logging
		int searchType = DDSQueryAction.SEARCHTYPE_GENERAL_RSS;

		// If this is a what's new request, set the logging response type
		if ( request.getParameter( "wnfrom" ) != null || request.getParameter( "wnto" ) != null ) {
			searchType = DDSQueryAction.SEARCHTYPE_WHATSNEW_RSS;
		}

		String metadataVocabInstanceAttributeName = (String)servlet.getServletContext().getInitParameter( "metadataVocabInstanceAttributeName" );
		MetadataVocab vocab = (MetadataVocab)servlet.getServletContext().getAttribute( metadataVocabInstanceAttributeName );

		// Perform the search...
		DDSStandardSearchResult standardSearchResult =
			DDSQueryAction.ddsStandardQuery(
			request,
			null,
			rm,
			vocab,
			servlet.getServletContext(),
			searchType );

		ResultDocList resultDocs = standardSearchResult.getResults();

		if ( resultDocs == null || resultDocs.size() == 0 ) {
			rssForm.setResults( null );
		}
		else {
			rssForm.setResults( resultDocs );
		}

		return mapping.findForward( "rss20.response" );
	}


	// --------------- Debug output ------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
			new SimpleDateFormat( "MMM d, yyyy h:mm:ss a zzz" ).format( new Date() );
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final void prtlnErr( String s ) {
		System.err.println( getDateStamp() + " " + s );
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln( String s ) {
		if ( debug ) {
			System.out.println( getDateStamp() + " " + s );
		}
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug( boolean db ) {
		debug = db;
	}
}

