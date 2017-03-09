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
package org.dlese.dpc.vocab;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.text.*;
import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.dds.DDSServlet;

/**
 *  Provided as an intialization (and administrative) servlet for the DLESE
 *  Metadata UI system (OPML-based controlled vocabularies)
 *
 * @author    Ryan Deardorff
 */
public class MetadataVocabServlet extends HttpServlet {

	private static boolean debug = false;
	private static boolean isInitialized = false;
	NewVocabsChecker newVocabsChecker = null;
	String vocabTextFile;
	String metadataGroupsLoaderFile;
	String metadataVocabInstanceAttributeName;
	

	/**
	 *  Constructor for the MetadataServlet object
	 */
	public MetadataVocabServlet() { }

	/**
	 *  The standard <code>HttpServlet</code> init method, called only when the
	 *  servlet is first loaded.
	 *
	 * @param  config
	 * @exception  ServletException
	 */
	public void init( ServletConfig config ) throws ServletException {
		super.init( config );
		instantiateVocab();

	}
	
	/**
	 *  Description of the Method
	 */
	private synchronized void instantiateVocab() {
		if ( isInitialized ) {
			prtlnErr( "Metadata UI has already been initialized. Call to MetadataLoaderServlet.init() aborted..." );
			return;
		}
		isInitialized = true;
		// Context init params set in the context definition in server.xml or web.xml:
		if ( ( (String)getServletContext().getInitParameter( "debug" ) ).toLowerCase().equals( "true" ) ) {
			debug = true;
		}
		else {
			debug = false;
		}
		
		// if there is a value for "suppressSetCollectionsVocabDisplay", then calls to 
		// DDSServlet.setCollectionsVocabDisplay() are suppressed
		getServletContext().setAttribute ("suppressSetCollectionsVocabDisplay",
			(String) getServletContext().getInitParameter("suppressSetCollectionsVocabDisplay"));
		
		vocabTextFile = (String)getServletContext().getInitParameter( "vocabTextFile" );
		metadataGroupsLoaderFile = (String)getServletContext().getInitParameter( "metadataGroupsLoaderFile" );
		metadataVocabInstanceAttributeName = (String)getServletContext().getInitParameter( "metadataVocabInstanceAttributeName" );
		prtln( "Initializing MetadataServlet for '" + metadataVocabInstanceAttributeName + "'" );
		loadVocabs();
		System.gc();
		System.runFinalization();
		getServletContext().setAttribute( "MetadataVocabServlet", this );
		System.out.println( getDateStamp() + " MetadataVocabServlet initialized, with MetadataVocab stored as '" + metadataVocabInstanceAttributeName + "'" );
	}

	/**
	 *  Load/reload a vocab instance (invoked by newVocabsChecker when it detects
	 *  that the base loader file timestamp has changed)
	 */
	public synchronized void loadVocabs() {
		prtln( "Loading OPML from base loader at " + metadataGroupsLoaderFile );
		LoadMetadataOPML.getMetadataVocabInstance( metadataGroupsLoaderFile,
			metadataVocabInstanceAttributeName, getServletContext(), vocabTextFile, debug );
		synchWithCollectionManager( getServletContext() );
		if ( newVocabsChecker == null ) {
			newVocabsChecker = new NewVocabsChecker( this, metadataGroupsLoaderFile, getServletContext(), metadataVocabInstanceAttributeName );
			newVocabsChecker.setPriority( Thread.MIN_PRIORITY );
			newVocabsChecker.setDaemon( true );
			newVocabsChecker.start();
		}
		else {
			prtln ("about to notifyListeners via MetadataVocabOPML");
			MetadataVocab vocab = (MetadataVocab)getServletContext().getAttribute( "MetadataVocab" );
			if (vocab instanceof MetadataVocabOPML) {
				notifyReloadListeners(vocab);
			}
			else {
				// prtln ("loadVocabs - vocab was not instanceof MetadataVocabOPML!");
			}
		}
	}

	/**
	 *  Grab the MetadataVocab and RepositoryManager from the servlet context, and
	 *  if both exist, invoke the synchronization of the CM "enabled" states with
	 *  their corresponding vocabulary "display" states.
	 *
	 * @param  context
	 */
	public static void synchWithCollectionManager( ServletContext context ) {
		RepositoryManager rm = (RepositoryManager)context.getAttribute( "repositoryManager" );
		MetadataVocab vocab = (MetadataVocab)context.getAttribute( "MetadataVocab" );
		if ( ( rm != null ) && ( vocab != null ) ) {
			if (context.getAttribute ("suppressSetCollectionsVocabDisplay") == null) {
				DDSServlet.setCollectionsVocabDisplay( vocab, rm );
			}
			rm.updateVocab( vocab );
		}
	}
	
	// ----------- listener notification machinery --------------
	private List reloadListeners = null;
	
	/**
	* Nofity MetadataVocabReloadEvent listeners that a reload has occurred.
	*/
	 void notifyReloadListeners(MetadataVocab vocab) {
		prtln ("notifyListeners()");
		MetadataVocabReloadEvent event = new MetadataVocabReloadEvent(vocab);
		if(reloadListeners != null) {
			for (int i = 0; i < reloadListeners.size(); i++) {
				try {
					((MetadataVocabReloadListener) reloadListeners.get(i)).metadataVocabReloaded(event);
				} catch (Throwable t) {
					prtln("WARNING: Unexpected exception occurred while notifying reloadListeners..." + t.getMessage());
					t.printStackTrace();
				}
			}
		}
	}
	
	/**
	 *  Registers a Listener to be notified of MetadataVocabReloadEvents.
	 *
	 *@param  listener  The feature to be added to the Listener attribute
	 */
	public void addListener(MetadataVocabReloadListener listener) {
		if (listener != null) {
			if (reloadListeners == null) {
				reloadListeners = new ArrayList();
			}
			else if (reloadListeners.contains(listener)) {
				return;
			}
			reloadListeners.add(listener);
		}
	}
	

	/**
	 *  Description of the Method
	 *
	 *@param  listener  Description of the Parameter
	 */
	public void removeListener(MetadataVocabReloadListener listener) {
		if (listener != null && reloadListeners != null) {
			int index = reloadListeners.indexOf(listener);
			if (index > -1) {
				try {
					reloadListeners.remove(index);
				} catch (IndexOutOfBoundsException ioobe) {
					return;
				}
			}
		}
	}
	
	/**
	 *  Shut down sequence.
	 */
	public void destroy() {
		newVocabsChecker.shutDown();
		System.out.println( getDateStamp() + " MetadataVocabServlet stopped." );
	}

	/**
	 *  Standard doPost method forwards to doGet
	 *
	 * @param  request
	 * @param  response
	 * @exception  ServletException
	 * @exception  IOException
	 */
	public void doPost( HttpServletRequest request, HttpServletResponse response )
		 throws ServletException, IOException {
		doGet( request, response );
	}

	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getDateStamp() {
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
	private final static void prtln( String s ) {
		if ( debug ) {
			System.out.println( getDateStamp() + " " + s );
		}
	}

	/**
	 *  Sets the debug attribute of the DDSServlet object
	 *
	 * @param  db  The new debug value
	 */
	public final void setDebug( boolean db ) {
		debug = db;
	}
}

