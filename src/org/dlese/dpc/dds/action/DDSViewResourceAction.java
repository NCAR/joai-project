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
import org.dlese.dpc.webapps.tools.GeneralServletTools;

import java.util.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.*;
import javax.servlet.http.*;
import java.text.*;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

/**
 *  A Struts Action for handling display of resource record descriptions, and
 *  their various collection info.
 *
 * @author    Ryan Deardorff
 */
public final class DDSViewResourceAction extends Action {

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
		DDSViewResourceForm vrf = (DDSViewResourceForm)form;
		if ( vrf == null ) {
			System.out.println( "vrf is NULL" );
			vrf = new DDSViewResourceForm();
		}
		else {
			vrf.setError( null );
		}

		// As of v3.3 these requests are handled by the DSV, so we simply redirect
		vrf.setForwardUrl( "/library/view_resource.do?" + request.getQueryString() );
		return mapping.findForward( "dsv.redirect" );
	}


	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	private final static String getDateStamp() {
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

}

