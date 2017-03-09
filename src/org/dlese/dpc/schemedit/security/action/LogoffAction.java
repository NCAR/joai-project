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
package org.dlese.dpc.schemedit.security.action;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;

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

import org.dlese.dpc.schemedit.Constants;
import org.dlese.dpc.schemedit.security.user.User;

/**
 * Implementation of <strong>Action</strong> that processes a
 * user logoff.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.6 $ $Date: 2009/03/20 23:33:57 $
 */

public final class LogoffAction extends Action {

	private static boolean debug = true;
	
    // --------------------------------------------------------- Public Methods


    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it).
     * Return an <code>ActionForward</code> instance describing where and how
     * control should be forwarded, or <code>null</code> if the response has
     * already been completed.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param actionForm The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception occurs
     */
    public ActionForward execute(ActionMapping mapping,
				 ActionForm form,
				 HttpServletRequest request,
				 HttpServletResponse response)
	throws IOException, ServletException {

	// Extract attributes we will need
	Locale locale = getLocale(request);
	MessageResources messages = getResources(request);
	HttpSession session = request.getSession();
	User user = (User) session.getAttribute(Constants.USER_KEY);

	// Process this user logoff
	if (user != null) {
	    if (debug)
	        servlet.log("LogoffAction: User '" + user.getUsername() +
	                    "' logged off in session " + session.getId());
	} else {
	    if (debug)
	        servlet.log("LogoffActon: User logged off in session " +
	                    session.getId());
	}
	session.removeAttribute(Constants.USER_KEY);
	session.invalidate();

	ActionErrors errors = new ActionErrors ();
	errors.add("message",
		new ActionError("logoff.success"));
	saveErrors(request, errors);
	
	// Forward control to the specified success URI
	return (mapping.findForward("logoff"));

    }


}
