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
package org.dlese.dpc.dds.action;
import org.dlese.dpc.dds.action.form.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;
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
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.vocab.*;
/**
 *  Implementation of <strong>Action</strong> that handles display of library
 *  histograms
 *
 * @author    Ryan Deardorff
 */
public final class HistogramAction extends Action {
	private static boolean debug = false;

	// --------------------------------------------------------- Public Methods

	/**
	 *  Process the specified HTTP request, and create the corresponding HTTP
	 *  response (or forward to another web component that will create it). Return
	 *  an <code>ActionForward</code> instance describing where and how control
	 *  should be forwarded, or <code>null</code> if the response has already been
	 *  completed.
	 *
	 * @param  mapping        The ActionMapping used to select this instance
	 * @param  response       The HTTP response we are creating
	 * @param  form           The ActionForm for the given page
	 * @param  req            The HTTP request.
	 * @return                The ActionForward instance describing where and how
	 *      control should be forwarded
	 * @exception  Exception  If error.
	 */
	public ActionForward execute(
	                              ActionMapping mapping,
	                              ActionForm form,
	                              HttpServletRequest req,
	                              HttpServletResponse response )
		 throws Exception {
		/*
		 *Design note:
		 *Only one instance of this class gets created for the app and shared by
		 *all threads. To be thread-safe, use only local variables, not instance
		 *variables (the JVM will handle these properly using the stack). Pass
		 *all variables via method signatures rather than instance vars.
		 */
		HistogramForm histogramForm = (HistogramForm)form;
		histogramForm.setServletContext( getServlet().getServletContext() );
		ActionErrors errors = new ActionErrors();
		MetadataVocab vocab = (MetadataVocab)getServlet().getServletContext().getAttribute( "MetadataVocab" );
		histogramForm.setVocab( vocab );
		SimpleLuceneIndex index = (SimpleLuceneIndex)servlet.getServletContext().getAttribute( "index" );

		try {
			// Handle software actions:
			if ( req.getParameter( "summary" ) != null ) {               // Summary display (non-linked, for generating reports)
				RepositoryManager rm =
					(RepositoryManager)servlet.getServletContext().getAttribute( "repositoryManager" );
				histogramForm.setIndex( index );
				histogramForm.setRepositoryManager( rm );
				if ( req.getParameter( "summary" ).equals( "csv" ) ) {
					return mapping.findForward( "summary.csv" );
				}
				return mapping.findForward( "summary" );
			}
			else if ( req.getParameter( "group" ) != null ) {            // Standard display
				RepositoryManager rm =
					(RepositoryManager)servlet.getServletContext().getAttribute( "repositoryManager" );
				histogramForm.resetLargestTotal();
				histogramForm.setIndex( index );
				histogramForm.setRepositoryManager( rm );
				histogramForm.setCurrentVocabName( req.getParameter( "group" ) );
				histogramForm.setCurrentVocabFramework( "adn" );
				String collectionKey = req.getParameter( "key" );
				if ( ( collectionKey != null ) && ( !collectionKey.equals( "null" ) ) ) {
					histogramForm.setHasCollectionSpecified( "true" );
					String collectionId = vocab.getTranslatedValue( "dlese_collect", "key", collectionKey );
					histogramForm.setCollectionMetaName( collectionKey );
					//	vocab.getMetaNameOfId( "dds.descr.en-us", "ky", collectionId ) );
					ResultDocList resultDocs = null;
					if ( index != null ) {
						resultDocs = index.searchDocs( "key:" + collectionKey );
						if ( resultDocs != null && resultDocs.size() > 0 ) {
							DocReader docReader = resultDocs.get(0).getDocReader();
							if ( docReader != null ) {
								histogramForm.setCollectionDocReader( docReader );
							}
							histogramForm.setCollection( collectionKey );
						}
					}
				}
				else {
					histogramForm.setHasCollectionSpecified( "false" );
					histogramForm.setCollection( "0*" );
				}
				return mapping.findForward( "display" );
			}
			// Default (non-matched) forwarding:
			return mapping.findForward( "error" );
		}
		catch ( NullPointerException e ) {
			prtln( "HistogramAction caught exception." );
			e.printStackTrace();
			return mapping.findForward( "error" );
		}
		catch ( Throwable e ) {
			prtln( "HistogramAction caught exception: " + e );
			return mapping.findForward( "error" );
		}
	}


	// ---------------------- Debug info --------------------

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
	private final void prtlnErr( String s ) {
		System.err.println( getDateStamp() + " " + s );
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln( String s ) {
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


