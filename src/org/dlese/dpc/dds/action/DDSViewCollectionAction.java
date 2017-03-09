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

import org.dlese.dpc.repository.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.dds.action.form.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.vocab.*;

import java.util.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

/**
 *  A Struts Action for handling display of collection info.
 *
 * @author    Ryan Deardorff
 */
public final class DDSViewCollectionAction extends Action {

	/**
	 *  Processes the specified HTTP request and creates the corresponding HTTP
	 *  response by forwarding to a JSP that will create it.
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
	public ActionForward execute( ActionMapping mapping,
	                              ActionForm form,
	                              HttpServletRequest request,
	                              HttpServletResponse response )
		 throws IOException, ServletException {
		/*
		 *Design note:
		 *Only one instance of this class gets created for the app and shared by
		 *all threads. To be thread-safe, use only local variables, not instance
		 *variables (the JVM will handle these properly using the stack). Pass
		 *all variables via method signatures rather than instance vars.
		 */
		DDSViewCollectionForm vcf = (DDSViewCollectionForm)form;
		if ( vcf == null ) {
			vcf = new DDSViewCollectionForm();
		}
		else {
			vcf.setError( null );
		}
		SimpleLuceneIndex index = (SimpleLuceneIndex)servlet.getServletContext().getAttribute( "index" );
		MetadataVocab vocab = (MetadataVocab)getServlet().getServletContext().getAttribute( "MetadataVocab" );
		if ( index == null ) {
			throw new ServletException( "The attribute \"index\" could not be found in the Servlet Context." );
		}
		else if ( vocab == null ) {
			throw new ServletException( "The attribute \"MetadataVocab\" could not be found in the Servlet Context." );
		}
		else {
			vcf.setVocab( vocab );
			vcf.clearVocabCache();
			// Brief display (used by collection drop-down "tooltip" popups):
			if ( request.getParameter( "keyBrief" ) != null ) {
				ResultDocList resultDocs = index.searchDocs( "key:" + request.getParameter( "keyBrief" ) );
				if ( resultDocs == null || resultDocs.size() < 1 ) {
					vcf.setError( "Collection not in index" );
				}
				else {
					vcf.setResultDoc( resultDocs.get(0) );
					return mapping.findForward( "view.collection.description.brief" );
				}
			}
			// Standard display:
			else if ( request.getParameter( "key" ) != null || request.getParameter( "ky" ) != null ) {
				if ( request.getParameter( "noHead" ) != null ) {
					vcf.setHeadless( true );
				}
				String collKey = "";
				if ( request.getParameter( "ky" ) != null ) {
					collKey = vocab.getMetaNameOfId( "dds.descr.en-us", "ky", request.getParameter( "ky" ) );
				}
				else {
					collKey = request.getParameter( "key" );
				}
				ResultDocList resultDocs = index.searchDocs( "key:" + collKey );
				if ( resultDocs == null || resultDocs.size() < 1 ) {
					vcf.setError( "Collection not in index" );
				}
				else {
					vcf.setResultDoc( resultDocs.get(0) );
					return mapping.findForward( "view.collection.description" );
				}
			}
		}
		return mapping.findForward( "error" );
	}
}

